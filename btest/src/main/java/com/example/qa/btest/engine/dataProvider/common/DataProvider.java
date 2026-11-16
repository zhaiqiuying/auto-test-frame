package com.example.qa.btest.engine.dataProvider.common;


public interface DataProvider extends com.github.superproxy.testdataprovider.DataProvider {
    Object[][] getObjects(MethodContext var1);

    default Object[][] getObjects(com.github.superproxy.testdataprovider.MethodContext var1) {
        return null;
    }
}
