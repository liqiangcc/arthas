#!/bin/bash

# 测试 trace-flow --reload-probes 功能的脚本

echo "==========================================="
echo "trace-flow --reload-probes 功能测试"
echo "==========================================="

# 设置测试环境
ARTHAS_HOME="${ARTHAS_HOME:-$(pwd)}"
PROBES_DIR="$ARTHAS_HOME/probes"

echo "ARTHAS_HOME: $ARTHAS_HOME"
echo "探针配置目录: $PROBES_DIR"

# 创建测试探针配置目录
if [ ! -d "$PROBES_DIR" ]; then
    echo "创建探针配置目录: $PROBES_DIR"
    mkdir -p "$PROBES_DIR"
fi

# 创建测试探针配置文件
echo "创建测试探针配置文件..."

cat > "$PROBES_DIR/test-probe.json" << 'EOF'
{
  "name": "Test探针",
  "description": "用于测试重新加载功能的探针",
  "enabled": true,
  "metrics": [
    {
      "name": "testMetric",
      "description": "测试指标",
      "targets": [
        {
          "className": "java.lang.String",
          "methods": ["toString"]
        }
      ],
      "source": "returnValue",
      "type": "string",
      "capturePoint": "after"
    }
  ],
  "output": {
    "type": "TEST",
    "template": "[TEST] Result: ${testMetric}"
  }
}
EOF

echo "✓ 创建了测试探针配置: $PROBES_DIR/test-probe.json"

# 测试步骤
echo ""
echo "==========================================="
echo "测试步骤"
echo "==========================================="

echo "1. 首次列出探针（应该只有内置的deep_call探针）:"
echo "   tf --list-probes"
echo ""

echo "2. 重新加载探针配置（应该加载新的test-probe）:"
echo "   tf --reload-probes"
echo ""

echo "3. 再次列出探针（应该包含test-probe）:"
echo "   tf --list-probes"
echo ""

echo "4. 显示新探针的配置:"
echo "   tf --show-config \"Test探针\""
echo ""

echo "5. 修改探针配置文件，然后重新加载:"
echo "   # 编辑 $PROBES_DIR/test-probe.json"
echo "   tf --reload-probes"
echo ""

echo "6. 删除探针配置文件，然后重新加载:"
echo "   rm $PROBES_DIR/test-probe.json"
echo "   tf --reload-probes"
echo ""

# 创建另一个测试配置文件
cat > "$PROBES_DIR/another-test-probe.json" << 'EOF'
{
  "name": "Another Test探针",
  "description": "另一个测试探针",
  "enabled": false,
  "metrics": [
    {
      "name": "anotherMetric",
      "description": "另一个测试指标",
      "targets": [
        {
          "className": "java.lang.Object",
          "methods": ["hashCode"]
        }
      ],
      "source": "returnValue",
      "type": "int",
      "capturePoint": "after"
    }
  ],
  "output": {
    "type": "ANOTHER_TEST",
    "template": "[ANOTHER_TEST] Hash: ${anotherMetric}"
  }
}
EOF

echo "✓ 创建了另一个测试探针配置: $PROBES_DIR/another-test-probe.json"

echo ""
echo "==========================================="
echo "预期结果"
echo "==========================================="
echo "- 初始状态：只有deep_call探针"
echo "- 重新加载后：包含deep_call + test-probe + another-test-probe"
echo "- test-probe状态：Enabled"
echo "- another-test-probe状态：Disabled"
echo "- 删除文件后重新加载：只剩下deep_call探针"

echo ""
echo "==========================================="
echo "清理测试文件"
echo "==========================================="
echo "测试完成后，可以运行以下命令清理测试文件："
echo "rm -f $PROBES_DIR/test-probe.json"
echo "rm -f $PROBES_DIR/another-test-probe.json"

echo ""
echo "测试环境准备完成！现在可以使用 tf --reload-probes 命令进行测试。"
