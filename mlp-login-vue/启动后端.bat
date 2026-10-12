@echo off
chcp 936 >nul
setlocal
title 小马王国 - 后端 (Spring Boot 8081)
cd /d "%~dp0"

rem ================= 可调参数 =================
rem 后端项目目录。留空 = 自动查找:
rem   1. 前端同级 ..\springBootTest
rem   2. 前端上级 ..\..\springBootTest
rem   3. D:\workSpace\springBootTest
rem 自动找不到就把下面这行前面的 rem 去掉，写死路径:
rem set "BACKEND_DIR=D:\workSpace\springBootTest"
set "BACKEND_DIR="
set "APP_PORT=8081"
rem ===========================================

echo ==========================================================
echo   小马王国 - 启动后端 (Spring Boot)
echo   端口: %APP_PORT%      接口前缀: /api
echo ==========================================================
echo.

rem ---------- 1) 定位后端项目 ----------
if defined BACKEND_DIR goto :dir_ok
for %%D in (
    "%~dp0..\springBootTest"
    "%~dp0..\..\springBootTest"
    "%~dp0backend\springBootTest"
    "%~dp0..\..\workSpace\springBootTest"
    "D:\workSpace\springBootTest"
    "C:\workSpace\springBootTest"
) do if not defined BACKEND_DIR if exist "%%~fD\pom.xml" set "BACKEND_DIR=%%~fD"
:dir_ok
if not defined BACKEND_DIR (
    echo [X] 找不到后端项目，需要有 pom.xml 的 springBootTest 目录。
    echo     可以放到 %CD%\..\springBootTest
    echo     或者用记事本打开本脚本，修改 BACKEND_DIR 那一行。
    goto :bye
)
echo [1/4] 后端目录 : %BACKEND_DIR%

rem ---------- 2) 定位 JDK ----------
if not defined JAVA_HOME if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" set "JAVA_HOME=C:\Program Files\Zulu\zulu-21"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Zulu\*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Java\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("D:\Program Files\Java\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /f "delims=" %%i in ('where java 2^>nul') do if not defined JAVA_HOME set "JAVA_HOME=%%~dpi.."
if not defined JAVA_HOME (
    echo [X] 找不到 JDK，后端跑不起来。请安装 JDK 17 或 JDK 21。
    goto :bye
)
for %%J in ("%JAVA_HOME%") do set "JAVA_HOME=%%~fJ"
echo [2/4] JAVA_HOME : %JAVA_HOME%

rem ---------- 3) 定位 Maven ----------
set "MVN_CMD="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN_CMD if defined M2_HOME if exist "%M2_HOME%\bin\mvn.cmd" set "MVN_CMD=%M2_HOME%\bin\mvn.cmd"
if not defined MVN_CMD for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%i"
if not defined MVN_CMD for /d %%D in ("C:\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("D:\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("C:\Program Files\JetBrains\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("D:\Program Files\JetBrains\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD if exist "%BACKEND_DIR%\mvnw.cmd" set "MVN_CMD=%BACKEND_DIR%\mvnw.cmd"
if not defined MVN_CMD (
    echo [X] 找不到 Maven。装一个 Maven 并加入 PATH，或者装 IntelliJ IDEA，它自带 Maven。
    goto :bye
)
echo [3/4] Maven     : %MVN_CMD%

rem ---------- 4) 启动 ----------
cd /d "%BACKEND_DIR%"
echo [4/4] 编译并启动 Spring Boot ...
echo       target 每次开机都被清掉，所以每次都会重新编译，大约 10~40 秒。
echo       要停止: 在本窗口按 Ctrl+C，或双击 停止全部.bat
echo ----------------------------------------------------------
echo.
call "%MVN_CMD%" -DskipTests spring-boot:run

:bye
echo.
echo 后端服务已退出。按任意键关闭本窗口...
pause >nul
endlocal
