package com.happy_query.writer;

import com.happy_query.parser.domain.DataDefinition;
import com.happy_query.parser.domain.DataDefinitionDataType;
import com.happy_query.query.domain.Row;
import com.happy_query.util.Constant;
import com.happy_query.util.HappyQueryException;
import com.happy_query.util.JDBCUtils;
import com.happy_query.util.OpenCSVUtil;
import com.happy_query.writer.domain.ImportParam;
import com.happy_query.writer.domain.InsertResult;
import com.happy_query.writer.domain.Record;
import com.sun.tools.classfile.ConstantPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * writer to import data
 * Created by frio on 16/6/17.
 */
public class Writer implements IWriter {
    private static Logger LOG = LoggerFactory.getLogger(Writer.class);
    private DataSource dataSource;

    public void importDataByCSV(ImportParam importParam) {
        InsertResult insertResult
                = OpenCSVUtil.readAllDefaultTemplate(importParam.getReader(), dataSource);
        try {
            insertRows(insertResult);
        } catch (SQLException e) {
            throw new HappyWriterException("import failed!", e);
        }

    }

    private void updateRows(InsertResult insertResult) throws SQLException {
        // use category type to decide which table to insert datas
        String rightTable = Constant.RIGHT_TABLE_MAP.get(insertResult.getCategoryType());
        String leftTable = Constant.LEFT_TABLE_MAP.get(insertResult.getCategoryType());
        int subKey = Constant.SUB_KEY_MAP.get(insertResult.getCategoryType());
        String leftIdColumn = Constant.LEFT_ID_COLUMNS.get(insertResult.getCategoryType());
        //transaction use
        Connection connection = null;
        /**
         * do dao update operation
         * 1: update left table;
         * 2: update right table;
         */
        List<Row> rows = insertResult.getRows();

        for (Row r : rows) {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            try {
                /**
                 * update left datas information
                 */
                Map<String, Object> leftDatas = getLeftInsertDatas(leftIdColumn, r);
                if (leftDatas.size() > 0) {
                    try {
                        if (r.getLeftId() != null) {
                            JDBCUtils.executeUpdateById(connection, leftTable, leftDatas, leftIdColumn, r.getLeftId());
                        } else {
                            throw new HappyWriterException("leftId is invalid,cannot be null!");
                        }
                    } catch (SQLException e) {
                        throw new HappyWriterException("execute left table update failed");
                    }
                }
                /**
                 * update right datas information
                 */
                Map<DataDefinition, Row.Value> m = r.getData();
                for (DataDefinition d : m.keySet()) {
                    Row.Value v = m.get(d);
                    if (v == null || v.getValue() == null) {
                        continue;
                    }
                    String valueColumn = DataDefinitionDataType.getColumnNameByDataDefinitionDataType(d.getDataType());
                    StringBuilder sqlSB = new StringBuilder();
                    sqlSB.append("insert into ").append(rightTable).append("(left_id,dd_ref_id,sub_key,").append(valueColumn).append(")")
                            .append("values").append("(?,?,?,?) on duplicate key update ").append(valueColumn).append("=?");
                    List<Object> parameters = new ArrayList<Object>();
                    parameters.add(r.getLeftId());
                    parameters.add(d.getId());
                    parameters.add(subKey);
                    parameters.add(v.getValue());
                    parameters.add(v.getValue());
                    JDBCUtils.executeUpdate(connection, sqlSB.toString(), parameters);
                }
            } catch (Exception e) {
                connection.rollback();
                throw new HappyWriterException("met a exception when doing roll insert", e);
            } finally {
                connection.commit();
                JDBCUtils.close(connection);
            }
        }
    }

