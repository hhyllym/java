<template>
  <div class="admin">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="brand">🛠 小马乐园 · 后台管理系统</div>
      <div class="right">
        <span class="who">{{ admin.permission === 2 ? '超级管理员' : '管理员' }}：{{ admin.name || 'admin' }}</span>
        <button class="logout" @click="handleLogout">退出</button>
      </div>
    </header>

    <div class="body">
      <!-- 侧边菜单 -->
      <aside class="sidebar">
        <button class="menu" :class="{ on: menu === 'overview' }" @click="switchMenu('overview')">📊 概览</button>
        <button class="menu" :class="{ on: menu === 'pony' }" @click="switchMenu('pony')">🦄 小马图鉴</button>
        <button class="menu" :class="{ on: menu === 'category' }" @click="switchMenu('category')">🗂 分类管理</button>
        <button class="menu" :class="{ on: menu === 'tag' }" @click="switchMenu('tag')">🏷 标签管理</button>
        <button class="menu" :class="{ on: menu === 'user' }" @click="switchMenu('user')">👥 用户管理</button>
        <button class="menu" :class="{ on: menu === 'message' }" @click="switchMenu('message')">💬 消息管理</button>
      </aside>

      <!-- 内容区 -->
      <main class="content">
        <!-- ============ 概览 ============ -->
        <section v-if="menu === 'overview'">
          <h2 class="title">数据概览</h2>
          <p class="desc">首页展示的数据都来自这里，下面是实时统计。点卡片可以直接跳去管理。</p>
          <div class="stat-grid">
            <button class="stat clickable" @click="switchMenu('pony')"><b>{{ ov.ponyTotal || 0 }}</b><span>图鉴条目</span></button>
            <button class="stat clickable" @click="switchMenu('category')"><b>{{ ov.categoryTotal || 0 }}</b><span>分类</span></button>
            <button class="stat clickable" @click="switchMenu('tag')"><b>{{ ov.tagTotal || 0 }}</b><span>标签</span></button>
            <button class="stat clickable" @click="switchMenu('user')"><b>{{ ov.memberTotal || 0 }}</b><span>注册小马</span></button>
          </div>

          <div class="panel-grid">
            <!-- 热搜词 -->
            <div class="panel">
              <div class="panel-head">🔥 热搜词 TOP {{ hotWords.length }}</div>
              <div v-if="hotWords.length" class="chips">
                <span v-for="(w, i) in hotWords" :key="w.word" class="chip" :class="{ top: i < 3 }">
                  {{ w.word }}<em>{{ w.hitCount }}</em>
                </span>
              </div>
              <div v-else class="panel-empty">还没有人搜过</div>
            </div>
            <!-- 热门小马 -->
            <div class="panel">
              <div class="panel-head">⭐ 热门小马</div>
              <div v-if="hotPonies.length" class="rank">
                <div v-for="(p, i) in hotPonies" :key="p.id" class="rank-item">
                  <span class="rank-no" :class="'no-' + (i + 1)">{{ i + 1 }}</span>
                  <span class="dot" :style="{ background: p.color || '#b79cf0' }"></span>
                  <span class="rank-name">{{ p.name }}</span>
                  <span class="muted">{{ p.viewCount || 0 }} 次浏览</span>
                </div>
              </div>
              <div v-else class="panel-empty">暂无浏览数据</div>
            </div>
            <!-- 最近注册 -->
            <div class="panel">
              <div class="panel-head">🆕 最近注册的小马</div>
              <div v-if="recentUsers.length" class="rank">
                <div v-for="u in recentUsers" :key="u.id" class="rank-item">
                  <span class="rank-emoji">{{ u.avatar || '🐴' }}</span>
                  <span class="rank-name">{{ u.name }}</span>
                  <span class="rank-perm" :class="permClass(u.permission)">{{ permText(u.permission) }}</span>
                  <span class="muted">{{ fmt(u.createTime) }}</span>
                </div>
              </div>
              <div v-else class="panel-empty">还没有人注册</div>
            </div>
            <!-- 最新消息 -->
            <div class="panel">
              <div class="panel-head">💬 最新动态</div>
              <div v-if="recentMessages.length" class="rank">
                <div v-for="m in recentMessages" :key="m.id" class="rank-item">
                  <span class="pill">{{ typeText(m.type) }}</span>
                  <span class="rank-msg">{{ m.content }}</span>
                </div>
              </div>
              <div v-else class="panel-empty">还没有消息</div>
              <button class="panel-more" @click="switchMenu('message')">去消息管理 →</button>
            </div>
          </div>

          <div class="quick">
            <button class="q" @click="switchMenu('pony')">去管理小马图鉴 →</button>
            <button class="q" @click="switchMenu('category')">去管理分类 →</button>
            <button class="q" @click="switchMenu('message')">发一条系统通知 →</button>
          </div>
        </section>

        <!-- ============ 小马图鉴 ============ -->
        <section v-if="menu === 'pony'">
          <div class="bar">
            <h2 class="title">小马图鉴管理</h2>
            <div class="spacer"></div>
            <input v-model.trim="ponyKeyword" class="search" placeholder="搜名字 / 英文名" @keyup.enter="loadPony(1)" />
            <button class="btn" @click="loadPony(1)">查询</button>
            <button class="btn primary" @click="openPonyForm(null)">+ 新增小马</button>
          </div>
          <div v-if="ponyLoading" class="loading">加载中…</div>
          <table v-else class="tbl">
            <thead>
              <tr><th>#</th><th>小马</th><th>英文名</th><th>稀有度</th><th>标签</th><th>分类</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-for="p in ponyList" :key="p.id">
                <td>{{ p.id }}</td>
                <td>
                  <span class="dot" :style="{ background: p.color || '#b79cf0' }"></span>
                  {{ p.name }}
                </td>
                <td class="muted">{{ p.alias }}</td>
                <td><span class="rarity" :class="rarityClass(p.rarity)">{{ p.rarity }}</span></td>
                <td class="muted">{{ (p.tags || []).join('、') || '—' }}</td>
                <td class="muted">{{ (p.categories || []).map(c => c.name).join('、') || '—' }}</td>
                <td>
                  <button class="link" @click="openPonyForm(p)">编辑</button>
                  <button class="link danger" @click="delPony(p)">删除</button>
                </td>
              </tr>
              <tr v-if="!ponyList.length"><td colspan="7" class="empty">暂无数据</td></tr>
            </tbody>
          </table>
          <div class="pager" v-if="ponyPages > 1">
            <button class="btn" :disabled="ponyPage <= 1" @click="loadPony(ponyPage - 1)">上一页</button>
            <span>{{ ponyPage }} / {{ ponyPages }}</span>
            <button class="btn" :disabled="ponyPage >= ponyPages" @click="loadPony(ponyPage + 1)">下一页</button>
          </div>
        </section>

        <!-- ============ 分类管理 ============ -->
        <section v-if="menu === 'category'">
          <div class="bar">
            <h2 class="title">分类管理</h2>
            <div class="spacer"></div>
            <button class="btn primary" @click="openCategoryForm(null)">+ 新增分类</button>
          </div>
          <table class="tbl">
            <thead><tr><th>#</th><th>名称</th><th>层级</th><th>图标</th><th>所属父级</th><th>排序</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="c in categoryList" :key="c.id">
                <td>{{ c.id }}</td>
                <td>{{ c.icon }} {{ c.name }}</td>
                <td>{{ c.level === 1 ? '一级' : '二级' }}</td>
                <td>{{ c.icon }}</td>
                <td class="muted">{{ parentName(c.parentId) }}</td>
                <td class="muted">{{ c.sortNo }}</td>
                <td>
                  <button class="link" @click="openCategoryForm(c)">编辑</button>
                  <button class="link danger" @click="delCategory(c)">删除</button>
                </td>
              </tr>
              <tr v-if="!categoryList.length"><td colspan="7" class="empty">暂无数据</td></tr>
            </tbody>
          </table>
        </section>

        <!-- ============ 标签管理 ============ -->
        <section v-if="menu === 'tag'">
          <div class="bar">
            <h2 class="title">标签管理</h2>
            <div class="spacer"></div>
          </div>
          <div class="inline-form">
            <span class="muted">给某小马加标签：</span>
            <input v-model.trim="tagAdd.ponyId" class="search small" placeholder="小马 id" style="width:90px" />
            <input v-model.trim="tagAdd.tag" class="search small" placeholder="标签内容" @keyup.enter="addTag" />
            <button class="btn primary" @click="addTag">添加</button>
          </div>
          <table class="tbl">
            <thead><tr><th>标签</th><th>出现次数</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="t in tagList" :key="t.tag">
                <td>{{ t.tag }}</td>
                <td>{{ t.cnt }}</td>
                <td>
                  <button class="link" @click="openRename(t.tag)">改名</button>
                  <button class="link danger" @click="delTag(t.tag)">删除</button>
                </td>
              </tr>
              <tr v-if="!tagList.length"><td colspan="3" class="empty">暂无标签</td></tr>
            </tbody>
          </table>
        </section>

        <!-- ============ 用户管理 ============ -->
        <section v-if="menu === 'user'">
          <div class="bar">
            <h2 class="title">用户管理</h2>
            <span v-if="userTotal" class="muted">共 {{ userTotal }} 位</span>
            <div class="spacer"></div>
            <select v-model.number="userPermFilter" class="search" @change="loadUser(1)">
              <option :value="-1">全部权限</option>
              <option :value="1">普通用户</option>
              <option :value="0">普通管理员</option>
              <option :value="2">超级管理员</option>
            </select>
            <input v-model.trim="userKeyword" class="search" placeholder="搜名字" @keyup.enter="loadUser(1)" />
            <button class="btn" @click="loadUser(1)">查询</button>
          </div>
          <div v-if="userLoading" class="loading">加载中…</div>
          <table v-else class="tbl">
            <thead><tr><th>#</th><th>小马名字</th><th>头像</th><th>积分</th><th>权限</th><th>注册时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="u in userList" :key="u.id">
                <td>{{ u.id }}</td>
                <td>{{ u.name }}</td>
                <td>{{ u.avatar || '🐴' }}</td>
                <td>{{ u.points }}</td>
                <td>
                  <span class="tag" :class="permClass(u.permission)">{{ permText(u.permission) }}</span>
                </td>
                <td class="muted">{{ fmt(u.createTime) }}</td>
                <td>
                  <button class="link" @click="openPoints(u)">改积分</button>
                  <button class="link" @click="openPwd(u)">重置密码</button>
                  <template v-if="admin.permission === 2 && u.permission !== 2">
                    <button class="link" @click="setRole(u, 2)">升为超管</button>
                    <button class="link" @click="setRole(u, u.permission === 0 ? 1 : 0)">
                      {{ u.permission === 0 ? '降为普通用户' : '设为管理员' }}
                    </button>
                  </template>
                  <button class="link danger" @click="delUser(u)">删除</button>
                </td>
              </tr>
              <tr v-if="!userList.length"><td colspan="7" class="empty">暂无用户</td></tr>
            </tbody>
          </table>
          <div class="pager" v-if="userPages > 1">
            <button class="btn" :disabled="userPage <= 1" @click="loadUser(userPage - 1)">上一页</button>
            <span>{{ userPage }} / {{ userPages }}</span>
            <button class="btn" :disabled="userPage >= userPages" @click="loadUser(userPage + 1)">下一页</button>
          </div>
        </section>

        <!-- ============ 消息管理 ============ -->
        <section v-if="menu === 'message'">
          <div class="bar">
            <h2 class="title">消息管理</h2>
            <span v-if="msgTotal" class="muted">共 {{ msgTotal }} 条</span>
            <div class="spacer"></div>
            <button class="btn primary" @click="openSend">📣 发系统通知</button>
            <select v-model="msgType" class="search" @change="loadMessage(1)">
              <option value="">全部类型</option>
              <option value="BOARD">留言板</option>
              <option value="PM">私信</option>
              <option value="NOTIFY">系统通知</option>
            </select>
            <input v-model.trim="msgKeyword" class="search" placeholder="搜内容" @keyup.enter="loadMessage(1)" />
            <button class="btn" @click="loadMessage(1)">查询</button>
          </div>
          <div v-if="msgLoading" class="loading">加载中…</div>
          <table v-else class="tbl">
            <thead><tr><th>#</th><th>类型</th><th>发送者</th><th>接收者</th><th>内容</th><th>时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="m in messageList" :key="m.id">
                <td>{{ m.id }}</td>
                <td><span class="pill">{{ typeText(m.type) }}</span></td>
                <td class="muted">{{ m.fromName || '—' }}</td>
                <td class="muted">{{ m.toName || (m.toId ? m.toId : '全员') }}</td>
                <td class="msg">{{ m.content }}</td>
                <td class="muted">{{ fmt(m.createTime) }}</td>
                <td><button class="link danger" @click="delMessage(m)">删除</button></td>
              </tr>
              <tr v-if="!messageList.length"><td colspan="7" class="empty">暂无消息</td></tr>
            </tbody>
          </table>
          <div class="pager" v-if="msgPages > 1">
            <button class="btn" :disabled="msgPage <= 1" @click="loadMessage(msgPage - 1)">上一页</button>
            <span>{{ msgPage }} / {{ msgPages }}</span>
            <button class="btn" :disabled="msgPage >= msgPages" @click="loadMessage(msgPage + 1)">下一页</button>
          </div>
        </section>
      </main>
    </div>

    <!-- ============ 小马表单弹窗 ============ -->
    <div class="mask" v-if="ponyForm.show" @click.self="ponyForm.show = false">
      <div class="modal">
        <h3>{{ ponyForm.data.id ? '编辑小马' : '新增小马' }}</h3>
        <div class="grid2">
          <label>名字*<input v-model="ponyForm.data.name" placeholder="如 星澜" /></label>
          <label>英文名<input v-model="ponyForm.data.alias" placeholder="如 Lumina Gleam" /></label>
          <label>心印徽记
            <select v-model="ponyForm.data.mark">
              <option v-for="m in markOptions" :key="m" :value="m">{{ m }}</option>
            </select>
          </label>
          <label>emoji<input v-model="ponyForm.data.emoji" placeholder="📖" /></label>
          <label>招牌色<input v-model="ponyForm.data.color" placeholder="#b79cf0" /></label>
          <label>稀有度
            <select v-model="ponyForm.data.rarity">
              <option v-for="r in rarityOptions" :key="r" :value="r">{{ r }}</option>
            </select>
          </label>
          <label>排序<input v-model.number="ponyForm.data.sortNo" type="number" /></label>
        </div>
        <label class="full">简介<textarea v-model="ponyForm.data.intro" rows="2"></textarea></label>
        <label class="full">标签（空格或逗号分隔）
          <input v-model="ponyForm.tagText" placeholder="魔法 读书 学霸" />
        </label>
        <div class="full">
          <div class="lbl">所属分类（可多选）</div>
          <div class="checks">
            <label v-for="c in categoryList" :key="c.id" class="chk">
              <input type="checkbox" :value="c.id" v-model="ponyForm.data.categoryIds" />
              {{ c.level === 1 ? '【' + c.name + '】' : '　' + c.name }}
            </label>
          </div>
        </div>
        <div class="modal-bar">
          <button class="btn" @click="ponyForm.show = false">取消</button>
          <button class="btn primary" :disabled="ponyForm.saving" @click="savePony">{{ ponyForm.saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- ============ 分类表单弹窗 ============ -->
    <div class="mask" v-if="categoryForm.show" @click.self="categoryForm.show = false">
      <div class="modal">
        <h3>{{ categoryForm.data.id ? '编辑分类' : '新增分类' }}</h3>
        <div class="grid2">
          <label>名称*<input v-model="categoryForm.data.name" placeholder="如 云翼驹" /></label>
          <label>图标<input v-model="categoryForm.data.icon" placeholder="🐦" /></label>
          <label>主题色<input v-model="categoryForm.data.color" placeholder="#8fc7f0" /></label>
          <label>排序<input v-model.number="categoryForm.data.sortNo" type="number" /></label>
          <label class="full">父级分类
            <select v-model="categoryForm.data.parentId">
              <option :value="0">（无 · 作为一级分类）</option>
              <option v-for="c in level1Categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
        </div>
        <label class="full">一句话说明<textarea v-model="categoryForm.data.intro" rows="2"></textarea></label>
        <div class="modal-bar">
          <button class="btn" @click="categoryForm.show = false">取消</button>
          <button class="btn primary" :disabled="categoryForm.saving" @click="saveCategory">{{ categoryForm.saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- ============ 标签改名弹窗 ============ -->
    <div class="mask" v-if="renameForm.show" @click.self="renameForm.show = false">
      <div class="modal">
        <h3>标签改名</h3>
        <label class="full">原标签<input :value="renameForm.oldTag" disabled /></label>
        <label class="full">新标签<input v-model="renameForm.newTag" placeholder="新标签名" /></label>
        <div class="modal-bar">
          <button class="btn" @click="renameForm.show = false">取消</button>
          <button class="btn primary" @click="doRename">确认改名</button>
        </div>
      </div>
    </div>

    <!-- ============ 改积分弹窗 ============ -->
    <div class="mask" v-if="pointsForm.show" @click.self="pointsForm.show = false">
      <div class="modal">
        <h3>调整积分 · {{ pointsForm.name }}</h3>
        <label class="full">积分余额
          <input v-model.number="pointsForm.points" type="number" />
        </label>
        <div class="modal-bar">
          <button class="btn" @click="pointsForm.show = false">取消</button>
          <button class="btn primary" @click="savePoints">保存</button>
        </div>
      </div>
    </div>

    <!-- ============ 重置密码弹窗 ============ -->
    <div class="mask" v-if="pwdForm.show" @click.self="pwdForm.show = false">
      <div class="modal">
        <h3>重置密码 · {{ pwdForm.name }}</h3>
        <label class="full">新密码（6~32 位）
          <input v-model="pwdForm.password" type="text" placeholder="给「{{ pwdForm.name }}」设一个新密码" @keyup.enter="savePwd" />
        </label>
        <p class="tip">重置后该小马下次登录就用新密码，旧密码立刻作废。</p>
        <div class="modal-bar">
          <button class="btn" @click="pwdForm.show = false">取消</button>
          <button class="btn primary" :disabled="pwdForm.saving" @click="savePwd">{{ pwdForm.saving ? '保存中…' : '确认重置' }}</button>
        </div>
      </div>
    </div>

    <!-- ============ 发系统通知弹窗 ============ -->
    <div class="mask" v-if="sendForm.show" @click.self="sendForm.show = false">
      <div class="modal">
        <h3>发系统通知</h3>
        <label class="full">收件小马（留空 = 全员广播）
          <input v-model="sendForm.toName" placeholder="如 泡芙；留空发给所有小马" />
        </label>
        <label class="full">通知内容
          <textarea v-model="sendForm.content" rows="4" placeholder="要通知大家什么事情，写在这里（最多 500 字）"></textarea>
        </label>
        <div class="modal-bar">
          <button class="btn" @click="sendForm.show = false">取消</button>
          <button class="btn primary" :disabled="sendForm.sending" @click="sendNotify">{{ sendForm.sending ? '发送中…' : '发送' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getAdminSession, clearAdminSession,
  apiAdminMe, apiAdminPonyList, apiAdminPonyCreate, apiAdminPonyUpdate, apiAdminPonyDelete,
  apiAdminCategoryList, apiAdminCategoryCreate, apiAdminCategoryUpdate, apiAdminCategoryDelete,
  apiAdminTagList, apiAdminTagAdd, apiAdminTagRename, apiAdminTagDelete,
  apiAdminUserList, apiAdminUserDelete, apiAdminUserPoints, apiAdminUserPassword, apiAdminUserPermission,
  apiAdminMessageList, apiAdminMessageDelete, apiAdminMessageSend
} from '../adminAuth'

const MARK_OPTIONS = ['star', 'pinkie', 'twilight', 'dash', 'applejack', 'fluttershy', 'rarity']
const RARITY_OPTIONS = ['常见', '稀有', '传说']
const RARITY_CLASS = { 常见: 'rr-common', 稀有: 'rr-rare', 传说: 'rr-legend' }

export default {
  name: 'AdminView',
  inject: ['toast'],
  data() {
    return {
      admin: Object.assign({ permission: 1, name: '' }, getAdminSession() || {}),
      menu: 'overview',
      markOptions: MARK_OPTIONS,
      rarityOptions: RARITY_OPTIONS,

      ov: {},
      // 概览的四块小面板（数据来自 /api/home/overview + 管理端接口）
      hotWords: [], hotPonies: [], recentUsers: [], recentMessages: [],

      // 小马
      ponyList: [], ponyPage: 1, ponyPages: 1, ponyKeyword: '', ponyLoading: false,
      ponyForm: { show: false, saving: false, data: {}, tagText: '' },

      // 分类
      categoryList: [],
      categoryForm: { show: false, saving: false, data: {} },

      // 标签
      tagList: [], tagAdd: { ponyId: '', tag: '' },
      renameForm: { show: false, oldTag: '', newTag: '' },

      // 用户
      userList: [], userPage: 1, userPages: 1, userKeyword: '', userLoading: false,
      userTotal: 0, userPermFilter: -1,
      pointsForm: { show: false, id: null, name: '', points: 0 },
      pwdForm: { show: false, saving: false, id: null, name: '', password: '' },

      // 消息
      messageList: [], msgPage: 1, msgPages: 1, msgType: '', msgKeyword: '', msgLoading: false,
      msgTotal: 0,
      sendForm: { show: false, sending: false, toName: '', content: '' }
    }
  },
  computed: {
    parentMap() {
      const m = {}
      this.categoryList.forEach(c => { m[c.id] = c.name })
      return m
    },
    level1Categories() {
      return this.categoryList.filter(c => c.level === 1)
    }
  },
  async mounted() {
    // 进来先校验令牌，失效就回登录页
    const me = await apiAdminMe()
    if (me.code !== 200) {
      clearAdminSession()
      this.$router.replace({ name: 'adminLogin' })
      return
    }
    if (me.data) {
      this.admin.name = me.data.name || this.admin.name
      // 超级管理员 permission=2；普通管理员 permission=0
      if (me.data.permission === 0 || me.data.permission === 2) {
        this.admin.permission = me.data.permission
      }
    }
    this.loadOverview()
  },
  methods: {
    // ---------- 通用 ----------
    rarityClass(r) { return RARITY_CLASS[r] || 'rr-common' },
    parentName(pid) {
      if (!pid) return '（一级分类）'
      return this.parentMap[pid] || '—'
    },
    fmt(t) {
      if (!t) return '—'
      const d = new Date(t)
      if (isNaN(d.getTime())) return String(t)
      const p = n => (n < 10 ? '0' + n : n)
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    typeText(t) {
      return { BOARD: '留言板', PM: '私信', NOTIFY: '系统通知' }[t] || t
    },
    switchMenu(m) {
      this.menu = m
      if (m === 'pony') this.loadPony(1)
      else if (m === 'category') this.loadCategory()
      else if (m === 'tag') this.loadTag()
      else if (m === 'user') this.loadUser(1)
      else if (m === 'message') this.loadMessage(1)
      else if (m === 'overview') this.loadOverview()
    },
    handleLogout() {
      clearAdminSession()
      this.toast('已退出后台', 'ok')
      this.$router.replace({ name: 'home' })
    },

    // ---------- 概览 ----------
    async loadOverview() {
      // 第一块：统计 + 热搜词 + 热门小马，一次拿齐
      try {
        const res = await fetch('/api/home/overview')
        const text = await res.text()
        const d = JSON.parse(text)
        const data = (d && d.data) || {}
        this.ov = data.stats || {}
        this.hotWords = (data.hotWords || []).slice(0, 10)
        this.hotPonies = (data.recommends || []).slice(0, 5)
      } catch (e) {
        this.ov = {}
        this.hotWords = []
        this.hotPonies = []
      }
      // 第二块：最近注册（管理端接口，复用用户列表按注册时间倒序的第一页）
      const users = await apiAdminUserList('', 1, 6)
      this.recentUsers = users.code === 200 ? (users.data && users.data.list) || [] : []
      // 第三块：最新动态（消息列表第一页）
      const msgs = await apiAdminMessageList('', '', 1, 5)
      this.recentMessages = msgs.code === 200 ? (msgs.data && msgs.data.list) || [] : []
    },

    // ---------- 小马 ----------
    async loadPony(page) {
      this.ponyLoading = true
      this.ponyPage = page || 1
      const res = await apiAdminPonyList(this.ponyKeyword, this.ponyPage, 12)
      this.ponyLoading = false
      if (res.code !== 200) { this.toast(res.msg || '加载失败', 'err'); return }
      const d = res.data || {}
      this.ponyList = d.list || []
      this.ponyPages = d.pages || 1
    },
    openPonyForm(p) {
      if (!this.categoryList.length) this.loadCategory()
      if (p) {
        this.ponyForm.data = {
          id: p.id, name: p.name, alias: p.alias, mark: p.mark || 'star',
          emoji: p.emoji, color: p.color, rarity: p.rarity || '常见',
          intro: p.intro, sortNo: p.sortNo || 0,
          categoryIds: (p.categories || []).map(c => c.id)
        }
        this.ponyForm.tagText = (p.tags || []).join(' ')
      } else {
        this.ponyForm.data = {
          name: '', alias: '', mark: 'star', emoji: '⭐', color: '#b79cf0',
          rarity: '常见', intro: '', sortNo: 0, categoryIds: []
        }
        this.ponyForm.tagText = ''
      }
      this.ponyForm.show = true
    },
    async savePony() {
      if (!this.ponyForm.data.name || !this.ponyForm.data.name.trim()) {
        this.toast('请填写小马名字', 'err'); return
      }
      this.ponyForm.saving = true
      const payload = {
        ...this.ponyForm.data,
        name: this.ponyForm.data.name.trim(),
        tags: (this.ponyForm.tagText || '').split(/[\s,，、]+/).map(s => s.trim()).filter(Boolean),
        categoryIds: this.ponyForm.data.categoryIds || []
      }
      const isEdit = !!payload.id
      const res = isEdit ? await apiAdminPonyUpdate(payload) : await apiAdminPonyCreate(payload)
      this.ponyForm.saving = false
      if (res.code !== 200) { this.toast(res.msg || '保存失败', 'err'); return }
      this.toast(res.msg || '已保存', 'ok')
      this.ponyForm.show = false
      this.loadPony(this.ponyPage)
      this.loadOverview()
    },
    async delPony(p) {
      if (!confirm(`确定删除「${p.name}」？该小马下的标签/分类关联也会一起删掉。`)) return
      const res = await apiAdminPonyDelete(p.id)
      if (res.code !== 200) { this.toast(res.msg || '删除失败', 'err'); return }
      this.toast(res.msg || '已删除', 'ok')
      this.loadPony(this.ponyPage)
      this.loadOverview()
    },

    // ---------- 分类 ----------
    async loadCategory() {
      const res = await apiAdminCategoryList()
      if (res.code !== 200) { this.toast(res.msg || '加载失败', 'err'); return }
      this.categoryList = res.data || []
    },
    openCategoryForm(c) {
      if (c) {
        this.categoryForm.data = { id: c.id, name: c.name, icon: c.icon, color: c.color, intro: c.intro, parentId: c.parentId || 0, sortNo: c.sortNo || 0 }
      } else {
        this.categoryForm.data = { name: '', icon: '🏷', color: '#b79cf0', intro: '', parentId: 0, sortNo: 0 }
      }
      this.categoryForm.show = true
    },
    async saveCategory() {
      if (!this.categoryForm.data.name || !this.categoryForm.data.name.trim()) {
        this.toast('请填写分类名', 'err'); return
      }
      this.categoryForm.saving = true
      const payload = { ...this.categoryForm.data, name: this.categoryForm.data.name.trim() }
      const isEdit = !!payload.id
      const res = isEdit ? await apiAdminCategoryUpdate(payload) : await apiAdminCategoryCreate(payload)
      this.categoryForm.saving = false
      if (res.code !== 200) { this.toast(res.msg || '保存失败', 'err'); return }
      this.toast(res.msg || '已保存', 'ok')
      this.categoryForm.show = false
      this.loadCategory()
      this.loadOverview()
    },
    async delCategory(c) {
      if (!confirm(`确定删除「${c.name}」？它下面的所有子分类和条目关联都会一起删掉。`)) return
      const res = await apiAdminCategoryDelete(c.id)
      if (res.code !== 200) { this.toast(res.msg || '删除失败', 'err'); return }
      this.toast(res.msg || '已删除', 'ok')
      this.loadCategory()
      this.loadOverview()
    },

    // ---------- 标签 ----------
    async loadTag() {
      const res = await apiAdminTagList()
      if (res.code !== 200) { this.toast(res.msg || '加载失败', 'err'); return }
      this.tagList = res.data || []
    },
    async addTag() {
      const ponyId = Number(this.tagAdd.ponyId)
      const tag = this.tagAdd.tag.trim()
      if (!ponyId || !tag) { this.toast('请填写小马 id 和标签', 'err'); return }
      const res = await apiAdminTagAdd(ponyId, tag)
      if (res.code !== 200) { this.toast(res.msg || '添加失败', 'err'); return }
      this.toast(res.msg || '已添加', 'ok')
      this.tagAdd = { ponyId: '', tag: '' }
      this.loadTag()
    },
    openRename(oldTag) {
      this.renameForm = { show: true, oldTag, newTag: '' }
    },
    async doRename() {
      const newTag = this.renameForm.newTag.trim()
      if (!newTag) { this.toast('请填写新标签名', 'err'); return }
      const res = await apiAdminTagRename(this.renameForm.oldTag, newTag)
      if (res.code !== 200) { this.toast(res.msg || '改名失败', 'err'); return }
      this.toast(res.msg || '已改名', 'ok')
      this.renameForm.show = false
      this.loadTag()
    },
    async delTag(tag) {
      if (!confirm(`确定删除标签「${tag}」？所有小马上的这个标签都会清掉。`)) return
      const res = await apiAdminTagDelete(tag)
      if (res.code !== 200) { this.toast(res.msg || '删除失败', 'err'); return }
      this.toast(res.msg || '已删除', 'ok')
      this.loadTag()
    },

    // ---------- 用户 ----------
    async loadUser(page) {
      this.userLoading = true
      this.userPage = page || 1
      const res = await apiAdminUserList(this.userKeyword, this.userPage, 12, this.userPermFilter)
      this.userLoading = false
      if (res.code !== 200) { this.toast(res.msg || '加载失败', 'err'); return }
      const d = res.data || {}
      this.userList = d.list || []
      this.userPages = d.pages || 1
      this.userTotal = d.total || 0
    },
    openPoints(u) {
      this.pointsForm = { show: true, id: u.id, name: u.name, points: u.points || 0 }
    },
    async savePoints() {
      const res = await apiAdminUserPoints(this.pointsForm.id, Number(this.pointsForm.points) || 0)
      if (res.code !== 200) { this.toast(res.msg || '保存失败', 'err'); return }
      this.toast(res.msg || '已保存', 'ok')
      this.pointsForm.show = false
      this.loadUser(this.userPage)
    },
    async delUser(u) {
      if (!confirm(`确定删除用户「${u.name}」？`)) return
      const res = await apiAdminUserDelete(u.id)
      if (res.code !== 200) { this.toast(res.msg || '删除失败', 'err'); return }
      this.toast(res.msg || '已删除', 'ok')
      this.loadUser(this.userPage)
      this.loadOverview()
    },
    openPwd(u) {
      this.pwdForm = { show: true, saving: false, id: u.id, name: u.name, password: '' }
    },
    async savePwd() {
      const pw = (this.pwdForm.password || '').trim()
      if (pw.length < 6 || pw.length > 32) { this.toast('新密码要 6~32 位', 'err'); return }
      this.pwdForm.saving = true
      const res = await apiAdminUserPassword(this.pwdForm.id, pw)
      this.pwdForm.saving = false
      if (res.code !== 200) { this.toast(res.msg || '重置失败', 'err'); return }
      this.toast(res.msg || '密码已重置', 'ok')
      this.pwdForm.show = false
    },
    permText(p) {
      if (p === 2) return '超级管理员'
      if (p === 0) return '普通管理员'
      return '普通用户'
    },
    permClass(p) {
      if (p === 2) return 'tag-super'
      if (p === 0) return 'tag-admin'
      return 'tag-user'
    },
    async setRole(u, target) {
      const res = await apiAdminUserPermission(u.id, target)
      if (res.code !== 200) { this.toast(res.msg || '操作失败', 'err'); return }
      this.toast(res.msg || '已更新权限', 'ok')
      this.loadUser(this.userPage)
    },

    // ---------- 消息 ----------
    async loadMessage(page) {
      this.msgLoading = true
      this.msgPage = page || 1
      const res = await apiAdminMessageList(this.msgType, this.msgKeyword, this.msgPage, 12)
      this.msgLoading = false
      if (res.code !== 200) { this.toast(res.msg || '加载失败', 'err'); return }
      const d = res.data || {}
      this.messageList = d.list || []
      this.msgPages = d.pages || 1
      this.msgTotal = d.total || 0
    },
    async delMessage(m) {
      if (!confirm('确定删除这条消息？')) return
      const res = await apiAdminMessageDelete(m.id)
      if (res.code !== 200) { this.toast(res.msg || '删除失败', 'err'); return }
      this.toast(res.msg || '已删除', 'ok')
      this.loadMessage(this.msgPage)
    },
    openSend() {
      this.sendForm = { show: true, sending: false, toName: '', content: '' }
    },
    async sendNotify() {
      const content = (this.sendForm.content || '').trim()
      if (!content) { this.toast('通知内容不能为空', 'err'); return }
      this.sendForm.sending = true
      const res = await apiAdminMessageSend((this.sendForm.toName || '').trim(), content)
      this.sendForm.sending = false
      if (res.code !== 200) { this.toast(res.msg || '发送失败', 'err'); return }
      this.toast(res.msg || '已发送', 'ok')
      this.sendForm.show = false
      this.msgType = 'NOTIFY'
      this.loadMessage(1)
    }
  }
}
</script>

<style scoped>
.admin { min-height: 100vh; background: #f7f3fb; color: #4a3f5c; display: flex; flex-direction: column; }
.topbar {
  height: 56px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 20px; background: #fff; box-shadow: 0 2px 12px rgba(160,108,213,.08);
}
.brand { font-weight: 800; font-size: 16px; color: #7c5cbf; }
.right { display: flex; align-items: center; gap: 14px; }
.who { font-size: 13px; color: #9088a0; }
.logout { border: none; background: #efe9fb; color: #7c5cbf; font-weight: 700; padding: 7px 14px; border-radius: 10px; cursor: pointer; }
.body { flex: 1; display: flex; min-height: 0; }
.sidebar {
  width: 180px; padding: 16px 10px; background: #fff; border-right: 1px solid #efe9f5;
  display: flex; flex-direction: column; gap: 6px;
}
.menu {
  text-align: left; border: none; background: transparent; padding: 11px 14px; border-radius: 10px;
  font-size: 14px; font-weight: 700; color: #6b6380; cursor: pointer; transition: .15s;
}
.menu:hover { background: #f6f1fb; }
.menu.on { background: linear-gradient(135deg, #a06cd5, #c89bf0); color: #fff; }
.content { flex: 1; padding: 22px 26px; overflow: auto; }

/* 权限徽章 */
.tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 800; }
.tag-admin { background: #efe6fb; color: #7c5cbf; }
.tag-user { background: #f1eef7; color: #8a7fa3; }
.tag-super { background: #fff3d6; color: #c08a00; }
.title { margin: 0 0 4px; font-size: 20px; }
.desc { margin: 0 0 16px; color: #9088a0; font-size: 13px; }

.stat-grid { display: flex; gap: 14px; flex-wrap: wrap; }
.stat { background: #fff; border-radius: 14px; padding: 18px 22px; min-width: 120px; box-shadow: 0 6px 18px rgba(160,108,213,.08); }
.stat b { display: block; font-size: 26px; color: #7c5cbf; }
.stat span { font-size: 12.5px; color: #9088a0; }
.stat.clickable { border: none; cursor: pointer; text-align: left; font-family: inherit; transition: .15s; }
.stat.clickable:hover { transform: translateY(-2px); box-shadow: 0 10px 24px rgba(160,108,213,.16); }
.quick { margin-top: 18px; display: flex; gap: 10px; }
.q { border: 1.5px solid #e0d4f5; background: #fff; color: #7c5cbf; font-weight: 700; padding: 9px 14px; border-radius: 10px; cursor: pointer; }

/* 概览四块小面板 */
.panel-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; margin-top: 16px; }
.panel { background: #fff; border-radius: 14px; padding: 16px 18px; box-shadow: 0 6px 18px rgba(160,108,213,.06); }
.panel-head { font-size: 14px; font-weight: 800; color: #5a4b78; margin-bottom: 10px; }
.panel-empty { color: #b3aac6; font-size: 13px; padding: 6px 0; }
.panel-more { border: none; background: transparent; color: #7c5cbf; font-weight: 700; font-size: 12.5px; cursor: pointer; padding: 8px 0 0; }
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip { display: inline-flex; align-items: center; gap: 6px; background: #f6f1fb; color: #6b6380; border-radius: 999px; padding: 5px 12px; font-size: 12.5px; font-weight: 700; }
.chip em { font-style: normal; font-size: 11px; color: #a59cba; }
.chip.top { background: linear-gradient(135deg, #ffe9f3, #fdf3d6); color: #d6589b; }
.chip.top em { color: #e0953f; }
.rank { display: flex; flex-direction: column; gap: 8px; }
.rank-item { display: flex; align-items: center; gap: 8px; font-size: 13px; min-width: 0; }
.rank-no { width: 20px; height: 20px; border-radius: 50%; background: #efe9fb; color: #7c5cbf; font-size: 11px; font-weight: 800; display: flex; align-items: center; justify-content: center; flex: none; }
.rank-no.no-1 { background: #ffd9e8; color: #d6589b; }
.rank-no.no-2 { background: #fde9c8; color: #c08a00; }
.rank-no.no-3 { background: #e3ecfb; color: #3f8fd6; }
.rank-emoji { flex: none; }
.rank-name { font-weight: 700; color: #5a4b78; }
.rank-perm { font-size: 11px; font-weight: 800; padding: 1px 8px; border-radius: 999px; flex: none; }
.rank-item .muted { margin-left: auto; font-size: 12px; flex: none; }
.rank-msg { color: #6b6380; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; }
.tip { margin: -4px 0 0; font-size: 12px; color: #a59cba; }

.bar { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.spacer { flex: 1; }
.search { padding: 8px 12px; border: 1.5px solid #e4dcf5; border-radius: 10px; font-size: 13px; outline: none; background: #fff; }
.search:focus { border-color: #a06cd5; }
.search.small { padding: 6px 9px; font-size: 12px; }
.btn { border: 1.5px solid #e0d4f5; background: #fff; color: #6b6380; font-weight: 700; padding: 8px 14px; border-radius: 10px; cursor: pointer; }
.btn:hover { background: #f6f1fb; }
.btn.primary { background: linear-gradient(135deg, #a06cd5, #c89bf0); color: #fff; border: none; }
.btn:disabled { opacity: .6; cursor: default; }

.tbl { width: 100%; border-collapse: collapse; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 6px 18px rgba(160,108,213,.06); }
.tbl th, .tbl td { padding: 11px 12px; text-align: left; font-size: 13px; border-bottom: 1px solid #f1ecf8; }
.tbl th { background: #faf7fe; color: #8a7fa8; font-weight: 700; }
.tbl tr:last-child td { border-bottom: none; }
.muted { color: #a59cba; }
.empty { text-align: center; color: #b3aac6; padding: 24px; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.rarity { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 999px; }
.rr-common { background: #eef0f5; color: #7c7689; }
.rr-rare { background: #e6f0fb; color: #3f8fd6; }
.rr-legend { background: #fbe9f3; color: #d6589b; }
.pill { font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 999px; background: #efe9fb; color: #7c5cbf; }
.msg { max-width: 320px; }
.link { border: none; background: transparent; color: #7c5cbf; font-weight: 700; cursor: pointer; font-size: 13px; padding: 2px 6px; }
.link.danger { color: #e06666; }
.pager { display: flex; align-items: center; gap: 12px; justify-content: center; margin-top: 14px; font-size: 13px; color: #9088a0; }
.inline-form { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }

.loading { padding: 30px; text-align: center; color: #9088a0; }

/* 弹窗 */
.mask { position: fixed; inset: 0; background: rgba(60,40,80,.4); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal { width: 480px; max-width: 92vw; max-height: 88vh; overflow: auto; background: #fff; border-radius: 16px; padding: 20px 22px; box-shadow: 0 20px 50px rgba(0,0,0,.25); }
.modal h3 { margin: 0 0 14px; color: #5a4b78; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 14px; }
.modal label { display: block; font-size: 12.5px; font-weight: 700; color: #6b6380; margin-bottom: 12px; }
.modal label.full { grid-column: 1 / -1; }
.modal input, .modal select, .modal textarea {
  width: 100%; box-sizing: border-box; margin-top: 5px; padding: 9px 11px;
  border: 1.5px solid #e4dcf5; border-radius: 10px; font-size: 13px; outline: none; font-family: inherit;
}
.modal input:focus, .modal select:focus, .modal textarea:focus { border-color: #a06cd5; }
.checks { margin-top: 6px; display: grid; grid-template-columns: 1fr 1fr; gap: 4px 12px; max-height: 160px; overflow: auto; }
.chk { display: flex; align-items: center; gap: 6px; font-weight: 400; font-size: 13px; margin: 0; }
.lbl { font-size: 12.5px; font-weight: 700; color: #6b6380; }
.modal-bar { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
</style>
