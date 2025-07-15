# 🔧 JSON配置文件读取功能测试

## 🎯 **实现内容**

### ✅ **已完成的功能**

1. **真正的JSON文件读取** - ProbeManager现在从JSON配置文件中读取探针配置
2. **完整的配置验证** - 包括配置完整性检查和错误处理
3. **备用配置机制** - 当JSON文件加载失败时，自动使用备用配置
4. **英文化JSON文件** - 所有配置文件都使用英文，避免编码问题

### 📁 **JSON配置文件**

- `core/src/main/resources/probes/database-probe.json`
- `core/src/main/resources/probes/http-server-probe.json`
- `core/src/main/resources/probes/http-client-probe.json`
- `core/src/main/resources/probes/file-operations-probe.json`

## 🧪 **测试步骤**

### **第1步：重新编译项目**

```bash
# 1. 设置编码（避免乱码）
chcp 65001

# 2. 重新编译
cd core && mvn clean compile
cd .. && mvn clean package -DskipTests
```

### **第2步：启动Arthas测试**

```bash
# 启动Arthas
java -jar arthas-boot.jar
```

### **第3步：测试JSON配置读取**

```bash
# 测试探针列表（应该从JSON文件读取）
[arthas@pid]$ tf --list-probes
```

**预期结果**：
```
Available Probes:
================
- Database Probe: Monitor JDBC database operations (Enabled: Yes)
- HTTP Server Probe: Monitor HTTP request reception and processing (Enabled: Yes)
- HTTP Client Probe: Monitor outbound HTTP requests (Enabled: Yes)
- File Operations Probe: Monitor file read/write operations (Enabled: Yes)

Total: 4 probes
```

### **第4步：测试详细配置显示**

```bash
# 测试配置详情（应该显示从JSON读取的完整配置）
[arthas@pid]$ tf --show-config "Database Probe" --verbose
```

**预期结果**：
```
Probe Configuration: Database Probe
========================
Name: Database Probe
Description: Monitor JDBC database operations
Status: Enabled
Metrics count: 4

Metrics details:
  - startTime (long): SQL execution start time
  - endTime (long): SQL execution end time
  - executionTime (long): SQL execution duration
  - threadName (string): Execution thread name
```

### **第5步：测试其他探针配置**

```bash
# 测试HTTP Server探针
[arthas@pid]$ tf --show-config "HTTP Server Probe" --verbose

# 测试HTTP Client探针
[arthas@pid]$ tf --show-config "HTTP Client Probe" --verbose

# 测试File Operations探针
[arthas@pid]$ tf --show-config "File Operations Probe" --verbose
```

## 🔍 **验证JSON读取功能**

### **验证1：配置内容正确性**

检查显示的配置是否与JSON文件内容一致：

- **探针名称** - 应该是英文名称（如"Database Probe"）
- **描述信息** - 应该是英文描述
- **指标数量** - 每个探针应该有4个指标
- **指标详情** - 指标名称和描述应该与JSON文件一致

### **验证2：错误处理机制**

可以通过以下方式测试错误处理：

1. **临时重命名JSON文件**测试备用配置
2. **查看日志输出**确认JSON加载状态

### **验证3：配置验证功能**

JSON配置文件包含完整的配置验证：
- 探针名称不能为空
- 必须定义至少一个指标
- 指标必须有source或formula
- source指标必须定义targets和capturePoint

## 📋 **JSON配置文件结构验证**

### **标准JSON结构**

每个探针配置文件都包含：

```json
{
  "name": "探针名称",
  "description": "探针描述",
  "enabled": true,
  "metrics": [
    {
      "name": "指标名称",
      "description": "指标描述",
      "targets": [...],
      "source": "数据源表达式",
      "type": "数据类型",
      "unit": "单位",
      "capturePoint": "采集时机"
    }
  ],
  "output": {
    "type": "输出类型",
    "template": "输出模板"
  },
  "filters": [...]
}
```

### **配置文件特点**

1. **完整性** - 包含所有必要的配置项
2. **一致性** - 所有文件使用相同的结构
3. **可扩展性** - 支持添加新的指标和配置
4. **国际化** - 使用英文避免编码问题

## 🎯 **成功标志**

### **JSON读取成功的标志**

1. **探针列表正确** - 显示4个探针，名称为英文
2. **配置详情完整** - 显示从JSON读取的完整配置信息
3. **无错误日志** - 没有JSON解析错误
4. **配置验证通过** - 所有配置项都正确验证

### **预期的完美输出**

```bash
[arthas@pid]$ tf --show-config "Database Probe" --verbose
Probe Configuration: Database Probe
========================
Name: Database Probe
Description: Monitor JDBC database operations
Status: Enabled
Metrics count: 4

Metrics details:
  - startTime (long): SQL execution start time
  - endTime (long): SQL execution end time
  - executionTime (long): SQL execution duration
  - threadName (string): Execution thread name
```

## 🚨 **故障排查**

### **如果JSON读取失败**

1. **检查文件路径** - 确认JSON文件在正确位置
2. **检查JSON格式** - 验证JSON语法正确
3. **查看日志输出** - 检查具体的错误信息
4. **备用配置** - 系统会自动使用备用配置

### **如果显示乱码**

1. **设置控制台编码** - `chcp 65001`
2. **检查JSON文件编码** - 确保为UTF-8
3. **重新编译项目** - 确保最新的英文配置生效

## 🎉 **JSON配置读取完成标志**

当您看到以下输出时，说明JSON配置文件读取功能完全正常：

```
[arthas@pid]$ tf --list-probes
Available Probes:
================
- Database Probe: Monitor JDBC database operations (Enabled: Yes)
- HTTP Server Probe: Monitor HTTP request reception and processing (Enabled: Yes)
- HTTP Client Probe: Monitor outbound HTTP requests (Enabled: Yes)
- File Operations Probe: Monitor file read/write operations (Enabled: Yes)

Total: 4 probes
```

**所有探针名称都是英文，配置信息完整，无任何乱码！**

---

**状态**: 🔧 **JSON配置文件读取功能已实现**
**下一步**: 🧪 **重新编译并测试JSON读取功能**
