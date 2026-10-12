# AGENTS.md — 给 AI 的速读笔记

> **目的**：这台电脑每次开机都会重置，AI 不要重新摸索环境。先读这个文件，3 秒进入状态，直接干活。
> **最后更新**：2026-10-12 ｜ **对应实训步骤**：《AI项目生成步骤.txt》第 5 步

---

## 0. 一句话搞清项目

| 角色 | 路径 | 技术 | 端口 |
|---|---|---|---|
| 前端 | `C:\Users\Administrator\Desktop\java-main\mlp-login-vue` | Vue3 + Vite + vue-router | **5173** |
| 后端 | `D:\workSpace\springBootTest` | Spring Boot 3.4.5（当前还是空壳） | **8081** |

- 前端登录/注册调用的接口：`POST /api/auth/login`、`POST /api/auth/register`（见 `src/auth.js` 的 `API_BASE = '/api/auth'`）
- 开发时用 vite 代理转发：`/api` → `http://localhost:8081`（见 `vite.config.js`）
- 主题：小马宝莉 / 马卡龙可爱风，中文界面，不要改成英文或商务风。

---

## 1. 直接跑脚本，不要自己拼命令

所有脚本在 `mlp-login-vue\` 下，**GBK 编码**，内部只用 `%~dp0` 相对路径，换机器换目录都能跑。

| 需求 | 运行 |
|---|---|
| 一键起前后端 | `启动全部.bat` |
| 只起前端 | `启动前端.bat` |
| 只起后端 | `启动后端.bat` |
| 停掉前后端（按端口杀） | `停止全部.bat` |
| 环境哪里坏了 | `环境检测.bat` |

`C:\Users\Administrator\Desktop\java-main\` 下也有一份 `启动全部.bat` / `停止全部.bat`，只是转发到上面的脚本。

> 脚本被封在 bash 里调不起来（这台机器安全策略禁止从 bash 调 cmd.exe），
> 需要用户双击运行，或者让用户把输出贴回来。

---

## 2. 环境实测值（2026-10-12 在本机验证过）

| 项 | 实测结果 |
|---|---|
| Node | `v22.22.2`（托管版 `...\.workbuddy\binaries\node\` + 系统版 `D:\Program Files\nodejs`） |
| npm | `10.9.7` |
| JDK | Zulu **21.0.10**，`JAVA_HOME=C:\Program Files\Zulu\zulu-21\` |
| Maven | ⚠️ **PATH 里没有 `mvn`**。用 IDEA 自带的：`D:\IntelliJ IDEA 2024.3.2.2\plugins\maven\lib\maven3\bin\mvn.cmd`（Maven **3.9.9**，可用） |
| Maven 本地库 | `C:\Users\Administrator\.m2\repository`（约 **763 MB**，依赖已齐，`mvn -o compile` **1.9 秒**通过） |
| MySQL | 3306 正在监听，账号 `root` / 密码 `123456` |
| 前端地址 | `http://localhost:5173`（`vite.config.js` 里 `open: true`，会自动开浏览器） |
| 后端地址 | `http://localhost:8081`（`application-dev.properties` 里 `server.port=8081`） |

**结论：环境是好的，能跑。只是 mvn 不在 PATH 里。**

---

## 3. 每次开机都会被删掉的两个目录

| 被删的目录 | 后果 | 脚本怎么处理 |
|---|---|---|
| `mlp-login-vue\node_modules` | 前端起不来 | 检查 `node_modules\vite\bin\vite.js` 在不在，不在就 `npm install`（1~3 分钟） |
| `D:\workSpace\springBootTest\target` | 后端要重新编译 | 每次 `mvn spring-boot:run` 自动重编（10~40 秒） |

**硬性规则：脚本运行时不得依赖这两个目录里的任何东西**，只允许用 `if exist` 判断它们存不存在。

---

## 4. 千万别做

- ❌ 别删 `C:\Users\Administrator\.m2\repository`——删了要重新拉一堆依赖。
- ❌ 别删项目里的 `.workbuddy` 目录。
- ❌ 别只改一边端口：`vite.config.js` 的 proxy target 和后端 `server.port` 必须都是 `8081`。
- ❌ 别从 bash 里调 `cmd.exe`（安全策略会拦，白费力气）。
- ❌ 别把 GBK 的 `.bat` 用 UTF-8 重新保存，中文会变乱码（脚本已 `chcp 936`）。

---

## 5. 当前状态（重要 · 2026-10-12 更新）

