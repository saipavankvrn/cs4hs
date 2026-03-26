@echo off
echo ===================================================
echo    Smart Student Planner - Auto Startup Script
echo ===================================================
echo.

IF NOT EXIST ".maven\apache-maven-3.9.6\bin\mvn.cmd" (
    echo [1/3] Downloading Portable Maven environment, this may take a minute...
    mkdir .maven
    powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '.maven\maven.zip'"
    echo [2/3] Extracting files...
    powershell -Command "Expand-Archive -Path '.maven\maven.zip' -DestinationPath '.maven' -Force"
)

echo [3/3] Starting the Spring Boot Java Backend...
set PATH=%~dp0.maven\apache-maven-3.9.6\bin;%PATH%
call mvn spring-boot:run
pause
