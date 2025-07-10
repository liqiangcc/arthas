# Arthas 命令扩展 - 一页纸快速开始

## 🚀 3分钟扩展新命令

### 1️⃣ 复制模板 
```bash
# 复制 templates/command-template.java 中的代码
# 全局替换: YourCommand → StatusCommand, yourcommand → status
```

### 2️⃣ 创建4个文件
```
core/src/main/java/.../basic1000/StatusCommand.java     # 命令实现
core/src/main/java/.../model/StatusModel.java          # 数据模型  
core/src/main/java/.../view/StatusView.java            # 视图渲染
core/src/test/java/.../basic1000/StatusCommandTest.java # 测试
```

### 3️⃣ 实现业务逻辑
```java
private String executeYourLogic() {
    // 在这里写你的核心逻辑
    return "你的结果";
}
```

### 4️⃣ 构建测试
```batch
scripts\build-change.bat  # 增量构建
scripts\run.bat          # 测试运行
```

---

## 📋 核心模板结构

### 命令类 (必须)
```java
@Name("status")
@Summary("显示状态信息")
public class StatusCommand extends AnnotatedCommand {
    @Option(shortName = "f", longName = "format")
    public void setFormat(String format) { this.format = format; }
    
    @Override
    public void process(CommandProcess process) {
        String result = executeYourLogic();
        StatusModel model = new StatusModel(result);
        process.appendResult(model);
        process.end();
    }
}
```

### 模型类 (必须)
```java
public class StatusModel extends ResultModel {
    private String result;
    
    @Override
    public String getType() { return "status"; }  // 与命令名一致
    
    // getter/setter...
}
```

### 视图类 (必须)
```java
public class StatusView extends ResultView<StatusModel> {
    @Override
    public void draw(CommandProcess process, StatusModel result) {
        process.write(result.getResult());
        process.write("\n");
    }
}
```

---

## 🔧 常用参数类型

```java
// 字符串参数
@Option(shortName = "f", longName = "format", argName = "pattern")
public void setFormat(String format) { }

// 数字参数
@Option(shortName = "n", longName = "number", argName = "num") 
public void setNumber(int number) { }

// 布尔标志
@Option(shortName = "v", longName = "verbose", flag = true)
public void setVerbose(boolean verbose) { }
```

---

## 🎨 常用视图样式

```java
// 简单文本
process.write("结果文本\n");

// 彩色文本
LabelElement label = label("成功").style(Decoration.bold.fg(Color.green));
process.write(RenderUtil.render(label, process.width()));

// 表格显示
TableElement table = new TableElement().leftCellPadding(1);
table.row(label("键"), label("值"));
process.write(RenderUtil.render(table, process.width()));
```

---

## ✅ 检查清单

- [ ] 命令类: `@Name("命令名")` 设置正确
- [ ] 模型类: `getType()` 返回命令名
- [ ] 视图类: 继承 `ResultView<你的Model>`  
- [ ] 测试类: 基本测试用例
- [ ] 构建成功: `build-change.bat` 无错误
- [ ] 运行测试: `run.bat` 中可以使用新命令

---

## 🚨 常见问题

| 问题 | 解决方案 |
|------|----------|
| 命令不识别 | 检查 `@Name` 注解 |
| 视图不显示 | 确保 `getType()` 与命令名一致 |
| 构建失败 | 使用 `build.bat` 完整构建 |
| 参数解析错误 | 检查 `@Option` 注解配置 |

---

## 💡 实用示例

### 系统信息命令
```java
private String executeYourLogic() {
    return "Java版本: " + System.getProperty("java.version") + 
           "\n操作系统: " + System.getProperty("os.name");
}
```

### 环境变量命令  
```java
private String executeYourLogic() {
    if (param != null) {
        return System.getenv(param);
    }
    return System.getenv().toString();
}
```

### 内存信息命令
```java
private String executeYourLogic() {
    Runtime runtime = Runtime.getRuntime();
    long total = runtime.totalMemory();
    long free = runtime.freeMemory();
    return String.format("总内存: %d MB, 可用: %d MB", 
                        total/1024/1024, free/1024/1024);
}
```

---

## 🎯 开发流程

```
复制模板 → 修改类名 → 实现逻辑 → 增量构建 → 测试运行
   ↓
templates/command-template.java 包含完整模板代码
```

**记住**: 专注于 `executeYourLogic()` 方法，其他都是模板代码！
