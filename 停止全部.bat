@echo off
chcp 936 >nul
cd /d "%~dp0mlp-login-vue"
if not exist "停止全部.bat" (
    echo [X] 找不到 mlp-login-vue\停止全部.bat
    pause
    exit /b 1
)
call "停止全部.bat"
