# Arthas `http-trace` & `config`: Effects Demonstration

This document demonstrates the expected behavior, output, and configuration examples for the `http-trace` and `config` commands.

## 1. `http-trace` Command in Action

### Scenario 1: Basic Tracing

Trace the next request to `/api/user/info` and display the result in the console.

**Command:**
```bash
ht --url-pattern /api/user/info
```

**Expected Console Output:**
```
+------------------------------------------------------------------------------------+
| Arthas-Trace-Id: a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8                               |
| Request Trace: GET /api/user/info?id=123                                           |
+------------------------------------------------------------------------------------+
| Total Time: 85ms                                                                   |
+------------------------------------------------------------------------------------+
  |
  '---[80ms] JDBC: SELECT * FROM user WHERE id = ?
  |    |
  |    '---[2ms] Redis: GET user:cache:123
  |
  '---[5ms] Kafka: SEND topic=user-activity, key=123
```

### Scenario 2: Tracing with a Slow SQL and Stack Trace Capture

Trace a request where a database call exceeds 100ms, and capture its stack trace.

**Command:**
```bash
ht --url-pattern /api/slow_query --stack-trace-threshold 100
```

**Expected Console Output:**
```
+------------------------------------------------------------------------------------+
| Arthas-Trace-Id: b2c3d4e5-f6g7-8901-h2i3-j4k5l6m7n8o9                               |
| Request Trace: POST /api/slow_query                                                |
+------------------------------------------------------------------------------------+
| Total Time: 155ms                                                                  |
+------------------------------------------------------------------------------------+
  |
  '---[150ms] JDBC: SELECT * FROM very_slow_table WHERE ...
       |
       '--- StackTrace:
            at com.mycorp.dao.SlowDao.findData(SlowDao.java:88)
            at com.mycorp.service.DataService.getData(DataService.java:42)
            at com.mycorp.controller.ApiController.post(ApiController.java:25)
            ... (more lines in verbose mode)
```

### Scenario 3: File Output (Default JSON)

Trace a request and save the full trace details to a file.

**Command:**
```bash
ht --url-pattern /api/order/create --output /tmp/traces.log
```

**`/tmp/traces.log` Content (a single line is appended):**
```json
{"arthasTraceId":"c3d4e5f6-g7h8-9012-i3j4-k5l6m7n8o9p0","businessTraceId":null,"request":{"url":"/api/order/create","method":"POST"},"totalTimeMs":155,"timestamp":"2023-10-27T10:30:00.000Z","traceTree":{"type":"HTTP_REQUEST","durationMs":155,"details":{"url":"/api/order/create"},"children":[{"type":"JDBC","durationMs":150,"details":{"sql":"INSERT INTO orders ..."},"children":[],"stackTrace":null}]}}
```

### Scenario 4: File Output (Custom Format)

Trace a request and save the output in a custom, pipe-delimited format.

**Command:**
```bash
ht --url-pattern /api/** --output /tmp/prod_traffic.log --output-format "${timestamp} | ${arthasTraceId} | ${request.method} ${request.url} | ${totalTimeMs}ms"
```

**`/tmp/prod_traffic.log` Content (a single line is appended):**
```
2023-10-27T10:35:00.000Z | d4e5f6g7-h8i9-0123-j4k5-l6m7n8o9p0q1 | POST /api/inventory/update | 55ms
```

### Scenario 5: Extracting a Business Trace ID

Trace a request and extract a business trace ID from the `X-Trace-Id` header.

**Command:**
```bash
ht --url-pattern /api/** --trace-id-expr '#request.getHeader("X-Trace-Id")'
```

**Expected Console Output Header:**
```
+------------------------------------------------------------------------------------+
| Arthas-Trace-Id: e5f6g7h8-i9j0-1234-k5l6-m7n8o9p0q1r2                               |
| Business-Trace-Id: app-trace-id-98765                                              |
| Request Trace: GET /api/user/profile                                               |
+------------------------------------------------------------------------------------+
```

## 2. `config` Command in Action

### Scenario 1: Setting and Viewing Global Configuration

**Commands:**
```bash
# Set a global default for the output file
config --global http-trace.output /var/log/arthas/all_traces.log

# Set a global threshold for stack traces
config --global http-trace.stack-trace-threshold 150

# View the configuration
config --list
```

**Expected `config --list` Output:**
```
http-trace.output=/var/log/arthas/all_traces.log
http-trace.stack-trace-threshold=150
```

### Scenario 2: Setting Project-specific Configuration

**Commands (run from `/app/my-project/`):**
```bash
# Set a project-specific URL pattern
config http-trace.url-pattern /api/v2/**

# Override the global output file for this project only
config http-trace.output /app/my-project/logs/trace.log
```

**`./arthas.properties` File Content:**
```properties
http-trace.url-pattern = /api/v2/**
http-trace.output = /app/my-project/logs/trace.log
```

### Scenario 3: Running `http-trace` with Layered Configurations

Now, when running `ht` from within the `/app/my-project/` directory, it will use the combined configuration.

**Command:**
```bash
# No parameters needed, it will use the config values!
ht
```

**Behavior:**
- Traces requests matching `/api/v2/**` (from project config).
- Outputs to `/app/my-project/logs/trace.log` (project config overrides global).
- Captures stack traces for calls longer than `150ms` (from global config).

### Scenario 4: Overriding All Configurations with a Command-line Argument

Even with all the configurations, a command-line argument always wins.

**Command:**
```bash
# This will trace the specific URL, ignoring all configured url-patterns
ht --url-pattern /api/legacy/debug
```
