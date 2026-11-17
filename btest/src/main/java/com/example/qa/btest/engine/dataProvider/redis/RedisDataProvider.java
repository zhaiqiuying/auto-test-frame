package com.example.qa.btest.engine.dataProvider.redis;

import com.example.qa.btest.db.DataSourceFactory;
import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.engine.dataProvider.common.DataProvider;
import com.example.qa.btest.engine.dataProvider.common.MethodContext;
import com.example.qa.btest.util.ListUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 数据驱动 Provider，从 Redis 中读取数据注入测试方法参数。
 * 注解 @Redis 标注的测试方法会通过 CommonDataProvider 注册表分发到此类。
 */
@Slf4j
public class RedisDataProvider implements DataProvider {

    private static final String KEY_REDIS_HOST = "redis_host";
    private static final String KEY_REDIS_PORT = "redis_port";
    private static final String KEY_REDIS_PASSWORD = "redis_password";
    private static final String KEY_URL = "url";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_DATABASE = "database";
    private static final int DEFAULT_REDIS_PORT = 6379;

    @Override
    public Class<?> getDataType() {
        return Redis.class;
    }

    @Override
    public Object[][] getObjects(MethodContext methodContext) {
        if (methodContext == null) {
            return new Object[0][0];
        }
        Annotation annotation = methodContext.getAnnotation();
        if (annotation instanceof Redis) {
            Redis redis = (Redis) annotation;

            Map<String, Object> confMap = buildConfMap(redis);
            if (confMap == null) {
                return new Object[0][0];
            }

            // 构建命令字符串
            String command = buildCommand(redis);
            if (StringUtils.isEmpty(command)) {
                log.warn("Redis command is empty, skip data provider");
                return new Object[0][0];
            }

            log.debug("Redis conf: {}, command: {}", confMap, command);
            return getObjects(confMap, command);
        }
        return new Object[0][0];
    }

    /**
     * 根据注解属性构建配置 Map。
     * @return 配置 Map，若 host 或 url 未配置则返回 null
     */
    private Map<String, Object> buildConfMap(Redis redis) {
        Map<String, Object> confMap = new HashMap<>();

        // 注解显式值优先，suite 参数兜底
        String host = StringUtils.isNotEmpty(redis.host()) ? redis.host() : null;
        int port = redis.port() != DEFAULT_REDIS_PORT ? redis.port() : DEFAULT_REDIS_PORT;
        String password = StringUtils.isNotEmpty(redis.password()) ? redis.password() : null;

        // 支持 url 方式配置（redis://host:port/db）
        if (StringUtils.isNotEmpty(redis.url())) {
            confMap.put(KEY_URL, redis.url());
        } else if (StringUtils.isNotEmpty(host)) {
            confMap.put(KEY_HOST, host);
            confMap.put(KEY_PORT, String.valueOf(port));
            if (StringUtils.isNotEmpty(password)) {
                confMap.put(KEY_PASSWORD, password);
            }
            confMap.put(KEY_DATABASE, String.valueOf(redis.database()));
        } else {
            log.warn("Redis host or url not configured, skip data provider");
            return null;
        }
        return confMap;
    }

    /**
     * 根据注解属性构建 Redis 命令字符串。
     */
    private String buildCommand(Redis redis) {
        String cmd = redis.command();
        if (StringUtils.isEmpty(cmd)) {
            return "";
        }

        StringBuilder sb = new StringBuilder(cmd.toUpperCase());
        if (StringUtils.isNotEmpty(redis.key())) {
            sb.append(" ").append(redis.key());
        }
        if (StringUtils.isNotEmpty(redis.field())) {
            sb.append(" ").append(redis.field());
        }
        if (StringUtils.isNotEmpty(redis.value())) {
            sb.append(" ").append(redis.value());
        }
        return sb.toString();
    }

    private static Object[][] getObjects(Map<String, Object> confMap, String command) {
        IDataSource dataSource = DataSourceFactory.createDataSource(confMap);
        if (dataSource != null) {
            IDBConnection connection = dataSource.getConnection();
            try {
                List<?> list = (List<?>) connection.execute(command);
                log.info("get {} data from redis", list.size());
                return ListUtil.listToArray(list);
            } catch (Exception e) {
                log.error("Redis data provider error", e);
            } finally {
                connection.close();
            }
        }
        return new Object[0][0];
    }
}
