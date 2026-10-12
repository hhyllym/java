@echo off
chcp 936 >nul
cd /d "%~dp0mlp-login-vue"
if not exist "启动全部.bat" (
    echo [X] 找不到 mlp-login-vue\启动全部.bat
    pause
    exit /b 1
)
call "启动全部.bat"
