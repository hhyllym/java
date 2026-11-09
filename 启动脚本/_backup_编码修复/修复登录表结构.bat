@echo off
setlocal
title Fix MLP table schema
cd /d "%~dp0"

echo ==========================================================
echo   Fix MLP database schema (missing columns)
echo   - t_user.permission         : fixes login 500
echo   - t_chat_message 3 columns  : fixes chat / invite 500
echo   - t_chat_message.content    : varchar(500) to MEDIUMTEXT
echo   Safe: only ADD / MODIFY columns, never drops tables or data.
echo   Idempotent: safe to run repeatedly.
echo ==========================================================
echo.

set "MYSQL_EXE="
for /d %%D in ("C:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
for /d %%D in ("D:\Program Files\MySQL\*") do if not defined MYSQL_EXE if exist "%%~D\bin\mysql.exe" set "MYSQL_EXE=%%~D\bin\mysql.exe"
if not defined MYSQL_EXE (
    echo [X] mysql.exe not found. Install the MySQL client first.
    goto :bye
)
echo MySQL client : %MYSQL_EXE%
echo.

set "FIXSQL=SET @s1=(SELECT IF(COUNT(*)=0,'ALTER TABLE mlp_login.t_user ADD COLUMN permission TINYINT NOT NULL DEFAULT 1','SELECT ''permission exists''') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='mlp_login' AND TABLE_NAME='t_user' AND COLUMN_NAME='permission'); PREPARE p1 FROM @s1; EXECUTE p1; DEALLOCATE PREPARE p1;"
set "FIXSQL=%FIXSQL% SET @s2=(SELECT IF(COUNT(*)=0,'ALTER TABLE mlp_login.t_chat_message ADD COLUMN msg_type VARCHAR(16) NOT NULL DEFAULT ''text''','SELECT ''msg_type exists''') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='mlp_login' AND TABLE_NAME='t_chat_message' AND COLUMN_NAME='msg_type'); PREPARE p2 FROM @s2; EXECUTE p2; DEALLOCATE PREPARE p2;"
set "FIXSQL=%FIXSQL% SET @s3=(SELECT IF(COUNT(*)=0,'ALTER TABLE mlp_login.t_chat_message ADD COLUMN recalled TINYINT NOT NULL DEFAULT 0','SELECT ''recalled exists''') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='mlp_login' AND TABLE_NAME='t_chat_message' AND COLUMN_NAME='recalled'); PREPARE p3 FROM @s3; EXECUTE p3; DEALLOCATE PREPARE p3;"
set "FIXSQL=%FIXSQL% SET @s4=(SELECT IF(COUNT(*)=0,'ALTER TABLE mlp_login.t_chat_message ADD COLUMN extra VARCHAR(255) NULL DEFAULT ''''','SELECT ''extra exists''') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='mlp_login' AND TABLE_NAME='t_chat_message' AND COLUMN_NAME='extra'); PREPARE p4 FROM @s4; EXECUTE p4; DEALLOCATE PREPARE p4;"
set "FIXSQL=%FIXSQL% SET @s5=(SELECT IF(DATA_TYPE='mediumtext','SELECT ''content already mediumtext''','ALTER TABLE mlp_login.t_chat_message MODIFY COLUMN content MEDIUMTEXT NOT NULL') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='mlp_login' AND TABLE_NAME='t_chat_message' AND COLUMN_NAME='content'); PREPARE p5 FROM @s5; EXECUTE p5; DEALLOCATE PREPARE p5;"
set "FIXSQL=%FIXSQL% SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='mlp_login' AND ((TABLE_NAME='t_user' AND COLUMN_NAME='permission') OR TABLE_NAME='t_chat_message') ORDER BY TABLE_NAME,ORDINAL_POSITION;"

"%MYSQL_EXE%" -uroot -p123456 -h127.0.0.1 -P3306 --default-character-set=utf8mb4 -e "%FIXSQL%"
if errorlevel 1 (
    echo.
    echo [X] Fix failed. Check MySQL is running and the root password is 123456.
    goto :bye
)
echo.
echo [OK] Schema is up to date. Start the app as usual.

:bye
echo.
echo Press any key to close ...
pause >nul
endlocal
exit /b
