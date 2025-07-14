package com.taobao.arthas.core.command.trace;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Source表达式解析器 - 阶段1基础版本
 * 支持内置变量解析：startTime, endTime, threadName
 */
public class SourceExpressionParser {

    private static final Pattern BUILTIN_VARIABLE_PATTERN = Pattern.compile("^(startTime|endTime|executionTime|threadName)$");
    
    /**
     * 解析source表达式
     * 
     * @param expression 表达式字符串
     * @param context 执行上下文
     * @return 解析结果
     */
    public Object parse(String expression, ExecutionContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("表达式不能为空");
        }

        String trimmedExpression = expression.trim();

        try {
            // 阶段1：只支持内置变量
            if (BUILTIN_VARIABLE_PATTERN.matcher(trimmedExpression).matches()) {
                return parseBuiltinVariable(trimmedExpression, context);
            }

            // 阶段1暂不支持复杂表达式
            throw new UnsupportedOperationException("阶段1暂不支持复杂表达式: " + expression + 
                    "\n支持的表达式: startTime, endTime, executionTime, threadName");

        } catch (Exception e) {
            throw new SourceExpressionException("解析表达式失败: " + expression, e);
        }
    }

    /**
     * 解析内置变量
     */
    private Object parseBuiltinVariable(String variable, ExecutionContext context) {
        switch (variable) {
            case "startTime":
                return context.getStartTime();
            case "endTime":
                return context.getEndTime();
            case "executionTime":
                return context.getExecutionTime();
            case "threadName":
                return context.getThreadName();
            default:
                throw new UnsupportedOperationException("不支持的内置变量: " + variable);
        }
    }

    /**
     * 检查表达式是否为内置变量
     */
    public boolean isBuiltinVariable(String expression) {
        if (expression == null) {
            return false;
        }
        return BUILTIN_VARIABLE_PATTERN.matcher(expression.trim()).matches();
    }

    /**
     * 获取支持的内置变量列表
     */
    public String[] getSupportedBuiltinVariables() {
        return new String[]{"startTime", "endTime", "executionTime", "threadName"};
    }

    /**
     * 验证表达式语法
     */
    public void validateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("表达式不能为空");
        }

        String trimmedExpression = expression.trim();

        // 阶段1只验证内置变量
        if (!BUILTIN_VARIABLE_PATTERN.matcher(trimmedExpression).matches()) {
            throw new UnsupportedOperationException("阶段1暂不支持此表达式: " + expression);
        }
    }

    /**
     * Source表达式解析异常
     */
    public static class SourceExpressionException extends RuntimeException {
        public SourceExpressionException(String message) {
            super(message);
        }

        public SourceExpressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
