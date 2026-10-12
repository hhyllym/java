@echo off
chcp 936 >nul
setlocal
title 小马王国 - 停止前后端

echo ==========================================================
echo   停止前端 5173 和 后端 8081
echo ==========================================================
echo.

set "FOUND=0"
for %%P in (5173 8081) do (
    for /f "tokens=5" %%A in ('netstat -ano -p TCP ^| findstr /I "LISTENING" ^| findstr ":%%P"') do (
        if not "%%A"=="0" (
            echo [停止] 端口 %%P  的进程 PID %%A
            taskkill /F /PID %%A >nul 2>nul
            set "FOUND=1"
        )
    )
)

if "%FOUND%"=="0" echo 没有发现正在运行的前端/后端服务。
echo.
echo 提示: 如果还有黑窗口没关掉，点右上角 X 关掉就行。
echo.
echo 按任意键关闭本窗口...
pause >nul
endlocal
