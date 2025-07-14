package com.taobao.arthas.core.command.trace;

import com.alibaba.fastjson2.JSON;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 探针管理器 - 负责加载和管理探针配置
 * 从JSON配置文件中读取探针配置
 */
public class ProbeManager {

    private static final String[] BUILTIN_PROBE_FILES = {
        "/probes/database-probe.json",
        "/probes/http-server-probe.json",
        "/probes/http-client-probe.json",
        "/probes/file-operations-probe.json"
    };

    private final Map<String, ProbeConfig> probeConfigs = new ConcurrentHashMap<>();
    private final Map<String, Object> activeProbes = new ConcurrentHashMap<>();
    private boolean initialized = false;

    /**
     * 加载内置探针配置
     * 从JSON配置文件中读取
     */
    public List<ProbeConfig> loadBuiltinProbes() {
        List<ProbeConfig> configs = new ArrayList<>();

        for (String probeFile : BUILTIN_PROBE_FILES) {
            try {
                ProbeConfig config = loadProbeConfigFromResource(probeFile);
                if (config != null) {
                    configs.add(config);
                    probeConfigs.put(config.getName(), config);
                }
            } catch (Exception e) {
                System.err.println("Failed to load probe config: " + probeFile + ", error: " + e.getMessage());
                // 如果JSON文件加载失败，使用备用配置
                ProbeConfig fallbackConfig = createFallbackConfig(probeFile);
                if (fallbackConfig != null) {
                    configs.add(fallbackConfig);
                    probeConfigs.put(fallbackConfig.getName(), fallbackConfig);
                }
            }
        }

        return configs;
    }

