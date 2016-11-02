package com.happy_query.domain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.happy_query.parser.JsonSqlParser;
import com.happy_query.query.cache.DataDefinitionCacheManager;
import com.happy_query.util.*;
import com.happy_query.util.Constant;
import com.jkys.moye.*;
import org.apache.commons.lang.math.DoubleRange;
import org.apache.commons.lang.math.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by frio on 16/6/14.
 */
public class DataDefinition {
    static Logger LOG = LoggerFactory.getLogger(DataDefinition.class);
    private Long id;
    private Long parentId;
    private Integer type;
    private String childCommentName;
    private Integer updateType;
    private Integer rank;
    private String computationRule;
    private Integer tagType;
    private String definitionType;
    private String dataType;
    private DataDefinition parent;
    private DataDefinition childComment;
    private String computationJson;

    /**
     * 字典名称
     */
    private String key;

    /**
     * 字段数据类型
     */
    private DataDefinitionDataType dataTypeEnum;

    private DefinitionType definitionTypeEnum;
    /**
     * 是否支持筛选
     */
    private Boolean isQuery;
    /**
     * 字段描述
     */
    private String description;
    /**
     * 规则
     */
    private String rule;
    /**
     * 模板,控制展示,freemarker脚本
     */
    private String template;

    /**
     * 控制是否使用template
     */
    private Boolean isUseTemplate;
    /**
     * 字段状态
     */
    private String unit;

    private String sourceData;

    private int status;

    private String nickName;

    private Boolean isEditable;

    private Boolean isLeftData;

    private String leftColName;

    private Date gmtCreate;

    private Date gmtModified;

    private String isRequired;

    private List<DataOption> dataOptionList;

    public static DataDefinition getDataDefinition(DataSource dataSource, Long id) {
        NullChecker.checkNull(id);
        List<Object> list = Arrays.asList((Object) id);
        try {
            String sql = String.format("select * from %s where id=? and status=0 order by gmt_create desc limit 1", Constant.TABLE_NAME);
            List<Map<String, Object>> data = JDBCUtils.executeQuery(dataSource, sql, list);
            return getDataDefinition(data);
        } catch (SQLException e) {
            LOG.error("getDataDefinition failed!", e);
            LOG.error("param id is [{}]", id);
        }
        return new DataDefinitionCacheManager.NullDataDefinition();
    }

    private static DataDefinition getDataDefinition(List<Map<String, Object>> data) {
        if (data == null || data.size() == 0) {
            return new DataDefinitionCacheManager.NullDataDefinition();
        }
        DataDefinition dataDefinition = new DataDefinition();
        ReflectionUtil.cloneMapValueToBean(data.get(0), dataDefinition);
        return dataDefinition;
    }

    public static DataDefinition getDataDefinitionByName(DataSource dataSource, String name) {
        NullChecker.checkNull(name);
        List<Object> list = Arrays.asList((Object) name);
        try {
            String sql = String.format("select * from %s where `key`=? and status=0 order by gmt_create desc limit 1", Constant.TABLE_NAME);
            List<Map<String, Object>> data = JDBCUtils.executeQuery(dataSource, sql, list);
            return getDataDefinition(data);
        } catch (SQLException e) {
            LOG.error("getDataDefinitionByName failed!param name is [{}]", name, e);
            throw new HappyQueryException("getDataDefinitionByName failed!param name is " + name, e);
        }
    }

    public static List<DataDefinition> queryGroupDataDefinitions(DataSource dataSource){
        NullChecker.checkNull(dataSource);
        List<Object> params = new ArrayList<>();
        try {
            List<Map<String, Object>> datas = JDBCUtils.executeQuery(dataSource, "select * from " + Constant.TABLE_NAME + " where tag_type=3 and status=0", params);
            List<DataDefinition> result = new ArrayList<>();
            for(Map<String, Object> data : datas){
                result.add(DataDefinitionCacheManager.getDataDefinition(data.get("key").toString()));
            }
            return result;
        } catch (SQLException e) {
            LOG.error("queryGroupDataDefinitions failed", e);
            throw new HappyQueryException(e);
        }
    }

