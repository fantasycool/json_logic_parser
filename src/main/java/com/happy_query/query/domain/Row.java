package com.happy_query.query.domain;

import com.happy_query.parser.domain.DataDefinition;
import com.happy_query.util.TemplateUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by frio on 16/6/15.
 */
public class Row {
    /**
     * 字典表指标对应的字段
     */
    private Map<DataDefinition, Value> data = new HashMap<DataDefinition, Value>();
    /**
     * left table 对应的固有列的值
     */
    private Map<String, Value> leftTableData = new HashMap<String, Value>();

    /**
     * left table id value
     */
    private Long leftId;

    public Map<DataDefinition, Value> getData() {
        return data;
    }

    public void setData(Map<DataDefinition, Value> data) {
        this.data = data;
    }

    public Map<String, Value> getLeftTableData() {
        return leftTableData;
    }

    public void setLeftTableData(Map<String, Value> leftTableData) {
        this.leftTableData = leftTableData;
    }

    /**
     * 返回id->value的result
     * @return
     */
    public Map<Long, Value> getFlatMapData(){
        Map<Long, Value> result = new HashMap<Long, Value>();
        for(Map.Entry<DataDefinition, Value> e: data.entrySet()){
            result.put(e.getKey().getId(), e.getValue());
        }
        return result;
    }

    public static class Value {
        /**
         * right table use
         * when is left table datadefinitions, should be null
         */
        DataDefinition dataDefinition;
        private String viewValue;
        private String columnName;
        private int columnType; //refer to java.sql.Types
        /**
         * value object
         */
        Object value;

        public Value(Object value, int columnType, DataDefinition dataDefinition){
            this.dataDefinition = dataDefinition;
            this.value = value;
            this.columnType = columnType;
        }

        public Value(Object value, int columnType){
            this.value = value;
            this.columnType = columnType;
        }

        /**
         * when creating value
         * generate view value with datadefinition template
         * @param dataDefinition
         * @param value
         * @return
         */
        public static Value createValue(DataDefinition dataDefinition, String value){
            Value v= new Value();
            Map<String, Object> context = new HashMap<String, Object>();
            context.put(dataDefinition.getTemplate(), value);
            String viewValue = TemplateUtil.getViewValueByTemplateStr(dataDefinition.getTemplate(), context);
            v.setViewValue(viewValue);
            return v;
        }

        public Value(){

        }

        /**
         * 读取template脚本,进行渲染
         * @return
         */
        public String getViewValue(){
            return "";
        }

        public DataDefinition getDataDefinition() {
            return dataDefinition;
        }

        public void setDataDefinition(DataDefinition dataDefinition) {
            this.dataDefinition = dataDefinition;
        }

        public int getColumnType() {
            return columnType;
        }

        public void setColumnType(int columnType) {
            this.columnType = columnType;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public void setViewValue(String viewValue) {
            this.viewValue = viewValue;
        }
    }

    public Long getLeftId() {
        return leftId;
    }

    public void setLeftId(Long leftId) {
        this.leftId = leftId;
    }
}
