package com.example.qa.itest;

import com.example.qa.btest.db.DataSourceFactory;
import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.util.YamlUtil;
import com.example.qa.itest.model.CommonContext;
import com.github.fppt.jedismock.RedisServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * context.yml 配置式 tag 接入测试。
 *
 * 验证：配置 redises 段后 CommonContext.redisDataSourceMap 包含对应 tag、
 * getRedisConnection(tag) 返回非 null 连接、通过 tag 连接执行 GET/SET 命令结果正确。
 *
 * 此测试直接模拟 Config.loadContext 中 redises 段的加载逻辑，
 * 不依赖 Spring Context（itest 层 Spring 基础设施不完整）。
 */
public class RedisTagConnectionTest {

    private static final int REDIS_PORT = 6383;
    private static RedisServer redisServer;
    private static CommonContext commonContext;

    @BeforeClass
    public void setUp() throws IOException {
        redisServer = RedisServer.newRedisServer(REDIS_PORT).start();

        // 模拟 Config.loadContext 中 redises 段加载逻辑
        commonContext = new CommonContext();
        String contextFile = this.getClass().getResource("/test/data/common/context.yml").getPath();
        Map map = YamlUtil.load(contextFile);
        if (map != null && map.containsKey("redises")) {
            ((List) map.get("redises")).forEach(item -> {
                IDataSource dataSource = DataSourceFactory.getDataSource((Map) item);
                commonContext.getRedisDataSourceMap()
                        .put((String) ((Map<?, ?>) item).get("tag"), dataSource);
            });
        }
    }

    @AfterClass
    public void tearDown() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    /**
     * redisDataSourceMap 应包含 context.yml 中配置的 tag。
     */
    @Test
    public void testRedisDataSourceMapContainsTag() {
        assertNotNull(commonContext.getRedisDataSourceMap(), "redisDataSourceMap 不应为 null");
        assertTrue(commonContext.getRedisDataSourceMap().containsKey("cache_db"),
                "redisDataSourceMap 应包含 tag 'cache_db'");
    }

    /**
     * 通过 tag 获取的数据源应返回非 null 连接。
     */
    @Test
    public void testGetRedisConnectionReturnsNonNull() {
        IDataSource dataSource = commonContext.getRedisDataSourceMap().get("cache_db");
        assertNotNull(dataSource, "tag 'cache_db' 的数据源不应为 null");

        IDBConnection connection = dataSource.getConnection();
        assertNotNull(connection, "getRedisConnection('cache_db') 应返回非 null 连接");
        connection.close();
    }

    /**
     * 通过 tag 连接执行 SET/GET 命令结果正确。
     */
    @Test
    public void testSetAndGetViaTagConnection() throws Exception {
        IDataSource dataSource = commonContext.getRedisDataSourceMap().get("cache_db");
        IDBConnection conn = dataSource.getConnection();
        try {
            // SET
            List setResult = (List) conn.execute("SET tag_key tag_value");
            assertEquals(setResult.size(), 1, "SET 应返回 1 行");
            List setRow = (List) setResult.get(0);
            assertEquals(setRow.get(0), "OK", "SET 应返回 OK");

            // GET
            List getResult = (List) conn.execute("GET tag_key");
            assertEquals(getResult.size(), 1, "GET 应返回 1 行");
            List getRow = (List) getResult.get(0);
            assertEquals(getRow.get(0), "tag_value", "GET 应返回设置的值");
        } finally {
            conn.close();
        }
    }
}
