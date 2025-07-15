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

    private static final String PROBE_CONFIG_DIR = "/probes/";

    private final Map<String, ProbeConfig> probeConfigs = new ConcurrentHashMap<>();
    private final Map<String, Object> activeProbes = new ConcurrentHashMap<>();
    private boolean initialized = false;

    /**
     * 加载内置探针配置
     * 从JSON配置文件中读取
     */
    public List<ProbeConfig> loadBuiltinProbes() {
        List<ProbeConfig> configs = new ArrayList<>();

        // 配置驱动：动态获取探针配置文件
        String[] probeFiles = getProbeConfigFiles();

        for (String probeFile : probeFiles) {
            try {
                ProbeConfig config = loadProbeConfigFromResource(PROBE_CONFIG_DIR + probeFile);
                if (config != null) {
                    configs.add(config);
                    probeConfigs.put(config.getName(), config);
                }
            } catch (Exception e) {
                System.err.println("Failed to load probe config: " + probeFile + ", error: " + e.getMessage());
                // 配置驱动：如果JSON文件加载失败，不使用备用配置，直接跳过
                // 这确保了所有逻辑都严格基于配置文件
            }
        }

        return configs;
    }

    /**
     * 动态获取探针配置文件列表（配置驱动）
     * 支持JAR包和文件系统两种环境
     */
    private String[] getProbeConfigFiles() {
        try {
            java.net.URL url = getClass().getResource(PROBE_CONFIG_DIR);
            if (url != null) {
                String protocol = url.getProtocol();

                if ("file".equals(protocol)) {
                    // 文件系统环境
                    java.io.File dir = new java.io.File(url.toURI());
                    if (dir.exists() && dir.isDirectory()) {
                        String[] files = dir.list((dir1, name) -> name.endsWith(".json"));
                        if (files != null && files.length > 0) {
                            return files;
                        }
                    }
                } else if ("jar".equals(protocol)) {
                    // JAR包环境
                    return getProbeConfigFilesFromJar(url);
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning probe config directory: " + e.getMessage());
            e.printStackTrace();
        }

        // 如果动态扫描失败，返回已知的配置文件列表
        return getKnownProbeConfigFiles();
    }

    /**
     * 从JAR包中获取探针配置文件列表
     */
    private String[] getProbeConfigFilesFromJar(java.net.URL url) {
        try {
            java.net.URLConnection connection = url.openConnection();
            if (connection instanceof java.net.JarURLConnection) {
                java.net.JarURLConnection jarConnection = (java.net.JarURLConnection) connection;
                java.util.jar.JarFile jarFile = jarConnection.getJarFile();

                String entryName = jarConnection.getEntryName();
                if (entryName != null && !entryName.endsWith("/")) {
                    entryName += "/";
                }

                java.util.List<String> configFiles = new java.util.ArrayList<>();
                java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (entryName != null && name.startsWith(entryName) &&
                        name.endsWith(".json") && !name.equals(entryName)) {
                        // 只取文件名，不包含路径
                        String fileName = name.substring(entryName.length());
                        if (!fileName.contains("/")) {  // 确保是直接子文件，不是子目录中的文件
                            configFiles.add(fileName);
                        }
                    }
                }

                return configFiles.toArray(new String[0]);
            }
        } catch (Exception e) {
            System.err.println("Error reading probe configs from JAR: " + e.getMessage());
        }

        return new String[]{};
    }

    /**
     * 返回已知的探针配置文件列表（备选方案）
     */
    private String[] getKnownProbeConfigFiles() {
        return new String[]{
            "http-server-probe.json",
            "database-probe.json",
            "file-operations-probe.json",
            "http-client-probe.json"
        };
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
            activeProbes.put(config.getName(), null);
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
