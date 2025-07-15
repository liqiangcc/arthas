@echo off
chcp 65001 >nul
echo ========================================
echo Arthas Trace Flow Test Application
echo ========================================
echo.

echo Changing to test-spring-app directory...
cd /d "%~dp0"

echo Current directory: %CD%
echo.

echo [1/2] Compiling standalone application...
if exist "mvnw.cmd" (
    echo Using Maven Wrapper in current directory...
    mvnw.cmd clean compile -f pom.xml
) else (
    echo Maven Wrapper not found, using system Maven...
    mvn clean compile -f pom.xml
)

if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.

echo [2/2] Starting application...
echo Application will start at http://localhost:8080
echo.
echo Test endpoints:
echo - GET  /api/test/simple     - Simple HTTP response
echo - GET  /api/test/database   - Database operations test
echo - GET  /api/test/file       - File operations test
echo - GET  /api/test/http       - HTTP client test
echo - GET  /api/test/complex    - Complex trace test (RECOMMENDED)
echo.

if exist "mvnw.cmd" (
    mvnw.cmd spring-boot:run -f pom.xml
) else (
    mvn spring-boot:run -f pom.xml
)

echo.
echo Application stopped.
pause
