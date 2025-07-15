package com.taobao.arthas.core.command.trace;

import java.util.List;

/**
 * 探针配置数据结构
 */
public class ProbeConfig {
    
    private String name;
    private String description;
    private boolean enabled = true;
    private List<MetricConfig> metrics;
    private OutputConfig output;
    private List<FilterConfig> filters;

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

    public List<MetricConfig> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricConfig> metrics) {
        this.metrics = metrics;
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

    /**
     * 指标配置
     */
    public static class MetricConfig {
        private String name;
        private String description;
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
        private String className;
        private List<String> methods;
        private String classAnnotation;
        private String methodAnnotation;

        // 构造函数
        public TargetConfig() {}

        public TargetConfig(String className, List<String> methods) {
            this.className = className;
            this.methods = methods;
        }

        // Getters and Setters
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

        // 构造函数
        public OutputConfig() {}

        public OutputConfig(String type, String template) {
            this.type = type;
            this.template = template;
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
