# Arthas trace-flow 命令需求文档 (MVP 核心版本)

## 📋 文档说明

本文档定义 **`trace-flow` 命令的最小可行产品 (MVP)**，专注于解决最常见的请求链路分析和性能问题定位。

**MVP 核心功能**:
- ✅ `trace-flow` (tf) 命令 - HTTP请求链路跟踪
- ✅ 内置4个核心探针 (HTTP Server, Database, HTTP Client, File Operations)
- ✅ URL模式匹配和基础过滤
- ✅ 控制台树状输出 + JSON文件保存

**暂不包含**:
- ❌ 请求回放、结果对比功能
- ❌ 复杂探针配置 (版本匹配、热更新)
- ❌ 全局配置管理

---

## 1. 概述

### 1.1 核心问题

**问题**：当HTTP请求出现性能问题或异常时，开发者难以快速了解完整的执行链路（数据库查询、缓存操作、外部调用等），导致问题定位效率低下。

**解决方案**：提供 `trace-flow` 命令，非侵入式地跟踪HTTP请求的完整执行流程，以树状结构展示调用链路和关键指标。

### 1.2 核心价值

- **快速问题定位**：从"小时级"排查缩短到"分钟级"定位
- **完整链路可视化**：清晰展示HTTP请求→数据库→缓存→外部调用的完整流程
- **非侵入式分析**：无需修改代码或重启应用

---

## 2. 核心功能需求

### 2.1 基本命令

- **FR-1**: 提供 `trace-flow` 命令，别名 `tf`
- **FR-2**: 支持跟踪次数限制：`tf -n 5` (跟踪5次请求)
- **FR-3**: 无参数时跟踪下一个HTTP请求

### 2.2 链路跟踪能力

- **FR-4**: 自动跟踪HTTP请求的完整执行链路：
  - HTTP请求接收 (Servlet/Spring MVC)
  - 数据库操作 (JDBC PreparedStatement/Statement)
  - HTTP客户端调用 (HttpClient/OkHttp/RestTemplate)
  - 文件读写操作 (FileInputStream/FileOutputStream/Files)

### 2.3 指标驱动过滤

- **FR-5**: 支持基于指标的过滤表达式，所有过滤都基于探针定义的指标：
  - `--filter "executionTime > 1000"` - 基于executionTime指标过滤慢请求
  - `--filter "sql.contains('user')"` - 基于sql指标过滤包含'user'的SQL操作
  - `--filter "operationType == 'SELECT'"` - 基于operationType指标过滤查询操作
  - `--filter "isSlowQuery == true"` - 基于isSlowQuery指标过滤慢查询
  - `--filter "status >= 400"` - 基于status指标过滤错误请求
  - `--filter "url.startsWith('/api')"` - 基于url指标过滤API请求 (替代URL模式匹配)

- **FR-5.1**: 指标驱动过滤的优势：
  - **类型安全**: 基于指标的类型定义进行过滤，避免类型错误
  - **语义清晰**: 过滤条件直接对应业务指标，易于理解
  - **可扩展**: 新增指标后自动支持相应的过滤功能
  - **统一接口**: URL匹配、性能过滤、业务过滤都使用同一套过滤语法

### 2.4 内置探针配置

- **FR-6**: MVP版本内置4个核心探针，基于预定义的配置文件实现：
  - **HTTP Server探针**: `http-server-probe.json` - 监控Servlet/Spring MVC请求处理
  - **Database探针**: `database-probe.json` - 监控JDBC SQL执行
  - **HTTP Client探针**: `http-client-probe.json` - 监控出站HTTP请求
  - **File Operations探针**: `file-operations-probe.json` - 监控文件读写操作

- **FR-6.1**: 内置探针配置文件特点：
  - 配置文件内置在jar包中，用户无需手动配置
  - 采用简化的配置格式，专注核心功能
  - 支持用户通过外部配置文件覆盖内置配置（高级功能）

**指标驱动的探针配置示例 (`database-probe.json`)：**
```json
{
  "name": "Database探针",
  "enabled": true,
  "metrics": [
    {
      "name": "sql",
      "description": "执行的SQL语句",
      "targets": [
        {
          "class": "java.sql.PreparedStatement",
          "methods": ["execute", "executeQuery", "executeUpdate"]
        },
        {
          "class": "java.sql.Statement",
          "methods": ["execute", "executeQuery", "executeUpdate"]
        }
      ],
      "source": "this.toString()",
      "type": "string"
    },
    {
      "name": "executionTime",
      "description": "SQL执行耗时",
      "targets": [
        {
          "class": "java.sql.PreparedStatement",
          "methods": ["execute", "executeQuery", "executeUpdate"]
        }
      ],
      "source": "executionTime",
      "type": "long",
      "unit": "milliseconds"
    },
    {
      "name": "affectedRows",
      "description": "影响的行数",
      "targets": [
        {
          "class": "java.sql.PreparedStatement",
          "methods": ["executeUpdate"]
        }
      ],
      "source": "returnValue instanceof Integer ? returnValue : -1",
      "type": "integer"
    },
    {
      "name": "operationType",
      "description": "操作类型",
      "targets": [
        {
          "class": "java.sql.PreparedStatement",
          "methods": ["execute", "executeQuery", "executeUpdate"]
        }
      ],
      "source": "sql.toUpperCase().startsWith('SELECT') ? 'READ' : 'WRITE'",
      "type": "string"
    }
  ],
  "output": {
    "type": "DATABASE",
    "template": "SQL: ${sql} | Time: ${executionTime}ms | Rows: ${affectedRows} | Type: ${operationType}"
  }
}
```

