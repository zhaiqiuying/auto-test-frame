# Proposal

## Why

当前框架的 DB 抽象层仅支持 MySQL（JDBC + Druid），无法满足用例中需要从 Redis 读取/写入测试数据的场景。许多接口测试需要校验 Redis 缓存状态（如验证写入缓存的数据、读取缓存做断言前置数据准备），缺少 Redis 数据源支持使得这类用例不得不绕行外部脚本或手动操作，降低了自动化覆盖率和效率。

## What Changes

- 在 `btest` 层新增 Redis 数据源抽象（`RedisDataSource` 实现 `IDataSource`，`RedisConnection` 实现 `IDBConnection`），通过 Jedis 客户端管理连接池
- `DataSourceFactory` 工厂新增 Redis 类型判别分支（`url.contains("redis")` 或配置含 `host`+`port` 的 redis scheme）
- 新增 `@Redis` 注解，支持在用例方法上声明 Redis 连接信息和命令，实现注解驱动的数据驱动测试（DDT）
- 新增 `RedisDataProvider`，注册到 `CommonDataProvider` 注册表，用例侧零侵入使用
- `context.yml` 新增 `redises` 配置段，`Config.loadContext` 加载后存入 `CommonContext.redisDataSourceMap`，支持按 tag 获取连接
- `btest/pom.xml` 新增 Jedis 依赖（4.x 系列，兼容 Java 8）
- README 同步更新 Redis 数据源使用说明章节

## Capabilities

### New Capabilities

- `redis-datasource`: Redis 数据源能力，涵盖 Redis 连接池管理、命令执行（GET/SET/HGET/HSET/DEL/EXPIRE 等）、注解驱动的 `@Redis` 数据驱动测试、context.yml 配置式 tag 接入，以及与现有 `DataSourceFactory`/`CommonDataProvider`/`CommonContext` 的集成

### Modified Capabilities

（无 —— 项目当前 `openspec/specs/` 为空，无已有 capability 需要修改）

## Impact

- **代码影响**：
  - `btest/src/main/java/com/example/qa/btest/db/`：新增 `redis/` 子包（`RedisDataSource`、`RedisConnection`）
  - `btest/src/main/java/com/example/qa/btest/db/DataSourceFactory.java`：修改工厂方法添加 Redis 分支
  - `btest/src/main/java/com/example/qa/btest/engine/dataProvider/`：新增 `redis/` 子包（`Redis` 注解、`RedisDataProvider`）
  - `btest/src/main/java/com/example/qa/btest/engine/dataProvider/common/CommonDataProvider.java`：static 块注册 `RedisDataProvider`
  - `itest/src/main/java/com/example/qa/itest/model/CommonContext.java`：新增 `redisDataSourceMap` 字段
  - `itest/src/main/java/com/example/qa/itest/common/Config.java`：`loadContext` 新增 `redises` 段加载逻辑
- **依赖影响**：`btest/pom.xml` 新增 `redis.clients:jedis` 依赖（4.x）
- **配置影响**：`context.yml` 新增 `redises` 配置段
- **向后兼容**：完全向后兼容，现有 MySQL/CSV 数据源不受影响
