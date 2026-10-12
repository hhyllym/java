# AGENTS.md — java-main 工作区总览（给 AI 看的）

> 这台电脑每次开机都会重置。AI 先读这个，再读 `mlp-login-vue\AGENTS.md`（详细版）。
> 最后更新：2026-10-12

## 目录里有什么

| 路径 | 说明 |
|---|---|
| `mlp-login-vue\` | **真正在用的项目**：Vue3 + Vite 前端，端口 5173。启动脚本和详细笔记都在这里面 |
| `mlp-login-vue\AGENTS.md` | ⭐ **详细速读笔记，先看这个** |
| `java-main\` | 旧的一层备份（html 单页 + `AI项目生成步骤.txt` 实训步骤清单） |
| `mlp-login-vue.rar` / `java-main.rar` | 旧备份压缩包，不要解压覆盖 |
| `小马宝莉登录.html` | 最早的单文件原型，已不用 |
| `启动全部.bat` / `停止全部.bat` | 转发到 `mlp-login-vue\` 里的同名脚本 |

## 后端在哪

`D:\workSpace\springBootTest`（Spring Boot 8081）——**已完成**：/api/auth/captcha、/api/auth/register、/api/auth/login 全部实测通过，MySQL 库 `mlp_login`。
登录只用「小马名字 + 密码 + 验证码」，**不用邮箱也不用手机号**（用户明确要求，别加回去）。
详见 `mlp-login-vue\AGENTS.md` 第 5 节。

## 一句话启动

双击 `mlp-login-vue\启动全部.bat`（后端 8081 + 前端 5173）。
停止：`mlp-login-vue\停止全部.bat`。环境有问题：`mlp-login-vue\环境检测.bat`。
