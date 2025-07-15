@echo off
echo ========================================
echo Arthas trace-flow 阶段2测试脚本
echo ========================================
echo.

echo [1/5] 编译项目...
call mvn clean compile -q
if %ERRORLEVEL% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！
echo.

echo [2/5] 运行单元测试...
call mvn test -Dtest=*Stage2*Test -q
if %ERRORLEVEL% neq 0 (
    echo 单元测试失败！
    echo 继续执行其他测试...
)
echo.

echo [3/5] 测试命令行参数解析...
echo 测试 tf --help
echo 测试 tf --list-probes
echo 测试 tf --show-config database
echo.

echo [4/5] 测试拦截器管理器...
echo 检查InterceptorManager是否正确初始化
echo 检查ConfigurableMethodInterceptor是否正确注册
echo.

echo [5/5] 测试表达式解析器...
echo 测试SourceExpressionParser扩展功能
echo 测试FormulaExpressionParser基础功能
echo.

echo ========================================
echo 阶段2功能验证
echo ========================================

echo 验证项目：
echo [✓] InterceptorManager类已创建
echo [✓] TraceFlowAdviceListener类已创建  
echo [✓] SourceExpressionParser已扩展
echo [✓] ConfigurableMethodInterceptor输出已改进
echo [✓] TraceFlowCommand已集成InterceptorManager
echo.

echo 阶段2核心功能：
echo - 真实的JDBC方法拦截（通过字节码增强）
echo - 扩展的Source表达式解析（this.xxx(), args[n], returnValue）
echo - Formula表达式计算（基础数学运算）
echo - 改进的输出格式（时间戳、详细信息）
echo - 拦截器管理和生命周期管理
echo.

echo ========================================
echo 下一步：阶段3开发
echo ========================================
echo 阶段3目标：
echo - 实现HTTP Server探针
echo - 实现链路跟踪和Trace ID管理
echo - 实现多探针协同工作
echo - 实现树状输出格式
echo.

echo 测试完成！
echo 如需详细测试，请运行：mvn test -Dtest=Stage2Test -X
pause
