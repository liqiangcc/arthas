package com.taobao.arthas.core.command.trace;

import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.view.Ansi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TraceFlow命令的EnhancerAdvice监听器
 * 阶段3版本：集成链路跟踪和多探针协同
 */
public class TraceFlowEnhancerAdviceListener extends AdviceListenerAdapter {
    
    private final TraceFlowCommand command;
    private final CommandProcess process;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final TraceManager traceManager;
    private final ProbeManager probeManager;

    public TraceFlowEnhancerAdviceListener(TraceFlowCommand command, CommandProcess process, TraceManager traceManager,ProbeManager probeManager) {
        this.command = command;
        this.process = process;
        this.traceManager = traceManager;
        this.probeManager = probeManager;
    }
    
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        System.out.println("[DEBUG] before called for: " + clazz.getName() + "." + method.getName());
        // 阶段3：启动链路跟踪节点
        String methodSignature = clazz.getName() + "." + method.getName();
        ProbeConfig probeConfig = probeManager.getProbeConfig(clazz, method);
        TraceNode node = traceManager.startNode(probeConfig.getName(), methodSignature);

        node.setProbeConfig(probeConfig);
    }
    
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {
        after(loader, clazz, method, target, args, returnObject, null);
    }
    
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
        after(loader, clazz, method, target, args, null, throwable);
    }

    private void after(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args , Object returnObject,Throwable throwable) {
        System.out.println("[DEBUG] after called for: " + clazz.getName() + "." + method.getName());
        TraceNode node = traceManager.finishedNode();
        if (node != null) {
            // 阶段3：结束链路跟踪节点
            node.setException(throwable);
            populateNodeAttributes(node, clazz.getName(), target, args, returnObject);
            // 显示执行完成信息
            if (command.isVerbose()) {
                process.write("  Execution Time: " + node.getEndTime() + "ms\n");
                process.write("  Completed at: " + dateFormat.format(new Date(node.getEndTime())) + "\n");
            }

            boolean filtered = filter(node);
            if (!filtered) {
                traceManager.endTrace();
                return;
            }
            // 基于nodeStack的出口判断：检查是否是trace的根节点
            boolean isTraceRoot = isTraceRootNode(node);
            if (isTraceRoot) {
                if (command.isVerbose()) {
                    process.write("[DEBUG] Trace root exit detected, outputting tree trace for: " + node.getMethodSignature() + "\n");
                }
                node.setAttribute(command.getFilter(), true);
                // 输出完整的树状trace结果
                outputTreeTrace();
                traceManager.endTrace();
                // 完整的trace结束，增加计数
                int count = process.times().incrementAndGet();
                if (isLimitExceeded(count, command.getCount())) {
                    abortProcess(process, command.getCount() != null ? command.getCount() : 1);
                }
            }
        }
    }

    private boolean filter(TraceNode node) {
        FilterEngine filterEngine = command.getFilterEngine();
        boolean result = filterEngine.matches(node.getAttributes());
        System.out.println("[DEBUG] filter result: " + result);
        return result;
    }


    /**
     * 输出树状trace结果
     */
    private void outputTreeTrace() {
        try {
            String traceId = traceManager.getCurrentTraceId();
            if (traceId == null) {
                return;
            }

            // 获取trace上下文中的所有节点
            TraceManager.TraceContext context = traceManager.getTraceContext(traceId);
            if (context == null || context.getRootNodes().isEmpty()) {
                return;
            }

            StringBuilder output = new StringBuilder();

            // 头部分隔线（蓝色）
            Ansi headerLine = Ansi.ansi().fg(Ansi.Color.BLUE).a(repeat("=", 80)).reset();
            output.append("\n").append(headerLine.toString()).append("\n");

            // Trace ID（青色）
            Ansi traceIdAnsi = Ansi.ansi().fg(Ansi.Color.CYAN).a("Trace ID: ").reset()
                    .fg(Ansi.Color.YELLOW).a(traceId).reset();
            output.append(traceIdAnsi.toString()).append("\n");

            // Thread（绿色）
            Ansi threadAnsi = Ansi.ansi().fg(Ansi.Color.CYAN).a("Thread: ").reset()
                    .fg(Ansi.Color.GREEN).a(Thread.currentThread().getName()).reset();
            output.append(threadAnsi.toString()).append("\n");

            // Total Nodes（洋红色）
            Ansi nodesAnsi = Ansi.ansi().fg(Ansi.Color.CYAN).a("Total Nodes: ").reset()
                    .fg(Ansi.Color.MAGENTA).a(countTotalNodes(context.getRootNodes())).reset();
            output.append(nodesAnsi.toString()).append("\n");

            // 分隔线（蓝色）
            Ansi separatorLine = Ansi.ansi().fg(Ansi.Color.BLUE).a(repeat("-", 80)).reset();
            output.append(separatorLine.toString()).append("\n");

            // 构建树状结构输出
            buildTreeOutput(context.getRootNodes(), output, "", true);

            // 尾部分隔线（蓝色）
            Ansi footerLine = Ansi.ansi().fg(Ansi.Color.BLUE).a(repeat("=", 80)).reset();
            output.append(footerLine.toString()).append("\n");
            process.write(output.toString());

        } catch (Exception e) {
            e.printStackTrace(System.err);
            process.write("Error outputting tree trace: " + e.getMessage() + "\n");
        }
    }

    /**
     * 递归构建树状输出
     */
    private void buildTreeOutput(java.util.List<TraceNode> nodes, StringBuilder output, String prefix, boolean isRoot) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (int i = 0; i < nodes.size(); i++) {
            TraceNode node = nodes.get(i);
            boolean isLast = (i == nodes.size() - 1);

            // 输出当前节点
            if (!isRoot) {
                output.append(prefix);
                output.append(isLast ? "`-- " : "+-- ");
            }

            // 节点类型（带颜色）
            String nodeType = node.getNodeType();
            ProbeConfig probeConfig = node.getProbeConfig();
            ProbeConfig.OutputConfig outputConfig = probeConfig.getOutput();
            String colorName = outputConfig.getColorName();
            Ansi.Color color = Ansi.Color.valueOf(colorName);
            Ansi nodeTypeAnsi = Ansi.ansi().fg(color).a("[").a(nodeType).a("]").reset();
            output.append(nodeTypeAnsi.toString()).append(" ");

            // 方法名（带颜色）
            Ansi methodAnsi = Ansi.ansi().fg(color).a(node.getMethodSignature()).reset();
            output.append(methodAnsi.toString());

            // 执行时间（根据时间长短显示不同颜色）
            long executionTime = node.getExecutionTime();
            Ansi timeAnsi = Ansi.ansi().fg(getTimeColor(executionTime))
                    .a(" (").a(executionTime).a("ms)").reset();
            output.append(timeAnsi.toString());
            output.append("\n");

            // 输出属性信息（metrics）
            Map<String, Object> attributes = node.getAttributes();
            if (attributes != null && !attributes.isEmpty()) {
                String attrPrefix = isRoot ? "" : prefix + (isLast ? "    " : "|   ");

                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    output.append(attrPrefix);
                    output.append("  +- ");

                    // 属性名（黄色）
                    Ansi attrNameAnsi = Ansi.ansi().fg(Ansi.Color.YELLOW).a(entry.getKey()).reset();
                    output.append(attrNameAnsi.toString()).append(": ");

                    // 属性值（绿色）
                    Ansi attrValueAnsi = Ansi.ansi().fg(Ansi.Color.GREEN).a(entry.getValue()).reset();
                    output.append(attrValueAnsi.toString());
                    output.append("\n");
                }
            }

            // 递归输出子节点
            String childPrefix = isRoot ? "" : prefix + (isLast ? "    " : "|   ");
            buildTreeOutput(node.getChildren(), output, childPrefix, false);
        }
    }

    /**
     * 根据执行时间获取颜色
     */
    private Ansi.Color getTimeColor(long executionTime) {
        if (executionTime >= 1000) {
            return Ansi.Color.RED;      // 超过1秒，红色警告
        } else if (executionTime >= 500) {
            return Ansi.Color.YELLOW;   // 超过500ms，黄色提醒
        } else if (executionTime >= 100) {
            return Ansi.Color.CYAN;     // 超过100ms，青色
        } else {
            return Ansi.Color.GREEN;    // 小于100ms，绿色正常
        }
    }

    /**
     * 阶段3：填充节点属性（完全配置驱动）
     */
    private void populateNodeAttributes(TraceNode node, String className, Object target, Object[] args, Object returnObject) {
        try {
            // 配置驱动：根据探针配置中的指标定义来提取属性
            populateAttributesFromProbeConfig(node, className, target, args, returnObject);
            // 通用属性
        } catch (Exception e) {
            // 忽略属性填充错误
        }
    }

    /**
     * 根据探针配置提取属性（配置驱动）
     */
    private void populateAttributesFromProbeConfig(TraceNode node, String className, Object target, Object[] args, Object returnObject) {
        try {
            probeManager.initializeProbes();

            // 找到匹配的探针配置
            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled()) continue;

                // 检查这个类是否在当前探针的目标类中
                boolean isMatchingProbe = false;
                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (metric.getTargets() != null) {
                        for (ProbeConfig.TargetConfig targetConfig : metric.getTargets()) {
                            if (className.equals(targetConfig.getClassName())) {
                                isMatchingProbe = true;
                                break;
                            }
                        }
                    }
                    if (isMatchingProbe) break;
                }

                if (isMatchingProbe) {
                    // 使用这个探针的配置来提取属性
                    extractAttributesFromMetrics(node, config, target, args, returnObject);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error populating attributes from probe config: " + e.getMessage());
        }
    }

    /**
     * 从指标配置中提取属性（配置驱动）
     */
    private void extractAttributesFromMetrics(TraceNode node, ProbeConfig config, Object target, Object[] args, Object returnObject) {
        try {
            // 遍历探针配置中的所有指标
            List<ProbeConfig.MetricConfig> metrics = config.getMetrics(node.getMethodSignature());
            if (metrics == null || metrics.isEmpty()) {
                System.out.println("No metrics found for method signature: " + node.getMethodSignature());
                System.out.println("config: " + JSONObject.toJSONString(config));
                return;
            }
            for (ProbeConfig.MetricConfig metric : metrics) {
                System.out.println("metric: " + metric + " node.getMethodSignature(): " + node.getMethodSignature());
                String metricName = metric.getName();
                String source = metric.getSource();
                if (source != null && !source.isEmpty()) {
                    // 使用SourceExpressionParser解析source表达式
                    SourceExpressionParser sourceExpressionParser = new SourceExpressionParser();
                    ExecutionContext context = new ExecutionContext(target, args, null);
                    context.setReturnValue(returnObject);
                    context.setException(context.getException());
                    Object parseValue = sourceExpressionParser.parse(source, context);
                    System.out.println("metricName: " + metricName + " source: " + source + ", parseValue: " + parseValue);
                    if (parseValue != null) {
                        node.setAttribute(metricName, parseValue);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error extracting attributes from metrics: " );
            e.printStackTrace();
        }
    }

    /**
     * 统计总节点数
     */
    private int countTotalNodes(java.util.List<TraceNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }

        int count = nodes.size();
        for (TraceNode node : nodes) {
            count += countTotalNodes(node.getChildren());
        }
        return count;
    }


    /**
     * Java 8兼容的字符串重复方法
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 基于nodeStack判断节点是否是trace的根节点
     * 根节点是trace的入口点，也是最后一个退出的节点
     */
    private boolean isTraceRootNode(TraceNode node) {
        if (node == null) {
            return false;
        }

        try {
            // 判断节点是否是根节点
            // 1. 节点的parent为null
            // 2. 节点的depth为0
            // 3. 节点在TraceContext.rootNodes中

            // 简单判断：节点的depth为0
            return node.getDepth() == 0 ||  node.getParent() == null;

        } catch (Exception e) {
            if (command.isVerbose()) {
                process.write("[DEBUG] Error in isTraceRootNode: " + e.getMessage() + "\n");
            }
            return false;
        }
    }


}
