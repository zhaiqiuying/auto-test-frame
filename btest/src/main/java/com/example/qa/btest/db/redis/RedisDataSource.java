package com.example.qa.btest.db.redis;

import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Redis 数据源实现，通过 JedisPool 管理连接池。
 * 实现 IDataSource 接口，与 MySQL 数据源统一抽象。
 */
@Slf4j
public class RedisDataSource implements IDataSource {

    private static final String REDIS_URL_PREFIX = "redis://";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_DATABASE = 0;
    private static final int TIMEOUT_MILLIS = 2000;

    private static final String KEY_URL = "url";
    private static final String KEY_HOST = "host";
    private static final String KEY_PORT = "port";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_DATABASE = "database";
    private static final String KEY_MAX_TOTAL = "maxTotal";
    private static final String KEY_MAX_IDLE = "maxIdle";
    private static final String KEY_MAX_WAIT = "maxWait";

    private final JedisPool jedisPool;

    /**
     * 通过配置 Map 创建 Redis 数据源。
     * 支持的配置项：host、port、password、database、maxTotal、maxIdle、maxWait。
     * 也支持通过 url（redis://host:port/db）方式配置。
     *
     * @param conf Redis 配置 Map
     */
    public RedisDataSource(Map<String, Object> conf) {
        String host;
        int port;
        int database;

        // 支持 url 格式：redis://host:port/db
        String url = (String) conf.getOrDefault(KEY_URL, "");
        if (url.startsWith(REDIS_URL_PREFIX)) {
            String[] parsed = parseRedisUrl(url);
            host = parsed[0];
            port = Integer.parseInt(parsed[1]);
            database = parsed.length > 2 ? Integer.parseInt(parsed[2]) : DEFAULT_DATABASE;
        } else {
            host = (String) conf.getOrDefault(KEY_HOST, DEFAULT_HOST);
            port = conf.containsKey(KEY_PORT) ? Integer.parseInt(String.valueOf(conf.get(KEY_PORT))) : DEFAULT_PORT;
            database = conf.containsKey(KEY_DATABASE) ? Integer.parseInt(String.valueOf(conf.get(KEY_DATABASE))) : DEFAULT_DATABASE;
        }

        String password = (String) conf.getOrDefault(KEY_PASSWORD, "");

        JedisPoolConfig poolConfig = buildPoolConfig(conf);

        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, TIMEOUT_MILLIS, password, database);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, TIMEOUT_MILLIS, null, database);
        }

        log.info("RedisDataSource initialized: host={}, port={}, database={}", host, port, database);
    }

    /**
     * 解析 redis://host:port/db 格式的 URL，返回 [host, port] 或 [host, port, database]。
     */
    private String[] parseRedisUrl(String url) {
        String stripped = url.substring(REDIS_URL_PREFIX.length());
        String[] parts = stripped.split("/");
        String hostPort = parts[0];
        String[] hp = hostPort.split(":");
        if (hp.length > 2) {
            return new String[]{hp[0], hp[1]};
        }
        if (parts.length > 1) {
            return new String[]{hp[0], hp[1], parts[1]};
        }
        return new String[]{hp[0], hp[1]};
    }

    /**
     * 从配置 Map 构建 JedisPoolConfig。
     */
    private JedisPoolConfig buildPoolConfig(Map<String, Object> conf) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        if (conf.containsKey(KEY_MAX_TOTAL)) {
            poolConfig.setMaxTotal(Integer.parseInt(String.valueOf(conf.get(KEY_MAX_TOTAL))));
        }
        if (conf.containsKey(KEY_MAX_IDLE)) {
            poolConfig.setMaxIdle(Integer.parseInt(String.valueOf(conf.get(KEY_MAX_IDLE))));
        }
        if (conf.containsKey(KEY_MAX_WAIT)) {
            poolConfig.setMaxWaitMillis(Long.parseLong(String.valueOf(conf.get(KEY_MAX_WAIT))));
        }
        return poolConfig;
    }

    @Override
    public IDBConnection getConnection() {
        Jedis jedis = jedisPool.getResource();
        return new RedisConnection(jedis);
    }

    /**
     * Redis 连接实现，适配 IDBConnection 接口。
     * 将 Redis 命令字符串（如 "GET key"、"HGETALL key"）解析并执行，
     * 结果统一转换为 List<List<Object>> 格式，与 MySQL 数据源保持一致。
     */
    @Slf4j
    public static class RedisConnection implements IDBConnection {

        private Jedis jedis;

        public RedisConnection(Jedis jedis) {
            this.jedis = jedis;
        }

        /**
         * 获取底层 Jedis 实例（供高级命令使用）。
         */
        public Jedis getJedis() {
            return jedis;
        }

        /**
         * 执行 Redis 命令，支持自定义结果处理。
         * 此方法为接口适配，params[0] 传入解析后的命令结果。
         */
        @Override
        public Object execute(String command, com.example.qa.btest.db.Execute execute) {
            Object result = executeCommand(command);
            return execute.run(result);
        }

        /**
         * 执行 Redis 命令，结果转换为 List<List<Object>>。
         * 命令格式：命令名 + 空格分隔的参数（如 "GET mykey"、"HGETALL myhash"）。
         */
        @Override
        public List<List<Object>> execute(String command) {
            return executeCommandToList(command);
        }

        /**
         * 解析并执行 Redis 命令，返回 List<List<Object>> 格式结果。
         */
        private List<List<Object>> executeCommandToList(String command) {
            String[] parts = command.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) {
                return Collections.emptyList();
            }

            String cmd = parts[0].toUpperCase();
            List<List<Object>> result = new ArrayList<>();

            try {
                switch (cmd) {
                    // ---- 字符串操作 ----
                    case "GET": {
                        if (parts.length < 2) {
                            return Collections.emptyList();
                        }
                        String val = jedis.get(parts[1]);
                        if (val != null) {
                            List<Object> row = new ArrayList<>();
                            row.add(val);
                            result.add(row);
                        }
                        break;
                    }
                    case "SET": {
                        if (parts.length < 2) {
                            return Collections.emptyList();
                        }
                        // SET key value
                        String status = jedis.set(parts[1], parts.length > 2 ? parts[2] : "");
                        List<Object> row = new ArrayList<>();
                        row.add(status);
                        result.add(row);
                        break;
                    }
                    case "DEL": {
                        if (parts.length < 2) {
                            return Collections.emptyList();
                        }
                        // DEL key1 key2 ...
                        String[] keys = new String[parts.length - 1];
                        System.arraycopy(parts, 1, keys, 0, keys.length);
                        Long count = jedis.del(keys);
                        List<Object> row2 = new ArrayList<>();
                        row2.add(count);
                        result.add(row2);
                        break;
                    }
                    case "EXPIRE": {
                        if (parts.length < 3) {
                            return Collections.emptyList();
                        }
                        // EXPIRE key seconds
                        Long ret = jedis.expire(parts[1], Long.parseLong(parts[2]));
                        List<Object> row3 = new ArrayList<>();
                        row3.add(ret);
                        result.add(row3);
                        break;
                    }
                    // ---- 哈希操作 ----
                    case "HGET": {
                        if (parts.length < 3) {
                            return Collections.emptyList();
                        }
                        // HGET hash field
                        String val = jedis.hget(parts[1], parts[2]);
                        if (val != null) {
                            List<Object> row = new ArrayList<>();
                            row.add(val);
                            result.add(row);
                        }
                        break;
                    }
                    case "HSET": {
                        if (parts.length < 3) {
                            return Collections.emptyList();
                        }
                        // HSET hash field value
                        Long count = jedis.hset(parts[1], parts[2], parts.length > 3 ? parts[3] : "");
                        List<Object> row4 = new ArrayList<>();
                        row4.add(count);
                        result.add(row4);
                        break;
                    }
                    case "HGETALL": {
                        if (parts.length < 2) {
                            return Collections.emptyList();
                        }
                        // HGETALL hash → 每对 field-value 为一行（两列）
                        Map<String, String> map = jedis.hgetAll(parts[1]);
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            List<Object> row = new ArrayList<>();
                            row.add(entry.getKey());
                            row.add(entry.getValue());
                            result.add(row);
                        }
                        break;
                    }
                    case "HDEL": {
                        if (parts.length < 3) {
                            return Collections.emptyList();
                        }
                        // HDEL hash field1 field2 ...
                        String[] fields = new String[parts.length - 2];
                        System.arraycopy(parts, 2, fields, 0, fields.length);
                        Long count = jedis.hdel(parts[1], fields);
                        List<Object> row5 = new ArrayList<>();
                        row5.add(count);
                        result.add(row5);
                        break;
                    }
                    // ---- 列表操作 ----
                    case "LRANGE": {
                        if (parts.length < 4) {
                            return Collections.emptyList();
                        }
                        // LRANGE key start stop
                        List<String> list = jedis.lrange(parts[1], Long.parseLong(parts[2]), Long.parseLong(parts[3]));
                        for (String item : list) {
                            List<Object> row = new ArrayList<>();
                            row.add(item);
                            result.add(row);
                        }
                        break;
                    }
                    case "LPUSH": {
                        if (parts.length < 3) {
                            return Collections.emptyList();
                        }
                        // LPUSH key value1 value2 ...
                        String[] values = new String[parts.length - 2];
                        System.arraycopy(parts, 2, values, 0, values.length);
                        Long count = jedis.lpush(parts[1], values);
                        List<Object> row6 = new ArrayList<>();
                        row6.add(count);
                        result.add(row6);
                        break;
                    }
                    case "RPUSH": {
                        if (parts.length < 3) {
                            return Collections.emptyList();
                        }
                        // RPUSH key value1 value2 ...
                        String[] values = new String[parts.length - 2];
                        System.arraycopy(parts, 2, values, 0, values.length);
                        Long count = jedis.rpush(parts[1], values);
                        List<Object> row7 = new ArrayList<>();
                        row7.add(count);
                        result.add(row7);
                        break;
                    }
                    default:
                        log.warn("Unsupported Redis command: {}", cmd);
                        break;
                }
            } catch (Exception e) {
                log.error("Redis command execution error: {}", command, e);
                throw new RuntimeException(e);
            }

            return result;
        }

        /**
         * 执行 Redis 命令，返回原始结果对象（供 Execute 回调使用）。
         */
        private Object executeCommand(String command) {
            return executeCommandToList(command);
        }

        @Override
        public Object executeWithoutException(String command) {
            try {
                return execute(command);
            } catch (Exception e) {
                log.error("executeWithoutException error, command: {}", command, e);
            }
            return Collections.emptyList();
        }

        @Override
        public Object executeWithClose(String command) {
            Object result = executeWithoutException(command);
            this.close();
            return result;
        }

        @Override
        public void close() {
            try {
                if (jedis != null) {
                    jedis.close();
                }
            } catch (Exception e) {
                log.warn("Error closing Redis connection", e);
            }
        }
    }
}
