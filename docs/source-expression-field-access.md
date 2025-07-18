# Source表达式字段访问功能

## 概述

Source表达式解析器现在支持字段访问功能，可以直接访问对象的字段，包括嵌套字段访问。

## 支持的表达式类型

### 1. 内置变量
```
startTime       - 方法开始执行时间
endTime         - 方法结束执行时间  
executionTime   - 方法执行耗时
threadName      - 执行线程名称
```

### 2. this方法调用
```
this.methodName()           - 调用当前对象的方法
```

### 3. this字段访问 ⭐ **新功能**
```
this.fieldName              - 访问当前对象的字段
this.field.subfield         - 访问嵌套字段
this.field.sub.subsub       - 支持多层嵌套
```

### 4. 参数访问
```
args[0]                     - 访问第一个参数
args[1].methodName()        - 调用参数的方法
```

### 5. 返回值访问
```
returnValue                 - 访问返回值
returnValue.methodName()    - 调用返回值的方法
```

## 字段访问示例

### Tomcat Http11Processor示例

```json
{
  "metrics": [
    {
      "name": "bufferEnd",
      "source": "this.inputBuffer.end",
      "type": "int"
    },
    {
      "name": "bufferPos", 
      "source": "this.inputBuffer.pos",
      "type": "int"
    },
    {
      "name": "requestMethod",
      "source": "this.request.method",
      "type": "string"
    },
    {
      "name": "bufferSize",
      "source": "this.inputBuffer.buf.length",
      "type": "int"
    }
  ]
}
```

### 实际使用场景

#### 1. 获取HTTP完整报文长度
```json
{
  "name": "httpMessageLength",
  "formula": "metrics.bufferEnd + getContentLength(this.request)",
  "type": "int"
}
```

#### 2. 监控缓冲区使用情况
```json
{
  "name": "bufferUsage",
  "formula": "(metrics.bufferEnd * 100) / metrics.bufferSize",
  "type": "double"
}
```

## 技术实现

### 字段访问机制

1. **反射访问**：使用Java反射API访问字段
2. **权限处理**：自动设置字段可访问性
3. **继承支持**：支持访问父类字段
4. **空值处理**：安全处理null对象

### 嵌套字段解析

```java
// 表达式: this.inputBuffer.end
// 解析过程:
// 1. 获取 this.inputBuffer 字段
// 2. 从 inputBuffer 对象获取 end 字段
// 3. 返回 end 字段的值
```

### 错误处理

- **字段不存在**：返回null，不抛出异常
- **对象为null**：返回null，不抛出异常
- **访问权限**：自动设置字段可访问
- **类型转换**：保持原始类型

## 配置示例

### 完整的Http11Processor探针配置

```json
{
  "name": "HTTP11 Processor探针",
  "description": "监控Tomcat Http11Processor处理HTTP请求",
  "enabled": true,
  "metrics": [
    {
      "name": "inputBufferEnd",
      "source": "this.inputBuffer.end",
      "type": "int",
      "capturePoint": "before"
    },
    {
      "name": "requestMethod",
      "source": "this.request.method", 
      "type": "string",
      "capturePoint": "before"
    },
    {
      "name": "headerLength",
      "formula": "metrics.inputBufferEnd - this.inputBuffer.pos",
      "type": "int"
    }
  ],
  "output": {
    "template": "[HTTP] ${requestMethod} - Header: ${headerLength}B"
  }
}
```

## 性能考虑

### 优化建议

1. **缓存字段访问**：频繁访问的字段可以考虑缓存
2. **避免深层嵌套**：过深的字段访问可能影响性能
3. **异常处理**：字段访问失败不会影响主流程

### 性能对比

| 访问方式 | 性能 | 安全性 | 灵活性 |
|---------|------|--------|--------|
| 方法调用 | 中等 | 高 | 中等 |
| 字段访问 | 高 | 中等 | 高 |
| 反射访问 | 低 | 低 | 最高 |

## 最佳实践

### 1. 字段命名
- 使用准确的字段路径
- 避免访问可能变化的内部字段

### 2. 错误处理
- 总是检查返回值是否为null
- 使用formula进行计算时注意null值

### 3. 类型安全
- 在配置中正确指定字段类型
- 使用适当的类型转换

## 故障排除

### 常见问题

1. **字段访问返回null**
   - 检查字段名是否正确
   - 确认对象不为null
   - 验证字段在目标类中存在

2. **NoSuchFieldException**
   - 字段名拼写错误
   - 字段在父类中，需要检查继承关系
   - 字段可能是私有的，但这通常不是问题

3. **性能问题**
   - 避免在高频方法中使用复杂的字段访问
   - 考虑使用方法调用替代深层字段访问

### 调试技巧

```json
{
  "name": "debugField",
  "source": "this.someField",
  "type": "string",
  "capturePoint": "before"
}
```

通过添加调试字段来验证字段访问是否正常工作。
