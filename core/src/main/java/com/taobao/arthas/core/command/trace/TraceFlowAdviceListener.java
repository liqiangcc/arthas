package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.advisor.AdviceListener;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TraceFlow命令的Advice监听器
 * 连接Arthas增强框架和trace-flow拦截器
 */
public class TraceFlowAdviceListener implements AdviceListener {

    private final MethodInterceptor interceptor;
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);
    private final long id = ID_GENERATOR.incrementAndGet();

    public TraceFlowAdviceListener(MethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public void create() {
        // 监听器创建时的初始化逻辑
    }

    @Override
    public void destroy() {
        // 监听器销毁时的清理逻辑
    }

    @Override
    public void before(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args) throws Throwable {
        if (!interceptor.isEnabled()) {
            return;
        }
        
        try {
            // 创建执行上下文
            ExecutionContext context = new ExecutionContext();
            context.setArgs(args);
            context.setMethodSignature(clazz.getName() + "." + methodName);

            // 存储上下文到线程本地变量
            ExecutionContextHolder.setContext(context);

            // 调用拦截器的before方法
            interceptor.beforeMethod(context);

        } catch (Throwable e) {
            System.err.println("Error in TraceFlowAdviceListener.before: " + e.getMessage());
            // 不抛出异常，避免影响原方法执行
        }
    }
    
    @Override
    public void afterReturning(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args, Object returnObject) throws Throwable {
        if (!interceptor.isEnabled()) {
            return;
        }

        try {
            // 获取执行上下文
            ExecutionContext context = ExecutionContextHolder.getContext();
            if (context != null) {
                context.setReturnValue(returnObject);

                // 调用拦截器的after方法
                interceptor.afterMethod(context);
            }

        } catch (Throwable e) {
            System.err.println("Error in TraceFlowAdviceListener.afterReturning: " + e.getMessage());
        } finally {
            // 清理线程本地变量
            ExecutionContextHolder.clearContext();
        }
    }

    @Override
    public void afterThrowing(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args, Throwable throwable) throws Throwable {
        if (!interceptor.isEnabled()) {
            return;
        }
        
        try {
            // 获取执行上下文
            ExecutionContext context = ExecutionContextHolder.getContext();
            if (context != null) {
                context.setException(throwable);
                
                // 调用拦截器的异常处理方法
                interceptor.onException(context);
            }
            
        } catch (Throwable e) {
            System.err.println("Error in TraceFlowAdviceListener.afterThrowing: " + e.getMessage());
        } finally {
            // 清理线程本地变量
            ExecutionContextHolder.clearContext();
        }
    }
    
    /**
     * 线程本地执行上下文持有者
     */
    private static class ExecutionContextHolder {
        private static final ThreadLocal<ExecutionContext> CONTEXT_HOLDER = new ThreadLocal<>();
        
        public static void setContext(ExecutionContext context) {
            CONTEXT_HOLDER.set(context);
        }
        
        public static ExecutionContext getContext() {
            return CONTEXT_HOLDER.get();
        }
        
        public static void clearContext() {
            CONTEXT_HOLDER.remove();
        }
    }
}