### 2.5 配置文件架构

- **FR-8**: 指标驱动的探针配置文件统一结构：
```json
{
  "name": "探针名称",
  "enabled": true,
  "metrics": [
    {
      "name": "指标名称",
      "description": "指标描述",
      "targets": [
        {
          "className": "目标类名",
          "methods": ["目标方法列表"]
        }
      ],
      "source": "数据提取表达式",
      "type": "数据类型",
      "unit": "单位(可选)"
    }
  ],
  "output": {
    "type": "输出类型标识",
    "template": "输出模板(使用${指标名称}引用)"
  }
}
```

- **FR-8.1**: 指标级别的targets定义优势：
  - **精细控制**: 每个指标可以指定不同的拦截目标
  - **性能优化**: 只在需要的方法上拦截特定指标
  - **灵活组合**: 不同指标可以组合不同的目标类和方法
  - **减少干扰**: 避免不必要的方法拦截

- **FR-8.2**: 指标定义规范：
  - **name**: 指标的唯一标识，用于过滤和输出模板中引用
  - **targets**: 该指标需要拦截的目标类和方法
  - **source**: OGNL表达式，定义如何从执行上下文中提取数据
  - **formula**: 计算公式，用于定义计算指标（与source互斥）
  - **type**: 数据类型 (string, integer, long, double, boolean, object)
  - **unit**: 可选的单位信息 (milliseconds, bytes, count等)
  - **capturePoint**: 数据采集时机，对于source指标必须指定

- **FR-8.3**: 采集时机定义：
  - **before**: 方法执行前采集，可访问this、args、method
  - **after**: 方法执行后采集，可访问this、args、returnValue、exception、method
  - **around**: 方法执行前后都采集，用于计算执行时间等
  - **计算指标**: 无需指定capturePoint，在其依赖的指标采集完成后计算

- **FR-8.4**: 指标分类：
  - **基础指标**: 直接从执行上下文提取的原始数据，需要指定capturePoint
  - **计算指标**: 基于其他指标通过公式计算得出的派生数据，无需capturePoint
  - **聚合指标**: 跨多次调用的统计数据（未来版本）

- **FR-8.4**: 指标数据提取上下文：
  - **this**: 当前被拦截的对象实例
  - **args**: 方法参数数组 (args[0], args[1], ...)
  - **returnValue**: 方法返回值
  - **exception**: 抛出的异常 (如果有)
  - **startTime**: 方法开始执行时间戳 (毫秒)
  - **endTime**: 方法结束执行时间戳 (毫秒)
  - **threadName**: 当前线程名称
  - **method**: 当前被拦截的方法对象
  - **metrics**: 其他已计算的指标值 (用于计算指标)

- **FR-8.5**: source表达式设计原则：
  - **核心目的**: 从方法调用上下文中提取原始数据
  - **数据来源**: 被拦截方法的this、参数、返回值、异常
  - **保持简单**: 避免复杂的计算逻辑，复杂计算使用formula
  - **类型安全**: 提取的数据应该有明确的类型

- **FR-8.5**: source表达式语法：
  - **内置变量**: `startTime`, `endTime`, `executionTime`, `threadName`
  - **对象实例**: `this` (被拦截的对象实例)
  - **方法参数**: `args[0]`, `args[1]`, `args.length`
  - **返回值**: `returnValue` (仅在after时可用)
  - **异常信息**: `exception` (仅在after时可用)
  - **方法信息**: `method.getName()`, `method.getDeclaringClass()`
  - **属性访问**: `this.connection`, `args[0].method`
  - **方法调用**: `this.toString()`, `returnValue.getStatus()`
  - **安全访问**: `args[0] != null ? args[0].getValue() : null`

- **FR-8.6**: 采集时机示例：
```json
{
  "metrics": [
    {
      "name": "sql",
      "source": "this.toString()",
      "capturePoint": "before",  // 方法执行前获取SQL语句
      "type": "string"
    },
    {
      "name": "affectedRows",
      "source": "returnValue",
      "capturePoint": "after",   // 方法执行后获取返回值
      "type": "integer"
    },
    {
      "name": "hasException",
      "source": "exception != null",
      "capturePoint": "after",   // 方法执行后检查异常
      "type": "boolean"
    },
    {
      "name": "executionTime",
      "formula": "metrics.endTime - metrics.startTime",  // 计算指标无需capturePoint
      "type": "long"
    }
  ]
}
```

