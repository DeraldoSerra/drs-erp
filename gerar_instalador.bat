@echo off
:: ============================================================
:: DRS ERP - Gerar Instalador .exe
:: Execute este script para criar o instalador Windows
:: ============================================================

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=C:\Users\Semnick144hz\.m2\maven-portable\apache-maven-3.9.6\bin\mvn.cmd
set PROJECT_DIR=%~dp0
set INSTALLER_DIR=%PROJECT_DIR%installer

echo ============================================================
echo  DRS ERP - Gerando Instalador
echo ============================================================

:: 1. Build do JAR
echo [1/3] Compilando e gerando JAR...
cd /d "%PROJECT_DIR%"
call "%MVN%" clean package -DskipTests -q
if errorlevel 1 (
    echo ERRO: Falha na compilacao!
    pause
    exit /b 1
)
echo JAR gerado com sucesso!

:: 2. Criar pasta do instalador
if not exist "%INSTALLER_DIR%" mkdir "%INSTALLER_DIR%"

:: 3. Gerar instalador com jpackage
echo [2/3] Gerando instalador .exe com jpackage...
jpackage ^
  --input "%PROJECT_DIR%target" ^
  --main-jar erp-desktop-1.0.0.jar ^
  --main-class com.erp.Launcher ^
  --name "DRS ERP" ^
  --app-version 1.0.0 ^
  --description "DRS ERP - Sistema de Gestao e Frente de Caixa" ^
  --vendor "DRS" ^
  --dest "%INSTALLER_DIR%" ^
  --type exe ^
  --icon "%PROJECT_DIR%src\main\resources\icon.ico" ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "DRS ERP" ^
  --win-dir-chooser ^
  --java-options "-Xms256m -Xmx512m"

if errorlevel 1 (
    echo ERRO: Falha ao gerar instalador!
    pause
    exit /b 1
)

echo [3/3] Instalador criado em: %INSTALLER_DIR%
echo ============================================================
echo  CONCLUIDO! Arquivo pronto para distribuir.
echo ============================================================
pause
