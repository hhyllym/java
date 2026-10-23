@echo off
chcp 936 >nul
setlocal enabledelayedexpansion
title 小马王国 - 数据库备份/恢复
cd /d "%~dp0"

set "DB_NAME=mlp_login"
set "DUMP_EXE="
set "MYSQL_EXE="
set "BACKUP_DIR=%~dp0数据库备份"
set "LATEST=%BACKUP_DIR%\mlp_login_latest.sql"

rem ---------- 自动找 MySQL 工具 ----------
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" set "DUMP_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined DUMP_EXE for /d %%D in ("C:\Program Files\MySQL\*") do if not defined DUMP_EXE if exist "%%~D\bin\mysqldump.exe" set "DUMP_EXE=%%~D\bin\mysqldump.exe"
if not defined MYSQL_EXE for /d %%D in ("C:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined DUMP_EXE for /d %%D in ("D:\Program Files\MySQL\*") do if not defined DUMP_EXE if exist "%%~D\bin\mysqldump.exe" set "DUMP_EXE=%%~D\bin\mysqldump.exe"
if not defined MYSQL_EXE for /d %%D in ("D:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"

echo ==========================================================
echo   小马王国 - 数据库备份 / 恢复
echo   数据库: %DB_NAME%    备份文件: 数据库备份\mlp_login_latest.sql
echo ==========================================================
echo.

if not defined DUMP_EXE (
    echo [X] 找不到 mysqldump.exe，请确认 MySQL 装在 C 盘或 D 盘的 Program Files 下。
    goto :bye
)
if not defined MYSQL_EXE (
    echo [X] 找不到 mysql.exe，请确认 MySQL 已安装。
    goto :bye
)

rem ---------- 判断模式：双击=备份，传 restore=恢复 ----------
if /I "%~1"=="restore" goto :do_restore

:do_backup
echo [1/2] 正在备份 %DB_NAME% ...
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%" >nul 2>nul
"%DUMP_EXE%" -uroot -p123456 --default-character-set=utf8mb4 --routines --single-transaction %DB_NAME% > "%LATEST%" 2>nul
if errorlevel 1 (
    echo [X] 备份失败。检查 MySQL 服务有没有启动。
    del "%LATEST%" >nul 2>nul
    goto :bye
)
for %%F in ("%LATEST%") do set "SIZE=%%~zF"
if %SIZE% LSS 1000 (
    echo [X] 备份文件只有 %SIZE% 字节，内容不对，已删除。检查 MySQL。
    del "%LATEST%" >nul 2>nul
    goto :bye
)
echo [2/2] 备份完成 -^> 数据库备份\mlp_login_latest.sql  (约 !SIZE! 字节)
echo.
echo 提示：这个文件会跟着 git 一起推送，D 盘重置后拉下来就能恢复。
echo       恢复方法：双击本脚本后选 2，或在启动脚本目录运行 备份数据库.bat restore
goto :bye

:do_restore
if not exist "%LATEST%" (
    echo [X] 找不到备份文件 %LATEST%
    echo     先运行一次备份。
    goto :bye
)
for %%F in ("%LATEST%") do set "SIZE=%%~zF"
echo 找到备份：数据库备份\mlp_login_latest.sql  (约 !SIZE! 字节)
echo 警告：恢复会把 mlp_login 库整库覆盖（先用当前数据重建再导入）！
if /I "%~2"=="force" goto :skip_confirm
echo.
:skip_confirm
if not "%~2"=="force" choice /C YN /M "确认恢复请按 Y，取消按 N"
if not "%~2"=="force" if errorlevel 2 goto :bye

echo.
echo [1/2] 重建空库 ...
"%MYSQL_EXE%" -uroot -p123456 -e "DROP DATABASE IF EXISTS %DB_NAME%; CREATE DATABASE %DB_NAME% DEFAULT CHARSET utf8mb4;" 2>nul
if errorlevel 1 (
    echo [X] 重建库失败，检查 MySQL 是否启动。
    goto :bye
)
echo [2/2] 导入备份 ...
"%MYSQL_EXE%" -uroot -p123456 --default-character-set=utf8mb4 %DB_NAME% < "%LATEST%" 2>nul
if errorlevel 1 (
    echo [X] 导入失败。
    goto :bye
)
echo.
echo [OK] 恢复完成！双击 启动全部.bat 就能用了。

:bye
echo.
echo 按任意键关闭本窗口 ...
pause >nul
endlocal
exit /b