- **FR-8.6**: source表达式示例：
```json
{
  "name": "sql",
  "source": "this.toString()",
  "type": "string"
},
{
  "name": "method",
  "source": "args[0].getMethod()",
  "type": "string"
},
{
  "name": "status",
  "source": "returnValue.getStatusLine().getStatusCode()",
  "type": "integer"
},
{
  "name": "isError",
  "source": "exception != null",
  "type": "boolean"
},
{
  "name": "operationType",
  "source": "sql.toUpperCase().trim().split('\\\\s+')[0]",
  "type": "string"
},
{
  "name": "safeValue",
  "source": "args[0] != null ? args[0].getValue() : 'default'",
  "type": "string"
}
```

- **FR-8.7**: 计算指标支持：
  - **公式计算**: 支持基于其他指标的数学计算
  - **条件计算**: 支持基于条件的分支计算
  - **函数调用**: 支持内置函数和自定义函数
  - **依赖解析**: 自动解析指标间的依赖关系

- **FR-8.3**: 配置文件位置和加载顺序：
  1. **内置配置**: `resources/probes/` 目录下的默认配置
  2. **用户配置**: `~/.arthas/probes/` 目录下的用户自定义配置（可选）
  3. **项目配置**: 当前目录下 `./probes/` 的项目特定配置（可选）

### 2.6 输出格式

- **FR-9**: 控制台树状输出格式：
```
Arthas Trace ID: 12345678-1234-1234-1234-123456789abc
GET /api/users/123 -> 200 OK (Total: 1.5s)
├── [HTTP] GET /api/users/123 (15ms)
├── [DATABASE] SELECT * FROM users WHERE id=? (800ms)
│   └── SQL: SELECT * FROM users WHERE id=123
├── [FILE_READ] /data/users/123.json (120ms)
│   └── File: /data/users/123.json, Size: 2.5KB
├── [HTTP_CLIENT] GET http://profile-service/users/123 (350ms)
│   └── Status: 200, Size: 1.2KB
├── [FILE_WRITE] /cache/users/123.cache (80ms)
│   └── File: /cache/users/123.cache, Size: 3.1KB
└── [HTTP] Response 200 OK (5ms)
```

- **FR-10**: JSON文件输出 (`--output-file result.json`)：
```json
{
  "traceId": "12345678-1234-1234-1234-123456789abc",
  "request": {"method": "GET", "url": "/api/users/123"},
  "totalTime": 1500,
  "nodes": [
    {"type": "HTTP", "time": 15, "details": "GET /api/users/123"},
    {"type": "DATABASE", "time": 800, "sql": "SELECT * FROM users WHERE id=123"},
    {"type": "FILE_READ", "time": 120, "filePath": "/data/users/123.json", "fileSize": 2560},
    {"type": "HTTP_CLIENT", "time": 350, "url": "http://profile-service/users/123"},
    {"type": "FILE_WRITE", "time": 80, "filePath": "/cache/users/123.cache", "fileSize": 3174}
  ]
}
```

### 2.7 高级功能

- **FR-11**: 堆栈跟踪：`--stack-trace-threshold 1000` (耗时超过1秒时显示堆栈)
- **FR-12**: 详细模式：`--verbose` (显示更多参数和返回值信息)

---

## 3. 使用示例

### 3.1 基本使用
```bash
# 跟踪下一个HTTP请求
tf

# 跟踪API请求 (使用filter代替URL模式)
tf --filter "url.startsWith('/api')"

# 跟踪5次用户相关请求
tf -n 5 --filter "url.contains('/user')"

# 只显示慢请求 (>1秒)
tf --filter "executionTime > 1000"

# 组合过滤条件
tf --filter "url.startsWith('/api') && executionTime > 500"

# 保存结果到文件
tf --output-file trace-result.json
```

### 3.2 指标驱动的典型场景

**场景1: 性能问题定位**
```bash
# 找出所有慢查询（基于计算指标）
tf --filter "isSlowQuery == true"

# 找出性能等级为SLOW的操作
tf --filter "performanceLevel == 'SLOW'"

# 找出查询效率低于60分的SQL
tf --filter "queryEfficiency < 60"

# 找出队列等待时间过长的请求
tf --filter "queueWaitTime > 500"
```

**场景2: 业务流程分析**
```bash
# 分析用户相关的数据操作
tf --filter "sql.contains('user')"

# 分析API请求的数据操作
tf --filter "url.startsWith('/api') && operationType in ['INSERT', 'UPDATE', 'DELETE']"

# 分析高吞吐量的查询
tf --filter "throughputScore > 100"

# 分析特定路径的队列效率
tf --filter "url.contains('/user') && queueEfficiency < 80"
```

