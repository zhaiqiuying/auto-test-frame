# Design

## Context

当前框架的 DB 抽象层（`btest/src/main/java/com/example/qa/btest/db/`）采用「接口 + 抽象类 + 工厂 + 缓存」模式：`IDataSource` 定义连接获取入口，`AbstractDataSource` 通过 Druid 连接池实现 JDBC 数据源，`DataSourceFactory` 按 URL 内容判别类型并用 `ConcurrentHashMap` 缓存实例。DataProvider 层采用「注解 + 注册表」模式：`CommonDataProvider` 在 static 块中注册所有 `DataProvider` 实现，TestNG `@DataProvider` 入口按方法注解匹配分发。

现有 MySQL 数据源完整依赖 JDBC 体系（`Connection`/`Statement`/`ResultSet`），Redis 不是 JDBC 数据源，无法直接复用 `AbstractDataSource` 的 Druid 连接池逻辑，但可以复用 `IDataSource`/`IDBConnection` 接口契约和 `DataSourceFactory` 的工厂+缓存模式。

详见 proposal.md - Why。

## Goals / Non-Goals

**Goals:**
- Redis 数据源与现有 MySQL/CSV 数据源在用例侧使用方式保持一致（注解驱动 + tag 接入两种路径）
- 复用现有 `IDataSource`/`IDBConnection` 接口契约和 `DataSourceFactory` 工厂缓存模式
- 支持常用 Redis 命令（GET/SET/DEL/EXPIRE/HGET/HSET/HGETALL/HDEL/LRANGE/LPUSH/RPUSH）
- 对现有代码改动最小化，保持向后兼容

**Non-Goals:**
- 不支持 Redis Cluster 模式（仅支持单节点 Standalone）
- 不支持 Redis Pub/Sub 和 Stream 功能
- 不支持 Redis 事务（MULTI/EXEC）
- 不实现 Redis 连接的 SSL/TLS 加密传输
- 不替换或重构现有 MySQL 数据源实现

## Decisions

### Decision 1: Redis 客户端选择 Jedis 4.x

**选择**: Jedis 4.x（`redis.clients:jedis:4.4.3`）

**理由**:
- API 简洁直观，与 JDBC 风格相似（`Jedis` → `get()`/`set()`/`hgetAll()`），学习成本低
- `JedisPool` 连接池管理与 Druid 的 `DataSource.getConnection()` 模式概念一致，适配 `IDataSource` 接口自然
- 4.x 系列最后兼容 Java 8 的版本（5.x 需要 Java 11+）
- 社区活跃，文档丰富

**备选方案**:
- **Lettuce**：基于 Netty 的异步客户端，线程安全无需连接池，但 API 较复杂，引入 Netty 依赖较重
- **Redisson**：功能强大但过于重量级，包含分布式锁、布隆过滤器等用例场景不需要的功能

### Decision 2: RedisDataSource 不继承 AbstractDataSource

**选择**: `RedisDataSource` 直接实现 `IDataSource`，不继承 `AbstractDataSource`

**理由**:
- `AbstractDataSource` 的核心逻辑是通过 `DruidDataSourceFactory.createDataSource(Map)` 创建 JDBC 连接池，Redis 不适用
- `AbstractDataSource` 内部持有 `javax.sql.DataSource` 类型字段，Redis 的 `JedisPool` 不是 JDBC DataSource
- 继承会导致无用的父类字段和方法，违反 ISP 原则

**备选方案**:
- 新增 `AbstractRedisDataSource` 中间抽象类 —— 过度设计，当前只有一个 Redis 实现

### Decision 3: RedisConnection 适配 IDBConnection 接口

**选择**: `RedisConnection` 实现 `IDBConnection`，将 Redis 命令适配为 `execute(String)` 接口

**理由**:
- 保持接口统一，`MysqlDataProvider` 和 `RedisDataProvider` 可以共用相同的「获取连接 → 执行 → 关闭」流程
- `IDBConnection.execute(String sql)` 的参数语义从 "SQL 语句" 扩展为 "命令表达式"（如 `GET key`、`HGET hash field`）
- `executeWithoutException` 和 `executeWithClose` 的异常处理语义同样适用于 Redis

