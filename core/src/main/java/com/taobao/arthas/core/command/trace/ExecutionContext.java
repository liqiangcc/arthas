package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 执行上下文 - 存储方法执行过程中的所有相关信息
 */
public class ExecutionContext {
    
    private Object targetObject;
    private Object[] args;
    private Method method;
    private Object returnValue;
    private Throwable exception;
    private long startTime;
    private long endTime;
    private String threadName;
    private String methodSignature;
    private Map<String, Object> metrics = new HashMap<>();

    // 构造函数
    public ExecutionContext() {
        this.threadName = Thread.currentThread().getName();
        this.startTime = System.currentTimeMillis();
    }

    public ExecutionContext(Object targetObject, Object[] args, Method method) {
        this();
        this.targetObject = targetObject;
        this.args = args;
        this.method = method;
    }

    /**
     * 计算执行时间
     */
    public long getExecutionTime() {
        if (endTime > 0 && startTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }

    /**
     * 标记方法执行结束
     */
    public void markEnd() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 标记方法执行结束并设置返回值
     */
    public void markEnd(Object returnValue) {
        this.endTime = System.currentTimeMillis();
        this.returnValue = returnValue;
    }

    /**
     * 标记方法执行异常
     */
    public void markException(Throwable exception) {
        this.endTime = System.currentTimeMillis();
        this.exception = exception;
    }

    /**
     * 检查是否有异常
     */
    public boolean hasException() {
        return exception != null;
    }

    /**
     * 检查是否已完成（正常或异常）
     */
    public boolean isCompleted() {
        return endTime > 0;
    }

    /**
     * 获取方法签名字符串
     */
    public String getMethodSignature() {
        if (methodSignature != null) {
            return methodSignature;
        }
        if (method == null) {
            return "unknown";
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    /**
     * 获取参数数量
     */
    public int getArgCount() {
        return args != null ? args.length : 0;
    }

    /**
     * 安全获取参数
     */
    public Object getArg(int index) {
        if (args == null || index < 0 || index >= args.length) {
            return null;
        }
        return args[index];
    }

    // Getters and Setters
    public Object getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "method=" + getMethodSignature() +
                ", executionTime=" + getExecutionTime() +
                ", threadName='" + threadName + '\'' +
                ", hasException=" + hasException() +
                ", completed=" + isCompleted() +
                '}';
    }

    /**
     * 创建用于测试的模拟上下文
     */
    public static ExecutionContext createMockContext() {
        ExecutionContext context = new ExecutionContext();
        context.setStartTime(1000L);
        context.setEndTime(2000L);
        context.setThreadName("test-thread");
        return context;
    }

    // 新增的getter和setter方法
    public Object getTarget() {
        return targetObject;
    }

    public void setTarget(Object target) {
        this.targetObject = target;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    // 指标管理方法
    public void addMetric(String name, Object value) {
        metrics.put(name, value);
    }

    public Object getMetric(String name) {
        return metrics.get(name);
    }

    public Map<String, Object> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * 创建带有指定时间的模拟上下文
     */
    public static ExecutionContext createMockContext(long startTime, long endTime) {
        ExecutionContext context = new ExecutionContext();
        context.setStartTime(startTime);
        context.setEndTime(endTime);
        context.setThreadName("test-thread");
        return context;
    }
}
