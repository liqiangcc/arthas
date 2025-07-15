@echo off
setlocal enabledelayedexpansion

REM 阶段1测试脚本 (Windows版本)
echo ==========================================
echo Arthas trace-flow 阶段1测试
echo ==========================================

REM 测试结果统计
set TOTAL_TESTS=0
set PASSED_TESTS=0
set FAILED_TESTS=0

REM 检查Java环境
echo 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Java环境
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr "version"') do (
    set JAVA_VERSION=%%i
    goto :java_version_found
)
:java_version_found
echo Java版本: %JAVA_VERSION%

REM 检查Maven环境
echo 检查Maven环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Maven环境
    exit /b 1
)

for /f "tokens=*" %%i in ('mvn -version 2^>^&1 ^| findstr "Apache Maven"') do (
    echo Maven版本: %%i
    goto :maven_version_found
)
:maven_version_found

echo.
echo ==========================================
echo 开始阶段1功能测试
echo ==========================================

REM 1. 编译测试
echo.
echo 测试: 代码编译
echo 命令: mvn clean compile -q
set /a TOTAL_TESTS+=1

mvn clean compile -q >nul 2>&1
if errorlevel 1 (
    echo X 失败
    set /a FAILED_TESTS+=1
) else (
    echo √ 通过
    set /a PASSED_TESTS+=1
)

REM 2. 单元测试
echo.
echo 测试: 单元测试执行
echo 命令: mvn test -Dtest=Stage1Test -q
set /a TOTAL_TESTS+=1

mvn test -Dtest=Stage1Test -q >nul 2>&1
if errorlevel 1 (
    echo X 失败
    set /a FAILED_TESTS+=1
) else (
    echo √ 通过
    set /a PASSED_TESTS+=1
)

REM 3. 代码覆盖率测试
echo.
echo 测试: 代码覆盖率检查
echo 命令: mvn jacoco:report -q
set /a TOTAL_TESTS+=1

mvn jacoco:report -q >nul 2>&1
if errorlevel 1 (
    echo X 失败
    set /a FAILED_TESTS+=1
) else (
    echo √ 通过
    set /a PASSED_TESTS+=1
)

REM 4. 配置文件验证测试
echo.
echo 测试: 配置文件JSON格式验证
set /a TOTAL_TESTS+=1

REM 创建临时Python脚本验证JSON
echo import json > temp_json_test.py
echo import sys >> temp_json_test.py
echo try: >> temp_json_test.py
echo     with open('src/main/resources/probes/database-probe.json', 'r') as f: >> temp_json_test.py
echo         json.load(f) >> temp_json_test.py
echo     with open('src/main/resources/probes/http-server-probe.json', 'r') as f: >> temp_json_test.py
echo         json.load(f) >> temp_json_test.py
echo     with open('src/main/resources/probes/http-client-probe.json', 'r') as f: >> temp_json_test.py
echo         json.load(f) >> temp_json_test.py
echo     with open('src/main/resources/probes/file-operations-probe.json', 'r') as f: >> temp_json_test.py
echo         json.load(f) >> temp_json_test.py
echo     print('所有配置文件JSON格式正确') >> temp_json_test.py
echo except Exception as e: >> temp_json_test.py
echo     print(f'配置文件JSON格式错误: {e}') >> temp_json_test.py
echo     sys.exit(1) >> temp_json_test.py

python temp_json_test.py >nul 2>&1
if errorlevel 1 (
    echo X 失败
    set /a FAILED_TESTS+=1
) else (
    echo √ 通过
    set /a PASSED_TESTS+=1
)

del temp_json_test.py >nul 2>&1

REM 5. 基础功能集成测试
echo.
echo 测试: 基础功能集成测试
set /a TOTAL_TESTS+=1

REM 创建临时Java测试文件
echo import com.taobao.arthas.core.command.trace.*; > TempTest.java
echo. >> TempTest.java
echo public class TempTest { >> TempTest.java
echo     public static void main(String[] args) { >> TempTest.java
echo         try { >> TempTest.java
echo             TraceFlowCommand command = new TraceFlowCommand(); >> TempTest.java
echo             System.out.println("√ TraceFlowCommand创建成功"); >> TempTest.java
echo. >> TempTest.java
echo             ProbeManager manager = new ProbeManager(); >> TempTest.java
echo             var configs = manager.loadBuiltinProbes(); >> TempTest.java
echo             System.out.println("√ 加载了 " + configs.size() + " 个探针配置"); >> TempTest.java
echo. >> TempTest.java
echo             SourceExpressionParser parser = new SourceExpressionParser(); >> TempTest.java
echo             ExecutionContext context = ExecutionContext.createMockContext(1000L, 2000L); >> TempTest.java
echo             Object result = parser.parse("executionTime", context); >> TempTest.java
echo             System.out.println("√ 表达式解析结果: " + result); >> TempTest.java
echo. >> TempTest.java
echo             FilterEngine filter = new FilterEngine(); >> TempTest.java
echo             java.util.Map^<String, Object^> metrics = new java.util.HashMap^<^>(); >> TempTest.java
echo             metrics.put("executionTime", 1500L); >> TempTest.java
echo             boolean matches = filter.matches("executionTime ^> 1000", metrics); >> TempTest.java
echo             System.out.println("√ 过滤测试结果: " + matches); >> TempTest.java
echo. >> TempTest.java
echo             System.out.println("\n所有基础功能测试通过!"); >> TempTest.java
echo. >> TempTest.java
echo         } catch (Exception e) { >> TempTest.java
echo             System.err.println("测试失败: " + e.getMessage()); >> TempTest.java
echo             e.printStackTrace(); >> TempTest.java
echo             System.exit(1); >> TempTest.java
echo         } >> TempTest.java
echo     } >> TempTest.java
echo } >> TempTest.java

mvn exec:java -Dexec.mainClass="TempTest" -q >nul 2>&1
if errorlevel 1 (
    echo X 失败
    set /a FAILED_TESTS+=1
) else (
    echo √ 通过
    set /a PASSED_TESTS+=1
)

del TempTest.java >nul 2>&1

echo.
echo ==========================================
echo 阶段1测试总结
echo ==========================================
echo 总测试数: %TOTAL_TESTS%
echo 通过: %PASSED_TESTS%
echo 失败: %FAILED_TESTS%

if %FAILED_TESTS% equ 0 (
    echo.
    echo 🎉 阶段1所有测试通过! 可以进入阶段2开发
    exit /b 0
) else (
    echo.
    echo ❌ 有 %FAILED_TESTS% 个测试失败，请修复后重新测试
    exit /b 1
)

endlocal
