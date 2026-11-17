package com.example.qa.btest.engine.dataProvider.common;

import java.lang.reflect.Method;


public interface DataProvider extends com.github.superproxy.testdataprovider.DataProvider {
    @Override
    Class<?> getDataType();

    Object[][] getObjects(MethodContext var1);

    default Object[][] getObjects(com.github.superproxy.testdataprovider.MethodContext var1) {
        return null;
    }
}
