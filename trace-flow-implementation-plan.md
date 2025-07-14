# Arthas trace-flow 命令实施文档

## 📋 文档概述

本文档基于 `http-trace-requirements-MVP-CN.md` 需求文档，制定 trace-flow 命令的具体实施计划，包括技术架构、开发计划、测试策略和交付标准。

---

## 🏗️ 技术架构设计

### 核心模块架构
```
trace-flow-command
├── command/                    # 命令行模块
│   ├── TraceFlowCommand.java
│   ├── CommandLineParser.java
│   └── OutputFormatter.java
├── config/                     # 配置管理模块
│   ├── ProbeConfigLoader.java
│   ├── ProbeConfig.java
│   └── MetricConfig.java
├── expression/                 # 表达式解析模块
│   ├── SourceExpressionParser.java
│   ├── FormulaExpressionParser.java
│   └── FilterEngine.java
├── probe/                      # 探针模块
│   ├── ProbeManager.java
│   ├── DatabaseProbe.java
│   ├── HttpServerProbe.java
│   ├── HttpClientProbe.java
│   └── FileOperationsProbe.java
├── trace/                      # 链路跟踪模块
│   ├── TraceManager.java
│   ├── TraceContext.java
│   └── TraceNode.java
└── metric/                     # 指标采集模块
    ├── MetricCollector.java
    ├── ExecutionContext.java
    └── MetricsContext.java
```

### 技术栈选择
- **基础框架**: Arthas 现有架构
- **命令行解析**: picocli
- **表达式解析**: JavaScript引擎 (Nashorn/GraalVM) + 自定义简单解析器
- **方法拦截**: AspectJ / Arthas现有拦截机制
- **配置格式**: JSON
- **输出格式**: 控制台树状 + JSON文件

---

## 📅 分阶段开发计划

### 阶段1: 基础框架 (2周)
**目标**: 建立命令框架和配置解析能力

#### 开发任务
| 任务 | 负责人 | 工期 | 依赖 |
|------|--------|------|------|
| 命令行框架搭建 | 开发者A | 3天 | - |
| 配置文件加载器 | 开发者B | 3天 | - |
| 简单表达式解析器 | 开发者A | 4天 | 命令行框架 |
| 基础拦截机制 | 开发者B | 4天 | 配置加载器 |

#### 交付标准
- [ ] 命令行参数正确解析 (`tf --help`, `tf -n 5`, `tf --filter`)
- [ ] 内置探针配置文件正确加载
- [ ] 简单表达式解析器支持内置变量 (`startTime`, `endTime`, `threadName`)
- [ ] 基础方法拦截机制工作正常

#### 测试验证
```bash
# 命令行测试
tf --help
tf --version

# 配置加载测试
tf --list-probes
tf --show-config database

# 单元测试覆盖率 >= 80%
mvn test -Dtest=*Stage1*Test
```

---

### 阶段2: Database探针 (3周)
**目标**: 完整实现Database探针，验证指标采集流程

#### 开发任务
| 任务 | 负责人 | 工期 | 依赖 |
|------|--------|------|------|
| Source表达式解析器完善 | 开发者A | 5天 | 阶段1完成 |
| Formula表达式解析器 | 开发者B | 5天 | 阶段1完成 |
| Database探针实现 | 开发者A | 4天 | 表达式解析器 |
| 指标采集引擎 | 开发者B | 4天 | Database探针 |
| 控制台输出格式化 | 开发者A | 3天 | 指标采集引擎 |

#### 交付标准
- [ ] Source表达式支持: `this`, `args`, `returnValue`, `exception`, `method`
- [ ] Formula表达式支持: 基础算术运算、条件判断、字符串处理
- [ ] Database探针采集8个核心指标
- [ ] 控制台输出清晰的单探针结果
- [ ] 过滤功能基本可用

#### 测试验证
```bash
# Database探针测试
tf --filter "executionTime > 100"
tf --filter "operationType == 'SELECT'"
tf --filter "isSlowQuery == true"

# 集成测试
mvn test -Dtest=*DatabaseProbe*Test
```

#### 测试用例
```java
@Test
public void testDatabaseProbe() {
    // 执行SQL操作
    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
    stmt.setInt(1, 123);
    ResultSet rs = stmt.executeQuery();
    
    // 验证指标采集
    List<Metric> metrics = getCollectedMetrics();
    assertThat(metrics).extracting("name")
        .contains("sql", "executionTime", "operationType", "isSlowQuery");
    assertThat(getMetric("operationType").getValue()).isEqualTo("SELECT");
}
```

