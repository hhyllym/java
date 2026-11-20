@echo off
chcp 936 >nul
setlocal EnableExtensions
title 小马乐园 - 恢复后端代码
cd /d "%~dp0"

set "APP_PORT=8081"

rem 是否静默模式：被 启动后端.bat 调用时传 auto，跳过按键等待
set "AUTO="
if /I "%~1"=="auto" set "AUTO=1"

rem 检查模式：只定位路径，不复制源码、不导入数据库
set "CHECK_ONLY="
if /I "%~1"=="check" set "CHECK_ONLY=1"

rem 判断后端代码是否完整的标志文件
set "AUTH=src\main\java\com\fenglin\springboottest\controller\AuthController.java"

rem 完整数据库备份（带数据），优先用它恢复
set "DUMP_FILE=%~dp0数据库备份\mlp_login_latest.sql"

echo ==========================================================
echo   小马乐园 - 恢复后端代码
echo ==========================================================
echo.

rem ---------- 1) 定位目标后端目录 ----------
rem 由 启动后端.bat 调用时，直接沿用它已经选好的 BACKEND_DIR
set "TARGET="
if defined BACKEND_DIR if exist "%BACKEND_DIR%\pom.xml" set "TARGET=%BACKEND_DIR%"
if not defined TARGET for %%D in (
    "D:\workSpace\java\backend-src.bak"
    "%~dp0..\backend-src.bak"
    "%~dp0..\backend-src"
    "D:\workSpace\java\backend-src"
    "D:\workSpace\springBootTest"
    "C:\workSpace\springBootTest"
    "%USERPROFILE%\Desktop\springBootTest"
    "D:\springBootTest"
) do if not defined TARGET if exist "%%~fD\pom.xml" set "TARGET=%%~fD"
if not defined TARGET (
    echo [X] 找不到目标后端目录，需要目录里有 pom.xml。
    echo     默认会找 D:\workSpace\java\backend-src.bak
    goto :bye
)
echo 目标目录 : %TARGET%
echo.

rem ---------- 2) 判断是否需要复制源码 ----------
if exist "%TARGET%\%AUTH%" goto :code_ok

set "SRC="
if defined RESTORE_SRC if exist "%RESTORE_SRC%\%AUTH%" set "SRC=%RESTORE_SRC%"
for %%D in (
    "%~dp0..\backend-src"
    "D:\workSpace\java\backend-src.bak"
    "%~dp0..\backend-src.bak"
    "D:\workSpace\java\backend-src"
    "D:\workSpace\springBootTest"
    "C:\workSpace\springBootTest"
    "%USERPROFILE%\Desktop\springBootTest"
    "D:\springBootTest"
) do if not defined SRC if /I not "%%~fD"=="%TARGET%" if exist "%%~fD\%AUTH%" set "SRC=%%~fD"
if not defined SRC (
    echo [X] 没找到完整的源码备份。
    echo     备份目录里必须有：%AUTH%
    goto :bye
)
echo 源码来源 : %SRC%
echo.
if defined CHECK_ONLY goto :check_done
echo 接下来会把备份里的源码、pom.xml、SQL 复制过去，同名文件会被覆盖：
echo   %TARGET%\pom.xml
echo   %TARGET%\src\
echo   %TARGET%\sql\
echo.
echo 只覆盖这些文件，target\ 和 log\ 不会动，也不会删你别的文件。
echo.
echo 按任意键开始，按 Ctrl+C 取消 ...
if not defined AUTO pause >nul
echo.

if not exist "%TARGET%" mkdir "%TARGET%" >nul 2>nul

echo [1/3] 复制源码 ...
xcopy "%SRC%\src" "%TARGET%\src" /E /Y /I /Q >nul
if errorlevel 1 (echo [X] 复制 src 失败) else (echo       src  -^> OK)

echo [2/3] 复制 pom.xml ...
copy /Y "%SRC%\pom.xml" "%TARGET%\pom.xml" >nul
if errorlevel 1 (echo [X] 复制 pom.xml 失败) else (echo       pom.xml -^> OK)

if exist "%SRC%\sql" (
    xcopy "%SRC%\sql" "%TARGET%\sql" /E /Y /I /Q >nul
    echo       sql  -^> OK
)
goto :db_init

:code_ok
echo [OK] 目标目录已有登录 / 注册代码，无需复制源码。
echo.
if defined CHECK_ONLY goto :check_done
goto :db_init

:check_done
echo [检查模式] 路径检查完成，未复制源码、未导入数据库。
goto :bye

:db_init
rem ---------- 3) 建库 / 导表 ----------
echo [3/3] 建数据库 mlp_login 并导入建表脚本 ...
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%D in ("C:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%D in ("D:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined MYSQL_EXE (
    echo       [注意] 没找到 mysql.exe，跳过建库。
    echo              请手工按顺序执行 %TARGET%\sql\ 下的 SQL 文件。
) else (
    "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 -e "CREATE DATABASE IF NOT EXISTS mlp_login DEFAULT CHARACTER SET utf8mb4;"
    if errorlevel 1 (
        echo       [注意] 建库失败，检查 MySQL 有没有启动、root 密码是不是 123456。
    ) else (
        echo       mlp_login 建库 -^> OK
        if exist "%DUMP_FILE%" (
            echo       从完整备份恢复数据 ...
            "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 mlp_login < "%DUMP_FILE%"
            if errorlevel 1 (
                echo       [注意] 完整备份导入失败，检查备份文件是否完整。
            ) else (
                echo       完整备份导入 -^> OK
            )
        ) else (
            echo       未找到完整备份，改用建表脚本 ...
            for %%F in (mlp_login.sql mlp_home.sql mlp_message.sql mlp_friend.sql mlp_extend.sql mlp_hall.sql) do (
                if exist "%TARGET%\sql\%%F" (
                    echo       - 执行 %%F ...
                    "%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 mlp_login < "%TARGET%\sql\%%F"
                    if errorlevel 1 (
                        echo         [注意] %%F 执行失败，检查 SQL 内容或 MySQL 日志。
                    ) else (
                        echo         %%F -^> OK
                    )
                )
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
