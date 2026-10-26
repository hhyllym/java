@echo off
chcp 936 >nul
setlocal
title 小马王国 - 一键启动
cd /d "%~dp0"

set "FRONT_PORT=5173"
set "BACK_PORT=8081"
set "FRONT_ALREADY=0"

echo ==========================================================
echo   小马王国 - 一键启动
echo   前端 http://localhost:%FRONT_PORT%      后端 http://localhost:%BACK_PORT%
echo ==========================================================
echo.

if not exist "启动后端.bat" (
    echo [X] 缺少 启动后端.bat，本脚本要和 启动后端.bat 放在同一个目录。
    goto :bye
)
if not exist "启动前端.bat" (
    echo [X] 缺少 启动前端.bat，本脚本要和 启动前端.bat 放在同一个目录。
    goto :bye
)

rem -------------------- 第一步：后端 --------------------
rem 先看看前端是不是本来就在跑（是的话最后直接开浏览器，不再重复起窗口）
call :port_up %FRONT_PORT%
if "%UP%"=="1" set "FRONT_ALREADY=1"

call :port_up %BACK_PORT%
if "%UP%"=="1" goto :back_ready

echo [1/3] 打开【后端】窗口 (Spring Boot)，它先编译再启动。
start "小马王国-后端" cmd /k "%~dp0启动后端.bat"
echo       等待后端监听 %BACK_PORT% 端口，最多等 150 秒 ...
call :wait_port %BACK_PORT% 150
if "%UP%"=="1" goto :back_ready
echo.
echo [注意] 150 秒内后端没起来。常见原因：找不到 JDK / Maven、MySQL(3306) 没启动、编译报错。
echo        请切到【后端】窗口看红字，或者双击 环境检测.bat 查环境。
echo        后端没起来不影响网页打开，只是登录 / 注册连不上接口。
echo.
goto :front

:back_ready
echo [1/3] 后端 OK  ->  http://localhost:%BACK_PORT%   (接口前缀 /api)
echo.

rem -------------------- 第二步：前端 --------------------
:front
call :port_up %FRONT_PORT%
if "%UP%"=="1" goto :front_ready

echo [2/3] 打开【前端】窗口 (Vite)，前端就绪后会自动帮你开浏览器。
start "小马王国-前端" cmd /k "%~dp0启动前端.bat"
echo       第一次跑要装依赖，1~3 分钟，前端窗口在滚字就说明没死。
echo       等待前端监听 %FRONT_PORT% 端口，最多等 300 秒 ...
call :wait_port %FRONT_PORT% 300
if "%UP%"=="1" goto :front_ready
echo.
echo [注意] 前端没起来，请切到【前端】窗口看报错信息。
echo.
goto :summary

:front_ready
echo [2/3] 前端 OK  ->  http://localhost:%FRONT_PORT%
echo.

rem -------------------- 第三步：汇总 --------------------
:summary
echo [3/3] 启动结果
call :port_up %BACK_PORT%
if "%UP%"=="1" (echo       后端 %BACK_PORT% : 运行中) else (echo       后端 %BACK_PORT% : 未运行)
call :port_up %FRONT_PORT%
if "%UP%"=="1" (echo       前端 %FRONT_PORT% : 运行中) else (echo       前端 %FRONT_PORT% : 未运行)
echo.
if "%FRONT_ALREADY%"=="1" (
    echo 前端本来就在跑，直接帮你打开网页 ...
    call "%~dp0打开网页.bat" %FRONT_PORT%
)
echo ----------------------------------------------------------
echo   在浏览器打开 http://localhost:%FRONT_PORT% 就是小马王国页面
echo   登录 / 注册接口走 http://localhost:%BACK_PORT%/api/auth/*
echo   停止服务：双击 停止全部.bat
echo   起不来  ：双击 环境检测.bat
echo ----------------------------------------------------------
echo.
echo 按任意键关掉本窗口（前后端会继续在各自的窗口里跑）...
pause >nul

:bye
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

:wait_port
rem 参数1=端口 参数2=最多等多少秒  等到了 UP=1，超时 UP=0
set /a _WP=0
set /a _TICK=0
:_wp_loop
call :port_up %~1
if "%UP%"=="1" goto :eof
set /a _WP+=1
if %_WP% GEQ %~2 goto :eof
timeout /t 1 /nobreak >nul
set /a _TICK+=1
if %_TICK% LSS 20 goto :_wp_loop
set /a _TICK=0
echo       已等待 %_WP% 秒 ...
goto :_wp_loop
