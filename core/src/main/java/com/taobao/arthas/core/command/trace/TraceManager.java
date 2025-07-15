package com.taobao.arthas.core.command.trace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 链路跟踪管理器 - 阶段3增强版本
 * 支持多探针协同、Trace ID传递、链路关联
 */
public class TraceManager {

    private static final TraceManager INSTANCE = new TraceManager();
    private final AtomicLong traceCounter = new AtomicLong(0);
    private final ConcurrentHashMap<String, TraceContext> activeTraces = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentTraceId = new ThreadLocal<>();
    private final ThreadLocal<Stack<TraceNode>> nodeStack = new ThreadLocal<>();
    private final ThreadLocal<String> entryMethodSignature = new ThreadLocal<>();  // 记录第一个入口方法签名
    private final ThreadLocal<String> entryClassName = new ThreadLocal<>();      // 记录第一个入口方法的类名
    private final ThreadLocal<String> entryMethodName = new ThreadLocal<>();     // 记录第一个入口方法的方法名
    
    private TraceManager() {}

    public static TraceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 开始新的跟踪（请求入口）
     */
    public String startTrace(String className, String methodName) {
        // 如果当前线程已经有Trace ID，直接返回（避免重复创建）
        String existingTraceId = currentTraceId.get();
        if (existingTraceId != null) {
            return existingTraceId;
        }

        String traceId = generateTraceId();
        TraceContext context = new TraceContext(traceId);
        activeTraces.put(traceId, context);

        // 设置当前线程的Trace ID
        currentTraceId.set(traceId);

        // 初始化节点栈
        nodeStack.set(new Stack<>());

        // 记录第一个入口方法的详细信息，用于匹配对应的出口方法
        String methodSignature = className + "." + methodName;
        entryMethodSignature.set(methodSignature);
        entryClassName.set(className);
        entryMethodName.set(methodName);

        return traceId;
    }

    /**
     * 兼容旧版本的startTrace方法
     */
    public String startTrace() {
        return startTrace("unknown", "unknown");
    }

    /**
     * 结束跟踪（请求出口）
     */
    public void endTrace() {
        String traceId = currentTraceId.get();
        if (traceId != null) {
            // 清理当前线程的跟踪上下文
            currentTraceId.remove();
            nodeStack.remove();
            entryMethodSignature.remove();
            entryClassName.remove();
            entryMethodName.remove();

            // 可选：从活跃跟踪中移除（如果需要释放内存）
            // activeTraces.remove(traceId);
        }
    }

    /**
     * 获取当前线程的第一个入口方法签名
     */
    public String getEntryMethodSignature() {
        return entryMethodSignature.get();
    }

    /**
     * 获取当前线程的第一个入口方法类名
     */
    public String getEntryClassName() {
        return entryClassName.get();
    }

    /**
     * 获取当前线程的第一个入口方法名
     */
    public String getEntryMethodName() {
        return entryMethodName.get();
    }



    /**
     * 获取当前线程的Trace ID
     */
    public String getCurrentTraceId() {
        return currentTraceId.get();
    }

    /**
     * 获取指定的跟踪上下文
     */
    public TraceContext getTraceContext(String traceId) {
        return activeTraces.get(traceId);
    }

    /**
     * 开始一个新的跟踪节点
     */
    public TraceNode startNode(String nodeType, String methodSignature) {
        String traceId = getCurrentTraceId();
        if (traceId == null) {
            // 如果没有活跃的trace，创建一个新的
            traceId = startTrace();
        }

        TraceContext context = activeTraces.get(traceId);
        if (context == null) {
            return null;
        }

        TraceNode node = new TraceNode(nodeType, methodSignature);
        node.setStartTime(System.currentTimeMillis());

        // 处理父子关系
        Stack<TraceNode> stack = nodeStack.get();
        if (stack != null && !stack.isEmpty()) {
            TraceNode parent = stack.peek();
            parent.addChild(node);
            node.setParent(parent);
        } else {
            // 根节点
            context.setRootNode(node);
        }

        if (stack != null) {
            stack.push(node);
        }

        return node;
    }

    /**
     * 结束当前跟踪节点
     */
    public void endNode(TraceNode node) {
        if (node == null) {
            return;
        }

        node.setEndTime(System.currentTimeMillis());

        Stack<TraceNode> stack = nodeStack.get();
        if (stack != null && !stack.isEmpty() && stack.peek() == node) {
            stack.pop();
        }
    }

    /**
     * 结束跟踪
     */
    public TraceContext endTrace(String traceId) {
        TraceContext context = activeTraces.remove(traceId);
        if (context != null) {
            context.markEnd();
        }

        // 清理线程本地变量
        if (traceId.equals(getCurrentTraceId())) {
            currentTraceId.remove();
            nodeStack.remove();
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
     * 跟踪上下文 - 阶段3增强版本
     */
    public static class TraceContext {
        private final String traceId;
        private final long startTime;
        private long endTime;
        private boolean completed;
        private TraceNode rootNode;
        private Map<String, Object> attributes;

        public TraceContext(String traceId) {
            this.traceId = traceId;
            this.startTime = System.currentTimeMillis();
            this.attributes = new HashMap<>();
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

        /**
         * 获取树状输出
         */
        public String getTreeOutput() {
            if (rootNode == null) {
                return "No trace data available";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Trace ID: ").append(traceId).append("\n");
            sb.append("Total Duration: ").append(getDuration()).append("ms\n");
            sb.append("Nodes: ").append(getTotalNodes()).append("\n");
            sb.append("Call Tree:\n");
            sb.append(rootNode.toTreeString());

            return sb.toString();
        }

        /**
         * 获取总节点数
         */
        public int getTotalNodes() {
            if (rootNode == null) {
                return 0;
            }
            return 1 + rootNode.getTotalDescendants();
        }

        // Getters and Setters
        public String getTraceId() { return traceId; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public boolean isCompleted() { return completed; }

        public TraceNode getRootNode() { return rootNode; }
        public void setRootNode(TraceNode rootNode) { this.rootNode = rootNode; }

        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttribute(String key, Object value) { attributes.put(key, value); }
        public Object getAttribute(String key) { return attributes.get(key); }

        @Override
        public String toString() {
            return "TraceContext{" +
                    "traceId='" + traceId + '\'' +
                    ", duration=" + getDuration() + "ms" +
                    ", nodes=" + getTotalNodes() +
                    ", completed=" + completed +
                    '}';
        }
    }
}
