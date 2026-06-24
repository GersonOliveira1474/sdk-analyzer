@echo off
title SDK Analyzer
set "BASE_DIR=%~dp0"

:: Lê java.home do updater.properties
if exist "%BASE_DIR%updater.properties" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%BASE_DIR%updater.properties") do (
        set "KEY=%%a"
        set "VAL=%%b"
        if "%%a"=="java.home" (
            if exist "%%b\bin\java.exe" (
                set "JAVA_CMD=%%b\bin\java.exe"
                goto :run
            )
        )
    )
)

:: Tenta usar JAVA_HOME se definido
if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    goto :run
)

:: Tenta encontrar java no PATH
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "JAVA_CMD=java"
    goto :run
)

:: Caminhos comuns em máquinas Senior
for %%p in (
    "C:\Program Files\Java\jre*\bin\java.exe"
    "C:\Program Files\Eclipse Adoptium\*\bin\java.exe"
    "C:\Program Files\OpenJDK\*\bin\java.exe"
) do (
    for %%f in (%%p) do (
        if exist "%%f" (
            set "JAVA_CMD=%%f"
            goto :run
        )
    )
)

echo ============================================
echo   Java nao encontrado.
echo.
echo   Configure java.home no updater.properties
echo   ou a variavel JAVA_HOME do sistema.
echo.
echo   Exemplo no updater.properties:
echo   java.home=C:\Program Files\Java\jre-21
echo ============================================
pause
exit /b 1

:run
echo Iniciando SDK Analyzer...
"%JAVA_CMD%" -jar "%BASE_DIR%sdk-analyzer.jar" --spring.config.additional-location=file:%BASE_DIR%config.properties %*
