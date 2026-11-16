@echo off
chcp 936 >nul
setlocal
title 小马乐园 - 脚本语法体检
cd /d "%~dp0"

echo ==========================================================
echo   批处理脚本语法体检
echo   专抓「此时不应有 ...。」这类括号语法错误
echo ==========================================================
echo.

where node >nul 2>nul
if errorlevel 1 (
    echo [X] 找不到 node.exe，跑不了这个体检工具。
    echo     请安装 Node.js，或者直接双击 启动全部.bat 试跑，看有没有报错。
    goto :bye
)

rem 不传路径参数：让工具自己用当前工作目录。
rem （cmd 把中文路径当参数传给 node 会出现编码错乱，目录名里带中文时尤其明显。）
set "CHECK_BAT_GBK=1"
node "%~dp0check-bat-syntax.cjs"
set "RC=%ERRORLEVEL%"

echo.
if "%RC%"=="0" (
    echo   结果：全部脚本语法正常。
) else (
    echo   结果：发现语法错误，请按上面提示修改后再运行。
)
echo ----------------------------------------------------------

:bye
echo.
echo 按任意键关闭本窗口 ...
pause >nul
endlocal
exit /b