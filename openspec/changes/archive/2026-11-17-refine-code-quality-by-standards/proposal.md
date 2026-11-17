# Proposal

## Why

本次 PR（`feature/support-redis-datasource`）新增的 Redis 数据源代码在编码规范审查中发现大量违规项：约 40 处裸类型（Raw Type）、5 处手动声明 Logger 未用 `@Slf4j`、2 处 `Collections.EMPTY_LIST`、1 处 `e.printStackTrace()`、~50 处魔法值、以及过长方法和重复代码。这些问题影响代码可维护性和团队评审效率，需要在合入 main 前按照编码规范（阿里手册基线 + 项目增量）统一修正。

## What Changes

- **消除裸类型**：所有 `Map`、`List`、`Class` 补全泛型参数（`Map<String, Object>`、`List<?>`、`Class<?>`），移除不必要的 `@SuppressWarnings("unchecked")`
- **日志统一 `@Slf4j`**：`RedisDataSource`、`CommonDataProvider`、`RedisDataProvider` 中手动声明的 `Logger` 替换为 `@Slf4j` 注解
- **禁止 `Collections.EMPTY_LIST`**：替换为 `Collections.emptyList()`
- **禁止 `e.printStackTrace()`**：`DataSourceFactory.main()` 中替换为 `log.error`（或移除 main 方法）
- **日志占位符**：`RedisDataSource:273` 的字符串拼接改为 SLF4J `{}` 占位符
- **`@Override` 补全**：`RedisDataProvider.getDataType()`、`getObjects()` 补加 `@Override`
- **`equals` 常量在前**：`RedisDataSourceTest` 和 `BaseCase` 中的 `equals` 调用调整常量位置
- **魔法值提取为常量**：`DataSourceFactory`、`RedisDataSource`、`Config`、`BaseCase` 中的字符串/数值魔法值提取为 `private static final` 常量
- **过长方法拆分**：`RedisDataSource` 构造函数、`Config.loadContext`、`BaseCase.beforeSuite` 拆分为子方法
- **重复代码消除**：`Config` 中 mysqls/redises 加载逻辑抽象为通用方法；`BaseCase` 中 `getDataSource`/`getRedisDataSource` 抽象为通用方法
- **移除 `DataSourceFactory.main()`**：生产代码中残留的 main 测试方法移除
- **潜在 NPE/ArrayIndexOutOfBounds 修复**：`RedisDataSource` 命令解析增加参数长度检查
- **`StringUtil` → `StringUtils`**：`BaseCase` 中统一使用 commons-lang3 `StringUtils`

## Capabilities

### New Capabilities

无。本次变更为纯代码质量重构，不引入新能力。

### Modified Capabilities

无。本次变更不改变 `redis-datasource` 的对外行为契约，仅优化内部代码质量。

> `skip_specs: true` — 纯重构，无 spec 级别行为变更。

## Impact

- **影响文件**：12 个 Java 文件（btest 5 个 + itest 4 个 + 测试 3 个）
- **影响模块**：`btest`（DataSourceFactory、RedisDataSource、CommonDataProvider、RedisDataProvider、Redis 注解）、`itest`（BaseCase、Config、CommonContext）
- **不影响行为**：所有重构保持对外接口签名和运行时行为不变
- **依赖无变更**：不新增/移除 Maven 依赖
- **测试不变**：测试用例逻辑不变，仅修正测试代码中的裸类型和 equals 方向