**场景3: 异常问题排查**
```bash
# 找出所有异常请求
tf --filter "hasException == true"

# 找出HTTP错误（基于计算指标）
tf --filter "isError == true"

# 找出总响应时间异常的请求
tf --filter "totalTime > processingTime * 2"

# 找出响应吞吐量异常低的请求
tf --filter "throughput < 1000"
```

**场景4: 文件操作分析**
```bash
# 找出所有慢文件操作
tf --filter "isSlowOperation == true"

# 找出大文件操作
tf --filter "isLargeFile == true"

# 找出配置文件读写操作
tf --filter "isConfigFile == true"

# 找出文件操作吞吐量低的情况
tf --filter "throughput < 1000000"

# 找出文件操作异常
tf --filter "hasException == true && operationType in ['READ', 'WRITE']"
```

**场景5: 复合条件分析**
```bash
# 找出慢查询且影响行数多的操作
tf --filter "isSlowQuery == true && affectedRows > 1000"

# 找出性能差但查询效率高的SQL（可能是数据量问题）
tf --filter "performanceLevel == 'SLOW' && queryEfficiency > 80"

# 找出队列等待时间占比过高的请求
tf --filter "queueWaitTime > processingTime"

# 找出涉及大文件读写的慢请求
tf --filter "isSlowRequest == true && isLargeFile == true"

# 找出文件操作和数据库操作都慢的请求
tf --filter "isSlowQuery == true && isSlowOperation == true"
```

---

## 4. 指标驱动架构设计

### 4.1 指标驱动的核心理念

- **FR-13**: 探针配置以**指标定义**为核心，明确定义每个指标如何采集：
  - **指标优先**: 先定义需要采集的指标，再定义如何采集
  - **数据源明确**: 每个指标都有明确的数据提取表达式
  - **类型安全**: 每个指标都有明确的数据类型定义
  - **语义清晰**: 指标名称和描述体现业务含义

### 4.2 指标级别targets的精细控制

**指标级别targets的优势示例：**
```json
{
  "metrics": [
    {
      "name": "sql",
      "description": "执行的SQL语句",
      "targets": [
        {
          "class": "java.sql.PreparedStatement",
          "methods": ["execute", "executeQuery", "executeUpdate"]
        },
        {
          "class": "java.sql.Statement",
          "methods": ["execute", "executeQuery", "executeUpdate"]
        }
      ],
      "source": "this.toString()",
      "type": "string"
    },
    {
      "name": "affectedRows",
      "description": "影响的行数",
      "targets": [
        {
          "class": "java.sql.PreparedStatement",
          "methods": ["executeUpdate"] 
        }
      ],
      "source": "returnValue instanceof Integer ? returnValue : -1",
      "type": "integer"
    },
    {
      "name": "connectionUrl",
      "description": "数据库连接URL",
      "targets": [
        {
          "class": "java.sql.Connection",
          "methods": ["prepareStatement"]  
        }
      ],
      "source": "this.getMetaData().getURL()",
      "type": "string"
    }
  ]
}
```

**精细控制的好处：**
1. **性能优化**: `affectedRows` 只在 `executeUpdate` 时采集，避免在查询操作时的无效拦截
2. **数据准确性**: `connectionUrl` 在连接层面采集，确保数据的准确性
3. **减少干扰**: 每个指标只拦截必要的方法，减少对应用的性能影响

### 4.3 计算指标设计

**基础指标 + 计算指标示例：**
```json
{
  "metrics": [
    {
      "name": "startTime",
      "description": "开始执行时间",
      "targets": [
        {"class": "java.sql.PreparedStatement", "methods": ["execute"]}
      ],
      "source": "startTime",
      "type": "long",
      "capturePoint": "before"
    },
    {
      "name": "endTime",
      "description": "结束执行时间",
      "targets": [
        {"class": "java.sql.PreparedStatement", "methods": ["execute"]}
      ],
      "source": "endTime",
      "type": "long",
      "capturePoint": "after"
    },
    {
      "name": "executionTime",
      "description": "执行耗时",
      "formula": "metrics.endTime - metrics.startTime",
      "type": "long",
      "unit": "milliseconds"
    },
    {
      "name": "performanceLevel",
      "description": "性能等级",
      "formula": "metrics.executionTime < 100 ? 'FAST' : (metrics.executionTime < 1000 ? 'NORMAL' : 'SLOW')",
      "type": "string"
    },
    {
      "name": "queryEfficiency",
      "description": "查询效率分数",
      "formula": "Math.max(0, 100 - (metrics.executionTime / 10))",
      "type": "double",
      "unit": "score"
    }
  ]
}
```