    private void insertRows(InsertResult insertResult) throws SQLException {
        // use category type to decide which table to insert datas
        String rightTable = Constant.RIGHT_TABLE_MAP.get(insertResult.getCategoryType());
        String leftTable = Constant.LEFT_TABLE_MAP.get(insertResult.getCategoryType());
        int subKey = Constant.SUB_KEY_MAP.get(insertResult.getCategoryType());
        String leftIdColumn = Constant.LEFT_ID_COLUMNS.get(insertResult.getCategoryType());
        //transaction use
        Connection connection = null;

        /**
         * do dao insert operation
         * 1: insert left table;
         * 2: insert right table;
         */
        List<Row> rows = insertResult.getRows();
        List<Map<String, Object>> leftBatchInsertValues = new ArrayList<Map<String, Object>>();
        for (Row r : rows) {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            try {
                /**
                 * 1: we insert left table first
                 */
                Map<String, Object> insertDatas = getLeftInsertDatas(leftIdColumn, r);
                leftBatchInsertValues.add(insertDatas);
                if (r.getLeftId() == null) {
                    Long leftPrimaryId = JDBCUtils.insertToTable(connection, leftTable, insertDatas);
                    r.setLeftId(leftPrimaryId);
                }
                /**
                 * 2: insert right table
                 */
                Map<DataDefinition, Row.Value> m = r.getData();
                Map<String, List<List<Object>>> cachPr = new HashMap<String, List<List<Object>>>();
                for (DataDefinition d : m.keySet()) {
                    List<Object> rightParameters = new ArrayList<Object>();
                    String insertSql = getInsertSql(d.getDataType(), rightTable, subKey);
                    rightParameters.add(r.getLeftId());
                    rightParameters.add(d.getId());
                    rightParameters.add(m.get(d).getValue());
                    if (cachPr.get(insertSql) != null) {
                        cachPr.get(insertSql).add(rightParameters);
                    } else {
                        List<List<Object>> list = new ArrayList<List<Object>>();
                        list.add(rightParameters);
                        cachPr.put(insertSql, list);
                    }
                }
                //batch insert
                for (String sql : cachPr.keySet()) {
                    JDBCUtils.batchExecuteUpdate(connection, sql, cachPr.get(sql));
                }
            } catch (Exception e) {
                connection.rollback();
                throw new HappyWriterException("met a exception when doing roll insert", e);
            } finally {
                connection.commit();
                JDBCUtils.close(connection);
            }
        }

    }

    private String getInsertSql(DataDefinitionDataType dataType, String rightTable, int subKey) {
        String insertSql;
        if (dataType == DataDefinitionDataType.BOOLEAN
                || dataType == DataDefinitionDataType.DATETIME
                || dataType == DataDefinitionDataType.INT) {
            insertSql = "insert into " + rightTable + "(left_id, dd_ref_id,int_value,sub_key) " +
                    "values(?,?,?," + subKey + ")";

        } else if (dataType == DataDefinitionDataType.FLOAT
                || dataType == DataDefinitionDataType.DOUBLE) {
            insertSql
                    = "insert into " + rightTable + "(left_id, dd_ref_id,double_value,sub_key) " +
                    "values(?,?,?," + subKey + ")";

        } else if (dataType == DataDefinitionDataType.STRING) {
            insertSql
                    = "insert into " + rightTable + "(left_id, dd_ref_id,str_value,sub_key) " +
                    "values(?,?,?," + subKey + ")";
        }  else {
            insertSql
                    = "insert into " + rightTable + "(left_id, dd_ref_id,feature,sub_key) " +
                    "values(?,?,?," + subKey + ")";
        }
        return insertSql;
    }

    private Map<String, Object> getLeftInsertDatas(String leftIdColumn, Row r) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, Row.Value> e : r.getLeftTableData().entrySet()) {
            result.put(e.getKey(), e.getValue().getValue());
        }
        return result;
    }

    public void writeRecord(InsertResult insertResult) {
        try {
            insertRows(insertResult);
        } catch (SQLException e) {
            throw new HappyWriterException("write record failed", e);
        }
    }

    public void updateRecord(InsertResult insertResult) {
        try {
            updateRows(insertResult);
        } catch (SQLException e) {
            throw new HappyWriterException("");
        }
    }

    public void deleteRecord(long leftId, String category, String leftIdColumnName) {
        String rightTable = Constant.RIGHT_TABLE_MAP.get(category);
        String leftTable = Constant.LEFT_TABLE_MAP.get(category);
        StringBuilder sb = new StringBuilder();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            sb.append("delete from ").append(leftTable).append(" where ").append(leftIdColumnName).append("=?");
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(leftId);
            JDBCUtils.execute(connection, sb.toString(), parameters);
            sb.setLength(0);
            sb.append("delete from ").append(rightTable).append(" where left_id=?");
            JDBCUtils.execute(connection, sb.toString(), parameters);
            connection.commit();
        } catch (Exception e) {
            JDBCUtils.rollback(connection);
        } finally {
            JDBCUtils.close(connection);
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
