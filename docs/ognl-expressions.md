# OGNL表达式支持

## 概述

Arthas trace-flow现在支持OGNL（Object-Graph Navigation Language）表达式，提供强大的数据提取和计算能力。

参考资料：
- [Arthas OGNL用法](https://github.com/alibaba/arthas/issues/71)
- [OGNL官方指南](https://commons.apache.org/dormant/commons-ognl/language-guide.html)

## 基本语法

### 1. 字段访问
```
this.inputBuffer.end                    # 访问嵌套字段
this.inputBuffer.buf.length             # 访问数组长度
```

### 2. 方法调用
```
args[0].getMethod()                     # 无参数方法
args[0].getHeader("content-length")     # 带参数方法
args[0].getRequestURI().toUpperCase()   # 链式调用
```

### 3. 数学运算
```
this.inputBuffer.end - this.inputBuffer.pos    # 减法
(this.inputBuffer.end * 100) / 2048            # 乘除法
```

### 4. 条件表达式
```
args[0].getMethod() == "POST" ? "POST请求" : "其他请求"
this.inputBuffer.end > 500 ? "大缓冲区" : "小缓冲区"
```

### 5. 逻辑运算
```
this.inputBuffer.end > 500 && args[0].getHeader("content-length") != null
args[0].getMethod() == "GET" || args[0].getMethod() == "POST"
```

### 6. 字符串操作
```
args[0].getHeader("content-type").contains("json")
args[0].getRequestURI().substring(0, 10)
args[0].getHeader("user-agent").length()
```

## 内置变量

OGNL表达式中可以使用以下变量：

```
this         # 当前对象（方法所属对象）
target       # 同this
args         # 方法参数数组
returnValue  # 方法返回值
startTime    # 方法开始时间
endTime      # 方法结束时间
executionTime # 方法执行时间
threadName   # 执行线程名
utils        # 工具类实例
```

## 工具方法

通过`@utils@`可以访问内置工具方法：

```
@utils@parseInt("123")              # 字符串转整数
@utils@parseLong("123456789")       # 字符串转长整数
@utils@isEmpty(str)                 # 检查字符串是否为空
@utils@isNotEmpty(str)              # 检查字符串是否不为空
@utils@formatTime(timestamp)        # 格式化时间戳
@utils@getClassName(obj)            # 获取对象类名
@utils@safeToString(obj)            # 安全的toString
```

## 实际应用示例

### 1. HTTP请求分析
```json
{
  "name": "requestType",
  "source": "args[0].getHeader(\"content-type\") != null && args[0].getHeader(\"content-type\").contains(\"json\") ? \"JSON\" : \"OTHER\"",
  "type": "string"
}
```

### 2. 性能分析
```json
{
  "name": "performanceLevel",
  "source": "executionTime < 10 ? \"EXCELLENT\" : (executionTime < 50 ? \"GOOD\" : \"SLOW\")",
  "type": "string"
}
```

### 3. 缓冲区使用率
```json
{
  "name": "bufferUsage",
  "source": "this.inputBuffer.end > 0 ? (this.inputBuffer.end * 100.0) / this.inputBuffer.buf.length : 0",
  "type": "double"
}
```

### 4. 复杂条件判断
```json
{
  "name": "isLargeRequest",
  "source": "@utils@parseInt(args[0].getHeader(\"content-length\")) > 1024 && args[0].getMethod() == \"POST\"",
  "type": "boolean"
}
```

### 5. 字符串处理
```json
{
  "name": "shortUserAgent",
  "source": "args[0].getHeader(\"user-agent\") != null ? (args[0].getHeader(\"user-agent\").length() > 50 ? args[0].getHeader(\"user-agent\").substring(0, 50) + \"...\" : args[0].getHeader(\"user-agent\")) : \"unknown\"",
  "type": "string"
}
```

## 高级特性

### 1. 集合操作
```
args[0].getHeaderNames().size()         # 获取头部数量
args[0].getParameterMap().keySet()      # 获取参数名集合
```

### 2. 类型转换
```
@utils@parseInt(args[0].getHeader("content-length"))
(Integer) args[0].getAttribute("someNumber")
```

### 3. 空值安全
```
args[0].getHeader("x-custom") != null ? args[0].getHeader("x-custom") : "default"
@utils@isEmpty(args[0].getHeader("authorization")) ? "anonymous" : "authenticated"
```

### 4. 复杂计算
```
# 计算请求处理效率（字节/毫秒）
(@utils@parseInt(args[0].getHeader("content-length")) > 0 && executionTime > 0) ? 
    @utils@parseInt(args[0].getHeader("content-length")) / executionTime : 0
```

## 性能考虑

### 1. 表达式复杂度
- 简单表达式：字段访问、基本运算
- 复杂表达式：多层嵌套、条件判断、字符串操作

### 2. 缓存机制
- OGNL表达式会被编译和缓存
- 相同表达式的重复执行性能较好

### 3. 最佳实践
- 避免过于复杂的嵌套表达式
- 使用工具方法处理类型转换
- 注意空值检查

## 故障排除

### 1. 表达式语法错误
```
错误：args[0].getHeader(content-length)
正确：args[0].getHeader("content-length")
```

### 2. 空指针异常
```
不安全：args[0].getHeader("x-custom").length()
安全：args[0].getHeader("x-custom") != null ? args[0].getHeader("x-custom").length() : 0
```

### 3. 类型转换错误
```
不安全：(Integer) args[0].getHeader("content-length")
安全：@utils@parseInt(args[0].getHeader("content-length"))
```

## 兼容性

- **向后兼容**：原有的简单表达式仍然支持
- **自动检测**：系统自动判断是否使用OGNL解析
- **降级处理**：OGNL解析失败时自动尝试传统解析

## 示例配置

完整的探针配置示例请参考：`probes/ognl-advanced-probe.json`
