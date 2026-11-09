<template>
  <div class="earn">
    <!-- 顶部：返回 / 标题 / 余额 -->
    <div class="earn-top">
      <button class="earn-back" @click="$router.push({ name: 'dashboard' })">‹ 返回</button>
      <div class="earn-title">
        <h3>💰 赚积分</h3>
        <p>每天动一动，宝石币进兜兜 ✨</p>
      </div>
      <div class="earn-balance">💎 <b>{{ balance }}</b></div>
      <router-link to="/shop" class="earn-shop">🧧 去花</router-link>
    </div>

    <!-- 今日概览 -->
    <div class="earn-hero">
      <div class="hero-row">
        <span>今日进账</span><b class="up">+{{ todayGain }}</b>
        <span>今日任务</span><b>{{ claimedCount }} / {{ normalTasks.length }}</b>
      </div>
      <div class="hero-bar"><i :style="{ width: taskPercent + '%' }"></i></div>
      <p>全部做完，一天最多能拿 <b>{{ totalPossible }}</b> 积分（含全勤宝箱）🎉</p>
    </div>

    <!-- 全勤宝箱 -->
    <div v-if="bonus" class="earn-box" :class="{ ready: bonus.unlocked && !bonus.claimed }">
      <span class="box-ico">👑</span>
      <div class="box-main">
        <b>全勤宝箱 <em>+{{ bonus.points }}</em></b>
        <span v-if="bonus.claimed">已经开启过啦，明天再来～</span>
        <span v-else-if="bonus.unlocked">其它奖励都领完了，宝箱解锁！</span>
        <span v-else>把今天的 {{ normalTasks.length }} 个任务奖励全领完，自动解锁</span>
      </div>
      <button v-if="bonus.unlocked && !bonus.claimed" class="box-btn" :disabled="claiming === bonus.id" @click="claim(bonus)">开启</button>
      <span v-else-if="bonus.claimed" class="box-tag done">已开启 ✓</span>
      <span v-else class="box-tag">还差 {{ bonus.need }} 个</span>
    </div>

    <!-- 分类筛选 -->
    <div class="earn-tabs">
      <button v-for="g in groups" :key="g.key" :class="{ on: group === g.key }" @click="group = g.key">
        {{ g.label }}
      </button>
    </div>

    <!-- 今日任务 -->
    <div class="earn-list">
      <div v-for="t in shownTasks" :key="t.id" class="earn-task" :class="{ got: t.claimed }">
        <span class="et-ico">{{ t.icon || '⭐' }}</span>
        <div class="et-main">
          <div class="et-head"><b>{{ t.name }}</b><em>+{{ t.points }}</em></div>
          <p>{{ t.desc }}</p>
          <div class="et-bar"><i :style="{ width: barWidth(t) + '%' }"></i></div>
          <small>{{ Math.min(t.progress, t.target) }} / {{ t.target }} · {{ statusText(t) }}</small>
        </div>
        <div class="et-act">
          <button v-if="t.claimed" class="et-btn done" disabled>已领取</button>
          <button v-else-if="t.done" class="et-btn claim" :disabled="claiming === t.id" @click="claim(t)">
            {{ claiming === t.id ? '领取中' : '领取' }}
          </button>
          <router-link v-else :to="t.go || '/home'" class="et-btn go">去完成</router-link>
        </div>
      </div>
      <div v-if="!shownTasks.length" class="earn-empty">这个分类今天没有任务啦～</div>
    </div>

    <!-- 其它稳定赚法 -->
    <div class="earn-sec">
      <h4>🌈 还有这些稳定赚法</h4>
      <div class="extra-grid">
        <router-link v-for="x in extras" :key="x.name" :to="x.go" class="extra-card">
          <span>{{ x.icon }}</span>
          <b>{{ x.name }}</b>
          <em>{{ x.gain }}</em>
        </router-link>
      </div>
    </div>

    <!-- 最近进账 -->
    <div class="earn-sec">
      <h4>📜 最近进账</h4>
      <div v-for="(l, i) in recent" :key="i" class="log-row">
        <span :class="l.amount > 0 ? 'up' : 'down'">{{ l.amount > 0 ? '+' + l.amount : l.amount }}</span>
        <em>{{ l.remark }}</em>
      </div>
      <div v-if="!recent.length" class="earn-empty">还没有记录，先去领个任务奖励吧～</div>
    </div>

    <div class="earn-note">
      💡 这里的余额是<b>可用积分</b>：能在积分商店换东西、上排行榜，四人联机大富翁也用它当本金。
      玩小游戏时，<b>只要打破自己的历史最佳，超出的分数就会直接变成可用积分存进这里</b>，结算页能看到「已存入账户」。
      学院页的「学习积分」是另一套课堂表现分，两边独立统计。
    </div>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { apiPointBalance, apiPointLogs } from '../point'
import { apiTaskToday, apiTaskClaim } from '../task'
import sound from '../sound'

