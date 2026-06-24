@echo off
title SDK Analyzer - Parar Servico

net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo   Execute como Administrador
    pause
    exit /b 1
)

echo Parando servico SDK Analyzer...
"%~dp0service\sdk-analyzer-service.exe" stop
echo.
pause
