# 🌈 星糖谷 · Star Sugar Valley

> 原创「小马 / 马卡龙」主题的全栈学习项目 —— Vue 3 + Spring Boot + MySQL。
> 登录注册、小马图鉴、积分系统、**27 款小游戏**、宠物养成、好友聊天、四人联机大富翁……

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Vue](https://img.shields.io/badge/Vue-3.4-42b883)
![Vite](https://img.shields.io/badge/Vite-5.2-646cff)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6db33f)

桌面端 Web 应用，中文界面，柔和粉彩配色。项目为 IT 实训作品，代码与美术设定均为本项目原创。

---

## ✨ 有什么

| 模块 | 说明 |
|---|---|
| 🔐 账号 | 小马名字 + 密码 + 图形验证码注册/登录，会话保持、登录守卫、后台管理入口 |
| 📚 小马图鉴 | 后端驱动的分级分类检索、小马详情、我的账号 |
| 🎓 友谊学院 | 6 门主题课 + 语文/数学/英语/美术/科学/体育课，题库、徽章、学习进度 |
| 🎮 游戏中心 | **27 款原创小游戏**，统一积分结算、历史最佳、排行榜、好友对战 |
| 💰 可用积分 | 一套账户余额打通赚积分 / 积分商店 / 排行榜 / 联机大富翁（见 `src/wallet.js`） |
| 🐾 宠物 | 领养、喂养、3D 预览（Three.js）、宠物单挑 |
| 👥 好友与聊天 | 好友申请、私聊、未读角标、在线状态 |
| 🎲 联机大富翁 | 四人各用设备进同一房间，掷骰买店收租，按名次结算积分 |
| 📔 其他 | 每日运势、心情日记、消息中心、积分商店、管理员后台 |

## 🧱 技术栈

| 层 | 技术 | 端口 |
|---|---|---|
| 前端 | Vue 3.4 + Vite 5 + vue-router 4 + Three.js | `5180` |
| 后端 | Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 | `8081` |
| 数据库 | MySQL 8，库 `mlp_login` | `3306` |

前端所有 `/api` 请求由 Vite 代理到 `http://localhost:8081`（见 `mlp-login-vue/vite.config.js`）。

## 📁 目录结构

```
java/                              ← 仓库根（github.com/hhyllym/java）
├── .github/                       Issue / PR 模板
├── mlp-login-vue/                 前端工程（npm 命令在这里跑）
│   ├── src/
│   │   ├── assets/style.css       全局主题 CSS 变量
│   │   ├── components/games/      27 款小游戏组件
│   │   ├── views/                 页面
│   │   ├── router/                路由 + 登录守卫
│   │   ├── courses.js             课程 / 题库 / 游戏清单 GAMES
│   │   ├── wallet.js              「可用积分」唯一入口
│   │   └── ...
│   ├── tools/static-server.cjs    零依赖静态预览（起 dist 并代理 /api）
│   └── package.json
├── backend-src.bak/               后端源码备份：pom.xml + src(com.fenglin) + sql/
├── 启动脚本/                       Windows 一键启动/停止/恢复/推送（GBK 编码 .bat）
├── AGENTS.md                      项目速读笔记
├── README.md · LICENSE · CONTRIBUTING.md
├── CHANGELOG.md · SECURITY.md · CODE_OF_CONDUCT.md
└── .gitignore
```

> ⚠️ **后端源码以备份形式存放**：`backend-src.bak/` 是完整的 Maven 工程，本机实际运行目录是 `D:\workSpace\springBootTest`（该盘每次开机会被重置，靠 `启动脚本\恢复后端.bat` 把备份装回去）。换机器时把 `backend-src.bak/` 拷成自己的后端工程即可。

## 🚀 快速开始

### 环境要求

| 项 | 版本（实测） |
|---|---|
| Node.js | ≥ 18（实测 22.22.2） |
| JDK | 21（Zulu 21.0.10） |
| Maven | 3.9.9（⚠️ 常不在 PATH，用 IDEA 自带的 `mvn.cmd`） |
| MySQL | 8，`root` / `123456`，3306 |

### 方式 A：Windows 一键启动（推荐）

双击 `启动脚本\启动全部.bat` → 后端 8081 + 前端 5180 一起起来，浏览器自动打开 `http://localhost:5180`。
停止用 `启动脚本\停止全部.bat`，环境排查用 `启动脚本\环境检测.bat`。
脚本是 **GBK 编码**，别用 UTF-8 重存，中文会乱码。

### 方式 B：手动命令

```bash
# 1) 建库（5 个 SQL）
cd backend-src.bak
mysql -uroot -p123456 < sql/mlp_login.sql
mysql -uroot -p123456 < sql/mlp_home.sql
mysql -uroot -p123456 < sql/mlp_extend.sql
mysql -uroot -p123456 < sql/mlp_friend.sql
mysql -uroot -p123456 < sql/mlp_message.sql

# 2) 起后端（8081）
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# 3) 另开终端，起前端（5180）
cd ../mlp-login-vue
npm install
npm run dev
```

构建产物：`npm run build` → `mlp-login-vue/dist/`；本地预览 `npm run preview`。
后端没起来时可用 `node tools/static-server.cjs` 做兜底静态预览（页面能开，登录类接口会失败）。

> 启动后端前请 `unset SERVER__PORT` —— Spring Boot 的宽松绑定会把它当成 `server.port`，导致抢随机端口启动失败。

## 🎮 27 款小游戏

游戏中心按 `src/courses.js` 里 `GAMES` 数组顺序展示；四人联机招牌 + 本轮新做的低概率抽奖 / 重做游戏排在最前。

| # | 游戏 | id | 备注 |
|---|---|---|---|
| 1 | 🎲 糖霜镇大富翁 | `monopoly` | 四人联机招牌 · 各用自己的设备进同一房间、积分当本金、掷骰买店收租 · 掷骰时浮 3D 骰子 |
| 2 | 🎡 星辉幸运转盘 | `wheel` | ✨ 新增：20 格彩色转盘（新增 🍭 糖果幸运奖）· 头奖 0.6% · 概率公开 · 返还约 87.5% |
| 3 | 🃏 魔法翻牌·找星辉 | `card` | ✨ 新增：16 张牌里 1 颗 ⭐ · 头奖 6.25% · 返还约 93.75% · 新增 💎 宝石好礼 |
| 4 | 🎰 彩虹扭蛋机 | `gacha` | ✨ 新增：稀有度抽蛋 + 收集图鉴 · 至臻 1% · 返还约 89.8% |
| 5 | 🦄 小马跳跳跑 | `jump` | ✨ 新增：Three.js 3D 建模独角小马、横版跑酷、8 秒热身缓冲 |
| 6 | 💣 星糖扫雷 | `mine` | ✨ 新增：首点安全、空白自动展开、插旗模式 |
| 7 | 🧩 心印拼拼乐 | `puzzle` | ✨ 新增：3×3 交换拼图 |
| 8 | ☁️ 流光穿越 | `glide` | ✨ 新增：5 秒热身缓冲的飞行闯关 |
| 9 | 🏁 云驰冲刺赛 | `race` | ✨ 重做：报名 / 体力 / 连击 / 名次奖金 |
| 10 | 🐍 彩虹贪吃蛇 | `snake` | ✨ Q 版 3D 建模（`src/pet3d/snake.js`）· 三档难度、挑战 ×1.5、可切经典画风 |
| 11 | 🍬 糖豆消消乐 | `sports` | ✨ 点一下就自动消（智能点消）· 可切手动交换、提示高亮、死局重排 |
| 12 | 🎰 小马老虎机 | `slot` | 积分下注 |
| 13 | ✨ 魔法问答 | `twilight` | |
| 14 | 🧺 苹果农庄 | `applejack` | |
| 15 | ⏱️ 闪电反应 | `dash` | |
| 16 | 🎈 派对翻牌 | `pinkie` | |
| 17 | 🥕 森林喂食 | `fluttershy` | |
| 18 | 💎 宝石工坊 | `rarity` | |
| 19 | 🧁 甜品工坊 | `dessert` | |
| 20 | 🎊 派对布置 | `party` | |
| 21 | 👗 时尚装扮 | `style` | |
| 22 | ⚔️ 宠物单挑 | `petbattle` | |
| 23 | ✊ 猜拳争霸 | `rps` | |
| 24 | 🪄 魔法记忆 | `memory` | |
| 25 | 🌈 彩虹速配 | `rainbow` | |
| 26 | 🐰 森林躲猫猫 | `whack` | |
| 27 | 💎 宝石鉴宝 | `gem` | |

小游戏统一通过 `emits: ['score', 'over']` 向 `GameView.vue` 上报；**下注类**（老虎机、转盘、翻牌、扭蛋）只用 `score` 展示、**不触发 `over` 结算**——它们的积分进出由 `src/wallet.js` 的 `spend()` / `earn()` 直接完成，避免与「破纪录奖励」重复发分。这几款只用游戏内虚拟积分、概率表公开、整体返还低于 100%，纯娱乐。

## 🔌 接口约定

- 前端基址：`/api`，由 Vite 代理转发到后端 8081
- 账号：`/api/auth/captcha`、`/api/auth/register`、`/api/auth/login`
- 图鉴：`/api/home/overview`、`/api/home/search`、`/api/category/tree`、`/api/pony/{id}`
- 积分：`/api/point/balance`、`/api/point/logs`、`/api/point/rank`

## 🤝 参与贡献

欢迎提 Issue 和 PR！第一次贡献请先读 [CONTRIBUTING.md](CONTRIBUTING.md)，里面有环境、分支、提交规范和 PR 流程。行为准则见 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)，漏洞报告见 [SECURITY.md](SECURITY.md)。

Fork → Clone → 新建分支 → 改代码 → `npm run build` 自检 → 提 PR，就这么简单。

## 📜 许可证

[MIT](LICENSE) © 2026 hhyllym —— 可以随便用、改、商用，记得保留版权声明。

## ⚠️ 声明

- 本项目是 **IT 实训学习作品**，不是生产级系统，别直接拿去上线。
- 「星糖谷」的角色、心印、地名与世界观均为**本项目原创**，与任何既有动画作品无关，也不是其官方产品。
- 仓库内 `.bat` 脚本含本机绝对路径与示例数据库口令，仅用于本机实训；部署到别处请自行改配置，别把真实密钥提交进来。

## 🙏 致谢

Vue · Vite · Spring Boot · MyBatis-Plus · Three.js，以及所有在实训里被反复折腾的那台电脑。
