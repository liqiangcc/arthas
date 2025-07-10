# Arthas Time 命令扩展开发文档

## 概述

本文档详细介绍了如何为 Arthas 扩展 `time` 命令的完整过程，包括命令实现、模型定义、视图渲染、测试编写以及构建部署的完整工作流程。

## 项目结构

```
arthas/
├── core/src/main/java/com/taobao/arthas/core/
│   ├── command/
│   │   ├── basic1000/
│   │   │   └── TimeCommand.java          # 命令实现
│   │   ├── model/
│   │   │   └── TimeModel.java            # 数据模型
│   │   └── view/
│   │       └── TimeView.java             # 视图渲染
├── core/src/test/java/com/taobao/arthas/core/command/basic1000/
│   ├── TimeCommandTest.java              # 单元测试
│   └── TimeCommandIntegrationTest.java   # 集成测试
└── scripts/
    ├── build.bat                         # 完整构建脚本
    ├── build-change.bat                  # 增量构建脚本
    ├── run.bat                           # 运行测试脚本
    └── README.md                         # 构建脚本说明
```

## 1. 命令实现 (TimeCommand.java)

### 核心特性
- **命令名称**: `time`
- **功能**: 显示当前时间和时区信息
- **支持参数**:
  - `-f, --format`: 自定义时间格式
  - `-z, --timezone`: 显示时区信息

### 关键代码结构

```java
@Name("time")
@Summary("Display current time and timezone information")
@Description("Examples:\n  time\n  time -f yyyy-MM-dd\n  time -z")
public class TimeCommand extends AnnotatedCommand {
    
    private String format;
    private boolean showTimezone = false;
    
    @Option(shortName = "f", longName = "format", argName = "format")
    @Description("Time format pattern (e.g., yyyy-MM-dd HH:mm:ss)")
    public void setFormat(String format) {
        this.format = format;
    }
    
    @Option(shortName = "z", longName = "timezone", flag = true)
    @Description("Show timezone information")
    public void setShowTimezone(boolean showTimezone) {
        this.showTimezone = showTimezone;
    }
    
    @Override
    public void process(CommandProcess process) {
        // 实现逻辑
    }
}
```

### 实现要点
1. **继承 AnnotatedCommand**: 使用注解驱动的命令框架
2. **参数处理**: 使用 `@Option` 注解定义命令行参数
3. **时间格式化**: 支持自定义 DateTimeFormatter 模式
4. **错误处理**: 捕获异常并返回友好错误信息

## 2. 数据模型 (TimeModel.java)

### 模型设计
```java
public class TimeModel extends ResultModel {
    private String currentTime;      // 格式化后的时间字符串
    private String timezoneId;       // 时区ID (如: Asia/Shanghai)
    private String timezoneName;     // 时区显示名称
    private boolean showTimezone;    // 是否显示时区信息
    
    @Override
    public String getType() {
        return "time";  // 用于视图路由
    }
}
```

### 设计原则
- **继承 ResultModel**: 遵循 Arthas 结果模型规范
- **类型标识**: `getType()` 返回 "time" 用于视图匹配
- **数据封装**: 包含所有渲染所需的数据

## 3. 视图渲染 (TimeView.java)

### 渲染逻辑
```java
public class TimeView extends ResultView<TimeModel> {
    @Override
    public void draw(CommandProcess process, TimeModel result) {
        if (result.isShowTimezone()) {
            // 表格形式显示详细信息
            TableElement table = new TableElement()
                .leftCellPadding(1).rightCellPadding(1);
            
            table.row(label("Current Time").style(Decoration.bold.bold()),
                     label(result.getCurrentTime()));
            // ... 更多行
            
            process.write(RenderUtil.render(table, process.width()));
        } else {
            // 简单格式显示时间
            LabelElement timeLabel = label(result.getCurrentTime())
                .style(Decoration.bold.fg(Color.green));
            process.write(RenderUtil.render(timeLabel, process.width()));
        }
    }
}
```

