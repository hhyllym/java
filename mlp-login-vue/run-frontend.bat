@echo off
chcp 936 >nul
title MLP Frontend - 5173
cd /d "%~dp0"

set "NODE_EXE=C:\Users\18017\.workbuddy\binaries\node\versions\22.22.2-3\node.exe"
if not exist "%NODE_EXE%" set "NODE_EXE=node"

echo.
echo  ============================================
echo   小马王国 - 前端启动脚本
echo   Vite dev server:  http://localhost:5173
echo   按 Ctrl+C 可停止
echo  ============================================
echo.

"%NODE_EXE%" node_modules\vite\bin\vite.js --port 5173 --strictPort

echo.
echo 前端已退出。
pause