---

### 阶段3: 多探针协同 (3周)
**目标**: 实现HTTP Server探针，验证链路跟踪

#### 开发任务
| 任务 | 负责人 | 工期 | 依赖 |
|------|--------|------|------|
| HTTP Server探针实现 | 开发者A | 5天 | 阶段2完成 |
| 链路跟踪管理器 | 开发者B | 5天 | 阶段2完成 |
| 多探针协同机制 | 开发者A | 4天 | HTTP探针 + 链路管理器 |
| 树状输出格式化 | 开发者B | 4天 | 多探针协同 |
| Trace ID生成和传递 | 开发者A | 3天 | 链路跟踪管理器 |

#### 交付标准
- [ ] HTTP Server探针采集12个核心指标
- [ ] 链路跟踪正确关联HTTP请求和数据库操作
- [ ] Trace ID正确生成和传递
- [ ] 树状输出格式清晰展示调用层次
- [ ] 多探针过滤功能正常

#### 测试验证
```bash
# 多探针协同测试
tf --filter "url.startsWith('/api/users')"
tf --filter "isSlowQuery == true || isSlowRequest == true"
tf --verbose

# 端到端测试
curl http://localhost:8080/api/users/123
```

#### 测试用例
```java
@RestController
public class TestController {
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id); // 触发HTTP + Database链路
    }
}

@Test
public void testHttpDatabaseTrace() {
    // 发送HTTP请求
    restTemplate.getForObject("/api/users/123", String.class);
    
    // 验证链路跟踪
    TraceResult result = getLastTrace();
    assertNotNull(result.getTraceId());
    assertThat(result.getNodes()).hasSize(2); // HTTP + Database
    assertThat(result.getTotalTime()).isGreaterThan(0);
}
```

---

### 阶段4: 完整MVP (4周)
**目标**: 实现所有探针，完成MVP功能

#### 开发任务
| 任务 | 负责人 | 工期 | 依赖 |
|------|--------|------|------|
| HTTP Client探针实现 | 开发者A | 6天 | 阶段3完成 |
| File Operations探针实现 | 开发者B | 6天 | 阶段3完成 |
| 完整过滤引擎 | 开发者A | 5天 | 所有探针完成 |
| JSON文件输出 | 开发者B | 4天 | 完整过滤引擎 |
| 性能优化 | 开发者A | 4天 | JSON输出完成 |
| 集成测试和文档 | 开发者B | 3天 | 性能优化完成 |

#### 交付标准
- [ ] 4个探针全部实现并正常工作
- [ ] 复杂过滤表达式正确执行
- [ ] JSON文件输出格式正确
- [ ] 性能开销控制在可接受范围内
- [ ] 完整的用户文档和示例

#### 测试验证
```bash
# 完整链路测试
tf --filter "url.startsWith('/api/users')" --output-file result.json

# 复杂过滤测试
tf --filter "isSlowQuery == true && isLargeFile == true"
tf --filter "operationType in ['READ', 'WRITE'] && executionTime > 500"

# 性能测试
tf --benchmark --requests 1000
```

---

### 阶段5: 增强功能 (3周) - 可选
**目标**: 添加高级功能和优化

#### 开发任务
- 堆栈跟踪功能
- 表达式缓存优化
- 用户自定义配置支持
- 详细模式输出

---

## 🧪 测试策略

### 单元测试
- **覆盖率要求**: >= 80%
- **重点模块**: 表达式解析器、指标采集器、过滤引擎
- **测试框架**: JUnit 5 + Mockito + AssertJ

### 集成测试
- **探针集成测试**: 每个探针的完整功能测试
- **多探针协同测试**: 链路跟踪和指标关联测试
- **配置加载测试**: 各种配置文件的加载和解析测试

### 端到端测试
- **真实场景测试**: 使用真实的Web应用进行完整链路测试
- **性能测试**: 验证性能开销在可接受范围内
- **兼容性测试**: 不同Java版本和框架的兼容性

### 测试环境
- **开发环境**: 本地开发机器
- **集成环境**: Docker容器化测试环境
- **性能环境**: 模拟生产环境的性能测试

---

## 📦 交付物清单