### 视图特性
- **条件渲染**: 根据 `showTimezone` 参数选择不同显示方式
- **样式支持**: 使用颜色和装饰增强显示效果
- **响应式**: 根据终端宽度自适应渲染

## 4. 测试实现

### 单元测试 (TimeCommandTest.java)
```java
public class TimeCommandTest {
    @Test
    public void testTimeCommandBasic() {
        // 测试基本功能
    }
    
    @Test
    public void testTimeCommandWithFormat() {
        // 测试自定义格式
    }
    
    @Test
    public void testTimeCommandWithTimezone() {
        // 测试时区显示
    }
}
```

### 集成测试 (TimeCommandIntegrationTest.java)
```java
public class TimeCommandIntegrationTest {
    @Test
    public void testFullCommandExecution() {
        // 端到端测试
    }
}
```

## 5. 开发工作流程

### 5.1 初始构建
```batch
# 第一次完整构建
scripts\build.bat
```

### 5.2 增量开发
```batch
# 修改代码后增量构建
scripts\build-change.bat

# 运行测试
scripts\run.bat
```

### 5.3 构建脚本说明

#### build.bat - 完整构建
- 构建所有核心模块
- 适用于首次构建或重大变更
- 时间较长但确保完整性

#### build-change.bat - 增量构建 ⚡
- 使用 Git 检测变更文件
- 只构建包含变更的模块
- 自动处理模块依赖关系
- 显著提升开发效率

#### run.bat - 快速测试
- 启动 arthas-boot.jar
- 用于测试命令扩展效果

## 6. 命令使用示例

### 基本用法
```bash
# 显示当前时间
[arthas@12345]$ time
2023-12-07T15:30:45.123

# 自定义格式
[arthas@12345]$ time -f "yyyy-MM-dd HH:mm:ss"
2023-12-07 15:30:45

# 显示时区信息
[arthas@12345]$ time -z
Current Time    2023-12-07T15:30:45.123
Timezone ID     Asia/Shanghai
Timezone Name   China Standard Time

# 组合使用
[arthas@12345]$ time -f "yyyy年MM月dd日 HH:mm:ss" -z
Current Time    2023年12月07日 15:30:45
Timezone ID     Asia/Shanghai
Timezone Name   China Standard Time
```

## 7. 扩展要点总结

### 7.1 Arthas 命令扩展模式
1. **命令类**: 继承 `AnnotatedCommand`，使用注解定义
2. **模型类**: 继承 `ResultModel`，封装结果数据
3. **视图类**: 继承 `ResultView<T>`，负责渲染输出
4. **测试类**: 编写单元测试和集成测试

### 7.2 关键注解
- `@Name`: 定义命令名称
- `@Summary`: 命令简短描述
- `@Description`: 详细描述和使用示例
- `@Option`: 定义命令行选项

### 7.3 最佳实践
- **错误处理**: 使用 try-catch 捕获异常
- **参数验证**: 验证用户输入的合法性
- **文档完善**: 提供清晰的使用示例
- **测试覆盖**: 编写全面的测试用例

## 8. 故障排除

### 8.1 常见问题
- **命令不识别**: 检查 `@Name` 注解是否正确
- **参数解析失败**: 验证 `@Option` 注解配置
- **视图不显示**: 确认 `TimeModel.getType()` 返回值与视图匹配

### 8.2 调试技巧
- 使用单元测试验证逻辑
- 检查 Maven 构建日志
- 使用 `build-change.bat` 快速迭代

## 9. 性能优化

### 9.1 构建优化
- 使用增量构建减少编译时间
- Git 变更检测精确定位需要重建的模块

### 9.2 运行时优化
- 缓存时区信息避免重复计算
- 使用高效的时间格式化方法

## 结论

通过本文档的指导，你可以：
1. 理解 Arthas 命令扩展的完整架构
2. 掌握从开发到测试的完整工作流程
3. 使用高效的增量构建工具提升开发效率
4. 编写符合 Arthas 规范的高质量命令扩展

这套工作流程特别适合频繁的开发迭代，通过 `build-change.bat` 可以显著减少构建时间，提升开发体验。
