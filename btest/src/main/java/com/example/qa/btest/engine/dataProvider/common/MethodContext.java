package com.example.qa.btest.engine.dataProvider.common;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MethodContext extends com.github.superproxy.testdataprovider.MethodContext {

    private Map<String, Object> valueMap = new HashMap<>();

    public static MethodContext build(MethodContext methodContext) {
        MethodContext mysqlMethodContext = new MethodContext();
        mysqlMethodContext.setAnnotation(methodContext.getAnnotation());
        return mysqlMethodContext;
    }
}
