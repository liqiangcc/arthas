package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;

import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.List;

/**
 * trace-flow命令 - 跟踪HTTP请求的完整执行链路
 * 
 * @author arthas
 */
@Name("trace-flow")
@Summary("跟踪HTTP请求的完整执行链路")
@Description(Constants.EXAMPLE +
        "  tf                                           # 跟踪下一个HTTP请求\n" +
        "  tf -n 5                                      # 跟踪5次请求\n" +
        "  tf --filter \"executionTime > 1000\"           # 只显示慢请求\n" +
        "  tf --filter \"url.startsWith('/api')\"        # 只跟踪API请求\n" +
        "  tf --output-file result.json                 # 保存结果到文件\n" +
        "  tf --verbose                                 # 详细模式输出\n" +
        Constants.WIKI + Constants.WIKI_HOME + "trace-flow")
public class TraceFlowCommand extends AnnotatedCommand {

    private Integer count = 1;
    private String filter;
    private String outputFile;
    private boolean verbose = false;
    private Long stackTraceThreshold;
    private boolean listProbes = false;
    private String showConfig;
    private boolean help = false;

    private ProbeManager probeManager;
    private TraceManager traceManager;
    private FilterEngine filterEngine;
    private OutputFormatter outputFormatter;

    public TraceFlowCommand() {
        this.probeManager = new ProbeManager();
        this.traceManager = new TraceManager();
        this.filterEngine = new FilterEngine();
        this.outputFormatter = new OutputFormatter();
    }

    @Override
    public void process(CommandProcess process) {
        try {
            // 显示帮助信息
            if (help) {
                process.write(outputFormatter.formatHelp());
                process.end();
                return;
            }

            // 列出所有探针
            if (listProbes) {
                listAvailableProbes(process);
                process.end();
                return;
            }

            // 显示探针配置
            if (showConfig != null) {
                showProbeConfig(process, showConfig);
                process.end();
                return;
            }

            // 验证参数
            if (count <= 0) {
                process.write("Error: Trace count must be greater than 0\n");
                process.end();
                return;
            }

            // 初始化探针
            initializeProbes(process);

            // 开始跟踪
            startTracing(process);

        } catch (Exception e) {
            process.write("Error executing trace-flow command: " + e.getMessage() + "\n");
            if (verbose) {
                e.printStackTrace();
            }
            process.end();
        }
    }

    private void listAvailableProbes(CommandProcess process) {
        process.write("Available Probes:\n");
        process.write("================\n");

        try {
            List<ProbeConfig> configs = probeManager.loadBuiltinProbes();
            for (ProbeConfig config : configs) {
                process.write(String.format("- %s: %s (Enabled: %s)\n",
                    config.getName(),
                    config.getDescription(),
                    config.isEnabled() ? "Yes" : "No"));
            }
            process.write("\nTotal: " + configs.size() + " probes\n");
        } catch (Exception e) {
            process.write("Failed to load probe configurations: " + e.getMessage() + "\n");
        }
    }

    private void showProbeConfig(CommandProcess process, String probeName) {
        process.write("Probe Configuration: " + probeName + "\n");
        process.write("========================\n");

        try {
            ProbeConfig config = probeManager.getProbeConfig(probeName);
            if (config == null) {
                process.write("Probe not found: " + probeName + "\n");
                process.write("Use --list-probes to see all available probes\n");
                return;
            }

            process.write("Name: " + config.getName() + "\n");
            process.write("Description: " + config.getDescription() + "\n");
            process.write("Status: " + (config.isEnabled() ? "Enabled" : "Disabled") + "\n");
            process.write("Metrics count: " + config.getMetrics().size() + "\n");

            if (verbose) {
                process.write("\nMetrics details:\n");
                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    process.write(String.format("  - %s (%s): %s\n",
                        metric.getName(),
                        metric.getType(),
                        metric.getDescription()));
                }
            }
        } catch (Exception e) {
            process.write("Failed to get probe configuration: " + e.getMessage() + "\n");
        }
    }

    private void initializeProbes(CommandProcess process) {
        if (verbose) {
            process.write("Initializing probes...\n");
        }

        try {
            probeManager.initializeProbes();
            if (verbose) {
                process.write("Probes initialized successfully\n");
            }
        } catch (Exception e) {
            process.write("Failed to initialize probes: " + e.getMessage() + "\n");
            throw new RuntimeException("Failed to initialize probes", e);
        }
    }

    private void startTracing(CommandProcess process) {
        process.write("Starting HTTP request tracing...\n");
        process.write("Trace count: " + count + "\n");

        if (filter != null) {
            process.write("Filter condition: " + filter + "\n");
        }

        if (outputFile != null) {
            process.write("Output file: " + outputFile + "\n");
        }

        process.write("Press Ctrl+C to stop tracing\n");
        process.write("========================\n");

        // TODO: 在阶段2实现实际的跟踪逻辑
        // 这里先输出模拟信息
        process.write("Waiting for HTTP requests...\n");

        // 模拟跟踪结果
        if (verbose) {
            process.write("\n[DEBUG] This is Stage 1 mock output\n");
            process.write("[DEBUG] Actual tracing will be implemented in Stage 2\n");
        }

        process.end();
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

    // Getters for testing
    public Integer getCount() { return count; }
    public String getFilter() { return filter; }
    public String getOutputFile() { return outputFile; }
    public boolean isVerbose() { return verbose; }
    public Long getStackTraceThreshold() { return stackTraceThreshold; }
    public boolean isListProbes() { return listProbes; }
    public String getShowConfig() { return showConfig; }
    public boolean isHelp() { return help; }
}
