# 贡献指南

感谢你愿意帮星糖谷变得更好 🌈 第一次参与？按下面走就行。

## 开始前

- 先读仓库根 [AGENTS.md](AGENTS.md) 和 `mlp-login-vue/AGENTS.md`，里面有环境踩坑记录。
- 环境：Node ≥ 18、JDK 21、Maven 3.9、MySQL 8。
- 后端源码在 `backend-src.bak/`（备份形态），本机实际跑在 `D:\workSpace\springBootTest`。

## 开发流程

1. Fork 本仓库，Clone 到本地。
2. 从 `main` 新建分支：
   - 新功能 `feat/xxx`（例 `feat/new-game`）
   - 修 bug `fix/xxx`（例 `fix/wallet-balance`）
   - 文档 `docs/xxx`
3. 改动后自检：
   ```bash
   cd mlp-login-vue
   npm install      # 首次
   npm run build    # 必须零错误通过
   ```
4. 提交信息用一句话说清「改了什么」：
   ```
   feat(game): 新增「心印拼拼乐」拼图小游戏
   fix(wallet): 统一游戏中心与账户的可用积分
   docs: 补全 README 快速开始
   ```
5. 推到你的 Fork，在 GitHub 上提 Pull Request 到 `main`。

## 提 PR 时注意

- 一个 PR 只做一件事，别把不相干的改动混进来。
- 贴一下改动前后截图（UI 改动尤其需要）。
- 说明怎么验证的（`npm run build` / 浏览器点过哪些路径）。
- 别提交 `node_modules/`、`dist/`、`.env`、数据库口令、Token。

## 代码约定

- **Vue 单文件组件**：`<template>` / `<script>` / `<style scoped>`，与现有文件的 Options API 风格保持一致。
- **主题色**统一走 `src/assets/style.css` 里的 CSS 变量（`--pink` / `--purple` / `--primary` / `--panel` / `--muted` / `--border` …），别硬编码一堆颜色；照顾 `prefers-reduced-motion`。
- **加新游戏**：`src/components/games/` 新建组件 → `src/courses.js` 的 `GAMES` 注册（数组顺序即展示顺序）→ `src/views/GameView.vue` 的 `GAME_COMPS` 映射；组件必须 `emits: ['score', 'over']`。
- **积分**一律走 `src/wallet.js`，不要再直接读 `loadLearn().points` 或自己请求 `/api/point/balance`。
- **音效**统一用 `src/sound.js`。
- **设定**：角色、心印、地名均为原创，不要引入任何既有动画作品的专有设定。
- **脚本**：`启动脚本/*.bat` 是 GBK 编码，不要用 UTF-8 重存；脚本内只用 `%~dp0` 相对路径。

## Issue

- Bug 用「🐛 Bug 反馈」模板，功能想法用「✨ 功能建议」模板。
- 提之前先搜一下有没有重复。
- 安全漏洞**不要**开公开 Issue，看 [SECURITY.md](SECURITY.md)。

## 行为准则

参与本项目即表示同意 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。友好、就事论事，别抬杠。