    public static List<DataDefinition> queryAllTagDefinitions(DataSource dataSource){
        NullChecker.checkNull(dataSource);
        List<Object> params = new ArrayList<>();
        try {
            List<Map<String, Object>> datas = JDBCUtils.executeQuery(dataSource, "select * from " + Constant.TABLE_NAME + " where type=1 and status=0", params);
            List<DataDefinition> result = new ArrayList<>();
            for(Map<String, Object> data : datas){
                result.add(DataDefinitionCacheManager.getDataDefinition(data.get("key").toString()));
            }
            return result;
        } catch (SQLException e) {
            LOG.error("queryGroupDataDefinitions failed", e);
            throw new HappyQueryException(e);
        }
    }

    /**
     * 查询获取所有字典
     * @param dataSource
     * @return
     */
    public static List<Map<String, Object>> queryAllTagMapDefinitions(DataSource dataSource){
        NullChecker.checkNull(dataSource);
        List<DataDefinition> dataDefinitions = queryAllTagDefinitions(dataSource);
        List<DataDefinition> groupDataDefinition = queryGroupDataDefinitions(dataSource);
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> addedKeys = new HashSet<>();
        /**
         * add group datadefinitions
         */
        for(DataDefinition dataDefinition : groupDataDefinition){
            if(dataDefinition.getType() == Constant.TAG_TYPE && dataDefinition.getTagType() == Constant.GROUP_BIAO_QIAN){
                List<String> keys = PrmTagKeyRelation.querySubKeysByGroupKey(dataSource, dataDefinition.getKey());
                for(String key : keys){
                    Map<String, Object> map = new HashMap<>();
                    addedKeys.add(key);
                    DataDefinition childDataDefinition = DataDefinitionCacheManager.getDataDefinition(key);
                    map.put("tagType", "组标签");
                    map.put("tagParentName", dataDefinition.getNickName());
                    fillMapFromDataDefinition(map, childDataDefinition);
                }
            }
        }
        /**
         * add normal tag data definitions
         */
        for(DataDefinition dataDefinition : dataDefinitions){
            if(addedKeys.contains(dataDefinition.getKey())){
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            fillMapFromDataDefinition(map, dataDefinition);
            result.add(map);
        }
        return result;
    }

    //TODO

    /**
     * 获取TagInfo的详细信息
     * @return
     */
    public static Map<String, Object> getTagInfo(){
        return null;
    }

    private static void fillMapFromDataDefinition(Map<String, Object> map, DataDefinition childDataDefinition) {
        map.put("tagKey", childDataDefinition.getId());
        map.put("tagName", childDataDefinition.getNickName());
        map.put("tagWay", childDataDefinition.getTagType() == 1 ? "系统标签": "动态标签");
        if(!StringUtils.isBlank(childDataDefinition.getComputationJson())){
            map.put("tagRule", DataDefinition.describeExpression(childDataDefinition.getComputationRule()));
        }
        map.put("tagComment", childDataDefinition.getDescription());
    }

    public static void insertDataDefinition(DataSource dataSource, DataDefinition dataDefinition) {
        NullChecker.checkNull(dataDefinition);
        Map<String, Object> map = ReflectionUtil.cloneBeanToMap(dataDefinition);
        try {
            dataDefinition.setId(JDBCUtils.insertToTable(dataSource, Constant.TABLE_NAME, map));
        } catch (SQLException e) {
            LOG.error("insert datadefinition failed, datadefinition content is:[{}], t is:[{}]", dataDefinition.toString(), e);
            throw new HappyQueryException("insert failed", e);
        }
    }

    public static int updateDataDefinitionByKey(DataSource dataSource, DataDefinition dataDefinition){
        NullChecker.checkNull(dataDefinition.getKey());
        Map<String, Object> map = ReflectionUtil.cloneBeanToMap(dataDefinition);
        try{
            DataDefinition oldDataDefinition = DataDefinitionCacheManager.getDataDefinition(dataDefinition.getKey());
            if(oldDataDefinition instanceof DataDefinitionCacheManager.NullDataDefinition){
                throw new HappyQueryException("dataDefinition:" + JSON.toJSONString(dataDefinition) + " not exists!");
            }
            dataDefinition.setId(oldDataDefinition.getId());
            dataDefinition.setDataType(oldDataDefinition.getDataType());
            DataDefinitionCacheManager.delByKey(dataDefinition.getKey());
            return JDBCUtils.executeUpdateById(dataSource, Constant.TABLE_NAME, map, "id", dataDefinition.getId());
        } catch(SQLException e){
            LOG.error("updateDataDefinitionByKey failed, dataDefintion:" + JSON.toJSONString(dataDefinition), e);
            throw new HappyQueryException(e);
        }
    }

    /**
     * 新增标签指标的时候调用,根据标签类型
     *  1. 系统标签. 多个指标的组合生成,这些多个指标都和此标签为映射关系
     *  2. 动态标签. 多个指标的组合生成,这些多个指标都和此标签为映射关系
     * @param dataSource
     * @param dataDefinition
     */
    public static void insertTagDataDefinition(DataSource dataSource, DataDefinition dataDefinition){
        if(dataDefinition.getType() != 1){
            throw new HappyQueryException("要创建的指标不为标签指标");
        }
        if(StringUtils.isNoneBlank(dataDefinition.getComputationRule())){
            MoyeParser moyeParser = new MoyeParserImpl();
            List<Word> words = moyeParser.parseExpression(dataDefinition.getComputationRule());
            for(Word w : words){
                if(w instanceof DynamicVariable){
                    KeyRelation.insertKeyRelation(dataDefinition.getKey(), w.getName(), dataSource);
                }
            }
        }
        insertDataDefinition(dataSource, dataDefinition);
    }


    /**
     * 修改组标签,修改子标签
     * @param dataSource
     * @param groupTag key, nick_name, description, computationJson
     * @param childsTag key, nick_name, computationJson, description
     * @param tagType
     * @return
     */
    public static void updateGroupTagDataDefinition(DataSource dataSource, DataDefinition groupTag,
                                                                    List<DataDefinition> childsTag){
        NullChecker.checkNull(dataSource, groupTag, childsTag);
        JsonSqlParser jsonSqlParser = new JsonSqlParser();
        if(StringUtils.isNotBlank(groupTag.getComputationJson())){
            groupTag.setComputationRule(jsonSqlParser.convertJsonToLispExpression(groupTag.getComputationJson()));
        }
        //update组标签
        updateDataDefinitionByKey(dataSource, groupTag);
        for(DataDefinition dataDefinition : childsTag){
            dataDefinition.setComputationRule(jsonSqlParser.convertJsonToLispExpression(mergeJson(groupTag.getComputationJson(), dataDefinition.getComputationJson())));
            updateDataDefinitionByKey(dataSource, dataDefinition);
        }
    }

    /**
     * 创建组标签,组标签的子标签, 组标签子标签连带关系
     * @param dataSource
     * @param groupTag nick_name, description, computationJson
     * @param childsTag nick_name, computationJson, description
     * @param tagType 1:系统标签;2:动态标签
     * @return 要进行异步任务打标的子标签列表
     */
    public static List<DataDefinition> insertGroupTagDataDefinition(DataSource dataSource, DataDefinition groupTag,
                                                                    List<DataDefinition> childsTag, int tagType){
        NullChecker.checkNull(groupTag, childsTag, groupTag.getNickName(), groupTag.getDescription(), groupTag.getComputationJson());
        for(DataDefinition dataDefinition : childsTag){
            NullChecker.checkNull(dataDefinition.getNickName(), dataDefinition.getComputationJson(), dataDefinition.getDescription());
        }
        List<DataDefinition> result = new ArrayList<>();
        //create group tag
        JsonSqlParser jsonSqlParser = new JsonSqlParser();
        if(StringUtils.isNotBlank(groupTag.getComputationJson())){
            groupTag.setComputationRule(jsonSqlParser.convertJsonToLispExpression(groupTag.getComputationJson()));
        }
        groupTag.setType(Constant.TAG_TYPE);
        groupTag.setTagType(Constant.GROUP_BIAO_QIAN);
        groupTag.setKey(String.valueOf(System.currentTimeMillis()));
        groupTag.setDefinitionType("input");
        groupTag.setDataType("int");
        insertDataDefinition(dataSource, groupTag);
        validateRange(childsTag);
        //create childs tags
        int i = 0;
        for(DataDefinition dataDefinition : childsTag){
            dataDefinition.setKey(String.valueOf(System.currentTimeMillis()) + i);
            dataDefinition.setComputationRule(jsonSqlParser.convertJsonToLispExpression(mergeJson(groupTag.getComputationJson(), dataDefinition.getComputationJson())));
            dataDefinition.setType(Constant.TAG_TYPE);
            dataDefinition.setTagType(tagType);
            dataDefinition.setDefinitionType("input");
            dataDefinition.setDataType("int");
            insertDataDefinition(dataSource, dataDefinition);

            PrmTagKeyRelation prmTagKeyRelation = new PrmTagKeyRelation();
            prmTagKeyRelation.setGroupKey(groupTag.getKey());
            prmTagKeyRelation.setSubKey(dataDefinition.getKey());
            PrmTagKeyRelation.insert(dataSource, prmTagKeyRelation);
            result.add(dataDefinition);
            i ++;
        }
        //end
        return result;
    }

    /**
     * 判断组标签下面的子标签是否互相重叠
     * @param childsTag
     */
    public static void validateRange(List<DataDefinition> childsTag) {
        NullChecker.checkNull(childsTag);
        List<Object> ranges = new ArrayList<>();
        for(DataDefinition dataDefinition : childsTag){
            NullChecker.checkNull(dataDefinition.getComputationJson());
            String json = dataDefinition.getComputationJson();
            JSONObject jsonObject = (JSONObject) JSON.parse(json);
            if(jsonObject.getString(Constant.OPERATOR).equals(Constant.OPERATOR_VALUE_RANGE)){
                if(!dataDefinition.getDataType().equals(DataDefinitionDataType.STRING.toString())){
                    String value = jsonObject.getString(Constant.VALUE);
                    DoubleRange doubleRange = generateDoubleRangeFromValue(value);
                    ranges.add(doubleRange);
                }else{
                    String value = jsonObject.getString(Constant.VALUE);
                    StringRange stringRange = generateStringRangeFromValue(value);
                    ranges.add(stringRange);
                }
            }else if(jsonObject.getString(Constant.OPERATOR).equals(Constant.OPERATOR_VALUE_CONTAINS)){
                String value = jsonObject.getString(Constant.VALUE);
                for(String v : value.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "").replaceAll("\"", "").split(",")){
                    ranges.add(new StringRange(v.trim(), v.trim()));
                }
            }else if(jsonObject.getString(Constant.OPERATOR).equals(Constant.OPERATOR_VALUE_EQUALS)){
                String value = jsonObject.getString(Constant.VALUE);
                ranges.add(new StringRange(value.trim(), value.trim()));
            }
        }
        //排列组合校验Ranges之间是否互相包含
        for(int i = 0; i < ranges.size(); i ++){
            for(int j = i+1; i < ranges.size(); j ++){
                if(j == ranges.size()){
                    break;
                }
                Object sourceRange = ranges.get(i);
                Object targetRange = ranges.get(j);
                if(sourceRange instanceof DoubleRange){
                    if(((DoubleRange) sourceRange).overlapsRange((Range) targetRange)){
                        throw new HappyQueryException(Constant.HAPPY_QUERY_ERROR_RULE_OVERRIDE);
                    }
                }else if(sourceRange instanceof StringRange){
                    if(((StringRange) sourceRange).overlapsRange((StringRange) targetRange)){
                        throw new HappyQueryException(Constant.HAPPY_QUERY_ERROR_RULE_OVERRIDE);
                    }
                }
            }
        }
    }


