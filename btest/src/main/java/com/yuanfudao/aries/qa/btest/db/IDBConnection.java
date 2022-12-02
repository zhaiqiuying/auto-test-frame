package com.yuanfudao.aries.qa.btest.db;

import java.sql.SQLException;

public interface IDBConnection {

    Object execute(String sql, Execute execute) throws SQLException;

    Object execute(String sql) throws SQLException;

    /**
     * 不抛出异常，返回空
     * @param sql
     * @return
     */
    Object executeWithoutException(String sql);

    /**
     * 不抛出异常，返回空；执行完毕后关闭connection链接
     * @return
     */
    Object executeWithClose(String sql);

    void close();
}
