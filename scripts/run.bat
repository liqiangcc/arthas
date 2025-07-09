@echo off
REM Arthas Run Script
REM Usage: run.bat

setlocal enabledelayedexpansion

REM Configuration
set "ARTHAS_HOME=%~dp0.."
set "BUILD_DIR=%ARTHAS_HOME%\packaging\target\arthas-bin"
set "ARTHAS_BOOT_JAR=%BUILD_DIR%\arthas-boot.jar"

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
java -jar "%ARTHAS_BOOT_JAR%"

echo.
echo =====================================================
echo.
echo Arthas Boot has exited.
echo Script execution completed!
pause
