package com.example.qa.btest.util;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;

public class JsonPathUtil {

    public static <T> T read(String json, String jsonPath, Predicate... filters) {
        try {
            return JsonPath.read(json, jsonPath, filters);
        } catch (Exception e) {
            return null;
        }
    }
}
