# mlp-login-vue · 星糖谷前端

本目录是 [星糖谷](../README.md) 的 Vue 3 前端工程；完整项目介绍、目录结构、后端说明请看**仓库根 README**。

## 跑起来

```bash
npm install
npm run dev      # http://localhost:5180
```

Vite 已配好 `/api` → `http://localhost:8081` 的代理，所以**后端要先起来**，登录 / 注册 / 积分等功能才可用。
后端源码在仓库的 `backend-src.bak/`，Windows 下也可直接双击 `启动脚本/启动全部.bat` 一键起前后端。

## 常用命令

| 命令 | 作用 |
|---|---|
| `npm run dev` | 开发服务，固定 5180 端口 |
| `npm run build` | 构建到 `dist/` |
| `npm run preview` | 预览构建产物 |
| `node tools/static-server.cjs` | 零依赖兜底静态预览（起 dist 并代理 /api） |

## 加一款新游戏

1. 在 `src/components/games/` 新建组件，`emits: ['score', 'over']` 上报分数与结算；
2. 在 `src/courses.js` 的 `GAMES` 里注册（数组顺序 = 游戏中心展示顺序）；
3. 在 `src/views/GameView.vue` 的 `GAME_COMPS` 里加映射；
4. 积分统一走 `src/wallet.js`，音效统一走 `src/sound.js`，主题色用 `src/assets/style.css` 的 CSS 变量。

详见 [CONTRIBUTING.md](../CONTRIBUTING.md)。许可证 [MIT](../LICENSE)。
