@echo off
title SDK Analyzer - Instalar Servico
set "BASE_DIR=%~dp0"

:: Verifica permissão de administrador
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ============================================
    echo   Execute como Administrador
    echo ============================================
    pause
    exit /b 1
)

:: Configura java.home do updater.properties se disponível
if exist "%BASE_DIR%updater.properties" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%BASE_DIR%updater.properties") do (
        if "%%a"=="java.home" (
            set "JAVA_HOME=%%b"
        )
    )
)

echo Instalando servico SDK Analyzer...
"%BASE_DIR%service\sdk-analyzer-service.exe" install
if %ERRORLEVEL% EQU 0 (
    echo.
    echo   Servico instalado com sucesso.
    echo   Use start-service.bat para iniciar.
) else (
    echo   Erro ao instalar o servico.
)
echo.
pause
