package com.taobao.arthas.core.command.trace;

import java.util.Map;

/**
 * 指标采集器接口
 * 定义指标采集的核心契约
 */
public interface MetricCollector {

    /**
     * 在方法执行前采集指标
     * 
     * @param context 执行上下文
     */
    void collectBeforeMetrics(ExecutionContext context);

    /**
     * 在方法执行后采集指标
     * 
     * @param context 执行上下文
     */
    void collectAfterMetrics(ExecutionContext context);

    /**
     * 在方法执行异常时采集指标
     * 
     * @param context 执行上下文
     */
    void collectExceptionMetrics(ExecutionContext context);

    /**
     * 获取采集到的所有指标
     * 
     * @param context 执行上下文
     * @return 指标映射表
     */
    Map<String, Object> getCollectedMetrics(ExecutionContext context);

    /**
     * 清理指标数据
     * 
     * @param context 执行上下文
     */
    void clearMetrics(ExecutionContext context);

    /**
     * 获取采集器名称
     */
    String getName();

    /**
     * 检查采集器是否启用
     */
    boolean isEnabled();
}
