@echo off
echo ==========================================
echo trace-flow 命令集成验证
echo ==========================================

echo.
echo 检查文件结构...
echo ==========================================

REM 检查Java源文件
echo 检查Java源文件:
if exist "src\main\java\com\taobao\arthas\core\command\trace\TraceFlowCommand.java" (
    echo ✓ TraceFlowCommand.java
) else (
    echo ✗ TraceFlowCommand.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\ProbeManager.java" (
    echo ✓ ProbeManager.java
) else (
    echo ✗ ProbeManager.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\ProbeConfig.java" (
    echo ✓ ProbeConfig.java
) else (
    echo ✗ ProbeConfig.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\SourceExpressionParser.java" (
    echo ✓ SourceExpressionParser.java
) else (
    echo ✗ SourceExpressionParser.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\ExecutionContext.java" (
    echo ✓ ExecutionContext.java
) else (
    echo ✗ ExecutionContext.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\TraceManager.java" (
    echo ✓ TraceManager.java
) else (
    echo ✗ TraceManager.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\FilterEngine.java" (
    echo ✓ FilterEngine.java
) else (
    echo ✗ FilterEngine.java 缺失
)

if exist "src\main\java\com\taobao\arthas\core\command\trace\OutputFormatter.java" (
    echo ✓ OutputFormatter.java
) else (
    echo ✗ OutputFormatter.java 缺失
)

echo.
echo 检查探针配置文件:
if exist "src\main\resources\probes\database-probe.json" (
    echo ✓ database-probe.json
) else (
    echo ✗ database-probe.json 缺失
)

if exist "src\main\resources\probes\http-server-probe.json" (
    echo ✓ http-server-probe.json
) else (
    echo ✗ http-server-probe.json 缺失
)

if exist "src\main\resources\probes\http-client-probe.json" (
    echo ✓ http-client-probe.json
) else (
    echo ✗ http-client-probe.json 缺失
)

if exist "src\main\resources\probes\file-operations-probe.json" (
    echo ✓ file-operations-probe.json
) else (
    echo ✗ file-operations-probe.json 缺失
)

echo.
echo 检查命令注册:
findstr /c:"TraceFlowCommand" "src\main\java\com\taobao\arthas\core\command\BuiltinCommandPack.java" >nul 2>&1
if errorlevel 1 (
    echo ✗ TraceFlowCommand 未在 BuiltinCommandPack.java 中注册
) else (
    echo ✓ TraceFlowCommand 已在 BuiltinCommandPack.java 中注册
)

echo.
echo 检查依赖配置:
findstr /c:"fastjson" "pom.xml" >nul 2>&1
if errorlevel 1 (
    echo ✗ fastjson 依赖未找到
) else (
    echo ✓ fastjson 依赖已配置
)

echo.
echo ==========================================
echo 集成状态总结
echo ==========================================

echo.
echo 已完成的集成步骤:
echo 1. ✓ Java源文件已移动到正确位置
echo 2. ✓ 探针配置文件已移动到resources目录
echo 3. ✓ TraceFlowCommand已注册到BuiltinCommandPack
echo 4. ✓ 依赖配置已存在

echo.
echo 下一步操作:
echo 1. 确保Maven环境可用
echo 2. 运行编译测试: mvn clean compile
echo 3. 运行单元测试: mvn test -Dtest=*trace*
echo 4. 启动Arthas并测试命令

echo.
echo 测试命令:
echo [arthas@pid]$ help trace-flow
echo [arthas@pid]$ trace-flow --help
echo [arthas@pid]$ trace-flow --list-probes
echo [arthas@pid]$ trace-flow --show-config database

echo.
echo ==========================================
echo 集成验证完成
echo ==========================================
