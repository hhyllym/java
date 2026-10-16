# AGENTS.md — 给 AI 的速读笔记

> **目的**：这台电脑每次开机都会重置，AI 不要重新摸索环境。先读这个文件，3 秒进入状态，直接干活。
> **最后更新**：2026-10-14 ｜ **对应实训步骤**：《AI项目生成步骤.txt》第 5 步
> **2026-10-14 大修**：一键启动脚本（原来跑错目录，前端起不来）、前端 6 个被回退的文件、整个后端 `/api/auth` 模块，
> 都已修好并端到端实测；git 仓库根（`.git` 位置）和 `推送GitHub.bat` 也一并修正。细节见第 3、5.3、5.4 节。
> **2026-10-14 第二轮**：首页从「前端写死数据」改成**后端驱动**——新建 5 张表 + `/api/home/*` 分级分类搜索，
> 见第 5.6 节。

---

## 0. 一句话搞清项目

| 角色 | 路径 | 技术 | 端口 |
|---|---|---|---|
| 前端 | `C:\Users\Administrator\Desktop\java-main\mlp-login-vue` | Vue3 + Vite + vue-router | **5173** |
| 后端 | `D:\workSpace\springBootTest` | Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + MySQL | **8081** |

- 前端登录/注册调用的接口：`POST /api/auth/login`、`POST /api/auth/register`（见 `src/auth.js` 的 `API_BASE = '/api/auth'`）
- **首页图鉴**调用的接口：`/api/home/overview`、`/api/home/search`、`/api/category/tree`、`/api/pony/{id}`（见 `src/home.js`）
- 开发时用 vite 代理转发：`/api` → `http://localhost:8081`（见 `vite.config.js`）
- 主题：小马宝莉 / 马卡龙可爱风，中文界面，不要改成英文或商务风。

---

## 1. 直接跑脚本，不要自己拼命令

所有脚本在 `mlp-login-vue\` 下，**GBK 编码**，内部只用 `%~dp0` 相对路径，换机器换目录都能跑。

| 需求 | 运行 |
|---|---|
| 一键起前后端 | `mlp-login-vue\启动全部.bat` |
| 只起前端 | `mlp-login-vue\启动前端.bat` |
| 只起后端 | `mlp-login-vue\启动后端.bat` |
| 停掉前后端（按端口杀） | `mlp-login-vue\停止全部.bat` |
| 环境哪里坏了 | `mlp-login-vue\环境检测.bat` |
| 前端依赖装不上时的兜底预览 | `tools\static-server.cjs`（零依赖，起 dist 并代理 /api） |
| 后端被还原成空壳时，把代码装回去 | `java-main\恢复后端.bat` |
| 推代码到 GitHub | `java-main\推送GitHub.bat`（提示粘贴 Token，会先同步远程再推） |

`C:\Users\Administrator\Desktop\java-main\` 下的 5 个同名 bat 只是**转发**到 `mlp-login-vue\` 里的真身，
点哪个都一样。`打开网页.bat` 是给 `启动前端.bat` 内部调用的（等端口起来再开浏览器），不用手动点。

⚠️ **脚本必须放在 `mlp-login-vue\` 里跑**：前端项目（package.json / node_modules）在这个子目录，
根目录只有一个空壳 `package-lock.json`。2026-10-14 之前的脚本就是因为在根目录 `npm run dev` 才起不来，
别再把 `cd /d "%~dp0"` 之后直接 `npm install` 的逻辑写回根目录。


> 脚本被封在 bash 里调不起来（这台机器安全策略禁止从 bash 调 cmd.exe），
> 需要用户双击运行，或者让用户把输出贴回来。

## Git 仓库（2026-10-12 起，2026-10-14 修正过）

- 仓库根 = `Desktop\java-main`（**.git 现在就在这一层**；2026-10-14 之前被错放在 `mlp-login-vue\` 下）
- 远程 `https://github.com/hhyllym/java`，分支 main
- 本地 HEAD 已合并远程旧上传历史（`6643c7d`）；远程比本地多 3 个提交，**推送前必须先 fetch + merge**，
  `推送GitHub.bat` 里已经写好了这一步
