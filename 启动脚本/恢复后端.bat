@echo off
chcp 936 >nul
setlocal
title 小马王国 - 恢复后端代码
cd /d "%~dp0"

set "SRC=%~dp0..\backend-src"
set "APP_PORT=8081"

rem 是否静默模式：被 启动后端.bat 调用时传 auto，跳过两次按键等待
set "AUTO="
if /I "%~1"=="auto" set "AUTO=1"

echo ==========================================================
echo   小马王国 - 恢复后端代码
echo   备份: %SRC%
echo ==========================================================
echo.

if not exist "%SRC%\pom.xml" (
    echo [X] 找不到备份目录 %SRC%
    echo     本脚本要和 backend-src 文件夹放在一起。
    goto :bye
)

rem ---------- 1) 找后端目录 ----------
set "BACKEND_DIR="
for %%D in (
    "D:\workSpace\springBootTest"
    "C:\workSpace\springBootTest"
    "%USERPROFILE%\Desktop\springBootTest"
    "D:\springBootTest"
) do if not defined BACKEND_DIR if exist "%%~fD" set "BACKEND_DIR=%%~fD"
if not defined BACKEND_DIR (
    echo [注意] 没找到已有的后端目录，帮你新建一个 D:\workSpace\springBootTest
    echo.
    set "BACKEND_DIR=D:\workSpace\springBootTest"
)
echo 后端目录 : %BACKEND_DIR%
echo.

rem ---------- 2) 确认 ----------
echo 接下来会把备份里的源码、pom.xml、SQL 复制过去，同名文件会被覆盖：
echo   %BACKEND_DIR%\pom.xml
echo   %BACKEND_DIR%\src\
echo   %BACKEND_DIR%\sql\
echo.
echo 只覆盖这些文件，target\ 和 log\ 不会动，也不会删你别的文件。
echo.
echo 按任意键开始，按 Ctrl+C 取消 ...
if not defined AUTO pause >nul
echo.

if not exist "%BACKEND_DIR%" mkdir "%BACKEND_DIR%" >nul 2>nul

echo [1/3] 复制源码 ...
xcopy "%SRC%\src" "%BACKEND_DIR%\src" /E /Y /I /Q >nul
if errorlevel 1 (echo [X] 复制 src 失败) else (echo       src  -> OK)

echo [2/3] 复制 pom.xml ...
copy /Y "%SRC%\pom.xml" "%BACKEND_DIR%\pom.xml" >nul
if errorlevel 1 (echo [X] 复制 pom.xml 失败) else (echo       pom.xml -> OK)

if exist "%SRC%\sql" (
    xcopy "%SRC%\sql" "%BACKEND_DIR%\sql" /E /Y /I /Q >nul
    echo       sql  -> OK
)

echo [3/3] 建数据库（mlp_login：账号表 + 首页图鉴表 + 消息中心表）...
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%D in ("C:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%D in ("D:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined MYSQL_EXE (
    echo       [注意] 没找到 mysql.exe，跳过建库。
    echo              请手工按顺序执行 %BACKEND_DIR%\sql\ 下的 mlp_login.sql、mlp_home.sql、mlp_message.sql
) else (
    for %%F in (mlp_login.sql mlp_home.sql mlp_message.sql mlp_friend.sql mlp_extend.sql) do (
        if exist "%BACKEND_DIR%\sql\%%F" (
            echo       - 执行 %%F ...
            "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 --force < "%BACKEND_DIR%\sql\%%F"
            if errorlevel 1 (
                echo         [注意] %%F 执行失败，检查 MySQL 有没有启动、root 密码是不是 123456。
            ) else (
                echo         %%F -^> OK
            )
        )
    )
)

echo.
echo ----------------------------------------------------------
echo   恢复完成。接下来双击 启动全部.bat 就能用了。
echo ----------------------------------------------------------
echo.

:bye
echo 按任意键关闭本窗口 ...
if not defined AUTO pause >nul
endlocal
exit /b
