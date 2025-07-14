package com.taobao.arthas.core.command.trace;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置驱动的指标采集器
 * 完全基于JSON配置文件，无任何硬编码逻辑
 */
public class ConfigurableMetricCollector implements MetricCollector {

    private final ProbeConfig probeConfig;
    private final SourceExpressionParser sourceParser;
    private final FormulaExpressionParser formulaParser;
    private boolean enabled = true;

    public ConfigurableMetricCollector(ProbeConfig probeConfig) {
        this.probeConfig = probeConfig;
        this.sourceParser = new SourceExpressionParser();
        this.formulaParser = new FormulaExpressionParser();
    }

    @Override
    public void collectBeforeMetrics(ExecutionContext context) {
        if (!enabled) {
            return;
        }

        try {
            // 遍历配置中的所有指标
            for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
                // 只采集before阶段的指标
                if ("before".equals(metric.getCapturePoint())) {
                    collectMetric(metric, context);
                }
            }
        } catch (Exception e) {
            System.err.println("Error collecting before metrics for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void collectAfterMetrics(ExecutionContext context) {
        if (!enabled) {
            return;
        }

        try {
            // 遍历配置中的所有指标
            for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
                // 只采集after阶段的指标
                if ("after".equals(metric.getCapturePoint())) {
                    collectMetric(metric, context);
                }
            }

            // 计算所有formula指标
            calculateFormulaMetrics(context);

        } catch (Exception e) {
            System.err.println("Error collecting after metrics for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void collectExceptionMetrics(ExecutionContext context) {
        if (!enabled) {
            return;
        }

        try {
            // 添加异常相关的通用指标
            context.addMetric("hasException", true);
            if (context.getException() != null) {
                context.addMetric("exceptionType", context.getException().getClass().getSimpleName());
                context.addMetric("exceptionMessage", context.getException().getMessage());
            }

            // 仍然计算formula指标（如执行时间等）
            calculateFormulaMetrics(context);

        } catch (Exception e) {
            System.err.println("Error collecting exception metrics for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    /**
     * 采集单个指标（完全基于配置）
     */
    private void collectMetric(ProbeConfig.MetricConfig metric, ExecutionContext context) {
        try {
            if (metric.getSource() != null && !metric.getSource().trim().isEmpty()) {
                // 使用source表达式采集指标
                Object value = sourceParser.parse(metric.getSource(), context);
                context.addMetric(metric.getName(), value);
                
                if (isDebugMode()) {
                    System.out.println("[DEBUG] Collected metric " + metric.getName() + 
                        " = " + value + " using source: " + metric.getSource());
                }
            }
        } catch (Exception e) {
            System.err.println("Error collecting metric " + metric.getName() + 
                " for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    /**
     * 计算所有formula指标（完全基于配置）
     */
    private void calculateFormulaMetrics(ExecutionContext context) {
        try {
            // 遍历配置中的所有指标
            for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
                if (metric.getFormula() != null && !metric.getFormula().trim().isEmpty()) {
                    // 使用formula表达式计算指标
                    Object value = formulaParser.parse(metric.getFormula(), context);
                    context.addMetric(metric.getName(), value);
                    
                    if (isDebugMode()) {
                        System.out.println("[DEBUG] Calculated metric " + metric.getName() + 
                            " = " + value + " using formula: " + metric.getFormula());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating formula metrics for " + probeConfig.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getCollectedMetrics(ExecutionContext context) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 获取配置中定义的所有指标
        for (ProbeConfig.MetricConfig metric : probeConfig.getMetrics()) {
            Object value = context.getMetric(metric.getName());
            if (value != null) {
                metrics.put(metric.getName(), value);
            }
        }

        // 添加一些通用的有用指标
        metrics.put("probeName", probeConfig.getName());
        metrics.put("methodSignature", context.getMethodSignature());
        metrics.put("completed", context.isCompleted());
        metrics.put("hasException", context.hasException());

        return metrics;
    }

    @Override
    public void clearMetrics(ExecutionContext context) {
        // 清理指标数据
        // 当前ExecutionContext没有提供清理方法，可以在后续扩展
        if (isDebugMode()) {
            System.out.println("[DEBUG] Clearing metrics for " + probeConfig.getName());
        }
    }

    @Override
    public String getName() {
        return probeConfig.getName() + "MetricCollector";
    }

    @Override
    public boolean isEnabled() {
        return enabled && probeConfig.isEnabled();
    }

    /**
     * 启用采集器
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * 禁用采集器
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * 检查是否为调试模式
     */
    private boolean isDebugMode() {
        // 这里可以从全局配置中获取debug设置
        return false; // 暂时返回false，后续可以改进
    }

    /**
     * 获取探针配置
     */
    public ProbeConfig getProbeConfig() {
        return probeConfig;
    }
}
