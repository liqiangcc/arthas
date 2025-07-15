# 🎯 阶段1验收测试指南

## 🎉 **当前状态**

✅ **布尔选项修复成功** - `--list-probes` 现在可以正常工作
✅ **命令注册成功** - `trace-flow` 和 `tf` 都被正确识别
🔧 **中文编码修复** - 已将探针名称改为英文避免乱码

## 🧪 **完整的阶段1验收测试**

### **重新编译测试修复**

```bash
# 1. 重新编译（修复中文编码）
cd core && mvn clean compile
cd .. && mvn clean package -DskipTests

# 2. 重新启动Arthas
java -jar arthas-boot.jar
```

### **测试1: 基础命令识别**

```bash
# 测试命令是否被识别
[arthas@pid]$ help trace-flow
[arthas@pid]$ help tf

# 预期结果: 显示命令的基本信息，不显示"command not found"
```

### **测试2: 帮助系统**

```bash
# 测试帮助信息
[arthas@pid]$ trace-flow --help
[arthas@pid]$ tf --help
[arthas@pid]$ trace-flow -h
[arthas@pid]$ tf -h

# 预期结果: 显示完整的帮助信息，包括所有参数说明和示例
```

### **测试3: 探针列表功能**

```bash
# 测试探针列表
[arthas@pid]$ trace-flow --list-probes
[arthas@pid]$ tf --list-probes

# 预期结果: 显示4个探针，无中文乱码
# Available Probes:
# ================
# - Database Probe: Monitor JDBC database operations (Enabled: Yes)
# - HTTP Server Probe: Monitor HTTP request reception and processing (Enabled: Yes)
# - HTTP Client Probe: Monitor outbound HTTP requests (Enabled: Yes)
# - File Operations Probe: Monitor file read/write operations (Enabled: Yes)
# 
# Total: 4 probes
```

### **测试4: 探针配置详情**

```bash
# 测试探针配置显示
[arthas@pid]$ trace-flow --show-config "Database Probe"
[arthas@pid]$ tf --show-config "HTTP Server Probe"

# 预期结果: 显示探针的详细配置
# Probe Configuration: Database Probe
# ========================
# Name: Database Probe
# Description: Monitor JDBC database operations
# Status: Enabled
# Metrics count: 4
```

### **测试5: 详细模式**

```bash
# 测试详细模式
[arthas@pid]$ trace-flow --show-config "Database Probe" --verbose
[arthas@pid]$ tf --show-config "Database Probe" --verbose

# 预期结果: 显示指标详情
# Metrics details:
#   - startTime (long): Start time
#   - endTime (long): End time
#   - executionTime (long): Execution time
#   - threadName (string): Thread name
```

### **测试6: 基础跟踪功能**

```bash
# 测试基础跟踪
[arthas@pid]$ trace-flow
[arthas@pid]$ tf
[arthas@pid]$ trace-flow -n 3
[arthas@pid]$ tf -n 5

# 预期结果: 显示跟踪启动信息
# Starting HTTP request tracing...
# Trace count: 1
# Press Ctrl+C to stop tracing
# ========================
# Waiting for HTTP requests...
```

### **测试7: 参数组合**

```bash
# 测试各种参数组合
[arthas@pid]$ tf -n 3 --verbose
[arthas@pid]$ trace-flow --filter "executionTime > 1000"
[arthas@pid]$ tf --output-file /tmp/trace.json
[arthas@pid]$ trace-flow -n 2 --verbose --filter "url.contains('/api')"

# 预期结果: 参数被正确解析并显示
# Starting HTTP request tracing...
# Trace count: 3
# Filter condition: executionTime > 1000
# Output file: /tmp/trace.json
```

### **测试8: 错误处理**

```bash
# 测试错误参数
[arthas@pid]$ trace-flow -n 0
[arthas@pid]$ trace-flow -n -1
[arthas@pid]$ tf --show-config "NonExistentProbe"

# 预期结果: 显示适当的错误信息
# Error: Trace count must be greater than 0
# Probe not found: NonExistentProbe
# Use --list-probes to see all available probes
```

### **测试9: 别名一致性**

```bash
# 测试功能一致性
[arthas@pid]$ trace-flow --list-probes
[arthas@pid]$ tf --list-probes
# 输出应该完全一致

[arthas@pid]$ trace-flow -n 2 --verbose
[arthas@pid]$ tf -n 2 --verbose
# 输出应该完全一致
```

## 📋 **阶段1验收清单**

### **✅ 核心功能验收**

- [ ] **命令注册** - `help trace-flow` 和 `help tf` 正常显示
- [ ] **帮助系统** - `--help` 显示完整帮助信息
- [ ] **探针列表** - `--list-probes` 显示4个探针，无乱码
- [ ] **探针配置** - `--show-config` 显示正确配置信息
- [ ] **详细模式** - `--verbose` 显示额外的调试信息
- [ ] **参数解析** - 所有参数被正确解析和显示
- [ ] **错误处理** - 无效参数显示适当错误信息
- [ ] **别名功能** - `tf` 和 `trace-flow` 功能完全一致

### **✅ 输出质量验收**

- [ ] **无编码问题** - 所有输出为英文，无乱码
- [ ] **格式正确** - 输出格式清晰易读
- [ ] **信息完整** - 所有必要信息都正确显示
- [ ] **错误友好** - 错误信息清晰有用

### **✅ 架构验收**

- [ ] **命令框架** - 正确集成到Arthas命令系统
- [ ] **配置系统** - 探针配置正确加载和管理
- [ ] **表达式解析** - 基础表达式解析框架就位
- [ ] **过滤引擎** - 基础过滤框架就位
- [ ] **输出格式化** - 输出格式化系统正常工作

## 🎯 **阶段1完成标准**

### **必须达到的标准**

当您能成功执行以下测试序列并看到预期输出时，说明阶段1完成：

```bash
[arthas@pid]$ tf --list-probes
Available Probes:
================
- Database Probe: Monitor JDBC database operations (Enabled: Yes)
- HTTP Server Probe: Monitor HTTP request reception and processing (Enabled: Yes)
- HTTP Client Probe: Monitor outbound HTTP requests (Enabled: Yes)
- File Operations Probe: Monitor file read/write operations (Enabled: Yes)

Total: 4 probes

[arthas@pid]$ tf --show-config "Database Probe" --verbose
Probe Configuration: Database Probe
========================
Name: Database Probe
Description: Monitor JDBC database operations
Status: Enabled
Metrics count: 4

Metrics details:
  - startTime (long): Start time
  - endTime (long): End time
  - executionTime (long): Execution time
  - threadName (string): Thread name

[arthas@pid]$ tf -n 3 --verbose
Starting HTTP request tracing...
Trace count: 3
Press Ctrl+C to stop tracing
========================
Waiting for HTTP requests...

[DEBUG] This is Stage 1 mock output
[DEBUG] Actual tracing will be implemented in Stage 2
```

## 🚀 **阶段1成功标志**

### **✅ 如果看到以上所有输出，恭喜您！**

**🎉 阶段1开发完成！**

- ✅ 命令框架完全就位
- ✅ 配置系统正常工作
- ✅ 基础架构搭建完成
- ✅ 所有核心组件可用
- ✅ 为阶段2开发奠定了坚实基础

### **🔄 准备进入阶段2**

阶段2将实现：
- 真实的方法拦截
- 完整的表达式解析
- 实际的链路跟踪
- JSON文件输出
- 性能优化

---

**当前状态**: 🎯 **阶段1验收测试中**
**下一步**: 🧪 **执行完整测试，确认阶段1完成**
