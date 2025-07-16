package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.view.Ansi;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 探针配置数据结构
 */
public class ProbeConfig {
    
    private String name;
    private String description;
    private boolean enabled = true;
    private List<TargetConfig> targets;
    private List<MetricConfig> metrics;
    private OutputConfig output;
    private List<FilterConfig> filters;
    private final HashMap<String, List<MetricConfig>> metricConfigMap = new HashMap<>();

    // 构造函数
    public ProbeConfig() {}

    public ProbeConfig(String name, String description, boolean enabled) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public HashMap<String, List<MetricConfig>> getMetricConfigMap() {
        return metricConfigMap;
    }


    public List<TargetConfig> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetConfig> targets) {
        this.targets = targets;
    }

    public List<MetricConfig> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricConfig> metrics) {
        this.metrics = metrics;
    }

    private void initMetricConfigMap(List<MetricConfig> metrics) {
        metricConfigMap.clear();
        for (MetricConfig metric : metrics) {
            if (Objects.nonNull(metric)) {
                List<MetricConfig> metricList = metricConfigMap.computeIfAbsent(metric.getTargetId(), k -> new java.util.ArrayList<>());
                metricList.add(metric);
            }
        }
        System.out.println("initMetricConfigMap: " + metrics);
    }

    public OutputConfig getOutput() {
        return output;
    }

    public void setOutput(OutputConfig output) {
        this.output = output;
    }

    public List<FilterConfig> getFilters() {
        return filters;
    }

    public void setFilters(List<FilterConfig> filters) {
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "ProbeConfig{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", metricsCount=" + (metrics != null ? metrics.size() : 0) +
                '}';
    }

    public List<MetricConfig> getMetrics(String methodSignature) {
        if (metricConfigMap.isEmpty()) {
            if (metrics != null) {
                initMetricConfigMap(metrics);
            }
        }
        return metricConfigMap.get(methodSignature);
    }

    /**
     * 指标配置
     */
    public static class MetricConfig {
        private String name;
        private String description;
        private String targetId; // 支持旧格式的targetId字段
        private List<TargetConfig> targets;
        private String source;
        private String formula;
        private String type;
        private String unit;
        private String capturePoint;

        // 构造函数
        public MetricConfig() {}

        public MetricConfig(String name, String description, String type) {
            this.name = name;
            this.description = description;
            this.type = type;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

        public List<TargetConfig> getTargets() {
            return targets;
        }

        public void setTargets(List<TargetConfig> targets) {
            this.targets = targets;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getFormula() {
            return formula;
        }

        public void setFormula(String formula) {
            this.formula = formula;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getCapturePoint() {
            return capturePoint;
        }

        public void setCapturePoint(String capturePoint) {
            this.capturePoint = capturePoint;
        }

        @Override
        public String toString() {
            return "MetricConfig{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", capturePoint='" + capturePoint + '\'' +
                    '}';
        }
    }

    /**
     * 目标配置
     */
    public static class TargetConfig {
        private String id; // 支持旧格式的id字段
        private String className;
        private List<String> methods;
        private String description; // 支持旧格式的description字段
        private String classAnnotation;
        private String methodAnnotation;

        // 构造函数
        public TargetConfig() {}

        public TargetConfig(String className, List<String> methods) {
            this.className = className;
            this.methods = methods;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getClassAnnotation() {
            return classAnnotation;
        }

        public void setClassAnnotation(String classAnnotation) {
            this.classAnnotation = classAnnotation;
        }

        public String getMethodAnnotation() {
            return methodAnnotation;
        }

        public void setMethodAnnotation(String methodAnnotation) {
            this.methodAnnotation = methodAnnotation;
        }

        @Override
        public String toString() {
            return "TargetConfig{" +
                    "className='" + className + '\'' +
                    ", methods=" + methods +
                    '}';
        }
    }

    /**
     * 输出配置
     */
    public static class OutputConfig {
        private String type;
        private String template;
        private String colorName = Ansi.Color.DEFAULT.name();
        // 构造函数
        public OutputConfig() {}

        public OutputConfig(String type, String template) {
            this.type = type;
            this.template = template;
        }

        public String getColorName() {
            return colorName;
        }

        public void setColorName(String colorName) {
            this.colorName = colorName;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        @Override
        public String toString() {
            return "OutputConfig{" +
                    "type='" + type + '\'' +
                    ", template='" + template + '\'' +
                    '}';
        }
    }

    /**
     * 过滤配置
     */
    public static class FilterConfig {
        private String name;
        private String condition;

        // 构造函数
        public FilterConfig() {}

        public FilterConfig(String name, String condition) {
            this.name = name;
            this.condition = condition;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        @Override
        public String toString() {
            return "FilterConfig{" +
                    "name='" + name + '\'' +
                    ", condition='" + condition + '\'' +
                    '}';
        }
    }

    /**
     * 方法配置类（用于入口和出口方法配置）
     */
    public static class MethodConfig {
        private String className;
        private String methodName;

        public MethodConfig() {}

        public MethodConfig(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }
    }
}