    /**
     * 生成expression的自然语言展示
     * @param expression
     * @return
     */
    public static String describeExpression(String expression) {
        MoyeParser moyeParser = new MoyeParserImpl();
        List<Word> words = moyeParser.parseExpression(expression);
        return describe(words);
    }

    public static String describe(List<Word> words) {
        boolean isLeft = false;
        Operator operator = null;
        List<Object> args = new ArrayList<Object>();
        for (int i = 0; i < words.size() - 1; i++) {
            if (i == 0 && !(words.get(i) instanceof LeftBracket)) {
                throw new ParseException("expression must be started with (");
            } else if (words.get(i) instanceof Operator) {
                operator = (Operator) words.get(i);
            } else if (words.get(i) instanceof BaseTypeValue || words.get(i) instanceof DynamicVariable) {
                if(words.get(i) instanceof DynamicVariable){
                    String key = words.get(i).getName();
                    DataDefinition dataDefinition = DataDefinitionCacheManager.getDataDefinition(key);
                    if(dataDefinition instanceof DataDefinitionCacheManager.NullDataDefinition){
                        args.add(words.get(i));
                    }else{
                        args.add(StringUtils.isBlank(dataDefinition.getNickName())?words.get(i): dataDefinition.getNickName());
                    }
                }else{
                    args.add(words.get(i));
                }
            } else if (words.get(i) instanceof LeftBracket && isLeft) {
                List<Word> groupWords = MoyeComputeEngineImpl.cutoutGroupWords(words, i);
                args.add(" (" + describe(groupWords) + ") ");
                i = i + groupWords.size() - 1;
            } else if (words.get(i) instanceof LeftBracket) {
                isLeft = true;
            }
        }
        if (operator == null) {
            throw new ParseException("no operator found in expression");
        }
        if (operator.getOperatorEnum() == OperatorEnum.PLUS) {
            return Operator.sumDesc(args.toArray());
        } else if (operator.getOperatorEnum() == OperatorEnum.AND) {
            return Operator.andDesc(args.toArray());
        } else if (operator.getOperatorEnum() == OperatorEnum.OR) {
            return Operator.orDesc(args.toArray());
        } else if (operator.getOperatorEnum() == OperatorEnum.MULTIPLICATION) {
            return Operator.multiDesc(args.toArray());
        } else if (operator.getOperatorEnum() == OperatorEnum.DIVISION) {
            return Operator.divisionDesc(args.toArray());
        } else if (operator.getOperatorEnum() == OperatorEnum.MINUS) {
            return Operator.minusDesc(args.toArray());
        } else if (operator.getOperatorEnum() == OperatorEnum.XOR) {
            return Operator.xorDesc(args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.EQUAL_EQUAL){
            return Operator.equalDesc(args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.GREATER_THAN){
            return Operator.greaterDesc(args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.GREATER_THAN_OR_EQUAL){
            return Operator.greaterEqualDesc( args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.LESS_THAN){
            return Operator.lessDesc(args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.LESS_THAN_OR_EQUAL){
            return Operator.lessEqualDesc(args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.IN){
            return Operator.inDesc(args.toArray());
        } else if( operator.getOperatorEnum() == OperatorEnum.AGE){
            return Operator.ageDesc(args.toArray());
        }
        return null;
    }

    /**
     * 根据Range产生StringRange
     * @param value
     * @return
     */
    private static StringRange generateStringRangeFromValue(String value) {
        value = value.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "").replaceAll("\"", "");
        String[] array = value.split(",");
        return new StringRange(array[0], array[1]);
    }

    /**
     * 根据Range值产生DoubleRange
     * @return
     * @param value
     */
    private static DoubleRange generateDoubleRangeFromValue(String value) {
        value = value.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "").replaceAll("\"", "");
        String[] array = value.split(",");
        double start = 0d;
        double end = 0d;
        for(int i = 0; i < array.length; i ++){
            if(StringUtils.isBlank(array[i]) && i > 0){
                end = Double.MAX_VALUE;
            }else if(StringUtils.isBlank(array[i]) && i == 0){
                start = Double.MIN_VALUE;
            }else if(i == 0){
                start = Double.valueOf(array[i].trim());
            }else if(i == 1){
                end = Double.valueOf(array[i].trim());
            }
        }
        return new DoubleRange(start, end);
    }

    /**
     * 合并组标签和子标签的json生成一个合并结果的json
     * @param computationJson
     * @param computationJson1
     * @return
     */
    private static String mergeJson(String computationJson, String computationJson1) {
        JSONArray groupJSONArray = (JSONArray)JSON.parse(computationJson);
        JSONObject childJsonObject = (JSONObject)JSON.parse(computationJson1);
        List<Object> params = new ArrayList<>();
        String connector = "and";
        params.add(connector);
        fillJsonArrayList(groupJSONArray, params);
        params.add(childJsonObject);
        JSONArray jsonArray = new JSONArray(params);
        return jsonArray.toJSONString();
    }

    private static void fillJsonArrayList(JSONArray groupJSONArray, List<Object> params) {
        for(Object object : groupJSONArray){
            if(!(object instanceof String)){
                params.add(object);
            }
        }
    }

    public static void setDataDefinitionTableName(String ddName){
        Constant.TABLE_NAME = ddName;
    }

    public String getComputationRule() {
        return computationRule;
    }

    public void setComputationRule(String computationRule) {
        this.computationRule = computationRule;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getIsRequired() {
        return isRequired;
    }

    public String getSourceData() {
        return sourceData;
    }

    public void setSourceData(String sourceData) {
        this.sourceData = sourceData;
    }

    public void setIsRequired(String isRequired) {
        this.isRequired = isRequired;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getUpdateType() {
        return updateType;
    }

    public void setUpdateType(Integer updateType) {
        this.updateType = updateType;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getLeftData() {
        return isLeftData;
    }

    public void setLeftData(Boolean leftData) {
        isLeftData = leftData;
    }

    public String getLeftColName() {
        return leftColName;
    }

    public void setLeftColName(String leftColName) {
        this.leftColName = leftColName;
    }

    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getUseTemplate() {
        return isUseTemplate;
    }

    public void setUseTemplate(Boolean useTemplate) {
        isUseTemplate = useTemplate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DataDefinitionDataType getDataTypeEnum() {
        return dataTypeEnum;
    }

    public void setDataTypeEnum(DataDefinitionDataType dataTypeEnum) {
        this.dataTypeEnum = dataTypeEnum;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public DefinitionType getDefinitionTypeEnum() {
        return definitionTypeEnum;
    }

    public void setDefinitionTypeEnum(DefinitionType definitionTypeEnum) {
        this.definitionTypeEnum = definitionTypeEnum;
    }

    public int hashCode() {
        return Integer.valueOf(String.valueOf(id));
    }

    public Boolean getEditable() {
        return isEditable;
    }

    public void setEditable(Boolean editable) {
        isEditable = editable;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public boolean equals(Object obj) {
        if (obj instanceof DataDefinition) {
            return ((DataDefinition) obj).getId() == this.getId();
        }
        return false;
    }


    public int getStatus() {
        return status;
    }

    public void initEnum(){
        this.definitionTypeEnum = DefinitionType.getByValue(definitionType);
        this.dataTypeEnum = DataDefinitionDataType.getByValue(dataType);
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
        this.dataTypeEnum = DataDefinitionDataType.getByValue(dataType);
    }

    public Integer getTagType() {
        return tagType;
    }

    public void setTagType(Integer tagType) {
        this.tagType = tagType;
    }

    public String getDefinitionType() {
        return definitionType;
    }

    public void setDefinitionType(String definitionType) {
        this.definitionType = definitionType;
    }

    public DataDefinition getParent() {
        return parent;
    }

    public void setParent(DataDefinition parent) {
        this.parent = parent;
    }

    public String getChildCommentName() {
        return childCommentName;
    }

    public void setChildCommentName(String childCommentName) {
        this.childCommentName = childCommentName;
    }

    public DataDefinition getChildComment() {
        return childComment;
    }

    public void setChildComment(DataDefinition childComment) {
        this.childComment = childComment;
    }

    public Boolean getQuery() {
        return isQuery;
    }

    public void setQuery(Boolean query) {
        isQuery = query;
    }

    public String getValueColumnName(){
        if(dataTypeEnum == DataDefinitionDataType.DOUBLE || dataTypeEnum == DataDefinitionDataType.FLOAT){
            return "double_value";
        }else if(dataTypeEnum == DataDefinitionDataType.TEXT){
            return "feature";
        }else if(dataTypeEnum == DataDefinitionDataType.BOOLEAN || dataTypeEnum == DataDefinitionDataType.INT || dataTypeEnum == DataDefinitionDataType.DATETIME){
            return "int_value";
        }else if(dataTypeEnum == DataDefinitionDataType.STRING){
            return "str_value";
        }else{
            throw new HappyQueryException("DataDefinitionValue ");
        }
    }

    public List<DataOption> getDataOptionList() {
        return dataOptionList;
    }

    public void setDataOptionList(List<DataOption> dataOptionList) {
        this.dataOptionList = dataOptionList;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public String getComputationJson() {
        return computationJson;
    }

    public void setComputationJson(String computationJson) {
        this.computationJson = computationJson;
    }
}
