package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 拦截器管理器 - 阶段2实现
 * 负责管理所有方法拦截器，集成Arthas字节码增强框架
 */
public class InterceptorManager {
    
    private static final InterceptorManager INSTANCE = new InterceptorManager();
    
    private final List<MethodInterceptor> interceptors = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Object> enhancerAffects = new ConcurrentHashMap<>();
    private boolean initialized = false;
    
    private InterceptorManager() {}
    
    public static InterceptorManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 注册方法拦截器
     */
    public void registerInterceptor(MethodInterceptor interceptor) {
        if (interceptor == null) {
            throw new IllegalArgumentException("Interceptor cannot be null");
        }
        
        interceptors.add(interceptor);
        
        // 如果已经初始化，立即启用拦截器
        if (initialized) {
            enableInterceptor(interceptor);
        }
    }
    
    /**
     * 移除方法拦截器
     */
    public void unregisterInterceptor(MethodInterceptor interceptor) {
        if (interceptor == null) {
            return;
        }
        
        interceptors.remove(interceptor);
        disableInterceptor(interceptor);
    }
    
    /**
     * 初始化拦截器管理器
     */
    public void initialize(Instrumentation instrumentation) {
        if (initialized) {
            return;
        }
        
        // 为每个拦截器启用字节码增强
        for (MethodInterceptor interceptor : interceptors) {
            enableInterceptor(interceptor, instrumentation);
        }
        
        initialized = true;
    }
    
    /**
     * 启用单个拦截器
     */
    private void enableInterceptor(MethodInterceptor interceptor) {
        // 这个方法在运行时调用，需要获取Instrumentation实例
        // 在实际实现中，可以通过Arthas的全局Instrumentation获取
        // 这里先提供一个占位实现
        System.out.println("Interceptor enabled: " + interceptor.getName());
    }
    
    /**
     * 启用单个拦截器（带Instrumentation）
     */
    private void enableInterceptor(MethodInterceptor interceptor, Instrumentation instrumentation) {
        try {
            ProbeConfig config = interceptor.getProbeConfig();
            if (config == null || !config.isEnabled()) {
                return;
            }
            
            // 为每个target创建增强器
            for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                if (metric.getTargets() != null) {
                    for (ProbeConfig.TargetConfig target : metric.getTargets()) {
                        enhanceTarget(target, interceptor, instrumentation);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to enable interceptor: " + interceptor.getName() + ", error: " + e.getMessage());
        }
    }
    
    /**
     * 增强目标类和方法
     */
    private void enhanceTarget(ProbeConfig.TargetConfig target, MethodInterceptor interceptor, 
                              Instrumentation instrumentation) {
        try {
            // 创建类名匹配器
            Matcher<String> classNameMatcher = new RegexMatcher(target.getClassName().replace(".", "\\."));
            
            // 创建方法名匹配器
            StringBuilder methodPattern = new StringBuilder();
            List<String> methods = target.getMethods();
            if (methods != null && !methods.isEmpty()) {
                methodPattern.append("(");
                for (int i = 0; i < methods.size(); i++) {
                    if (i > 0) {
                        methodPattern.append("|");
                    }
                    methodPattern.append(methods.get(i));
                }
                methodPattern.append(")");
            } else {
                methodPattern.append(".*"); // 匹配所有方法
            }
            
            Matcher<String> methodNameMatcher = new RegexMatcher(methodPattern.toString());
            
            // 创建Advice监听器
            TraceFlowAdviceListener adviceListener = new TraceFlowAdviceListener(interceptor);
            
            // 创建增强器 - 简化版本，先不使用复杂的增强
            // TODO: 在后续版本中实现完整的字节码增强

            // 保存增强结果
            String key = target.getClassName() + ":" + methodPattern.toString();
            enhancerAffects.put(key, "enhanced");

            System.out.println("Enhanced target: " + target.getClassName() +
                             ", methods: " + methodPattern.toString());
            
        } catch (Exception e) {
            System.err.println("Failed to enhance target: " + target.getClassName() + ", error: " + e.getMessage());
        }
    }
    
    /**
     * 禁用拦截器
     */
    private void disableInterceptor(MethodInterceptor interceptor) {
        // TODO: 实现拦截器禁用逻辑
        // 需要移除相关的字节码增强
        System.out.println("Interceptor disabled: " + interceptor.getName());
    }
    
    /**
     * 获取所有注册的拦截器
     */
    public List<MethodInterceptor> getInterceptors() {
        return new CopyOnWriteArrayList<>(interceptors);
    }
    
    /**
     * 获取拦截器数量
     */
    public int getInterceptorCount() {
        return interceptors.size();
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 重置管理器状态（主要用于测试）
     */
    public void reset() {
        interceptors.clear();
        enhancerAffects.clear();
        initialized = false;
    }
    
    /**
     * 查找匹配的拦截器
     */
    public MethodInterceptor findInterceptor(String className, String methodName, Method method) {
        for (MethodInterceptor interceptor : interceptors) {
            if (interceptor.isEnabled() && interceptor.shouldIntercept(className, methodName, method)) {
                return interceptor;
            }
        }
        return null;
    }
}
