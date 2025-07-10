# Arthas 命令快速扩展指南

## 🎯 5分钟扩展一个新命令

### 步骤概览
1. **创建命令类** → 2. **创建模型类** → 3. **创建视图类** → 4. **编写测试** → 5. **构建测试**

---

## 📋 Step 1: 创建命令类

**位置**: `core/src/main/java/com/taobao/arthas/core/command/basic1000/YourCommand.java`

```java
package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.YourModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Your command description
 */
@Name("yourcommand")  // ← 修改命令名
@Summary("Your command summary")  // ← 修改命令描述
@Description("\nExamples:\n" +
        "  yourcommand\n" +
        "  yourcommand -p param\n" +
        Constants.WIKI + Constants.WIKI_HOME + "yourcommand")
public class YourCommand extends AnnotatedCommand {
    
    // 定义命令参数
    private String param;
    private boolean flag = false;
    
    @Option(shortName = "p", longName = "param", argName = "param")
    @Description("Parameter description")
    public void setParam(String param) {
        this.param = param;
    }
    
    @Option(shortName = "f", longName = "flag", flag = true)
    @Description("Flag description")
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            // TODO: 实现你的业务逻辑
            String result = executeYourLogic();
            
            // 创建结果模型
            YourModel model = new YourModel(result, param, flag);
            
            // 返回结果
            process.appendResult(model);
            process.end();
            
        } catch (Exception e) {
            process.end(-1, "Error: " + e.getMessage());
        }
    }
    
    private String executeYourLogic() {
        // TODO: 在这里实现你的核心逻辑
        return "Your result here";
    }
}
```

---

## 📋 Step 2: 创建模型类

**位置**: `core/src/main/java/com/taobao/arthas/core/command/model/YourModel.java`

```java
package com.taobao.arthas.core.command.model;

/**
 * Result model for YourCommand
 */
public class YourModel extends ResultModel {
    
    private String result;
    private String param;
    private boolean flag;
    
    public YourModel() {
    }
    
    public YourModel(String result, String param, boolean flag) {
        this.result = result;
        this.param = param;
        this.flag = flag;
    }
    
    @Override
    public String getType() {
        return "yourcommand";  // ← 必须与命令名一致
    }
    
    // Getters and Setters
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public String getParam() { return param; }
    public void setParam(String param) { this.param = param; }
    
    public boolean isFlag() { return flag; }
    public void setFlag(boolean flag) { this.flag = flag; }
}
```

---

## 📋 Step 3: 创建视图类

**位置**: `core/src/main/java/com/taobao/arthas/core/command/view/YourView.java`

```java
package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.YourModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

/**
 * View for YourCommand result
 */
public class YourView extends ResultView<YourModel> {

    @Override
    public void draw(CommandProcess process, YourModel result) {
        if (result.isFlag()) {
            // 详细显示模式
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            
            table.row(label("Result").style(Decoration.bold.bold()),
                     label(result.getResult()));
            
            if (result.getParam() != null) {
                table.row(label("Parameter").style(Decoration.bold.bold()),
                         label(result.getParam()));
            }
            
            process.write(RenderUtil.render(table, process.width()));
        } else {
            // 简单显示模式
            LabelElement resultLabel = label(result.getResult())
                    .style(Decoration.bold.fg(Color.green));
            process.write(RenderUtil.render(resultLabel, process.width()));
        }
        process.write("\n");
    }
}
```

---

## 📋 Step 4: 编写测试

**位置**: `core/src/test/java/com/taobao/arthas/core/command/basic1000/YourCommandTest.java`

```java
package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.YourModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CLIConfigurator;
import com.taobao.middleware.cli.CommandLine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class YourCommandTest {
    
    private CLI cli;
    
    @Before
    public void setUp() {
        cli = CLIConfigurator.define(YourCommand.class);
    }
    
    @Test
    public void testBasicCommand() {
        List<String> args = Arrays.asList();
        YourCommand command = new YourCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, command);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        CommandProcess process = Mockito.mock(CommandProcess.class);
        command.process(process);
        
        Mockito.verify(process).appendResult(Mockito.any(YourModel.class));
        Mockito.verify(process).end();
    }
    
    @Test
    public void testCommandWithParam() {
        List<String> args = Arrays.asList("-p", "testvalue");
        YourCommand command = new YourCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, command);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        CommandProcess process = Mockito.mock(CommandProcess.class);
        command.process(process);
        
        Mockito.verify(process).appendResult(Mockito.any(YourModel.class));
        Mockito.verify(process).end();
    }
}
```

---

## 📋 Step 5: 构建和测试

```batch
# 增量构建（推荐）
scripts\build-change.bat

# 运行测试
scripts\run.bat

# 如果有问题，使用完整构建
scripts\build.bat
```

---

## 🔧 常用命令模式

### 1. 简单信息显示命令
```java
// 适用于: version, status, info 等
@Override
public void process(CommandProcess process) {
    String info = getSystemInfo();
    YourModel model = new YourModel(info);
    process.appendResult(model);
    process.end();
}
```

### 2. 带参数的查询命令
```java
// 适用于: search, find, query 等
@Override
public void process(CommandProcess process) {
    if (param == null) {
        process.end(-1, "Parameter required");
        return;
    }
    
    List<String> results = searchByParam(param);
    YourModel model = new YourModel(results);
    process.appendResult(model);
    process.end();
}
```

### 3. 系统操作命令
```java
// 适用于: clear, reset, reload 等
@Override
public void process(CommandProcess process) {
    try {
        performOperation();
        YourModel model = new YourModel("Operation completed successfully");
        process.appendResult(model);
        process.end();
    } catch (Exception e) {
        process.end(-1, "Operation failed: " + e.getMessage());
    }
}
```

---

## 🚀 快速检查清单

- [ ] 命令类: `@Name` 注解设置正确
- [ ] 模型类: `getType()` 返回值与命令名一致  
- [ ] 视图类: 继承 `ResultView<YourModel>`
- [ ] 测试类: 基本功能测试覆盖
- [ ] 构建测试: `build-change.bat` 成功
- [ ] 运行测试: `run.bat` 中命令可用

---

## 💡 扩展技巧

### 参数类型
```java
// 字符串参数
@Option(shortName = "s", longName = "string", argName = "value")
public void setString(String value) { this.string = value; }

// 数字参数  
@Option(shortName = "n", longName = "number", argName = "num")
public void setNumber(int number) { this.number = number; }

// 布尔标志
@Option(shortName = "f", longName = "flag", flag = true)
public void setFlag(boolean flag) { this.flag = flag; }

// 列表参数
@Option(shortName = "l", longName = "list", argName = "items")
public void setList(List<String> list) { this.list = list; }
```

### 视图样式
```java
// 颜色
label("text").style(Decoration.bold.fg(Color.green))
label("error").style(Decoration.bold.fg(Color.red))

// 表格
TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
table.row(label("Key"), label("Value"));

// 条件显示
if (condition) {
    // 显示详细信息
} else {
    // 显示简单信息
}
```

---

## 🎯 总结

按照这个模板，你可以在 **5-10分钟** 内快速扩展一个新的 Arthas 命令：

1. **复制模板代码** → 修改类名和命令名
2. **实现业务逻辑** → 在 `executeYourLogic()` 中添加功能  
3. **调整显示效果** → 修改视图渲染逻辑
4. **增量构建测试** → 使用 `build-change.bat` 快速验证

这个模板涵盖了 90% 的常见命令扩展场景，让你专注于业务逻辑而不是框架细节！
