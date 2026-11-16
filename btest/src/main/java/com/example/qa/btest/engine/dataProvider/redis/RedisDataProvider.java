package com.example.qa.btest.engine.dataProvider.redis;

import com.example.qa.btest.db.DataSourceFactory;
import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import com.example.qa.btest.engine.dataProvider.common.DataProvider;
import com.example.qa.btest.engine.dataProvider.common.MethodContext;
import com.example.qa.btest.util.ListUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 数据驱动 Provider，从 Redis 中读取数据注入测试方法参数。
 * 注解 @Redis 标注的测试方法会通过 CommonDataProvider 注册表分发到此类。
 */
public class RedisDataProvider implements DataProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDataProvider.class);

    @Override
    public Class getDataType() {
        return Redis.class;
    }

    @Override
    public Object[][] getObjects(MethodContext methodContext) {
        Annotation annotation = methodContext.getAnnotation();
        if (annotation instanceof Redis) {
            Redis redis = (Redis) annotation;

            // 构建配置 Map
            Map<String, Object> confMap = new HashMap<>();

            // suite 参数兜底（CommonDataProvider 注入到 valueMap）
            Map<String, Object> valueMap = methodContext.getValueMap();
            String suiteHost = valueMap != null ? (String) valueMap.get("redis_host") : null;
            String suitePort = valueMap != null ? String.valueOf(valueMap.get("redis_port")) : null;
            String suitePassword = valueMap != null ? (String) valueMap.get("redis_password") : null;

            // 注解显式值优先，suite 参数兜底
            String host = StringUtils.isNotEmpty(redis.host()) ? redis.host() : suiteHost;
            int port = redis.port() != 6379 ? redis.port() : (StringUtils.isNotEmpty(suitePort) ? Integer.parseInt(suitePort) : 6379);
            String password = StringUtils.isNotEmpty(redis.password()) ? redis.password() : suitePassword;

            // 支持 url 方式配置（redis://host:port/db）
            if (StringUtils.isNotEmpty(redis.url())) {
                confMap.put("url", redis.url());
            } else if (StringUtils.isNotEmpty(host)) {
                confMap.put("host", host);
                confMap.put("port", String.valueOf(port));
                if (StringUtils.isNotEmpty(password)) {
                    confMap.put("password", password);
                }
                confMap.put("database", String.valueOf(redis.database()));
            } else {
                LOGGER.warn("Redis host or url not configured, skip data provider");
                return new Object[0][0];
            }

            // 构建命令字符串
            String command = buildCommand(redis);
            if (StringUtils.isEmpty(command)) {
                LOGGER.warn("Redis command is empty, skip data provider");
                return new Object[0][0];
            }

            LOGGER.debug("Redis conf: {}, command: {}", confMap, command);
            return getObjects(confMap, command);
        }
        return new Object[0][0];
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

    @SuppressWarnings("unchecked")
    private static Object[][] getObjects(Map<String, Object> confMap, String command) {
        IDataSource dataSource = DataSourceFactory.createDataSource(confMap);
        if (dataSource != null) {
            IDBConnection connection = dataSource.getConnection();
            try {
                List list = (List) connection.execute(command);
                LOGGER.info("get {} data from redis", list.size());
                return ListUtil.listToArray(list);
            } catch (Exception e) {
                LOGGER.error("Redis data provider error", e);
            } finally {
                connection.close();
            }
        }
        return new Object[0][0];
    }
}
