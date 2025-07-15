package com.taobao.arthas.core.command.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阶段1功能测试
 */
@DisplayName("阶段1基础框架测试")
public class Stage1Test {

    private ProbeManager probeManager;
    private SourceExpressionParser sourceParser;
    private FilterEngine filterEngine;
    private TraceManager traceManager;
    private OutputFormatter outputFormatter;

    @BeforeEach
    void setUp() {
        probeManager = new ProbeManager();
        sourceParser = new SourceExpressionParser();
        filterEngine = new FilterEngine();
        traceManager = TraceManager.getInstance();
        outputFormatter = new OutputFormatter();
    }

    @Test
    @DisplayName("测试命令行参数解析")
    void testCommandLineParameters() {
        TraceFlowCommand command = new TraceFlowCommand();
        
        // 测试默认值
        assertEquals(1, command.getCount());
        assertNull(command.getFilter());
        assertNull(command.getOutputFile());
        assertFalse(command.isVerbose());
        assertFalse(command.isListProbes());
        assertFalse(command.isHelp());
    }

    @Test
    @DisplayName("测试配置文件加载")
    void testProbeConfigLoading() {
        List<ProbeConfig> configs = probeManager.loadBuiltinProbes();
        
        // 验证加载了4个探针
        assertEquals(4, configs.size());
        
        // 验证探针名称
        assertTrue(configs.stream().anyMatch(c -> "Database探针".equals(c.getName())));
        assertTrue(configs.stream().anyMatch(c -> "HTTP Server探针".equals(c.getName())));
        assertTrue(configs.stream().anyMatch(c -> "HTTP Client探针".equals(c.getName())));
        assertTrue(configs.stream().anyMatch(c -> "File Operations探针".equals(c.getName())));
        
        // 验证所有探针都是启用状态
        assertTrue(configs.stream().allMatch(ProbeConfig::isEnabled));
    }

    @Test
    @DisplayName("测试探针配置验证")
    void testProbeConfigValidation() {
        ProbeConfig config = probeManager.getProbeConfig("Database探针");
        assertNotNull(config);
        assertEquals("Database探针", config.getName());
        assertEquals("监控JDBC数据库操作", config.getDescription());
        assertTrue(config.isEnabled());
        assertNotNull(config.getMetrics());
        assertFalse(config.getMetrics().isEmpty());
    }

    @Test
    @DisplayName("测试简单表达式解析")
    void testSimpleSourceExpressionParsing() {
        ExecutionContext context = ExecutionContext.createMockContext(1000L, 2000L);
        
        // 测试内置变量解析
        assertEquals(1000L, sourceParser.parse("startTime", context));
        assertEquals(2000L, sourceParser.parse("endTime", context));
        assertEquals(1000L, sourceParser.parse("executionTime", context));
        assertEquals("test-thread", sourceParser.parse("threadName", context));
    }

    @Test
    @DisplayName("测试表达式验证")
    void testExpressionValidation() {
        // 测试有效表达式
        assertDoesNotThrow(() -> sourceParser.validateExpression("startTime"));
        assertDoesNotThrow(() -> sourceParser.validateExpression("endTime"));
        assertDoesNotThrow(() -> sourceParser.validateExpression("executionTime"));
        assertDoesNotThrow(() -> sourceParser.validateExpression("threadName"));
        
        // 测试无效表达式
        assertThrows(UnsupportedOperationException.class, 
                () -> sourceParser.validateExpression("this.toString()"));
        assertThrows(IllegalArgumentException.class, 
                () -> sourceParser.validateExpression(""));
        assertThrows(IllegalArgumentException.class, 
                () -> sourceParser.validateExpression(null));
    }

    @Test
    @DisplayName("测试基础过滤功能")
    void testBasicFiltering() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("executionTime", 1500L);
        metrics.put("url", "/api/users");
        metrics.put("operationType", "SELECT");
        
        // 测试数值比较
        assertTrue(filterEngine.matches("executionTime > 1000", metrics));
        assertFalse(filterEngine.matches("executionTime > 2000", metrics));
        assertTrue(filterEngine.matches("executionTime < 2000", metrics));
        assertFalse(filterEngine.matches("executionTime < 1000", metrics));
        
        // 测试字符串比较
        assertTrue(filterEngine.matches("operationType == 'SELECT'", metrics));
        assertFalse(filterEngine.matches("operationType == 'INSERT'", metrics));
        
