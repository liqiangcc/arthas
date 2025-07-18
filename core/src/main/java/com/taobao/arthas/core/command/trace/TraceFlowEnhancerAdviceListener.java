package com.taobao.arthas.core.command.trace;

import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.view.Ansi;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
        if (probeConfig == null) {
            System.out.println("[DEBUG] No probe config found for: " + clazz.getName() + "." + method.getName());
            return;
        }
        TraceNode node = traceManager.startNode(probeConfig.getName(), methodSignature);
        node.setProbeConfig(probeConfig);

        List<ProbeConfig.MetricConfig> metricConfigs = probeManager.getMetricConfigs(clazz,method);
        List<ProbeConfig.MetricConfig> beforeMetricConfig = metricConfigs.stream().filter(metricConfig -> Objects.equals(metricConfig.getCapturePoint(), "before")).collect(Collectors.toList());
        if (!beforeMetricConfig.isEmpty()) {
            System.out.println("[DEBUG] beforeMetricConfig: " + beforeMetricConfig);
            populateNodeMerits(node,probeConfig,target,args,null,beforeMetricConfig);
        }
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
        System.out.println("[DEBUG] node: " + node);
        if (node != null) {
            // 阶段3：结束链路跟踪节点
            populateNodeMeritsAtAfter(node, loader,  clazz,  method,  target, args ,  returnObject, throwable);
            // 显示执行完成信息
            if (command.isVerbose()) {
                process.write("  Execution Time: " + node.getEndTime() + "ms\n");
                process.write("  Completed at: " + dateFormat.format(new Date(node.getEndTime())) + "\n");
            }

            boolean filtered = filter(node);
            if (!filtered) {
                System.out.println("[DEBUG] filtered: " + filtered + " node: " + JSONObject.toJSONString(node));
                traceManager.endTrace();
                return;
            }
            if (command.isVerbose()) {
                System.out.println("[DEBUG] filtered: " + filtered + " node: " + JSONObject.toJSONString(node));
            }
            // 基于nodeStack的出口判断：检查是否是trace的根节点
            boolean isTraceRoot = isTraceRootNode(node);
            if (command.isVerbose()) {
                process.write("[DEBUG] isTraceRoot: " + isTraceRoot + " node: " + JSONObject.toJSONString(node));
            }
            if (isTraceRoot) {
                if (command.isVerbose()) {
                    process.write("[DEBUG] Trace root exit detected, outputting tree trace for: " + node.getMethodSignature() + "\n");
                }
                node.setAttribute(command.getFilter(), true);
                // 输出完整的树状trace结果
                outputTreeTrace(node);
                traceManager.endTrace();
                // 完整的trace结束，增加计数
                int count = process.times().incrementAndGet();
                if (isLimitExceeded(count, command.getCount())) {
                    abortProcess(process, command.getCount() != null ? command.getCount() : 1);
                }
            }
        }
    }

    private void populateNodeMeritsAtAfter(TraceNode node, ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject, Throwable throwable) {
        List<ProbeConfig.MetricConfig> metricConfigs = probeManager.getMetricConfigs(clazz, method);
        List<ProbeConfig.MetricConfig> afterMetricConfig = metricConfigs.stream().filter(metricConfig -> Objects.equals(metricConfig.getCapturePoint(), "after")).collect(Collectors.toList());
        if (!afterMetricConfig.isEmpty()) {
            System.out.println("[DEBUG] afterMetricConfig: " + afterMetricConfig);
            populateNodeMerits(node,probeManager.getProbeConfig(clazz,method),target,args,returnObject,afterMetricConfig);
        }
    }

    private boolean filter(TraceNode node) {
        FilterEngine filterEngine = command.getFilterEngine();
        boolean result = filterEngine.matches(node.getAttributes());
        System.out.println("[DEBUG] filtered: " + result + " node: " + JSONObject.toJSONString(node));
        return result;
    }


    /**
     * 输出树状trace结果
     */
    private void outputTreeTrace(TraceNode node) {
        try {
            String traceId = traceManager.getCurrentTraceId();
            if (traceId == null) {
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
                    .fg(Ansi.Color.MAGENTA).a(1 + countTotalNodes(node.getChildren())).reset();
            output.append(nodesAnsi.toString()).append("\n");

            // 分隔线（蓝色）
            Ansi separatorLine = Ansi.ansi().fg(Ansi.Color.BLUE).a(repeat("-", 80)).reset();
            output.append(separatorLine.toString()).append("\n");
            List<TraceNode> rootNodes = new ArrayList<>();
            rootNodes.add(node);
            // 构建树状结构输出
            buildTreeOutput(rootNodes, output, "", true);

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
        System.out.println("[DEBUG] nodes: " + JSONObject.toJSONString(nodes));
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
            String s = isRoot ? "" : prefix + (isLast ? "    " : "|   ");
            if (attributes != null && !attributes.isEmpty()) {

                for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                    output.append(s);
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
            buildTreeOutput(node.getChildren(), output, s, false);
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

    private static void populateNodeMerits(TraceNode node, ProbeConfig config, Object target, Object[] args, Object returnObject, List<ProbeConfig.MetricConfig> metrics) {
        System.out.println(node.getMethodSignature() + " metrics: " + metrics);
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
                System.out.println(node.getMethodSignature() + " source: " + source);
                Object parseValue = sourceExpressionParser.parse(source, context);
                System.out.println("metricName: " + metricName + " source: " + source + ", parseValue: " + parseValue);
                if (parseValue != null) {
                    node.setAttribute(metricName, parseValue);
                }
            }
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
        System.out.println("isTraceRootNode: " + JSONObject.toJSONString(node));
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
