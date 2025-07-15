@echo off
chcp 65001 >nul
echo ========================================
echo Arthas Trace Flow Test Application
echo ========================================
echo.

echo Starting application with Maven Wrapper...
echo.

if exist "mvnw.cmd" (
    echo Found mvnw.cmd, using Maven Wrapper...
    mvnw.cmd spring-boot:run
) else (
    echo mvnw.cmd not found, trying system Maven...
    mvn spring-boot:run
)

echo.
echo Application stopped.
pause
