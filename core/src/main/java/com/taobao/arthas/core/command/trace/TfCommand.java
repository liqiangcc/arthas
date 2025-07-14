package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.List;

/**
 * tf命令 - trace-flow的别名
 * 
 * @author arthas
 */
@Name("tf")
@Summary("Alias for trace-flow command - trace HTTP request execution flow")
@Description(Constants.EXAMPLE +
        "  tf                                           # Trace next HTTP request\n" +
        "  tf -n 5                                      # Trace 5 requests\n" +
        "  tf --filter \"executionTime > 1000\"           # Show only slow requests\n" +
        "  tf --filter \"url.startsWith('/api')\"        # Trace only API requests\n" +
        "  tf --output-file result.json                 # Save results to file\n" +
        "  tf --verbose                                 # Verbose mode\n" +
        "  tf --list-probes                             # List all probes\n" +
        "  tf --show-config database                    # Show probe config\n" +
        Constants.WIKI + Constants.WIKI_HOME + "trace-flow")
public class TfCommand extends AnnotatedCommand {

    private Integer count = 1;
    private String filter;
    private String outputFile;
    private boolean verbose = false;
    private Long stackTraceThreshold;
    private boolean listProbes = false;
    private String showConfig;
    private boolean help = false;

    private TraceFlowCommand traceFlowCommand;

    public TfCommand() {
        this.traceFlowCommand = new TraceFlowCommand();
    }

    @Override
    public void process(CommandProcess process) {
        // 将所有参数传递给TraceFlowCommand
        traceFlowCommand.setCount(count);
        traceFlowCommand.setFilter(filter);
        traceFlowCommand.setOutputFile(outputFile);
        traceFlowCommand.setVerbose(verbose);
        traceFlowCommand.setStackTraceThreshold(stackTraceThreshold);
        traceFlowCommand.setListProbes(listProbes);
        traceFlowCommand.setShowConfig(showConfig);
        traceFlowCommand.setHelp(help);

        // 委托给TraceFlowCommand处理
        traceFlowCommand.process(process);
    }

    // Setter methods with annotations (Arthas style)
    @Option(shortName = "n", longName = "count")
    @Description("Number of traces, default is 1")
    public void setCount(Integer count) {
        this.count = count;
    }

    @Option(longName = "filter")
    @Description("Filter expression based on metrics")
    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Option(longName = "output-file")
    @Description("Output file path, supports JSON format")
    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    @Option(longName = "verbose", flag = true)
    @Description("Verbose mode, show more debug information")
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Option(longName = "stack-trace-threshold")
    @Description("Stack trace threshold (ms), operations exceeding this time will show stack trace")
    public void setStackTraceThreshold(Long stackTraceThreshold) {
        this.stackTraceThreshold = stackTraceThreshold;
    }

    @Option(longName = "list-probes", flag = true)
    @Description("List all available probes")
    public void setListProbes(boolean listProbes) {
        this.listProbes = listProbes;
    }

    @Option(longName = "show-config")
    @Description("Show configuration of specified probe")
    public void setShowConfig(String showConfig) {
        this.showConfig = showConfig;
    }

    @Option(shortName = "h", longName = "help", flag = true)
    @Description("Show help information")
    public void setHelp(boolean help) {
        this.help = help;
    }
}
