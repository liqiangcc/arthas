package com.taobao.arthas.core.command.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OGNL表达式解析器测试
 */
public class OgnlSourceExpressionParserTest {

    private SourceExpressionParser parser;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        // 启用OGNL支持
        parser = new SourceExpressionParser(true);
        
        // 创建测试对象
        TestObject testObj = new TestObject();
        testObj.inputBuffer = new InputBuffer();
        testObj.inputBuffer.end = 219;
        testObj.inputBuffer.pos = 0;
        
        MockRequest request = new MockRequest();
        request.addHeader("content-length", "256");
        request.addHeader("content-type", "application/json");
        
        context = new ExecutionContext();
        context.setTarget(testObj);
        context.setArgs(new Object[]{request});
        context.setStartTime(System.currentTimeMillis() - 100);
        context.setEndTime(System.currentTimeMillis());
        context.setThreadName(Thread.currentThread().getName());
        context.addMetric("content-length", 81);
        context.addMetric("head-length", 219);
    }

    @Test
    @DisplayName("测试基本字段访问")
    void testBasicFieldAccess() {
        Object result = parser.parse("targetObject.inputBuffer.end", context);
        assertEquals(1024, result);
    }

    @Test
    @DisplayName("方法带属性访问")
    void testMethodFieldAccess() {
        Object result = parser.parse("targetObject.inputBuffer.end", context);
        assertEquals(1024, result);
    }

    @Test
    @DisplayName("测试带参数的方法调用")
    void testMethodWithParams() {
        Object result = parser.parse("#v1=targetObject.inputBuffer", context);
        System.out.println(result);
    }

    @Test
    @DisplayName("测试数学运算表达式")
    void testMathExpression() {
        Object result = parser.parse("targetObject.inputBuffer.end + metrics.get(\"content-length\")", context);
        assertEquals(512, result);
    }

    @Test
    @DisplayName("复制表达式")
    void testExpression() {
        Object result = parser.parse("#v1 = metrics.get(\"head-length\")," +
                "#v2 = metrics.get(\"content-length\"),#v3=#v1 + #v2,#v5=inputBuffer.getByteBuffer().hb,v4=new String(#v5,0,#v3)", context);
        System.out.println(result);
    }

    @Test
    @DisplayName("测试条件表达式")
    void testConditionalExpression() {
        Object result = parser.parse("args[0].getHeader(\"content-type\") == \"application/json\" ? \"JSON\" : \"OTHER\"", context);
        assertEquals("JSON", result);
    }

    @Test
    @DisplayName("测试比较表达式")
    void testComparisonExpression() {
        Object result = parser.parse("targetObject.inputBuffer.end > 100", context);
        assertEquals(true, result);
    }

    @Test
    @DisplayName("测试逻辑表达式")
    void testLogicalExpression() {
        Object result = parser.parse("targetObject.inputBuffer.end > 100 && args[0].getHeader(\"content-length\") != null", context);
        assertEquals(true, result);
    }

    @Test
    @DisplayName("测试工具方法")
    void testUtilsMethods() {
        Object result = parser.parse("#v1=args[0].getHeader(\"content-length\"),#v2=@Integer@parseInt(#v1)", context);
        assertEquals(256, result);
    }

    @Test
    @DisplayName("测试复杂表达式")
    void testComplexExpression() {
        // 计算缓冲区使用率
        Object result = parser.parse("(targetObject.inputBuffer.end * 100) / 2048", context);
        assertEquals(50, result);
    }

    @Test
    @DisplayName("测试字符串操作")
    void testStringOperations() {
        Object result = parser.parse("args[0].getHeader(\"content-type\").toUpperCase()", context);
        assertEquals("APPLICATION/JSON", result);
    }

    @Test
    @DisplayName("测试null安全访问")
    void testNullSafeAccess() {
        Object result = parser.parse("args[0].getHeader(\"non-existent\") != null ? args[0].getHeader(\"non-existent\") : \"default\"", context);
        assertEquals("default", result);
    }

    @Test
    @DisplayName("测试传统表达式兼容性")
    void testTraditionalCompatibility() {
        // 简单表达式应该仍然工作
        Object result1 = parser.parse("startTime", context);
        assertNotNull(result1);
        
        Object result2 = parser.parse("threadName", context);
        assertNotNull(result2);
        
        Object result3 = parser.parse("executionTime", context);
        assertNotNull(result3);
    }

    // 测试用的内部类
    public static class TestObject {
        public InputBuffer inputBuffer;
    }
    
    public static class InputBuffer {
        public int end;
        public int pos;
        public byte[] buf = new byte[2048];
    }
    
    public static class MockRequest {
        private Map<String, String> headers = new HashMap<>();
        
        public void addHeader(String name, String value) {
            headers.put(name.toLowerCase(), value);
        }
        
        public String getHeader(String name) {
            return headers.get(name != null ? name.toLowerCase() : null);
        }
    }
}
