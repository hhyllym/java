@echo off
chcp 936 >nul
setlocal
title 小马王国 - 推送到 GitHub (hhyllym/java)
cd /d "%~dp0"

set "REPO_URL=https://github.com/hhyllym/java.git"
set "BRANCH=main"
set "MSG=更新小马王国项目"

echo ==========================================================
echo   小马王国 - 推送代码到 GitHub
echo   仓库: https://github.com/hhyllym/java   分支: main
echo   目录: %CD%
echo ==========================================================
echo.

rem ---------- 0) 环境检查 ----------
if not exist "%~dp0..\.git" (
    echo [X] 当前目录不是 git 仓库: %~dp0..
    echo     也就是这里找不到 .git 目录。别在这个窗口里手动 git init，
    echo     先确认 .git 是不是在 %~dp0.. 下（仓库根在 java 这一层）。
    goto :bye
)
where git >nul 2>nul
if errorlevel 1 (
    echo [X] 找不到 git 命令，请先装 Git for Windows。
    goto :bye
)
for /f "delims=" %%i in ('git rev-parse --show-toplevel 2^>nul') do set "TOP=%%i"
echo 仓库根 : %TOP%
echo.

rem 防呆：node_modules 要是没被忽略，推上去能有好几百兆
if exist "%~dp0..\mlp-login-vue\node_modules" (
    git check-ignore -q "%~dp0..\mlp-login-vue/node_modules"
    if errorlevel 1 (
        echo [X] mlp-login-vue\node_modules 没有被 .gitignore 忽略，先别推！
        echo     不然会把几百兆依赖一起传上去。检查 mlp-login-vue\.gitignore 里有没有 node_modules
        goto :bye
    )
    echo [OK] node_modules 已被忽略，不会被推上去。
)
echo.

rem ---------- 1) 输入 Token ----------
echo Token 去这里生成: GitHub 右上角头像 - Settings - Developer settings
echo                   - Personal access tokens - Tokens (classic)
echo                   - Generate new token, 勾选 repo 权限
echo.
set "TOKEN="
set /p TOKEN=请粘贴你的 GitHub Token 后回车: 
if "%TOKEN%"=="" (
    echo [X] 没有输入 Token，不推了。
    goto :bye
)
set "AUTH_URL=https://hhyllym:%TOKEN%@github.com/hhyllym/java.git"
echo.

rem ---------- 2) 提交本地改动 ----------
set /p MSG=提交说明(直接回车用默认「更新小马王国项目」): 
if "%MSG%"=="" set "MSG=更新小马王国项目"
git add -A
echo [1/4] 本次要提交的改动:
echo        ^(中文文件名显示成 \345\220... 这种八进制转义是正常的，不影响提交^)
git status --short
echo.
git commit -m "%MSG%" >nul 2>&1
if errorlevel 1 (
    echo       没有新改动需要提交，直接进入推送步骤。
) else (
    echo       本地提交完成。
)
echo.

rem ---------- 3) 先把远程的新提交合过来 ----------
rem 不做这一步，只要远程有你这台机器不知道的提交，push 一定被拒（非快进）
echo [2/4] 同步远程 %BRANCH% ...
rem 注意：下面这些 git 命令故意不接管道，接了管道 errorlevel 就变成 findstr 的了
git fetch "%AUTH_URL%" %BRANCH%
if errorlevel 1 (
    echo       换一种方式重试（跳过证书吊销检查）...
    git -c http.schannelCheckRevoke=false fetch "%AUTH_URL%" %BRANCH%
)
if errorlevel 1 (
    echo [X] 连不上 GitHub。检查网络 / 代理，或者 Token 是否有效。
    goto :bye
)

git merge --no-edit FETCH_HEAD
if errorlevel 1 (
    echo.
    echo [X] 合并远程改动时冲突了，脚本不敢自己乱决定。
    echo     请在这个目录打开 Git Bash，敲 git status 看带 both modified 的文件，
    echo     手工改完再 git add . 然后重新运行本脚本。
    goto :bye
)
echo       远程改动已合并（远程删掉的文件这里也会跟着删，属正常）。
echo.

rem ---------- 4) 推送 ----------
echo [3/4] 正在推送到 GitHub ...
git push "%AUTH_URL%" %BRANCH%
if errorlevel 1 (
    echo.
    echo       重新试一次（跳过证书吊销检查）...
    git -c http.schannelCheckRevoke=false push "%AUTH_URL%" %BRANCH%
)
if errorlevel 1 (
    echo.
    echo [X] 推送失败。按下面顺序排查:
    echo     1. Token 过期 / 没勾 repo 权限  -^> 重新生成一个
    echo     2. 网络不通 / 需要代理
    echo     3. 还是提示 non-fast-forward   -^> 说明远程又多了新提交，重新运行本脚本一次
) else (
    echo.
    echo [4/4] [OK] 推送成功！去 https://github.com/hhyllym/java 刷新看看。
)

:bye
echo.
echo 按任意键关闭本窗口 ...
pause >nul
endlocal
exit /b
