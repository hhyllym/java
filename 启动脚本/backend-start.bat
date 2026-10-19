@echo off
chcp 65001 >nul
title mlp-backend 8081
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\jbr"
cd /d C:\Users\18017\Desktop\1java-main\1java-main\backend-src
echo 启动后端，端口 8081（首次编译约 1 分钟）...
"C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" -DskipTests spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
