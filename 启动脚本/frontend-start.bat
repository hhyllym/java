@echo off
chcp 65001 >nul
title mlp-frontend 5173
cd /d C:\Users\18017\Desktop\1java-main\1java-main\mlp-login-vue
set "NODE=C:\Users\18017\.workbuddy\binaries\node\versions\22.22.2-3\node.exe"
if not exist "%NODE%" set "NODE=node"
echo 启动前端，端口 5173...
"%NODE%" node_modules\vite\bin\vite.js
