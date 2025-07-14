package com.taobao.arthas.core.command.trace;

import java.util.Map;

/**
 * 过滤引擎 - 阶段1基础版本
 */
public class FilterEngine {

    /**
     * 检查指标是否匹配过滤条件
     * 
     * @param filterExpression 过滤表达式
     * @param metrics 指标数据
     * @return 是否匹配
     */
    public boolean matches(String filterExpression, Map<String, Object> metrics) {
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return true; // 无过滤条件，匹配所有
        }

        try {
            // 阶段1：简单的过滤逻辑
            return evaluateSimpleFilter(filterExpression.trim(), metrics);
        } catch (Exception e) {
            System.err.println("过滤表达式执行失败: " + filterExpression + ", 错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 简单过滤逻辑评估
     */
    private boolean evaluateSimpleFilter(String expression, Map<String, Object> metrics) {
        // 阶段1：支持一些基本的过滤表达式
        
        // 特殊情况：true 匹配所有
        if ("true".equals(expression)) {
            return true;
        }
        
        // 特殊情况：false 匹配无
        if ("false".equals(expression)) {
            return false;
        }

        // 简单的数值比较：executionTime > 1000
        if (expression.contains(" > ")) {
            return evaluateGreaterThan(expression, metrics);
        }
        
        if (expression.contains(" < ")) {
            return evaluateLessThan(expression, metrics);
        }
        
        if (expression.contains(" == ")) {
            return evaluateEquals(expression, metrics);
        }

        // 字符串包含：url.contains('/api')
        if (expression.contains(".contains(")) {
            return evaluateContains(expression, metrics);
        }

        // 字符串开始：url.startsWith('/api')
        if (expression.contains(".startsWith(")) {
            return evaluateStartsWith(expression, metrics);
        }

        // 阶段1暂不支持复杂表达式
        System.err.println("阶段1暂不支持复杂过滤表达式: " + expression);
        return false;
    }

    private boolean evaluateGreaterThan(String expression, Map<String, Object> metrics) {
        String[] parts = expression.split(" > ");
        if (parts.length != 2) return false;
        
        String metricName = parts[0].trim();
        String valueStr = parts[1].trim();
        
        Object metricValue = metrics.get(metricName);
        if (metricValue == null) return false;
        
        try {
            double metricNum = Double.parseDouble(metricValue.toString());
            double compareNum = Double.parseDouble(valueStr);
            return metricNum > compareNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean evaluateLessThan(String expression, Map<String, Object> metrics) {
        String[] parts = expression.split(" < ");
        if (parts.length != 2) return false;
        
        String metricName = parts[0].trim();
        String valueStr = parts[1].trim();
        
        Object metricValue = metrics.get(metricName);
        if (metricValue == null) return false;
        
        try {
            double metricNum = Double.parseDouble(metricValue.toString());
            double compareNum = Double.parseDouble(valueStr);
            return metricNum < compareNum;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean evaluateEquals(String expression, Map<String, Object> metrics) {
        String[] parts = expression.split(" == ");
        if (parts.length != 2) return false;
        
        String metricName = parts[0].trim();
        String expectedValue = parts[1].trim().replace("'", "").replace("\"", "");
        
        Object metricValue = metrics.get(metricName);
        if (metricValue == null) return false;
        
        return expectedValue.equals(metricValue.toString());
    }

    private boolean evaluateContains(String expression, Map<String, Object> metrics) {
        // 解析 url.contains('/api') 格式
        int dotIndex = expression.indexOf('.');
        int parenIndex = expression.indexOf('(');
        int closeParenIndex = expression.indexOf(')', parenIndex);
        
        if (dotIndex == -1 || parenIndex == -1 || closeParenIndex == -1) return false;
        
        String metricName = expression.substring(0, dotIndex).trim();
        String searchValue = expression.substring(parenIndex + 1, closeParenIndex)
                .trim().replace("'", "").replace("\"", "");
        
        Object metricValue = metrics.get(metricName);
        if (metricValue == null) return false;
        
        return metricValue.toString().contains(searchValue);
    }

    private boolean evaluateStartsWith(String expression, Map<String, Object> metrics) {
        // 解析 url.startsWith('/api') 格式
        int dotIndex = expression.indexOf('.');
        int parenIndex = expression.indexOf('(');
        int closeParenIndex = expression.indexOf(')', parenIndex);
        
        if (dotIndex == -1 || parenIndex == -1 || closeParenIndex == -1) return false;
        
        String metricName = expression.substring(0, dotIndex).trim();
        String prefixValue = expression.substring(parenIndex + 1, closeParenIndex)
                .trim().replace("'", "").replace("\"", "");
        
        Object metricValue = metrics.get(metricName);
        if (metricValue == null) return false;
        
        return metricValue.toString().startsWith(prefixValue);
    }

    /**
     * 验证过滤表达式语法
     */
    public void validateFilter(String filterExpression) {
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return; // 空过滤条件是有效的
        }

        // 阶段1：基本语法检查
        String expression = filterExpression.trim();
        
        // 检查是否包含支持的操作符
        boolean hasSupported = expression.equals("true") || 
                              expression.equals("false") ||
                              expression.contains(" > ") ||
                              expression.contains(" < ") ||
                              expression.contains(" == ") ||
                              expression.contains(".contains(") ||
                              expression.contains(".startsWith(");
        
        if (!hasSupported) {
            throw new IllegalArgumentException("阶段1暂不支持此过滤表达式: " + expression);
        }
    }
}
