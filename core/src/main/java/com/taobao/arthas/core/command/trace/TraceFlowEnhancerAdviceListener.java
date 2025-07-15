package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.shell.command.CommandProcess;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TraceFlow命令的EnhancerAdvice监听器
 * 阶段3版本：集成链路跟踪和多探针协同
 */
public class TraceFlowEnhancerAdviceListener extends AdviceListenerAdapter {
    
    private final TraceFlowCommand command;
    private final CommandProcess process;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final TraceManager traceManager = TraceManager.getInstance();

    public TraceFlowEnhancerAdviceListener(TraceFlowCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }
    
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        System.out.println("[DEBUG] before called for: " + clazz.getName() + "." + method.getName());
        // 阶段3：启动链路跟踪节点
        String nodeType = determineNodeType(clazz.getName());
        String methodSignature = clazz.getName() + "." + method.getName();
        traceManager.startNode(nodeType, methodSignature);
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

            // 基于nodeStack的出口判断：检查是否是trace的根节点
            boolean isTraceRoot = isTraceRootNode(node);
            if (isTraceRoot) {
                if (command.isVerbose()) {
                    process.write("[DEBUG] Trace root exit detected, outputting tree trace for: " + node.getMethodSignature() + "\n");
                }
                // 输出完整的树状trace结果
                outputTreeTrace();
                traceManager.endTrace();
                // 完整的trace结束，增加计数
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getCount() != null ? command.getCount() : 1, process.times().get())) {
                    abortProcess(process, command.getCount() != null ? command.getCount() : 1);
                }
            }
        }
    }

    
    /**
     * 阶段3：确定节点类型（完全配置驱动）
     */
    private String determineNodeType(String className) {
        try {
            // 配置驱动：从探针配置中查找匹配的类
            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled()) continue;

                // 检查这个类是否在当前探针的目标类中
                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (metric.getTargets() != null) {
                        for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                            if (className.equals(target.getClassName())) {
                                // 找到匹配的探针，返回探针的输出类型
                                return getNodeTypeFromProbeConfig(config);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error determining node type from config: " + e.getMessage());
        }

        // 如果配置中没有找到，返回通用类型
        return "METHOD_CALL";
    }

    /**
     * 从探针配置中获取节点类型（配置驱动）
     */
    private String getNodeTypeFromProbeConfig(ProbeConfig config) {
        // 优先使用配置中的output.type
        if (config.getOutput() != null && config.getOutput().getType() != null) {
            return config.getOutput().getType();
        }

        // 如果没有配置output.type，根据探针名称推断
        String probeName = config.getName();
        if (probeName != null) {
            // 移除"探针"或"Probe"后缀，转换为大写
            return probeName.replace("探针", "")
                           .replace("Probe", "")
                           .replace(" ", "_")
                           .toUpperCase();
        }

        // 最后的默认值
        return "UNKNOWN";
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
            output.append("\n").append(repeat("=", 80)).append("\n");
            output.append("Trace ID: ").append(traceId).append("\n");
            output.append("Thread: ").append(Thread.currentThread().getName()).append("\n");
            output.append("Total Nodes: ").append(countTotalNodes(context.getRootNodes())).append("\n");
            output.append(repeat("-", 80)).append("\n");

            // 构建树状结构输出
            buildTreeOutput(context.getRootNodes(), output, "", true);

            output.append(repeat("=", 80)).append("\n");
            process.write(output.toString());

        } catch (Exception e) {
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
                output.append(isLast ? "+-- " : "|-- ");
            }

            // 时间戳
            output.append("[").append(dateFormat.format(new Date(node.getStartTime()))).append("] ");

            // 节点类型
            output.append("[").append(node.getNodeType()).append("] ");

            // 方法签名（简化显示）
            String methodName = simplifyMethodName(node.getMethodSignature());
            output.append(methodName);

            // 执行时间
            if (node.getExecutionTime() > 0) {
                output.append(" (").append(node.getExecutionTime()).append("ms)");
            }

            // 参数信息（简化显示）
            if (node.getArgs() != null && node.getArgs().length > 0) {
                String argsStr = formatArgs(node.getArgs());
                if (!argsStr.isEmpty()) {
                    output.append(" ").append(argsStr);
                }
            }

            output.append("\n");

            // 递归输出子节点
            String childPrefix = isRoot ? "" : (prefix + (isLast ? "    " : "|   "));
            buildTreeOutput(node.getChildren(), output, childPrefix, false);
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
            ProbeManager probeManager = new ProbeManager();
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
            for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                String metricName = metric.getName();
                String source = metric.getSource();

                if (source != null && !source.isEmpty()) {
                    // 使用SourceExpressionParser解析source表达式
                    Object value = parseSourceExpression(source, target, args, returnObject);
                    if (value != null) {
                        node.setAttribute(metricName, value);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error extracting attributes from metrics: " + e.getMessage());
        }
    }

    /**
     * 解析source表达式（简化版本）
     */
    private Object parseSourceExpression(String source, Object target, Object[] args, Object returnObject) {
        try {
            // 简化的表达式解析
            if ("target".equals(source) && target != null) {
                return target.toString();
            } else if ("args[0]".equals(source) && args != null && args.length > 0) {
                return args[0];
            } else if ("returnValue".equals(source)) {
                return returnObject;
            } else if (source.startsWith("args[") && source.endsWith("]")) {
                // 解析 args[n] 格式
                String indexStr = source.substring(5, source.length() - 1);
                int index = Integer.parseInt(indexStr);
                if (args != null && index >= 0 && index < args.length) {
                    return args[index];
                }
            }

        } catch (Exception e) {
            // 忽略解析错误
        }

        return null;
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
     * 简化方法名显示
     */
    private String simplifyMethodName(String methodSignature) {
        if (methodSignature == null) {
            return "unknown";
        }

        // 提取类名和方法名
        int lastDot = methodSignature.lastIndexOf('.');
        if (lastDot > 0) {
            String className = methodSignature.substring(0, lastDot);
            String methodName = methodSignature.substring(lastDot + 1);

            // 简化类名（只保留最后一部分）
            int classLastDot = className.lastIndexOf('.');
            if (classLastDot > 0) {
                className = className.substring(classLastDot + 1);
            }

            return className + "." + methodName;
        }

        return methodSignature;
    }


    

    
    /**
     * 格式化参数显示
     */
    private String formatArgs(Object[] args) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            if (args[i] != null) {
                if (args[i] instanceof byte[]) {
                    sb.append("byte[").append(((byte[])args[i]).length).append("]");
                } else {
                    String argStr = args[i].toString();
                    if (argStr.length() > 50) {
                        argStr = argStr.substring(0, 50) + "...";
                    }
                    sb.append(argStr);
                }
            } else {
                sb.append("null");
            }
        }
        sb.append("]");
        return sb.toString();
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
