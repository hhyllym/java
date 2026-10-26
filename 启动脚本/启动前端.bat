@echo off
chcp 936 >nul
setlocal
title 小马王国 - 前端 (Vite 5173)
cd /d "%~dp0..\mlp-login-vue"

rem ================= 可调参数 =================
set "NPM_REGISTRY=https://registry.npmmirror.com"
rem 镜像源装不上会自动换回官方源，一般不用改
set "DEV_PORT=5173"
rem ===========================================

echo ==========================================================
echo   小马王国 - 启动前端
echo   项目目录: %CD%
echo   访问地址: http://localhost:%DEV_PORT%
echo ==========================================================
echo.

rem ---------- 0) 端口占用检查 ----------
call :port_up %DEV_PORT%
if "%UP%"=="1" (
    echo [注意] 端口 %DEV_PORT% 已经有前端在跑，不用重复启动。
    echo        要重启请先双击 停止全部.bat。
    goto :bye
)

rem ---------- 1) 找 node ----------
set "NODE_EXE="
for /f "delims=" %%i in ('where node.exe 2^>nul') do if not defined NODE_EXE set "NODE_EXE=%%i"
if not defined NODE_EXE if exist "D:\Program Files\nodejs\node.exe" set "NODE_EXE=D:\Program Files\nodejs\node.exe"
if not defined NODE_EXE if exist "%ProgramFiles%\nodejs\node.exe" set "NODE_EXE=%ProgramFiles%\nodejs\node.exe"
if not defined NODE_EXE if exist "%ProgramFiles(x86)%\nodejs\node.exe" set "NODE_EXE=%ProgramFiles(x86)%\nodejs\node.exe"
if not defined NODE_EXE if exist "%USERPROFILE%\AppData\Roaming\nvm\current\node.exe" set "NODE_EXE=%USERPROFILE%\AppData\Roaming\nvm\current\node.exe"
if not defined NODE_EXE for /d %%D in ("%USERPROFILE%\.workbuddy\binaries\node\versions\*") do if not defined NODE_EXE if exist "%%~D\node.exe" set "NODE_EXE=%%~D\node.exe"
if not defined NODE_EXE goto :no_node
for %%N in ("%NODE_EXE%") do set "NODE_DIR=%%~dpN"
set "PATH=%NODE_DIR%;%PATH%"

set "NPM_CMD="
for /f "delims=" %%i in ('where npm.cmd 2^>nul') do if not defined NPM_CMD set "NPM_CMD=%%i"
if not defined NPM_CMD if exist "%NODE_DIR%npm.cmd" set "NPM_CMD=%NODE_DIR%npm.cmd"
if not defined NPM_CMD goto :no_npm

echo [1/3] node : %NODE_EXE%
for /f "delims=" %%v in ('node -v 2^>nul') do echo       node 版本 : %%v
for /f "delims=" %%v in ('npm -v  2^>nul') do echo       npm  版本 : %%v
echo.

rem ---------- 2) 依赖 ----------
if exist "node_modules\vite\bin\vite.js" goto :run

echo [2/3] 没检测到 node_modules，开始装依赖 ...
echo       第一次大约 1~3 分钟，请耐心等待，不要关窗口。
echo.
call "%NPM_CMD%" install --registry=%NPM_REGISTRY% --no-audit --no-fund
if errorlevel 1 (
    echo.
    echo [注意] 镜像源安装失败，改用官方源重试 ...
    call "%NPM_CMD%" install --no-audit --no-fund
    if errorlevel 1 (
        echo.
        echo [X] 依赖安装失败，看看网络。先试静态预览兜底 ...
        goto :fallback
    )
)
if not exist "node_modules\vite\bin\vite.js" (
    echo.
    echo [注意] 装完了还是找不到 vite，依赖不完整，先试静态预览兜底 ...
    goto :fallback
)
echo.
echo       依赖安装完成。
echo.
goto :run

rem ---------- 3) 启动 vite ----------
:run
echo [3/3] 启动 Vite 开发服务器 ...
echo       浏览器马上会自动打开 http://localhost:%DEV_PORT%
echo       要停止：本窗口按 Ctrl+C，或者双击 停止全部.bat
echo ----------------------------------------------------------
echo.
start "" /b cmd /c "%~dp0打开网页.bat %DEV_PORT%"
call "%NPM_CMD%" run dev
echo.
echo 前端服务已退出。
goto :bye

rem ---------- 兜底：零依赖静态预览 ----------
:fallback
if not exist "dist\index.html" (
    echo [X] 连 dist 构建产物都没有，兜底也起不来，请检查网络后重新运行本脚本。
    goto :bye
)
echo.
echo [注意] 改用【零依赖静态预览】：直接跑 dist 里的构建产物。
echo        只读页面，不能热更新，改了 src 代码不会生效。
echo        不要再用这个模式开发，能连上接口就够用。
echo ----------------------------------------------------------
echo.
start "" /b cmd /c "%~dp0打开网页.bat %DEV_PORT%"
"%NODE_EXE%" "tools\static-server.cjs" %DEV_PORT%
echo.
echo 静态预览已退出。
goto :bye

:no_node
echo [X] 找不到 node.exe，前端没法启动。
echo     请安装 Node.js（建议 v18 以上），或者把它加入 PATH 后重试。
goto :bye

:no_npm
echo [X] 找到了 node 但找不到 npm.cmd，Node 安装可能不完整。
echo     建议重装 Node.js。
goto :bye

:bye
echo.
echo 按任意键关闭本窗口 ...
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
