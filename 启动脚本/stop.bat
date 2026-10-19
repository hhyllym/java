@echo off
chcp 65001 >nul
echo 正在停止小马王国前后端...

echo [1/2] 停止后端（8081）...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8081" ^| findstr LISTENING') do taskkill /f /pid %%p >nul 2>&1

echo [2/2] 停止前端（5173）...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173" ^| findstr LISTENING') do taskkill /f /pid %%p >nul 2>&1

echo.
echo 已停止（MySQL 服务保持运行，数据库不受影响）
pause
