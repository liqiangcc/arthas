// ============================================================================
// Arthas 命令扩展模板 - 复制此模板快速创建新命令
// 使用方法: 
// 1. 复制此文件内容
// 2. 全局替换 "YourCommand" 为你的命令名 (如: StatusCommand)
// 3. 全局替换 "yourcommand" 为你的命令名 (如: status)  
// 4. 实现 executeYourLogic() 方法
// 5. 运行 scripts\build-change.bat 构建测试
// ============================================================================

// ============================================================================
// 1. 命令类 - 放在 core/src/main/java/com/taobao/arthas/core/command/basic1000/
// ============================================================================
package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.YourCommandModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * YourCommand - 命令描述
 */
@Name("yourcommand")
@Summary("命令简短描述")
@Description("\nExamples:\n" +
        "  yourcommand\n" +
        "  yourcommand -p value\n" +
        "  yourcommand -f\n" +
        Constants.WIKI + Constants.WIKI_HOME + "yourcommand")
public class YourCommand extends AnnotatedCommand {
    
    private String param;
    private boolean flag = false;
    
    @Option(shortName = "p", longName = "param", argName = "value")
    @Description("参数描述")
    public void setParam(String param) {
        this.param = param;
    }
    
    @Option(shortName = "f", longName = "flag", flag = true)
    @Description("标志描述")
    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            // 执行业务逻辑
            String result = executeYourLogic();
            
            // 创建结果模型
            YourCommandModel model = new YourCommandModel(result, param, flag);
            
            // 返回结果
            process.appendResult(model);
            process.end();
            
        } catch (Exception e) {
            process.end(-1, "Error: " + e.getMessage());
        }
    }
    
    /**
     * TODO: 在这里实现你的核心业务逻辑
     */
    private String executeYourLogic() {
        // 示例逻辑 - 替换为你的实际实现
        if (param != null) {
            return "处理参数: " + param;
        } else {
            return "默认结果";
        }
    }
}

// ============================================================================
// 2. 模型类 - 放在 core/src/main/java/com/taobao/arthas/core/command/model/
// ============================================================================
package com.taobao.arthas.core.command.model;

/**
 * YourCommand 结果模型
 */
public class YourCommandModel extends ResultModel {
    
    private String result;
    private String param;
    private boolean flag;
    
    public YourCommandModel() {
    }
    
    public YourCommandModel(String result, String param, boolean flag) {
        this.result = result;
        this.param = param;
        this.flag = flag;
    }
    
    @Override
    public String getType() {
        return "yourcommand";  // 必须与命令名一致
    }
    
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    
    public String getParam() { return param; }
    public void setParam(String param) { this.param = param; }
    
    public boolean isFlag() { return flag; }
    public void setFlag(boolean flag) { this.flag = flag; }
}

// ============================================================================
// 3. 视图类 - 放在 core/src/main/java/com/taobao/arthas/core/command/view/
// ============================================================================
package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.YourCommandModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

/**
 * YourCommand 视图渲染
 */
public class YourCommandView extends ResultView<YourCommandModel> {

    @Override
    public void draw(CommandProcess process, YourCommandModel result) {
        if (result.isFlag()) {
            // 详细显示模式 - 表格形式
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            
            table.row(label("结果").style(Decoration.bold.bold()),
                     label(result.getResult()));
            
            if (result.getParam() != null) {
                table.row(label("参数").style(Decoration.bold.bold()),
                         label(result.getParam()));
            }
            
            process.write(RenderUtil.render(table, process.width()));
        } else {
            // 简单显示模式 - 单行显示
            LabelElement resultLabel = label(result.getResult())
                    .style(Decoration.bold.fg(Color.green));
            process.write(RenderUtil.render(resultLabel, process.width()));
        }
        process.write("\n");
    }
}

// ============================================================================
// 4. 测试类 - 放在 core/src/test/java/com/taobao/arthas/core/command/basic1000/
// ============================================================================
package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.YourCommandModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.CLIConfigurator;
import com.taobao.middleware.cli.CommandLine;
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
        
        Mockito.verify(process).appendResult(Mockito.any(YourCommandModel.class));
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
        
        Mockito.verify(process).appendResult(Mockito.any(YourCommandModel.class));
        Mockito.verify(process).end();
    }
    
    @Test
    public void testCommandWithFlag() {
        List<String> args = Arrays.asList("-f");
        YourCommand command = new YourCommand();
        CommandLine commandLine = cli.parse(args, true);
        
        try {
            CLIConfigurator.inject(commandLine, command);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        
        CommandProcess process = Mockito.mock(CommandProcess.class);
        command.process(process);
        
        Mockito.verify(process).appendResult(Mockito.any(YourCommandModel.class));
        Mockito.verify(process).end();
    }
}

// ============================================================================
// 快速使用步骤:
// 1. 复制上述代码到对应的文件位置
// 2. 全局替换 "YourCommand" → "你的命令名" (如: StatusCommand)
// 3. 全局替换 "yourcommand" → "你的命令名" (如: status)
// 4. 实现 executeYourLogic() 方法中的业务逻辑
// 5. 运行 scripts\build-change.bat 进行增量构建
// 6. 运行 scripts\run.bat 测试你的新命令
// ============================================================================
