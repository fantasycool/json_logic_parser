package com.happ_query.test;

import com.happy_query.cache.CacheManager;
import com.happy_query.parser.IJsonLogicParser;
import com.happy_query.parser.JsonLogicParser;
import com.happy_query.parser.definition.DataDefinition;
import com.happy_query.parser.definition.DataDefinitionDataType;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

/**
 * Created by frio on 16/6/15.
 */
public class JsonParserTest {
    @Test
    public void testMethod() throws ExecutionException {
        CacheManager cacheManager = new CacheManager(null);
        DataDefinition d1 =  DataDefinition.createDataDefinitionById(1l);
        d1.setDataType(DataDefinitionDataType.BOOLEAN);
        cacheManager.putValue(DataDefinition.createDataDefinitionById(1l), d1);

        DataDefinition d2 =  DataDefinition.createDataDefinitionById(2l);
        d1.setDataType(DataDefinitionDataType.DATETIME);
        cacheManager.putValue(DataDefinition.createDataDefinitionById(2l), d2);

        DataDefinition d3 =  DataDefinition.createDataDefinitionById(3l);
        d1.setDataType(DataDefinitionDataType.INT);
        cacheManager.putValue(DataDefinition.createDataDefinitionById(3l), d3);

        DataDefinition d4 =  DataDefinition.createDataDefinitionById(4l);
        d1.setDataType(DataDefinitionDataType.STRING);
        cacheManager.putValue(DataDefinition.createDataDefinitionById(4l), d4);
        IJsonLogicParser jsonLogicParser = new JsonLogicParser(cacheManager);
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
                "                      \"value\":\"(1,2,3,4)\"\n" +
                "                  },\n" +
                "                  {\n" +
                "                      \"attr\":\"4\",\n" +
                "                      \"operator\":\"=\",\n" +
                "                      \"value\":\"10\"\n" +
                "                  }\n" +
                "          ]\n" +
                "  ]";
        System.out.println(j);
        System.out.println(jsonLogicParser.convertJsonToLogicExpression(j, null, null));
    }
}
