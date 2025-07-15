# 🚀 阶段2开发计划 - Database探针实现

## 🎯 **阶段2目标**

实现完整的Database探针功能，从模拟输出转向真实的JDBC方法拦截和数据采集。

## 📋 **核心任务分解**

### **任务1: 方法拦截框架 (优先级: 高)**

#### **1.1 创建方法拦截器基础设施**
- `MethodInterceptor.java` - 方法拦截器接口
- `DatabaseMethodInterceptor.java` - Database专用拦截器
- `InterceptorManager.java` - 拦截器管理器

#### **1.2 集成Arthas的字节码增强**
- 使用Arthas现有的ASM字节码增强能力
- 集成到现有的`EnhancerAffect`框架
- 支持动态增强和卸载

#### **1.3 目标方法识别**
- 解析JSON配置中的`targets`定义
- 支持类名和方法名匹配
- 支持方法签名匹配

### **任务2: 完整表达式解析器 (优先级: 高)**

#### **2.1 扩展SourceExpressionParser**
- 支持 `this.toString()` 类型表达式
- 支持 `args[0].getValue()` 参数访问
- 支持 `returnValue.getResultSet()` 返回值访问

#### **2.2 实现Formula表达式解析器**
- `FormulaExpressionParser.java` - Formula表达式解析器
- 集成JavaScript引擎（GraalVM JS）
- 支持 `metrics.endTime - metrics.startTime` 计算

#### **2.3 表达式上下文管理**
- 扩展`ExecutionContext`支持复杂对象
- 实现表达式变量绑定
- 支持嵌套对象访问

### **任务3: 指标采集引擎 (优先级: 中)**

#### **3.1 创建指标采集器**
- `MetricCollector.java` - 指标采集器接口
- `DatabaseMetricCollector.java` - Database指标采集器
- 支持before/after/exception采集点

#### **3.2 指标计算引擎**
- `MetricCalculator.java` - 指标计算器
- 支持实时计算和延迟计算
- 支持指标依赖关系处理

#### **3.3 指标存储和管理**
- `MetricStorage.java` - 指标存储
- 支持临时存储和持久化
- 支持指标查询和过滤

### **任务4: Database探针实现 (优先级: 高)**

#### **4.1 JDBC方法拦截**
- 拦截`PreparedStatement.execute*`方法
- 拦截`Statement.execute*`方法
- 拦截`Connection.prepareStatement`方法

#### **4.2 SQL信息提取**
- 提取SQL语句内容
- 识别SQL操作类型（SELECT/INSERT/UPDATE/DELETE）
- 提取参数信息

#### **4.3 性能指标采集**
- 执行时间测量
- 线程信息采集
- 异常信息捕获

### **任务5: 输出和过滤增强 (优先级: 中)**

#### **5.1 实时输出**
- 实现真实的跟踪输出
- 支持实时指标显示
- 支持进度显示

#### **5.2 过滤引擎增强**
- 支持复杂过滤表达式
- 支持多条件组合
- 支持动态过滤条件

#### **5.3 输出格式优化**
- 优化控制台输出格式
- 支持表格化显示
- 支持颜色高亮

## 🏗️ **技术架构设计**

### **核心组件关系**

```
TraceFlowCommand
    ↓
ProbeManager (加载JSON配置)
    ↓
InterceptorManager (管理方法拦截器)
    ↓
DatabaseMethodInterceptor (拦截JDBC方法)
    ↓
MetricCollector (采集指标数据)
    ↓
SourceExpressionParser + FormulaExpressionParser (解析表达式)
    ↓
MetricCalculator (计算指标值)
    ↓
FilterEngine (过滤结果)
    ↓
OutputFormatter (格式化输出)
```

### **数据流设计**

```
JDBC方法调用
    ↓ (拦截)
ExecutionContext (创建执行上下文)
    ↓ (before)
采集startTime, threadName, SQL等
    ↓ (执行原方法)
方法执行
    ↓ (after/exception)
采集endTime, returnValue, exception等
    ↓ (计算)
Formula表达式计算 (executionTime = endTime - startTime)
    ↓ (过滤)
FilterEngine应用过滤条件
    ↓ (输出)
实时显示跟踪结果
```

## 📅 **开发时间表**

### **第1周：方法拦截框架**
- **Day 1-2**: 创建拦截器基础设施
- **Day 3-4**: 集成Arthas字节码增强
- **Day 5**: 测试基础拦截功能

### **第2周：表达式解析器**
- **Day 1-2**: 扩展SourceExpressionParser
- **Day 3-4**: 实现FormulaExpressionParser
- **Day 5**: 集成测试表达式解析

### **第3周：Database探针实现**
- **Day 1-2**: JDBC方法拦截实现
- **Day 3-4**: 指标采集和计算
- **Day 5**: Database探针端到端测试

### **第4周：集成和优化**
- **Day 1-2**: 输出格式优化
- **Day 3-4**: 性能优化和错误处理
- **Day 5**: 完整的阶段2验收测试

## 🧪 **阶段2验收标准**

### **功能验收**
- [ ] 真实拦截JDBC方法调用
- [ ] 正确采集SQL执行时间
- [ ] 支持复杂表达式解析
- [ ] Formula计算正确工作
- [ ] 过滤条件正确应用
- [ ] 实时输出跟踪结果

### **性能验收**
- [ ] 拦截开销 < 5%
- [ ] 内存使用合理
- [ ] 无内存泄漏
- [ ] 支持高并发场景

### **质量验收**
- [ ] 单元测试覆盖率 >= 80%
- [ ] 集成测试通过
- [ ] 错误处理完善
- [ ] 文档完整

## 🎯 **阶段2成功标志**

当您能看到以下真实的跟踪输出时，说明阶段2成功：

```bash
[arthas@pid]$ tf -n 3 --filter "executionTime > 100"
Starting HTTP request tracing...
Trace count: 3
Filter condition: executionTime > 100
========================

[2025-07-14 14:30:15.123] [DATABASE] 
  SQL: SELECT * FROM users WHERE id = ?
  Execution Time: 156ms
  Thread: http-nio-8080-exec-1
  Parameters: [12345]
  Result: 1 row(s)

[2025-07-14 14:30:15.456] [DATABASE]
  SQL: UPDATE user_stats SET last_login = ? WHERE user_id = ?
  Execution Time: 234ms
  Thread: http-nio-8080-exec-1
  Parameters: [2025-07-14 14:30:15.456, 12345]
  Result: 1 row(s) affected

Trace completed. Total: 2 operations captured (1 filtered out)
```

## 🚀 **立即开始**

### **第一步：创建方法拦截框架**

让我们从最核心的方法拦截框架开始。这是阶段2的基础，所有其他功能都依赖于此。

准备好开始实现了吗？我们将从创建`MethodInterceptor`接口和`DatabaseMethodInterceptor`实现开始！

---

**当前状态**: 🎯 **阶段2开发计划完成**
**下一步**: 🏗️ **开始实现方法拦截框架**