        // 测试字符串包含
        assertTrue(filterEngine.matches("url.contains('/api')", metrics));
        assertFalse(filterEngine.matches("url.contains('/admin')", metrics));
        
        // 测试字符串开始
        assertTrue(filterEngine.matches("url.startsWith('/api')", metrics));
        assertFalse(filterEngine.matches("url.startsWith('/admin')", metrics));
        
        // 测试特殊情况
        assertTrue(filterEngine.matches("true", metrics));
        assertFalse(filterEngine.matches("false", metrics));
        assertTrue(filterEngine.matches("", metrics)); // 空过滤条件匹配所有
        assertTrue(filterEngine.matches(null, metrics)); // null过滤条件匹配所有
    }

    @Test
    @DisplayName("测试过滤表达式验证")
    void testFilterValidation() {
        // 测试有效过滤表达式
        assertDoesNotThrow(() -> filterEngine.validateFilter("executionTime > 1000"));
        assertDoesNotThrow(() -> filterEngine.validateFilter("url.contains('/api')"));
        assertDoesNotThrow(() -> filterEngine.validateFilter("true"));
        assertDoesNotThrow(() -> filterEngine.validateFilter(""));
        assertDoesNotThrow(() -> filterEngine.validateFilter(null));
        
        // 测试无效过滤表达式
        assertThrows(IllegalArgumentException.class, 
                () -> filterEngine.validateFilter("complex && expression"));
    }

    @Test
    @DisplayName("测试跟踪管理")
    void testTraceManagement() {
        // 测试开始跟踪
        String traceId = traceManager.startTrace();
        assertNotNull(traceId);
        assertTrue(traceId.startsWith("trace-"));
        assertEquals(1, traceManager.getActiveTraceCount());
        
        // 测试结束跟踪
        TraceManager.TraceContext context = traceManager.endTrace(traceId);
        assertNotNull(context);
        assertEquals(traceId, context.getTraceId());
        assertTrue(context.isCompleted());
        assertEquals(0, traceManager.getActiveTraceCount());
    }

    @Test
    @DisplayName("测试输出格式化")
    void testOutputFormatting() {
        Map<String, Object> data = new HashMap<>();
        data.put("executionTime", 1500L);
        data.put("operationType", "SELECT");
        
        String output = outputFormatter.format(data);
        assertNotNull(output);
        assertTrue(output.contains("executionTime"));
        assertTrue(output.contains("1500"));
        assertTrue(output.contains("operationType"));
        assertTrue(output.contains("SELECT"));
    }

    @Test
    @DisplayName("测试探针管理器初始化")
    void testProbeManagerInitialization() {
        assertFalse(probeManager.isInitialized());
        assertEquals(0, probeManager.getActiveProbeCount());
        
        probeManager.initializeProbes();
        
        assertTrue(probeManager.isInitialized());
        assertEquals(4, probeManager.getActiveProbeCount()); // 4个启用的探针
    }

    @Test
    @DisplayName("测试执行上下文")
    void testExecutionContext() {
        ExecutionContext context = new ExecutionContext();
        assertNotNull(context.getThreadName());
        assertTrue(context.getStartTime() > 0);
        assertFalse(context.isCompleted());
        assertFalse(context.hasException());
        
        // 测试标记结束
        context.markEnd("result");
        assertTrue(context.isCompleted());
        assertEquals("result", context.getReturnValue());
        assertTrue(context.getExecutionTime() >= 0);
        
        // 测试异常标记
        ExecutionContext context2 = new ExecutionContext();
        Exception ex = new RuntimeException("test error");
        context2.markException(ex);
        assertTrue(context2.hasException());
        assertEquals(ex, context2.getException());
        assertTrue(context2.isCompleted());
    }

    @Test
    @DisplayName("测试内置变量识别")
    void testBuiltinVariableRecognition() {
        assertTrue(sourceParser.isBuiltinVariable("startTime"));
        assertTrue(sourceParser.isBuiltinVariable("endTime"));
        assertTrue(sourceParser.isBuiltinVariable("executionTime"));
        assertTrue(sourceParser.isBuiltinVariable("threadName"));
        
        assertFalse(sourceParser.isBuiltinVariable("this.toString()"));
        assertFalse(sourceParser.isBuiltinVariable("args[0]"));
        assertFalse(sourceParser.isBuiltinVariable(""));
        assertFalse(sourceParser.isBuiltinVariable(null));
    }
}
