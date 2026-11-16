# Tasks

## 1. 依赖引入

- [x] 1.1 在 `btest/pom.xml` 中新增 Jedis 依赖（`redis.clients:jedis:4.4.3`，compile scope）和嵌入式 Redis 测试依赖（`com.github.kstyrc:embedded-redis:0.6`，test scope），验证 `mvn dependency:tree` 能正确解析两个依赖且无版本冲突
- [x] 1.2 执行 `mvn -B clean test` 验证依赖引入后项目构建正常，现有测试不受影响

## 2. Redis 数据源抽象层实现

- [x] 2.1 创建 `btest/src/main/java/com/example/qa/btest/db/redis/RedisDataSource.java`，实现 `IDataSource` 接口，内部通过 `JedisPool` 管理连接池。支持通过 Map 配置（host/port/password/database/maxTotal/maxIdle/maxWait）初始化连接池。验证：单元测试中能创建 `RedisDataSource` 实例并获取 `IDBConnection` 连接
- [x] 2.2 在 `RedisDataSource` 中创建内部静态类 `RedisConnection` 实现 `IDBConnection`，将 `execute(String)` 适配为 Redis 命令执行（解析命令字符串如 `GET key`、`HGETALL key`），结果统一转换为 `List<List<Object>>`。验证：`execute("GET test_key")` 返回包含值的单行 List，`execute("HGETALL test_hash")` 返回多行多列 List
- [x] 2.3 实现 `RedisConnection` 的 `executeWithoutException`（异常返回空 List）、`executeWithClose`（执行后自动关闭）、`close`（归还连接到池）方法。验证：模拟异常场景时返回空 List 且不抛出异常

## 3. 工厂扩展

- [x] 3.1 修改 `DataSourceFactory.createDataSource(Map)` 方法，新增 `url.contains("redis")` 判别分支（优先于 mysql 检查），匹配时创建 `RedisDataSource` 实例。验证：传入 `url=redis://127.0.0.1:6379/0` 的配置 Map 返回 `RedisDataSource` 实例，传入 `url=jdbc:mysql://...` 仍返回 `MysqlDataSource` 实例
- [x] 3.2 执行 `mvn -B clean test` 验证工厂扩展后现有 MySQL 数据源功能不受影响

## 4. @Redis 注解与 DataProvider 实现

- [x] 4.1 创建 `btest/src/main/java/com/example/qa/btest/engine/dataProvider/redis/Redis.java` 注解，定义 `host`、`port`（默认 6379）、`password`（默认空）、`database`（默认 0）、`command`、`key`、`field`、`value`、`url`（默认空）属性，`@Retention(RUNTIME) @Target(METHOD)`。验证：注解能正确标注在测试方法上并通过反射读取属性值
- [x] 4.2 创建 `RedisDataProvider` 实现 `DataProvider` 接口，`getDataType()` 返回 `Redis.class`，`getObjects(MethodContext)` 中解析注解属性构建配置 Map，调用 `DataSourceFactory.createDataSource` 创建数据源，执行命令并通过 `ListUtil.listToArray` 转换为 `Object[][]`。验证：传入标注 `@Redis` 的方法上下文能返回非空 `Object[][]` 数据
- [x] 4.3 在 `CommonDataProvider` 的 static 块中添加 `register(new RedisDataProvider())`，并在 `genData` 方法中注入 `redis_host`/`redis_port`/`redis_password` suite 参数到 `MethodContext.valueMap`。验证：`CommonDataProvider.dataProviderMap` 中存在 `Redis.class` 的注册条目

## 5. 配置式 Tag 接入

- [x] 5.1 在 `CommonContext` 中新增 `redisDataSourceMap` 字段（`Map<String, IDataSource>`，`ConcurrentHashMap` 初始化）。验证：字段存在且可通过 getter/setter 访问
- [x] 5.2 修改 `Config.loadContext` 方法，新增 `redises` 配置段加载逻辑：遍历配置列表，通过 `DataSourceFactory.getDataSource(Map)` 创建数据源，按 tag 存入 `CommonContext.redisDataSourceMap`。验证：配置 `redises` 段的 context.yml 加载后 `redisDataSourceMap` 包含对应 tag 的数据源实例
- [x] 5.3 在 `BaseCase` 中新增 `getRedisConnection(String tag)` 便捷方法，从 `CommonContext.redisDataSourceMap` 按 tag 获取数据源并返回连接。验证：调用 `getRedisConnection("cache_db")` 能返回 `IDBConnection` 实例

