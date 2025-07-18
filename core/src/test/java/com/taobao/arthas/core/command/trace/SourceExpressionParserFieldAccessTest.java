package com.taobao.arthas.core.command.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SourceExpressionParser字段访问功能测试
 */
public class SourceExpressionParserFieldAccessTest {

    private SourceExpressionParser parser;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        parser = new SourceExpressionParser();
        
        // 创建测试对象
        TestObject testObj = new TestObject();
        testObj.inputBuffer = new InputBuffer();
        testObj.inputBuffer.end = 1024;
        testObj.inputBuffer.pos = 512;
        testObj.request = new Request();
        testObj.request.method = "POST";
        
        context = new ExecutionContext();
        context.setTarget(testObj);
        context.setStartTime(System.currentTimeMillis());
        context.setThreadName(Thread.currentThread().getName());
    }

    @Test
    @DisplayName("测试简单字段访问: this.inputBuffer")
    void testSimpleFieldAccess() {
        Object result = parser.parse("this.inputBuffer", context);
        assertNotNull(result);
        assertTrue(result instanceof InputBuffer);
    }

    @Test
    @DisplayName("测试嵌套字段访问: this.inputBuffer.end")
    void testNestedFieldAccess() {
        Object result = parser.parse("this.inputBuffer.end", context);
        assertNotNull(result);
        assertEquals(1024, result);
    }

    @Test
    @DisplayName("测试多层嵌套字段访问: this.request.method")
    void testDeepNestedFieldAccess() {
        Object result = parser.parse("this.request.method", context);
        assertNotNull(result);
        assertEquals("POST", result);
    }

    @Test
    @DisplayName("测试访问不存在的字段")
    void testNonExistentField() {
        Object result = parser.parse("this.nonExistentField", context);
        assertNull(result);
    }

    @Test
    @DisplayName("测试访问null对象的字段")
    void testNullObjectFieldAccess() {
        TestObject testObj = new TestObject();
        testObj.inputBuffer = null; // 设置为null
        
        context.setTarget(testObj);
        
        Object result = parser.parse("this.inputBuffer.end", context);
        assertNull(result);
    }

    @Test
    @DisplayName("测试字段访问与方法调用的区别")
    void testFieldVsMethodAccess() {
        // 字段访问
        Object fieldResult = parser.parse("this.inputBuffer.end", context);
        assertEquals(1024, fieldResult);
        
        // 方法调用（如果存在的话）
        // Object methodResult = parser.parse("this.getInputBuffer()", context);
    }

    // 测试用的内部类
    public static class TestObject {
        public InputBuffer inputBuffer;
        public Request request;
        private String privateField = "private";
        
        public InputBuffer getInputBuffer() {
            return inputBuffer;
        }
    }
    
    public static class InputBuffer {
        public int end;
        public int pos;
        public byte[] buf = new byte[2048];
    }
    
    public static class Request {
        public String method;
        public String uri = "/api/test";
    }
}
