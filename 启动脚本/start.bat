@echo off
chcp 65001 >nul
echo ============================================
echo    小马王国 mlp-login  一键启动
echo ============================================
echo.

REM --- 1. MySQL 服务 ---
sc query MySQL84 | findstr RUNNING >nul
if errorlevel 1 (
    echo [1/3] MySQL 未运行，正在启动...
    net start MySQL84
) else (
    echo [1/3] MySQL 已在运行
)

REM --- 2. 后端 8081 ---
netstat -ano | findstr ":8081" | findstr LISTENING >nul
if not errorlevel 1 (
    echo [2/3] 后端已在 8081 运行，跳过
) else (
    echo [2/3] 正在新窗口启动后端（首次要编译，稍慢）...
    start "mlp-backend" cmd /k "%~dp0backend-start.bat"
)

REM --- 3. 前端 5173 ---
netstat -ano | findstr ":5173" | findstr LISTENING >nul
if not errorlevel 1 (
    echo [3/3] 前端已在 5173 运行，跳过
) else (
    echo [3/3] 正在新窗口启动前端...
    start "mlp-frontend" cmd /k "%~dp0frontend-start.bat"
)

echo.
echo 全部就绪！浏览器打开 http://localhost:5173
timeout /t 2 >nul
start http://localhost:5173
pause