**计算指标的优势：**
1. **精确计算**: `executionTime = endTime - startTime` 确保时间计算的准确性
2. **业务语义**: `performanceLevel` 将技术指标转换为业务可理解的等级
3. **复合指标**: `queryEfficiency` 基于多个因素计算综合评分
4. **无需拦截**: 计算指标不需要额外的方法拦截，性能开销为零

### 4.3 内置配置文件结构
```
arthas-core.jar
└── resources/
    └── probes/
        ├── http-server-probe.json      # HTTP服务探针 (12个指标)
        ├── database-probe.json         # 数据库探针 (10个指标)
        ├── http-client-probe.json      # HTTP客户端探针 (12个指标)
        └── file-operations-probe.json  # 文件操作探针 (16个指标)
```

### 4.4 配置加载机制
- **FR-14**: 系统启动时自动加载内置探针配置和指标定义
- **FR-15**: 支持配置文件的分层覆盖（内置 < 用户 < 项目）
- **FR-16**: 配置文件解析错误时提供明确的错误信息

### 4.5 指标驱动的优势
1. **业务导向**: 指标定义直接对应业务关注点
2. **类型安全**: 明确的类型定义避免运行时错误
3. **过滤友好**: 所有过滤都基于预定义的指标
4. **输出一致**: 输出模板统一引用指标，格式一致
5. **扩展简单**: 新增指标只需修改配置文件

---

## 5. Source和Formula表达式解析设计

### 5.1 解析器架构设计

#### 5.1.1 整体架构
```
ExpressionEngine
├── SourceExpressionParser     # Source表达式解析器
├── FormulaExpressionParser    # Formula表达式解析器
├── ExecutionContext          # 执行上下文
└── MetricsContext            # 指标上下文
```

#### 5.1.2 核心接口设计
```java
public interface ExpressionParser {
    Object parse(String expression, Context context);
    boolean supports(String expression);
    Set<String> getDependencies(String expression);
}

public interface Context {
    Object getValue(String key);
    void setValue(String key, Object value);
    Map<String, Object> getAllValues();
}
```

### 5.2 Source表达式解析器

#### 5.2.1 设计原则
- **数据来源固定**: 仅从方法调用上下文提取数据
- **语法简单**: 支持属性访问、方法调用、条件判断
- **性能优先**: 轻量级解析，避免复杂计算
- **安全可控**: 限制可访问的对象和方法

#### 5.2.2 支持的表达式类型
```java
public enum SourceExpressionType {
    BUILTIN_VARIABLE,    // startTime, endTime, threadName
    PROPERTY_ACCESS,     // this.connection, args[0].method
    METHOD_CALL,         // this.toString(), args[0].getValue()
    CONDITIONAL,         // exception != null ? 'ERROR' : 'OK'
    ARRAY_ACCESS,        // args[0], args[1]
    TYPE_CHECK          // returnValue instanceof Integer
}
```

#### 5.2.3 解析器实现
```java
public class SourceExpressionParser implements ExpressionParser {
    private static final Pattern BUILTIN_PATTERN = Pattern.compile("^(startTime|endTime|executionTime|threadName)$");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^(this|args\\[\\d+\\]|returnValue|exception|method)\\.\\w+");

    @Override
    public Object parse(String expression, Context context) {
        // 1. 内置变量
        if (BUILTIN_PATTERN.matcher(expression).matches()) {
            return parseBuiltinVariable(expression, context);
        }

        // 2. 属性访问
        if (PROPERTY_PATTERN.matcher(expression).find()) {
            return parsePropertyAccess(expression, context);
        }

        // 3. 条件表达式
        if (expression.contains("?")) {
            return parseConditional(expression, context);
        }

        // 4. 使用OGNL作为后备解析器
        return parseWithOGNL(expression, context);
    }

    private Object parseBuiltinVariable(String variable, Context context) {
        switch (variable) {
            case "startTime": return context.getValue("startTime");
            case "endTime": return context.getValue("endTime");
            case "executionTime": return context.getValue("executionTime");
            case "threadName": return context.getValue("threadName");
            default: throw new UnsupportedExpressionException(variable);
        }
    }

    private Object parsePropertyAccess(String expression, Context context) {
        // 解析 this.toString(), args[0].getValue() 等
        String[] parts = expression.split("\\.");
        Object target = resolveTarget(parts[0], context);

        for (int i = 1; i < parts.length; i++) {
            target = resolveProperty(target, parts[i]);
        }

        return target;
    }
}
```

#### 5.2.4 执行上下文
```java
public class ExecutionContext implements Context {
    private final Object targetObject;
    private final Object[] args;
    private final Method method;
    private final Object returnValue;
    private final Throwable exception;
    private final long startTime;
    private final long endTime;
    private final String threadName;

    @Override
    public Object getValue(String key) {
        switch (key) {
            case "this": return targetObject;
            case "args": return args;
            case "method": return method;
            case "returnValue": return returnValue;
            case "exception": return exception;
            case "startTime": return startTime;
            case "endTime": return endTime;
            case "threadName": return threadName;
            default: return null;
        }
    }
}
```

