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
- **FR-2**: 支持URL模式匹配：`tf '/api/**'` 或 `tf -u '/user/*'`
- **FR-3**: 支持跟踪次数限制：`tf -n 5` (跟踪5次请求)
- **FR-4**: 无参数时跟踪下一个HTTP请求

### 2.2 链路跟踪能力

- **FR-5**: 自动跟踪HTTP请求的完整执行链路：
  - HTTP请求接收 (Servlet/Spring MVC)
  - 数据库操作 (JDBC PreparedStatement/Statement)
  - HTTP客户端调用 (HttpClient/OkHttp/RestTemplate)
  - 文件读写操作 (FileInputStream/FileOutputStream/Files)

### 2.3 指标驱动过滤

- **FR-6**: 支持基于指标的过滤表达式，所有过滤都基于探针定义的指标：
  - `--filter "executionTime > 1000"` - 基于executionTime指标过滤慢请求
  - `--filter "sql.contains('user')"` - 基于sql指标过滤包含'user'的SQL操作
  - `--filter "operationType == 'SELECT'"` - 基于operationType指标过滤查询操作
  - `--filter "isSlowQuery == true"` - 基于isSlowQuery指标过滤慢查询
  - `--filter "status >= 400"` - 基于status指标过滤错误请求

- **FR-6.1**: 指标驱动过滤的优势：
  - **类型安全**: 基于指标的类型定义进行过滤，避免类型错误
  - **语义清晰**: 过滤条件直接对应业务指标，易于理解
  - **可扩展**: 新增指标后自动支持相应的过滤功能

### 2.4 内置探针配置

- **FR-7**: MVP版本内置4个核心探针，基于预定义的配置文件实现：
  - **HTTP Server探针**: `http-server-probe.json` - 监控Servlet/Spring MVC请求处理
  - **Database探针**: `database-probe.json` - 监控JDBC SQL执行
  - **HTTP Client探针**: `http-client-probe.json` - 监控出站HTTP请求
  - **File Operations探针**: `file-operations-probe.json` - 监控文件读写操作

- **FR-7.1**: 内置探针配置文件特点：
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
          "class": "目标类名",
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

# 跟踪API请求
tf '/api/**'

# 跟踪5次用户相关请求
tf -u '/user/*' -n 5

# 只显示慢请求 (>1秒)
tf --filter "executionTime > 1000"

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

# 分析写操作（基于计算指标）
tf --filter "operationType in ['INSERT', 'UPDATE', 'DELETE']"

# 分析高吞吐量的查询
tf --filter "throughputScore > 100"

# 分析队列效率低的请求
tf --filter "queueEfficiency < 80"
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

## 5. 实现优先级

### P0 (必须实现)
- 基本的trace-flow命令和URL匹配
- 配置文件解析和加载机制
- HTTP Server + Database探针配置
- 控制台树状输出
- 基础指标和计算指标支持

### P1 (重要)
- HTTP Client + File Operations探针配置
- 基础过滤功能（基于计算指标）
- JSON文件输出
- 指标级别targets支持

### P2 (可选)
- 堆栈跟踪功能
- 详细模式输出
- 用户自定义配置文件支持
- 高级计算指标和聚合功能
