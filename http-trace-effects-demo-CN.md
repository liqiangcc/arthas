# Arthas `http-trace` & `config`: 效果演示文档

本文档旨在通过示例展示 `http-trace` 和 `config` 命令的预期行为、输出和配置方法。

## 1. `http-trace` 命令实战

### 场景 1: 基本跟踪

跟踪下一个访问 `/api/user/info` 的请求，并在控制台显示结果。

**命令:**
```bash
ht --url-pattern /api/user/info
```

**预期控制台输出:**
```
+------------------------------------------------------------------------------------+
| Arthas-Trace-Id: a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8                               |
| 请求跟踪: GET /api/user/info?id=123                                                |
+------------------------------------------------------------------------------------+
| 总耗时: 85ms                                                                       |
+------------------------------------------------------------------------------------+
  |
  '---[80ms] JDBC: SELECT * FROM user WHERE id = ?
  |    |
  |    '---[2ms] Redis: GET user:cache:123
  |
  '---[5ms] Kafka: SEND topic=user-activity, key=123
```

### 场景 2: 跟踪慢 SQL 并捕获堆栈

跟踪一个数据库调用耗时超过 100ms 的请求，并捕获其线程调用堆栈。

**命令:**
```bash
ht --url-pattern /api/slow_query --stack-trace-threshold 100
```

**预期控制台输出:**
```
+------------------------------------------------------------------------------------+
| Arthas-Trace-Id: b2c3d4e5-f6g7-8901-h2i3-j4k5l6m7n8o9                               |
| 请求跟踪: POST /api/slow_query                                                     |
+------------------------------------------------------------------------------------+
| 总耗时: 155ms                                                                      |
+------------------------------------------------------------------------------------+
  |
  '---[150ms] JDBC: SELECT * FROM very_slow_table WHERE ...
       |
       '--- 调用堆栈:
            at com.mycorp.dao.SlowDao.findData(SlowDao.java:88)
            at com.mycorp.service.DataService.getData(DataService.java:42)
            at com.mycorp.controller.ApiController.post(ApiController.java:25)
            ... (更多堆栈信息可在 verbose 模式下查看)
```

### 场景 3: 文件输出 (默认 JSON 格式)

跟踪一个请求，并将完整的跟踪详情保存到文件。

**命令:**
```bash
ht --url-pattern /api/order/create --output /tmp/traces.log
```

**`/tmp/traces.log` 文件内容 (单行追加):**
```json
{"arthasTraceId":"c3d4e5f6-g7h8-9012-i3j4-k5l6m7n8o9p0","businessTraceId":null,"request":{"url":"/api/order/create","method":"POST"},"totalTimeMs":155,"timestamp":"2023-10-27T10:30:00.000Z","traceTree":{"type":"HTTP_REQUEST","durationMs":155,"details":{"url":"/api/order/create"},"children":[{"type":"JDBC","durationMs":150,"details":{"sql":"INSERT INTO orders ..."},"children":[],"stackTrace":null}]}}
```

### 场景 4: 文件输出 (自定义格式)

跟踪一个请求，并以自定义的管道符分隔格式保存输出。

**命令:**
```bash
ht --url-pattern /api/** --output /tmp/prod_traffic.log --output-format "${timestamp} | ${arthasTraceId} | ${request.method} ${request.url} | ${totalTimeMs}ms"
```

**`/tmp/prod_traffic.log` 文件内容 (单行追加):**
```
2023-10-27T10:35:00.000Z | d4e5f6g7-h8i9-0123-j4k5-l6m7n8o9p0q1 | POST /api/inventory/update | 55ms
```

### 场景 5: 提取业务 Trace ID

跟踪一个请求，并从 `X-Trace-Id` 请求头中提取业务跟踪 ID。

**命令:**
```bash
ht --url-pattern /api/** --trace-id-expr '#request.getHeader("X-Trace-Id")'
```

**预期控制台输出头部:**
```
+------------------------------------------------------------------------------------+
| Arthas-Trace-Id: e5f6g7h8-i9j0-1234-k5l6-m7n8o9p0q1r2                               |
| Business-Trace-Id: app-trace-id-98765                                              |
| 请求跟踪: GET /api/user/profile                                                    |
+------------------------------------------------------------------------------------+
```

## 2. `config` 命令实战

### 场景 1: 设置和查看全局配置

**命令:**
```bash
# 设置一个全局默认的输出文件
config --global http-trace.output /var/log/arthas/all_traces.log

# 设置一个全局的堆栈跟踪阈值
config --global http-trace.stack-trace-threshold 150

# 查看配置
config --list
```

**`config --list` 的预期输出:**
```
http-trace.output=/var/log/arthas/all_traces.log
http-trace.stack-trace-threshold=150
```

### 场景 2: 设置项目级配置

**命令 (在 `/app/my-project/` 目录下执行):**
```bash
# 设置一个项目专属的 URL 匹配模式
config http-trace.url-pattern /api/v2/**

# 仅为此项目覆盖全局的输出文件配置
config http-trace.output /app/my-project/logs/trace.log
```

**`./arthas.properties` 文件内容:**
```properties
http-trace.url-pattern = /api/v2/**
http-trace.output = /app/my-project/logs/trace.log
```

### 场景 3: 使用分层配置运行 `http-trace`

现在，当在 `/app/my-project/` 目录下运行时，`ht` 命令将会使用合并后的配置。

**命令:**
```bash
# 无需任何参数，命令会自动加载配置值！
ht
```

**行为分析:**
- 跟踪匹配 `/api/v2/**` 的请求 (来自项目配置)。
- 输出到 `/app/my-project/logs/trace.log` (项目配置覆盖了全局配置)。
- 对耗时超过 `150ms` 的调用捕获堆栈 (来自全局配置)。

### 场景 4: 使用命令行参数覆盖所有配置

即使设置了所有配置，命令行参数总是拥有最高优先级。

**命令:**
```bash
# 这将跟踪一个特定的 URL，忽略所有已配置的 url-pattern
ht --url-pattern /api/legacy/debug
```