### 5.3 Formula表达式解析器

#### 5.3.1 设计原则
- **功能强大**: 支持复杂的数学计算和逻辑判断
- **类型安全**: 基于已知指标类型进行计算
- **依赖管理**: 自动解析指标间的依赖关系
- **性能优化**: 表达式缓存和预编译

#### 5.3.2 解析策略
```java
public class FormulaExpressionParser implements ExpressionParser {
    private final SimpleExpressionParser simpleParser;
    private final JavaScriptEngine jsEngine;
    private final Map<String, CompiledExpression> cache;

    @Override
    public Object parse(String formula, Context context) {
        // 1. 检查缓存
        CompiledExpression compiled = cache.get(formula);
        if (compiled == null) {
            compiled = compile(formula);
            cache.put(formula, compiled);
        }

        // 2. 执行表达式
        return compiled.evaluate(context);
    }

    private CompiledExpression compile(String formula) {
        // 1. 尝试简单表达式解析
        if (isSimpleArithmetic(formula)) {
            return new SimpleCompiledExpression(formula);
        }

        // 2. 使用JavaScript引擎编译
        return new JavaScriptCompiledExpression(formula, jsEngine);
    }

    private boolean isSimpleArithmetic(String formula) {
        // 检查是否为简单的算术表达式
        return formula.matches("metrics\\.[a-zA-Z_]\\w*(\\s*[+\\-*/]\\s*metrics\\.[a-zA-Z_]\\w*)*");
    }
}
```

#### 5.3.3 简单表达式优化
```java
public class SimpleExpressionParser {
    public Object parse(String formula, MetricsContext context) {
        // 处理简单的算术表达式: metrics.endTime - metrics.startTime
        Pattern pattern = Pattern.compile("metrics\\.([a-zA-Z_]\\w*)");
        Matcher matcher = pattern.matcher(formula);

        String expression = formula;
        while (matcher.find()) {
            String metricName = matcher.group(1);
            Object value = context.getMetric(metricName);
            expression = expression.replace(matcher.group(0), String.valueOf(value));
        }

        return evaluateArithmetic(expression);
    }

    private Object evaluateArithmetic(String expression) {
        // 使用简单的算术解析器
        return new ArithmeticEvaluator().evaluate(expression);
    }
}
```

#### 5.3.4 JavaScript引擎集成
```java
public class JavaScriptFormulaEngine {
    private final ScriptEngine engine;

    public JavaScriptFormulaEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("javascript");

        // 预加载常用函数
        try {
            engine.eval("var Math = Java.type('java.lang.Math');");
            engine.eval("function safe(obj, defaultValue) { return obj != null ? obj : defaultValue; }");
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to initialize JavaScript engine", e);
        }
    }

    public Object evaluate(String formula, MetricsContext context) {
        try {
            engine.put("metrics", context.getAllMetrics());
            return engine.eval(formula);
        } catch (ScriptException e) {
            throw new FormulaEvaluationException("Formula evaluation failed: " + formula, e);
        }
    }
}
```

### 5.4 指标上下文管理

#### 5.4.1 MetricsContext设计
```java
public class MetricsContext implements Context {
    private final Map<String, Object> metrics = new LinkedHashMap<>();
    private final Map<String, MetricConfig> configs = new HashMap<>();

    public void addMetric(String name, Object value, MetricConfig config) {
        metrics.put(name, value);
        configs.put(name, config);
    }

    public Object getMetric(String name) {
        return metrics.get(name);
    }

    public Map<String, Object> getAllMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    public boolean hasMetric(String name) {
        return metrics.containsKey(name);
    }
}
```

#### 5.4.2 依赖解析器
```java
public class DependencyResolver {
    public List<MetricConfig> resolveDependencies(List<MetricConfig> metrics) {
        Map<String, MetricConfig> metricMap = metrics.stream()
            .collect(Collectors.toMap(MetricConfig::getName, Function.identity()));

        List<MetricConfig> resolved = new ArrayList<>();
        Set<String> processing = new HashSet<>();
        Set<String> processed = new HashSet<>();

        for (MetricConfig metric : metrics) {
            resolveDependency(metric, metricMap, resolved, processing, processed);
        }

        return resolved;
    }

    private void resolveDependency(MetricConfig metric, Map<String, MetricConfig> metricMap,
                                 List<MetricConfig> resolved, Set<String> processing, Set<String> processed) {
        if (processed.contains(metric.getName())) {
            return;
        }

        if (processing.contains(metric.getName())) {
            throw new CircularDependencyException("Circular dependency detected: " + metric.getName());
        }

        processing.add(metric.getName());

        // 解析依赖
        Set<String> dependencies = extractDependencies(metric);
        for (String dep : dependencies) {
            MetricConfig depMetric = metricMap.get(dep);
            if (depMetric != null) {
                resolveDependency(depMetric, metricMap, resolved, processing, processed);
            }
        }

        processing.remove(metric.getName());
        processed.add(metric.getName());
        resolved.add(metric);
    }

    private Set<String> extractDependencies(MetricConfig metric) {
        if (metric.getFormula() != null) {
            return extractFromFormula(metric.getFormula());
        }
        return Collections.emptySet();
    }

    private Set<String> extractFromFormula(String formula) {
        Set<String> dependencies = new HashSet<>();
        Pattern pattern = Pattern.compile("metrics\\.([a-zA-Z_]\\w*)");
        Matcher matcher = pattern.matcher(formula);

        while (matcher.find()) {
            dependencies.add(matcher.group(1));
        }

        return dependencies;
    }
}
```

