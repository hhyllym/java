<template>
  <div class="dash">
    <!-- 头部：头像 + 名字 + 积分 -->
    <div class="dash-head">
      <button class="dash-avatar" title="点我换头像" @click="avatarPicker = !avatarPicker">
        {{ account.avatar || '🦄' }}
        <span class="dash-avatar-edit">✏️</span>
      </button>
      <div class="dash-head-main">
        <h2>
          {{ profile.name || session.name || '小马' }}
          <button class="dash-rename" title="改名字（支持特殊符号）" @click="startRename">✏️</button>
        </h2>
        <div class="meta">{{ lastLoginText ? '上次登录：' + lastLoginText : '欢迎回来～' }}</div>
        <div class="dash-points">💎 <b>{{ account.points }}</b> 积分</div>
      </div>
    </div>

    <!-- 头像选择器 -->
    <div v-if="avatarPicker" class="dash-avatars">
      <button v-for="e in AVATARS" :key="e" @click="pickAvatar(e)">{{ e }}</button>
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

    <!-- 积分中心 -->
    <div class="dash-sec">
      <h4>💎 积分中心</h4>
      <div class="dash-point-actions">
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

    <!-- 宠物 -->
    <div class="dash-sec">
      <h4>🐾 我的宠物</h4>
      <div v-if="petLoading" class="dash-tip">看看宠物窝里有没有小家伙…</div>
      <div v-else-if="pet" class="pet-card">
        <div class="pet-face" :class="{ hungry: pet.hunger < 30 }">
          {{ pet.kindInfo.emoji }}
          <span v-if="pet.cosmetic" class="pet-cosmetic" :title="pet.cosmetic.name">{{ pet.cosmetic.icon }}</span>
        </div>
        <div class="pet-main">
          <p><b>{{ pet.name }}</b> <span class="pet-lv">Lv.{{ pet.level }}</span></p>
          <div class="pet-stats">
            <span>🍖 饱食 {{ pet.hunger }}</span>
            <span>💖 心情 {{ pet.mood }}</span>
            <span>⭐ 经验 {{ pet.exp }}/{{ pet.level * 50 }}</span>
            <span v-if="pet.cosmetic">🎀 {{ pet.cosmetic.name }}</span>
          </div>
          <div class="pet-actions">
            <button @click="feedPet">🍖 喂食</button>
            <button @click="playPet">🎾 玩耍</button>
            <button @click="renamePet">✏️ 改名</button>
            <button @click="openWardrobe">🎀 衣橱</button>
          </div>
        </div>
      </div>
      <div v-else class="dash-tip">
        还没有宠物 🥚<br />
        <span>领养一只小家伙，或去商店兑换宠物蛋</span>
        <div class="pet-adopt-actions">
          <button class="dash-goto-adopt" @click="openAdopt">🥚 去领养</button>
          <button class="dash-goto-shop" @click="$router.push({ name: 'shop' })">去商店看看</button>
        </div>
      </div>
    </div>

    <!-- 领养中心：选种类 + 起名（手动领养入口） -->
    <Teleport to="body">
      <div v-if="adoptOpen" class="sheet-mask" @click.self="adoptOpen = false">
        <div class="sheet adopt-sheet">
          <h3>🥚 领养中心</h3>
          <p class="sheet-tip">选一只喜欢的小家伙，给它起个名字吧</p>
          <div class="adopt-kinds">
            <button
              v-for="k in PET_KINDS"
              :key="k.kind"
              :class="{ on: adoptKind === k.kind }"
              @click="adoptKind = k.kind"
            >
              <span class="adopt-face">{{ k.emoji }}</span>
              <b>{{ k.name }}</b>
              <em>{{ k.desc }}</em>
            </button>
          </div>
          <input v-model.trim="adoptName" maxlength="20" placeholder="起个名字（特殊符号也行）" @keyup.enter="confirmAdopt" />
          <div class="sheet-btns">
            <button class="primary" :disabled="adopting" @click="confirmAdopt">
              {{ adopting ? '领养中…' : '领养！' }}
            </button>
            <button class="ghost" @click="adoptOpen = false">再想想</button>
          </div>
        </div>
      </div>

      <!-- 衣橱：给宠物换装扮（积分兑换来的） -->
      <div v-if="wardrobeOpen" class="sheet-mask" @click.self="wardrobeOpen = false">
        <div class="sheet wardrobe-sheet">
          <h3>🎀 宠物衣橱</h3>
          <p class="sheet-tip">点一下就换上，再点一次卸下（只能戴一件）</p>
          <div class="wardrobe-grid">
            <button
              v-for="c in cosmetics"
              :key="c.id"
              :class="{ on: c.equipped }"
              @click="toggleCosmetic(c)"
            >
              <span class="wardrobe-face">{{ c.icon }}</span>
              <b>{{ c.name }}</b>
              <em v-if="c.equipped">装备中</em>
              <em v-else-if="c.count > 1">x{{ c.count }}</em>
            </button>
            <div v-if="!cosmetics.length" class="sheet-tip">
              衣橱还空着，去商店「宠物装扮」区兑换几件吧
            </div>
          </div>
          <div class="sheet-btns">
            <button class="ghost" @click="wardrobeOpen = false">搞定</button>
          </div>
        </div>
      </div>
    </Teleport>

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
import { apiTaskToday, apiTaskClaim, apiTaskBump } from '../task'
import { apiPetMine, apiPetFeed, apiPetPlay, apiPetRename, apiPetAdopt, apiPetCosmetics, apiPetCosmeticEquip, apiPetCosmeticUnequip } from '../pet'
import sound from '../sound'
import CutieMark from '../components/CutieMark.vue'

