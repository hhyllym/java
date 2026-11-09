<template>
  <div class="dash">
    <!-- 返回主页 -->
    <button class="dash-back" title="返回主页" @click="$router.push({ name: 'home' })">← 返回主页</button>

    <!-- 头部：头像 + 名字 + 积分 -->
    <div class="dash-head">
      <button class="dash-avatar" title="点我换头像" @click="avatarPicker = !avatarPicker">
        <img v-if="isImage(avatarDisplay)" :src="avatarDisplay" class="dash-avatar-img" alt="头像" />
        <span v-else class="dash-avatar-emoji">{{ avatarDisplay }}</span>
        <span class="dash-avatar-edit">✏️</span>
      </button>
      <div class="dash-head-main">
        <h2>
          <span class="dash-name">{{ profile.name || session.name || '小马' }}</span>
          <button class="dash-rename" title="改名字（支持特殊符号）" @click="startRename">✏️</button>
        </h2>
        <div class="meta">{{ lastLoginText ? '上次登录：' + lastLoginText : '欢迎回来～' }}</div>
        <div class="dash-points">💎 <b>{{ account.points }}</b> 积分</div>
      </div>
    </div>

    <!-- 头像选择器 -->
    <div v-if="avatarPicker" class="dash-avatars">
      <button v-for="e in AVATARS" :key="e" @click="pickAvatar(e)">{{ e }}</button>
      <!-- 自定义：上传图片 -->
      <label class="dash-avatar-custom" title="上传自定义图片头像">
        📷<span>上传图片</span>
        <input type="file" accept="image/*" class="hidden-file" @change="onAvatarFile" />
      </label>
      <!-- 自定义：任意 emoji -->
      <input
        v-model.trim="customEmoji"
        maxlength="16"
        class="dash-avatar-emoji-input"
        placeholder="或输入自定义 emoji"
        @keyup.enter="pickAvatar(customEmoji)"
      />
      <button v-if="customAvatar" class="dash-avatar-reset" @click="resetCustomAvatar">🦄 用默认</button>
    </div>

    <!-- 改名输入 -->
    <div v-if="renaming" class="dash-rename-box">
      <input v-model.trim="newName" maxlength="20" placeholder="新名字，特殊符号随便加～" @keyup.enter="confirmRename" />
      <button @click="confirmRename">保存</button>
      <button class="ghost" @click="renaming = false">取消</button>
    </div>

    <!-- 账号资料（后端 /api/user/profile） -->
    <div class="dash-kv">
      <div>
        <span>注册时间</span>
        <b>{{ profile.createTime || '—' }}</b>
      </div>
      <div>
        <span>加入天数</span>
        <b>{{ profile.days ? '第 ' + profile.days + ' 天' : '—' }}</b>
      </div>
    </div>

    <!-- 我的宠物：领养后会出现（主页 / 个人主页 / 聊天界面 三处都有） -->
    <div class="dash-sec">
      <h4>🐾 我的宠物</h4>
      <PetBadge />
    </div>

    <!-- 积分中心 -->
    <div class="dash-sec">
      <h4>💎 积分中心</h4>
      <div class="dash-point-actions">
        <button @click="$router.push({ name: 'earn' })">💰 赚积分</button>
        <button @click="$router.push({ name: 'shop' })">🧧 积分商店</button>
        <button @click="showRank = !showRank">🏆 排行榜</button>
        <button @click="toggleLogs">📜 积分流水</button>
      </div>
      <div v-if="showRank" class="dash-rank">
        <div v-for="r in rank" :key="r.id" class="dash-rank-row" :class="{ me: r.id === session.id }">
          <span class="dash-rank-no">{{ r.rank }}</span>
          <span class="dash-rank-face">{{ r.avatar || '🦄' }}</span>
          <b>{{ r.name }}</b>
          <em>{{ r.points }}</em>
        </div>
      </div>
      <div v-if="logsOpen" class="dash-logs">
        <div v-for="(l, i) in logs" :key="i" class="dash-log-row">
          <span :class="l.amount > 0 ? 'up' : 'down'">{{ l.amount > 0 ? '+' + l.amount : l.amount }}</span>
          <em>{{ l.remark }}</em>
        </div>
        <div v-if="!logs.length" class="dash-tip">还没有积分记录，做任务赚第一笔吧</div>
      </div>
    </div>

    <!-- 每日任务 -->
    <div class="dash-sec">
      <h4>📋 每日任务</h4>
      <div v-if="!tasks.length" class="dash-tip">任务加载中…</div>
      <div v-else class="task-grid">
        <div v-for="t in tasks" :key="t.id" class="task-row">
          <div class="task-main">
            <b>{{ t.name }} <em>+{{ t.points }}</em></b>
            <span>{{ t.desc }}</span>
            <div class="task-bar"><i :style="{ width: (t.progress / t.target * 100) + '%' }"></i></div>
          </div>
          <button
            class="task-claim"
            :disabled="!t.done || t.claimed"
            @click="claimTask(t)"
          >{{ t.claimed ? '已领' : t.done ? '领取' : t.progress + '/' + t.target }}</button>
        </div>
      </div>
    </div>

    <!-- 同名小马：图鉴里有同名的就亮出来 -->
    <div class="dash-sec">
      <h4>你的同名小马</h4>

      <div v-if="loadingPony" class="dash-tip">正在翻图鉴…</div>

      <div v-else-if="profile.pony" class="dash-pony" @click="openPony">
        <span class="dash-face" :style="{ background: profile.pony.color || '#f3e8ff' }">
          <CutieMark v-if="hasMark(profile.pony.mark)" :mark="profile.pony.mark" />
          <span v-else class="dash-emoji">{{ profile.pony.emoji || '🐴' }}</span>
        </span>
        <div class="dash-pony-main">
          <p>
            <b>{{ profile.pony.name }}</b>
            <span class="rarity inline" :class="rarityClass(profile.pony.rarity)">{{ profile.pony.rarity }}</span>
          </p>
          <p class="dash-alias">{{ profile.pony.alias || '' }}</p>
          <div class="kv">
            <button v-for="c in (profile.pony.categories || [])" :key="c.id" @click.stop="goCategory(c)">
              {{ c.parentName ? c.parentName + ' · ' : '' }}{{ c.name }}
            </button>
          </div>
        </div>
      </div>

      <div v-else-if="apiError" class="dash-tip err">{{ apiError }}</div>

      <div v-else class="dash-tip">
        图鉴里还没有同名的你 🐴<br />
        <span>去首页逛逛，或者换个更像你的名字～</span>
      </div>
    </div>

    <div class="dash-actions">
      <button @click="$router.push({ name: 'home' })">去图鉴逛逛</button>
      <button class="switch" @click="switchAccount">🔁 切换账号</button>
      <button @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script>
