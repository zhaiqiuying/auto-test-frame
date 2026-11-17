package com.example.qa.btest.db;

import com.example.qa.btest.db.mysql.MysqlDataSource;
import com.example.qa.btest.db.redis.RedisDataSource;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取数据源，建立链接，并执行sql
 */
public class DataSourceFactory {

    private static final String TYPE_REDIS = "redis";
    private static final String TYPE_MYSQL = "mysql";
    private static final String KEY_URL = "url";
    private static final String KEY_HOST = "host";
    private static final String KEY_USER = "user";

    private static final Map<String, IDataSource> dataSourceMap = new ConcurrentHashMap<>();

    public static IDataSource getDataSource(String dataSourceType) {
        return dataSourceMap.computeIfAbsent(dataSourceType,
                key -> createDataSource(dataSourceType)
        );
    }

    public static IDataSource getDataSource(Map<String, Object> dataBaseConf) {
        return dataSourceMap.computeIfAbsent(DigestUtils.md5Hex(dataBaseConf.toString()),
                key -> createDataSource(dataBaseConf)
        );
    }

    private static IDataSource createDataSource(String dataSourceType) {
        IDataSource dataSource = null;
        if (dataSourceType.contains(TYPE_MYSQL)) {
            dataSource = new MysqlDataSource(dataSourceType);
        }
        return dataSource;
    }

    public static IDataSource createDataSource(Map<String, Object> dataBaseConf) {
        IDataSource dataSource = null;
        String url = (String) dataBaseConf.getOrDefault(KEY_URL, "");
        if (url.contains(TYPE_REDIS)) {
            dataSource = new RedisDataSource(dataBaseConf);
        } else if (url.contains(TYPE_MYSQL)) {
            dataSource = new MysqlDataSource(dataBaseConf);
        } else if (url.isEmpty() && dataBaseConf.containsKey(KEY_HOST) && !dataBaseConf.containsKey(KEY_USER)) {
            // 无 url 但有 host 且无 MySQL 特有的 user 字段，按 Redis 处理
            dataSource = new RedisDataSource(dataBaseConf);
        }
        return dataSource;
    }
}
