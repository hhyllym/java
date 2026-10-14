@echo off
chcp 936 >nul
setlocal
title 小马王国 - 后端 (Spring Boot 8081)
cd /d "%~dp0"

rem ================= 可调参数 =================
rem 后端项目目录。留空 = 按下面顺序自动查找。
rem 自动找不到就把下面这行前面的 rem 去掉，写死路径：
rem set "BACKEND_DIR=D:\workSpace\springBootTest"
set "BACKEND_DIR="
set "APP_PORT=8081"
rem ===========================================

echo ==========================================================
echo   小马王国 - 启动后端 (Spring Boot)
echo   脚本目录: %CD%
echo   端口: %APP_PORT%      接口前缀: /api
echo ==========================================================
echo.

rem ---------- 0) 清掉可能顶掉端口的残留环境变量 ----------
rem 有些工具（IDE / 沙箱 / 终端）会设 SERVER_PORT 或 SERVER__PORT，
rem Spring Boot 读到之后就会把下面的 8081 顶掉，必须先清掉。
set "SERVER_PORT="
set "SERVER__PORT="

rem ---------- 0.1) 端口占用检查 ----------
call :port_up %APP_PORT%
if "%UP%"=="1" (
    echo [注意] 端口 %APP_PORT% 已经有服务在跑，不用重复启动。
    echo        要重启请先双击 停止全部.bat。
    goto :bye
)

rem ---------- 1) 定位后端项目 ----------
if defined BACKEND_DIR goto :dir_ok
for %%D in (
    "D:\workSpace\springBootTest"
    "C:\workSpace\springBootTest"
    "%~dp0..\springBootTest"
    "%~dp0..\..\springBootTest"
    "%~dp0..\..\workSpace\springBootTest"
    "%~dp0..\..\..\workSpace\springBootTest"
    "%~dp0backend\springBootTest"
) do if not defined BACKEND_DIR if exist "%%~fD\pom.xml" set "BACKEND_DIR=%%~fD"
:dir_ok
if not defined BACKEND_DIR (
    echo [X] 找不到后端项目（需要有 pom.xml）。
    echo     默认会去找 D:\workSpace\springBootTest
    echo     找不到就用记事本打开本脚本，把 BACKEND_DIR 那一行改成你的真实路径。
    goto :bye
)
echo [1/4] 后端目录 : %BACKEND_DIR%

rem ---------- 1.1) 检查登录注册代码在不在（这台机器会重置 D 盘，代码可能被还原掉） ----------
if not exist "%BACKEND_DIR%\src\main\java\com\fenglin\springboottest\controller\AuthController.java" (
    echo.
    echo [注意] 这个后端里找不到 AuthController.java，说明代码被还原成空壳了。
    echo        请先双击 恢复后端.bat 把登录 / 注册代码装回去，再运行本脚本。
    echo        现在硬启动也能起来，但前端的登录和注册一定会 404。
    echo.
    echo 按任意键继续 ...
    pause >nul
)

rem ---------- 2) 定位 JDK ----------
if not defined JAVA_HOME if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" set "JAVA_HOME=C:\Program Files\Zulu\zulu-21"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Zulu\*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Java\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("D:\Program Files\Java\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /d %%J in ("D:\Java\jdk*") do if not defined JAVA_HOME if exist "%%~J\bin\java.exe" set "JAVA_HOME=%%~J"
if not defined JAVA_HOME for /f "delims=" %%i in ('where java 2^>nul') do if not defined JAVA_HOME set "JAVA_HOME=%%~dpi.."
if not defined JAVA_HOME (
    echo [X] 找不到 JDK，后端跑不起来。请安装 JDK 17 或 JDK 21。
    goto :bye
)
for %%J in ("%JAVA_HOME%") do set "JAVA_HOME=%%~fJ"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [2/4] JAVA_HOME : %JAVA_HOME%

rem ---------- 3) 定位 Maven ----------
set "MVN_CMD="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN_CMD if defined M2_HOME if exist "%M2_HOME%\bin\mvn.cmd" set "MVN_CMD=%M2_HOME%\bin\mvn.cmd"
if not defined MVN_CMD for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%i"
if not defined MVN_CMD for /d %%D in ("D:\IntelliJ*" "C:\IntelliJ*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("D:\Program Files\JetBrains\*" "C:\Program Files\JetBrains\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("C:\*" "D:\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("D:\apache-maven*" "C:\apache-maven*" "D:\maven*" "C:\maven*") do if not defined MVN_CMD if exist "%%~D\bin\mvn.cmd" set "MVN_CMD=%%~D\bin\mvn.cmd"
if not defined MVN_CMD if exist "%BACKEND_DIR%\mvnw.cmd" set "MVN_CMD=%BACKEND_DIR%\mvnw.cmd"
if not defined MVN_CMD (
    echo [X] 找不到 Maven。装一个 Maven 并加入 PATH，或者装 IntelliJ IDEA（自带 Maven）。
    goto :bye
)
echo [3/4] Maven     : %MVN_CMD%

rem ---------- 4) 启动 ----------
cd /d "%BACKEND_DIR%"
echo [4/4] 编译并启动 Spring Boot（端口 %APP_PORT%）...
echo       target 每次开机都会被清掉，所以每次都会重新编译，大约 10~40 秒。
echo       看到 Started SpringBootTestApplication 就是起来了，别关这个窗口。
echo       要停止：本窗口按 Ctrl+C，或者双击 停止全部.bat
echo ----------------------------------------------------------
echo.
rem --server.port 直接写进启动参数，优先级最高，保证就是 8081
call "%MVN_CMD%" -DskipTests spring-boot:run -Dspring-boot.run.arguments=--server.port=%APP_PORT%

:bye
echo.
echo 后端服务已退出。按任意键关闭本窗口 ...
pause >nul
endlocal
exit /b

rem ================= 内部子过程 =================
:port_up
rem 参数1=端口  结果放进 UP (1=有人在监听 0=没有)
rem 注意：这里故意不写 netstat -p TCP。Vite 只监听 IPv6 的 [::1]:5173，
rem 而 -p TCP 只列 IPv4 表，会导致 5173 永远检测不到（浏览器不弹、杀不掉）。
rem 不带 -p 同时列 IPv4/IPv6，tokens=5 都是 PID。
set "UP=0"
for /f "tokens=5" %%A in ('netstat -ano ^| findstr /I "LISTENING" ^| findstr ":%~1 "') do set "UP=1"
goto :eof
