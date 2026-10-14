@echo off
chcp 936 >nul
setlocal
rem 参数1 = 端口，默认 5173
set "PORT=%~1"
if "%PORT%"=="" set "PORT=5173"
set /a _N=0
:wait
set /a _N+=1
netstat -ano | findstr /I "LISTENING" | findstr ":%PORT% " >nul 2>nul
if not errorlevel 1 goto :open
if %_N% GEQ 120 goto :open
ping -n 2 127.0.0.1 >nul
goto :wait
:open
start "" "http://localhost:%PORT%/"
endlocal
exit /b