- 身份配置写在**仓库本地**（.git/config：hhyllym），全局 .gitconfig 被开机重置也不影响提交
- 沙箱里访问 GitHub 要加 `GIT_SSL_NO_VERIFY=1`（沙箱代理证书问题）；用户双击 bat 不受影响

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
| 前端地址 | `http://localhost:5173`（`vite.config.js` 是 `open: false` + `strictPort: true`，浏览器由 `启动前端.bat` → `打开网页.bat` 在端口就绪后打开） |
| 后端地址 | `http://localhost:8081`（`application-dev.properties` 里 `server.port=8081`） |

**结论：环境是好的，能跑。只是 mvn 不在 PATH 里。**

---

## 3. 每次开机都会被去掉的东西（3 处，不是 2 处）

| 被清掉的东西 | 后果 | 怎么恢复 |
|---|---|---|
| `mlp-login-vue\node_modules` | 前端起不来 | `启动前端.bat` 检测 `node_modules\vite\bin\vite.js`，不在就 `npm install`（1~3 分钟） |
| `D:\workSpace\springBootTest\target` | 后端要重新编译 | 每次 `mvn spring-boot:run` 自动重编（10~40 秒） |
| **`D:\workSpace\springBootTest` 的源码会退回空壳**（只剩启动类 + 老的 `UserController`，pom 也没有 MyBatis-Plus） | 后端起来了但 `/api/auth/*`、`/api/home/*` 全 404，页面能开、登录注册必失败 | 双击 `java-main\恢复后端.bat`，它会把 `java-main\backend-src\` 的备份拷回去，并依次执行 `sql\mlp_login.sql` + `sql\mlp_home.sql` |

> 2026-10-14 实测：`D:\` 上这个后端的配置文件日期全是 2025-09-02，说明整个目录被还原过一次，
> `AuthController / User / CaptchaService / PasswordUtil / CorsConfig / sql` 全没了。
> **桌面 `java-main\backend-src\` 现在存着完整备份（pom.xml + src + sql），改完后端记得同步回去。**

**硬性规则：脚本运行时不得依赖上面这些目录里的任何东西**，只允许用 `if exist` 判断它们存不存在。

---

## 4. 千万别做

- ❌ 别删 `C:\Users\Administrator\.m2\repository`——删了要重新拉一堆依赖。
- ❌ 别删项目里的 `.workbuddy` 目录。
- ❌ 别只改一边端口：`vite.config.js` 的 proxy target 和后端 `server.port` 必须都是 `8081`。
- ❌ **别让 `SERVER_PORT` / `SERVER__PORT` 这类环境变量混进后端的启动环境**。
  2026-10-14 踩过：某工具在会话里设了 `SERVER__PORT=58203`，Spring Boot 的宽松绑定把它当成 `server.port`，
  Tomcat 就去抢随机端口然后报 “Port 58203 was already in use”，看起来像端口冲突，其实是环境变量。
  `启动后端.bat` 已经先 `set "SERVER_PORT="` 清掉，并且用 `-Dspring-boot.run.arguments=--server.port=8081` 兜底。
- ❌ 别从 bash 里调 `cmd.exe`（安全策略会拦，白费力气）。**PowerShell 里可以正常跑 mvn / 杀进程。**
- ❌ 别把 GBK 的 `.bat` 用 UTF-8 重新保存，中文会变乱码（脚本已 `chcp 936`）。
  生成脚本用的工具在 `C:\Users\Administrator\WorkBuddy\<会话>\.workbuddy\tmp\gen-bats.py`，改脚本优先用它重新生成。
- ❌ 别用 `netstat -ano -p TCP` 判断端口。**Vite 只监听 IPv6 的 `[::1]:5173`**，
  而 `-p TCP` 只列 IPv4 表 → 5173 永远查不到，症状是「前端明明起来了，脚本却说没起来、浏览器不弹、`停止全部.bat` 杀不掉」。
  2026-10-14 实测：`netstat -ano -p TCP | findstr ":5173 "` 空，`netstat -ano | findstr ":5173 "` 才拿得到 PID。
  现在所有脚本统一用 `netstat -ano`（不带 `-p`），`tokens=5` 在 IPv4/IPv6 两种行里都是 PID。
- ✅ **git 仓库根已修正（2026-10-14）**：`.git` 原来被错放在 `mlp-login-vue\` 下，而索引里存的是「上一级目录」的路径，
  导致 `git status` 满屏 `D`、`推送GitHub.bat` 报「当前目录不是 git 仓库」（它的 `if not exist .git` 检查一直不过）。
  现在 `.git` 已移到 `C:\Users\Administrator\Desktop\java-main\.git`，
  `git rev-parse --show-toplevel` = `C:\Users\Administrator\Desktop\java-main`，和索引一致了。**别再把它挪回去。**
- ℹ️ **推送前必须先合并远程**：远程 main（`2c0f34c`）比本地 HEAD（`6643c7d`）多 3 个提交
  （`c53bf3e Add files via upload` → `2ba9294 Delete mlp-login-vue.rar` → `2c0f34c Delete mlp-login-vue.zip`），
  直接 push 一定被拒（非快进）。`推送GitHub.bat` 里已经加了 `git fetch` + `git merge FETCH_HEAD` 再 push，
  并用 `git merge-tree` 预演过：**无冲突**。
- ℹ️ GitHub 上 `hhyllym/java` 目前只有 `README.md` + `java-main.rar` 两个文件（用户自己在网页上删过东西）。
  本地那份 `java-main.rar` 已按 HEAD 的 blob 还原（hash `f66115b4…`）就是为了别把远程这两个文件误删。
  推送之后才会把 mlp-login-vue 全套 + `backend-src` 传上去。

---

## 5. 当前状态（重要 · 2026-10-14 更新）

### ✅ 5.1 后端已经补齐（2026-10-14 重建，实测全通）
`D:\workSpace\springBootTest` 被还原成空壳后已按原设计重建，并跑通端到端联调：
- `controller\AuthController.java`：`GET /api/auth/captcha`（PNG 验证码）、`POST /api/auth/register`、`POST /api/auth/login`
- pom 已加 `mybatis-plus-spring-boot3-starter 3.5.9` + `mysql-connector-j`；
  数据源在 `application-dev.properties`（`mlp_login` 库，root/123456，`server.port=8081`）
- 包 `com.fenglin.springboottest` 下：`common\Result`、`common\PasswordUtil`、`common\GlobalExceptionHandler`、
  `config\CorsConfig`、`entity\User`、`mapper\UserMapper`、`service\UserService(Impl)`、`service\CaptchaService`
- 建表 SQL 在 `sql\mlp_login.sql`，**可重复执行**（老库自动 DROP 掉 email 列）
- 日志写到 `log\xx.log`；把 `com.fenglin.springboottest` 的日志级别调成 debug 就能在后端窗口看到验证码明文
- **密码是加盐哈希存储**：`t_user.password = SHA-256(salt + 明文)`，`t_user.salt` 每用户随机 16 位 hex，
  和 MySQL 的 `SHA2(CONCAT(salt,pwd),256)` 结果逐字节一致（已实测 `= 1`）。
  ⚠️ 现在的实现**没有**「明文老数据自动升级成哈希」的逻辑，别照着老笔记以为有；库里一律存哈希。
- 验证码走 **session**（`CaptchaService.SESSION_KEY`），一次性：校验完立刻作废，
  所以前端登录失败后必须换一张新的（`LoginView` 已经这么做了）。
  JSESSIONID 能穿过 Vite 代理，实测经 `http://localhost:5173/api/auth/*` 登录注册都正常。
