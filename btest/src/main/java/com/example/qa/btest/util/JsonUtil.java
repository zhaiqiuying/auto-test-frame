package com.example.qa.btest.util;

import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.testng.Assert;
import org.testng.Reporter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JsonUtil {
    public static boolean validateJsonWithSchema(String schema, String jsonStr) {
        JSONObject jsonObject = (JSONObject) JSONObject.stringToValue(jsonStr);
        return validateJsonWithSchema(schema, jsonObject);
    }
    public static boolean validateJsonWithSchema(String schema, Object jsonObject) {

        if (StringUtil.isEmpty(schema) || jsonObject == null) {
            Assert.fail("validateJsonWithSchema error, schema | jsonObject invalid");
            return false;
        }
        Schema jsonSchema;
        try {
            jsonSchema = SchemaLoader.load(new JSONObject(new JSONTokener(schema)));
        } catch (JSONException e) {
            log.error("validateJsonWithSchema fail", e);
            Assert.fail("validateJsonWithSchema fail, invalid jsonschema");
            return false;
        }

        try {
            jsonSchema.validate(jsonObject);
        } catch (ValidationException e) {
            Reporter.log("validateJsonWithSchema fail:" + getReason(e), 0, true);
            return false;
        }
        return true;
    }

    private static String getReason(ValidationException exception) {
        if (exception == null) {
            return "";
        }
        StringBuilder reasonBuilder = new StringBuilder();
        List<ValidationException> exceptions = new ArrayList<>();
        exceptions.add(exception);
        int len = exceptions.size();
        int i = 0;
        while (i < len) {
            ValidationException e = exceptions.get(i);
            if (e.getCausingExceptions() == null) {
                reasonBuilder.append("\n").append(e.getMessage());
                i += 1;
            } else {
                exceptions.addAll(e.getCausingExceptions());
                i += 1;
                len = exceptions.size();
            }
        }
        return reasonBuilder.toString();
    }
}

