@echo off
chcp 936 >nul
setlocal
title 小马王国 - 一键启动
cd /d "%~dp0"

echo ==========================================================
echo   小马王国 - 一键启动    后端 8081  +  前端 5173
echo ==========================================================
echo.

if not exist "启动后端.bat" (
    echo [X] 缺少 启动后端.bat
    goto :bye
)
if not exist "启动前端.bat" (
    echo [X] 缺少 启动前端.bat
    goto :bye
)

echo [1/2] 打开【后端】窗口，先等它编译启动...
start "小马王国-后端" cmd /k "启动后端.bat"
echo       等 20 秒，让后端先起来。后端没起来也不影响前端运行。
timeout /t 20 /nobreak >nul

echo [2/2] 打开【前端】窗口...
start "小马王国-前端" cmd /k "启动前端.bat"

echo.
echo ----------------------------------------------------------
echo 两个窗口都打开了:
echo   后端窗口 = Spring Boot / 8081
echo   前端窗口 = Vite        / 5173
echo 停止服务: 双击 停止全部.bat，或者直接关掉那两个黑窗口。
echo ----------------------------------------------------------
echo.
echo 按任意键关闭本引导窗口...
pause >nul
:bye
endlocal
