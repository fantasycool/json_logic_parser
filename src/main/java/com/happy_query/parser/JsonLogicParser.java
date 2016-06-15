package com.happy_query.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.happy_query.cache.CacheManager;
import com.happy_query.parser.definition.DataDefinition;
import com.happy_query.parser.definition.DataDefinitionDataType;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by frio on 16/6/14.
 */
public class JsonLogicParser implements IJsonLogicParser {
    private String BLANK = " ";
    private String AND = "and";
    private CacheManager cacheManager;

    public JsonLogicParser(CacheManager cacheManager){
        this.cacheManager = cacheManager;
    }

    public String convertJsonToLogicExpression(String json, String prefix, Map<String, String> attributesMap) {
        try {
            Object o = JSON.parse(json);
            if (o instanceof JSONArray) {
                String connector = ((JSONArray) o).get(0).toString();
                return appendOperation(connector, (JSONArray) o);
            } else {
                throw new JsonLogicParseException(String.format("invalid json logic format:%s", o.toString()));
            }
        }catch(Exception e){
            throw new JSONException("parse unexpected exception,please check your json str", e);
        }
    }

    public String appendOperation(String connector, JSONArray jsonArray) {
        StringBuilder sb = new StringBuilder();
        if (jsonArray.get(0) instanceof String) {
            for (int i = 0; i < jsonArray.size(); i++) {
                if (jsonArray.get(i) instanceof String) {
                    continue;
                } else if (jsonArray.get(i) instanceof JSONArray) {
                    sb.append("(");
                    sb.append(appendOperation(((JSONArray) jsonArray.get(i)).getString(0), (JSONArray) jsonArray.get(i)));
                    sb.append(")");
                } else {
                    JSONObject o = (JSONObject) jsonArray.get(i);
                    sb.append("(");
                    Long dd_ref_id = o.getLong("attr");
                    sb.append("dd_ref_id=").append(dd_ref_id).append(BLANK).append(AND).append(BLANK);
                    DataDefinition dataDefinition = null;
                    try {
                        dataDefinition = (DataDefinition) cacheManager.getValue(DataDefinition.createDataDefinitionById(dd_ref_id));
                    } catch (ExecutionException e) {
                        throw new JsonLogicParseException(String.format("cache get failed!dd_ref_id %d", dd_ref_id), e);
                    }
                    if(dataDefinition.getDataType() == DataDefinitionDataType.INT
                            ||dataDefinition.getDataType() == DataDefinitionDataType.BOOLEAN
                            ||dataDefinition.getDataType() == DataDefinitionDataType.DATETIME){
                        sb.append("int_value").append(o.getString("operator")).append(o.getString("value"));
                    }else if(dataDefinition.getDataType() == DataDefinitionDataType.DOUBLE
                            ||dataDefinition.getDataType()==DataDefinitionDataType.FLOAT){
                        sb.append("double_value").append(o.getString("operator")).append(o.getString("value"));
                    }else{
                        sb.append("str_value").append(o.getString("operator")).append(o.getString("value"));
                    }
                    sb.append(")").append(BLANK);
                    if (i < jsonArray.size() - 1) {
                        sb.append(((String) jsonArray.get(0))).append(BLANK);
                    }
                }
            }
            return sb.toString();
        } else {
            throw new JsonLogicParseException(String.format("invalid json logic format:%s", jsonArray.toJSONString()));
        }
    }
}
