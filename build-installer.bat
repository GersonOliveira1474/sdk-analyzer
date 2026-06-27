@echo off
setlocal

echo ===================================================
echo  Senior Watch - Build Installer
echo ===================================================

set PROJECT_DIR=%~dp0
set FRONTEND_DIR=F:\Projetos\sdk-insight
set JAR_NAME=sdk-analyzer-1.0.0.jar
set APP_VERSION=1.0.0

:: ─── Step 1: Build frontend ─────────────────────────
echo.
echo [1/4] Building frontend...
cd /d "%FRONTEND_DIR%"
call npm run build
if errorlevel 1 (
    echo ERRO: Falha no build do frontend
    exit /b 1
)

:: ─── Step 2: Copy frontend to static ────────────────
echo.
echo [2/4] Copiando frontend para resources/static...
set STATIC_DIR=%PROJECT_DIR%src\main\resources\static
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
xcopy /s /e /q "%FRONTEND_DIR%\dist\*" "%STATIC_DIR%\"

:: ─── Step 3: Build JAR ──────────────────────────────
echo.
echo [3/4] Building JAR...
cd /d "%PROJECT_DIR%"
call mvnw.cmd package -DskipTests -q
if errorlevel 1 (
    echo ERRO: Falha no build do JAR
    exit /b 1
)

:: ─── Step 4: jpackage ───────────────────────────────
echo.
echo [4/4] Gerando instalador com jpackage...

set JPACKAGE_TYPE=app-image
set INSTALLER_DIR=%PROJECT_DIR%installer

:: Check if WiX is available for MSI
where candle >nul 2>&1
if not errorlevel 1 (
    set JPACKAGE_TYPE=msi
    echo    WiX detectado - gerando .msi
) else (
    echo    WiX nao encontrado - gerando app-image (.exe portavel)
    echo    Para gerar .msi, instale WiX Toolset: https://wixtoolset.org/
)

if exist "%INSTALLER_DIR%" rmdir /s /q "%INSTALLER_DIR%"

jpackage ^
    --type %JPACKAGE_TYPE% ^
    --name "Senior Watch" ^
    --app-version %APP_VERSION% ^
    --vendor "Senior Sistemas" ^
    --description "Senior Watch - Monitoramento e analise de logs" ^
    --icon "%PROJECT_DIR%src\main\resources\icon.ico" ^
    --input "%PROJECT_DIR%target" ^
    --main-jar %JAR_NAME% ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --dest "%INSTALLER_DIR%" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-menu-group "Senior" ^
    --win-shortcut ^
    --win-shortcut-prompt ^
    --java-options "-Djava.awt.headless=false" ^
    --java-options "-Xmx512m"

if errorlevel 1 (
    echo ERRO: Falha no jpackage
    exit /b 1
)

echo.
echo ===================================================
echo  Build concluido!
echo  Saida em: %INSTALLER_DIR%
echo ===================================================

endlocal
