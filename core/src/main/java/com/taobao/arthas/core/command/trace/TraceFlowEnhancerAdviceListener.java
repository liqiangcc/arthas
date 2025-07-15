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

        // 配置驱动：检查是否是跟踪入口
        boolean isEntry = isEntryMethod(clazz.getName(), method.getName());

        // 关键逻辑：只有当前线程没有Trace ID时，入口方法才真正作为入口处理
        if (isEntry && traceManager.getCurrentTraceId() == null) {
            traceManager.startTrace(clazz.getName(), method.getName()); // 记录第一个入口方法
        }

        TraceNode node = traceManager.startNode(nodeType, methodSignature);
        
        // 存储到线程本地变量
        startTimeHolder.set(startTime);
        nodeHolder.set(node);
        
        // 阶段3：实时输出拦截信息
        outputInterceptionInfo(clazz, method, target, args, startTime, nodeType);

        // 注意：不在这里增加计数，计数应该在完整的trace结束时进行
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

                // 配置驱动：检查是否是出口方法，并验证与入口方法的关联
                String currentMethodSignature = clazz.getName() + "." + method.getName();
                if (isExitMethod(clazz.getName(), method.getName()) &&
                    isMatchingExitMethod(currentMethodSignature)) {
                    traceManager.endTrace();

                    // 完整的trace结束，增加计数
                    process.times().incrementAndGet();
                    if (isLimitExceeded(command.getCount() != null ? command.getCount() : 1, process.times().get())) {
                        abortProcess(process, command.getCount() != null ? command.getCount() : 1);
                    }
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

            // 配置驱动：如果是跟踪入口且发生异常，也要清理跟踪上下文并增加计数
            if (isTraceEntry(clazz.getName(), method.getName())) {
                traceManager.endTrace();

                // 完整的trace结束（虽然有异常），增加计数
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getCount() != null ? command.getCount() : 1, process.times().get())) {
                    abortProcess(process, command.getCount() != null ? command.getCount() : 1);
                }
            }
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
     * 配置驱动：判断是否是入口方法
     */
    private boolean isEntryMethod(String className, String methodName) {
        try {
            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            // 遍历所有探针配置，查找入口方法
            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled() || !config.isTraceEntry()) {
                    continue;
                }

                // 检查入口方法配置
                if (config.getEntryMethods() != null) {
                    for (ProbeConfig.MethodConfig entryMethod : config.getEntryMethods()) {
                        if (className.equals(entryMethod.getClassName()) &&
                            methodName.equals(entryMethod.getMethodName())) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error checking entry method: " + e.getMessage());
        }

        return false;
    }

    /**
     * 配置驱动：判断是否是出口方法
     */
    private boolean isExitMethod(String className, String methodName) {
        try {
            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            // 遍历所有探针配置，查找出口方法
            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled() || !config.isTraceExit()) {
                    continue;
                }

                // 检查出口方法配置
                if (config.getExitMethods() != null) {
                    for (ProbeConfig.MethodConfig exitMethod : config.getExitMethods()) {
                        if (className.equals(exitMethod.getClassName()) &&
                            methodName.equals(exitMethod.getMethodName())) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error checking exit method: " + e.getMessage());
        }

        return false;
    }

    /**
     * 配置驱动：验证出口方法是否与第一个入口方法匹配
     * 一次跟踪中，只有与第一个入口方法对应的出口方法才能结束跟踪
     */
    private boolean isMatchingExitMethod(String currentMethodSignature) {
        String entryClassName = traceManager.getEntryClassName();
        String entryMethodName = traceManager.getEntryMethodName();

        if (entryClassName == null || entryMethodName == null) {
            return false;
        }

        try {
            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            // 遍历所有探针配置，查找与第一个入口方法对应的出口方法
            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled() || !config.isTraceExit()) {
                    continue;
                }

                // 检查是否有匹配的入口方法
                boolean hasMatchingEntry = false;
                if (config.getEntryMethods() != null) {
                    for (ProbeConfig.MethodConfig entryMethod : config.getEntryMethods()) {
                        if (entryClassName.equals(entryMethod.getClassName()) &&
                            entryMethodName.equals(entryMethod.getMethodName())) {
                            hasMatchingEntry = true;
                            break;
                        }
                    }
                }

                // 如果找到匹配的入口方法，检查对应的出口方法
                if (hasMatchingEntry && config.getExitMethods() != null) {
                    for (ProbeConfig.MethodConfig exitMethod : config.getExitMethods()) {
                        String exitMethodSig = exitMethod.getClassName() + "." + exitMethod.getMethodName();
                        if (currentMethodSignature.equals(exitMethodSig)) {
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error checking matching exit method: " + e.getMessage());
        }

        return false;
    }

    /**
     * 配置驱动：判断是否是跟踪入口（兼容旧方法）
     */
    private boolean isTraceEntry(String className, String methodName) {
        try {
            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            // 遍历所有探针配置，查找标记为traceEntry的探针
            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                if (!config.isEnabled() || !config.isTraceEntry()) {
                    continue;
                }

                // 检查当前类和方法是否匹配这个探针的目标
                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (metric.getTargets() != null) {
                        for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                            if (className.equals(target.getClassName())) {
                                // 检查方法是否匹配
                                if (target.getMethods() != null) {
                                    for (String method : target.getMethods()) {
                                        if (methodName.matches(method.replace("*", ".*"))) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error checking trace entry: " + e.getMessage());
        }

        return false;
    }

    /**
     * 配置驱动：判断请求是否真正结束
     */
    private boolean isRequestEnd(String className, String methodName) {
        // 1. 必须是跟踪入口
        if (!isTraceEntry(className, methodName)) {
            return false;
        }

        // 2. 检查是否是最外层的HTTP调用
        // 通过检查当前线程的调用栈深度来判断
        try {
            String currentTraceId = traceManager.getCurrentTraceId();
            if (currentTraceId == null) {
                return false;
            }

            // 获取当前线程的节点栈
            // 如果栈中只有当前节点，说明这是最外层调用
            return isOutermostCall(className, methodName);

        } catch (Exception e) {
            // 如果检查失败，保守地认为是请求结束
            return true;
        }
    }

    /**
     * 检查是否是最外层调用
     */
    private boolean isOutermostCall(String className, String methodName) {
        try {
            // 方法1：检查调用栈深度
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            int httpServletCallCount = 0;

            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().equals(className) &&
                    element.getMethodName().equals(methodName)) {
                    httpServletCallCount++;
                }
            }

            // 如果调用栈中只有一个HttpServlet.service调用，说明是最外层
            return httpServletCallCount <= 1;

        } catch (Exception e) {
            // 如果检查失败，保守地认为是最外层调用
            return true;
        }
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
