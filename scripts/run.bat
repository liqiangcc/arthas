@echo off
REM Arthas Run Script
REM Usage:
REM   run.bat          - Start Arthas normally
REM   run.bat --debug  - Start Arthas with debug port enabled (port 5005)

setlocal enabledelayedexpansion

REM Configuration
set "ARTHAS_HOME=%~dp0.."
set "BUILD_DIR=%ARTHAS_HOME%\packaging\target\arthas-bin"
set "ARTHAS_BOOT_JAR=%BUILD_DIR%\arthas-boot.jar"

REM Debug Configuration
set "DEBUG_PORT=5005"
set "ENABLE_DEBUG=false"

REM Check for debug parameter
if "%1"=="--debug" (
    set "ENABLE_DEBUG=true"
    echo DEBUG MODE ENABLED - Debug port: %DEBUG_PORT%
    echo You can attach a debugger to localhost:%DEBUG_PORT%
    echo.
)

echo ========================================
echo        Arthas Run Script
echo ========================================
echo.

REM Check Java Environment
echo Checking Java environment...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found, please ensure Java is installed and in PATH
    exit /b 1
)
echo SUCCESS: Java environment check passed
echo.

REM Check Build Artifacts
echo Checking build artifacts...
if not exist "%BUILD_DIR%" (
    echo ERROR: Build directory does not exist: %BUILD_DIR%
    echo Please run build.bat first to build the project
    exit /b 1
)

if not exist "%ARTHAS_BOOT_JAR%" (
    echo ERROR: Arthas Boot JAR does not exist: %ARTHAS_BOOT_JAR%
    echo Please run build.bat first to build the project
    exit /b 1
)

echo SUCCESS: Build artifacts check passed
echo   - Arthas Boot JAR: %ARTHAS_BOOT_JAR%
echo.

echo Starting Arthas Boot...
echo You can now attach to any Java process or start a new one.
echo.
echo ==================== Arthas Boot ====================

REM Run Arthas Boot
if "%ENABLE_DEBUG%"=="true" (
    echo Starting Arthas Boot with debug enabled...
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%DEBUG_PORT% -jar "%ARTHAS_BOOT_JAR%"
) else (
    echo Starting Arthas Boot...
    java -jar "%ARTHAS_BOOT_JAR%"
)

echo.
echo =====================================================
echo.
echo Arthas Boot has exited.
echo Script execution completed!
pause
