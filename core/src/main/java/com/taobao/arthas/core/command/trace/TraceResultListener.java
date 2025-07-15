package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.shell.command.CommandProcess;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 跟踪结果监听器 - 收集和显示真实的拦截结果
 */
public class TraceResultListener {
    
    private final CommandProcess process;
    private final boolean verbose;
    private final List<TraceResult> pendingResults = new CopyOnWriteArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private int displayedCount = 0;
    
    public TraceResultListener(CommandProcess process, boolean verbose) {
        this.process = process;
        this.verbose = verbose;
        
        // 注册为全局监听器，接收拦截结果
        TraceResultCollector.getInstance().addListener(this);
    }
    
    /**
     * 接收新的跟踪结果
     */
    public void onTraceResult(TraceResult result) {
        pendingResults.add(result);
    }
    
    /**
     * 检查并显示新的跟踪结果
     */
    public int checkAndDisplayNewTraces() {
        int newCount = 0;
        
        while (!pendingResults.isEmpty()) {
            TraceResult result = pendingResults.remove(0);
            displayTraceResult(result);
            newCount++;
            displayedCount++;
        }
        
        return newCount;
    }
    
    /**
     * 显示单个跟踪结果
     */
    private void displayTraceResult(TraceResult result) {
        try {
            // 显示时间戳和基本信息
            process.write("\n" + repeatString("=", 60) + "\n");
            process.write("[" + dateFormat.format(new Date(result.getTimestamp())) + "] ");
            process.write("[" + result.getProbeType() + "]\n");
            
            // 显示方法信息
            process.write("  Method: " + result.getMethodSignature() + "\n");
            process.write("  Execution Time: " + result.getExecutionTime() + "ms\n");
            process.write("  Thread: " + result.getThreadName() + "\n");
            
            // 显示探针特定的属性
            if (result.getAttributes() != null && !result.getAttributes().isEmpty()) {
                for (String key : result.getAttributes().keySet()) {
                    Object value = result.getAttributes().get(key);
                    if (value != null && !"traceNode".equals(key)) {
                        process.write("  " + formatAttributeName(key) + ": " + value + "\n");
                    }
                }
            }
            
            // 如果有异常，显示异常信息
            if (result.getException() != null) {
                process.write("  Error: " + result.getException().getMessage() + "\n");
            }
            
            // 如果有链路跟踪信息，显示调用树
            if (result.getTraceContext() != null) {
                displayTraceTree(result.getTraceContext());
            }
            
            process.write(repeatString("=", 60) + "\n");
            
        } catch (Exception e) {
            if (verbose) {
                process.write("Error displaying trace result: " + e.getMessage() + "\n");
            }
        }
    }
    
    /**
     * 显示调用树
     */
    private void displayTraceTree(TraceManager.TraceContext traceContext) {
        if (traceContext.getRootNode() != null) {
            process.write("\n  Call Tree:\n");
            String treeOutput = traceContext.getRootNode().toTreeString();
            // 为每行添加缩进
            String[] lines = treeOutput.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    process.write("  " + line + "\n");
                }
            }
        }
    }
    
    /**
     * 重复字符串（Java 8兼容）
     */
    private String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 格式化属性名称（配置驱动）
     */
    private String formatAttributeName(String key) {
        // 配置驱动：从探针配置中获取指标的显示名称
        try {
            ProbeManager probeManager = new ProbeManager();
            probeManager.initializeProbes();

            for (ProbeConfig config : probeManager.getAllProbeConfigs().values()) {
                for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                    if (key.equals(metric.getName())) {
                        // 如果配置中有description，使用description作为显示名称
                        String description = metric.getDescription();
                        if (description != null && !description.isEmpty()) {
                            return description;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略配置读取错误
        }

        // 如果配置中没有找到，使用原始名称
        return key;
    }
    
    /**
     * 获取已显示的跟踪数量
     */
    public int getDisplayedCount() {
        return displayedCount;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        TraceResultCollector.getInstance().removeListener(this);
        pendingResults.clear();
    }
    
    /**
     * 跟踪结果数据结构
     */
    public static class TraceResult {
        private long timestamp;
        private String probeType;
        private String methodSignature;
        private long executionTime;
        private String threadName;
        private java.util.Map<String, Object> attributes;
        private Throwable exception;
        private TraceManager.TraceContext traceContext;
        
        public TraceResult(String probeType, String methodSignature) {
            this.timestamp = System.currentTimeMillis();
            this.probeType = probeType;
            this.methodSignature = methodSignature;
            this.attributes = new java.util.HashMap<>();
        }
        
        // Getters and Setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getProbeType() { return probeType; }
        public void setProbeType(String probeType) { this.probeType = probeType; }
        
        public String getMethodSignature() { return methodSignature; }
        public void setMethodSignature(String methodSignature) { this.methodSignature = methodSignature; }
        
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        
        public String getThreadName() { return threadName; }
        public void setThreadName(String threadName) { this.threadName = threadName; }
        
        public java.util.Map<String, Object> getAttributes() { return attributes; }
        public void setAttributes(java.util.Map<String, Object> attributes) { this.attributes = attributes; }
        
        public Throwable getException() { return exception; }
        public void setException(Throwable exception) { this.exception = exception; }
        
        public TraceManager.TraceContext getTraceContext() { return traceContext; }
        public void setTraceContext(TraceManager.TraceContext traceContext) { this.traceContext = traceContext; }
    }
}
