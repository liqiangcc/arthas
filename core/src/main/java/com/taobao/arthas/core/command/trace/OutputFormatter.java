package com.taobao.arthas.core.command.trace;

import java.util.Map;

/**
 * 输出格式化器 - 阶段1基础版本
 */
public class OutputFormatter {

    /**
     * 格式化输出信息
     */
    public String format(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "无数据";
        }

        StringBuilder sb = new StringBuilder();
        
        // 阶段1：简单的键值对输出
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 格式化跟踪结果
     */
    public String formatTrace(TraceManager.TraceContext trace) {
        if (trace == null) {
            return "无跟踪数据";
        }

        return String.format("Trace ID: %s | Duration: %dms | Status: %s",
                trace.getTraceId(),
                trace.getDuration(),
                trace.isCompleted() ? "完成" : "进行中");
    }

    /**
     * 格式化探针信息
     */
    public String formatProbeInfo(ProbeConfig config) {
        if (config == null) {
            return "无探针配置";
        }

        return String.format("探针: %s | 状态: %s | 指标数: %d",
                config.getName(),
                config.isEnabled() ? "启用" : "禁用",
                config.getMetrics() != null ? config.getMetrics().size() : 0);
    }

    /**
     * 格式化错误信息
     */
    public String formatError(String message, Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("错误: ").append(message).append("\n");
        
        if (error != null) {
            sb.append("详细信息: ").append(error.getMessage()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 格式化帮助信息
     */
    public String formatHelp() {
        return "trace-flow (tf) - 跟踪HTTP请求的完整执行链路\n" +
               "\n" +
               "用法:\n" +
               "  tf                                    # 跟踪下一个HTTP请求\n" +
               "  tf -n 5                               # 跟踪5次请求\n" +
               "  tf --filter \"executionTime > 1000\"    # 只显示慢请求\n" +
               "  tf --filter \"url.startsWith('/api')\" # 只跟踪API请求\n" +
               "  tf --output-file result.json          # 保存结果到文件\n" +
               "  tf --verbose                          # 详细模式输出\n" +
               "  tf --list-probes                      # 列出所有探针\n" +
               "  tf --show-config database             # 显示探针配置\n" +
               "\n" +
               "参数:\n" +
               "  -n, --count <number>                  跟踪次数，默认为1\n" +
               "  --filter <expression>                 过滤表达式\n" +
               "  --output-file <file>                  输出文件路径\n" +
               "  --verbose                             详细模式\n" +
               "  --stack-trace-threshold <ms>          堆栈跟踪阈值\n" +
               "  --list-probes                         列出所有探针\n" +
               "  --show-config <probe>                 显示探针配置\n" +
               "  -h, --help                            显示帮助信息\n" +
               "\n" +
               "过滤表达式示例:\n" +
               "  executionTime > 1000                  # 执行时间超过1秒\n" +
               "  url.contains('/user')                 # URL包含'/user'\n" +
               "  url.startsWith('/api')                # URL以'/api'开始\n" +
               "  operationType == 'SELECT'             # SQL操作类型为SELECT\n" +
               "\n" +
               "注意: 阶段1为基础版本，支持有限的过滤表达式";
    }
}
