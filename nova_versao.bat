@echo off
:: ============================================================
:: DRS ERP - Lancando Nova Versao
:: Uso: nova_versao.bat 1.1.0 "Descricao da atualizacao" [obrigatoria]
:: Pre-requisito: variavel de ambiente GITHUB_TOKEN definida
::   setx GITHUB_TOKEN "ghp_SEU_TOKEN_AQUI"
:: ============================================================

set GIT=C:\Program Files\Git\bin\git.exe
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=C:\Users\Semnick144hz\.m2\maven-portable\apache-maven-3.9.6\bin\mvn.cmd
set PROJECT_DIR=%~dp0
set INSTALLER_DIR=%PROJECT_DIR%installer
set GITHUB_REPO=DeraldoSerra/drs-erp

:: Ler token do ambiente (nao fica salvo no codigo)
if "%GITHUB_TOKEN%"=="" (
    echo ERRO: Variavel GITHUB_TOKEN nao definida!
    echo Execute: setx GITHUB_TOKEN "ghp_SEU_TOKEN_AQUI"
    echo Depois feche e reabra o terminal.
    pause
    exit /b 1
)

if "%~1"=="" (
    echo ERRO: Informe o numero da versao!
    echo Uso: nova_versao.bat 1.1.0 "Descricao das mudancas" [obrigatoria]
    pause
    exit /b 1
)

set VERSAO=%~1
set DESCRICAO=%~2
if "%DESCRICAO%"=="" set DESCRICAO=Versao %VERSAO%
set OBRIGATORIA=false
if /i "%~3"=="obrigatoria" set OBRIGATORIA=true

echo ============================================================
echo  DRS ERP - Lancando versao %VERSAO% (obrigatoria: %OBRIGATORIA%)
echo ============================================================

cd /d "%PROJECT_DIR%"

:: ?? [1/6] Atualizar app.properties ??????????????????????????
echo [1/6] Atualizando app.properties...
echo app.version=%VERSAO%> src\main\resources\app.properties
echo app.name=DRS ERP>> src\main\resources\app.properties
for /f "tokens=1-3 delims=/ " %%a in ("%DATE%") do echo app.build=%%c-%%b-%%a>> src\main\resources\app.properties

:: ?? [2/6] Compilar e gerar JAR ??????????????????????????????
echo [2/6] Compilando projeto...
call "%MVN%" clean package -DskipTests -q
if errorlevel 1 (
    echo ERRO: Falha na compilacao!
    pause
    exit /b 1
)

:: ?? [3/6] Gerar instalador .exe ?????????????????????????????
echo [3/6] Gerando instalador .exe...
if not exist "%INSTALLER_DIR%" mkdir "%INSTALLER_DIR%"

:: Detectar JAR shaded gerado (ignora original-*.jar)
set JARFILE=
for /f "delims=" %%f in ('dir /b /a-d "%PROJECT_DIR%target\erp-desktop-*.jar" 2^>nul ^| findstr /v /i "original"') do set JARFILE=%%f
if "%JARFILE%"=="" (
    echo ERRO: JAR nao encontrado em target\. Verifique a compilacao.
    pause
    exit /b 1
)
echo    JAR encontrado: %JARFILE%

jpackage ^
  --input "%PROJECT_DIR%target" ^
  --main-jar %JARFILE% ^
  --main-class com.erp.Launcher ^
  --name "DRS ERP" ^
  --app-version %VERSAO% ^
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

:: ?? [4/6] Commit + tag + push ???????????????????????????????
echo [4/6] Salvando e enviando para o GitHub...
"%GIT%" add -A
"%GIT%" commit -m "v%VERSAO% - %DESCRICAO%"
"%GIT%" tag -a "v%VERSAO%" -m "%DESCRICAO%"
"%GIT%" push origin master --tags
if errorlevel 1 (
    echo ERRO: Falha ao enviar para o GitHub!
    pause
    exit /b 1
)

