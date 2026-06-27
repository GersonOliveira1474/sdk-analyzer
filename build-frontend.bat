@echo off
setlocal

echo ===================================================
echo  Senior Watch - Build Frontend
echo ===================================================

set FRONTEND_DIR=F:\Projetos\sdk-insight
set STATIC_DIR=%~dp0src\main\resources\static

echo.
echo [1/3] Building frontend...
cd /d "%FRONTEND_DIR%"
call npm run build
if errorlevel 1 (
    echo ERRO: Falha no build do frontend
    exit /b 1
)

echo.
echo [2/3] Limpando static anterior...
if exist "%STATIC_DIR%" (
    rmdir /s /q "%STATIC_DIR%"
)
mkdir "%STATIC_DIR%"

echo.
echo [3/3] Copiando build para resources/static...
xcopy /s /e /q "%FRONTEND_DIR%\dist\*" "%STATIC_DIR%\"
if errorlevel 1 (
    echo ERRO: Falha ao copiar arquivos
    exit /b 1
)

echo.
echo ===================================================
echo  Build concluido com sucesso!
echo  Agora execute: mvnw package -DskipTests
echo ===================================================

endlocal
