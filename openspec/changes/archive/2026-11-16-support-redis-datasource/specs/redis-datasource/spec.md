# Spec Delta

## Purpose

为接口自动化测试框架提供 Redis 数据源能力，支持用例通过注解或配置式 tag 两种方式连接 Redis 并执行命令获取/写入测试数据，与现有 MySQL/CSV 数据源形成统一的 DDT 体系。

## ADDED Requirements

### Requirement: Redis 数据源连接池管理

系统 SHALL 提供 Redis 数据源实现，通过连接池管理 Redis 连接，支持按配置（host、port、password、database）初始化并缓存连接池实例。同一份配置 MUST 只创建一次连接池。

#### Scenario: 通过配置 Map 创建 Redis 数据源
- **WHEN** 调用 `DataSourceFactory.getDataSource(Map)` 且配置中包含 `host` 字段或 url 以 `redis://` 开头
- **THEN** 系统 SHALL 创建 `RedisDataSource` 实例并缓存，后续相同配置返回同一实例

#### Scenario: 通过注解参数创建 Redis 数据源
- **WHEN** 用例方法标注 `@Redis` 注解并指定 host/port/password/database
- **THEN** 系统 SHALL 根据注解参数构建配置 Map，通过工厂创建 Redis 数据源并执行命令

#### Scenario: 连接池参数配置
- **WHEN** 配置中包含 `maxTotal`、`maxIdle`、`maxWait` 等连接池参数
- **THEN** 系统 SHALL 将这些参数应用到 Redis 连接池配置上

### Requirement: Redis 命令执行

系统 SHALL 支持通过 Redis 数据源执行常用 Redis 命令，包括字符串操作（GET/SET/DEL/EXPIRE）、哈希操作（HGET/HSET/HGETALL/HDEL）和列表操作（LRANGE/LPUSH/RPUSH）。执行结果 SHALL 转换为 `List` 类型，与 MySQL 数据源的返回格式保持一致。

#### Scenario: 执行 GET 命令
- **WHEN** 通过 `@Redis(command="GET", key="test_key")` 注解执行 GET 命令
- **THEN** 系统 SHALL 返回该 key 对应的值，封装为单行单列的 `List` 结构

#### Scenario: 执行 HGETALL 命令
- **WHEN** 通过 `@Redis(command="HGETALL", key="test_hash")` 注解执行 HGETALL 命令
- **THEN** 系统 SHALL 返回哈希表中所有字段和值，每对字段值封装为一行（两列），结果为 `List<List<Object>>` 结构

#### Scenario: 执行 SET 命令
- **WHEN** 通过 `@Redis(command="SET", key="test_key", value="test_value")` 注解执行 SET 命令
- **THEN** 系统 SHALL 写入数据并返回状态字符串（如 "OK"），封装为单行单列的 `List` 结构

#### Scenario: 命令执行异常处理
- **WHEN** Redis 命令执行抛出异常（如连接失败、命令不存在）
- **THEN** 系统 SHALL 捕获异常并返回空 List，不中断测试执行流程

### Requirement: @Redis 注解驱动的数据驱动测试

系统 SHALL 提供 `@Redis` 注解，标注在测试方法上时自动通过 `CommonDataProvider` 注册表分发到 `RedisDataProvider`，执行 Redis 命令并将结果转换为 TestNG DataProvider 格式（`Object[][]`）注入测试方法参数。

#### Scenario: 注解驱动数据注入
- **WHEN** 测试方法标注 `@Redis(host="127.0.0.1", port=6379, command="GET", key="user:1001")` 且方法参数为 `String value`
- **THEN** 系统 SHALL 执行 GET 命令获取 `user:1001` 的值，将其作为方法参数注入测试方法

#### Scenario: suite 参数默认值
- **WHEN** `@Redis` 注解的 host/port/password 为空且 suite 参数中配置了 `redis_host`/`redis_port`/`redis_password`
- **THEN** 系统 SHALL 使用 suite 参数作为默认连接配置

#### Scenario: 多行结果注入
- **WHEN** Redis 命令返回多行结果（如 HGETALL 返回多个字段值对）且测试方法参数为 `String field, String value`
- **THEN** 系统 SHALL 将每行结果拆分为多个参数，为每行数据生成一组测试参数

