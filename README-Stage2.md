# Arthas trace-flow 命令 - 阶段2实现

## 🎯 阶段2目标

实现完整的Database探针功能，从模拟输出转向真实的JDBC方法拦截和数据采集。

## 📁 新增组件

### 核心组件
```
core/src/main/java/com/taobao/arthas/core/command/trace/
├── InterceptorManager.java              # 拦截器管理器
├── TraceFlowAdviceListener.java         # Arthas增强框架集成
├── ConfigurableMethodInterceptor.java   # 配置驱动拦截器（已扩展）
├── SourceExpressionParser.java          # 表达式解析器（已扩展）
└── FormulaExpressionParser.java         # Formula表达式解析器（已扩展）
```

### 测试脚本
```
scripts/
└── test-stage2.bat                      # Windows测试脚本
```

## 🚀 阶段2新功能

### 1. 真实方法拦截
- ✅ **InterceptorManager**: 管理所有方法拦截器
- ✅ **字节码增强集成**: 使用Arthas的Enhancer框架
- ✅ **TraceFlowAdviceListener**: 连接增强框架和拦截器
- ✅ **动态拦截器注册**: 支持运行时注册和管理拦截器

### 2. 扩展表达式解析
- ✅ **this.method()调用**: 支持 `this.toString()` 等方法调用
- ✅ **参数访问**: 支持 `args[0]` 和 `args[0].getValue()` 
- ✅ **返回值访问**: 支持 `returnValue` 和 `returnValue.getResultSet()`
- ✅ **Formula计算**: 支持 `metrics.endTime - metrics.startTime` 等计算

### 3. 改进输出格式
- ✅ **时间戳显示**: 精确到毫秒的时间戳
- ✅ **结构化输出**: 清晰的层次结构
- ✅ **Database特定信息**: SQL语句、参数、结果提取
- ✅ **异常处理**: 完善的异常信息显示

### 4. 拦截器生命周期管理
- ✅ **注册管理**: 动态注册和注销拦截器
- ✅ **启用/禁用**: 支持拦截器的启用和禁用
- ✅ **状态监控**: 实时监控拦截器状态

## 🧪 测试验证

### 快速测试
```bash
# Windows
scripts\test-stage2.bat

# 手动测试
mvn clean compile
mvn test -Dtest=*Stage2*Test
```

### 功能验证
```bash
# 启动Arthas并连接到Java进程
java -jar arthas-boot.jar

# 在Arthas控制台中测试
[arthas@pid]$ trace-flow --help
[arthas@pid]$ trace-flow --list-probes  
[arthas@pid]$ trace-flow --show-config database
[arthas@pid]$ trace-flow -n 3 --verbose
```

## 📊 阶段2成果

### 已实现功能
- [x] 真实的JDBC方法拦截
- [x] 扩展的Source表达式解析
- [x] 基础的Formula表达式计算
- [x] 改进的输出格式
- [x] 拦截器管理框架
- [x] Arthas增强框架集成

### 支持的表达式类型
**Source表达式**:
- `startTime`, `endTime`, `executionTime`, `threadName` - 内置变量
- `this.toString()` - this方法调用
- `args[0]`, `args[0].getValue()` - 参数访问
- `returnValue`, `returnValue.getResultSet()` - 返回值访问

**Formula表达式**:
- `metrics.endTime - metrics.startTime` - 数学运算
- `metrics.xxx + metrics.yyy` - 加法运算
- `metrics.xxx * 1000` - 乘法运算

### 输出格式示例
```
[2025-07-14 14:30:15.123] [DATABASE]
  Method: java.sql.PreparedStatement.executeQuery
  Execution Time: 156ms
  Thread: http-nio-8080-exec-1
  SQL: SELECT * FROM users WHERE id = ?
  Parameters: [12345]
  Result: ResultSet
```

## 🎯 验收标准

### 功能验收
- [x] 真实拦截JDBC方法调用
- [x] 正确采集SQL执行时间
- [x] 支持复杂表达式解析
- [x] Formula计算正确工作
- [x] 实时输出跟踪结果
- [x] 拦截器正确注册和管理

### 技术验收
- [x] 集成Arthas字节码增强框架
- [x] 支持动态拦截器管理
- [x] 线程安全的执行上下文管理
- [x] 完善的异常处理机制

### 质量验收
- [x] 代码符合Arthas规范
- [x] 使用Arthas工具类和框架
- [x] 完善的错误处理
- [x] 清晰的日志输出

## 🔄 下一步：阶段3开发

### 阶段3目标
- 实现HTTP Server探针
- 实现链路跟踪和Trace ID管理  
- 实现多探针协同工作
- 实现树状输出格式

### 阶段3验收标准
```bash
# 阶段3测试命令
tf --filter "url.startsWith('/api/users')"
tf --filter "isSlowQuery == true || isSlowRequest == true"
tf --verbose
```

## 📞 问题排查

### 常见问题
1. **拦截器未生效**: 检查Instrumentation是否正确初始化
2. **表达式解析失败**: 检查表达式语法是否正确
3. **输出格式异常**: 检查ExecutionContext是否正确设置
4. **字节码增强失败**: 检查目标类是否可以被增强

### 调试方法
```bash
# 详细日志
tf --verbose

# 检查拦截器状态
tf --list-probes

# 查看配置详情
tf --show-config database
```

## 📝 开发日志

- [x] 2025-07-14: 创建InterceptorManager和TraceFlowAdviceListener
- [x] 2025-07-14: 扩展SourceExpressionParser支持复杂表达式
- [x] 2025-07-14: 改进ConfigurableMethodInterceptor输出格式
- [x] 2025-07-14: 集成Arthas字节码增强框架
- [x] 2025-07-14: 完成阶段2核心功能实现

**阶段2状态**: ✅ 完成，可进入阶段3开发
