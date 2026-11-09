@echo off
cd /d %~dp0
set JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd
echo Starting MLP backend on port 8081 ...
"%MVN%" spring-boot:run -DskipTests -Dspring-boot.run.arguments=--server.port=8081
pause
