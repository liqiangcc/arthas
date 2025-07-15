package com.taobao.arthas.core.command.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 链路跟踪节点 - 表示调用链中的一个节点
 */
public class TraceNode {
    
    private String nodeType;           // 节点类型：HTTP, DATABASE, FILE_OPERATION等
    private String methodSignature;    // 方法签名
    private long startTime;           // 开始时间
    private long endTime;             // 结束时间
    private String threadName;        // 线程名称
    private TraceNode parent;         // 父节点
    private List<TraceNode> children; // 子节点列表
    private Map<String, Object> attributes; // 节点属性
    private Throwable exception;      // 异常信息
    private Object[] args;            // 方法参数
    private int depth = 0;            // 调用深度
    
    public TraceNode(String nodeType, String methodSignature) {
        this.nodeType = nodeType;
        this.methodSignature = methodSignature;
        this.threadName = Thread.currentThread().getName();
        this.children = new ArrayList<>();
        this.attributes = new HashMap<>();
    }
    
    /**
     * 添加子节点
     */
    public void addChild(TraceNode child) {
        children.add(child);
    }
    
    /**
     * 获取执行时间
     */
    public long getExecutionTime() {
        if (endTime > 0 && startTime > 0) {
            return endTime - startTime;
        }
        return 0;
    }
    
    /**
     * 设置属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * 检查是否有异常
     */
    public boolean hasException() {
        return exception != null;
    }
    
    /**
     * 获取节点深度
     */
    public int getDepth() {
        int depth = 0;
        TraceNode current = parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }
    
    /**
     * 是否为根节点
     */
    public boolean isRoot() {
        return parent == null;
    }
    
    /**
     * 是否为叶子节点
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
    
    /**
     * 获取所有后代节点数量
     */
    public int getTotalDescendants() {
        int count = children.size();
        for (TraceNode child : children) {
            count += child.getTotalDescendants();
        }
        return count;
    }
    
    /**
     * 格式化为树状字符串
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, "", true);
        return sb.toString();
    }
    
    private void buildTreeString(StringBuilder sb, String prefix, boolean isLast) {
        // 当前节点
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(String.format("[%s] %s (%dms)",
            nodeType, methodSignature, getExecutionTime()));

        if (hasException()) {
            sb.append(" [ERROR: ").append(exception.getMessage()).append("]");
        }

        // 配置驱动：显示所有重要属性
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 显示重要的属性（可配置）
            if (isImportantAttribute(key) && value != null) {
                sb.append(" ").append(formatAttributeForDisplay(key, value));
            }
        }

        sb.append("\n");

        // 子节点
        for (int i = 0; i < children.size(); i++) {
            boolean childIsLast = (i == children.size() - 1);
            String childPrefix = prefix + (isLast ? "    " : "│   ");
            children.get(i).buildTreeString(sb, childPrefix, childIsLast);
        }
    }

    /**
     * 判断是否为重要属性（配置驱动）
     */
    private boolean isImportantAttribute(String key) {
        // 配置驱动：所有非内部属性都是重要的
        // 排除内部使用的属性
        return !"executionTime".equals(key) &&
               !"threadName".equals(key) &&
               !"startTime".equals(key) &&
               !"endTime".equals(key);
    }

    /**
     * 格式化属性显示（配置驱动）
     */
    private String formatAttributeForDisplay(String key, Object value) {
        // 配置驱动：统一的格式化方式，不硬编码特定的属性名
        return key + ": " + value;
    }
    
    // Getters and Setters
    public String getNodeType() {
        return nodeType;
    }
    
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }
    
    public String getMethodSignature() {
        return methodSignature;
    }
    
    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
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
    
    public TraceNode getParent() {
        return parent;
    }
    
    public void setParent(TraceNode parent) {
        this.parent = parent;
    }
    
    public List<TraceNode> getChildren() {
        return children;
    }
    
    public void setChildren(List<TraceNode> children) {
        this.children = children;
    }
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        return String.format("TraceNode{type=%s, method=%s, time=%dms, children=%d}", 
            nodeType, methodSignature, getExecutionTime(), children.size());
    }
}