### 5.5 性能优化策略

#### 5.5.1 表达式缓存
```java
public class CachedExpressionEngine {
    private final Map<String, CompiledExpression> sourceCache = new ConcurrentHashMap<>();
    private final Map<String, CompiledExpression> formulaCache = new ConcurrentHashMap<>();

    public Object evaluateSource(String source, ExecutionContext context) {
        CompiledExpression compiled = sourceCache.computeIfAbsent(source, this::compileSource);
        return compiled.evaluate(context);
    }

    public Object evaluateFormula(String formula, MetricsContext context) {
        CompiledExpression compiled = formulaCache.computeIfAbsent(formula, this::compileFormula);
        return compiled.evaluate(context);
    }
}
```

#### 5.5.2 类型优化
```java
public class TypedExpressionEvaluator {
    public <T> T evaluate(String expression, Context context, Class<T> expectedType) {
        Object result = baseEvaluate(expression, context);
        return convertType(result, expectedType);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertType(Object value, Class<T> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return (T) value;

        // 类型转换逻辑
        if (targetType == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        // ... 其他类型转换

        throw new TypeConversionException("Cannot convert " + value.getClass() + " to " + targetType);
    }
}
```

### 5.6 错误处理和安全性

#### 5.6.1 安全限制
```java
public class SecurityManager {
    private static final Set<String> ALLOWED_CLASSES = Set.of(
        "java.lang.String", "java.lang.Integer", "java.lang.Long",
        "java.lang.Double", "java.lang.Boolean", "java.lang.Math"
    );

    private static final Set<String> FORBIDDEN_METHODS = Set.of(
        "getClass", "notify", "notifyAll", "wait", "finalize"
    );

    public boolean isAllowedAccess(String className, String methodName) {
        return ALLOWED_CLASSES.contains(className) && !FORBIDDEN_METHODS.contains(methodName);
    }
}
```

#### 5.6.2 错误处理
```java
public class ExpressionException extends RuntimeException {
    private final String expression;
    private final int position;

    public ExpressionException(String message, String expression, int position, Throwable cause) {
        super(formatMessage(message, expression, position), cause);
        this.expression = expression;
        this.position = position;
    }

    private static String formatMessage(String message, String expression, int position) {
        return String.format("%s at position %d in expression: %s", message, position, expression);
    }
}
```

---

## 6. 分阶段实现计划

### 阶段1: 基础框架 (可独立测试)
**目标**: 建立基本的命令框架和配置解析能力

**功能范围**:
- `trace-flow` 命令基础框架
- 配置文件解析和加载
- 简单的Source表达式解析 (内置变量: startTime, endTime, threadName)
- 基础的方法拦截机制

**测试方式**:
```bash
# 测试命令框架
tf --help

# 测试配置加载
tf --list-probes

# 测试简单拦截 (手动触发)
tf --target "java.lang.System.currentTimeMillis()" --action "System.out.println"
```

**交付物**:
- 基础命令行解析
- 配置文件加载器
- 简单表达式解析器
- 方法拦截基础框架

---

### 阶段2: 单探针实现 (Database探针)
**目标**: 完整实现一个探针，验证指标采集和计算

**功能范围**:
- Database探针完整实现
- Source表达式完整支持 (this, args, returnValue, exception)
- Formula表达式基础支持 (JavaScript引擎)
- 基础指标和计算指标
- 控制台输出

**测试方式**:
```bash
# 测试数据库探针
tf --probe database

# 测试指标采集
tf --filter "executionTime > 100"

# 测试计算指标
tf --filter "operationType == 'SELECT'"
```

**测试用例**:
```java
// 创建测试用的数据库操作
public class DatabaseTest {
    public void testSlowQuery() {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
        stmt.setInt(1, 123);
        ResultSet rs = stmt.executeQuery(); // 这里会被拦截
    }
}
```

**交付物**:
- 完整的Database探针
- Source/Formula表达式解析器
- 指标采集和计算引擎
- 控制台输出格式

---

### 阶段3: 多探针协同 (HTTP + Database)
**目标**: 验证多探针协同工作和链路跟踪

