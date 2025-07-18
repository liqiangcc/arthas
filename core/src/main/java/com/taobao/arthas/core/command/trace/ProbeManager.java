package com.taobao.arthas.core.command.trace;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.taobao.arthas.core.advisor.ArthasMethod;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 探针管理器 - 负责加载和管理探针配置
 * 从JSON配置文件中读取探针配置
 */
public class ProbeManager {

    private static final String INTERNAL_PROBE_CONFIG_DIR = "/probes/";
    private static final String EXTERNAL_PROBE_CONFIG_DIR = "./probes";

    private final Map<String, ProbeConfig> probeConfigs = new ConcurrentHashMap<>();
    private final Map<String, Object> activeProbes = new ConcurrentHashMap<>();
    private boolean initialized = false;
    private HashMap<String, ProbeConfig> methodSignature2ProbeConfig = new HashMap<>();

    private static final ProbeManager instance = new ProbeManager();
    private Set<String> enableProbeMethods = new HashSet<>();
    private Set<String> enableProbeClassNames = new HashSet<>();;
    private HashMap<String, List<ProbeConfig.MetricConfig>> methodSignature2MetricConfig = new HashMap<>();

    public ProbeManager() {
        System.out.println("[DEBUG] ProbeManager created");
        new RuntimeException().printStackTrace(System.err);
    }

    public static ProbeManager getInstance() {
        return instance;
    }

    public  List<ProbeConfig>  initialize() {
        return initialize(false);
    }
    /**
     * 初始化探针管理器
     */
    public List<ProbeConfig> initialize(boolean reload) {
        if (initialized && !reload) {
            return new ArrayList<>(probeConfigs.values());
        }
        System.out.println("Initializing probe manager@" + this.hashCode() + ",initialized = " + initialized + ",reload = " + reload);
        new RuntimeException().printStackTrace(System.err);
        clear();

        List<ProbeConfig> configs = new ArrayList<>();

        // 1. 加载内置探针配置文件（只有deep_call.json）
        loadInternalProbeConfigs(configs);

        // 2. 加载外部探针配置文件（从启动目录下的probes文件夹）
        loadExternalProbeConfigs(configs);

        initialized = true;
        System.out.println("ProbeManager initialized completed, probe count = " + probeConfigs.size());
        System.out.println("Method…  probeConfigs: " + JSONObject.toJSONString(probeConfigs));
        return configs;
    }

    private void clear() {
        probeConfigs.clear();
        methodSignature2ProbeConfig.clear();
        methodSignature2MetricConfig.clear();
        activeProbes.clear();
        enableProbeMethods.clear();
        enableProbeClassNames.clear();
    }

