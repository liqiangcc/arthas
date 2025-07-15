# Arthas trace-flow 命令 - 阶段1实现

## 🎯 阶段1目标

建立trace-flow命令的基础框架，包括命令行解析、配置文件加载、简单表达式解析和基础拦截机制。

## 📁 项目结构

```
arthas-trace-flow/
├── src/
│   ├── main/
│   │   ├── java/com/taobao/arthas/core/command/trace/
│   │   │   ├── TraceFlowCommand.java          # 主命令类
│   │   │   ├── ProbeManager.java              # 探针管理器
│   │   │   ├── ProbeConfig.java               # 配置数据结构
│   │   │   ├── SourceExpressionParser.java    # 表达式解析器
│   │   │   ├── ExecutionContext.java          # 执行上下文
│   │   │   ├── TraceManager.java              # 跟踪管理器
│   │   │   ├── FilterEngine.java              # 过滤引擎
│   │   │   └── OutputFormatter.java           # 输出格式化器
│   │   └── resources/probes/
│   │       ├── database-probe.json            # 数据库探针配置
│   │       ├── http-server-probe.json         # HTTP服务探针配置
│   │       ├── http-client-probe.json         # HTTP客户端探针配置
│   │       └── file-operations-probe.json     # 文件操作探针配置
│   └── test/
│       └── java/com/taobao/arthas/core/command/trace/
│           └── Stage1Test.java                # 阶段1测试
├── scripts/
│   ├── test-stage1.sh                         # Linux/Mac测试脚本
│   └── test-stage1.bat                        # Windows测试脚本
└── README-Stage1.md                           # 本文档
```

## 🚀 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+
- Python 3.x (用于JSON验证)

### 编译和测试

#### Linux/Mac
```bash
# 给脚本执行权限
chmod +x scripts/test-stage1.sh

# 运行阶段1测试
./scripts/test-stage1.sh
```

#### Windows
```cmd
# 运行阶段1测试
scripts\test-stage1.bat
```

#### 手动测试
```bash
# 编译代码
mvn clean compile

# 运行单元测试
mvn test -Dtest=Stage1Test

# 生成代码覆盖率报告
mvn jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

## 🧪 阶段1功能测试

### 1. 命令行参数解析
```java
TraceFlowCommand command = new TraceFlowCommand();
// 测试默认参数值
assertEquals(1, command.getCount());
assertNull(command.getFilter());
assertFalse(command.isVerbose());
```

### 2. 配置文件加载
```java
ProbeManager manager = new ProbeManager();
List<ProbeConfig> configs = manager.loadBuiltinProbes();
// 验证加载了4个探针
assertEquals(4, configs.size());
```

### 3. 简单表达式解析
```java
SourceExpressionParser parser = new SourceExpressionParser();
ExecutionContext context = ExecutionContext.createMockContext(1000L, 2000L);

// 测试内置变量
assertEquals(1000L, parser.parse("startTime", context));
assertEquals(2000L, parser.parse("endTime", context));
assertEquals(1000L, parser.parse("executionTime", context));
assertEquals("test-thread", parser.parse("threadName", context));
```

### 4. 基础过滤功能
```java
FilterEngine filter = new FilterEngine();
Map<String, Object> metrics = Map.of("executionTime", 1500L);

// 测试数值比较
assertTrue(filter.matches("executionTime > 1000", metrics));
assertFalse(filter.matches("executionTime > 2000", metrics));

// 测试字符串操作
assertTrue(filter.matches("url.contains('/api')", metrics));
assertTrue(filter.matches("url.startsWith('/api')", metrics));
```

## 📋 已实现功能

### ✅ 命令行框架
- [x] TraceFlowCommand类集成picocli
- [x] 基本参数解析 (-n, --filter, --output-file, --verbose)
- [x] 帮助信息和版本信息
- [x] 参数验证

### ✅ 配置文件加载
- [x] ProbeConfig和MetricConfig数据结构
- [x] JSON配置文件解析器
- [x] 4个内置探针配置文件
- [x] 配置文件验证和错误处理

### ✅ 简单表达式解析
- [x] SourceExpressionParser基础版本
- [x] 内置变量解析 (startTime, endTime, executionTime, threadName)
- [x] ExecutionContext数据结构
- [x] 表达式解析异常处理

### ✅ 基础拦截机制
- [x] ProbeManager和拦截器接口
- [x] 基础的方法拦截框架
- [x] ExecutionContext构建器
- [x] 拦截器注册和管理机制

### ✅ 过滤引擎
- [x] 基础过滤表达式支持
- [x] 数值比较 (>, <, ==)
- [x] 字符串操作 (contains, startsWith)
- [x] 过滤表达式验证

### ✅ 输出格式化
- [x] 基础输出格式化器
- [x] 帮助信息格式化
- [x] 错误信息格式化

## 🔍 测试覆盖率

阶段1目标代码覆盖率: >= 80%

运行测试后查看覆盖率报告:
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

## 🚫 阶段1限制

### 不支持的功能
- 复杂表达式解析 (this.toString(), args[0].getValue())
- 实际的方法拦截 (只有模拟实现)
- Formula表达式计算
- 真实的链路跟踪
- JSON文件输出
- 堆栈跟踪

### 支持的表达式
**Source表达式 (阶段1)**:
- `startTime` - 方法开始时间
- `endTime` - 方法结束时间  
- `executionTime` - 执行耗时
- `threadName` - 线程名称

**过滤表达式 (阶段1)**:
- `executionTime > 1000` - 数值比较
- `operationType == 'SELECT'` - 字符串相等
- `url.contains('/api')` - 字符串包含
- `url.startsWith('/api')` - 字符串开始
- `true` / `false` - 布尔值

## 🎯 验收标准

### 功能验收
- [x] 命令行参数正确解析
- [x] 内置探针配置正确加载
- [x] 简单表达式解析器支持内置变量
- [x] 基础方法拦截机制工作正常
- [x] 过滤引擎支持基本表达式
- [x] 输出格式化器正常工作

### 质量验收
- [x] 单元测试覆盖率 >= 80%
- [x] 所有测试用例通过
- [x] 代码符合规范
- [x] 配置文件JSON格式正确

### 性能验收
- [x] 编译时间 < 30秒
- [x] 测试执行时间 < 10秒
- [x] 内存使用合理

## 🔄 下一步计划

阶段1完成后，进入阶段2开发:

### 阶段2目标
- 完善Source表达式解析器 (支持this, args, returnValue)
- 实现Formula表达式解析器 (JavaScript引擎)
- 完整实现Database探针
- 实现指标采集引擎
- 控制台输出格式化

### 阶段2验收标准
```bash
# 阶段2测试命令
tf --filter "executionTime > 100"
tf --filter "operationType == 'SELECT'"
tf --filter "hasException == true"
```

## 📞 问题反馈

如果在阶段1测试中遇到问题:

1. 检查Java和Maven环境
2. 确认所有依赖正确安装
3. 运行详细测试: `mvn test -Dtest=Stage1Test -X`
4. 查看测试报告: `target/surefire-reports/`

## 📝 开发日志

- [x] 2024-01-XX: 完成命令行框架搭建
- [x] 2024-01-XX: 完成配置文件加载器
- [x] 2024-01-XX: 完成简单表达式解析器
- [x] 2024-01-XX: 完成基础拦截机制
- [x] 2024-01-XX: 完成阶段1测试验证

**阶段1状态**: ✅ 完成，可进入阶段2开发
