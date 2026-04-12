@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@IF "%MAVEN_BATCH_ECHO%" == "on"  echo on
@IF "%MAVEN_BATCH_PAUSE%" == "on" pause

@REM Set local scope for the variables with windows NT shell
@setlocal enableextensions

@set MAVEN_CMD_LINE_ARGS=%*

@set WRAPPERDIR=%~dp0.mvn\wrapper

@REM ==========================================================================
@REM Validate JAVA_HOME, or find java.exe from PATH
@REM ==========================================================================

@REM Find a suitable Java executable
@set JAVACMD=java
@if not "%JAVA_HOME%"=="" set JAVACMD=%JAVA_HOME%\bin\java

@REM Prefer JDK 25 over JDK 8
@if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\java.exe" (
    set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
    set JAVACMD=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\java.exe
)

@REM ==========================================================================
@REM Download Maven if not present
@REM ==========================================================================
@set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6
@set MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd

@if not exist "%MVN_CMD%" (
    echo [ERP] Maven nao encontrado. Baixando automaticamente...
    echo [ERP] Isso pode demorar alguns minutos na primeira vez.
    echo.

    @set MAVEN_ZIP=%TEMP%\apache-maven-3.9.6-bin.zip
    @set MAVEN_EXTRACT=%USERPROFILE%\.m2\wrapper\dists

    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_ZIP%' -UseBasicParsing }"

    if not exist "%MAVEN_EXTRACT%" mkdir "%MAVEN_EXTRACT%"
    powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_EXTRACT%' -Force"
    del "%MAVEN_ZIP%"
    echo [ERP] Maven instalado com sucesso!
    echo.
)

@REM ==========================================================================
@REM Execute Maven
@REM ==========================================================================
@"%JAVACMD%" -classpath "%WRAPPERDIR%\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%~dp0" ^
  org.apache.maven.wrapper.MavenWrapperMain %MAVEN_CMD_LINE_ARGS%

@if ERRORLEVEL 1 goto error
@goto end

:error
@set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
@exit /b %ERROR_CODE%
