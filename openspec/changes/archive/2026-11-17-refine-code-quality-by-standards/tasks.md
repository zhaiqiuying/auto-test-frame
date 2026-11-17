# Tasks

## 1. 接口层泛型修复

- [x] 1.1 修改 `IDataSource.java` — 无裸类型（当前已合规，确认即可）
- [x] 1.2 修改 `IDBConnection.java` — 确认无裸类型（当前已合规）
- [x] 1.3 修改 `DataProvider.java` 接口 — `getDataType()` 返回类型 `Class` → `Class<?>`，验证 `mvn -pl btest compile` 通过
- [x] 1.4 修改 `Execute.java` — 确认无裸类型（当前已合规）

## 2. DataSourceFactory 重构

- [x] 2.1 移除 `main()` 方法（含 `System.out.println` 和 `e.printStackTrace()`），验证 `mvn -pl btest compile` 通过
- [x] 2.2 `getDataSource(Map)` 和 `createDataSource(Map)` 参数类型 `Map` → `Map<String, Object>`，验证编译通过
- [x] 2.3 提取魔法值为常量：`"redis"`、`"mysql"`、`"url"`、`"host"`、`"user"` → `private static final String` 常量（`UPPER_SNAKE_CASE`），验证编译通过
- [x] 2.4 `dataSourceMap` 字段改为 `private static final`，验证编译通过
- [x] 2.5 main 中的裸类型 `List list` 修复（随 main 移除自动消除）

## 3. RedisDataSource 重构

- [x] 3.1 替换手动 Logger 为 `@Slf4j`（`RedisDataSource` 和内部类 `RedisConnection`），删除 `import org.slf4j.Logger/LoggerFactory`，验证编译通过
- [x] 3.2 构造函数参数 `Map conf` → `Map<String, Object> conf`，验证编译通过
- [x] 3.3 提取魔法值为常量：`"redis://"`、`"127.0.0.1"`、`6379`、`0`、`2000`、`"url"`、`"host"`、`"port"`、`"password"`、`"database"`、`"maxTotal"`、`"maxIdle"`、`"maxWait"`，验证编译通过
- [x] 3.4 构造函数拆分为 `parseRedisUrl(String url)`、`parseHostPort(Map conf)`、`buildPoolConfig(Map conf)` 子方法，验证编译通过
- [x] 3.5 `Collections.EMPTY_LIST`（2处）替换为 `Collections.emptyList()`，验证编译通过
- [x] 3.6 日志字符串拼接 `"executeWithoutException error, command: " + command` 改为 `{}` 占位符，验证编译通过
- [x] 3.7 命令解析增加参数长度检查（`parts[1]`/`parts[2]`/`parts[3]` 访问前检查 `parts.length`），不足时返回 `Collections.emptyList()`，验证 `RedisDataSourceTest` 全部通过
- [x] 3.8 `executeCommandToList` 中裸类型 `List` → `List<List<Object>>`，移除 `@SuppressWarnings("unchecked")`，验证编译通过
- [x] 3.9 `jedisPool` 字段加 `final`，验证编译通过

## 4. CommonDataProvider 重构

- [x] 4.1 替换手动 `LOGGER` 为 `@Slf4j`，统一变量名 `log`，验证编译通过
- [x] 4.2 `dataProviderMap` 声明改为 `private static final Map<Class<?>, DataProvider>`，`Class` → `Class<?>`，验证编译通过
- [x] 4.3 `genData` 中裸类型 `Class c` → `Class<?> c`，验证编译通过
- [x] 4.4 提取魔法值：`"url"`、`"user"`、`"password"`、`"redis_host"`、`"redis_port"`、`"redis_password"`、`"mysql_url"`、`"mysql_user"`、`"mysql_password"` → 常量，验证编译通过

## 5. RedisDataProvider 重构

- [x] 5.1 替换手动 `LOGGER` 为 `@Slf4j`，统一变量名 `log`，验证编译通过
- [x] 5.2 `getDataType()` 返回类型 `Class` → `Class<?>`，补加 `@Override`，验证编译通过
- [x] 5.3 `getObjects(MethodContext)` 补加 `@Override`，增加 `methodContext` null 检查，验证编译通过
- [x] 5.4 提取魔法值：`"redis_host"`、`"redis_port"`、`"redis_password"`、`"url"`、`"host"`、`"port"`、`"password"`、`"database"`、`6379` → 常量，验证编译通过
- [x] 5.5 `getObjects` 拆分出 `buildConfMap(Redis redis)` 子方法，验证编译通过
- [x] 5.6 裸类型 `List list` → `List<?> list`，移除 `@SuppressWarnings("unchecked")`，验证编译通过
- [x] 5.7 日志 `LOGGER.info` → `log.info`，验证编译通过

