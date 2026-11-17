package com.example.qa.itest.model;


import com.example.qa.btest.db.IDataSource;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class CommonContext {

    private Map<String, User> userMap = new ConcurrentHashMap<>(100);
    private Map<String, User.Device> deviceMap = new ConcurrentHashMap<>(100);
    private Map<String, User.App> appMap = new ConcurrentHashMap<>(10);
    private Map<String, User.Env> envMap = new ConcurrentHashMap<>(10);
    private Map<String, IDataSource> mysqlDataSourceMap = new ConcurrentHashMap<>(100);
    private Map<String, IDataSource> redisDataSourceMap = new ConcurrentHashMap<>(100);

}
