package com.taobao.arthas.core.command.trace;

import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.MemberAccess;
import ognl.ClassResolver;
import com.taobao.arthas.core.command.express.DefaultMemberAccess;
import com.taobao.arthas.core.command.express.CustomClassResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于OGNL的Source表达式解析器
 * 支持强大的表达式语法，包括：
 * - 字段访问：this.inputBuffer.end
 * - 方法调用：args[0].getHeader("content-length")
 * - 条件表达式：args[0].getMethod() == 'POST' ? 'POST请求' : '其他请求'
 * - 集合操作：args[0].getHeaderNames().size()
 * - 数学运算：this.inputBuffer.end - this.inputBuffer.pos
 * 
 * 参考：
 * - Arthas OGNL用法：https://github.com/alibaba/arthas/issues/71
 * - OGNL官方指南：https://commons.apache.org/dormant/commons-ognl/language-guide.html
 */
public class OgnlSourceExpressionParser {
    
    /**
     * 解析表达式并返回结果
     * 
     * @param expression 表达式字符串
     * @param context 执行上下文
     * @return 表达式计算结果
     */
    public Object parse(String expression, ExecutionContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 创建OGNL上下文（参考Arthas的OgnlExpress实现）
            MemberAccess memberAccess = new DefaultMemberAccess(true);
            ClassResolver classResolver = CustomClassResolver.customClassResolver;
            OgnlContext ognlContext = new OgnlContext(memberAccess, classResolver, null, null);

            // 设置根对象和上下文变量
            setupOgnlContext(ognlContext, context);
            
            // 编译并执行OGNL表达式
            Object compiledExpression = Ognl.parseExpression(expression);
            return Ognl.getValue(compiledExpression, ognlContext, context);
            
        } catch (OgnlException e) {
            System.err.println("OGNL表达式解析失败: " + expression + ", 错误: " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        } catch (Exception e) {
            System.err.println("表达式执行异常: " + expression + ", 错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 设置OGNL上下文
     */
    private void setupOgnlContext(OgnlContext ognlContext, ExecutionContext context) {
        // 设置根对象为ExecutionContext，这样可以直接访问其属性
        // 例如：target, args, returnValue, startTime等
        
        // 添加常用变量到上下文
        Map<String, Object> variables = new HashMap<>();
        
        // 基本变量
        variables.put("this", context.getTarget());
        variables.put("target", context.getTarget());
        variables.put("args", context.getArgs());
        variables.put("returnValue", context.getReturnValue());
        
        // 时间相关变量
        variables.put("startTime", context.getStartTime());
        variables.put("endTime", context.getEndTime());
        variables.put("executionTime", context.getExecutionTime());
        variables.put("threadName", context.getThreadName());
        
        // 添加工具方法
        variables.put("utils", new OgnlUtils());
        
        // 将变量添加到OGNL上下文
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            ognlContext.put(entry.getKey(), entry.getValue());
        }
        
        // 设置类型转换器（如果需要）
        // ognlContext.setTypeConverter(new CustomTypeConverter());
    }
    
    /**
     * OGNL工具类，提供常用的辅助方法
     */
    public static class OgnlUtils {
        
        /**
         * 安全的字符串转整数
         */
        public Integer parseInt(String str) {
            if (str == null || str.trim().isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        /**
         * 安全的字符串转长整数
         */
        public Long parseLong(String str) {
            if (str == null || str.trim().isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        /**
         * 检查字符串是否为空
         */
        public boolean isEmpty(String str) {
            return str == null || str.trim().isEmpty();
        }
        
        /**
         * 检查字符串是否不为空
         */
        public boolean isNotEmpty(String str) {
            return !isEmpty(str);
        }
        
        /**
         * 格式化时间戳
         */
        public String formatTime(long timestamp) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                    .format(new java.util.Date(timestamp));
        }
        
        /**
         * 获取对象的类名
         */
        public String getClassName(Object obj) {
            return obj != null ? obj.getClass().getSimpleName() : "null";
        }
        
        /**
         * 安全的toString
         */
        public String safeToString(Object obj) {
            return obj != null ? obj.toString() : "null";
        }
    }
}