## 6. BaseCase 重构

- [x] 6.1 `StringUtil.isEmpty()` 替换为 `org.apache.commons.lang3.StringUtils.isEmpty()`，验证编译通过
- [x] 6.2 提取魔法值：`"ENV_TYPE"`、`"test"`、`"logback.configurationFile"`、`"execute_device_tag"` 等 suite 参数 key → 常量，验证编译通过
- [x] 6.3 `THOUSAND` 改为 `private static final int MILLIS_PER_SECOND = 1000`，验证编译通过
- [x] 6.4 `beforeSuite` 拆分为 `buildDevice()`、`buildApp()`、`buildEnv()`、`buildUser()` 子方法，验证编译通过
- [x] 6.5 `getDataSource`/`getRedisDataSource` 抽象为 `getDataSource(Map<String, IDataSource>, String tag)`，`getConnection`/`getRedisConnection` 同理抽象，验证编译通过
- [x] 6.6 `equals` 调用调整常量在前：`userTag.equals(...)` → `"tag".equals(...)` 或 `Objects.equals()`，验证编译通过
- [x] 6.7 `sleepSeconds` 参数校验 `second < 0` 抛 `IllegalArgumentException`，验证编译通过
- [x] 6.8 `InterruptedException` catch 中恢复中断状态 `Thread.currentThread().interrupt()`，验证编译通过
- [x] 6.9 `getDevice` 返回 null 改为抛 `UnsupportedOperationException`，验证编译通过

## 7. Config 重构

- [x] 7.1 `loadContext` 中裸类型修复：`Map map` → `Map<String, Object> map`，`(List) map.get(...)` → `(List<?>)`，`(Map) item` → `(Map<String, Object>)`，验证编译通过
- [x] 7.2 提取魔法值：`"users"`、`"devices"`、`"envs"`、`"apps"`、`"mysqls"`、`"redises"`、`"tag"`、`"test"` → 常量，验证编译通过
- [x] 7.3 mysqls/redises 加载逻辑抽象为 `loadDataSources(String key, Map<String, IDataSource> targetMap)`，验证编译通过
- [x] 7.4 `map.size() == 0` 改为 `map.isEmpty()`，验证编译通过
- [x] 7.5 `.stream().forEach()` 简化为 `.forEach()`，验证编译通过

## 8. CommonContext 重构

- [x] 8.1 提取初始容量魔法值 `100`/`10` 为常量或使用默认容量，验证编译通过

## 9. 测试代码修复

- [x] 9.1 `RedisDataSourceTest` 所有裸类型 `List` → `List<?>`（~15处），验证 `mvn -pl btest test -Dtest=RedisDataSourceTest` 通过
- [x] 9.2 `RedisDataSourceTest` 中 `field.equals("f1")` → `"f1".equals(field)`（2处），验证测试通过
- [x] 9.3 `RedisTagConnectionTest` 裸类型修复：`Map map` → `Map<String, Object>`、`(List)` → `(List<?>)`、`(Map)` → `(Map<String, Object>)`，验证测试通过
- [x] 9.4 `RedisTagConnectionTest` 提取魔法值 `"cache_db"`、`"redises"`、`"tag"` 为常量，验证测试通过
- [x] 9.5 `RedisDataProviderTest` 注解中的 `port = 6382` 改为 `port = REDIS_PORT`（`static final` 常量引用），验证测试通过

## 10. 构建验证

- [x] 10.1 运行 `mvn -B clean test`，验证 btest 15 tests + itest 6 tests 全部通过，0 failures
- [x] 10.2 确认无 `@SuppressWarnings("unchecked")` 残留（`grep -r "SuppressWarnings" --include="*.java"` 结果为空或已合理）
- [x] 10.3 确认无 `e.printStackTrace()` 残留（`grep -r "printStackTrace" --include="*.java"` 结果为空）
- [x] 10.4 确认无 `Collections.EMPTY_LIST` 残留（`grep -r "EMPTY_LIST" --include="*.java"` 结果为空）
- [x] 10.5 确认无手动 `Logger` 声明残留（`grep -r "private static final Logger" --include="*.java"` 仅历史文件或已替换）
