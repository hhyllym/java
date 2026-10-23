@echo off
rem 静默备份：给 Windows 计划任务调用，不弹窗、不等待按键
rem 备份 mlp_login 到 数据库备份\mlp_login_latest.sql，数据有变化时自动 git 提交
chcp 936 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "DB_NAME=mlp_login"
set "BACKUP_DIR=%~dp0数据库备份"
set "LATEST=%BACKUP_DIR%\mlp_login_latest.sql"
set "LOGFILE=%BACKUP_DIR%\自动备份日志.txt"
set "DUMP_EXE="
set "MYSQL_EXE="

if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" set "DUMP_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined DUMP_EXE for /d %%D in ("C:\Program Files\MySQL\*") do if not defined DUMP_EXE if exist "%%~D\bin\mysqldump.exe" set "DUMP_EXE=%%~D\bin\mysqldump.exe"
if not defined MYSQL_EXE for /d %%D in ("C:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined DUMP_EXE for /d %%D in ("D:\Program Files\MySQL\*") do if not defined DUMP_EXE if exist "%%~D\bin\mysqldump.exe" set "DUMP_EXE=%%~D\bin\mysqldump.exe"
if not defined MYSQL_EXE for /d %%D in ("D:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%" >nul 2>nul
set "STAMP=%DATE% %TIME%"

if not defined DUMP_EXE (
    echo [%STAMP%] 失败：找不到 mysqldump.exe >> "%LOGFILE%"
    exit /b 1
)

rem 先备份到临时文件，成功后替换，避免中途失败把好备份覆盖成半个
"%DUMP_EXE%" -uroot -p123456 --default-character-set=utf8mb4 --routines --single-transaction --skip-dump-date %DB_NAME% > "%LATEST%.tmp" 2>nul
if errorlevel 1 (
    del "%LATEST%.tmp" >nul 2>nul
    echo [%STAMP%] 失败：mysqldump 出错，检查 MySQL 服务是否启动 >> "%LOGFILE%"
    exit /b 1
)
for %%F in ("%LATEST%.tmp") do set "SIZE=%%~zF"
if !SIZE! LSS 1000 (
    del "%LATEST%.tmp" >nul 2>nul
    echo [%STAMP%] 失败：备份内容异常（!SIZE! 字节），已丢弃 >> "%LOGFILE%"
    exit /b 1
)

rem 和旧备份一样就不折腾 git
fc /b "%LATEST%" "%LATEST%.tmp" >nul 2>nul
if not errorlevel 1 (
    del "%LATEST%.tmp" >nul 2>nul
    echo [%STAMP%] 跳过：数据无变化 >> "%LOGFILE%"
    exit /b 0
)
move /y "%LATEST%.tmp" "%LATEST%" >nul

rem 数据变了：顺手提交到 git，跟着仓库上云
set "GIT="
for /f "delims=" %%i in ('where git.exe 2^>nul') do if not defined GIT set "GIT=%%i"
if not defined GIT (
    echo [%STAMP%] 已备份（!SIZE! 字节），但没有 git 命令，未提交 >> "%LOGFILE%"
    exit /b 0
)
cd /d "%~dp0.."
"%GIT%" add "启动脚本/数据库备份/mlp_login_latest.sql" >nul 2>nul
"%GIT%" commit -q -m "自动备份数据库 %DATE%" >nul 2>nul
if errorlevel 1 (
    echo [%STAMP%] 已备份（!SIZE! 字节），git 无需提交 >> "%LOGFILE%"
) else (
    echo [%STAMP%] 已备份（!SIZE! 字节）并提交 git >> "%LOGFILE%"
)
exit /b 0
