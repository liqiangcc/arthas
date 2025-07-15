@echo off
chcp 65001 >nul
echo ========================================
echo Arthas Trace Flow Test Application
echo ========================================
echo.

echo [1/3] Compiling application...
if exist "mvnw.cmd" (
    echo Using Maven Wrapper mvnw.cmd...
    call mvnw.cmd clean compile
    goto compile_done
)
if exist "mvnw" (
    echo Using Maven Wrapper mvnw...
    call mvnw clean compile
    goto compile_done
)

where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Neither Maven Wrapper nor Maven found
    echo Please ensure mvnw.cmd exists or install Maven
    pause
    exit /b 1
)
echo Using system Maven...
call mvn clean compile

:compile_done
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Compilation successful!
echo.

echo [2/3] Starting application...
echo Application will start at http://localhost:8080
echo.
echo Test endpoints:
echo - GET  /api/test/simple     - Simple HTTP response
echo - GET  /api/test/database   - Database operations test
echo - GET  /api/test/file       - File operations test
echo - GET  /api/test/http       - HTTP client test
echo - GET  /api/test/complex    - Complex trace test (RECOMMENDED)
echo - GET  /api/users           - User list
echo - POST /api/users           - Create user
echo.
echo H2 Database Console: http://localhost:8080/h2-console
echo.

echo [3/3] Running application...
if exist "mvnw.cmd" (
    call mvnw.cmd spring-boot:run
    goto end
)
if exist "mvnw" (
    call mvnw spring-boot:run
    goto end
)
call mvn spring-boot:run

:end
