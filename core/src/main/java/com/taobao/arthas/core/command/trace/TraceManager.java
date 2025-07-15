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
    private final ThreadLocal<Stack<TraceNode>> nodeStack = new ThreadLocal<>();     // 记录第一个入口方法的方法名

    private TraceManager() {}

    public static TraceManager getInstance() {
        return INSTANCE;
    }

    /**
     * 开始新的跟踪（请求入口）
     */
    public String startTrace() {
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
        return traceId;
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
            // 可选：从活跃跟踪中移除（如果需要释放内存）
             activeTraces.remove(traceId);
        }
    }

    /**
     * 获取指定trace ID的上下文
     */
    public TraceContext getTraceContext(String traceId) {
        return activeTraces.get(traceId);
    }



    /**
     * 获取当前线程的Trace ID
     */
    public String getCurrentTraceId() {
        return currentTraceId.get();
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
        node.setThreadName(Thread.currentThread().getName());
        // 配置驱动：记录方法调用计数
        context.incrementMethodCallCount(methodSignature);

        // 处理父子关系和调用深度
        Stack<TraceNode> stack = nodeStack.get();
        if (stack != null && !stack.isEmpty()) {
            TraceNode parent = stack.peek();
            parent.addChild(node);
            node.setParent(parent);
            node.setDepth(parent.getDepth() + 1);  // 设置调用深度
        } else {
            // 根节点
            context.addRootNode(node);
            node.setDepth(0);  // 根节点深度为0
        }

        if (stack != null) {
            stack.push(node);
        }

        return node;
    }

    /**
     * 结束当前跟踪节点
     */
    public TraceNode finishedNode() {
        Stack<TraceNode> stack = nodeStack.get();
        if (stack != null && !stack.isEmpty()) {
            TraceNode traceNode = stack.pop();
            traceNode.setEndTime(System.currentTimeMillis());
            return traceNode;
        }
        return null;
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
        private List<TraceNode> rootNodes = new ArrayList<>();  // 支持多个根节点
        private Map<String, Object> attributes;
        private Map<String, Integer> methodCallCounts = new HashMap<>();  // 方法调用计数
        private Map<String, Integer> completedMethodCalls = new HashMap<>();  // 已完成的方法调用计数

        public TraceContext(String traceId) {
            this.traceId = traceId;
            this.startTime = System.currentTimeMillis();
            this.attributes = new HashMap<>();
        }

        public void markEnd() {
            this.endTime = System.currentTimeMillis();
            this.completed = true;
        }

        /**
         * 增加方法调用计数
         */
        public void incrementMethodCallCount(String methodSignature) {
            Integer count = methodCallCounts.getOrDefault(methodSignature, 0);
            methodCallCounts.put(methodSignature, count + 1);
        }

        /**
         * 增加已完成的方法调用计数
         */
        public void incrementCompletedMethodCall(String methodSignature) {
            Integer count = completedMethodCalls.getOrDefault(methodSignature, 0);
            completedMethodCalls.put(methodSignature, count + 1);
        }

        /**
         * 检查方法调用是否全部完成
         */
        public boolean areAllMethodCallsCompleted(String methodSignature) {
            Integer totalCalls = methodCallCounts.getOrDefault(methodSignature, 0);
            Integer completedCalls = completedMethodCalls.getOrDefault(methodSignature, 0);
            return totalCalls > 0 && totalCalls.equals(completedCalls);
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
        public void setRootNode(TraceNode rootNode) {
            this.rootNode = rootNode;
            if (rootNode != null && !rootNodes.contains(rootNode)) {
                rootNodes.add(rootNode);
            }
        }

        public List<TraceNode> getRootNodes() { return rootNodes; }
        public void addRootNode(TraceNode rootNode) {
            if (rootNode != null && !rootNodes.contains(rootNode)) {
                rootNodes.add(rootNode);
                if (this.rootNode == null) {
                    this.rootNode = rootNode;  // 第一个根节点作为主根节点
                }
            }
        }

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
