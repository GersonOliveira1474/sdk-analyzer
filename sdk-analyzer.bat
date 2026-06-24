@echo off
title SDK Analyzer

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
echo   Configure a variavel JAVA_HOME apontando
echo   para o diretorio do Java instalado.
echo.
echo   Exemplo: set JAVA_HOME=C:\Program Files\Java\jre-21
echo ============================================
pause
exit /b 1

:run
echo Iniciando SDK Analyzer...
"%JAVA_CMD%" -jar "%~dp0sdk-analyzer.jar" %*
