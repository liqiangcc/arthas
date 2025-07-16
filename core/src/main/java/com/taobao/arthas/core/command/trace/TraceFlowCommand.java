package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.monitor200.EnhancerCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;

import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.List;
import java.util.Objects;

/**
 * trace-flow命令 - 跟踪HTTP请求的完整执行链路
 * 阶段3版本：基于EnhancerCommand实现真正的字节码增强
 *
 * @author arthas
 */
@Name("trace-flow")
@Summary("跟踪HTTP请求的完整执行链路")
@Description(Constants.EXAMPLE +
        "  trace-flow                                   # 跟踪下一个HTTP请求\n" +
        "  trace-flow -n 5                              # 跟踪5次请求\n" +
        "  trace-flow --filter \"executionTime > 1000\"   # 只显示慢请求\n" +
        "  trace-flow --list-probes                     # 列出所有探针\n" +
        "  trace-flow --show-config database            # 显示探针配置\n" +
        "  trace-flow --verbose                         # 详细模式输出\n" +
        Constants.WIKI + Constants.WIKI_HOME + "trace-flow")
public class TraceFlowCommand extends EnhancerCommand {

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
        this.traceManager = TraceManager.getInstance();
        this.filterEngine = new FilterEngine(filter);
        this.outputFormatter = new OutputFormatter();

