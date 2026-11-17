package com.example.qa.itest;

import com.example.qa.btest.engine.dataProvider.redis.Redis;
import com.example.qa.btest.engine.dataProvider.common.MethodContext;
import com.example.qa.btest.engine.dataProvider.redis.RedisDataProvider;
import com.github.fppt.jedismock.RedisServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * @Redis 注解驱动的数据注入测试。
 *
 * 验证 RedisDataProvider 能正确解析 @Redis 注解、连接 Redis、
 * 执行命令并将结果转换为 Object[][] 注入测试方法参数。
 */
public class RedisDataProviderTest {

    private static final int REDIS_PORT = 6382;
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

    /**
     * 从当前类中获取标注了 @Redis 的方法，构造 MethodContext。
     */
    private MethodContext buildContext(String methodName) throws NoSuchMethodException {
        Method method = RedisDataProviderTest.class.getMethod(methodName);
        Redis redis = method.getAnnotation(Redis.class);
        MethodContext ctx = new MethodContext();
        ctx.setAnnotation(redis);
        ctx.setMethod(method);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("redis_host", "127.0.0.1");
        valueMap.put("redis_port", String.valueOf(REDIS_PORT));
        ctx.setValueMap(valueMap);
        return ctx;
    }

    private Object[][] getData(String methodName) throws NoSuchMethodException {
        RedisDataProvider provider = new RedisDataProvider();
        return provider.getObjects(buildContext(methodName));
    }

    // ==================== 测试方法 ====================

    /**
     * SET 命令应通过 @Redis 注解执行并返回非空 Object[][]。
     */
    @Test
    public void testSetReturnsNonEmptyData() throws NoSuchMethodException {
        Object[][] data = getData("annotatedSetMethod");
        assertNotNull(data, "SET 应返回非 null Object[][]");
        assertTrue(data.length > 0, "SET 应返回非空数据");
        assertEquals(data[0][0], "OK", "SET 应返回状态 OK");
    }

    /**
     * SET 后 GET 注入的参数值应正确。
     */
    @Test
    public void testGetReturnsCorrectValue() throws NoSuchMethodException {
        // 先 SET
        getData("annotatedSetGetMethod");

        // 再 GET
        Object[][] data = getData("annotatedGetMethod");
        assertTrue(data.length > 0, "GET 应返回非空 Object[][]");
        assertEquals(data[0][0], "dp_get_value", "GET 注入的参数值应为 dp_get_value");
    }

    /**
     * HGETALL 多行结果正确拆分为多组参数。
     */
    @Test
    public void testHgetAllMultiRowData() throws NoSuchMethodException {
        // 先 HSET 两个字段
        getData("annotatedHsetMethod1");
        getData("annotatedHsetMethod2");

        // HGETALL 获取多行数据
        Object[][] data = getData("annotatedHgetAllMethod");
        assertEquals(data.length, 2, "HGETALL 应返回 2 行数据（两个字段）");

        // 每行应为 2 列（field, value）
        for (Object[] row : data) {
            assertEquals(row.length, 2, "HGETALL 每行应为 2 列（field, value）");
        }
    }

    // ==================== 辅助方法（仅用于承载 @Redis 注解） ====================

    @Redis(command = "SET", key = "dp_key", value = "dp_value", host = "127.0.0.1", port = REDIS_PORT)
    public void annotatedSetMethod() {}

    @Redis(command = "SET", key = "dp_get_key", value = "dp_get_value", host = "127.0.0.1", port = REDIS_PORT)
    public void annotatedSetGetMethod() {}

    @Redis(command = "GET", key = "dp_get_key", host = "127.0.0.1", port = REDIS_PORT)
    public void annotatedGetMethod() {}

    @Redis(command = "HSET", key = "dp_hash", field = "f1", value = "v1", host = "127.0.0.1", port = REDIS_PORT)
    public void annotatedHsetMethod1() {}

    @Redis(command = "HSET", key = "dp_hash", field = "f2", value = "v2", host = "127.0.0.1", port = REDIS_PORT)
    public void annotatedHsetMethod2() {}

    @Redis(command = "HGETALL", key = "dp_hash", host = "127.0.0.1", port = REDIS_PORT)
    public void annotatedHgetAllMethod() {}
}