const GROUP_OF = {
  login: 'daily', view: 'daily', pet: 'daily',
  checkin: 'study', lesson: 'study', quiz: 'study',
  game: 'play', room: 'play',
  msg: 'social', friend: 'social', gift: 'social', shop: 'social'
}

const EXTRAS = [
  { icon: '🎮', name: '玩小游戏破纪录', gain: '得分即积分', go: '/games' },
  { icon: '🥚', name: '领养宠物', gain: '+20', go: '/pet' },
  { icon: '🍼', name: '喂 / 陪宠物', gain: '+2 / 次', go: '/pet' },
  { icon: '🎂', name: '宠物升级', gain: '+20', go: '/pet' },
  { icon: '💞', name: '绑定好友关系', gain: '双方 +50', go: '/friends' },
  { icon: '🧧', name: '收到积分红包', gain: '+50', go: '/friends' },
  { icon: '🎲', name: '联机大富翁夺冠', gain: '最多 +45', go: '/game/monopoly' }
]

export default {
  name: 'EarnView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      balance: 0,
      tasks: [],
      logs: [],
      group: 'all',
      claiming: '',
      groups: [
        { key: 'all', label: '全部' },
        { key: 'daily', label: '🌅 日常' },
        { key: 'study', label: '📖 学习' },
        { key: 'play', label: '🎮 玩乐' },
        { key: 'social', label: '🤝 社交' }
      ],
      extras: EXTRAS
    }
  },
  computed: {
    normalTasks() { return this.tasks.filter(t => t.type !== 'bonus') },
    bonus() { return this.tasks.find(t => t.type === 'bonus') || null },
    shownTasks() {
      if (this.group === 'all') return this.normalTasks
      return this.normalTasks.filter(t => GROUP_OF[t.type] === this.group)
    },
    claimedCount() { return this.tasks.filter(t => t.claimed).length },
    totalPossible() { return this.tasks.reduce((s, t) => s + (t.points || 0), 0) },
    taskPercent() {
      if (!this.tasks.length) return 0
      return Math.round(this.claimedCount / this.tasks.length * 100)
    },
    todayGain() {
      const d = new Date()
      const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      return this.logs
        .filter(l => l.amount > 0 && String(l.time || '').startsWith(today))
        .reduce((s, l) => s + l.amount, 0)
    },
    recent() { return this.logs.filter(l => l.amount > 0).slice(0, 6) }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      if (!this.session.id) return
      const [b, t, l] = await Promise.all([
        apiPointBalance(this.session.id),
        apiTaskToday(this.session.id),
        apiPointLogs(this.session.id)
      ])
      if (b.code === 200) this.balance = Number(b.data) || 0
      if (t.code === 200) this.tasks = t.data || []
      if (l.code === 200) this.logs = l.data || []
    },
    barWidth(t) {
      if (!t.target) return 0
      return Math.min(100, Math.round((t.progress || 0) / t.target * 100))
    },
    statusText(t) {
      if (t.claimed) return '已到账'
      if (t.done) return '完成啦，快领奖'
      return '进行中'
    },
    async claim(t) {
      if (this.claiming) return
      this.claiming = t.id
      const r = await apiTaskClaim(this.session.id, t.id)
      this.claiming = ''
      if (r.code === 200) {
        sound.success()
        this.toast(r.data || '领取成功', 'ok')
        await this.load()
      } else {
        this.toast(r.msg || '领取失败', 'err')
      }
    }
  }
}
</script>

