package com.taobao.arthas.core.command.trace;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 配置驱动的方法拦截器
 * 完全基于JSON配置文件，无任何硬编码逻辑
 */
public class ConfigurableMethodInterceptor implements MethodInterceptor {

    private final ProbeConfig probeConfig;
    private final MetricCollector metricCollector;
    private boolean enabled = true;

    public ConfigurableMethodInterceptor(ProbeConfig probeConfig) {
        this.probeConfig = probeConfig;
        this.metricCollector = new ConfigurableMetricCollector(probeConfig);
    }

    @Override
    public String getName() {
        return probeConfig.getName() + "Interceptor";
    }

    @Override
    public boolean shouldIntercept(String className, String methodName, Method method) {
        if (!enabled || !probeConfig.isEnabled()) {
            return false;
        }

        // 检查是否匹配配置中定义的任何target
        for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
            if (metric.getTargets() != null) {
                for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                    if (matchesTarget(className, methodName, method, target)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查方法是否匹配目标配置
     */
    private boolean matchesTarget(String className, String methodName, Method method, ProbeConfig.TargetConfig target) {
        // 检查类名匹配
        if (!matchesClassName(className, target.getClassName())) {
            return false;
        }

        // 检查方法名匹配
        if (!matchesMethodName(methodName, target.getMethods())) {
            return false;
        }

        // 检查类注解匹配（如果配置了）
        if (target.getClassAnnotation() != null) {
            if (!hasClassAnnotation(className, target.getClassAnnotation())) {
                return false;
            }
        }

        // 检查方法注解匹配（如果配置了）
        if (target.getMethodAnnotation() != null) {
            if (!hasMethodAnnotation(method, target.getMethodAnnotation())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查类名是否匹配
     */
    private boolean matchesClassName(String actualClassName, String targetClassName) {
        if (targetClassName == null) {
            return false;
        }

        // 精确匹配
        if (actualClassName.equals(targetClassName)) {
            return true;
        }

        // 支持通配符匹配
        if (targetClassName.contains("*")) {
            String pattern = targetClassName.replace("*", ".*");
            return actualClassName.matches(pattern);
        }

        // 检查继承关系
        return isAssignableFrom(actualClassName, targetClassName);
    }

    /**
     * 检查方法名是否匹配
     */
    private boolean matchesMethodName(String actualMethodName, List<String> targetMethods) {
        if (targetMethods == null || targetMethods.isEmpty()) {
            return true; // 如果没有指定方法，匹配所有方法
        }

        for (String targetMethod : targetMethods) {
            if (actualMethodName.equals(targetMethod)) {
                return true;
            }
            
            // 支持通配符匹配
            if (targetMethod.contains("*")) {
                String pattern = targetMethod.replace("*", ".*");
                if (actualMethodName.matches(pattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查类是否有指定注解
     */
    private boolean hasClassAnnotation(String className, String annotationName) {
        try {
            Class<?> clazz = Class.forName(className);
            Class<?> annotationClass = Class.forName(annotationName);
            return clazz.isAnnotationPresent((Class) annotationClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查方法是否有指定注解
     */
    private boolean hasMethodAnnotation(Method method, String annotationName) {
        try {
            Class<?> annotationClass = Class.forName(annotationName);
            return method.isAnnotationPresent((Class) annotationClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查类继承关系
     */
    private boolean isAssignableFrom(String actualClassName, String targetClassName) {
        try {
            Class<?> actualClass = Class.forName(actualClassName);
            Class<?> targetClass = Class.forName(targetClassName);
            return targetClass.isAssignableFrom(actualClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void beforeMethod(ExecutionContext context) {
        if (!enabled) {
            return;
        }

        try {
            // 设置基础信息
            context.setStartTime(System.currentTimeMillis());
            context.setThreadName(Thread.currentThread().getName());

            // 阶段3：集成链路跟踪（完全配置驱动）
            TraceManager traceManager = TraceManager.getInstance();
            String nodeType = getNodeTypeFromConfig();
            TraceNode node = traceManager.startNode(nodeType, context.getMethodSignature());

            // 将TraceNode存储到ExecutionContext中
            context.addMetric("traceNode", node);

            // 采集before阶段的指标
            metricCollector.collectBeforeMetrics(context);

            // 总是输出调试信息（用于验证拦截是否工作）
            String traceId = traceManager.getCurrentTraceId();
            System.out.println("🔍 [INTERCEPTED] " + probeConfig.getName() + " -> " +
                context.getMethodSignature() + " [TraceID: " + traceId + "] at " + context.getStartTime());

            if (isVerboseMode()) {
                System.out.println("[DEBUG] " + probeConfig.getName() + " intercepted: " +
                    context.getMethodSignature() + " [TraceID: " + traceId + "] at " + context.getStartTime());
            }

        } catch (Exception e) {
            System.err.println("Error in beforeMethod for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void afterMethod(ExecutionContext context) {
        if (!enabled) {
            return;
        }

        try {
            // 设置结束时间
            context.setEndTime(System.currentTimeMillis());

            // 阶段3：更新链路跟踪节点
            TraceNode node = (TraceNode) context.getMetric("traceNode");
            if (node != null) {
                TraceManager traceManager = TraceManager.getInstance();

                // 设置节点属性
                populateNodeAttributes(node, context);

                // 结束节点
                traceManager.endNode(node);
            }

            // 采集after阶段的指标
            metricCollector.collectAfterMetrics(context);

            if (isVerboseMode()) {
                String traceId = TraceManager.getInstance().getCurrentTraceId();
                System.out.println("[DEBUG] " + probeConfig.getName() + " completed: " +
                    context.getMethodSignature() + " [TraceID: " + traceId + "] in " + context.getExecutionTime() + "ms");
            }

            // 智能输出：如果有trace-flow监听器，使用新机制；否则使用原有机制
            if (TraceResultCollector.getInstance().getListenerCount() > 0) {
                // 有trace-flow命令在监听，使用新的发布机制
                publishTraceResult(context, node);
            } else {
                // 没有trace-flow命令，使用原有的直接输出机制
                outputTraceResult(context);
            }

        } catch (Exception e) {
            System.err.println("Error in afterMethod for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void onException(ExecutionContext context) {
        if (!enabled) {
            return;
        }

        try {
            // 设置结束时间
            context.setEndTime(System.currentTimeMillis());

            // 采集异常指标
            metricCollector.collectExceptionMetrics(context);

            if (isVerboseMode()) {
                System.out.println("[DEBUG] " + probeConfig.getName() + " failed: " + 
                    context.getMethodSignature() + " with exception: " + 
                    context.getException().getMessage());
            }

            // 输出异常结果
            outputExceptionResult(context);

        } catch (Exception e) {
            System.err.println("Error in onException for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    /**
     * 输出跟踪结果（基于配置的输出模板）
     */
    private void outputTraceResult(ExecutionContext context) {
        try {
            String output = formatOutput(context, false);
            System.out.println(output);
        } catch (Exception e) {
            System.err.println("Error formatting output: " + e.getMessage());
        }
    }

    /**
     * 输出异常结果
     */
    private void outputExceptionResult(ExecutionContext context) {
        try {
            String output = formatOutput(context, true);
            System.err.println(output);
        } catch (Exception e) {
            System.err.println("Error formatting exception output: " + e.getMessage());
        }
    }

    /**
     * 格式化输出（基于配置的模板）
     */
    private String formatOutput(ExecutionContext context, boolean isException) {
        StringBuilder output = new StringBuilder();

        // 时间戳
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        output.append("[").append(sdf.format(new java.util.Date())).append("] ");

        // 配置驱动：探针类型
        String probeType = getNodeTypeFromConfig();
        if (isException) {
            output.append("[").append(probeType).append(" ERROR]\n");
        } else {
            output.append("[").append(probeType).append("]\n");
        }

        // 方法信息
        output.append("  Method: ").append(context.getMethodSignature()).append("\n");

        // 执行时间
        output.append("  Execution Time: ").append(context.getExecutionTime()).append("ms\n");

        // 线程信息
        output.append("  Thread: ").append(context.getThreadName()).append("\n");

        // 配置驱动：显示所有采集到的指标
        try {
            formatMetricsFromContext(output, context);
        } catch (Exception e) {
            // 忽略指标提取错误
        }

        // 如果有异常，添加异常信息
        if (isException && context.hasException()) {
            output.append("  Error: ").append(context.getException().getMessage()).append("\n");
        }

        return output.toString();
    }

    /**
     * 配置驱动：格式化所有采集到的指标
     */
    private void formatMetricsFromContext(StringBuilder output, ExecutionContext context) {
        try {
            // 显示所有采集到的指标
            Map<String, Object> allMetrics = context.getAllMetrics();

            for (Map.Entry<String, Object> entry : allMetrics.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // 跳过内部使用的指标
                if ("traceNode".equals(key)) {
                    continue;
                }

                // 格式化显示指标
                if (value != null) {
                    output.append("  ").append(formatMetricName(key)).append(": ");
                    output.append(formatMetricValue(value)).append("\n");
                }
            }

        } catch (Exception e) {
            // 忽略格式化错误
        }
    }

    /**
     * 格式化指标名称（配置驱动）
     */
    private String formatMetricName(String metricName) {
        // 配置驱动：从探针配置中获取指标的显示名称
        for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
            if (metricName.equals(metric.getName())) {
                // 如果配置中有description，使用description作为显示名称
                String description = metric.getDescription();
                if (description != null && !description.isEmpty()) {
                    return description;
                }
            }
        }

        // 如果配置中没有找到，使用原始名称
        return metricName;
    }

    /**
     * 格式化指标值
     */
    private String formatMetricValue(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            return value.toString();
        }
    }

    /**
     * 填充节点属性（完全配置驱动）
     */
    private void populateNodeAttributes(TraceNode node, ExecutionContext context) {
        try {
            // 配置驱动：根据探针配置中的指标定义来提取属性
            populateNodeAttributesFromConfig(node, context, probeConfig.getName());

            // 通用属性
            node.setAttribute("executionTime", context.getExecutionTime());
            node.setAttribute("threadName", context.getThreadName());

        } catch (Exception e) {
            System.err.println("Error populating node attributes: " + e.getMessage());
        }
    }

    /**
     * 根据配置文件提取节点属性（配置驱动）
     */
    private void populateNodeAttributesFromConfig(TraceNode node, ExecutionContext context, String probeName) {
        try {
            // 遍历探针配置中的所有指标
            for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
                String metricName = metric.getName();
                String source = metric.getSource();

                if (source != null && !source.isEmpty()) {
                    // 使用SourceExpressionParser解析source表达式
                    SourceExpressionParser parser = new SourceExpressionParser();
                    Object value = parser.parse(source, context);

                    if (value != null) {
                        node.setAttribute(metricName, value);
                    }
                }
            }

            // 如果配置中有Formula表达式，也进行计算
            for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
                String formula = metric.getFormula();
                if (formula != null && !formula.isEmpty()) {
                    try {
                        FormulaExpressionParser formulaParser = new FormulaExpressionParser();
                        Object calculatedValue = formulaParser.parse(formula, context);
                        if (calculatedValue != null) {
                            node.setAttribute(metric.getName(), calculatedValue);
                        }
                    } catch (Exception e) {
                        // Formula计算失败不影响其他属性
                        System.err.println("Formula calculation failed for " + metric.getName() + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error extracting attributes from config for " + probeName + ": " + e.getMessage());
        }
    }



    /**
     * 从配置中获取节点类型（配置驱动）
     */
    private String getNodeTypeFromConfig() {
        // 配置驱动：从探针配置的output部分获取类型
        if (probeConfig.getOutput() != null && probeConfig.getOutput().getType() != null) {
            return probeConfig.getOutput().getType();
        }

        // 如果没有配置output.type，使用探针名称作为默认值
        String probeName = probeConfig.getName();
        if (probeName != null) {
            // 移除"探针"后缀，转换为大写
            return probeName;
        }

        // 最后的默认值
        return "UNKNOWN";
    }

    /**
     * 发布跟踪结果到全局收集器
     */
    private void publishTraceResult(ExecutionContext context, TraceNode node) {
        try {
            // 创建跟踪结果
            TraceResultListener.TraceResult result = new TraceResultListener.TraceResult(
                getNodeTypeFromConfig(),
                context.getMethodSignature()
            );

            // 设置基本信息
            result.setExecutionTime(context.getExecutionTime());
            result.setThreadName(context.getThreadName());
            result.setAttributes(context.getAllMetrics());

            if (context.hasException()) {
                result.setException(context.getException());
            }

            // 设置链路跟踪上下文
            String traceId = TraceManager.getInstance().getCurrentTraceId();
            if (traceId != null) {
                TraceManager.TraceContext traceContext = TraceManager.getInstance().getTraceContext(traceId);
                result.setTraceContext(traceContext);
            }

            // 发布到全局收集器
            TraceResultCollector.getInstance().publishResult(result);

        } catch (Exception e) {
            System.err.println("Error publishing trace result: " + e.getMessage());
        }
    }

    /**
     * 检查是否为详细模式
     */
    private boolean isVerboseMode() {
        // 这里可以从全局配置中获取verbose设置
        return false; // 暂时返回false，后续可以改进
    }

    @Override
    public boolean isEnabled() {
        return enabled && probeConfig.isEnabled();
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public ProbeConfig getProbeConfig() {
        return probeConfig;
    }
}
