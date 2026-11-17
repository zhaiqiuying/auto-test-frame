package com.example.qa.btest.db.redis;

import com.example.qa.btest.db.IDBConnection;
import com.example.qa.btest.db.IDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RedisDataSource implements IDataSource {

    private static final Logger log = LoggerFactory.getLogger(RedisDataSource.class);

    private JedisPool jedisPool;

    /**
     * 通过配置 Map 创建 Redis 数据源。
     * 支持的配置项：host、port、password、database、maxTotal、maxIdle、maxWait。
     * 也支持通过 url（redis://host:port/db）方式配置。
     *
     * @param conf Redis 配置 Map
     */
    public RedisDataSource(Map conf) {
        String host;
        int port;
        String password;
        int database;

        // 支持 url 格式：redis://host:port/db
        String url = (String) conf.getOrDefault("url", "");
        if (url.startsWith("redis://")) {
            String stripped = url.substring("redis://".length());
            String[] parts = stripped.split("/");
            String hostPort = parts[0];
            String[] hp = hostPort.split(":");
            host = hp[0];
            port = hp.length > 1 ? Integer.parseInt(hp[1]) : 6379;
            database = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        } else {
            host = (String) conf.getOrDefault("host", "127.0.0.1");
            port = conf.containsKey("port") ? Integer.parseInt(String.valueOf(conf.get("port"))) : 6379;
            database = conf.containsKey("database") ? Integer.parseInt(String.valueOf(conf.get("database"))) : 0;
        }

        password = (String) conf.getOrDefault("password", "");

        // 连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        if (conf.containsKey("maxTotal")) {
            poolConfig.setMaxTotal(Integer.parseInt(String.valueOf(conf.get("maxTotal"))));
        }
        if (conf.containsKey("maxIdle")) {
            poolConfig.setMaxIdle(Integer.parseInt(String.valueOf(conf.get("maxIdle"))));
        }
        if (conf.containsKey("maxWait")) {
            poolConfig.setMaxWaitMillis(Long.parseLong(String.valueOf(conf.get("maxWait"))));
        }

        if (password != null && !password.isEmpty()) {
            jedisPool = new JedisPool(poolConfig, host, port, 2000, password, database);
        } else {
            jedisPool = new JedisPool(poolConfig, host, port, 2000, null, database);
        }

        log.info("RedisDataSource initialized: host={}, port={}, database={}", host, port, database);
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
    public static class RedisConnection implements IDBConnection {

        private static final Logger log = LoggerFactory.getLogger(RedisConnection.class);

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
        @SuppressWarnings("unchecked")
        public List execute(String command) {
            return executeCommandToList(command);
        }

        /**
         * 解析并执行 Redis 命令，返回 List<List<Object>> 格式结果。
         */
        @SuppressWarnings("unchecked")
        private List executeCommandToList(String command) {
            String[] parts = command.trim().split("\\s+");
            if (parts.length == 0) {
                return Collections.EMPTY_LIST;
            }

            String cmd = parts[0].toUpperCase();
            List result = new ArrayList();

            try {
                switch (cmd) {
                    // ---- 字符串操作 ----
                    case "GET": {
                        String val = jedis.get(parts[1]);
                        if (val != null) {
                            List<Object> row = new ArrayList<>();
                            row.add(val);
                            result.add(row);
                        }
                        break;
                    }
                    case "SET": {
                        // SET key value
                        String status = jedis.set(parts[1], parts.length > 2 ? parts[2] : "");
                        List<Object> row = new ArrayList<>();
                        row.add(status);
                        result.add(row);
                        break;
                    }
                    case "DEL": {
                        // DEL key1 key2 ...
                        String[] keys = new String[parts.length - 1];
                        System.arraycopy(parts, 1, keys, 0, keys.length);
                        Long count = jedis.del(keys);
                        List<Object> row = new ArrayList<>();
                        row.add(count);
                        result.add(row);
                        break;
                    }
                    case "EXPIRE": {
                        // EXPIRE key seconds
                        Long ret = jedis.expire(parts[1], Long.parseLong(parts[2]));
                        List<Object> row = new ArrayList<>();
                        row.add(ret);
                        result.add(row);
                        break;
                    }
                    // ---- 哈希操作 ----
                    case "HGET": {
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
                        // HSET hash field value
                        Long count = jedis.hset(parts[1], parts[2], parts.length > 3 ? parts[3] : "");
                        List<Object> row = new ArrayList<>();
                        row.add(count);
                        result.add(row);
                        break;
                    }
                    case "HGETALL": {
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
                        // HDEL hash field1 field2 ...
                        String[] fields = new String[parts.length - 2];
                        System.arraycopy(parts, 2, fields, 0, fields.length);
                        Long count = jedis.hdel(parts[1], fields);
                        List<Object> row = new ArrayList<>();
                        row.add(count);
                        result.add(row);
                        break;
                    }
                    // ---- 列表操作 ----
                    case "LRANGE": {
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
                        // LPUSH key value1 value2 ...
                        String[] values = new String[parts.length - 2];
                        System.arraycopy(parts, 2, values, 0, values.length);
                        Long count = jedis.lpush(parts[1], values);
                        List<Object> row = new ArrayList<>();
                        row.add(count);
                        result.add(row);
                        break;
                    }
                    case "RPUSH": {
                        // RPUSH key value1 value2 ...
                        String[] values = new String[parts.length - 2];
                        System.arraycopy(parts, 2, values, 0, values.length);
                        Long count = jedis.rpush(parts[1], values);
                        List<Object> row = new ArrayList<>();
                        row.add(count);
                        result.add(row);
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
        @SuppressWarnings("unchecked")
        public Object executeWithoutException(String command) {
            try {
                return execute(command);
            } catch (Exception e) {
                log.error("executeWithoutException error, command: " + command, e);
            }
            return Collections.EMPTY_LIST;
        }

        @Override
        @SuppressWarnings("unchecked")
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
