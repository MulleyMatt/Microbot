@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
cd /d "%~dp0"
echo Starting Microbot RuneLite Client with JDK 21...
call gradlew.bat :client:run
pause
