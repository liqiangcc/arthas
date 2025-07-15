package com.taobao.arthas.core.command.trace;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 跟踪结果收集器 - 全局单例，收集所有拦截器的结果
 */
public class TraceResultCollector {
    
    private static final TraceResultCollector INSTANCE = new TraceResultCollector();
    private final List<TraceResultListener> listeners = new CopyOnWriteArrayList<>();
    
    private TraceResultCollector() {}
    
    public static TraceResultCollector getInstance() {
        return INSTANCE;
    }
    
    /**
     * 添加监听器
     */
    public void addListener(TraceResultListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(TraceResultListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 发布跟踪结果到所有监听器
     */
    public void publishResult(TraceResultListener.TraceResult result) {
        for (TraceResultListener listener : listeners) {
            try {
                listener.onTraceResult(result);
            } catch (Exception e) {
                // 忽略监听器异常，不影响其他监听器
                System.err.println("Error in trace result listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取监听器数量
     */
    public int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * 清理所有监听器
     */
    public void clearListeners() {
        listeners.clear();
    }
}