    /**
     * 加载内置探针配置（从JAR包中的/probes目录）
     */
    private void loadInternalProbeConfigs(List<ProbeConfig> configs) {
        String[] internalProbeFiles = getInternalProbeConfigFiles();

        for (String probeFile : internalProbeFiles) {
            try {
                ProbeConfig config = loadProbeConfigFromResource(INTERNAL_PROBE_CONFIG_DIR + probeFile);
                if (config != null) {
                    configs.add(config);
                    probeConfigs.put(config.getName(), config);
                    System.out.println("Loaded internal probe config: " + probeFile);
                }
            } catch (Exception e) {
                System.err.println("Failed to load internal probe config: " + probeFile + ", error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * 加载外部探针配置文件（从ARTHAS_HOME目录下的probes文件夹）
     */
    private void loadExternalProbeConfigs(List<ProbeConfig> configs) {
        try {
            java.io.File externalProbeDir = getExternalProbeDirectory();
            if (externalProbeDir == null) {
                return;
            }
            if (!externalProbeDir.exists()) {
                System.out.println("External probe directory not found: " + externalProbeDir.getAbsolutePath());
                System.out.println("You can create this directory and place probe config files (.json) there for external probes.");
                return;
            }

            if (!externalProbeDir.isDirectory()) {
                System.err.println("External probe path is not a directory: " + externalProbeDir.getAbsolutePath());
                return;
            }

            java.io.File[] jsonFiles = externalProbeDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles == null || jsonFiles.length == 0) {
                System.out.println("No external probe config files found in: " + externalProbeDir.getAbsolutePath());
                return;
            }

            System.out.println("Loading external probe configs from: " + externalProbeDir.getAbsolutePath());
            for (java.io.File jsonFile : jsonFiles) {
                try {
                    ProbeConfig config = loadProbeConfigFromFile(jsonFile);
                    if (config != null) {
                        configs.add(config);
                        probeConfigs.put(config.getName(), config);
                        System.out.println("Loaded external probe config: " + jsonFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load external probe config: " + jsonFile.getAbsolutePath() + ", error: " + e.getMessage());
                    e.printStackTrace(System.err);
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning external probe directory: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * 重置探针管理器状态，用于重新加载配置
     */
    public void reset() {
        initialized = false;
        probeConfigs.clear();
        activeProbes.clear();
        methodSignature2ProbeConfig.clear();
        methodSignature2MetricConfig.clear();
        System.out.println("ProbeManager reset completed");
    }

    /**
     * 获取外部探针配置目录
     * 从ArthasBootstrap获取arthasHome路径
     */
    private java.io.File getExternalProbeDirectory() {
        try {
            // 1. 尝试从ArthasBootstrap获取arthasHome
            String arthasHome = getArthasHomeFromBootstrap();
            if (arthasHome != null && !arthasHome.trim().isEmpty()) {
                java.io.File arthasHomeDir = new java.io.File(arthasHome.trim());
                if (arthasHomeDir.exists() && arthasHomeDir.isDirectory()) {
                    java.io.File probeDir = new java.io.File(arthasHomeDir, EXTERNAL_PROBE_CONFIG_DIR);
                    System.out.println("Using Arthas home probe directory: " + probeDir.getAbsolutePath());
                    return probeDir;
                } else {
                    System.err.println("Arthas home directory does not exist: " + arthasHome);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get arthas home from bootstrap: " + e.getMessage());
        }

        // 2. 降级：尝试从系统属性获取
        String arthasHomeProp = System.getProperty("arthas.home");
        if (arthasHomeProp != null && !arthasHomeProp.trim().isEmpty()) {
            java.io.File arthasHomeDir = new java.io.File(arthasHomeProp.trim());
            if (arthasHomeDir.exists() && arthasHomeDir.isDirectory()) {
                java.io.File probeDir = new java.io.File(arthasHomeDir, EXTERNAL_PROBE_CONFIG_DIR);
                System.out.println("Using arthas.home system property probe directory: " + probeDir.getAbsolutePath());
                return probeDir;
            } else {
                System.err.println("arthas.home directory does not exist: " + arthasHomeProp);
            }
        }

        // 3. 最后降级到当前目录
        java.io.File currentDirProbeDir = new java.io.File(EXTERNAL_PROBE_CONFIG_DIR);
        System.out.println("Using current directory probe directory: " + currentDirProbeDir.getAbsolutePath());
        System.out.println("Tip: Arthas home not found, using current directory as fallback.");
        return currentDirProbeDir;
    }

    /**
     * 从ArthasBootstrap获取arthasHome路径
     */
    private String getArthasHomeFromBootstrap() {
        try {
            // 获取ArthasBootstrap实例并调用arthasHome方法
            com.taobao.arthas.core.server.ArthasBootstrap bootstrap =
                com.taobao.arthas.core.server.ArthasBootstrap.getInstance();

            // 通过反射调用私有的arthasHome方法
            java.lang.reflect.Method arthasHomeMethod = bootstrap.getClass().getDeclaredMethod("arthasHome");
            arthasHomeMethod.setAccessible(true);
            return (String) arthasHomeMethod.invoke(bootstrap);
        } catch (Exception e) {
            System.err.println("Failed to get arthas home from bootstrap instance: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取内置探针配置文件列表（从JAR包中的/probes目录）
     */
    private String[] getInternalProbeConfigFiles() {
        try {
            java.net.URL url = getClass().getResource(INTERNAL_PROBE_CONFIG_DIR);
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
            System.err.println("Error scanning internal probe config directory: " + e.getMessage());
            e.printStackTrace();
        }

        // 如果扫描失败，返回空数组（不使用备用列表）
        return new String[]{};
    }

    /**
     * 从文件加载探针配置
     */
    private ProbeConfig loadProbeConfigFromFile(java.io.File file) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            // Java 8兼容的读取方式
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            String content = new String(buffer, java.nio.charset.StandardCharsets.UTF_8);

            // 解析JSON
            ProbeConfig config = JSON.parseObject(content, ProbeConfig.class);

            // 处理旧格式配置（将targetId转换为targets）
            processLegacyFormat(config);

            // 验证配置
            validateProbeConfig(config);

            return config;
        } catch (Exception e) {
            System.err.println("Error loading probe config from file: " + file.getAbsolutePath());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 动态获取探针配置文件列表（配置驱动）
     * 支持JAR包和文件系统两种环境
     */
    private String[] getProbeConfigFiles() {
        try {
            java.net.URL url = getClass().getResource(INTERNAL_PROBE_CONFIG_DIR);
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
     * 返回已知的内置探针配置文件列表（备选方案）
     * 只包含JAR包中应该存在的配置文件
     */
    private String[] getKnownProbeConfigFiles() {
        return new String[]{
            "deep_call.json"
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

            // 处理旧格式配置（将targetId转换为targets）
            processLegacyFormat(config);

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
     * 处理旧格式配置（将targetId转换为targets）
     */
    private void processLegacyFormat(ProbeConfig config) {
        if (config == null || config.getMetrics() == null) {
            return;
        }

        // 如果有独立的targets数组，处理targetId引用
        if (config.getTargets() != null && !config.getTargets().isEmpty()) {
            // 创建id到target的映射
            Map<String, ProbeConfig.TargetConfig> targetMap = new HashMap<>();
            for (ProbeConfig.TargetConfig target : config.getTargets()) {
                if (target.getId() != null) {
                    targetMap.put(target.getId(), target);
                }
            }

            // 为每个使用targetId的metric设置对应的targets
            for (ProbeConfig.MetricConfig metric : config.getMetrics()) {
                if (metric.getTargetId() != null && metric.getTargets() == null) {
                    ProbeConfig.TargetConfig target = targetMap.get(metric.getTargetId());
                    if (target != null) {
                        // 创建新的TargetConfig，不包含id字段
                        ProbeConfig.TargetConfig newTarget = new ProbeConfig.TargetConfig();
                        newTarget.setClassName(target.getClassName());
                        newTarget.setMethods(target.getMethods());
                        newTarget.setClassAnnotation(target.getClassAnnotation());
                        newTarget.setMethodAnnotation(target.getMethodAnnotation());

                        List<ProbeConfig.TargetConfig> targets = new ArrayList<>();
                        targets.add(newTarget);
                        metric.setTargets(targets);
                    }
                }
            }
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
        // 如果还没有加载，先初始化
        if (probeConfigs.isEmpty()) {
            initialize();
        }

        return probeConfigs.get(probeName);
    }

    /**
     * 获取所有探针配置
     */
    public Map<String, ProbeConfig> getAllProbeConfigs() {
        if (probeConfigs.isEmpty()) {
            initialize();
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

        // 初始化探针配置
        List<ProbeConfig> configs = initialize();
        
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
            activeProbes.put(config.getName(), config);
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

    public ProbeConfig getProbeConfig(Class<?> clazz, ArthasMethod method) {
        String methodSignature = buildMethodSignature(clazz, method);
        if (methodSignature2ProbeConfig.isEmpty()) {
            for (ProbeConfig config : probeConfigs.values()) {
                List<ProbeConfig.TargetConfig> targets = config.getTargets();
                if (Objects.nonNull( targets)) {
                    for (ProbeConfig.TargetConfig target : targets) {
                        for (String targetMethod : target.getMethods()) {
                            this.methodSignature2ProbeConfig.put( target.getClassName() + "." + targetMethod, config);
                        }
                    }
                }
            }
        }
        return this.methodSignature2ProbeConfig.get(methodSignature);
    }

    private static String buildMethodSignature(Class<?> clazz, ArthasMethod method) {
        String clazzName = clazz.getName();
        String methodName = method.getName();
        return clazzName + "." + methodName;
    }

    public List<ProbeConfig> reInitialize() {
        return initialize(true);
    }

    public Set<String> getEnableProbeMethods() {
        if (enableProbeMethods.isEmpty()) {
            for (ProbeConfig config : getAllProbeConfigs().values()) {
                if (config.isEnabled()) {
                    for (ProbeConfig.TargetConfig target : config.getTargets()) {
                        this.enableProbeMethods.addAll(target.getMethods());
                    }
                }
            }
        }
        return this.enableProbeMethods;
    }

    public Set<String> getEnableProbeClassNames() {
        if (enableProbeClassNames.isEmpty()) {
            for (ProbeConfig config : getAllProbeConfigs().values()) {
                if (config.isEnabled()) {
                    for (ProbeConfig.TargetConfig target : config.getTargets()) {
                        this.enableProbeClassNames.add(target.getClassName());
                    }
                }
            }
        }
        return this.enableProbeClassNames;
    }

    public List<ProbeConfig.MetricConfig> getMetricConfigs(Class<?> clazz, ArthasMethod method) {
        String methodSignature = buildMethodSignature(clazz, method);
        if (methodSignature2MetricConfig.isEmpty()) {
            for (ProbeConfig config : getAllProbeConfigs().values()) {
                if (config.isEnabled()) {
                    for (ProbeConfig.TargetConfig target : config.getTargets()) {
                        for (String targetMethod : target.getMethods()) {
                            String targetMethodSignature = target.getClassName() + "." + targetMethod;
                            methodSignature2MetricConfig.computeIfAbsent(targetMethodSignature, k -> new ArrayList<>());
                            methodSignature2MetricConfig.get(targetMethodSignature).addAll(config.getMetrics(targetMethodSignature));
                        }
                    }
                }
            }
        }
        return methodSignature2MetricConfig.get(methodSignature);
    }
}
