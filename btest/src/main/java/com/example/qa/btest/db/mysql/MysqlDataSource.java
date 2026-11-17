package com.example.qa.btest.db.mysql;

import com.example.qa.btest.db.AbstractDataSource;
import com.example.qa.btest.db.AbstractDbConnection;
import com.example.qa.btest.db.Execute;
import com.example.qa.btest.db.IDBConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class MysqlDataSource extends AbstractDataSource {

    public MysqlDataSource(String propertyName) {
        super(propertyName);
    }

    public MysqlDataSource(Map dataBaseConf) {
        super(dataBaseConf);
    }

    public IDBConnection getConnection() {
        Connection connection = getConnection(getDataSource());
        return new MysqlConnection(connection);
    }

    public static class MysqlConnection extends AbstractDbConnection {

        public MysqlConnection(Connection connection) {
            super(connection);
        }

        public List execute(String sql) throws SQLException {
            Execute execute = (a) -> {
               ResultSet rs = (ResultSet) a[0];
               List result = new ArrayList();
                try {
                    while(rs.next()) {
                        List<Object> row = new ArrayList<>();
                        int colNum = rs.getMetaData().getColumnCount();
                        for(int i = 1; i <= colNum; i++) {
                            row.add(rs.getObject(i));
                        }
                        result.add(row);
                    }

                } catch (SQLException e) {
                    log.error("Error reading ResultSet", e);
                }
                return result;
            };
            try {
                return (List)execute(sql, execute);
            } catch (SQLException e) {
                log.error("execute sql error: {}", sql, e);
            }
            return Collections.emptyList();
        }

        @Override
        public Object executeWithoutException(String sql) {
            try {
                return execute(sql);
            } catch (SQLException e) {
                log.error("executeWithoutException error, sql:" + sql, e);
            }
            return new ArrayList[0];
        }

        @Override
        public Object executeWithClose(String sql) {
            Object result = executeWithoutException(sql);
            this.close();
            return result;
        }
    }
}