## 6. 测试用例实现

- [x] 6.1 创建 `btest/src/test/java/com/example/qa/btest/db/redis/RedisDataSourceTest.java`，使用 jedis-mock（`RedisServer.newRedisServer(port)` 在 `@BeforeClass` 启动、`@AfterClass` 关闭）作为测试基础设施。测试点：通过 Map 配置创建 `RedisDataSource` 实例、`getConnection()` 返回非 null 的 `IDBConnection`、相同配置返回缓存的同一实例。验证：`mvn -pl btest -Dtest=RedisDataSourceTest test` 通过
- [x] 6.2 在 `RedisDataSourceTest` 中测试 `RedisConnection.execute(String)` 的命令执行与结果转换。测试点：SET 后 GET 返回正确值且封装为单行单列 List、HSET 多字段后 HGETALL 返回多行两列 List、DEL 返回删除数量、EXPIRE 返回 1、LPUSH+LRANGE 返回列表元素。验证：每个断言通过，结果结构与 spec 场景一致
- [x] 6.3 在 `RedisDataSourceTest` 中测试异常处理方法。测试点：`executeWithoutException` 在连接关闭后执行命令返回空 List 不抛异常、`executeWithClose` 执行后连接自动归还（再次 getConnection 可正常使用）、`close` 后连接池仍可获取新连接。验证：无异常抛出且断言通过
- [x] 6.4 创建 `btest/src/test/java/com/example/qa/btest/db/DataSourceFactoryRedisTest.java`，测试工厂类型判别扩展。测试点：`url=redis://127.0.0.1:6379/0` 返回 `RedisDataSource` 实例、`url=jdbc:mysql://127.0.0.1:3306/test` 返回 `MysqlDataSource` 实例、相同 Redis 配置的 Map 返回缓存的同一实例（MD5 key 一致）。验证：`mvn -pl btest -Dtest=DataSourceFactoryRedisTest test` 通过
- [x] 6.5 创建 `itest/src/test/java/com/example/qa/itest/RedisDataProviderTest.java`，测试 `@Redis` 注解驱动的数据注入。测试点：`@Redis(command="SET", key="dp_key", value="dp_value")` 标注的方法能通过 `RedisDataProvider.getObjects` 获取到非空 `Object[][]`、SET 后 `@Redis(command="GET", key="dp_key")` 注入的参数值为 "dp_value"、`@Redis(command="HGETALL", key="dp_hash")` 多行结果正确拆分为多组参数。验证：`mvn -pl itest -Dtest=RedisDataProviderTest test` 通过，testng.xml 中注册该测试类
- [x] 6.6 创建 `itest/src/test/java/com/example/qa/itest/RedisTagConnectionTest.java`，测试 context.yml 配置式 tag 接入。测试点：在 `/{ENV_TYPE}/data/common/context.yml` 中配置 `redises` 段后 `CommonContext.redisDataSourceMap` 包含对应 tag、`getRedisConnection("cache_db")` 返回非 null 连接、通过 tag 连接执行 GET/SET 命令结果正确。验证：`mvn -pl itest -Dtest=RedisTagConnectionTest test` 通过

## 7. 文档更新

- [x] 7.1 更新 README，新增「Redis 数据源」章节，包含：@Redis 注解使用示例、context.yml redises 配置段说明、getRedisConnection(tag) 用法、支持的命令列表。验证：README 中能检索到 Redis 相关内容且示例代码格式正确

## 8. 构建验证

- [x] 8.1 执行 `mvn -B clean test` 完整构建验证，确认编译通过、所有新增 Redis 测试用例（RedisDataSourceTest、DataSourceFactoryRedisTest、RedisDataProviderTest、RedisTagConnectionTest）和现有测试全部通过、无新增编译警告。验证：构建输出 `BUILD SUCCESS`，TestNG 报告中 Redis 相关测试方法均为 PASS