import { getSession, clearSession, rememberAccount } from '../auth'
import { apiPresenceOffline } from '../presence'
import { apiProfile } from '../user'
import { apiAccountProfile, apiAccountUpdate } from '../account'
import { apiPointBalance, apiPointLogs, apiPointRank } from '../point'
import { apiTaskToday, apiTaskClaim } from '../task'
import { bump } from '../quest'
import sound from '../sound'
import CutieMark from '../components/CutieMark.vue'
import PetBadge from '../components/PetBadge.vue'

const MARKS = ['pinkie', 'twilight', 'dash', 'applejack', 'fluttershy', 'rarity']
const RARITY_CLASS = { 常见: 'rr-common', 稀有: 'rr-rare', 传说: 'rr-legend' }
const AVATARS = ['🦄', '🐲', '🌙', '☀️', '🎀', '💎', '⚡', '🍎', '🦋', '🎉', '📚', '⭐', '🐱', '🐦', '🧁', '👑']

export default {
  name: 'DashboardView',
  components: { CutieMark, PetBadge },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      loadingPony: true,
      apiError: '',
      profile: { id: null, name: '', createTime: '', days: 0, pony: null },

      // 账号 / 积分 / 任务
      account: { name: '', avatar: '', points: 0 },
      tasks: [],
      rank: [],
      logs: [],
      logsOpen: false,
      showRank: false,

      // 头像与改名交互
      avatarPicker: false,
      renaming: false,
      newName: '',
      // 自定义头像（本地图片，存 localStorage；emoji 走后端）
      customEmoji: '',
      customAvatar: '',

      AVATARS
    }
  },
  computed: {
    lastLoginText() {
      if (!this.session.ts) return ''
      return new Date(this.session.ts).toLocaleString('zh-CN')
    },
    avatarDisplay() {
      return this.customAvatar || this.account.avatar || '🦄'
    }
  },
  created() {
    this.loadProfile()
    this.loadAccount()
    this.loadRank()
    // 读回本地保存的自定义图片头像
    const saved = localStorage.getItem('mlp_avatar_' + this.session.id)
    if (saved) this.customAvatar = saved
    // 每日登录任务顺手达成；上报完再拉列表，避免竞态看不到进度
    bump('login').then(() => this.loadTasks())
  },
  methods: {
    hasMark(mark) {
      return MARKS.indexOf(mark) >= 0
    },
    rarityClass(rarity) {
      return RARITY_CLASS[rarity] || 'rr-common'
    },
    async loadProfile() {
      const id = this.session.id
      if (!id) {
        this.loadingPony = false
        this.apiError = '本地没有会话信息，重新登录一下'
        return
      }
      const r = await apiProfile(id)
      this.loadingPony = false
      if (r.code !== 200) {
        this.apiError = r.msg || '账号资料获取失败'
        return
      }
      this.profile = r.data || this.profile
    },
    async loadAccount() {
      const r = await apiAccountProfile(this.session.id)
      if (r.code === 200 && r.data) {
        this.account = r.data
      }
    },
    async loadTasks() {
      const r = await apiTaskToday(this.session.id)
      if (r.code === 200) this.tasks = r.data || []
    },
    async loadRank() {
      const r = await apiPointRank()
      if (r.code === 200) this.rank = r.data || []
    },
    async toggleLogs() {
      this.logsOpen = !this.logsOpen
      if (this.logsOpen && !this.logs.length) {
        const r = await apiPointLogs(this.session.id)
        if (r.code === 200) this.logs = r.data || []
      }
    },
    async pickAvatar(emoji) {
      if (!emoji) return
      sound.pop()
      // 选了 emoji：清掉本地自定义图片，统一用后端头像
      if (this.customAvatar) {
        localStorage.removeItem('mlp_avatar_' + this.session.id)
        this.customAvatar = ''
      }
      const r = await apiAccountUpdate(this.session.id, { avatar: emoji })
      if (r.code === 200) {
        this.account.avatar = emoji
        this.avatarPicker = false
        this.toast('头像换好啦 ' + emoji, 'ok')
      } else {
        this.toast(r.msg || '换头像失败', 'err')
      }
    },
    /** 是否图片型头像（data: 或 http(s): 走 <img>；其余当 emoji 文本） */
    isImage(v) {
      return typeof v === 'string' && (v.startsWith('data:') || v.startsWith('http'))
    },
    /** 上传本地图片当作头像：读图 → 居中裁成 160×160 → 压成 PNG dataURL → 存本地 */
    async onAvatarFile(e) {
      const file = e.target.files && e.target.files[0]
      if (!file) return
      const dataUrl = await this.compressImage(file, 160)
      if (!dataUrl) {
        this.toast('图片读取出错啦', 'err')
        return
      }
      localStorage.setItem('mlp_avatar_' + this.session.id, dataUrl)
      this.customAvatar = dataUrl
      this.avatarPicker = false
      this.toast('头像换好啦 🖼', 'ok')
    },
    compressImage(file, size) {
      return new Promise((resolve) => {
        const reader = new FileReader()
        reader.onload = () => {
          const img = new Image()
          img.onload = () => {
            const c = document.createElement('canvas')
            c.width = size
            c.height = size
            const ctx = c.getContext('2d')
            const min = Math.min(img.width, img.height)
            const sx = (img.width - min) / 2
            const sy = (img.height - min) / 2
            ctx.drawImage(img, sx, sy, min, min, 0, 0, size, size)
            resolve(c.toDataURL('image/png'))
          }
          img.onerror = () => resolve(null)
          img.src = reader.result
        }
        reader.onerror = () => resolve(null)
        reader.readAsDataURL(file)
      })
    },
    /** 清除本地自定义图片，回到 emoji 默认 */
    resetCustomAvatar() {
      localStorage.removeItem('mlp_avatar_' + this.session.id)
      this.customAvatar = ''
      this.toast('已切换回默认头像 🦄', 'ok')
    },
    startRename() {
      this.newName = this.profile.name || this.account.name || ''
      this.renaming = true
    },
    async confirmRename() {
      const name = this.newName
      if (!name) {
        this.toast('名字不能是空白的', 'err')
        return
      }
      const r = await apiAccountUpdate(this.session.id, { name })
      if (r.code === 200) {
        this.account.name = name
        this.profile.name = name
        this.renaming = false
        sound.magic()
        this.toast('改名成功，以后就叫「' + name + '」啦', 'ok')
      } else {
        this.toast(r.msg || '改名失败', 'err')
      }
    },
    async claimTask(t) {
      const r = await apiTaskClaim(this.session.id, t.id)
      if (r.code === 200) {
        sound.success()
        this.toast(r.data, 'ok')
        this.loadTasks()
        this.loadAccount()
      } else {
        this.toast(r.msg || '领奖失败', 'err')
      }
    },
    openPony() {
      this.$router.push({ name: 'home', query: { pony: this.profile.pony.id } })
    },
    goCategory(c) {
      this.$router.push({ name: 'home', query: { category: c.id } })
    },
    handleLogout() {
      apiPresenceOffline(this.session.id)
      clearSession()
      this.toast('已退出登录，拜拜 👋', 'ok')
      this.$router.push({ name: 'login' })
    },
    /** 切换账号：记住当前账号名 → 清会话 → 回登录页（会预填上次账号，改密码即可登另一个） */
    switchAccount() {
      const me = this.profile.name || this.session.name
      if (!confirm(`要切换账号吗？\n当前账号「${me || ''}」将退出登录，聊天和积分数据都会保留。`)) return
      rememberAccount(me)
      apiPresenceOffline(this.session.id)
      clearSession()
      this.toast('已退出，请登录另一个账号 🔁', 'ok')
      this.$router.push({ name: 'login', query: { switch: '1' } })
    }
  }
}
</script>

