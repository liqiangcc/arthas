package com.taobao.arthas.core.command.trace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formula表达式解析器 - 阶段2基础版本
 * 支持简单的数学计算和指标引用
 */
public class FormulaExpressionParser {

    // 匹配 metrics.xxx 的模式
    private static final Pattern METRIC_REFERENCE_PATTERN = Pattern.compile("metrics\\.([a-zA-Z_][a-zA-Z0-9_]*)");
    
    /**
     * 解析formula表达式
     * 
     * @param formula 表达式字符串，如 "metrics.endTime - metrics.startTime"
     * @param context 执行上下文
     * @return 计算结果
     */
    public Object parse(String formula, ExecutionContext context) {
        if (formula == null || formula.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula expression cannot be empty");
        }

        String trimmedFormula = formula.trim();

        try {
            // 阶段2：支持简单的数学表达式
            return evaluateSimpleFormula(trimmedFormula, context);

        } catch (Exception e) {
            throw new FormulaExpressionException("Failed to parse formula: " + formula, e);
        }
    }

    /**
     * 评估简单的formula表达式
     */
    private Object evaluateSimpleFormula(String formula, ExecutionContext context) {
        // 替换所有的 metrics.xxx 引用为实际值
        String processedFormula = replaceMetricReferences(formula, context);
        
        // 阶段2：支持基本的数学运算
        if (processedFormula.contains(" - ")) {
            return evaluateSubtraction(processedFormula);
        }
        
        if (processedFormula.contains(" + ")) {
            return evaluateAddition(processedFormula);
        }
        
        if (processedFormula.contains(" * ")) {
            return evaluateMultiplication(processedFormula);
        }
        
        if (processedFormula.contains(" / ")) {
            return evaluateDivision(processedFormula);
        }

        // 如果没有运算符，尝试解析为数字或返回原值
        try {
            return Long.parseLong(processedFormula);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(processedFormula);
            } catch (NumberFormatException e2) {
                return processedFormula; // 返回字符串值
            }
        }
    }

    /**
     * 替换formula中的指标引用
     */
    private String replaceMetricReferences(String formula, ExecutionContext context) {
        Matcher matcher = METRIC_REFERENCE_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String metricName = matcher.group(1);
            Object metricValue = context.getMetric(metricName);
            
            if (metricValue != null) {
                matcher.appendReplacement(result, metricValue.toString());
            } else {
                // 如果指标不存在，保持原样或使用默认值
                matcher.appendReplacement(result, "0");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 评估减法表达式
     */
    private Object evaluateSubtraction(String expression) {
        String[] parts = expression.split(" - ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid subtraction expression: " + expression);
        }
        
        try {
            long left = Long.parseLong(parts[0].trim());
            long right = Long.parseLong(parts[1].trim());
            return left - right;
        } catch (NumberFormatException e) {
            try {
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left - right;
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException("Cannot perform subtraction on non-numeric values: " + expression);
            }
        }
    }

    /**
     * 评估加法表达式
     */
    private Object evaluateAddition(String expression) {
        String[] parts = expression.split(" \\+ ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid addition expression: " + expression);
        }
        
        try {
            long left = Long.parseLong(parts[0].trim());
            long right = Long.parseLong(parts[1].trim());
            return left + right;
        } catch (NumberFormatException e) {
            try {
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left + right;
            } catch (NumberFormatException e2) {
                // 字符串连接
                return parts[0].trim() + parts[1].trim();
            }
        }
    }

    /**
     * 评估乘法表达式
     */
    private Object evaluateMultiplication(String expression) {
        String[] parts = expression.split(" \\* ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid multiplication expression: " + expression);
        }
        
        try {
            long left = Long.parseLong(parts[0].trim());
            long right = Long.parseLong(parts[1].trim());
            return left * right;
        } catch (NumberFormatException e) {
            try {
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return left * right;
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException("Cannot perform multiplication on non-numeric values: " + expression);
            }
        }
    }

    /**
     * 评估除法表达式
     */
    private Object evaluateDivision(String expression) {
        String[] parts = expression.split(" / ");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid division expression: " + expression);
        }
        
        try {
            double left = Double.parseDouble(parts[0].trim());
            double right = Double.parseDouble(parts[1].trim());
            
            if (right == 0) {
                throw new IllegalArgumentException("Division by zero: " + expression);
            }
            
            return left / right;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot perform division on non-numeric values: " + expression);
        }
    }

    /**
     * 验证formula表达式语法
     */
    public void validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula expression cannot be empty");
        }

        String trimmedFormula = formula.trim();

        // 检查是否包含支持的操作符或指标引用
        boolean hasSupported = trimmedFormula.contains("metrics.") ||
                              trimmedFormula.contains(" + ") ||
                              trimmedFormula.contains(" - ") ||
                              trimmedFormula.contains(" * ") ||
                              trimmedFormula.contains(" / ");

        if (!hasSupported) {
            // 检查是否为简单的数字或字符串
            try {
                Double.parseDouble(trimmedFormula);
                return; // 是数字，有效
            } catch (NumberFormatException e) {
                // 不是数字，检查是否为有效的字符串
                if (trimmedFormula.length() > 0) {
                    return; // 非空字符串，有效
                }
            }
        }
    }

    /**
     * 获取formula中引用的所有指标名称
     */
    public String[] getReferencedMetrics(String formula) {
        if (formula == null) {
            return new String[0];
        }

        Matcher matcher = METRIC_REFERENCE_PATTERN.matcher(formula);
        java.util.List<String> metrics = new java.util.ArrayList<>();
        
        while (matcher.find()) {
            String metricName = matcher.group(1);
            if (!metrics.contains(metricName)) {
                metrics.add(metricName);
            }
        }
        
        return metrics.toArray(new String[0]);
    }

    /**
     * Formula表达式解析异常
     */
    public static class FormulaExpressionException extends RuntimeException {
        public FormulaExpressionException(String message) {
            super(message);
        }

        public FormulaExpressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
