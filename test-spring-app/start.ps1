# Arthas Trace Flow Test Application - PowerShell Launcher
Write-Host "========================================" -ForegroundColor Green
Write-Host "Arthas Trace Flow Test Application" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "[1/2] Checking Maven..." -ForegroundColor Yellow

if (Test-Path "mvnw.cmd") {
    Write-Host "Found Maven Wrapper (mvnw.cmd)" -ForegroundColor Green
    Write-Host "Compiling and starting application..." -ForegroundColor Yellow
    Write-Host ""
    
    & .\mvnw.cmd spring-boot:run
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host "Using system Maven" -ForegroundColor Green
    Write-Host "Compiling and starting application..." -ForegroundColor Yellow
    Write-Host ""
    
    mvn spring-boot:run
} else {
    Write-Host "ERROR: Neither Maven Wrapper nor Maven found!" -ForegroundColor Red
    Write-Host "Please install Maven or ensure mvnw.cmd exists" -ForegroundColor Red
    Write-Host ""
    Write-Host "Download Maven from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Application stopped." -ForegroundColor Yellow
Read-Host "Press Enter to exit"
