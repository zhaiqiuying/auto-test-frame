package com.example.qa.itest.model;


import com.example.qa.btest.db.IDataSource;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class CommonContext {

    private static final int INITIAL_CAPACITY_LARGE = 100;
    private static final int INITIAL_CAPACITY_SMALL = 10;

    private Map<String, User> userMap = new ConcurrentHashMap<>(INITIAL_CAPACITY_LARGE);
    private Map<String, User.Device> deviceMap = new ConcurrentHashMap<>(INITIAL_CAPACITY_LARGE);
    private Map<String, User.App> appMap = new ConcurrentHashMap<>(INITIAL_CAPACITY_SMALL);
    private Map<String, User.Env> envMap = new ConcurrentHashMap<>(INITIAL_CAPACITY_SMALL);
    private Map<String, IDataSource> mysqlDataSourceMap = new ConcurrentHashMap<>(INITIAL_CAPACITY_LARGE);
    private Map<String, IDataSource> redisDataSourceMap = new ConcurrentHashMap<>(INITIAL_CAPACITY_LARGE);

}
