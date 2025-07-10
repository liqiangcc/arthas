@echo off
REM Arthas Build Script
REM Usage: build.bat

setlocal enabledelayedexpansion

REM Configuration
set "ARTHAS_HOME=%~dp0.."
set "BUILD_DIR=%ARTHAS_HOME%\packaging\target\arthas-bin"
set "ARTHAS_BOOT_JAR=%BUILD_DIR%\arthas-boot.jar"

echo ========================================
echo        Arthas Build Script
echo ========================================
echo.

REM Step 1: Check Java Environment
echo [Step 1/2] Checking Java environment...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found, please ensure Java is installed and in PATH
    exit /b 1
)
echo SUCCESS: Java environment check passed
echo.

REM Step 2: Build Project
echo [Step 2/2] Building Arthas project...
cd /d "%ARTHAS_HOME%"

echo Executing: mvnw clean install -DskipTests -pl common,spy,core,agent,client,boot,packaging
call mvnw.cmd clean install -DskipTests -pl common,spy,core,agent,client,boot,packaging
if errorlevel 1 (
    echo ERROR: Project build failed
    exit /b 1
)
echo SUCCESS: Project build completed
echo.

REM Check Build Artifacts
echo Checking build artifacts...
if not exist "%BUILD_DIR%" (
    echo ERROR: Build directory does not exist: %BUILD_DIR%
    exit /b 1
)

if not exist "%ARTHAS_BOOT_JAR%" (
    echo ERROR: Arthas Boot JAR does not exist: %ARTHAS_BOOT_JAR%
    exit /b 1
)

echo SUCCESS: Build artifacts check passed
echo   - Build Directory: %BUILD_DIR%
echo   - Arthas Boot JAR: %ARTHAS_BOOT_JAR%
echo.

echo ========================================
echo Build completed successfully!
echo You can now run: run.bat
echo ========================================
pause
