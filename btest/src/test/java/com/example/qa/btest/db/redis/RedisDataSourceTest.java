package com.example.qa.btest.db.redis;

import com.github.fppt.jedismock.RedisServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.example.qa.btest.db.IDBConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Redis 数据源单元测试，使用 jedis-mock 作为测试基础设施。
 * 覆盖连接池管理、命令执行与结果转换、异常处理各场景。
 *
 * jedis-mock 是纯 Java 实现的 Redis 协议服务器，跨平台无需原生二进制。
 */
public class RedisDataSourceTest {

    private static final int REDIS_PORT = 6380;
    private static RedisServer redisServer;

    @BeforeClass
    public void setUp() throws IOException {
        redisServer = RedisServer.newRedisServer(REDIS_PORT).start();
    }

    @AfterClass
    public void tearDown() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    private Map<String, Object> buildConf() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("host", "127.0.0.1");
        conf.put("port", String.valueOf(REDIS_PORT));
        conf.put("database", "0");
        return conf;
    }

    // ==================== 6.1 连接池管理测试 ====================

    @Test
    public void testCreateRedisDataSource() {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        assertNotNull(dataSource, "RedisDataSource 实例应创建成功");
    }

    @Test
    public void testGetConnectionReturnsNonNull() {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection connection = dataSource.getConnection();
        assertNotNull(connection, "getConnection() 应返回非 null 的 IDBConnection");
        connection.close();
    }

    @Test
    public void testSameConfigReturnsCachedInstance() {
        Map<String, Object> conf1 = buildConf();
        Map<String, Object> conf2 = buildConf();
        // DataSourceFactory 通过 MD5(conf.toString()) 缓存，相同配置返回同一实例
        com.example.qa.btest.db.IDataSource ds1 = com.example.qa.btest.db.DataSourceFactory.getDataSource(conf1);
        com.example.qa.btest.db.IDataSource ds2 = com.example.qa.btest.db.DataSourceFactory.getDataSource(conf2);
        assertSame(ds1, ds2, "相同配置应返回缓存的同一实例");
    }

    // ==================== 6.2 命令执行与结果转换测试 ====================

    @Test
    public void testSetAndGetCommand() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();
        try {
            // SET key value → 返回单行单列 "OK"
            List<?> setResult = (List<?>) conn.execute("SET test_key test_value");
            assertEquals(setResult.size(), 1, "SET 应返回 1 行");
            List<?> setRow = (List<?>) setResult.get(0);
            assertEquals(setRow.size(), 1, "SET 应返回单列");
            assertEquals(setRow.get(0), "OK", "SET 应返回状态 OK");

            // GET key → 返回单行单列 value
            List<?> getResult = (List<?>) conn.execute("GET test_key");
            assertEquals(getResult.size(), 1, "GET 应返回 1 行");
            List<?> getRow = (List<?>) getResult.get(0);
            assertEquals(getRow.size(), 1, "GET 应返回单列");
            assertEquals(getRow.get(0), "test_value", "GET 应返回设置的值");
        } finally {
            conn.close();
        }
    }

    @Test
    public void testHsetAndHgetAllCommand() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();
        try {
            // HSET hash field1 value1
            conn.execute("HSET test_hash f1 v1");
            // HSET hash field2 value2
            conn.execute("HSET test_hash f2 v2");

            // HGETALL → 每对 field-value 为一行（两列）
            List<?> result = (List<?>) conn.execute("HGETALL test_hash");
            assertEquals(result.size(), 2, "HGETALL 应返回 2 行（两个字段）");

            for (Object rowObj : result) {
                List<?> row = (List<?>) rowObj;
                assertEquals(row.size(), 2, "HGETALL 每行应为 2 列（field, value）");
                String field = (String) row.get(0);
                String value = (String) row.get(1);
                if ("f1".equals(field)) {
                    assertEquals(value, "v1", "f1 的值应为 v1");
                } else if ("f2".equals(field)) {
                    assertEquals(value, "v2", "f2 的值应为 v2");
                } else {
                    fail("未知的 field: " + field);
                }
            }
        } finally {
            conn.close();
        }
    }

    @Test
    public void testDelCommand() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();
        try {
            // SET 两个 key
            conn.execute("SET del_key1 val1");
            conn.execute("SET del_key2 val2");

            // DEL key1 key2 → 返回删除数量
            List<?> result = (List<?>) conn.execute("DEL del_key1 del_key2");
            assertEquals(result.size(), 1, "DEL 应返回 1 行");
            List<?> row = (List<?>) result.get(0);
            assertEquals(row.size(), 1, "DEL 应返回单列");
            assertEquals(row.get(0), 2L, "DEL 应返回删除数量 2");
        } finally {
            conn.close();
        }
    }

    @Test
    public void testExpireCommand() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();
        try {
            conn.execute("SET expire_key val");
            List<?> result = (List<?>) conn.execute("EXPIRE expire_key 60");
            assertEquals(result.size(), 1, "EXPIRE 应返回 1 行");
            List<?> row = (List<?>) result.get(0);
            assertEquals(row.size(), 1, "EXPIRE 应返回单列");
            assertEquals(row.get(0), 1L, "EXPIRE 应返回 1（成功设置过期时间）");
        } finally {
            conn.close();
        }
    }

    @Test
    public void testLpushAndLrangeCommand() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();
        try {
            // LPUSH list a b c → 返回列表长度
            List<?> pushResult = (List<?>) conn.execute("LPUSH test_list a b c");
            assertEquals(pushResult.size(), 1, "LPUSH 应返回 1 行");
            List<?> pushRow = (List<?>) pushResult.get(0);
            assertEquals(pushRow.size(), 1, "LPUSH 应返回单列");
            assertEquals(pushRow.get(0), 3L, "LPUSH 应返回列表长度 3");

            // LRANGE list 0 -1 → 返回所有元素，每元素一行
            List<?> rangeResult = (List<?>) conn.execute("LRANGE test_list 0 -1");
            assertEquals(rangeResult.size(), 3, "LRANGE 应返回 3 行（3 个元素）");
            for (Object rowObj : rangeResult) {
                List<?> row = (List<?>) rowObj;
                assertEquals(row.size(), 1, "LRANGE 每行应为单列");
            }
        } finally {
            conn.close();
        }
    }

    // ==================== 6.3 异常处理测试 ====================

    @Test
    public void testExecuteWithoutExceptionReturnsEmptyList() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();
        conn.close();

        // 连接关闭后执行命令应返回空 List 而非抛出异常
        Object result = conn.executeWithoutException("GET nonexistent_key");
        assertNotNull(result, "executeWithoutException 不应返回 null");
        assertTrue(result instanceof List, "executeWithoutException 应返回 List 类型");
        assertTrue(((List<?>) result).isEmpty(), "异常/关闭后应返回空 List");
    }

    @Test
    public void testExecuteWithCloseAutoReturnsConnection() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn = dataSource.getConnection();

        // executeWithClose 执行后应自动关闭连接
        Object result = conn.executeWithClose("SET close_key close_val");
        assertNotNull(result, "executeWithClose 不应返回 null");

        // 再次 getConnection 应能正常使用（连接池归还后可获取新连接）
        IDBConnection newConn = dataSource.getConnection();
        try {
            List<?> getResult = (List<?>) newConn.execute("GET close_key");
            assertEquals(getResult.size(), 1, "executeWithClose 后应能重新获取连接");
            List<?> row = (List<?>) getResult.get(0);
            assertEquals(row.get(0), "close_val", "之前设置的值应仍存在");
        } finally {
            newConn.close();
        }
    }

    @Test
    public void testCloseAndReacquireConnection() throws Exception {
        RedisDataSource dataSource = new RedisDataSource(buildConf());
        IDBConnection conn1 = dataSource.getConnection();
        conn1.close();

        // close 后连接池应仍可获取新连接
        IDBConnection conn2 = dataSource.getConnection();
        assertNotNull(conn2, "close 后应能获取新连接");
        try {
            List<?> result = (List<?>) conn2.execute("SET reacquire_key reacquire_val");
            assertNotNull(result, "close 后通过新连接执行命令应成功");
        } finally {
            conn2.close();
        }
    }
}
