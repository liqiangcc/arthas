package com.taobao.arthas.core.command.trace;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Source表达式解析器 - 阶段2扩展版本
 * 支持内置变量和复杂表达式解析：startTime, endTime, threadName, this.xxx(), args[n].xxx(), returnValue.xxx()
 */
public class SourceExpressionParser {

    private static final Pattern BUILTIN_VARIABLE_PATTERN = Pattern.compile("^(startTime|endTime|executionTime|threadName)$");
    private static final Pattern THIS_METHOD_PATTERN = Pattern.compile("^this\\.([a-zA-Z_][a-zA-Z0-9_]*)\\(\\)$");
    private static final Pattern ARGS_ACCESS_PATTERN = Pattern.compile("^args\\[(\\d+)\\](?:\\.([a-zA-Z_][a-zA-Z0-9_]*)\\(\\))?$");
    private static final Pattern RETURN_VALUE_PATTERN = Pattern.compile("^returnValue(?:\\.([a-zA-Z_][a-zA-Z0-9_]*)\\(\\))?$");
    
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
            // 阶段2：支持多种表达式类型

            // 1. 内置变量
            if (BUILTIN_VARIABLE_PATTERN.matcher(trimmedExpression).matches()) {
                return parseBuiltinVariable(trimmedExpression, context);
            }

            // 2. this.method() 调用
            Matcher thisMatcher = THIS_METHOD_PATTERN.matcher(trimmedExpression);
            if (thisMatcher.matches()) {
                return parseThisMethodCall(thisMatcher.group(1), context);
            }

            // 3. args[n] 或 args[n].method() 访问
            Matcher argsMatcher = ARGS_ACCESS_PATTERN.matcher(trimmedExpression);
            if (argsMatcher.matches()) {
                int index = Integer.parseInt(argsMatcher.group(1));
                String methodName = argsMatcher.group(2);
                return parseArgsAccess(index, methodName, context);
            }

            // 4. returnValue 或 returnValue.method() 访问
            Matcher returnMatcher = RETURN_VALUE_PATTERN.matcher(trimmedExpression);
            if (returnMatcher.matches()) {
                String methodName = returnMatcher.group(1);
                return parseReturnValueAccess(methodName, context);
            }

            // 不支持的表达式
            throw new UnsupportedOperationException("不支持的表达式: " + expression +
                    "\n支持的表达式类型:" +
                    "\n  - 内置变量: startTime, endTime, executionTime, threadName" +
                    "\n  - this方法调用: this.methodName()" +
                    "\n  - 参数访问: args[n] 或 args[n].methodName()" +
                    "\n  - 返回值访问: returnValue 或 returnValue.methodName()");

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
     * 解析this.method()调用
     */
    private Object parseThisMethodCall(String methodName, ExecutionContext context) {
        Object target = context.getTarget();
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            System.err.println("调用this." + methodName + "()失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析args[n]或args[n].method()访问
     */
    private Object parseArgsAccess(int index, String methodName, ExecutionContext context) {
        Object[] args = context.getArgs();
        if (args == null || index >= args.length) {
            return null;
        }

        Object arg = args[index];
        if (arg == null) {
            return null;
        }

        // 如果没有方法名，直接返回参数
        if (methodName == null) {
            return arg;
        }

        // 调用参数的方法
        try {
            Method method = arg.getClass().getMethod(methodName);
            return method.invoke(arg);
        } catch (Exception e) {
            System.err.println("调用args[" + index + "]." + methodName + "()失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析returnValue或returnValue.method()访问
     */
    private Object parseReturnValueAccess(String methodName, ExecutionContext context) {
        Object returnValue = context.getReturnValue();
        if (returnValue == null) {
            return null;
        }

        // 如果没有方法名，直接返回值
        if (methodName == null) {
            return returnValue;
        }

        // 调用返回值的方法
        try {
            Method method = returnValue.getClass().getMethod(methodName);
            return method.invoke(returnValue);
        } catch (Exception e) {
            System.err.println("调用returnValue." + methodName + "()失败: " + e.getMessage());
            return null;
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
