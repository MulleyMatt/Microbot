@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Starting Microbot from shaded jar (client-1.13.2-shaded.jar)...
"%JAVA_HOME%\bin\java.exe" -jar "%~dp0runelite-client\build\libs\client-1.13.2-shaded.jar" %*