### Requirement: context.yml 配置式 Tag 接入

系统 SHALL 支持在 `context.yml` 中配置 `redises` 段，每项包含 `tag`、`host`、`port`、`password`、`database` 字段。框架启动时 SHALL 加载所有 Redis 配置，创建数据源并按 tag 存入 `CommonContext`，供用例按 tag 获取连接。

#### Scenario: context.yml 加载 Redis 数据源
- **WHEN** `context.yml` 中配置了 `redises` 段，包含多个 Redis 实例配置
- **THEN** 系统 SHALL 在框架启动时为每项创建 `IDataSource` 实例并按 tag 存入 `CommonContext.redisDataSourceMap`

#### Scenario: 用例按 tag 获取 Redis 连接
- **WHEN** 用例通过 `getRedisConnection("cache_db")` 获取连接
- **THEN** 系统 SHALL 从 `CommonContext.redisDataSourceMap` 中按 tag 获取对应数据源并返回连接对象

### Requirement: 工厂类型判别扩展

系统 SHALL 扩展 `DataSourceFactory` 的类型判别逻辑，在现有 MySQL 分支基础上新增 Redis 分支。当配置 URL 包含 `redis` 字符串时 SHALL 创建 `RedisDataSource`，当 URL 包含 `mysql` 时 SHALL 创建 `MysqlDataSource`，未匹配的类型 SHALL 返回 null。

#### Scenario: Redis URL 判别
- **WHEN** 配置 Map 中 url 字段值为 `redis://127.0.0.1:6379/0`
- **THEN** 系统 SHALL 创建 `RedisDataSource` 实例

#### Scenario: MySQL URL 判别不受影响
- **WHEN** 配置 Map 中 url 字段值为 `jdbc:mysql://127.0.0.1:3306/test`
- **THEN** 系统 SHALL 创建 `MysqlDataSource` 实例，行为与现有逻辑完全一致

### Requirement: 测试可验证性

系统 SHALL 提供完整的测试用例覆盖所有 Redis 数据源功能，测试 SHALL 使用嵌入式 Redis 作为测试基础设施以避免对外部 Redis 实例的依赖。测试用例 MUST 覆盖连接池管理、命令执行与结果转换、异常处理、工厂类型判别、注解驱动数据注入和 tag 接入各场景。

#### Scenario: 连接池管理测试
- **WHEN** 使用嵌入式 Redis 运行 `RedisDataSourceTest`
- **THEN** 测试 SHALL 验证通过 Map 配置创建 `RedisDataSource` 实例、`getConnection()` 返回非 null 连接、相同配置返回缓存的同一实例

#### Scenario: 命令执行与结果转换测试
- **WHEN** 使用嵌入式 Redis 运行命令执行测试
- **THEN** 测试 SHALL 验证 GET 返回值封装为单行单列 List、HGETALL 返回多行两列 List、DEL 返回删除数量、LRANGE 返回列表元素，结果结构与 MySQL 数据源格式一致

#### Scenario: 异常处理测试
- **WHEN** 模拟 Redis 连接异常或命令错误
- **THEN** 测试 SHALL 验证 `executeWithoutException` 返回空 List 不抛异常、`executeWithClose` 执行后连接自动归还、`close` 后连接池仍可获取新连接

#### Scenario: 工厂类型判别测试
- **WHEN** 运行 `DataSourceFactoryRedisTest`
- **THEN** 测试 SHALL 验证 Redis URL 创建 `RedisDataSource`、MySQL URL 创建 `MysqlDataSource`、相同配置的 Map 返回缓存的同一实例

#### Scenario: 注解驱动数据注入测试
- **WHEN** 使用嵌入式 Redis 运行 `RedisDataProviderTest`
- **THEN** 测试 SHALL 验证 `@Redis` 注解标注的方法通过 `CommonDataProvider.genData` 获取非空 `Object[][]`、GET 命令结果正确注入方法参数、HGETALL 多行结果正确拆分为多组参数

#### Scenario: tag 接入测试
- **WHEN** 在 context.yml 中配置 `redises` 段并运行 `RedisTagConnectionTest`
- **THEN** 测试 SHALL 验证 `CommonContext.redisDataSourceMap` 包含对应 tag、`getRedisConnection(tag)` 返回非 null 连接、通过 tag 连接执行命令结果正确
