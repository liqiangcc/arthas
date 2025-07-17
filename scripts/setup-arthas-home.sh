#!/bin/bash

# Arthas Home 设置脚本
# 用于设置ARTHAS_HOME环境变量和创建外部探针配置目录

echo "==========================================="
echo "Arthas Home 设置脚本"
echo "==========================================="

# 获取当前脚本所在目录的父目录作为ARTHAS_HOME
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARTHAS_HOME="$(dirname "$SCRIPT_DIR")"

echo "检测到的Arthas目录: $ARTHAS_HOME"

# 创建probes目录
PROBES_DIR="$ARTHAS_HOME/probes"
if [ ! -d "$PROBES_DIR" ]; then
    echo "创建外部探针配置目录: $PROBES_DIR"
    mkdir -p "$PROBES_DIR"
else
    echo "外部探针配置目录已存在: $PROBES_DIR"
fi

# 复制示例配置文件
if [ -f "$ARTHAS_HOME/probes/http-server-probe.json" ]; then
    echo "示例配置文件已存在"
else
    echo "复制示例配置文件到外部目录..."
    if [ -d "$ARTHAS_HOME/probes" ]; then
        # 这里的示例文件已经在项目根目录的probes文件夹中
        echo "示例配置文件位置: $ARTHAS_HOME/probes/"
    fi
fi

# 设置环境变量
echo ""
echo "==========================================="
echo "环境变量设置"
echo "==========================================="
echo "请将以下内容添加到您的shell配置文件中："
echo ""
echo "# Arthas Home"
echo "export ARTHAS_HOME=\"$ARTHAS_HOME\""
echo "export PATH=\"\$ARTHAS_HOME/bin:\$PATH\""
echo ""

# 检查当前环境变量
if [ -n "$ARTHAS_HOME" ]; then
    echo "当前ARTHAS_HOME: $ARTHAS_HOME"
else
    echo "当前未设置ARTHAS_HOME环境变量"
fi

echo ""
echo "==========================================="
echo "使用说明"
echo "==========================================="
echo "1. 将上述export命令添加到 ~/.bashrc 或 ~/.zshrc"
echo "2. 执行 'source ~/.bashrc' 或重新打开终端"
echo "3. 将自定义探针配置文件放入: $PROBES_DIR"
echo "4. 启动Arthas，系统会自动加载外部探针配置"
echo ""
echo "探针配置文件示例位置: $PROBES_DIR"
echo "内置探针配置位置: JAR包内的/probes/目录（只包含deep_call.json）"