    /**
     * 从资源文件加载探针配置
     */
    private ProbeConfig loadProbeConfigFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Probe config file not found: " + resourcePath);
                return null;
            }

            // 读取文件内容
            byte[] bytes = new byte[is.available()];
            int totalBytesRead = 0;
            while (totalBytesRead < bytes.length) {
                int bytesRead = is.read(bytes, totalBytesRead, bytes.length - totalBytesRead);
                if (bytesRead == -1) break;
                totalBytesRead += bytesRead;
            }

            String content = new String(bytes, 0, totalBytesRead, StandardCharsets.UTF_8);

            // 解析JSON
            ProbeConfig config = JSON.parseObject(content, ProbeConfig.class);

            // 验证配置
            validateProbeConfig(config);

            System.out.println("Successfully loaded probe config: " + resourcePath);
            return config;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read probe config file: " + resourcePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse probe config file: " + resourcePath, e);
        }
    }

    /**
     * 验证探针配置
     */
    private void validateProbeConfig(ProbeConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Probe config cannot be null");
        }

        if (config.getName() == null || config.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Probe name cannot be empty");
        }

        if (config.getMetrics() == null || config.getMetrics().isEmpty()) {
            throw new IllegalArgumentException("Probe must define at least one metric");
        }

        // 验证指标配置
        for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
            validateMetricConfig(metric);
        }
    }

    /**
     * 验证指标配置
     */
    private void validateMetricConfig(ProbeConfig.MetricConfig metric) {
        if (metric.getName() == null || metric.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name cannot be empty");
        }

        if (metric.getType() == null || metric.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Metric type cannot be empty: " + metric.getName());
        }

        // 检查source和formula不能同时为空
        boolean hasSource = metric.getSource() != null && !metric.getSource().trim().isEmpty();
        boolean hasFormula = metric.getFormula() != null && !metric.getFormula().trim().isEmpty();

        if (!hasSource && !hasFormula) {
            throw new IllegalArgumentException("Metric must define either source or formula: " + metric.getName());
        }

        // 如果有source，必须有targets和capturePoint
        if (hasSource) {
            if (metric.getTargets() == null || metric.getTargets().isEmpty()) {
                throw new IllegalArgumentException("Source metric must define targets: " + metric.getName());
            }
            if (metric.getCapturePoint() == null || metric.getCapturePoint().trim().isEmpty()) {
                throw new IllegalArgumentException("Source metric must define capturePoint: " + metric.getName());
            }
        }
    }

    /**
     * 创建备用配置（当JSON文件加载失败时使用）
     */
    private ProbeConfig createFallbackConfig(String probeFile) {
        String probeName = extractProbeNameFromFile(probeFile);
        return createMockProbeConfig(probeName, "Fallback configuration for " + probeName);
    }

    /**
     * 从文件路径提取探针名称
     */
    private String extractProbeNameFromFile(String probeFile) {
        if (probeFile.contains("database")) return "Database Probe";
        if (probeFile.contains("http-server")) return "HTTP Server Probe";
        if (probeFile.contains("http-client")) return "HTTP Client Probe";
        if (probeFile.contains("file-operations")) return "File Operations Probe";
        return "Unknown Probe";
    }

    /**
     * 创建模拟探针配置（备用方案）
     */
    private ProbeConfig createMockProbeConfig(String name, String description) {
        ProbeConfig config = new ProbeConfig();
        config.setName(name);
        config.setDescription(description);
        config.setEnabled(true);
        
        // 创建基础指标
        List<ProbeConfig.MetricConfig> metrics = new ArrayList<>();
        
        ProbeConfig.MetricConfig startTime = new ProbeConfig.MetricConfig();
        startTime.setName("startTime");
        startTime.setDescription("Start time");
        startTime.setSource("startTime");
        startTime.setType("long");
        startTime.setCapturePoint("before");
        metrics.add(startTime);

        ProbeConfig.MetricConfig endTime = new ProbeConfig.MetricConfig();
        endTime.setName("endTime");
        endTime.setDescription("End time");
        endTime.setSource("endTime");
        endTime.setType("long");
        endTime.setCapturePoint("after");
        metrics.add(endTime);

        ProbeConfig.MetricConfig executionTime = new ProbeConfig.MetricConfig();
        executionTime.setName("executionTime");
        executionTime.setDescription("Execution time");
        executionTime.setFormula("metrics.endTime - metrics.startTime");
        executionTime.setType("long");
        metrics.add(executionTime);

        ProbeConfig.MetricConfig threadName = new ProbeConfig.MetricConfig();
        threadName.setName("threadName");
        threadName.setDescription("Thread name");
        threadName.setSource("threadName");
        threadName.setType("string");
        threadName.setCapturePoint("before");
        metrics.add(threadName);
        
        config.setMetrics(metrics);
        return config;
    }

    /**
     * 获取探针配置
     */
    public ProbeConfig getProbeConfig(String probeName) {
        // 如果还没有加载，先加载内置探针
        if (probeConfigs.isEmpty()) {
            loadBuiltinProbes();
        }
        
        return probeConfigs.get(probeName);
    }

    /**
     * 获取所有探针配置
     */
    public Map<String, ProbeConfig> getAllProbeConfigs() {
        if (probeConfigs.isEmpty()) {
            loadBuiltinProbes();
        }
        return new HashMap<>(probeConfigs);
    }

    /**
     * 初始化所有探针
     */
    public void initializeProbes() {
        if (initialized) {
            return;
        }

        // 加载内置探针配置
        List<ProbeConfig> configs = loadBuiltinProbes();
        
        // 初始化启用的探针
        for (ProbeConfig config : configs) {
            if (config.isEnabled()) {
                try {
                    initializeProbe(config);
                } catch (Exception e) {
                    System.err.println("初始化探针失败: " + config.getName() + ", 错误: " + e.getMessage());
                }
            }
        }

        initialized = true;
    }

    /**
     * 初始化单个探针
     */
    private void initializeProbe(ProbeConfig config) {
        try {
            // 阶段2：创建真实的配置驱动拦截器
            ConfigurableMethodInterceptor interceptor = new ConfigurableMethodInterceptor(config);
            activeProbes.put(config.getName(), interceptor);

            // 注册到拦截器管理器
            InterceptorManager.getInstance().registerInterceptor(interceptor);

            System.out.println("Initialized probe: " + config.getName());
        } catch (Exception e) {
            System.err.println("Failed to initialize probe: " + config.getName() + ", error: " + e.getMessage());
        }
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取活跃的探针数量
     */
    public int getActiveProbeCount() {
        return activeProbes.size();
    }

    /**
     * 重置管理器状态（主要用于测试）
     */
    public void reset() {
        probeConfigs.clear();
        activeProbes.clear();
        initialized = false;
    }

    /**
     * 模拟探针拦截器（阶段1使用）
     */
    private static class MockProbeInterceptor implements ProbeInterceptor {
        private final ProbeConfig config;

        public MockProbeInterceptor(ProbeConfig config) {
            this.config = config;
        }

        @Override
        public String getName() {
            return config.getName();
        }

        @Override
        public boolean isEnabled() {
            return config.isEnabled();
        }
    }

    /**
     * 探针拦截器接口
     */
    public interface ProbeInterceptor {
        String getName();
        boolean isEnabled();
    }
}
