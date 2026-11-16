package com.example.qa.btest.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractDbConnection implements IDBConnection {

    private static final Logger log = LoggerFactory.getLogger(AbstractDbConnection.class);

    private Connection connection;

    public AbstractDbConnection(Connection conn) {
        this.connection = conn;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Object execute(String sql, Execute execute) throws SQLException {
        try (Statement stmt = this.connection.createStatement()) {
            ResultSet rs = null;
            try {
                rs = stmt.executeQuery(sql);
                return execute.run(rs);
            } finally {
                close(rs);
            }
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing Connection", e);
        }
    }

    public static void close(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            log.warn("Error closing ResultSet", e);
        }
    }
}
