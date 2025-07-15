package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.monitor200.EnhancerCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.middleware.cli.annotations.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 简单的trace命令 - 直接使用EnhancerCommand
 * 用于验证文件操作拦截
 */
@Name("simple-trace")
@Summary("Simple trace command for testing")
@Description("Simple trace command to test method interception")
public class SimpleTraceCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;

    @Argument(index = 0, argName = "class-pattern")
    @Description("Class name pattern")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classPattern == null) {
            return null;
        }
        // 转换通配符为正则表达式
        String regex = classPattern.replace(".", "\\.")
                                 .replace("*", ".*");
        return new RegexMatcher(regex);
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        // 不排除任何类
        return null;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodPattern == null) {
            return null;
        }
        // 转换通配符为正则表达式
        String regex = methodPattern.replace("*", ".*");
        return new RegexMatcher(regex);
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new SimpleTraceAdviceListener(this, process);
    }

    /**
     * 简单的Advice监听器
     */
    public static class SimpleTraceAdviceListener extends AdviceListenerAdapter {
        
        private final SimpleTraceCommand command;
        private final CommandProcess process;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        
        public SimpleTraceAdviceListener(SimpleTraceCommand command, CommandProcess process) {
            this.command = command;
            this.process = process;
        }
        
        @Override
        public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
            long startTime = System.currentTimeMillis();
            
            // 输出拦截信息
            StringBuilder output = new StringBuilder();
            output.append("\n").append(repeat("=", 60)).append("\n");
            output.append("[").append(dateFormat.format(new Date(startTime))).append("] ");
            output.append("[INTERCEPTED]\n");
            output.append("  Class: ").append(clazz.getName()).append("\n");
            output.append("  Method: ").append(method.getName()).append("\n");
            output.append("  Thread: ").append(Thread.currentThread().getName()).append("\n");
            
            if (args != null && args.length > 0) {
                output.append("  Args: [");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) output.append(", ");
                    if (args[i] != null) {
                        if (args[i] instanceof byte[]) {
                            output.append("byte[").append(((byte[])args[i]).length).append("]");
                        } else {
                            String argStr = args[i].toString();
                            if (argStr.length() > 50) {
                                argStr = argStr.substring(0, 50) + "...";
                            }
                            output.append(argStr);
                        }
                    } else {
                        output.append("null");
                    }
                }
                output.append("]\n");
            }
            
            output.append(repeat("=", 60)).append("\n");
            
            process.write(output.toString());
            
            // 增加计数
            process.times().incrementAndGet();
            if (isLimitExceeded(100, process.times().get())) {
                abortProcess(process, 100);
            }
        }
        
        @Override
        public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {
            // 可以在这里添加返回值信息
        }
        
        @Override
        public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {
            process.write("  Exception: " + throwable.getMessage() + "\n");
        }
        
        /**
         * Java 8兼容的字符串重复方法
         */
        private String repeat(String str, int count) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(str);
            }
            return sb.toString();
        }
    }
}
