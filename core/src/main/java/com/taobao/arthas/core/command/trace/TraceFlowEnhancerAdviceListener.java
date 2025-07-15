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
    private final TraceResultCollector resultCollector = TraceResultCollector.getInstance();

    // 简化版本：使用ThreadLocal存储开始时间
    private final ThreadLocal<Long> startTimeHolder = new ThreadLocal<>();
    private final ThreadLocal<TraceNode> nodeHolder = new ThreadLocal<>();
    
    public TraceFlowEnhancerAdviceListener(TraceFlowCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }
    
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 阶段3：启动链路跟踪节点
        String nodeType = determineNodeType(clazz.getName());
        String methodSignature = clazz.getName() + "." + method.getName();
        TraceNode node = traceManager.startNode(nodeType, methodSignature);
        
        // 存储到线程本地变量
        startTimeHolder.set(startTime);
        nodeHolder.set(node);
        
        // 阶段3：实时输出拦截信息
        outputInterceptionInfo(clazz, method, target, args, startTime, nodeType);
        
        // 增加计数
        process.times().incrementAndGet();
        if (isLimitExceeded(command.getCount() != null ? command.getCount() : 100, process.times().get())) {
            abortProcess(process, command.getCount() != null ? command.getCount() : 100);
        }
    }
    
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {
        try {
            Long startTime = startTimeHolder.get();
            TraceNode node = nodeHolder.get();

            if (startTime != null && node != null) {
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;

                // 阶段3：结束链路跟踪节点
                populateNodeAttributes(node, clazz.getName(), target, args, returnObject, executionTime);
                traceManager.endNode(node);

                // 显示执行完成信息
                if (command.isVerbose()) {
                    process.write("  Execution Time: " + executionTime + "ms\n");
                    process.write("  Completed at: " + dateFormat.format(new Date(endTime)) + "\n");
                }
            }
        } finally {
            startTimeHolder.remove();
            nodeHolder.remove();
        }
    }
    
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
        try {
            TraceNode node = nodeHolder.get();
            if (node != null) {
                node.setException(throwable);
                traceManager.endNode(node);
            }

            // 显示异常信息
            process.write("  Exception: " + throwable.getMessage() + "\n");
        } finally {
            startTimeHolder.remove();
            nodeHolder.remove();
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
     * 阶段3：输出拦截信息
     */
    private void outputInterceptionInfo(Class<?> clazz, ArthasMethod method, Object target, Object[] args, long startTime, String nodeType) {
        StringBuilder output = new StringBuilder();
        output.append("\n").append(repeat("=", 60)).append("\n");
        output.append("[").append(dateFormat.format(new Date(startTime))).append("] ");
        output.append("[").append(nodeType).append("]\n");
        output.append("  Method: ").append(clazz.getName()).append(".").append(method.getName()).append("\n");
        output.append("  Thread: ").append(Thread.currentThread().getName()).append("\n");
        
        // 显示参数
        if (args != null && args.length > 0) {
            output.append("  Args: ").append(formatArgs(args)).append("\n");
        }
        
        // 显示Trace ID
        String traceId = traceManager.getCurrentTraceId();
        if (traceId != null) {
            output.append("  Trace ID: ").append(traceId).append("\n");
        }
        
        output.append(repeat("=", 60)).append("\n");
        process.write(output.toString());
    }
    
    /**
     * 阶段3：填充节点属性（完全配置驱动）
     */
    private void populateNodeAttributes(TraceNode node, String className, Object target, Object[] args, Object returnObject, long executionTime) {
        try {
            // 配置驱动：根据探针配置中的指标定义来提取属性
            populateAttributesFromProbeConfig(node, className, target, args, returnObject);

            // 通用属性
            node.setAttribute("executionTime", executionTime);
            node.setAttribute("threadName", Thread.currentThread().getName());

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
}
