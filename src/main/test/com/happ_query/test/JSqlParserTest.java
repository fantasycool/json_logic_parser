package com.happ_query.test;

import com.happy_query.cache.CacheManager;
import com.happy_query.parser.IJsonLogicParser;
import com.happy_query.parser.IJsonSqlParser;
import com.happy_query.parser.JsonLogicParser;
import com.happy_query.parser.JsqlSqlParser;
import com.happy_query.parser.domain.DataDefinition;
import com.happy_query.parser.domain.DataDefinitionDataType;
import com.happy_query.parser.domain.JsonParseDataParam;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Created by frio on 16/6/16.
 */
public class JSqlParserTest {
    @Test
    public void testGenerateQuerySql() throws ExecutionException {
        DataDefinition d1 =  DataDefinition.createDataDefinitionById(1l);
        d1.setDataType(DataDefinitionDataType.BOOLEAN);
        CacheManager.putValue(DataDefinition.createDataDefinitionById(1l), d1);

        DataDefinition d2 =  DataDefinition.createDataDefinitionById(2l);
        d1.setDataType(DataDefinitionDataType.DATETIME);
        CacheManager.putValue(DataDefinition.createDataDefinitionById(2l), d2);

        DataDefinition d3 =  DataDefinition.createDataDefinitionById(3l);
        d1.setDataType(DataDefinitionDataType.STRING);
        CacheManager.putValue(DataDefinition.createDataDefinitionById(3l), d3);

        DataDefinition d4 =  DataDefinition.createDataDefinitionById(4l);
        d1.setDataType(DataDefinitionDataType.INT);
        CacheManager.putValue(DataDefinition.createDataDefinitionById(4l), d4);
        IJsonLogicParser jsonLogicParser = new JsonLogicParser();
        String j = "[\n" +
                "      \"and\",\n" +
                "          {\n" +
                "              \"attr\":\"1\",\n" +
                "              \"operator\":\">\",\n" +
                "              \"value\":\"1\"\n" +
                "          },\n" +
                "          {\n" +
                "              \"attr\":\"2\",\n" +
                "              \"operator\": \"<\",\n" +
                "              \"value\":\"10\"\n" +
                "          },\n" +
                "          [\n" +
                "              \"or\",\n" +
                "                  {\n" +
                "                      \"attr\":\"3\",\n" +
                "                      \"operator\":\"in\",\n" +
                "                      \"value\":[1,2,3,4]\n" +
                "                  },\n" +
                "                  {\n" +
                "                      \"attr\":\"4\",\n" +
                "                      \"operator\":\"=\",\n" +
                "                      \"value\":\"10\"\n" +
                "                  }\n" +
                "          ]\n" +
                "  ]";
        IJsonSqlParser jsqlSqlParser = new JsqlSqlParser(jsonLogicParser);
        JsonParseDataParam jsonParseDataParam = new JsonParseDataParam();
        jsonParseDataParam.setJsonOperation(j);
        jsonParseDataParam.setLeftPrimaryId("id");
        jsonParseDataParam.setLeftTableName("left_table");
        jsonParseDataParam.setLimitStart(0);
        jsonParseDataParam.setSize(30);
        jsonParseDataParam.setRightTableName("data_definition_value");
        System.out.println(jsqlSqlParser.convertJsonLogicToCountSql(jsonParseDataParam));
    }

    /**
     *
     */
    public void mockDataDefinitions(){

    }

    @Test
    public void testGenerateCountSql(){

    }
}