const MARKS = ['pinkie', 'twilight', 'dash', 'applejack', 'fluttershy', 'rarity']
const RARITY_CLASS = { 常见: 'rr-common', 稀有: 'rr-rare', 传说: 'rr-legend' }
const AVATARS = ['🦄', '🐲', '🌙', '☀️', '🎀', '💎', '⚡', '🍎', '🦋', '🎉', '📚', '⭐', '🐱', '🐦', '🧁', '👑']
/** 领养中心的宠物种类（与后端 ShopDefs 一致） */
const PET_KINDS = [
  { kind: 'dragon', name: '小火龙', emoji: '🐲', desc: '喷小火苗的龙崽' },
  { kind: 'cat', name: '奶糖猫', emoji: '🐱', desc: '又甜又软，爱打呼噜' },
  { kind: 'bird', name: '云雀', emoji: '🐦', desc: '嗓门很小，唱歌跑调' }
]

export default {
  name: 'DashboardView',
  components: { CutieMark },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      loadingPony: true,
      apiError: '',
      profile: { id: null, name: '', createTime: '', days: 0, pony: null },

      // 账号 / 积分 / 任务 / 宠物
      account: { name: '', avatar: '', points: 0 },
      tasks: [],
      rank: [],
      logs: [],
      logsOpen: false,
      showRank: false,
      pet: null,
      petLoading: true,

      // 头像与改名交互
      avatarPicker: false,
      renaming: false,
      newName: '',

      // 领养中心
      adoptOpen: false,
      adoptKind: 'dragon',
      adoptName: '',
      adopting: false,

      // 衣橱
      wardrobeOpen: false,
      cosmetics: [],

      PET_KINDS,
      AVATARS
    }
  },
  computed: {
    lastLoginText() {
      if (!this.session.ts) return ''
      return new Date(this.session.ts).toLocaleString('zh-CN')
    }
  },
  created() {
    this.loadProfile()
    this.loadAccount()
    this.loadTasks()
    this.loadPet()
    this.loadRank()
    // 每日登录任务顺手达成
    apiTaskBump(this.session.id, 'login')
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
    async loadPet() {
      this.petLoading = true
      const r = await apiPetMine(this.session.id)
      this.petLoading = false
      if (r.code === 200) this.pet = r.data
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
      sound.pop()
      const r = await apiAccountUpdate(this.session.id, { avatar: emoji })
      if (r.code === 200) {
        this.account.avatar = emoji
        this.avatarPicker = false
        this.toast('头像换好啦 ' + emoji, 'ok')
      } else {
        this.toast(r.msg || '换头像失败', 'err')
      }
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
    async feedPet() {
      const r = await apiPetFeed(this.session.id)
      this.afterPetAction(r)
    },
    async playPet() {
      const r = await apiPetPlay(this.session.id)
      this.afterPetAction(r)
    },
    afterPetAction(r) {
      if (r.code === 200) {
        sound.pop()
        this.toast(r.data, 'ok')
        this.loadPet()
        this.loadAccount()
      } else {
        this.toast(r.msg || '操作失败', 'err')
      }
    },
    async renamePet() {
      const name = prompt('给宠物起个新名字（特殊符号也行）：', this.pet ? this.pet.name : '')
      if (!name || !name.trim()) return
      const r = await apiPetRename(this.session.id, name.trim())
      if (r.code === 200) {
        this.toast(r.data, 'ok')
        this.loadPet()
      } else {
        this.toast(r.msg || '改名失败', 'err')
      }
    },
    /** 打开领养中心 */
    openAdopt() {
      this.adoptOpen = true
      this.adoptKind = 'dragon'
      this.adoptName = ''
    },
    /** 确认领养（手动领养入口） */
    async confirmAdopt() {
      if (this.adopting) return
      this.adopting = true
      const r = await apiPetAdopt(this.session.id, this.adoptKind, this.adoptName)
      this.adopting = false
      if (r.code === 200) {
        this.adoptOpen = false
        sound.magic()
        this.toast(r.data, 'ok')
        this.loadPet()
        this.loadAccount()
      } else {
        sound.fail()
        this.toast(r.msg || '领养失败', 'err')
      }
    },
    /** 打开衣橱 */
    async openWardrobe() {
      this.wardrobeOpen = true
      const r = await apiPetCosmetics(this.session.id)
      if (r.code === 200) this.cosmetics = r.data || []
    },
    /** 点装扮：没装备就穿上，已装备就卸下 */
    async toggleCosmetic(c) {
      const r = c.equipped
        ? await apiPetCosmeticUnequip(this.session.id)
        : await apiPetCosmeticEquip(this.session.id, c.id)
      if (r.code === 200) {
        sound.pop()
        this.toast(r.data, 'ok')
        this.openWardrobe()
        this.loadPet()
      } else {
        sound.fail()
        this.toast(r.msg || '操作失败', 'err')
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
.dash-rename {
  border: none; background: none; cursor: pointer;
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

/* 每日任务：两个一排 */
.task-grid {
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px;
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
  border-radius: 999px; padding: 6px 13px;
  font-size: 11.5px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.task-claim:disabled { background: var(--track); color: var(--muted); cursor: not-allowed; }

/* 宠物 */
.pet-card {
  display: flex; gap: 12px; padding: 12px;
  border-radius: 14px; background: var(--panel);
}
.pet-face {
  width: 62px; height: 62px; flex-shrink: 0;
  border-radius: 16px; background: var(--card);
  display: flex; align-items: center; justify-content: center;
  font-size: 34px;
  animation: petBounce 2.6s ease-in-out infinite;
}
.pet-face.hungry { filter: grayscale(.6); }
@keyframes petBounce {
  0%, 100% { transform: translateY(0) rotate(-2deg); }
  50% { transform: translateY(-6px) rotate(2deg); }
}
.pet-main { flex: 1; min-width: 0; }
.pet-main p { margin: 0 0 5px; font-size: 14.5px; }
.pet-lv {
  font-size: 10.5px; color: #fff; margin-left: 6px;
  background: var(--primary); border-radius: 999px; padding: 1px 8px;
}
.pet-stats { display: flex; flex-wrap: wrap; gap: 8px; font-size: 11.5px; color: var(--muted); margin-bottom: 8px; }
.pet-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.pet-actions button {
  border: 1.5px solid var(--border); cursor: pointer;
  background: var(--card); color: var(--text);
  border-radius: 999px; padding: 5px 12px; font-size: 11.5px; font-weight: 700;
  transition: all .15s;
}
.pet-actions button:hover { border-color: var(--primary); color: var(--primary); }
.dash-goto-shop {
  display: block; margin: 10px auto 0;
  border: none; cursor: pointer; border-radius: 999px;
  padding: 8px 18px; font-size: 12.5px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.pet-adopt-actions { display: flex; gap: 8px; justify-content: center; margin-top: 10px; }
.dash-goto-adopt {
  display: block; border: none; cursor: pointer; border-radius: 999px;
  padding: 8px 18px; font-size: 12.5px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.pet-adopt-actions .dash-goto-shop { margin-top: 0; }

/* 宠物头顶装扮角标 */
.pet-cosmetic {
  position: absolute; top: -10px; right: -8px;
  font-size: 20px;
  filter: drop-shadow(0 2px 3px rgba(160, 108, 213, .4));
  animation: petBounce 2s ease-in-out infinite;
}
.pet-face { position: relative; }

/* 领养中心 */
.adopt-kinds { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.adopt-kinds button {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: 1.5px solid var(--border); border-radius: 14px;
  background: var(--panel); color: var(--text);
  padding: 12px 6px; cursor: pointer; transition: all .15s;
}
.adopt-kinds button:hover { border-color: var(--primary); }
.adopt-kinds button.on {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}
.adopt-face { font-size: 32px; }
.adopt-kinds b { font-size: 12.5px; }
.adopt-kinds em {
  font-style: normal; font-size: 10px; color: var(--muted);
  text-align: center; line-height: 1.4;
}
.adopt-kinds button.on em { color: rgba(255, 255, 255, .85); }

/* 衣橱 */
.wardrobe-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.wardrobe-grid button {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: 1.5px solid var(--border); border-radius: 14px;
  background: var(--panel); color: var(--text);
  padding: 10px 4px; cursor: pointer; transition: all .15s;
}
.wardrobe-grid button:hover { border-color: var(--primary); transform: translateY(-2px); }
.wardrobe-grid button.on {
  border-color: var(--primary);
  outline: 2px solid var(--primary);
  outline-offset: -1px;
}
.wardrobe-face { font-size: 26px; }
.wardrobe-grid b { font-size: 11px; }
.wardrobe-grid em { font-style: normal; font-size: 10px; color: var(--primary); font-weight: 700; }
.wardrobe-grid button.on b { color: var(--primary); }

/* 通用弹层（领养/衣橱复用） */
.sheet-mask {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(60, 30, 90, .35);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.sheet {
  width: 100%; max-width: var(--app-w); max-height: var(--app-h); overflow-y: auto;
  background: var(--card); border-radius: 20px;
  padding: 20px; border: 1.5px solid var(--border);
}
.sheet h3 { margin: 0 0 4px; font-size: 17px; }
.sheet-tip { margin: 0 0 12px; font-size: 12px; color: var(--muted); }
.sheet input {
  width: 100%; box-sizing: border-box;
  border: 1.5px solid var(--border); border-radius: 10px;
  padding: 9px 12px; font-size: 13px;
  background: var(--panel); color: var(--text); outline: none;
}
.sheet input:focus { border-color: var(--primary); }
.sheet-btns { display: flex; gap: 8px; margin-top: 14px; }
.sheet-btns button {
  flex: 1; border: none; cursor: pointer; border-radius: 10px;
  padding: 9px 0; font-size: 13px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
}
.sheet-btns button.ghost { background: var(--panel); color: var(--muted); }
.sheet-btns button:disabled { background: var(--track); color: var(--muted); cursor: not-allowed; }
</style>
