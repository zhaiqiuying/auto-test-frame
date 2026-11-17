package com.example.qa.btest.db;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.example.qa.btest.util.PropertyUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

@Slf4j
public abstract class AbstractDataSource implements IDataSource {

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private DataSource dataSource;

    public AbstractDataSource(String propertyName) {
        Properties properties = PropertyUtil.loadProperty(propertyName);
        dataSource = getDataSource(properties);
    }

    public AbstractDataSource(Map dataBaseConf) {
        dataSource = getDataSource(dataBaseConf);
    }

    public DataSource getDataSource(Map property) {
        DataSource dataSource = new DruidDataSource();
        try {
            DruidDataSource druidDataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(property);
            druidDataSource.setConnectionErrorRetryAttempts(0);
            druidDataSource.setBreakAfterAcquireFailure(true);
            dataSource = druidDataSource;
        } catch (Exception e) {
            log.error("Failed to create Druid data source", e);
        }
        return dataSource;
    }

    public static Connection getConnection(DataSource dataSource) {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("Failed to get connection from data source", e);
        }
        return null;
    }
}
