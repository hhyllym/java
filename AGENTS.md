# AGENTS.md — java-main 工作区总览（给 AI 看的）

> 这台电脑每次开机都会重置。AI 先读这个，再读 `mlp-login-vue\AGENTS.md`（详细版）。
> 最后更新：2026-10-14

## 目录里有什么

| 路径 | 说明 |
|---|---|
| `mlp-login-vue\` | **真正在用的项目**：Vue3 + Vite 前端，端口 5173。启动脚本和详细笔记都在这里面 |
| `mlp-login-vue\AGENTS.md` | ⭐ **详细速读笔记，先看这个**（含首页图鉴 / 分级分类搜索） |
| `mlp-login-vue\src\home.js` | 首页图鉴接口封装（/api/home/* 、/api/category、/api/pony） |
| `backend-src\` | **后端源码备份**：pom.xml + src\ + sql\。D 盘会被重置，改完后端必须同步回来 |
| `恢复后端.bat` | D 盘后端被还原成空壳时：把 backend-src 拷回去 + 依次执行两个建库 SQL |
| `推送GitHub.bat` | 提交 + 先 fetch/merge 远程 + 推送（要粘贴 Token） |
| `启动全部.bat` 等 5 个 | **只是转发**到 `mlp-login-vue\` 里的同名真身脚本 |
| `AI项目生成步骤.txt` | 实训步骤清单（AGENTS.md 里说的「第 5 步」就是它） |
| `小马宝莉登录.html` | 最早的单文件原型，已不用，留作纪念 |
| `java-main\` | 空目录（旧备份残留） |
| `mlp-login-vue.rar` / `java-main.rar` | 旧备份压缩包，**用户已在网页上从 GitHub 删掉、本地也清掉了**（2026-10-14）。别再恢复回来，否则推送会和远程起冲突。原件在 `桌面\java-main.zip` |

## 后端在哪

`D:\workSpace\springBootTest`（Spring Boot 8081 + MyBatis-Plus + MySQL `mlp_login` 库）。

- 账号：`/api/auth/captcha`、`/api/auth/register`、`/api/auth/login`（`src/auth.js`）
- **首页图鉴**：`/api/home/overview`、`/api/home/search?keyword=&categoryId=&page=&size=`、
  `/api/category/tree`、`/api/pony/{id}`（`src/home.js`）——分级分类 + 关键词搜索
- 登录只用「小马名字 + 密码 + 验证码」，**不用邮箱也不用手机号**（用户明确要求，别加回去）
- 详见 `mlp-login-vue\AGENTS.md` 第 5 节（5.1 账号 / 5.6 首页图鉴）

## 一句话启动

双击 `mlp-login-vue\启动全部.bat`（后端 8081 + 前端 5173）。
停止：`mlp-login-vue\停止全部.bat`。环境有问题：`mlp-login-vue\环境检测.bat`。

## 本机没有浏览器

Chrome / Edge 都没装。要看页面效果就用无头 Chrome 截图：
`C:\Users\Administrator\.agent-browser\browsers\chrome-155.0.8059.79\chrome.exe --headless=new
--no-proxy-server --user-data-dir=<临时目录> --screenshot=x.png http://localhost:5173/...`
（首页要登录，先想办法写 localStorage，或者直接用 CDP；`agent-browser` 的 daemon 在沙箱里起不来，别用它。）