<style scoped>
.earn { display: flex; flex-direction: column; gap: 13px; padding-bottom: 10px; }
.earn-top { display: flex; align-items: center; gap: 10px; }
.earn-back {
  border: 1.5px solid var(--border); background: var(--card); color: var(--primary);
  border-radius: 12px; padding: 7px 12px; font-size: 13px; font-weight: 700; cursor: pointer;
}
.earn-back:hover { background: var(--panel); }
.earn-title { flex: 1; min-width: 0; }
.earn-title h3 { margin: 0; font-size: 17px; color: var(--primary); }
.earn-title p { margin: 2px 0 0; font-size: 11.5px; color: var(--muted); }
.earn-balance {
  background: linear-gradient(135deg, #f7e08a, #f7a8c4); color: #6b4a2f;
  border-radius: 14px; padding: 8px 13px; font-size: 13px; font-weight: 800; white-space: nowrap;
}
.earn-balance b { font-size: 18px; }
.earn-shop {
  text-decoration: none; font-size: 11.5px; font-weight: 800; color: var(--primary);
  border: 1.5px solid var(--border); border-radius: 12px; padding: 8px 10px; white-space: nowrap;
}
.earn-shop:hover { background: var(--panel); }

.earn-hero {
  background: linear-gradient(135deg, #f3e9ff, #e8f6ff 60%, #e6f8ee);
  border: 1.5px solid var(--border); border-radius: 16px; padding: 14px 16px;
}
.hero-row { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; font-size: 12px; color: var(--muted); }
.hero-row b { font-size: 16px; color: var(--text); margin-right: 10px; }
.hero-row b.up { color: var(--success); }
.hero-bar { height: 9px; border-radius: 9px; background: var(--track); overflow: hidden; margin: 10px 0 8px; }
.hero-bar i { display: block; height: 100%; border-radius: 9px; background: linear-gradient(90deg, var(--purple), var(--pink)); transition: width .35s; }
.earn-hero p { margin: 0; font-size: 11.5px; color: var(--muted); }
.earn-hero p b { color: var(--primary); }

.earn-box {
  display: flex; align-items: center; gap: 12px; padding: 13px 15px; border-radius: 16px;
  background: var(--panel); border: 1.5px dashed var(--border);
}
.earn-box.ready { background: linear-gradient(135deg, #fff6d8, #ffeaf2); border: 1.5px solid #f2d486; }
.box-ico { font-size: 28px; }
.box-main { flex: 1; min-width: 0; }
.box-main b { display: block; font-size: 14px; color: var(--text); }
.box-main b em { color: var(--primary); font-style: normal; }
.box-main span { display: block; font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.box-btn {
  border: 0; border-radius: 12px; padding: 9px 16px; font-size: 13px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, #e9a94b, #e97ba8); cursor: pointer;
}
.box-btn:disabled { opacity: .6; cursor: default; }
.box-tag { font-size: 11.5px; color: var(--muted); white-space: nowrap; }
.box-tag.done { color: var(--success); font-weight: 700; }

.earn-tabs { display: flex; gap: 7px; flex-wrap: wrap; }
.earn-tabs button {
  border: 1.5px solid var(--border); background: var(--card); color: var(--muted);
  border-radius: 999px; padding: 6px 13px; font-size: 12px; font-weight: 700; cursor: pointer;
}
.earn-tabs button.on { background: var(--primary); border-color: var(--primary); color: #fff; }

.earn-list { display: flex; flex-direction: column; gap: 9px; }
.earn-task {
  display: flex; align-items: center; gap: 11px; padding: 12px 13px;
  background: var(--card); border: 1.5px solid var(--border); border-radius: 15px;
}
.earn-task.got { opacity: .65; }
.et-ico {
  width: 38px; height: 38px; flex: 0 0 38px; display: grid; place-items: center;
  font-size: 20px; background: var(--panel); border-radius: 12px;
}
.et-main { flex: 1; min-width: 0; }
.et-head { display: flex; align-items: baseline; gap: 8px; }
.et-head b { font-size: 13.5px; color: var(--text); }
.et-head em { font-size: 12px; font-style: normal; font-weight: 800; color: var(--success); }
.et-main p { margin: 2px 0 6px; font-size: 11.5px; color: var(--muted); }
.et-bar { height: 6px; border-radius: 6px; background: var(--track); overflow: hidden; }
.et-bar i { display: block; height: 100%; border-radius: 6px; background: linear-gradient(90deg, var(--mint), var(--blue)); transition: width .3s; }
.et-main small { display: block; margin-top: 4px; font-size: 10.5px; color: var(--muted); }
.et-act { flex: 0 0 auto; }
.et-btn {
  display: inline-block; text-align: center; text-decoration: none;
  border: 1.5px solid var(--border); border-radius: 11px; padding: 8px 12px;
  font-size: 12px; font-weight: 800; cursor: pointer; min-width: 62px;
}
.et-btn.go { background: var(--panel); color: var(--primary); }
.et-btn.claim { background: var(--primary); border-color: var(--primary); color: #fff; }
.et-btn.done { background: var(--track); color: var(--muted); cursor: default; }
.et-btn:disabled { opacity: .6; cursor: default; }
.earn-empty { text-align: center; padding: 18px; font-size: 12px; color: var(--muted); }

.earn-sec h4 { margin: 2px 0 9px; font-size: 13.5px; color: var(--primary); }
.extra-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.extra-card {
  display: flex; flex-direction: column; align-items: center; gap: 3px; text-decoration: none;
  padding: 11px 6px; border-radius: 14px; background: var(--card); border: 1.5px solid var(--border);
}
.extra-card:hover { background: var(--panel); }
.extra-card span { font-size: 21px; }
.extra-card b { font-size: 11.5px; color: var(--text); text-align: center; }
.extra-card em { font-size: 11px; font-style: normal; font-weight: 800; color: var(--success); }

.log-row { display: flex; align-items: center; gap: 10px; padding: 7px 2px; border-bottom: 1px dashed var(--border); font-size: 12px; }
.log-row span { font-weight: 800; min-width: 42px; }
.log-row .up { color: var(--success); }
.log-row .down { color: var(--danger); }
.log-row em { font-style: normal; color: var(--muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.earn-note {
  font-size: 11.5px; line-height: 1.7; color: var(--muted);
  background: var(--panel); border-radius: 13px; padding: 11px 13px;
}
.earn-note b { color: var(--primary); }

@media (max-width: 480px) {
  .extra-grid { grid-template-columns: repeat(2, 1fr); }
  .earn-title p { display: none; }
}
</style>
