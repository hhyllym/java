@echo off
chcp 936 >nul
setlocal
title 小马王国 - 前端 (Vite 5173)
cd /d "%~dp0"

rem ================= 可调参数 =================
set "NPM_REGISTRY=https://registry.npmmirror.com"
rem 镜像源装不上会自动换回官方源，一般不用改
rem ===========================================

echo ==========================================================
echo   小马王国 - 启动前端
echo   项目目录: %CD%
echo   访问地址: http://localhost:5173
echo ==========================================================
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo [X] 找不到 node 命令，请先安装 Node.js 或把它加入 PATH。
    goto :bye
)
for /f "delims=" %%v in ('node -v 2^>nul') do echo [1/3] Node 版本 : %%v
for /f "delims=" %%v in ('npm -v  2^>nul') do echo [1/3] npm  版本 : %%v
echo.

if exist "node_modules\vite\bin\vite.js" goto :run

echo [2/3] 没检测到 node_modules（每次开机都会被清掉），开始装依赖...
echo       第一次大约 1~3 分钟，请耐心等待。
echo.
call npm install --registry=%NPM_REGISTRY% --no-audit --no-fund
if errorlevel 1 (
    echo.
    echo [!] 镜像源安装失败，改用官方源重试...
    call npm install --no-audit --no-fund
    if errorlevel 1 (
        echo.
        echo [X] 依赖安装失败，请检查网络后重新运行本脚本。
        goto :bye
    )
)
if not exist "node_modules\vite\bin\vite.js" (
    echo [X] 装完后仍然找不到 vite，依赖不完整，请重新运行本脚本。
    goto :bye
)
echo.
echo       依赖安装完成。
echo.
goto :run

:run
echo [3/3] 启动 Vite 开发服务器...
echo       浏览器会自动打开 http://localhost:5173
echo       要停止: 在本窗口按 Ctrl+C，或双击 停止全部.bat
echo ----------------------------------------------------------
echo.
call npm run dev

:bye
echo.
echo 前端服务已退出。按任意键关闭本窗口...
pause >nul
endlocal
