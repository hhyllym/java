@echo off
chcp 936 >nul
setlocal
title 小马王国 - 停止前后端

echo ==========================================================
echo   停止前端 5173 和 后端 8081
echo ==========================================================
echo.

call :kill_port 5173 前端
call :kill_port 8081 后端

if "%F_5173%%F_8081%"=="00" (
    echo 没有发现正在运行的前端 / 后端服务。
) else (
    echo.
    echo 等进程退出 ...
    timeout /t 2 /nobreak >nul
    call :port_up 5173
    call :port_up 8081
)
echo.
echo 提示：如果还有黑窗口没关掉，点右上角的 X 关掉就行。
echo.
echo 按任意键关闭本窗口 ...
pause >nul
endlocal
exit /b

:kill_port
rem 参数1=端口 参数2=名字
set "F_%~1=0"
for /f "tokens=5" %%A in ('netstat -ano ^| findstr /I "LISTENING" ^| findstr ":%~1 "') do (
    if not "%%A"=="0" (
        echo   [停止] %~2 端口 %~1  -^>  进程 PID %%A
        taskkill /F /PID %%A >nul 2>nul
        set "F_%~1=1"
    )
)
goto :eof

:port_up
set "UP=0"
for /f "tokens=5" %%A in ('netstat -ano ^| findstr /I "LISTENING" ^| findstr ":%~1 "') do set "UP=1"
if "%UP%"=="1" echo   [注意] 端口 %~1 还在监听，可能被别的程序占用了。
goto :eof