**功能范围**:
- HTTP Server探针实现
- 多探针协同工作
- 链路跟踪和Trace ID
- 树状输出格式

**测试方式**:
```bash
# 测试HTTP请求链路
tf --filter "url.startsWith('/api/users')"

# 测试多探针协同
tf --filter "isSlowQuery == true || isSlowRequest == true"
```

**测试用例**:
```java
@RestController
public class UserController {
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable Long id) {
        // HTTP请求会被拦截
        User user = userService.findById(id); // 数据库查询会被拦截
        return user;
    }
}
```

**交付物**:
- HTTP Server探针
- 链路跟踪机制
- 多探针协同框架
- 树状输出实现

---

### 阶段4: 完整MVP (所有探针)
**目标**: 实现所有核心探针，完成MVP功能

**功能范围**:
- HTTP Client探针
- File Operations探针
- 完整的过滤功能
- JSON文件输出
- 性能优化

**测试方式**:
```bash
# 测试完整链路
tf --filter "url.startsWith('/api/users')" --output-file result.json

# 测试复杂过滤
tf --filter "isSlowQuery == true && isLargeFile == true"

# 测试文件操作
tf --filter "operationType in ['READ', 'WRITE']"
```

**测试用例**:
```java
@RestController
public class UserController {
    @GetMapping("/api/users/{id}")
    public User getUser(@PathVariable Long id) {
        // 1. HTTP请求
        User user = userService.findById(id);        // 2. 数据库查询
        String config = fileService.readConfig();    // 3. 文件读取
        ProfileData profile = httpClient.get(url);   // 4. HTTP客户端调用
        fileService.writeCache(profile);             // 5. 文件写入
        return user;
    }
}
```

**交付物**:
- 所有核心探针
- 完整的过滤引擎
- JSON输出功能
- 性能优化实现

---

### 阶段5: 增强功能 (可选)
**目标**: 添加高级功能和优化

**功能范围**:
- 堆栈跟踪功能
- 表达式缓存优化
- 用户自定义配置
- 详细模式输出

**测试方式**:
```bash
# 测试堆栈跟踪
tf --stack-trace-threshold 1000

# 测试详细模式
tf --verbose

# 测试自定义配置
tf --config-file custom-probes.json
```

---

## 7. 每阶段测试策略

### 7.1 单元测试
```java
// 表达式解析器测试
@Test
public void testSourceExpression() {
    ExecutionContext context = createMockContext();
    Object result = sourceParser.parse("this.toString()", context);
    assertEquals("SELECT * FROM users", result);
}

// 指标计算测试
@Test
public void testFormulaCalculation() {
    MetricsContext context = new MetricsContext();
    context.addMetric("endTime", 1000L);
    context.addMetric("startTime", 200L);

    Object result = formulaParser.parse("metrics.endTime - metrics.startTime", context);
    assertEquals(800L, result);
}
```

### 7.2 集成测试
```java
// 探针集成测试
@Test
public void testDatabaseProbe() {
    // 1. 配置探针
    ProbeConfig config = loadProbeConfig("database-probe.json");

    // 2. 执行数据库操作
    executeSlowQuery();

    // 3. 验证指标采集
    List<Metric> metrics = getCollectedMetrics();
    assertTrue(metrics.stream().anyMatch(m -> m.getName().equals("isSlowQuery") && (Boolean)m.getValue()));
}
```

### 7.3 端到端测试
```java
// 完整链路测试
@Test
public void testFullTrace() {
    // 1. 启动trace-flow
    TraceFlowCommand cmd = new TraceFlowCommand();
    cmd.setUrlPattern("/api/users/*");

    // 2. 发送HTTP请求
    String response = restTemplate.getForObject("/api/users/123", String.class);

    // 3. 验证跟踪结果
    TraceResult result = cmd.getLastTrace();
    assertNotNull(result.getTraceId());
    assertTrue(result.getNodes().size() >= 3); // HTTP + Database + 其他
}
```

### 7.4 性能测试
```bash
# 性能基准测试
tf --benchmark --requests 1000 '/api/users/*'

# 内存使用测试
tf --memory-profile '/api/users/*'

# 表达式解析性能
tf --expression-benchmark
```

---

## 8. 实现里程碑

| 阶段 | 时间估算 | 核心交付 | 测试重点 |
|------|----------|----------|----------|
| 阶段1 | 1-2周 | 基础框架 | 命令解析、配置加载 |
| 阶段2 | 2-3周 | Database探针 | 指标采集、表达式解析 |
| 阶段3 | 2-3周 | 多探针协同 | 链路跟踪、输出格式 |
| 阶段4 | 3-4周 | 完整MVP | 所有探针、过滤功能 |
| 阶段5 | 2-3周 | 增强功能 | 性能优化、高级功能 |

**总计**: 10-15周完成完整MVP
