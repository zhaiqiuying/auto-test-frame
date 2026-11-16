package com.example.qa.btest.db;

import com.example.qa.btest.db.mysql.MysqlDataSource;
import com.example.qa.btest.db.redis.RedisDataSource;
import com.github.fppt.jedismock.RedisServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * DataSourceFactory 工厂类型判别扩展测试。
 * 验证 Redis URL 和 MySQL URL 的类型判别，以及缓存逻辑。
 */
public class DataSourceFactoryRedisTest {

    private static final int REDIS_PORT = 6381;
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

    @Test
    public void testRedisUrlReturnsRedisDataSource() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("url", "redis://127.0.0.1:" + REDIS_PORT + "/0");

        IDataSource dataSource = DataSourceFactory.createDataSource(conf);
        assertNotNull(dataSource, "Redis URL 应返回非 null 数据源");
        assertTrue(dataSource instanceof RedisDataSource,
                "redis:// URL 应返回 RedisDataSource 实例");
    }

    @Test
    public void testMysqlUrlReturnsMysqlDataSource() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("url", "jdbc:mysql://127.0.0.1:3306/test");
        conf.put("user", "root");
        conf.put("password", "root");

        IDataSource dataSource = DataSourceFactory.createDataSource(conf);
        assertNotNull(dataSource, "MySQL URL 应返回非 null 数据源");
        assertTrue(dataSource instanceof MysqlDataSource,
                "jdbc:mysql:// URL 应返回 MysqlDataSource 实例");
    }

    @Test
    public void testSameRedisConfigReturnsCachedInstance() {
        Map<String, Object> conf1 = new HashMap<>();
        conf1.put("url", "redis://127.0.0.1:" + REDIS_PORT + "/0");

        Map<String, Object> conf2 = new HashMap<>();
        conf2.put("url", "redis://127.0.0.1:" + REDIS_PORT + "/0");

        // getDataSource 使用 MD5(conf.toString()) 做缓存 key
        IDataSource ds1 = DataSourceFactory.getDataSource(conf1);
        IDataSource ds2 = DataSourceFactory.getDataSource(conf2);

        assertNotNull(ds1, "第一次 getDataSource 应返回非 null");
        assertNotNull(ds2, "第二次 getDataSource 应返回非 null");
        assertSame(ds1, ds2, "相同 Redis 配置的 Map 应返回缓存的同一实例（MD5 key 一致）");
    }

    @Test
    public void testUnmatchedUrlReturnsNull() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("url", "mongodb://127.0.0.1:27017/test");

        IDataSource dataSource = DataSourceFactory.createDataSource(conf);
        assertNull(dataSource, "未匹配的 URL 类型应返回 null");
    }
}