:: ?? [5/6] Criar GitHub Release e fazer upload do instalador ?
echo [5/6] Criando GitHub Release e fazendo upload do instalador...

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$token = $env:GITHUB_TOKEN; $repo = '%GITHUB_REPO%'; $versao = '%VERSAO%'; $descricao = '%DESCRICAO%'; $exePath = '%INSTALLER_DIR%\DRS ERP-%VERSAO%.exe';" ^
  "$headers = @{Authorization='token ' + $token; 'Content-Type'='application/json'; Accept='application/vnd.github+json'};" ^
  "$body = @{tag_name='v' + $versao; name='v' + $versao; body=$descricao; draft=$false; prerelease=$false} | ConvertTo-Json;" ^
  "$release = Invoke-RestMethod -Method Post -Uri ""https://api.github.com/repos/$repo/releases"" -Headers $headers -Body $body;" ^
  "if (-not (Test-Path $exePath)) { Write-Error ('Instalador nao encontrado: ' + $exePath); exit 1 };" ^
  "$uploadUrl = $release.upload_url -replace '\{.*\}',""?name=DRS-ERP-$versao.exe"";" ^
  "$fileBytes = [System.IO.File]::ReadAllBytes($exePath);" ^
  "$uploadHeaders = @{Authorization='token ' + $token; 'Content-Type'='application/octet-stream'; Accept='application/vnd.github+json'};" ^
  "$upload = Invoke-RestMethod -Method Post -Uri $uploadUrl -Headers $uploadHeaders -Body $fileBytes;" ^
  "Write-Host 'Download URL:' $upload.browser_download_url;" ^
  "[System.IO.File]::WriteAllText('%PROJECT_DIR%_release_url.tmp', $upload.browser_download_url)"

if errorlevel 1 (
    echo ERRO: Falha ao criar Release no GitHub!
    pause
    exit /b 1
)

:: ?? [6/6] Atualizar tabela versoes no Neon ??????????????????
echo [6/6] Registrando versao no banco de dados...
set /p DOWNLOAD_URL=<"%PROJECT_DIR%_release_url.tmp"
del "%PROJECT_DIR%_release_url.tmp"

where psql >nul 2>&1
if %errorlevel%==0 (
    set PGPASSWORD=npg_KA4SC9WtlgeQ
    psql "host=ep-muddy-base-acsse9s6-pooler.sa-east-1.aws.neon.tech port=5432 dbname=neondb user=neondb_owner sslmode=require" ^
      -c "INSERT INTO versoes (versao, descricao, url_download, obrigatoria) VALUES ('%VERSAO%', '%DESCRICAO%', '%DOWNLOAD_URL%', %OBRIGATORIA%) ON CONFLICT (versao) DO UPDATE SET descricao=EXCLUDED.descricao, url_download=EXCLUDED.url_download, obrigatoria=EXCLUDED.obrigatoria, data_lancamento=NOW();"
) else (
    echo.
    echo AVISO: psql nao encontrado. Execute manualmente no Neon Console:
    echo.
    echo INSERT INTO versoes (versao, descricao, url_download, obrigatoria^)
    echo VALUES ('%VERSAO%', '%DESCRICAO%', '%DOWNLOAD_URL%', %OBRIGATORIA%^)
    echo ON CONFLICT (versao^) DO UPDATE SET
    echo   descricao=EXCLUDED.descricao,
    echo   url_download=EXCLUDED.url_download,
    echo   obrigatoria=EXCLUDED.obrigatoria,
    echo   data_lancamento=NOW(^);
    echo.
    echo Acesse: https://console.neon.tech
)

echo ============================================================
echo  Versao %VERSAO% publicada com sucesso!
echo  Instalador: %INSTALLER_DIR%\DRS ERP-%VERSAO%.exe
echo  Download:   %DOWNLOAD_URL%
echo ============================================================
pause