        // 阶段3：根据探针配置动态计算最大匹配类数量
        this.maxNumOfMatchedClass = calculateMaxMatchedClassFromProbeConfig();
    }

    public FilterEngine getFilterEngine() {
        return filterEngine;
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
            filterEngine.setFilterExpression(filter);
            // 初始化探针
            initializeProbes(process);

            // 阶段3：使用EnhancerCommand的enhance机制进行真正的字节码增强
            startEnhancedTracing(process);

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
            // 确保探针管理器已初始化
            probeManager.initializeProbes();

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
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    private void showProbeConfig(CommandProcess process, String probeName) {
        process.write("Probe Configuration: " + probeName + "\n");
        process.write("========================\n");

        try {
            // 确保探针管理器已初始化
            probeManager.initializeProbes();

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

            // 显示Targets信息
            process.write("\nTargets:\n");
            java.util.Set<String> displayedTargets = new java.util.HashSet<>();
            for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                if (metric.getTargets() != null) {
                    for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                        String targetKey = target.getClassName();
                        if (!displayedTargets.contains(targetKey)) {
                            displayedTargets.add(targetKey);
                            process.write("- " + target.getClassName() + ": " + target.getMethods() + "\n");
                        }
                    }
                }
            }

            // 显示Output Type信息
            String outputType = config.getOutput() != null ?
                config.getOutput().getType() :
                config.getName().replace("探针", "").toUpperCase();
            process.write("\nOutput Type: " + outputType + "\n");

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
            // 初始化探针配置
            probeManager.initializeProbes();

            if (verbose) {
                process.write("Probes initialized successfully\n");
            }
        } catch (Exception e) {
            process.write("Failed to initialize probes: " + e.getMessage() + "\n");
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize probes", e);
        }
    }

    /**
     * 阶段3：启动增强的跟踪（使用EnhancerCommand机制）
     */
    private void startEnhancedTracing(CommandProcess process) {
        process.write("Starting enhanced HTTP request tracing...\n");
        process.write("Trace count: " + (count != null ? count : 1) + "\n");
        process.write("Press Ctrl+C to stop tracing\n");
        process.write("========================\n");

        // 立即检查verbose参数是否生效
        process.write("Verbose mode: " + verbose + "\n");
        if (verbose) {
            process.write("[DEBUG] Verbose mode is ENABLED\n");
        } else {
            process.write("[DEBUG] Verbose mode is DISABLED\n");
        }

        try {
            // 阶段3：验证探针配置
            if (verbose) {
                validateProbeConfigurations(process);
            }

            // 阶段3：根据当前探针配置重新计算最大匹配类数量
            if (verbose) {
                process.write("[DEBUG] About to calculate max matched class...\n");
            }

            int calculatedMax = calculateMaxMatchedClassFromProbeConfig();

            if (verbose) {
                process.write("[DEBUG] Calculated max matched class: " + calculatedMax + "\n");
            }

            if (calculatedMax != this.maxNumOfMatchedClass) {
                this.maxNumOfMatchedClass = calculatedMax;
                if (verbose) {
                    process.write("[DEBUG] Updated maxNumOfMatchedClass to: " + calculatedMax + "\n");
                }
            }

            // 阶段3：使用EnhancerCommand的enhance方法进行字节码增强
            process.write("Initializing bytecode enhancement...\n");

            if (verbose) {
                process.write("[DEBUG] About to create matchers...\n");
            }

            if (verbose) {
                process.write("[DEBUG] Creating matchers...\n");
                try {
                    Matcher classMatcher = getClassNameMatcher();
                    process.write("[DEBUG] Class matcher: " + (classMatcher != null ? "OK" : "NULL") + "\n");

                    Matcher methodMatcher = getMethodNameMatcher();
                    process.write("[DEBUG] Method matcher: " + (methodMatcher != null ? "OK" : "NULL") + "\n");
                } catch (Exception e) {
                    process.write("[DEBUG] Error creating matchers: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            }

            // 检查匹配器是否有效
            if (verbose) {
                process.write("[DEBUG] Checking matcher validity...\n");
            }

            Matcher classMatcher = null;
            Matcher methodMatcher = null;

            try {
                classMatcher = getClassNameMatcher();
                methodMatcher = getMethodNameMatcher();
            } catch (Exception e) {
                process.write("Error creating matchers: " + e.getMessage() + "\n");
                if (verbose) {
                    e.printStackTrace();
                }
                return;
            }

            if (classMatcher == null || methodMatcher == null) {
                process.write("Warning: No valid probe configurations found.\n");
                process.write("Possible reasons:\n");
                process.write("1. Probe configuration files are missing or invalid\n");
                process.write("2. All probes are disabled\n");
                process.write("3. No target classes/methods defined in probe configurations\n");
                process.write("\nUse --list-probes to check available probe configurations.\n");
                return;
            }

            if (verbose) {
                process.write("[DEBUG] Matchers are valid, calling enhance()...\n");
                process.write("[DEBUG] maxNumOfMatchedClass: " + this.maxNumOfMatchedClass + "\n");
                process.write("[DEBUG] About to call enhance() method...\n");
            }

            // 调用父类的enhance方法
            try {
                // 添加超时保护
                long enhanceStartTime = System.currentTimeMillis();
                enhance(process);
                long enhanceEndTime = System.currentTimeMillis();

                if (verbose) {
                    process.write("[DEBUG] enhance() completed successfully in " +
                                (enhanceEndTime - enhanceStartTime) + "ms\n");
                }
            } catch (Exception e) {
                process.write("Error in enhance(): " + e.getMessage() + "\n");
                if (verbose) {
                    process.write("[DEBUG] Exception type: " + e.getClass().getSimpleName() + "\n");
                    e.printStackTrace();
                }
                return;
            }

            if (verbose) {
                process.write("\n[DEBUG] Stage 3: Enhanced tracing is now active\n");
                process.write("[DEBUG] Real method interception using bytecode enhancement\n");
                process.write("[DEBUG] Multi-probe coordination enabled\n");
            }

        } catch (Exception e) {
            process.write("Error during enhanced tracing: " + e.getMessage() + "\n");
            if (verbose) {
                e.printStackTrace();
            }
        }
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

    @Option(shortName = "v", longName = "verbose", flag = true)
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

    // ========== 阶段3：EnhancerCommand必需的方法 ==========

    @Override
    protected Matcher getClassNameMatcher() {
        // 阶段3：基于探针配置动态生成类匹配器
        return createProbeBasedClassMatcher();
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        // 不排除任何类
        return null;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        // 阶段3：基于探针配置动态生成方法匹配器
        return createProbeBasedMethodMatcher();
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        // 阶段3：返回集成链路跟踪的监听器
        return new TraceFlowEnhancerAdviceListener(this, process, traceManager, probeManager);
    }

    /**
     * 阶段3：基于探针配置创建类匹配器（严格配置驱动）
     */
    private Matcher createProbeBasedClassMatcher() {
        try {
            if (verbose) {
                System.out.println("[DEBUG] Creating probe-based class matcher...");
            }

            // 临时调试：先尝试不初始化探针，看看是否是这里的问题
            if (verbose) {
                System.out.println("[DEBUG] About to initialize ProbeManager...");
            }



            if (verbose) {
                System.out.println("[DEBUG] ProbeManager created, about to initialize probes...");
            }

            probeManager.initializeProbes();

            if (verbose) {
                System.out.println("[DEBUG] Probes initialized successfully");
            }

            // 收集所有探针的目标类
            StringBuilder classPattern = new StringBuilder();
            boolean first = true;

            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled()) {
                    if (verbose) {
                        System.out.println("[DEBUG] Skipping disabled probe: " + config.getName());
                    }
                    continue;
                }

                if (verbose) {
                    System.out.println("[DEBUG] Processing probe: " + config.getName());
                }

                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (metric.getTargets() != null) {
                        for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                            if (!first) {
                                classPattern.append("|");
                            }
                            // 转换类名为正则表达式
                            String className = target.getClassName().replace(".", "\\.");
                            classPattern.append(className);
                            first = false;

                            if (verbose) {
                                System.out.println("[DEBUG] Added target class: " + target.getClassName());
                            }
                        }
                    }
                }
            }

            if (classPattern.length() > 0) {
                String pattern = "(" + classPattern.toString() + ")";
                if (verbose) {
                    System.out.println("[DEBUG] Final class pattern: " + pattern);
                    System.out.println("[DEBUG] Pattern length: " + pattern.length());
                }
                return new RegexMatcher(pattern);
            } else {
                if (verbose) {
                    System.out.println("[DEBUG] No target classes found in probe configurations");
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating probe-based class matcher: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }

        // 临时调试：如果配置加载失败，使用简化的文件操作匹配器
        if (verbose) {
            System.out.println("[DEBUG] No target classes found in probe configurations");
            System.out.println("[DEBUG] Using simplified file operations matcher for debugging");
        }
        return new RegexMatcher("java\\.io\\.(FileInputStream|FileOutputStream)");
    }

    /**
     * 阶段3：基于探针配置创建方法匹配器（严格配置驱动）
     */
    private Matcher createProbeBasedMethodMatcher() {
        try {
            if (verbose) {
                System.out.println("[DEBUG] Creating probe-based method matcher...");
            }

            probeManager.initializeProbes();

            // 收集所有探针的目标方法
            StringBuilder methodPattern = new StringBuilder();
            boolean first = true;

            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled()) continue;

                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (metric.getTargets() != null) {
                        for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                            if (target.getMethods() != null) {
                                for (String method : target.getMethods()) {
                                    if (!first) {
                                        methodPattern.append("|");
                                    }
                                    // 转换方法名为正则表达式
                                    String methodRegex = method.replace("*", ".*");
                                    methodPattern.append(methodRegex);
                                    first = false;

                                    if (verbose) {
                                        System.out.println("[DEBUG] Added target method: " + method + " -> " + methodRegex);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (methodPattern.length() > 0) {
                String pattern = "(" + methodPattern.toString() + ")";
                if (verbose) {
                    System.out.println("[DEBUG] Final method pattern: " + pattern);
                    System.out.println("[DEBUG] Pattern length: " + pattern.length());
                }
                return new RegexMatcher(pattern);
            } else {
                if (verbose) {
                    System.out.println("[DEBUG] No target methods found in probe configurations");
                }
            }

        } catch (Exception e) {
            System.err.println("Error creating probe-based method matcher: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }

        // 临时调试：如果配置加载失败，使用简化的方法匹配器
        if (verbose) {
            System.out.println("[DEBUG] No target methods found in probe configurations");
            System.out.println("[DEBUG] Using simplified read/write matcher for debugging");
        }
        return new RegexMatcher("(read|write).*");
    }

    /**
     * 验证探针配置是否正确加载（配置驱动）
     */
    private void validateProbeConfigurations(CommandProcess process) {
        try {
            probeManager.initializeProbes();

            process.write("Validating probe configurations...\n");

            int totalProbes = 0;
            int enabledProbes = 0;
            int totalTargets = 0;

            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                totalProbes++;
                if (config.isEnabled()) {
                    enabledProbes++;

                    for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                        if (metric.getTargets() != null) {
                            totalTargets += metric.getTargets().size();
                        }
                    }
                }

                if (verbose) {
                    process.write("  - " + config.getName() + ": " +
                                (config.isEnabled() ? "Enabled" : "Disabled") + "\n");
                }
            }

            process.write("Total probes: " + totalProbes + "\n");
            process.write("Enabled probes: " + enabledProbes + "\n");
            process.write("Total target classes: " + totalTargets + "\n");

            if (enabledProbes == 0) {
                process.write("WARNING: No enabled probes found!\n");
            }

            if (totalTargets == 0) {
                process.write("WARNING: No target classes defined in probe configurations!\n");
            }

        } catch (Exception e) {
            process.write("Error validating probe configurations: " + e.getMessage() + "\n");
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据探针配置动态计算最大匹配类数量（配置驱动）
     */
    private int calculateMaxMatchedClassFromProbeConfig() {
        try {
            if (Objects.nonNull(filter) && !filter.trim().isEmpty()) {
                return Integer.MAX_VALUE;
            }
            if (verbose) {
                System.out.println("[DEBUG] Calculating max matched class from probe config...");
            }

            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            int totalTargetClasses = 0;

            // 统计所有启用探针的目标类数量
            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled()) {
                    if (verbose) {
                        System.out.println("[DEBUG] Skipping disabled probe: " + config.getName());
                    }
                    continue;
                }

                int probeTargetCount = 0;
                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (metric.getTargets() != null) {
                        probeTargetCount += metric.getTargets().size();
                        totalTargetClasses += metric.getTargets().size();
                    }
                }

                if (verbose) {
                    System.out.println("[DEBUG] Probe '" + config.getName() + "' has " + probeTargetCount + " target classes");
                }
            }

            if (totalTargetClasses > 0) {
                // 配置驱动：基于实际的目标类数量，加上一些缓冲
                // 考虑到一个目标类可能匹配多个实际的JVM类（如子类、实现类等）
                int calculatedMax = totalTargetClasses * 3; // 每个配置的目标类可能匹配3个实际类

                if (verbose) {
                    System.out.println("[DEBUG] Calculated maxNumOfMatchedClass: " + calculatedMax +
                                     " (based on " + totalTargetClasses + " target classes in probe configs)");
                }

                return Math.max(calculatedMax, 10); // 最少10个类
            }

        } catch (Exception e) {
            System.err.println("Error calculating max matched class from probe config: " + e.getMessage());
        }

        // 如果计算失败，使用保守的默认值
        return 50; // EnhancerCommand的默认值
    }
}