### ✅ 5.1 后端已经补齐（不再是空壳）
`D:\workSpace\springBootTest` 已完成改造，前后端已联通，全部接口实测通过：
- `AuthController.java`：`/api/auth/captcha`（PNG 验证码）、`/api/auth/register`、`/api/auth/login`
- pom 已加 `mybatis-plus-spring-boot3-starter 3.5.9` + `mysql-connector-j`；数据源配在 `application-dev.properties`（`mlp_login` 库，root/123456）
- `Result / User / UserMapper / UserService / UserServiceImpl / CaptchaService / CorsConfig` 都在 `com.fenglin.springboottest` 包下
- 建表 SQL 在 `sql\mlp_login.sql`，**可重复执行**（老库会自动 DROP 掉 email 列）
- 日志写到 `log\xx.log`，把 `application.properties` 里 `com.fenglin.springboottest` 改成 debug 可看验证码明文
- **密码是加盐哈希存储**：`t_user.password = SHA-256(salt + 明文)`，`t_user.salt` 每用户随机 16 位 hex。
  工具类 `common/PasswordUtil.java`，和 MySQL 的 `SHA2(CONCAT(salt,pwd),256)` 结果一致。
  登录有明文兼容升级：老明文行登录成功后自动转哈希。SQL 里也有对应的幂等迁移。
  接口对前端完全透明，前端零改动。

### 🔒 5.2 登录方式（用户明确要求）
**只用「小马名字 + 密码 + 验证码」登录。不用邮箱，也不用手机号。**
2026-10-12 已把 email 从前端、后端、数据库表（t_user 无 email 列）全部移除。
**以后不要把邮箱/手机号加回任何页面或接口。**

### 🟡 5.3 其它小项
- `UserController.java` 还是 thymeleaf 老 demo，留着没删，与登录功能无关
- 没有 `.env`，接口地址写死 `/api`（用代理，够用）
- 没有 `README.md`
- `.m2\settings.xml` 镜像已清理成单条阿里云 `*` 镜像

---

## 6. 文件地图（前端）

```
mlp-login-vue/
├── 启动全部.bat / 启动前端.bat / 启动后端.bat / 停止全部.bat / 环境检测.bat   ← GBK
├── index.html                    入口，挂 #app，title=小马王国 · 账号中心
├── vite.config.js                端口 5173，open:true，proxy /api → 8081
├── package.json                  dev / build / preview 三个脚本
├── .gitignore                    node_modules, dist
├── src/
│   ├── main.js                   挂载 + 引入样式
│   ├── App.vue                   页面外壳
│   ├── auth.js                   会话(localStorage/sessionStorage) + /api/auth 请求封装
│   ├── router/index.js           路由：/ → Dashboard(需登录)、/login、/register
│   ├── assets/style.css          马卡龙主题样式
│   ├── components/PonyBackground.vue
│   └── views/  LoginView.vue / RegisterView.vue / DashboardView.vue
└── dist/                         旧构建产物（忽略）
```

后端关键文件：
```
D:\workSpace\springBootTest\
├── pom.xml                       Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + mysql-connector-j
├── mvnw / mvnw.cmd               wrapper（only-script，会去官网下 Maven，慢；优先用 IDEA 自带 Maven）
├── sql\mlp_login.sql             建库建表 + 老库自动删 email 列，可重复执行
├── src/main/resources/application.properties         spring.profiles.active=dev
├── src/main/resources/application-dev.properties     server.port=8081 + mlp_login 数据源
├── src/main/java/com/fenglin/springboottest/
│   ├── SpringBootTestApplication.java
│   ├── common/Result.java          统一响应 { code, msg, data }
│   ├── entity/User.java            t_user 表，无 email 字段
│   ├── mapper/UserMapper.java
│   ├── service/UserService.java / UserServiceImpl.java / CaptchaService.java
│   ├── controller/AuthController.java   /api/auth/captcha|register|login
│   ├── config/CorsConfig.java
│   └── UserController.java         thymeleaf 老 demo，与登录无关
└── log/xx.log                     运行日志
```

---

## 7. 后续提示词（用户可以直接抄）

- 补后端：`帮我把 D:\workSpace\springBootTest 改造成能对接前端 mlp-login-vue 的后端，接口 /api/auth/login 和 /api/auth/register，用 Spring Boot + MyBatis-Plus + MySQL(root/123456)，SQL 文件保存到项目里，注意每次开机 target 会被删掉。`
- 继续做页面：`在 mlp-login-vue 里加 XXX 页面，保持马卡龙 / 小马宝莉风格。`
- 提速：`看看怎么让 启动前端.bat / 启动后端.bat 更快。`
