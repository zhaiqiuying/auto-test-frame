package com.example.qa.btest.db;

import com.example.qa.btest.db.mysql.MysqlDataSource;
import org.apache.commons.codec.digest.DigestUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取数据源，建立链接，并执行sql
 */
public class DataSourceFactory {

    public static Map<String, IDataSource> dataSourceMap = new ConcurrentHashMap<>();

    public static IDataSource getDataSource(String dataSourceType) {
        IDataSource dataSource = dataSourceMap.computeIfAbsent(dataSourceType,
                key -> createDataSource(dataSourceType)
        );
        return dataSource;
    }

    public static IDataSource getDataSource(Map dataBaseConf) {
        IDataSource dataSource = dataSourceMap.computeIfAbsent(DigestUtils.md5Hex(dataBaseConf.toString()),
                key -> createDataSource(dataBaseConf)
        );
        return dataSource;
    }

    private static IDataSource createDataSource(String dataSourceType) {
        IDataSource dataSource = null;
        if (dataSourceType.contains("mysql")) {
            dataSource = new MysqlDataSource(dataSourceType);
        }
        return dataSource;
    }

    public static IDataSource createDataSource(Map dataBaseConf) {
        IDataSource dataSource = null;
        String url = (String) dataBaseConf.getOrDefault("url", "");
        if (url.contains("mysql")) {
            dataSource = new MysqlDataSource(dataBaseConf);
        }
        return dataSource;
    }

    public static void main(String[] args) {
        IDataSource dataSource = getDataSource("mysql.properties");
        if (dataSource != null) {
            IDBConnection connection = dataSource.getConnection();
            try {
                List list = (List) connection.execute("select * from sys_user");
                System.out.println(list.size());
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        }
    }
}
