@echo off
setlocal enabledelayedexpansion

REM 测试 trace-flow --reload-probes 功能的脚本

echo ===========================================
echo trace-flow --reload-probes 功能测试
echo ===========================================

REM 设置测试环境
if not defined ARTHAS_HOME set ARTHAS_HOME=%cd%
set PROBES_DIR=%ARTHAS_HOME%\probes

echo ARTHAS_HOME: %ARTHAS_HOME%
echo 探针配置目录: %PROBES_DIR%

REM 创建测试探针配置目录
if not exist "%PROBES_DIR%" (
    echo 创建探针配置目录: %PROBES_DIR%
    mkdir "%PROBES_DIR%"
)

REM 创建测试探针配置文件
echo 创建测试探针配置文件...

(
echo {
echo   "name": "Test探针",
echo   "description": "用于测试重新加载功能的探针",
echo   "enabled": true,
echo   "metrics": [
echo     {
echo       "name": "testMetric",
echo       "description": "测试指标",
echo       "targets": [
echo         {
echo           "className": "java.lang.String",
echo           "methods": ["toString"]
echo         }
echo       ],
echo       "source": "returnValue",
echo       "type": "string",
echo       "capturePoint": "after"
echo     }
echo   ],
echo   "output": {
echo     "type": "TEST",
echo     "template": "[TEST] Result: ${testMetric}"
echo   }
echo }
) > "%PROBES_DIR%\test-probe.json"

echo ✓ 创建了测试探针配置: %PROBES_DIR%\test-probe.json

REM 创建另一个测试配置文件
(
echo {
echo   "name": "Another Test探针",
echo   "description": "另一个测试探针",
echo   "enabled": false,
echo   "metrics": [
echo     {
echo       "name": "anotherMetric",
echo       "description": "另一个测试指标",
echo       "targets": [
echo         {
echo           "className": "java.lang.Object",
echo           "methods": ["hashCode"]
echo         }
echo       ],
echo       "source": "returnValue",
echo       "type": "int",
echo       "capturePoint": "after"
echo     }
echo   ],
echo   "output": {
echo     "type": "ANOTHER_TEST",
echo     "template": "[ANOTHER_TEST] Hash: ${anotherMetric}"
echo   }
echo }
) > "%PROBES_DIR%\another-test-probe.json"

echo ✓ 创建了另一个测试探针配置: %PROBES_DIR%\another-test-probe.json

echo.
echo ===========================================
echo 测试步骤
echo ===========================================

echo 1. 首次列出探针（应该只有内置的deep_call探针）:
echo    tf --list-probes
echo.

echo 2. 重新加载探针配置（应该加载新的test-probe）:
echo    tf --reload-probes
echo.

echo 3. 再次列出探针（应该包含test-probe）:
echo    tf --list-probes
echo.

echo 4. 显示新探针的配置:
echo    tf --show-config "Test探针"
echo.

echo 5. 修改探针配置文件，然后重新加载:
echo    REM 编辑 %PROBES_DIR%\test-probe.json
echo    tf --reload-probes
echo.

echo 6. 删除探针配置文件，然后重新加载:
echo    del "%PROBES_DIR%\test-probe.json"
echo    tf --reload-probes
echo.

echo ===========================================
echo 预期结果
echo ===========================================
echo - 初始状态：只有deep_call探针
echo - 重新加载后：包含deep_call + test-probe + another-test-probe
echo - test-probe状态：Enabled
echo - another-test-probe状态：Disabled
echo - 删除文件后重新加载：只剩下deep_call探针

echo.
echo ===========================================
echo 清理测试文件
echo ===========================================
echo 测试完成后，可以运行以下命令清理测试文件：
echo del "%PROBES_DIR%\test-probe.json"
echo del "%PROBES_DIR%\another-test-probe.json"

echo.
echo 测试环境准备完成！现在可以使用 tf --reload-probes 命令进行测试。

pause
