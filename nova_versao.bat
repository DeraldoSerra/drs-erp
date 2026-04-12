@echo off
:: ============================================================
:: DRS ERP - Lançar Nova Versão
:: Uso: nova_versao.bat 1.1.0 "Descrição da atualização"
:: ============================================================

set GIT=C:\Program Files\Git\bin\git.exe
set PROJECT_DIR=%~dp0

if "%~1"=="" (
    echo ERRO: Informe o numero da versao!
    echo Uso: nova_versao.bat 1.1.0 "Descricao das mudancas"
    pause
    exit /b 1
)

set VERSAO=%~1
set DESCRICAO=%~2
if "%DESCRICAO%"=="" set DESCRICAO=Versao %VERSAO%

echo ============================================================
echo  DRS ERP - Lancando versao %VERSAO%
echo ============================================================

cd /d "%PROJECT_DIR%"

:: Atualiza app.properties
echo app.version=%VERSAO%> src\main\resources\app.properties
echo app.name=DRS ERP>> src\main\resources\app.properties
for /f "tokens=1-3 delims=/ " %%a in ("%DATE%") do echo app.build=%%c-%%b-%%a>> src\main\resources\app.properties

:: Commit no Git
echo [1/3] Salvando alteracoes no Git...
"%GIT%" add -A
"%GIT%" commit -m "v%VERSAO% - %DESCRICAO%"

:: Tag de versao
echo [2/3] Criando tag v%VERSAO%...
"%GIT%" tag -a "v%VERSAO%" -m "%DESCRICAO%"

:: Push (se tiver GitHub configurado)
echo [3/3] Enviando para GitHub...
"%GIT%" push origin main --tags 2>nul
if errorlevel 1 (
    echo AVISO: Nao foi possivel enviar para o GitHub.
    echo Configure o repositorio com: git remote add origin URL_DO_SEU_REPO
)

echo ============================================================
echo  Versao %VERSAO% lancada com sucesso!
echo  Agora rode 'gerar_instalador.bat' para criar o .exe
echo ============================================================
pause
