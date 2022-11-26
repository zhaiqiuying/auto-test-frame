package com.yuanfudao.aries.qa.btest.db;

import java.sql.SQLException;

public interface IDBConnection {

    Object execute(String sql, Execute execute) throws SQLException;

    Object execute(String sql) throws SQLException;

    void close();
}
