@echo off
setlocal enabledelayedexpansion

REM Arthas Home 设置脚本
REM 用于设置ARTHAS_HOME环境变量和创建外部探针配置目录

echo ===========================================
echo Arthas Home 设置脚本
echo ===========================================

REM 获取当前脚本所在目录的父目录作为ARTHAS_HOME
set "SCRIPT_DIR=%~dp0"
set "ARTHAS_HOME=%SCRIPT_DIR:~0,-9%"

echo 检测到的Arthas目录: %ARTHAS_HOME%

REM 创建probes目录
set "PROBES_DIR=%ARTHAS_HOME%\probes"
if not exist "%PROBES_DIR%" (
    echo 创建外部探针配置目录: %PROBES_DIR%
    mkdir "%PROBES_DIR%"
) else (
    echo 外部探针配置目录已存在: %PROBES_DIR%
)

REM 检查示例配置文件
if exist "%PROBES_DIR%\http-server-probe.json" (
    echo 示例配置文件已存在
) else (
    echo 示例配置文件位置: %PROBES_DIR%\
)

REM 环境变量设置说明
echo.
echo ===========================================
echo 环境变量设置
echo ===========================================
echo 请设置以下环境变量：
echo.
echo ARTHAS_HOME=%ARTHAS_HOME%
echo.
echo 设置方法：
echo 1. 右键"此电脑" ^> "属性" ^> "高级系统设置"
echo 2. 点击"环境变量"
echo 3. 在"系统变量"中点击"新建"
echo 4. 变量名：ARTHAS_HOME
echo 5. 变量值：%ARTHAS_HOME%
echo.

REM 检查当前环境变量
if defined ARTHAS_HOME (
    echo 当前ARTHAS_HOME: %ARTHAS_HOME%
) else (
    echo 当前未设置ARTHAS_HOME环境变量
)

echo.
echo ===========================================
echo 使用说明
echo ===========================================
echo 1. 按照上述方法设置ARTHAS_HOME环境变量
echo 2. 重新打开命令提示符窗口
echo 3. 将自定义探针配置文件放入: %PROBES_DIR%
echo 4. 启动Arthas，系统会自动加载外部探针配置
echo.
echo 探针配置文件示例位置: %PROBES_DIR%
echo 内置探针配置位置: JAR包内的/probes/目录（只包含deep_call.json）

pause
