@echo off
chcp 936 >nul
setlocal
title 小马王国 - 环境自检
cd /d "%~dp0"

if not exist "..\mlp-login-vue\环境检测.bat" (
    echo [X] 找不到 ..\mlp-login-vue\环境检测.bat
    echo     请确认 mlp-login-vue 目录就在本脚本旁边。
    echo.
    echo 按任意键关闭本窗口 ...
    pause >nul
    goto :bye
)

echo [转发] 交给 ..\mlp-login-vue\环境检测.bat 执行 ...
echo.
call "..\mlp-login-vue\环境检测.bat"

:bye
endlocal
exit /b
