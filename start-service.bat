@echo off
title SDK Analyzer - Iniciar Servico

net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo   Execute como Administrador
    pause
    exit /b 1
)

echo Iniciando servico SDK Analyzer...
"%~dp0service\sdk-analyzer-service.exe" start
echo.
pause
