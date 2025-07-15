@echo off
echo Starting Arthas Trace Flow Test Application...
echo.

cd /d "%~dp0"
mvnw.cmd spring-boot:run

pause