**备选方案**:
- 新增独立的 `IRedisConnection` 接口 —— 打破了统一接口设计，`RedisDataProvider` 需要特判类型
- 不实现 `IDBConnection`，直接在 `RedisDataProvider` 中调用 Jedis —— 但会导致 tag 接入路径无法复用

### Decision 4: @Redis 注解设计

**选择**: 注解属性包括 `host`、`port`（默认 6379）、`password`（默认空）、`database`（默认 0）、`command`、`key`、`field`、`value`、`url`（可选，redis:// scheme）

**理由**:
- 与 `@Mysql` 注解风格保持一致（显式连接参数 + suite 参数兜底）
- `command` + `key` + `field` + `value` 覆盖常用命令的参数需求，避免用例写复杂命令字符串
- `url` 可选属性支持 `redis://host:port/db` 格式，与 MySQL 的 url 属性对齐

**备选方案**:
- 单一 `command` 字符串（如 `"GET user:1001"`）—— 灵活但需用例侧自行解析，且难以做参数化注入
- 多注解（`@RedisGet`、`@RedisSet` 等）—— 注解爆炸，增加注册表维护成本

### Decision 5: DataSourceFactory 判别逻辑

**选择**: 在 `createDataSource(Map)` 方法中新增 `url.contains("redis")` 判断分支，优先于 `url.contains("mysql")` 检查

**理由**:
- 与现有判别逻辑（`url.contains("mysql")`）风格一致
- Redis URL（`redis://host:port/db`）和 MySQL URL（`jdbc:mysql://...`）字符串无歧义

**备选方案**:
- 新增 `type` 字段显式声明 —— 需要修改 context.yml 结构，破坏向后兼容
- 在 `createDataSource(String)` 中也支持 redis —— 字符串入口当前仅用于 properties 文件名，Redis 配置通过 Map 入口加载即可

### Decision 6: RedisConnection 内部类设计

**选择**: `RedisConnection` 作为 `RedisDataSource` 的内部静态类，与 `MysqlConnection` 作为 `MysqlDataSource` 内部类的设计保持一致

**理由**:
- 数据源和连接一一对应，内部类体现了这种紧密耦合
- 与现有代码风格统一，降低认知成本

## Risks / Trade-offs

- **[Risk] Jedis 版本与 Java 8 兼容性** → 使用 Jedis 4.4.3，这是 4.x 系列最后明确支持 Java 8 的版本。后续如升级到 Java 11+ 可迁移到 Jedis 5.x
- **[Risk] Jedis 版本冲突** → `test-data-provider` 传递依赖引入了 jedis 3.8.0，与 btest 的 jedis 4.4.3 冲突（`hset` 签名从 `long` 变为 `Long`）。通过在 itest/pom.xml 中显式声明 `jedis 4.4.3` 解决
- **[Risk] 测试基础设施跨平台兼容性** → 原计划使用 `embedded-redis 0.6`，但其打包的 `redis-server` 仅支持 Linux x86，macOS 上无法运行。替换为 `jedis-mock 1.1.21`（纯 Java Redis 协议服务器），跨平台无需原生二进制，且支持 Java 8
- **[Risk] Redis 命令返回类型多样性** → Redis 不同命令返回 String、Long、List 等不同类型，统一转换为 `List<List<Object>>` 会丢失部分语义（如 SET 返回的 "OK" 字符串被包装为单行单列）。这是为了与 MySQL 数据源的返回格式保持一致以适配 TestNG DataProvider 的权衡
- **[Risk] 连接泄漏** → `RedisConnection` 需要正确关闭归还连接池。通过 try-finally 模式确保关闭（与 `MysqlDataProvider` 一致），并在 `executeWithClose` 方法中自动关闭
- **[Trade-off] 不支持 Cluster 模式** → 当前仅支持 Standalone，如有 Cluster 需求后续可扩展 `RedisClusterDataSource` 实现，不影响现有接口
- **[Trade-off] 命令覆盖范围有限** → 仅支持常用命令，如需执行 LUA 脚本或高级命令，可通过 `RedisConnection` 暴露的底层 Jedis 实例自行调用
