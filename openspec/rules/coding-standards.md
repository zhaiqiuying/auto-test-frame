# 编码规范（Coding Standards）

> 本规范用于 OpenSpec apply 生成代码时遵守，同时作为代码评审的对照基准。

## 规范层级

本规范由两层组成，上层为基线，下层为项目增量：

| 层级 | 来源 | 说明 |
|------|------|------|
| **基线** | 《阿里巴巴 Java 开发手册（黄山版）》 | 全量遵循，不在此重复抄录。生成代码时以此为默认标准 |
| **增量（本文档）** | 项目特有约定 | 仅记录项目与手册的差异、手册未覆盖的项目专属约定、以及对手册条目的细化 |

> 阿里手册获取：[GitHub 开源版](https://github.com/alibaba/p3c) / [IDE 插件](https://plugins.jetbrains.com/plugin/10046-alibaba-java-coding-guidelines)

### 与阿里手册的差异项

以下条目项目**不遵循手册默认建议**，按项目既有约定执行：

| 条目 | 阿里手册建议 | 项目约定 | 原因 |
|------|-------------|----------|------|
| 接口命名 | 不推荐 `I` 前缀 | 使用 `I` 前缀（`IDataSource`、`IDBConnection`） | 项目历史风格统一，沿用不改 |
| 抽象类命名 | 无特殊要求 | `Abstract` 前缀 | 同上 |
| 日志变量 | `LOGGER` 大写 | `log` 小写（配合 `@Slf4j`） | Lombok 注解生成即为 `log` |
| 异常返回 | 不建议吞异常 | db 层 `executeWithoutException` 吞异常 + log + 返回空集合 | 框架容错设计，提供容错入口 |

### 对阿里手册的细化补充

以下条目手册有提及但项目需要更明确约束：

| 条目 | 细化内容 |
|------|----------|
| 泛型 | 禁止裸类型，必须带类型参数（手册「OOP」章节已提及，项目强制） |
| 空集合 | 用 `Collections.emptyList()`，禁止 `Collections.EMPTY_LIST`（raw type） |
| `@Override` | 接口实现必须加（手册「OOP-12」已提及，项目强制） |
| `equals` 方向 | 常量在前（手册「OOP-6」已提及，项目强制） |

---

## 1. 项目特有规范

以下为项目专属约定，阿里手册未覆盖。

### 1.1 泛型

**禁止使用裸类型（Raw Type）**，所有泛型必须带类型参数：

```java
// 正确
Map<String, Object> confMap = new HashMap<>();
List<List<Object>> result = new ArrayList<>();
Class<?> dataType = Redis.class;

// 禁止
Map confMap = new HashMap();
List result = new ArrayList();
Class dataType = Redis.class;
```

### 1.2 访问修饰符

所有类、方法、字段必须**显式声明访问修饰符**（`public` / `private` / `protected`）：

```java
// 正确
private static final Logger log = LoggerFactory.getLogger(Xxx.class);
public static final int DEFAULT_PORT = 6379;

// 禁止（包级私有不显式声明）
static final int DEFAULT_PORT = 6379;
```

### 1.3 常量命名

`static final` 常量使用 `UPPER_SNAKE_CASE`：

```java
private static final int REDIS_DEFAULT_PORT = 6379;
public static final String ENV_TEST = "test";
```

### 1.4 `@Override` 注解

实现接口方法或重写父类方法时，**必须加 `@Override`**：

```java
@Override
public Class<?> getDataType() {
    return Redis.class;
}
```

### 1.5 空集合返回

使用泛型安全的空集合方法，**禁止 `Collections.EMPTY_LIST`（raw type）**：

```java
// 正确
return Collections.emptyList();
return new ArrayList<>(0);

// 禁止
return Collections.EMPTY_LIST;
```

### 1.6 字符串判空

使用 Apache Commons Lang3 `StringUtils`（项目已引入），不要手动判空：

```java
// 正确
if (StringUtils.isNotBlank(url)) { ... }
if (StringUtils.isEmpty(host)) { ... }

// 禁止
if (url != null && !url.isEmpty()) { ... }
if (host == null || host.equals("")) { ... }
```

### 1.7 `equals` 方向

常量或已知非空对象放在 `equals` 前面，避免 NPE：

```java
// 正确
if ("redis".equals(type)) { ... }

// 禁止
if (type.equals("redis")) { ... }
```

### 1.8 参数校验

公共方法入参应做基本校验，不合法时抛出 `IllegalArgumentException`：

```java
public IDBConnection getConnection(Map<String, Object> confMap) {
    if (confMap == null || confMap.isEmpty()) {
        throw new IllegalArgumentException("confMap must not be null or empty");
    }
    // ...
}
```

---

## 2. 技术栈约束

| 约束项 | 要求 | 原因 |
|--------|------|------|
| Java 版本 | Java 8（`maven.compiler.source=8`） | 父 POM 固定，不使用 Java 9+ 语法 |
| 构建工具 | Maven 多模块（parent → btest / itest） | 项目结构约定 |
| 测试框架 | TestNG 7.1.0（**不使用 JUnit**） | 全项目统一 |
| 日志框架 | SLF4J + Logback（Spring Boot 管理） | 不引入其他日志实现 |
| Lombok | 允许使用 `@Data`、`@Slf4j`、`@Builder`、`@Getter`、`@Setter`、`@NoArgsConstructor`、`@AllArgsConstructor` | 已在 btest/pom.xml 声明 |
| 连接池 | Alibaba Druid 1.2.8（MySQL）；JedisPool（Redis） | 现有约定 |

### Java 8 禁用特性

以下 Java 9+ 语法**禁止使用**：

- `var` 局部变量类型推断（Java 10+）
- `record`（Java 14+）
- `switch` 表达式（Java 14+）
- `Text Block` `"""`（Java 15+）
- `Stream.toList()`（Java 16+），用 `.collect(Collectors.toList())` 代替
- `Optional.isEmpty()`（Java 11+），用 `!opt.isPresent()` 代替
- `Files.writeString`（Java 11+）

---

## 3. 包结构约定

### 3.1 根包

```
com.example.qa.btest.*     # 框架层
com.example.qa.itest.*      # 接口测试层
```

### 3.2 btest 子包结构

| 包路径 | 用途 |
|--------|------|
| `btest.common` | 全局配置(Config)和常量(Constant) |
| `btest.db` | 数据源核心抽象（IDataSource, IDBConnection, AbstractDataSource, DataSourceFactory） |
| `btest.db.{type}` | 数据源实现（如 `db.mysql`、`db.redis`） |
| `btest.engine.annatation` | 自定义注解 |
| `btest.engine.dataProvider.common` | DataProvider 接口和注册表(CommonDataProvider) |
| `btest.engine.dataProvider.{type}` | 注解 + Provider 实现（如 `dataProvider.csv`、`dataProvider.redis`） |
| `btest.engine.listener` | TestNG 监听器 |
| `btest.engine.asserts` | 断言工具 |
| `btest.engine.monitor` | 监控通知 |
| `btest.logging` | 日志配置 |
| `btest.model` | 数据模型（HttpModel, Executor, Runner, TestContext） |
| `btest.util` | 工具类 |

### 3.3 itest 子包结构

| 包路径 | 用途 |
|--------|------|
| `itest.cases` | 测试基类(BaseCase) |
| `itest.common` | itest 配置(Config) |
| `itest.model` | 上下文模型(User, CommonContext) |
| `itest.model.exception` | 自定义异常 |
| `itest.service` | 服务接口 + 实现 |
| `itest.util` | 工具类 |

---

## 4. 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| 接口 | `I` 前缀 | `IDataSource`、`IDBConnection` |
| 抽象类 | `Abstract` 前缀 | `AbstractDataSource`、`AbstractDbConnection` |
| 实现类 | 无特殊后缀，或 `Impl` 后缀 | `MysqlDataSource`、`CommonServiceImpl` |
| 注解 | 与 Provider 同包同名根 | `@Redis` + `RedisDataProvider` |
| 测试类 | `*Test` 后缀 | `RedisDataSourceTest` |
| 常量类 | `Constant` | `Constant` |
| 配置类 | `Config` | `Config` |
| 日志变量名 | 统一用 `log`（配合 `@Slf4j`） | `log.info("...", arg)` |
| 测试方法 | `test{行为描述}` | `testSetAndGetCommand` |
| 断言消息 | 中文描述 | `assertEquals(size, 1, "SET 应返回 1 行")` |

### 日志声明统一

**新增代码统一使用 `@Slf4j` 注解**，生成 `log` 变量。不再手动声明 `private static final Logger`。

> 注意：项目中历史代码存在 `LOGGER` / `log` 混用，后续重构时逐步统一，新增代码不要沿用旧风格。

---

## 5. 数据源扩展模式

新增数据源类型时，必须同时实现以下组件：

```
1. 注解（btest.engine.dataProvider.{type}）
   @Retention(RUNTIME) @Target(METHOD)
   字段以连接参数 + 查询参数为主

2. DataProvider（btest.engine.dataProvider.{type}）
   implements DataProvider
   getDataType() → 返回对应注解 Class<?>
   getObjects(MethodContext) → 解析注解 → 构建配置 Map
     → DataSourceFactory.createDataSource(confMap)
     → connection.execute(sql/command)
     → ListUtil.listToArray(list) 转 Object[][]

3. DataSource（btest.db.{type}）
   implements IDataSource（非 JDBC 数据源）
   或 extends AbstractDataSource（JDBC 数据源）
   getConnection() → 返回 IDBConnection 实现

4. 注册
   DataSourceFactory.createDataSource() 增加类型判别分支
   CommonDataProvider static 块增加 register(new XxxDataProvider())

5. context.yml 配置（如支持 tag 接入）
   新增 YAML key（如 redises:），Config.loadContext 增加解析
   CommonContext 新增 {type}DataSourceMap
```

### 工厂判别约定

`DataSourceFactory.createDataSource(Map<String, Object> confMap)` 通过 URL 字符串 `contains` 判别类型：

```java
String url = (String) confMap.getOrDefault("url", "");
if ("redis".contains(url)) {
    return new RedisDataSource(confMap);
} else if ("mysql".contains(url)) {
    return new MysqlDataSource(confMap);
}
```

新增类型在 URL 中包含类型关键字，工厂方法增加对应分支。

### 缓存约定

`DataSourceFactory.dataSourceMap` 使用 `ConcurrentHashMap<String, IDataSource>`，key 为配置 Map 的 MD5 摘要，`computeIfAbsent` 懒加载。**不要每次 new 数据源实例**。

---

## 6. 异常处理规范

### 6.1 分层策略

| 层级 | 策略 | 说明 |
|------|------|------|
| **数据源层（db）** | `executeWithoutException` 吞异常 + `log.error` + 返回 `Collections.emptyList()` | 提供容错执行入口 |
| **DataProvider 层** | 捕获后包装为 `RuntimeException` 重抛 | TestNG DataProvider 不支持受检异常 |
| **Service 层** | 自定义受检异常 `ServiceException` | 业务语义 |
| **工具类** | 不捕获，向上抛出 | 职责单一 |

### 6.2 禁止做法

- **禁止 `e.printStackTrace()`**：用 `log.error("描述", e)` 代替
- **禁止吞异常后不记录**：catch 块必须有日志记录
- **禁止 `catch (Exception e) {}` 空处理**：至少 `log.error`
- **禁止 `catch` 后丢弃 cause**：`new RuntimeException("desc", e)` 必须传入原始异常

### 6.3 返回值约定

| 场景 | 返回值 |
|------|--------|
| 数据源执行异常 | `Collections.emptyList()` |
| DataProvider 执行异常 | 重抛 `RuntimeException` |
| 工厂未匹配类型 | `return null`，调用方做 null 检查 |
| DataProvider 正常无数据 | `new Object[0][0]` |

---

## 7. 集合使用约定

| 场景 | 类型 | 原因 |
|------|------|------|
| 注册表 / 缓存 Map | `ConcurrentHashMap<K, V>` | 多线程安全 |
| 静态初始化 Map（只读） | `HashMap<K, V>` | 初始化后不变 |
| 返回空集合 | `Collections.emptyList()` | 泛型安全 |
| 数据源结果 | `List<List<Object>>` | 与 MySQL 结果格式一致 |

**所有集合声明必须带泛型参数。**

---

## 8. 编码级防范规则

> 以下规则来源于 code-review 评审规则（`references/rules-*.md`），提取阿里手册未覆盖、需项目级细化的防范规则。
> 阿里手册已覆盖的（switch break、返回空集合、PreparedStatement、硬编码凭据、未使用变量等）不再重复。

### 8.1 NPE 防范

| 规则 | 要求 | 示例 |
|------|------|------|
| 调用链结果使用前判空 | 链式调用前确认可能为 null 的环节 | `if (result != null && result.size() > 0)` |
| Optional 不链式调用 | Java 8 的 Optional 无 `isEmpty()`，用 `!opt.isPresent()` | `if (!opt.isPresent()) return;` |
| 工厂方法返回 null 时调用方必须检查 | `DataSourceFactory.createDataSource()` 可能返回 null | `IDataSource ds = factory.create(); if (ds == null) throw ...` |

### 8.2 并发安全防范

| 规则 | 要求 | 说明 |
|------|------|------|
| check-then-act 需同步 | 先检查再操作的模式需要原子性 | 用 `computeIfAbsent` 替代 `if-contains-else-put` |
| 不在多线程上下文写 HashMap/ArrayList | 确认调用上下文是否多线程 | 局部变量不受限 |
| 双重检查锁需 volatile | DCL 模式的实例字段必须加 `volatile` | 防止指令重排导致半初始化对象泄露 |

### 8.3 逻辑防错

| 规则 | 要求 | 说明 |
|------|------|------|
| 逻辑表达式加括号 | 避免运算符优先级导致非预期执行顺序 | `if ((a && b) \|\| c)` 而非 `if (a && b \|\| c)` |
| 边界检查用 `>=` / `<` 而非 `>` / `<=` | 数组/列表索引从 0 开始 | `for (int i = 0; i < list.size(); i++)` |
| 循环必须有终止条件 | 不写无终止的 `while(true)` | 除非有明确 break 退出 |
| 条件分支覆盖完整 | if-else / switch 覆盖所有可能的输入 | 缺少 default/else 分支时需确认 |

### 8.4 安全防范

| 规则 | 要求 | 说明 |
|------|------|------|
| 反序列化限制类型 | fastjson 关闭 autotype 或用白名单 | `ParserConfig.getGlobalInstance().addAccept(...)` |
| 敏感日志脱敏 | 密码/token 不出现在日志中 | `log.info("user={}, token=***", user)` |

### 8.5 死代码防范

| 规则 | 要求 | 说明 |
|------|------|------|
| 不在 return 后写代码 | return/throw/break/continue 后的代码不可达 | 编译器部分检查，但逻辑分支中的需人工注意 |
| 不保留大段注释代码 | 有意保留的需注释说明原因 | `// DEBUG: 保留用于排查 xxx 问题` |

### 8.6 类型安全防范

| 规则 | 要求 | 说明 |
|------|------|------|
| Object 到 Map/List 强转前必须 instanceof | 防止 ClassCastException | `if (obj instanceof Map) { Map<?,?> map = (Map<?,?>) obj; }` |
| @SuppressWarnings 需注释说明原因 | 标注为什么忽略警告 | `@SuppressWarnings("unchecked") // 反序列化返回类型不确定` |
| JSON/YAML 反序列化用带类型 API | 不用裸强转 | `new TypeReference<Map<String, Object>>(){}` |

---

## 9. 配置约定

### 9.1 YAML 配置

- 配置文件：`context.yml`
- 路径约定：`/{ENV_TYPE}/data/common/context.yml`
- 环境类型：`virtual`、`test`、`preonline`、`product`（由 `ENV_TYPE` 系统属性控制，默认 `test`）
- 同类型多实例用 List + `tag` 字段标识

```yaml
# context.yml 结构约定
mysqls:
  - tag: user_db
    url: jdbc:mysql://...
    user: xxx
    password: xxx
redises:
  - tag: cache_db
    host: 127.0.0.1
    port: 6379
    database: 0
```

### 9.2 Maven 依赖

- 版本统一在 `<properties>` 中声明，使用 `${property}` 引用
- Spring Boot parent 管理的依赖不声明 version
- 测试依赖 scope 必须为 `test`
- 传递依赖引入的 slf4j/logback 必须 exclusion

---

## 10. 测试约定

### 10.1 框架与工具

- **测试框架**：TestNG（`@BeforeClass`/`@AfterClass`/`@Test`）
- **Mock 方式**：嵌入式真实实现（如 `jedis-mock`），不使用 Mockito
- **断言**：`static org.testng.Assert.*` 静态导入

### 10.2 测试类规范

| 约定 | 要求 |
|------|------|
| 命名 | `*Test` 后缀 |
| 包路径 | 与被测类对应的 test 包 |
| 生命周期 | `@BeforeClass` 启动基础设施，`@AfterClass` 清理 |
| 端口/资源 | 每个测试类使用不同端口（避免冲突） |
| 断言消息 | 中文描述 |
| 测试方法命名 | `test{行为描述}` |

### 10.3 测试覆盖要求

- 每个新增公共方法至少一条测试用例
- 异常路径必须覆盖（如连接失败、命令不支持）
- 工厂方法的新增类型分支必须有测试
- DataProvider 的新增注解必须有数据注入测试

---

## 11. Spring 集成约定

| 约定 | 要求 |
|------|------|
| 配置类 | `@Configuration("name")` 带命名参数 |
| Bean 注入 | `@Bean` 方法 |
| Service | `@Service("name")` |
| 依赖注入 | `@Autowired` + `@Qualifier` |
| 测试基类 | 继承 `AbstractTestNGSpringContextTests` |
| Context 配置 | `@ContextConfiguration(locations = {"classpath:..."})` |

---

## 12. 资源管理

### 12.1 try-with-resources

JDBC `Statement`/`ResultSet` 必须使用 try-with-resources：

```java
try (Statement stmt = connection.createStatement()) {
    ResultSet rs = stmt.executeQuery(sql);
    // ...
}
```

### 12.2 连接池

- MySQL：Druid 连接池，通过 `AbstractDataSource` 封装
- Redis：`JedisPool`，通过 `RedisDataSource` 封装
- 连接使用完毕必须调用 `close()`

### 12.3 日志占位符

使用 SLF4J `{}` 占位符，**不要用字符串拼接**：

```java
// 正确
log.info("host={}, port={}, database={}", host, port, database);

// 禁止
log.info("host=" + host + ", port=" + port);
```

---

## 13. 代码模板

### 13.1 注解定义模板

所有数据驱动注解遵循统一格式：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Xxx {
    // 连接参数
    String host() default "";
    int port() default 0;
    // 查询参数
    String command() default "";
}
```

### 13.2 DataProvider 实现模板

```java
@Slf4j
public class XxxDataProvider implements DataProvider {

    @Override
    public Class<?> getDataType() {
        return Xxx.class;
    }

    @Override
    public Object[][] getObjects(MethodContext methodContext) {
        // 1. 解析注解
        // 2. 构建配置 Map<String, Object>
        // 3. DataSourceFactory.createDataSource(confMap)
        // 4. connection.execute(sql/command)
        // 5. ListUtil.listToArray(list) 转 Object[][]
    }
}
```

### 13.3 DataSource 实现模板（非 JDBC）

```java
@Slf4j
public class XxxDataSource implements IDataSource {

    private final Map<String, Object> config;

    public XxxDataSource(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public IDBConnection getConnection() {
        // 返回 IDBConnection 实现
    }
}
```

### 13.4 资源占位符

Maven 资源过滤使用 `${}` 占位符（非 Spring Boot 默认的 `@..@`）。

---

## 14. README 同步

涉及框架行为改动时，同步更新 README 对应章节。新增数据源时，README 需包含：

- 快速示例（注解方式 + 配置方式）
- 配置说明
- 支持的命令/SQL 类型

---

## 15. 确定性防线（VibeCoding Gate）

> 本章定义项目的三层确定性防线，确保代码质量不依赖 LLM 自律。
> 对应 AgentOS 的 Permission + Hook + Gate 机制，本项目因 IDE 无原生 hook，将防线下沉到 Git hook + CI 层。

### 15.1 三层防线架构

| 层 | 触发时机 | 脚本/配置 | 检查内容 | 强制级别 |
|---|---------|----------|---------|---------|
| **L0 — Git pre-commit** | `git commit` | `scripts/pre-commit` | 密钥扫描、禁止模式、编译、SpotBugs High | 本地强制 |
| **L1 — GitHub Actions CI** | push / PR | `.github/workflows/ci.yml` | 编译测试 + SpotBugs + PMD + Checkstyle（`-Pci`） | CI 强制 |
| **L2 — Gate 证据检查** | change 归档前 | `scripts/gate-check.sh` | tasks.md 完成度 + 构建 + 残留检查 + 全量静态分析 | 归档前强制 |

### 15.2 CI Profile 门禁机制

pom.xml 中定义 `ci` profile，控制静态检查的 `failOnViolation`：

| 属性 | 本地默认 | `-Pci` 激活 |
|------|---------|------------|
| `spotbugs.failOnError` | `false` | `true` |
| `spotbugs.threshold` | `Low`（报告全部） | `High`（只阻断高危） |
| `pmd.failOnViolation` | `false` | `true` |
| `checkstyle.failOnViolation` | `false` | `true` |

```bash
# 本地开发（宽松，只报告不阻断）
mvn clean test

# CI 模拟（严格，与 GitHub Actions 一致）
mvn -Pci clean test spotbugs:check pmd:check checkstyle:check
```

### 15.3 禁止模式清单

以下模式在 pre-commit hook 和 gate-check 中都会检查，main 源码中出现即 fail：

| 模式 | 原因 | 替代方案 |
|------|------|---------|
| `e.printStackTrace()` | 吞异常、无上下文 | `@Slf4j` + `log.error("msg", e)` |
| `System.out.println` | 无日志级别、不可控 | logger |
| `System.err.println` | 同上 | logger |
| `Collections.EMPTY_LIST` | 类型不安全 | `Collections.emptyList()` |
| `Collections.EMPTY_MAP` | 类型不安全 | `Collections.emptyMap()` |
| `@SuppressWarnings("unchecked")` | 掩盖类型问题 | 修复裸类型 |

### 15.4 密钥扫描清单

pre-commit hook 扫描以下模式，命中即拒绝提交：

| 模式 | 说明 |
|------|------|
| `sk-[A-Za-z0-9]{20,}` | OpenAI API Key 格式 |
| `password = "..."` | 明文密码 |
| `secret = "..."` | 明文密钥 |
| `api_key = "..."` | API Key |
| `token = "..."` | Token |

### 15.5 与 AgentOS vibecoding 的对照

| AgentOS 机制 | 本项目对应 | 差异说明 |
|-------------|-----------|---------|
| `opencode.json` permission | coding-standards.md 禁止事项 + Rules 注入 | 弱：LLM 自律非强制 |
| `SPEC.md` + `CHECKLIST.md` | OpenSpec spec + tasks.md | 已覆盖 |
| `context-pack.md` 上下文隔离 | OpenSpec change context | 部分覆盖：结构化约束非文件级隔离 |
| `gate-checklist.md` + 证据 | `scripts/gate-check.sh` + tasks.md `[x]` | 强：CI 级确定性脚本 |
| `hooks/pre-commit.sample` | `scripts/pre-commit` + `install-hooks.sh` | 强：本地确定性脚本 |
| `AGENT_LOG.md` + `context-snapshot.md` | OpenSpec change proposal + design | 已覆盖 |