### 代码交付物
- [ ] trace-flow命令源代码
- [ ] 4个内置探针配置文件
- [ ] 完整的单元测试和集成测试
- [ ] 性能测试报告

### 文档交付物
- [ ] 用户使用手册
- [ ] 开发者文档
- [ ] API文档
- [ ] 故障排查指南

### 部署交付物
- [ ] 编译后的jar包
- [ ] Docker镜像
- [ ] 部署脚本
- [ ] 配置文件模板

---

## ⚡ 风险控制

### 技术风险
- **表达式解析复杂度**: 采用分层解析策略，简单表达式优先
- **性能开销**: 每阶段都进行性能测试，及时优化
- **兼容性问题**: 早期进行多环境测试

### 进度风险
- **需求变更**: 严格控制需求变更，重大变更需重新评估
- **技术难点**: 预留缓冲时间，及时寻求技术支持
- **资源不足**: 关键路径任务安排经验丰富的开发者

### 质量风险
- **测试覆盖不足**: 制定详细的测试计划，定期检查覆盖率
- **集成问题**: 每个阶段都进行集成测试
- **用户体验**: 定期进行用户体验测试和反馈收集

---

## 📊 项目里程碑

| 里程碑 | 时间节点 | 关键交付物 | 验收标准 |
|--------|----------|------------|----------|
| M1: 基础框架完成 | 第2周末 | 命令框架+配置加载 | 命令行正常工作，配置正确加载 |
| M2: Database探针完成 | 第5周末 | 完整Database探针 | 单探针功能完全正常 |
| M3: 多探针协同完成 | 第8周末 | HTTP+Database协同 | 链路跟踪正常工作 |
| M4: MVP功能完成 | 第12周末 | 完整MVP功能 | 所有需求功能正常 |
| M5: 增强功能完成 | 第15周末 | 高级功能 | 性能和用户体验优化 |

**总开发周期**: 12-15周
**核心团队规模**: 2-3名开发者
**预计工作量**: 240-360人天

---

## 🛠️ 开发环境配置

### 开发工具要求
- **JDK**: 1.8+
- **构建工具**: Maven 3.6+
- **IDE**: IntelliJ IDEA / Eclipse
- **版本控制**: Git
- **测试框架**: JUnit 5, Mockito, AssertJ

### 项目结构
```
arthas-trace-flow/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/taobao/arthas/core/command/trace/
│   │   └── resources/
│   │       └── probes/
│   │           ├── http-server-probe.json
│   │           ├── database-probe.json
│   │           ├── http-client-probe.json
│   │           └── file-operations-probe.json
│   └── test/
│       ├── java/
│       └── resources/
├── docs/
│   ├── user-guide.md
│   ├── developer-guide.md
│   └── api-reference.md
└── scripts/
    ├── build.sh
    ├── test.sh
    └── deploy.sh
```

### Maven依赖配置
```xml
<dependencies>
    <!-- Arthas Core -->
    <dependency>
        <groupId>com.taobao.arthas</groupId>
        <artifactId>arthas-core</artifactId>
        <version>${arthas.version}</version>
    </dependency>

    <!-- 命令行解析 -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli</artifactId>
        <version>4.6.3</version>
    </dependency>

    <!-- JSON处理 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.83</version>
    </dependency>

    <!-- 表达式解析 -->
    <dependency>
        <groupId>org.graalvm.js</groupId>
        <artifactId>js</artifactId>
        <version>21.3.0</version>
    </dependency>

    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 📋 详细开发任务

### 阶段1详细任务分解

#### 任务1.1: 命令行框架搭建 (3天)
**负责人**: 开发者A
**详细任务**:
- [ ] 创建TraceFlowCommand类，集成picocli
- [ ] 实现基本参数解析 (-n, --filter, --output-file, --verbose)
- [ ] 添加帮助信息和版本信息
- [ ] 集成到Arthas命令体系

**验收标准**:
```bash
tf --help          # 显示帮助信息
tf --version       # 显示版本信息
tf -n 5            # 参数解析正确
tf --filter "test" # 参数解析正确
```

**实现要点**:
```java
@Command(name = "trace-flow", aliases = {"tf"},
         description = "跟踪HTTP请求的完整执行链路")
public class TraceFlowCommand extends AnnotatedCommand {
    @Option(names = {"-n", "--count"}, description = "跟踪次数")
    private int count = 1;

    @Option(names = {"--filter"}, description = "过滤表达式")
    private String filter;