<style scoped>
/* 返回按钮悬浮在左上角，不占布局空间，头像位置保持原样 */
.dash { position: relative; }
.dash-back {
  position: absolute; top: 8px; left: 14px;
  border: none; cursor: pointer;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px; font-weight: 700;
  color: var(--primary); background: var(--panel);
  transition: background .2s, transform .2s;
}
.dash-back:hover { background: var(--track); transform: translateX(-2px); }
.dash-head {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 0 6px;
}
.dash-avatar {
  position: relative; width: 64px; height: 64px; flex-shrink: 0;
  border: none; cursor: pointer; border-radius: 20px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  font-size: 34px; line-height: 1;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 16px rgba(160, 108, 213, .3);
  transition: transform .15s;
}
.dash-avatar:hover { transform: scale(1.06) rotate(-3deg); }
.dash-avatar-emoji { font-size: 34px; line-height: 1; }
.dash-avatar-img {
  width: 100%; height: 100%; object-fit: cover;
  border-radius: 16px; display: block;
}
.dash-avatar-edit {
  position: absolute; right: -4px; bottom: -4px;
  width: 22px; height: 22px; border-radius: 50%;
  background: var(--card); border: 1px solid var(--border);
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.dash-head-main { flex: 1; min-width: 0; }
.dash-head-main h2 {
  margin: 0; font-size: 19px;
  display: flex; align-items: center; gap: 8px;
}
.dash-name {
  flex: 1 1 auto; min-width: 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.dash-rename {
  border: none; background: none; cursor: pointer;
  /* 全局 .dash button 有 width:100%，会把小铅笔拉满整行挤掉名字，这里必须豁免 */
  width: auto; flex-shrink: 0;
  font-size: 12px; padding: 2px 6px; border-radius: 6px;
  opacity: .6; transition: opacity .15s;
}
.dash-rename:hover { opacity: 1; background: var(--panel); }
.dash-points { font-size: 13px; color: var(--muted); margin-top: 3px; }
.dash-points b { color: var(--primary); font-size: 16px; }

/* 头像选择器 */
.dash-avatars {
  display: grid; grid-template-columns: repeat(8, 1fr); gap: 6px;
  padding: 10px; margin-top: 8px;
  background: var(--panel); border-radius: 12px;
}
.dash-avatars button {
  border: 1px solid var(--border); border-radius: 10px;
  background: var(--card); font-size: 20px; padding: 6px 0;
  cursor: pointer; transition: all .15s;
}
.dash-avatars button:hover { border-color: var(--primary); transform: scale(1.15); }
/* 自定义上传 / emoji 输入 / 恢复默认 */
.dash-avatar-custom {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  border: 1px dashed var(--primary); border-radius: 10px;
  background: var(--card); color: var(--primary);
  font-size: 18px; padding: 6px 0 4px; cursor: pointer; transition: all .15s;
}
.dash-avatar-custom:hover { background: var(--panel); transform: scale(1.05); }
.dash-avatar-custom span { font-size: 9px; color: var(--muted); }
.hidden-file { display: none; }
.dash-avatar-emoji-input {
  grid-column: 1 / -1; border: 1.5px solid var(--border);
  border-radius: 10px; padding: 7px 10px; font-size: 13px;
  background: var(--card); color: var(--text); outline: none;
}
.dash-avatar-emoji-input:focus { border-color: var(--primary); }
.dash-avatar-reset {
  grid-column: 1 / -1; border: none; cursor: pointer;
  border-radius: 10px; padding: 7px 0; font-size: 12px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}

/* 改名框 */
.dash-rename-box {
  display: flex; gap: 8px; margin-top: 10px;
}
.dash-rename-box input {
  flex: 1; min-width: 0; border: 1.5px solid var(--border);
  border-radius: 10px; padding: 8px 12px; font-size: 13px;
  background: var(--card); color: var(--text); outline: none;
}
.dash-rename-box input:focus { border-color: var(--primary); }
.dash-rename-box button {
  border: none; cursor: pointer; border-radius: 10px;
  padding: 8px 16px; font-size: 12.5px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.dash-rename-box button.ghost { background: var(--panel); color: var(--muted); }

/* 积分中心 */
.dash-point-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.dash-point-actions button {
  border: 1.5px solid var(--border); cursor: pointer;
  background: var(--card); color: var(--text);
  border-radius: 999px; padding: 7px 14px; font-size: 12.5px; font-weight: 700;
  transition: all .15s;
}
.dash-point-actions button:hover { border-color: var(--primary); color: var(--primary); }
.dash-rank { display: flex; flex-direction: column; gap: 4px; }
.dash-rank-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; border-radius: 9px;
  background: var(--panel); font-size: 12.5px;
}
.dash-rank-row.me { outline: 1.5px solid var(--primary); }
.dash-rank-no { width: 20px; text-align: center; font-weight: 800; color: var(--muted); }
.dash-rank-face { font-size: 16px; }
.dash-rank-row b { flex: 1; min-width: 0; }
.dash-rank-row em { font-style: normal; font-weight: 800; color: var(--primary); }
.dash-logs { display: flex; flex-direction: column; gap: 4px; }
.dash-log-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; border-radius: 9px;
  background: var(--panel); font-size: 12px;
}
.dash-log-row .up { color: var(--success); font-weight: 800; }
.dash-log-row .down { color: var(--danger); font-weight: 800; }
.dash-log-row em { font-style: normal; color: var(--muted); }

/* 每日任务：一列排布 */
.task-grid {
  display: grid; grid-template-columns: 1fr; gap: 8px;
}
.task-row {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 10px;
  border-radius: 11px; background: var(--panel);
  min-width: 0;
}
.task-main { flex: 1; min-width: 0; }
.task-main b { font-size: 13px; display: flex; align-items: center; gap: 6px; }
.task-main b em { font-style: normal; font-size: 11px; color: var(--primary); }
.task-main span { font-size: 11px; color: var(--muted); display: block; margin: 1px 0 4px; }
.task-bar { height: 5px; border-radius: 99px; background: var(--track); overflow: hidden; }
.task-bar i { display: block; height: 100%; border-radius: 99px; background: linear-gradient(90deg, var(--pink), var(--purple)); transition: width .3s; }
.task-claim {
  border: none; cursor: pointer; flex-shrink: 0;
  width: auto;
  border-radius: 999px; padding: 6px 13px;
  font-size: 11.5px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.task-claim:disabled { background: var(--track); color: var(--muted); cursor: not-allowed; }

</style>