- 前端提示文案要和后端 msg 对得上：`LoginView` 按 msg 里有没有「验证码 / 还没注册 / 名字」来把错误挂到对应输入框，
  `RegisterView` 按有没有「密码」来分。改后端文案时注意别把这两个匹配规则弄失效。

### 🔒 5.2 登录方式（用户明确要求）
**只用「小马名字 + 密码 + 验证码」登录。不用邮箱，也不用手机号。**
2026-10-12 已把 email 从前端、后端、数据库表（t_user 无 email 列）全部移除。
**以后不要把邮箱/手机号加回任何页面或接口。**

### ♻️ 5.3 2026-10-14 这轮修了什么（别再踩回去）
1. **脚本跑错目录**：根目录的 bat 在根目录 `npm run dev`，而 package.json 在 `mlp-login-vue\` → 前端根本起不来，
   所以「一键启动没反应、没有网页」。现在真身脚本全在 `mlp-login-vue\`，根目录只剩转发。
2. **前端被回退**：`auth.js / router/index.js / LoginView / RegisterView / DashboardView / style.css`
   被还原成 2026-10-10 的旧版（`RegisterView` 里调了不存在的 `getUsers()`，注册必崩；还带邮箱字段）。
   已用 `git show HEAD:mlp-login-vue/<文件>` 从 10-12 的提交恢复——**仓库 HEAD 里有最新完整版，以后丢了先来这儿捞**。
   本地比 HEAD 新的只有 `HomeView.vue`（CutieMark 版）、`CutieMark.vue`、`AGENTS.md`，别用 HEAD 覆盖这三个。
3. **后端源码丢失**：见第 3 节，用 `恢复后端.bat`。
4. **git 仓库根错位**：`.git` 在 `mlp-login-vue\` 下，索引路径却是上一级的 → `推送GitHub.bat` 永远报
   「当前目录不是 git 仓库」。已把 `.git` 移到 `Desktop\java-main\.git`（见 5.5）。

### ✅ 5.4 git / 推 GitHub 现状（2026-10-14 修正）
- `.git` 位置：`C:\Users\Administrator\Desktop\java-main\.git`，仓库根 = `Desktop\java-main`
- 本地分支 `main` HEAD = `6643c7d`（合并了本地完整源码 + 远程旧上传记录）
- 远程 `origin/main` = `2c0f34c`（比本地多 3 个提交，主要内容是删掉 `mlp-login-vue.rar` / `.zip`）
- 推送流程（`推送GitHub.bat` 已实现）：`git add -A` → `commit` → `git fetch AUTH_URL main` →
  `git merge --no-edit FETCH_HEAD` → `git push AUTH_URL main`（失败自动加 `-c http.schannelCheckRevoke=false` 重试）
- `git merge-tree` 预演结论：**无冲突**
- 身份配置在仓库本地 `.git/config`（`user.name=hhyllym` / `hhyllym@users.noreply.github.com`），全局被重置也不影响提交
- 沙箱里访问 GitHub 要 `GIT_SSL_NO_VERIFY=1`；用户双击 bat 不受影响
- 仓库里那批 `*.rar / *.zip / 小马宝莉登录.html / 新建文本文档.txt / java-main\* / 根目录 .gitignore、AGENTS.md`
  在本地工作区已经不存在，提交后会从仓库里去掉；**GitHub 上本来也没有它们**（远程只有 `README.md` + `java-main.rar`），
  所以推送不会删掉远程任何东西。

### 🟡 5.5 其它小项
- `UserController.java` 还是 thymeleaf 老 demo，留着没删，与登录功能无关
- 没有 `.env`，接口地址写死 `/api`（用代理，够用）
- 前端没有 `README.md`
- `.m2\settings.xml` 镜像已清理成单条阿里云 `*` 镜像（本机拉 MyBatis-Plus 实测可用）

### ✅ 5.6 首页图鉴 · 后端驱动 + 分级分类搜索（2026-10-14 新增）

**改动前**：`HomeView.vue` 里写死 6 只小马 + 前端 `filter` 假装搜索，后端一点没参与。
**改动后**：首页所有数据（统计 / 分类树 / 热搜词 / 热门榜 / 列表 / 详情）都从后端来。

#### 新增的 5 张表（`sql\mlp_home.sql`，可重复执行）

| 表 | 作用 | 要点 |
|---|---|---|
| `t_category` | **分级分类**，自关联 | `parent_id=0` 是一级；现在两层：一级=`种族/元素/栖息地`，二级=`飞马/忠诚/小马谷`… 结构本身支持任意层级 |
| `t_pony` | 图鉴条目 | `mark` 是 `CutieMark.vue` 支持的图案 key，没有对应图案的用 `emoji` 兜底 |
| `t_pony_category` | 条目 ↔ 分类 **多对多** | 一只小马同时挂到「飞马 + 忠诚 + 云中城」三个节点上 |
| `t_pony_tag` | 关键词标签 | 搜「派对」能搜出碧琪，命中的就是这张表 |
| `t_search_word` | 热搜词 | 每次带关键词搜索 `hit_count + 1`，首页热搜是**活的**（`INSERT ... ON DUPLICATE KEY UPDATE`） |

数据规模：17 个分类 / 16 只小马 / 40 条分类关联 / 80 个标签 / 10 个热搜词。

#### 接口

| 接口 | 说明 |
|---|---|
| `GET /api/home/overview` | 首页一次拿齐：`stats`(条目/分类/标签/注册数/搜索次数) + `categoryTree`(带每类数量) + `hotWords` + `recommends`(按 view_count 的热门榜) |
| `GET /api/home/search?keyword=&categoryId=&page=&size=` | **分级分类 + 关键词**搜索，分页。返回 `list/total/page/pages/hasMore` + `category`(含 `path`) |
| `GET /api/category/tree` | 分类树 |
| `GET /api/pony/{id}` | 详情（含所属分类、标签、同类推荐），看一次 `view_count + 1` |

#### 「分级」是怎么实现的（关键，别改坏）

1. 用户点一级分类「种族」(id=1) 时，**不能**只 `WHERE category_id = 1`——那样一只都查不到，
   因为小马挂在二级分类上。
2. `CategoryMapper.selectDescendantIds()` 用 **MySQL 8 递归 CTE** 把「自己 + 全部子孙」展开：
   ```sql
   WITH RECURSIVE sub AS (
     SELECT id FROM t_category WHERE id = #{id}
     UNION ALL SELECT c.id FROM t_category c JOIN sub s ON c.parent_id = s.id)
   SELECT id FROM sub
   ```
3. 展开后的 id 列表丢给 `PonyMapper.xml` 的 `search`，做 `p.id IN (SELECT pony_id FROM t_pony_category WHERE category_id IN (...))`，
   再叠加关键词条件（`name / alias / intro / tag` 四路 LIKE）。
4. 分类树上的「共 N 位」也**不能把子分类数量相加**：紫悦同时属于「独角兽」和「天角兽」，
   直接相加「种族」会变成 17。`CategoryServiceImpl.collect()` 是自底向上求 **集合并集**再取 size，实测「种族」= 16 ✓。

#### 其它要注意的

- 热门小马/详情用的是后端 `view_count`，所以点开详情后热门榜顺序会变，这是有意的。
- 前端 `src/home.js` 是新加的接口封装，风格和 `auth.js` 一致（`{code,msg,data}`、非 JSON 也给中文提示）。
- `.card` 原来是 `overflow: hidden` + `body{overflow:hidden}`，首页内容变长后会被**裁掉**。
  已改成 `max-height: calc(100vh - 40px); overflow-y: auto`，卡片内部滚动。
- 详情弹窗用了 `<Teleport to="body">`，避免被 `.card` 的滚动容器裁掉。**别改成普通 div 塞回卡片里。**
- `.search` 是 `<form>`，而全局有 `form { padding: 18px 28px 28px }`，会让搜索框比上下内容窄一圈，
  已用 `.search { padding: 0 }` 盖掉。
- 改分类/标签数据只需改 `sql\mlp_home.sql` 再执行一次；它是「先 DELETE id<=999 的参考资料再 INSERT」，
  **不会动 `t_user` 里的注册账号**（只对 `t_search_word` 是 `INSERT IGNORE`，已有的搜索次数不会被清零）。

### ✅ 5.7 「我的账号」页 · 接后端（2026-10-14 新增）

**改动前**：`DashboardView.vue` 只显示本地 `session.name`，一点后端数据都没有。
**改动后**：复用首页图鉴数据，做成「账号 + 你的同名小马」。

#### 接口

`GET /api/user/profile?id=1`（封装在 `src/user.js` 的 `apiProfile(id)`）

```json
{ "code": 200, "msg": "ok", "data": {
    "id": 1, "name": "碧琪",
    "createTime": "2026-10-14 09:05",
    "days": 1,
    "pony": { "id": 2, "name": "碧琪", "alias": "Pinkie Pie", "mark": "pinkie",
              "emoji": "🎈", "color": "#f7a8c4", "rarity": "稀有",
              "tags": ["派对", ...], "categories": [{ "parentName": "种族", "name": "陆马", ... }] }
}}
```

- `days` 用 `ChronoUnit.DAYS.between(注册日, 今天) + 1`，**注册当天算第 1 天**，最小 1。
- `pony` 是 `PonyService.findByName(账号名)` 的结果：先按中文名精确匹配，再按 `LOWER(alias)` 匹配英文别名，
  取 `limit 1`；图鉴里没有同名的就返回 `null`（前端给引导文案，不是错误）。
- 账号不存在 → `code 400`「没有找到这个账号，重新登录试试」。

#### 前端

- `DashboardView.vue` 重写：头像 / 名字 / 上次登录、注册时间 + 加入天数两栏、
  同名小马卡片（CutieMark 或 emoji、稀有度角标、可点分类）、「去图鉴逛逛 / 退出登录」；
  三种状态都处理了（loading / 有同名 / 没有同名 / 接口报错）。
- **首页加了深链**（`HomeView.applyQuery`）：`/home?category=12` 直接落分类（一级二级都吃）、
  `/home?pony=2` 直接弹详情。账号页点小马/点分类就是靠这两个 query 跳过去的。
  ⚠️ `created()` 因此改成了 `async` 并 `await loadOverview()` 之后才 `applyQuery()`——
  分类树还没到手时判定不出一级还是二级，调换顺序就失效。
- 新增样式：`.dash-kv / .dash-sec / .dash-pony / .dash-face / .dash-tip / .dash-actions`。

---

## 6. 文件地图（前端）

```
mlp-login-vue/                                     ← 前端项目就在这里
├── 启动全部.bat                  一键起前后端（会自动等端口、打印结果）  ← GBK
├── 启动前端.bat                  装依赖(如缺) + vite，浏览器由 打开网页.bat 开
├── 启动后端.bat                  Spring Boot 8081
├── 停止全部.bat                  按端口 5173/8081 杀进程
├── 环境检测.bat                  Node/JDK/Maven/目录/端口 一次性自检
├── 打开网页.bat                  内部用：轮询端口起来后再开默认浏览器
├── tools/static-server.cjs       零依赖兜底：起 dist/ + 代理 /api → 8081
├── index.html                    入口，挂 #app，title=小马王国 · 账号中心
├── vite.config.js                5173，open:false，strictPort，proxy /api → 8081
├── package.json                  dev / build / preview 三个脚本
├── .gitignore                    node_modules, dist
├── src/
│   ├── main.js                   挂载 + 引入样式
│   ├── App.vue                   页面外壳 + 主题切换 + toast
│   ├── auth.js                   会话(localStorage/sessionStorage) + /api/auth 请求封装
│   ├── home.js                   首页图鉴接口封装：/api/home/* + /api/category + /api/pony
│   ├── router/index.js           / → /home(需登录)、/dashboard(需登录)、/login、/register
│   ├── assets/style.css          马卡龙主题样式（含首页统计条/分级筛选/详情弹窗）
│   ├── components/PonyBackground.vue / CutieMark.vue
│   └── views/  HomeView.vue(登录后首页 · 全部走后端) / LoginView.vue / RegisterView.vue / DashboardView.vue
└── dist/                         构建产物（.gitignore，但兜底预览要用）

java-main/                                         ← 仓库根 / 启动入口
├── 启动全部.bat / 启动前端.bat / 启动后端.bat / 停止全部.bat / 环境检测.bat  ← 都只是转发到 mlp-login-vue\
├── 恢复后端.bat                  D 盘后端被还原成空壳时，用 backend-src 拷回去 + 依次执行两个建库 SQL
├── backend-src/                  后端源码备份：pom.xml + src\ + sql\（**改完后端要同步**）
└── 推送GitHub.bat                提交 + 同步远程 + 推送（会检查 node_modules 有没有被忽略）
```

后端关键文件：
```
D:\workSpace\springBootTest\
├── pom.xml                       Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + mysql-connector-j
├── mvnw / mvnw.cmd               wrapper（only-script，会去官网下 Maven，慢；优先用 IDEA 自带 Maven）
├── sql\mlp_login.sql             账号库：建库建表 + 老库自动删 email 列，可重复执行
├── sql\mlp_home.sql              **首页图鉴库**：5 张表 + 分级分类数据，可重复执行（先跑 mlp_login.sql）
├── src/main/resources/application.properties         spring.profiles.active=dev
├── src/main/resources/application-dev.properties     server.port=8081 + mlp_login 数据源 + mapper-locations
├── src/main/resources/mapper/PonyMapper.xml          **分级分类+关键词搜索的动态 SQL**
├── src/main/java/com/fenglin/springboottest/
│   ├── SpringBootTestApplication.java
│   ├── common/Result.java              统一响应 { code, msg, data }
│   ├── common/PasswordUtil.java        SHA-256(salt+明文)，16 位随机 salt
│   ├── common/GlobalExceptionHandler.java  异常也返回 JSON，不吐 HTML 错误页
│   ├── entity/User.java                t_user（无 email）
│   ├── entity/Category.java            t_category（自关联，分级）
│   ├── entity/Pony.java / PonyTag.java / SearchWord.java
│   ├── mapper/UserMapper.java          @Mapper + MyBatis-Plus BaseMapper
│   ├── mapper/CategoryMapper.java      递归 CTE 取子孙分类 + 分类关联关系
│   ├── mapper/PonyMapper.java          搜索 / 批量标签 / 批量分类 / view_count+1（SQL 在 XML 里）
│   ├── mapper/PonyTagMapper.java / SearchWordMapper.java
│   ├── vo/CategoryNode.java            分类树节点（带 count / children）
│   ├── vo/PonyVO.java                  卡片数据（含 tags / categories）
│   ├── vo/PageResult.java              通用分页
│   ├── service/UserService.java / UserServiceImpl.java / CaptchaService.java
│   ├── service/CategoryService.java / CategoryServiceImpl.java   分类树 + 子孙 id + 路径
│   ├── service/PonyService.java / PonyServiceImpl.java           首页总览 / 搜索 / 详情
│   ├── controller/AuthController.java      /api/auth/captcha|register|login
│   ├── controller/HomeController.java      /api/home/overview|search
│   ├── controller/CategoryController.java  /api/category/tree
│   ├── controller/PonyController.java      /api/pony/{id}
│   ├── config/CorsConfig.java              localhost:5173 / 4173 跨域（要带 cookie，所以不能写 *）
│   └── UserController.java                 thymeleaf 老 demo，与登录无关
└── log/xx.log                     运行日志
```

---

## 7. 后续提示词（用户可以直接抄）

- 后端又不见了：`D:\workSpace\springBootTest 又退回空壳了，用桌面 java-main\backend-src 里的备份把它恢复，并确认 /api/auth/captcha、/api/auth/register、/api/auth/login、/api/home/overview、/api/home/search 都能通。`
- 首页/搜索继续做：`首页图鉴还要加 XXX 功能，数据走 /api/home/*，分类数据在 sql\mlp_home.sql 里加，别把分级搜索写成只查单层 category_id。`
- 一键启动又坏了：`启动全部.bat 起不来，先看 AGENTS.md 第 1、3、5.3 节，别重新摸环境。`
- 推代码：`双击 推送GitHub.bat 把代码推到 hhyllym/java；要是报 non-fast-forward 或者冲突，看 AGENTS.md 5.4 节。`
- 继续做页面：`在 mlp-login-vue 里加 XXX 页面，保持马卡龙 / 小马宝莉风格。`
- 提速：`看看怎么让 启动前端.bat / 启动后端.bat 更快。`
