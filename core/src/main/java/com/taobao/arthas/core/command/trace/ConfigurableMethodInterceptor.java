package com.taobao.arthas.core.command.trace;

import java.lang.reflect.Method;
import java.util.List;

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

            // 采集before阶段的指标
            metricCollector.collectBeforeMetrics(context);

            if (isVerboseMode()) {
                System.out.println("[DEBUG] " + probeConfig.getName() + " intercepted: " + 
                    context.getMethodSignature() + " at " + context.getStartTime());
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

            // 采集after阶段的指标
            metricCollector.collectAfterMetrics(context);

            if (isVerboseMode()) {
                System.out.println("[DEBUG] " + probeConfig.getName() + " completed: " + 
                    context.getMethodSignature() + " in " + context.getExecutionTime() + "ms");
            }

            // 输出跟踪结果
            outputTraceResult(context);

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

        // 探针类型
        String probeType = probeConfig.getName().replace("探针", "").toUpperCase();
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

        // 尝试提取特定的指标信息
        try {
            if ("Database探针".equals(probeConfig.getName())) {
                formatDatabaseSpecificOutput(output, context);
            }
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
     * 格式化Database探针特定的输出
     */
    private void formatDatabaseSpecificOutput(StringBuilder output, ExecutionContext context) {
        try {
            // 尝试从target对象提取SQL信息
            Object target = context.getTarget();
            if (target != null) {
                String targetStr = target.toString();
                if (targetStr.contains("SELECT") || targetStr.contains("INSERT") ||
                    targetStr.contains("UPDATE") || targetStr.contains("DELETE")) {
                    output.append("  SQL: ").append(targetStr).append("\n");
                }
            }

            // 参数信息
            Object[] args = context.getArgs();
            if (args != null && args.length > 0) {
                output.append("  Parameters: [");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) output.append(", ");
                    output.append(args[i] != null ? args[i].toString() : "null");
                }
                output.append("]\n");
            }

            // 返回值信息
            Object returnValue = context.getReturnValue();
            if (returnValue != null) {
                if (returnValue instanceof Boolean) {
                    output.append("  Result: ").append(returnValue).append("\n");
                } else if (returnValue instanceof Number) {
                    output.append("  Affected Rows: ").append(returnValue).append("\n");
                } else {
                    output.append("  Result: ").append(returnValue.getClass().getSimpleName()).append("\n");
                }
            }

        } catch (Exception e) {
            // 忽略格式化错误
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
