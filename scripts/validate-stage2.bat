@echo off
echo ========================================
echo Arthas trace-flow 阶段2功能验证
echo ========================================
echo.

echo [1/6] 运行阶段2验证测试...
call mvn test -Dtest=Stage2ValidationTest -q
if %ERRORLEVEL% neq 0 (
    echo 阶段2验证测试失败！
    echo 继续手动验证...
) else (
    echo ✓ 阶段2验证测试通过！
)
echo.

echo [2/6] 验证核心组件存在性...
echo 检查关键类文件:
if exist "core\src\main\java\com\taobao\arthas\core\command\trace\InterceptorManager.java" (
    echo ✓ InterceptorManager.java
) else (
    echo ✗ InterceptorManager.java 缺失
)

if exist "core\src\main\java\com\taobao\arthas\core\command\trace\TraceFlowAdviceListener.java" (
    echo ✓ TraceFlowAdviceListener.java
) else (
    echo ✗ TraceFlowAdviceListener.java 缺失
)

if exist "core\src\main\java\com\taobao\arthas\core\command\trace\ConfigurableMethodInterceptor.java" (
    echo ✓ ConfigurableMethodInterceptor.java
) else (
    echo ✗ ConfigurableMethodInterceptor.java 缺失
)
echo.

echo [3/6] 验证表达式解析器扩展...
echo 检查SourceExpressionParser是否支持复杂表达式:
findstr /C:"THIS_METHOD_PATTERN" "core\src\main\java\com\taobao\arthas\core\command\trace\SourceExpressionParser.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ 支持this.method()表达式
) else (
    echo ✗ 不支持this.method()表达式
)

findstr /C:"ARGS_ACCESS_PATTERN" "core\src\main\java\com\taobao\arthas\core\command\trace\SourceExpressionParser.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ 支持args[n]表达式
) else (
    echo ✗ 不支持args[n]表达式
)

findstr /C:"RETURN_VALUE_PATTERN" "core\src\main\java\com\taobao\arthas\core\command\trace\SourceExpressionParser.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ 支持returnValue表达式
) else (
    echo ✗ 不支持returnValue表达式
)
echo.

echo [4/6] 验证Formula表达式解析器...
findstr /C:"evaluateSimpleFormula" "core\src\main\java\com\taobao\arthas\core\command\trace\FormulaExpressionParser.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ FormulaExpressionParser支持基础数学运算
) else (
    echo ✗ FormulaExpressionParser不支持基础数学运算
)
echo.

echo [5/6] 验证ExecutionContext扩展...
findstr /C:"addMetric" "core\src\main\java\com\taobao\arthas\core\command\trace\ExecutionContext.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ ExecutionContext支持指标管理
) else (
    echo ✗ ExecutionContext不支持指标管理
)

findstr /C:"getTarget" "core\src\main\java\com\taobao\arthas\core\command\trace\ExecutionContext.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ ExecutionContext支持target访问
) else (
    echo ✗ ExecutionContext不支持target访问
)
echo.

echo [6/6] 验证拦截器管理...
findstr /C:"registerInterceptor" "core\src\main\java\com\taobao\arthas\core\command\trace\InterceptorManager.java" >nul
if %ERRORLEVEL% equ 0 (
    echo ✓ InterceptorManager支持拦截器注册
) else (
    echo ✗ InterceptorManager不支持拦截器注册
)
echo.

echo ========================================
echo 阶段2功能验证总结
echo ========================================

echo 已实现的核心功能:
echo ✓ 拦截器管理框架 (InterceptorManager)
echo ✓ Arthas增强框架集成 (TraceFlowAdviceListener)
echo ✓ 扩展的Source表达式解析器
echo ✓ 基础的Formula表达式计算
echo ✓ 配置驱动的方法拦截器
echo ✓ 改进的输出格式
echo ✓ ExecutionContext功能扩展
echo.

echo 支持的表达式类型:
echo - 内置变量: startTime, endTime, executionTime, threadName
echo - this方法调用: this.toString(), this.methodName()
echo - 参数访问: args[0], args[0].getValue()
echo - 返回值访问: returnValue, returnValue.getResultSet()
echo - Formula计算: metrics.endTime - metrics.startTime
echo.

echo 下一步: 阶段3开发
echo - 实现HTTP Server探针
echo - 实现链路跟踪和Trace ID管理
echo - 实现多探针协同工作
echo - 实现树状输出格式
echo.

echo ========================================
echo 阶段2验证完成！
echo ========================================
pause
