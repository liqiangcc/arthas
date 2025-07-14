package com.taobao.arthas.core.command.trace;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 链路跟踪管理器 - 阶段1基础版本
 */
public class TraceManager {
    
    private final AtomicLong traceCounter = new AtomicLong(0);
    private final ConcurrentHashMap<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    
    /**
     * 开始新的跟踪
     */
    public String startTrace() {
        String traceId = generateTraceId();
        TraceContext context = new TraceContext(traceId);
        activeTraces.put(traceId, context);
        return traceId;
    }

    /**
     * 结束跟踪
     */
    public TraceContext endTrace(String traceId) {
        TraceContext context = activeTraces.remove(traceId);
        if (context != null) {
            context.markEnd();
        }
        return context;
    }

    /**
     * 获取当前活跃的跟踪数量
     */
    public int getActiveTraceCount() {
        return activeTraces.size();
    }

    /**
     * 生成跟踪ID
     */
    private String generateTraceId() {
        return "trace-" + traceCounter.incrementAndGet() + "-" + 
               UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 清理所有跟踪（主要用于测试）
     */
    public void clear() {
        activeTraces.clear();
        traceCounter.set(0);
    }

    /**
     * 跟踪上下文
     */
    public static class TraceContext {
        private final String traceId;
        private final long startTime;
        private long endTime;
        private boolean completed;

        public TraceContext(String traceId) {
            this.traceId = traceId;
            this.startTime = System.currentTimeMillis();
        }

        public void markEnd() {
            this.endTime = System.currentTimeMillis();
            this.completed = true;
        }

        public long getDuration() {
            if (completed) {
                return endTime - startTime;
            }
            return System.currentTimeMillis() - startTime;
        }

        // Getters
        public String getTraceId() { return traceId; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public boolean isCompleted() { return completed; }

        @Override
        public String toString() {
            return "TraceContext{" +
                    "traceId='" + traceId + '\'' +
                    ", duration=" + getDuration() + "ms" +
                    ", completed=" + completed +
                    '}';
        }
    }
}
