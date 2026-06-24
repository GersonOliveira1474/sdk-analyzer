@echo off
title SDK Analyzer - Remover Servico

net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo   Execute como Administrador
    pause
    exit /b 1
)

echo Removendo servico SDK Analyzer...
"%~dp0service\sdk-analyzer-service.exe" uninstall
if %ERRORLEVEL% EQU 0 (
    echo.
    echo   Servico removido com sucesso.
) else (
    echo   Erro ao remover o servico.
)
echo.
pause
