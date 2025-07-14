package com.taobao.arthas.core.command.trace;

import java.lang.reflect.Method;

/**
 * 方法拦截器接口
 * 定义方法拦截的核心契约
 */
public interface MethodInterceptor {

    /**
     * 获取拦截器名称
     */
    String getName();

    /**
     * 检查是否应该拦截指定的方法
     * 
     * @param className 类名
     * @param methodName 方法名
     * @param method 方法对象
     * @return 是否拦截
     */
    boolean shouldIntercept(String className, String methodName, Method method);

    /**
     * 方法执行前拦截
     * 
     * @param context 执行上下文
     */
    void beforeMethod(ExecutionContext context);

    /**
     * 方法执行后拦截（正常返回）
     * 
     * @param context 执行上下文
     */
    void afterMethod(ExecutionContext context);

    /**
     * 方法执行异常拦截
     * 
     * @param context 执行上下文
     */
    void onException(ExecutionContext context);

    /**
     * 检查拦截器是否启用
     */
    boolean isEnabled();

    /**
     * 启用拦截器
     */
    void enable();

    /**
     * 禁用拦截器
     */
    void disable();

    /**
     * 获取拦截器配置
     */
    ProbeConfig getProbeConfig();
}
