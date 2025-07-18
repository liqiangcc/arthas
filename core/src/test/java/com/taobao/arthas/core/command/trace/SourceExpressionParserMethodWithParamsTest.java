package com.taobao.arthas.core.command.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SourceExpressionParser带参数方法调用功能测试
 */
public class SourceExpressionParserMethodWithParamsTest {

    private SourceExpressionParser parser;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        parser = new SourceExpressionParser();
        
        // 创建测试对象
        MockRequest request = new MockRequest();
        request.addHeader("content-length", "1024");
        request.addHeader("content-type", "application/json");
        request.addHeader("user-agent", "Mozilla/5.0");
        
        context = new ExecutionContext();
        context.setArgs(new Object[]{request});
        context.setStartTime(System.currentTimeMillis());
        context.setThreadName(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("测试带参数的方法调用: args[0].getHeader(\"content-length\")")
    void testArgsMethodWithStringParam() {
        Object result = parser.parse("args[0].getHeader(\"content-length\")", context);
        assertNotNull(result);
        assertEquals("1024", result);
    }

    @Test
    @DisplayName("测试不同的参数值")
    void testDifferentParams() {
        Object contentType = parser.parse("args[0].getHeader(\"content-type\")", context);
        assertEquals("application/json", contentType);
        
        Object userAgent = parser.parse("args[0].getHeader(\"user-agent\")", context);
        assertEquals("Mozilla/5.0", userAgent);
    }

    @Test
    @DisplayName("测试不存在的header")
    void testNonExistentHeader() {
        Object result = parser.parse("args[0].getHeader(\"non-existent\")", context);
        assertNull(result);
    }

    @Test
    @DisplayName("测试空参数")
    void testEmptyParam() {
        Object result = parser.parse("args[0].getHeader(\"\")", context);
        assertNull(result);
    }

    @Test
    @DisplayName("测试参数索引越界")
    void testArgsIndexOutOfBounds() {
        Object result = parser.parse("args[1].getHeader(\"content-length\")", context);
        assertNull(result);
    }

    @Test
    @DisplayName("测试方法不存在")
    void testMethodNotExists() {
        Object result = parser.parse("args[0].nonExistentMethod(\"param\")", context);
        assertNull(result);
    }

    @Test
    @DisplayName("测试与无参数方法调用的区别")
    void testDifferenceFromNoParamMethod() {
        // 带参数的方法调用
        Object withParam = parser.parse("args[0].getHeader(\"content-length\")", context);
        assertEquals("1024", withParam);
        
        // 无参数的方法调用
        Object noParam = parser.parse("args[0].toString()", context);
        assertNotNull(noParam);
        assertTrue(noParam.toString().contains("MockRequest"));
    }

    // 模拟Request对象
    public static class MockRequest {
        private java.util.Map<String, String> headers = new java.util.HashMap<>();
        
        public void addHeader(String name, String value) {
            headers.put(name.toLowerCase(), value);
        }
        
        public String getHeader(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }
            return headers.get(name.toLowerCase());
        }
        
        @Override
        public String toString() {
            return "MockRequest{headers=" + headers + "}";
        }
    }
}