    @Option(names = {"--output-file"}, description = "输出文件")
    private String outputFile;

    @Option(names = {"--verbose"}, description = "详细模式")
    private boolean verbose;
}
```

#### 任务1.2: 配置文件加载器 (3天)
**负责人**: 开发者B
**详细任务**:
- [ ] 设计ProbeConfig和MetricConfig数据结构
- [ ] 实现JSON配置文件解析器
- [ ] 创建4个内置探针配置文件
- [ ] 实现配置文件验证和错误处理

**验收标准**:
```java
@Test
public void testConfigLoader() {
    ProbeConfigLoader loader = new ProbeConfigLoader();
    List<ProbeConfig> configs = loader.loadBuiltinProbes();
    assertEquals(4, configs.size());

    ProbeConfig dbConfig = configs.stream()
        .filter(c -> "Database探针".equals(c.getName()))
        .findFirst().orElse(null);
    assertNotNull(dbConfig);
    assertTrue(dbConfig.isEnabled());
}
```

**数据结构设计**:
```java
public class ProbeConfig {
    private String name;
    private String description;
    private boolean enabled;
    private List<MetricConfig> metrics;
    private OutputConfig output;
    private List<FilterConfig> filters;
}

public class MetricConfig {
    private String name;
    private String description;
    private List<TargetConfig> targets;
    private String source;
    private String formula;
    private String type;
    private String unit;
    private String capturePoint;
}
```

#### 任务1.3: 简单表达式解析器 (4天)
**负责人**: 开发者A
**详细任务**:
- [ ] 实现SourceExpressionParser基础版本
- [ ] 支持内置变量解析 (startTime, endTime, threadName)
- [ ] 实现ExecutionContext数据结构
- [ ] 添加表达式解析异常处理

**验收标准**:
```java
@Test
public void testSimpleSourceParser() {
    ExecutionContext context = new ExecutionContext();
    context.setStartTime(1000L);
    context.setEndTime(2000L);
    context.setThreadName("main");

    SourceExpressionParser parser = new SourceExpressionParser();
    assertEquals(1000L, parser.parse("startTime", context));
    assertEquals(2000L, parser.parse("endTime", context));
    assertEquals("main", parser.parse("threadName", context));
}
```

#### 任务1.4: 基础拦截机制 (4天)
**负责人**: 开发者B
**详细任务**:
- [ ] 设计ProbeManager和拦截器接口
- [ ] 实现基础的方法拦截框架
- [ ] 创建ExecutionContext构建器
- [ ] 添加拦截器注册和管理机制

**验收标准**:
```java
@Test
public void testBasicInterception() {
    ProbeManager manager = new ProbeManager();
    TestProbe probe = new TestProbe();
    manager.registerProbe(probe);

    // 执行被拦截的方法
    testMethod();

    // 验证拦截器被调用
    assertTrue(probe.isIntercepted());
}
```

### 阶段2详细任务分解

#### 任务2.1: Source表达式解析器完善 (5天)
**负责人**: 开发者A
**详细任务**:
- [ ] 扩展SourceExpressionParser支持对象属性访问
- [ ] 实现方法调用解析 (this.toString(), args[0].getValue())
- [ ] 添加条件表达式支持 (exception != null ? 'ERROR' : 'OK')
- [ ] 实现类型检查和安全访问

**验收标准**:
```java
@Test
public void testCompleteSourceParser() {
    ExecutionContext context = createMockContext();
    SourceExpressionParser parser = new SourceExpressionParser();

    assertEquals("SELECT * FROM users", parser.parse("this.toString()", context));
    assertEquals("getValue", parser.parse("args[0].getValue()", context));
    assertEquals("ERROR", parser.parse("exception != null ? 'ERROR' : 'OK'", context));
}
```

#### 任务2.2: Formula表达式解析器 (5天)
**负责人**: 开发者B
**详细任务**:
- [ ] 实现FormulaExpressionParser基于JavaScript引擎
- [ ] 支持基础算术运算 (metrics.endTime - metrics.startTime)
- [ ] 实现条件判断 (metrics.executionTime > 1000 ? 'SLOW' : 'FAST')
- [ ] 添加字符串处理函数支持

**验收标准**:
```java
@Test
public void testFormulaParser() {
    MetricsContext context = new MetricsContext();
    context.addMetric("endTime", 2000L);
    context.addMetric("startTime", 1000L);
    context.addMetric("executionTime", 1000L);

    FormulaExpressionParser parser = new FormulaExpressionParser();
    assertEquals(1000L, parser.parse("metrics.endTime - metrics.startTime", context));
    assertEquals("SLOW", parser.parse("metrics.executionTime > 500 ? 'SLOW' : 'FAST'", context));
}
```

#### 任务2.3: Database探针实现 (4天)
**负责人**: 开发者A
**详细任务**:
- [ ] 实现DatabaseProbe拦截器
- [ ] 集成database-probe.json配置
- [ ] 实现8个核心指标的采集
- [ ] 添加SQL解析和分类逻辑

**验收标准**:
```java
@Test
public void testDatabaseProbe() {
    // 执行SQL操作
    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
    stmt.setInt(1, 123);
    ResultSet rs = stmt.executeQuery();

    // 验证指标采集
    List<Metric> metrics = getCollectedMetrics();
    assertThat(metrics).extracting("name")
        .contains("sql", "executionTime", "operationType", "isSlowQuery");
    assertThat(getMetric("operationType").getValue()).isEqualTo("SELECT");
}
```

#### 任务2.4: 指标采集引擎 (4天)
**负责人**: 开发者B
**详细任务**:
- [ ] 实现MetricCollector核心引擎
- [ ] 支持before/after采集时机
- [ ] 实现指标依赖解析和计算顺序
- [ ] 添加指标缓存和性能优化

**核心实现**:
```java
public class MetricCollector {
    public Map<String, Object> collectMetrics(List<MetricConfig> configs,
                                            ExecutionContext context) {
        Map<String, Object> beforeMetrics = collectBeforeMetrics(configs, context);
        Map<String, Object> afterMetrics = collectAfterMetrics(configs, context);
        Map<String, Object> calculatedMetrics = calculateFormulaMetrics(configs,
                                                                       beforeMetrics,
                                                                       afterMetrics);

        Map<String, Object> allMetrics = new HashMap<>();
        allMetrics.putAll(beforeMetrics);
        allMetrics.putAll(afterMetrics);
        allMetrics.putAll(calculatedMetrics);

        return allMetrics;
    }
}
```

#### 任务2.5: 控制台输出格式化 (3天)
**负责人**: 开发者A
**详细任务**:
- [ ] 实现ConsoleOutputFormatter
- [ ] 支持单探针结果的清晰显示
- [ ] 添加颜色和格式化支持
- [ ] 实现过滤结果的输出

**输出格式示例**:
```
[DATABASE] SELECT * FROM users WHERE id=? | Time: 800ms | Rows: 1 | Type: SELECT
└── SQL: SELECT * FROM users WHERE id=123
```

---

## 🔍 质量保证

### 代码质量标准
- **代码覆盖率**: >= 80%
- **代码规范**: 遵循阿里巴巴Java开发手册
- **性能要求**: 单次拦截开销 < 1ms
- **内存使用**: 增加内存使用 < 10MB

### 代码审查流程
1. **开发者自测**: 完成功能开发和单元测试
2. **同行审查**: 另一名开发者进行代码审查
3. **集成测试**: 通过所有集成测试用例
4. **性能测试**: 验证性能指标符合要求

### 持续集成配置
```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 8
      uses: actions/setup-java@v2
      with:
        java-version: '8'
    - name: Run tests
      run: mvn clean test
    - name: Generate coverage report
      run: mvn jacoco:report
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v1
```

---

## 📈 项目监控

### 开发进度跟踪
- **每日站会**: 同步进度和问题
- **周报**: 每周五提交进度报告
- **里程碑评审**: 每个阶段结束进行评审

### 质量指标监控
- **代码覆盖率**: 每次提交自动检查
- **性能指标**: 每个阶段进行性能测试
- **Bug数量**: 跟踪和分析Bug趋势

### 风险预警机制
- **进度延期**: 超过计划1天触发预警
- **质量下降**: 覆盖率低于80%触发预警
- **性能劣化**: 性能指标超标触发预警

---

## 📞 联系方式

**项目经理**: [姓名] - [邮箱] - [电话]
**技术负责人**: [姓名] - [邮箱] - [电话]
**测试负责人**: [姓名] - [邮箱] - [电话]

**项目仓库**: [Git仓库地址]
**文档地址**: [文档地址]
**问题跟踪**: [Issue跟踪地址]
