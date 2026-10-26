@echo off
chcp 936 >nul
setlocal
title 小马王国 - 环境自检
cd /d "%~dp0..\mlp-login-vue"

echo ==========================================================
echo   环境自检      脚本目录: %CD%
echo ==========================================================
echo --- 1. Node / npm ---
set "NODE_EXE="
for /f "delims=" %%i in ('where node.exe 2^>nul') do if not defined NODE_EXE set "NODE_EXE=%%i"
if not defined NODE_EXE if exist "D:\Program Files\nodejs\node.exe" set "NODE_EXE=D:\Program Files\nodejs\node.exe"
if not defined NODE_EXE if exist "%ProgramFiles%\nodejs\node.exe" set "NODE_EXE=%ProgramFiles%\nodejs\node.exe"
if not defined NODE_EXE for /d %%D in ("%USERPROFILE%\.workbuddy\binaries\node\versions\*") do if not defined NODE_EXE if exist "%%~D\node.exe" set "NODE_EXE=%%~D\node.exe"
if defined NODE_EXE (
    echo   node.exe : %NODE_EXE%
    for %%N in ("%NODE_EXE%") do set "PATH=%%~dpN;%PATH%"
) else (
    echo   [X] 找不到 node.exe，前端起不来
)
for /f "delims=" %%v in ('node -v 2^>nul') do echo   node : %%v
for /f "delims=" %%v in ('npm -v  2^>nul') do echo   npm  : %%v
echo.
echo --- 2. JDK ---
echo   JAVA_HOME = %JAVA_HOME%
if exist "C:\Program Files\Zulu\zulu-21\bin\java.exe" echo   找到 Zulu 21 : C:\Program Files\Zulu\zulu-21
for /f "delims=" %%v in ('java -version 2^>^&1 ^| findstr /I "version"') do echo   java : %%v
echo.
echo --- 3. Maven ---
set "MVN_CMD="
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MVN_CMD for /f "delims=" %%i in ('where mvn.cmd 2^>nul') do if not defined MVN_CMD set "MVN_CMD=%%i"
if not defined MVN_CMD for /d %%D in ("D:\IntelliJ*" "C:\IntelliJ*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if not defined MVN_CMD for /d %%D in ("C:\*" "D:\*") do if not defined MVN_CMD if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=%%~D\plugins\maven\lib\maven3\bin\mvn.cmd"
if defined MVN_CMD (echo   找到 : %MVN_CMD%) else (echo   [注意] PATH 里没有 mvn，启动后端.bat 会自动去找 IDEA 自带的 Maven)
echo.
echo --- 4. 目录 ---
if exist "node_modules\vite\bin\vite.js" (echo   node_modules : 在，前端可直接启动) else (echo   node_modules : 不在，前端启动时要先 npm install)
if exist "dist\index.html" (echo   dist         : 在，前端装不上依赖时可以静态预览兜底) else (echo   dist         : 不在)
if exist "D:\workSpace\springBootTest\pom.xml" (echo   后端项目     : D:\workSpace\springBootTest) else (echo   [X] 后端项目     : 不在 D:\workSpace\springBootTest)
if exist "D:\workSpace\springBootTest\target" (echo   target       : 在) else (echo   target       : 不在，后端启动时要重新编译)
if exist "%USERPROFILE%\.m2\repository" (echo   maven 本地库 : %USERPROFILE%\.m2\repository) else (echo   [注意] maven 本地库不存在，第一次拉依赖会比较慢)
echo.
echo --- 5. 端口 / 服务 ---
echo   MySQL 3306:
netstat -ano | findstr /I "LISTENING" | findstr ":3306 " || echo     [注意] 3306 没在监听，MySQL 可能没启动
echo   Vite 5173:
netstat -ano | findstr /I "LISTENING" | findstr ":5173 " || echo     未启动
echo   Spring Boot 8081:
netstat -ano | findstr /I "LISTENING" | findstr ":8081 " || echo     未启动
echo.
echo ==========================================================
echo   自检结束
echo ==========================================================
echo.
echo 按任意键关闭本窗口 ...
pause >nul
endlocal
exit /b
