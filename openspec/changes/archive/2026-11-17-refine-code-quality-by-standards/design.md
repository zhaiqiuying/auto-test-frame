# Design

## Context

See proposal.md - Why. 本次变更针对 `feature/support-redis-datasource` 分支的 12 个 Java 文件，按编码规范（阿里手册基线 + 项目增量 `openspec/rules/coding-standards.md`）进行代码质量优化。所有重构保持对外接口签名和运行时行为不变。

当前代码存在 6 类系统性问题：裸类型泛滥（~40处）、手动 Logger 声明（5处）、`Collections.EMPTY_LIST`（2处）、`e.printStackTrace()`（1处）、魔法值（~50处）、过长方法和重复代码（5处）。

## Goals / Non-Goals

**Goals:**

- 消除所有裸类型，泛型参数完整
- 日志统一 `@Slf4j` + `{}` 占位符
- 魔法值提取为常量（`UPPER_SNAKE_CASE`）
- 过长方法拆分至合理长度（<30 行）
- 重复代码消除
- 潜在 NPE/越界修复
- `mvn -B clean test` 全量通过（btest 15 + itest 6）

**Non-Goals:**

- 不修改对外接口签名（`IDataSource`、`IDBConnection`、`DataProvider` 方法签名不变）
- 不改变运行时行为（异常处理策略保持分层约定不变）
- 不重构历史代码（本次只优化本次 PR 变更的 12 个文件）
- 不改变 Maven 依赖
- 不引入新的静态分析工具（如 SpotBugs/PMD 插件）

## Decisions

### D1: 裸类型修复策略 — 接口同步修改

**决策**：`IDBConnection`、`DataProvider` 接口中的裸类型 `Class` 同步修改为 `Class<?>`。`IDBConnection.execute` 返回类型保持 `Object`（历史约定，调用方强转），但调用方变量声明补全泛型。

**理由**：只改调用方不改接口，裸类型警告无法根除。

**备选方案**：只改实现不改接口 — 仍有 `@SuppressWarnings` 残留，不彻底。

### D2: 日志统一 — `@Slf4j` 替换手动声明

**决策**：`RedisDataSource`、`RedisDataSource.RedisConnection`、`CommonDataProvider`、`RedisDataProvider` 中手动声明的 `private static final Logger LOGGER/log` 替换为 `@Slf4j` 注解。统一变量名为 `log`。

**理由**：编码规范明确要求 `@Slf4j`，且 Lombok 已在依赖中。

**备选方案**：保持手动声明但统一命名 — 违反规范，后续仍需改。

### D3: 魔法值处理 — 提取为 `private static final` 常量

**决策**：配置 key（`"url"`、`"host"`、`"port"` 等）、类型标识（`"redis"`、`"mysql"`）、默认值（`6379`、`0`、`2000`）提取为 `private static final` 常量。测试注解中的端口号使用 `static final` 常量引用。

**理由**：阿里手册明确禁止魔法值。常量集中定义便于维护。

**备选方案**：使用枚举 — 对配置 key 过重，不适合。

### D4: 过长方法拆分 — 按职责提取子方法

**决策**：

| 方法 | 拆分策略 |
|------|----------|
| `RedisDataSource` 构造函数 | 提取 `parseRedisUrl()`、`parseHostPort()`、`buildPoolConfig()` |
| `Config.loadContext` | 提取 `loadDataSources(String key, Map<String, IDataSource> targetMap)` 消除 mysqls/redises 重复 |
| `BaseCase.beforeSuite` | 提取 `buildDevice()`、`buildApp()`、`buildEnv()`、`buildUser()` |
| `RedisDataProvider.getObjects` | 提取 `buildConfMap(Redis redis)` |

**理由**：每个方法单一职责，降低认知复杂度。

### D5: `DataSourceFactory.main()` 移除

**决策**：移除生产代码中残留的 `main` 方法（含 `System.out.println` 和 `e.printStackTrace()`）。

**理由**：生产代码不应包含 main 方法，调试用途应由单元测试替代。

### D6: `BaseCase` 重复代码抽象

**决策**：`getDataSource`/`getRedisDataSource` 和 `getConnection`/`getRedisConnection` 抽象为通用私有方法：

```java
private IDataSource getDataSource(Map<String, IDataSource> map, String tag)
private IDBConnection getConnection(Map<String, IDataSource> map, String tag)
```

**理由**：消除结构完全相同的重复代码。

### D7: `StringUtil` → `StringUtils`

**决策**：`BaseCase` 中的 `StringUtil.isEmpty()` 替换为 `org.apache.commons.lang3.StringUtils.isEmpty()`。

**理由**：编码规范要求使用 commons-lang3 `StringUtils`，项目已引入该依赖。

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 接口签名修改（`Class` → `Class<?>`）可能影响第三方调用 | `Class<?>` 是 `Class` 的超类型，调用方无需修改，向后兼容 |
| 方法拆分引入更多方法调用层级 | 子方法均为 `private`，不影响对外 API；可读性收益大于层级成本 |
| 测试代码大量裸类型修改可能引入编译错误 | 逐文件修改后 `mvn compile` 验证，再跑全量测试 |
| `RedisDataSource` 命令解析增加参数长度检查可能改变边界行为 | 检查不通过时返回 `Collections.emptyList()`，与现有异常路径行为一致 |
