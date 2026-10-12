@echo off
chcp 936 >nul
setlocal
title 小马王国 - 环境自检
cd /d "%~dp0"

echo ==========================================================
echo   环境自检
echo ==========================================================
echo --- 1. Node / npm ---
for /f "delims=" %%v in ('node -v 2^>nul') do echo   node : %%v
for /f "delims=" %%v in ('npm -v  2^>nul') do echo   npm  : %%v
where node >nul 2>nul || echo   [X] 找不到 node
echo.
echo --- 2. JDK ---
echo   JAVA_HOME = %JAVA_HOME%
for /f "delims=" %%v in ('java -version 2^>^&1 ^| findstr /I "version"') do echo   java : %%v
echo.
echo --- 3. Maven ---
set "MVN_CMD="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN_CMD for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%i"
if not defined MVN_CMD for /d %%D in ("C:\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("D:\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if defined MVN_CMD (echo   找到: %MVN_CMD%) else (echo   [!] PATH 里没有 mvn，启动后端.bat 会自动找 IDEA 自带的 Maven)
echo.
echo --- 4. 目录 ---
if exist "node_modules\vite\bin\vite.js" (echo   node_modules : 在，前端可直接启动) else (echo   node_modules : 不在，前端要先 npm install)
if exist "..\springBootTest\pom.xml" (echo   后端目录     : %~dp0..\springBootTest) else (echo   后端目录     : 不在前端同级)
if exist "D:\workSpace\springBootTest\pom.xml" echo   后端目录     : D:\workSpace\springBootTest
if exist "D:\workSpace\springBootTest\target" (echo   target       : 在) else (echo   target       : 不在，后端要重新编译)
if exist "%USERPROFILE%\.m2\repository" (echo   maven 本地库 : %USERPROFILE%\.m2\repository) else (echo   [!] maven 本地库不存在，第一次拉依赖会比较慢)
echo.
echo --- 5. 端口 / 服务 ---
echo   MySQL 3306:
netstat -ano -p TCP | findstr /I "LISTENING" | findstr ":3306" || echo     [!] 3306 没在监听，MySQL 可能没启动
echo   Vite 5173:
netstat -ano -p TCP | findstr /I "LISTENING" | findstr ":5173" || echo     未启动
echo   Spring Boot 8081:
netstat -ano -p TCP | findstr /I "LISTENING" | findstr ":8081" || echo     未启动
echo.
echo ==========================================================
echo   自检结束
echo ==========================================================
echo.
echo 按任意键关闭本窗口...
pause >nul
endlocal
