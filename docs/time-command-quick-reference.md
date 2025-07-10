# Arthas Time 命令扩展 - 快速参考

## 🚀 快速开始

### 开发工作流程
```batch
# 1. 首次构建
scripts\build.bat

# 2. 修改代码后增量构建
scripts\build-change.bat

# 3. 测试运行
scripts\run.bat
```

## 📁 核心文件

| 文件 | 作用 | 位置 |
|------|------|------|
| `TimeCommand.java` | 命令实现 | `core/src/main/java/.../basic1000/` |
| `TimeModel.java` | 数据模型 | `core/src/main/java/.../model/` |
| `TimeView.java` | 视图渲染 | `core/src/main/java/.../view/` |
| `TimeCommandTest.java` | 单元测试 | `core/src/test/java/.../basic1000/` |

## 🔧 命令结构模板

### 1. 命令类模板
```java
@Name("your-command")
@Summary("Command description")
@Description("Usage examples")
public class YourCommand extends AnnotatedCommand {
    
    @Option(shortName = "f", longName = "flag")
    @Description("Option description")
    public void setFlag(String value) {
        this.flag = value;
    }
    
    @Override
    public void process(CommandProcess process) {
        try {
            // 业务逻辑
            YourModel model = new YourModel(data);
            process.appendResult(model);
            process.end();
        } catch (Exception e) {
            process.end(-1, "Error: " + e.getMessage());
        }
    }
}
```

### 2. 模型类模板
```java
public class YourModel extends ResultModel {
    private String data;
    
    public YourModel(String data) {
        this.data = data;
    }
    
    @Override
    public String getType() {
        return "your-type";  // 必须与视图匹配
    }
    
    // getter/setter...
}
```

### 3. 视图类模板
```java
public class YourView extends ResultView<YourModel> {
    @Override
    public void draw(CommandProcess process, YourModel result) {
        // 渲染逻辑
        process.write(result.getData());
        process.write("\n");
    }
}
```

## 📋 Time 命令功能

### 支持的参数
- `-f, --format <pattern>`: 自定义时间格式
- `-z, --timezone`: 显示时区信息

### 使用示例
```bash
# 基本用法
time

# 自定义格式
time -f "yyyy-MM-dd HH:mm:ss"

# 显示时区
time -z

# 组合使用
time -f "yyyy年MM月dd日" -z
```

## ⚡ 增量构建说明

### build-change.bat 工作原理
1. 使用 `git diff` 检测变更文件
2. 映射文件到对应的 Maven 模块
3. 只构建变更的模块及其依赖
4. 自动更新 packaging 模块

### 支持的模块检测
- `common/` → common 模块
- `core/` → core 模块  
- `agent/` → agent 模块
- `client/` → client 模块
- `boot/` → boot 模块
- 其他核心模块...

## 🧪 测试指南

### 单元测试要点
```java
@Test
public void testCommand() {
    // 1. 准备参数
    List<String> args = Arrays.asList("-f", "yyyy-MM-dd");
    
    // 2. 创建命令实例
    TimeCommand command = new TimeCommand();
    CommandLine commandLine = cli.parse(args, true);
    CLIConfigurator.inject(commandLine, command);
    
    // 3. Mock CommandProcess
    CommandProcess process = Mockito.mock(CommandProcess.class);
    
    // 4. 执行命令
    command.process(process);
    
    // 5. 验证结果
    Mockito.verify(process).appendResult(Mockito.any(TimeModel.class));
    Mockito.verify(process).end();
}
```

## 🔍 调试技巧

### 1. 构建问题
```batch
# 如果增量构建失败，使用完整构建
scripts\build.bat

# 检查 Git 状态
git status

# 查看变更文件
git diff --name-only
```

### 2. 运行时问题
- 检查 `@Name` 注解是否正确
- 验证 `getType()` 返回值与视图类匹配
- 确认参数注解配置正确

### 3. 测试问题
- 使用 Maven 运行测试：`mvn test -Dtest=TimeCommandTest`
- 检查 Mock 对象配置
- 验证测试数据准备

## 📈 性能优化

### 构建性能
- ✅ 使用 `build-change.bat` 进行增量构建
- ✅ 提交代码后再构建，获得准确的变更检测
- ✅ 避免不必要的全量构建

### 运行时性能
- ✅ 缓存重复计算的数据
- ✅ 使用高效的字符串格式化
- ✅ 避免在循环中创建重对象

## 🚨 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| 命令不识别 | `@Name` 注解错误 | 检查注解配置 |
| 参数解析失败 | `@Option` 配置错误 | 验证参数定义 |
| 视图不显示 | `getType()` 不匹配 | 确保类型一致 |
| 构建失败 | 依赖问题 | 使用完整构建 |

## 📚 扩展资源

- **完整文档**: `docs/time-command-extension.md`
- **构建脚本说明**: `scripts/README.md`
- **Arthas 官方文档**: https://arthas.aliyun.com/
- **Maven 构建指南**: 项目根目录 `pom.xml`

## 💡 最佳实践

1. **开发流程**: 首次用 `build.bat`，后续用 `build-change.bat`
2. **代码规范**: 遵循现有代码风格和命名约定
3. **测试覆盖**: 为每个功能编写对应测试
4. **文档更新**: 及时更新使用说明和示例
5. **错误处理**: 提供友好的错误信息
6. **性能考虑**: 避免不必要的计算和对象创建

---

**提示**: 这个快速参考涵盖了 Time 命令扩展的核心要点。如需详细信息，请参考完整文档。
