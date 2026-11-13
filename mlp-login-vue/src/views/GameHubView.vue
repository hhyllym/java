<template>
  <div class="home hub">
    <!-- 顶部：返回 + 标题 -->
    <div class="home-top">
      <router-link to="/home" class="hub-back" title="回首页">«</router-link>
      <div class="logo hub-logo">🎮</div>
      <div class="home-greet">
        <h2>星糖游乐场</h2>
        <p>{{ GAMES.length }} 款小游戏 · 挑一款，开玩！</p>
      </div>
      <button class="logout" @click="goHome">返回</button>
    </div>

    <!-- 今日推荐：按日期挑一款，每天换一个 -->
    <div class="hub-hero" :style="{ '--pc': PONIES[daily.pony].color }" @click="open(daily)">
      <span class="hero-face"><CutieMark :mark="PONIES[daily.pony].mark" /></span>
      <span class="hero-main">
        <em class="hero-tag">✨ 今日推荐</em>
        <b>{{ daily.icon }} {{ daily.name }}</b>
        <em>{{ daily.rule }}</em>
      </span>
      <span class="hero-go">开玩 ›</span>
    </div>

    <!-- 战绩条：全部读本地学习存档 -->
    <div class="stat-bar">
      <div class="stat"><b>{{ GAMES.length }}</b><span>款游戏</span></div>
      <div class="stat"><b>{{ playedCount }}</b><span>已挑战</span></div>
      <div class="stat"><b>{{ totalBest }}</b><span>最佳分总和</span></div>
      <div class="stat"><b>{{ walletPoints }}</b><span>可用积分</span></div>
    </div>

    <!-- 按主角筛选 -->
    <div class="filter-label">🎠 按主角挑游戏</div>
    <div class="filter-row">
      <button class="fx" :class="{ on: !pony }" @click="pick('')">全部</button>
      <button
        v-for="p in ponyList"
        :key="p.id"
        class="fx"
        :class="{ on: pony === p.id }"
        @click="pick(p.id)"
      >{{ p.emoji }} {{ p.name }}<em>{{ countOf(p.id) }}</em></button>
    </div>

    <!-- 游戏卡片：点整张卡直接开玩 -->
    <div class="filter-label">🎮 共 {{ shown.length }} 款 · 点卡片直接开玩</div>
    <div class="hub-grid">
      <button
        v-for="g in shown"
        :key="g.id"
        class="hub-card"
        :style="{ '--pc': PONIES[g.pony].color }"
        @click="open(g)"
        @mouseenter="hover"
      >
        <span class="hub-face"><CutieMark :mark="PONIES[g.pony].mark" /></span>
        <span class="hub-icon">{{ g.icon }}</span>
        <b class="hub-name">{{ g.name }}</b>
        <em class="hub-teacher">{{ PONIES[g.pony].name }}出品</em>
        <span class="hub-rule">{{ g.rule }}</span>
        <span class="hub-foot">
          <i :class="{ none: !bestOf(g.id) }">{{ bestOf(g.id) ? '🏆 最佳 ' + bestOf(g.id) : '还没玩过' }}</i>
          <b>开玩 →</b>
        </span>
      </button>
    </div>
    <div v-if="!shown.length" class="empty">这位主角暂时没有游戏，换一个试试 🥺</div>

    <div class="hub-tip">💡 这里的「可用积分」和赚积分页、积分商店、大富翁是同一份账户余额；超过历史最佳的那部分得分也会存进来，结算页还能看排行榜、找好友对战～</div>
    <router-link to="/learn" class="hub-link">🎓 想上课？去星辉学院 →</router-link>
  </div>
</template>

<script>
import CutieMark from '../components/CutieMark.vue'
import { GAMES, PONIES } from '../courses'
import { loadLearn } from '../learn'
import { fetchBalance } from '../wallet'
import sound from '../sound'

/** 六位主角在筛选条上的展示顺序与装饰 */
const PONY_TABS = [
  { id: 'twilight', emoji: '🦄' },
  { id: 'applejack', emoji: '🍎' },
  { id: 'dash', emoji: '🌈' },
  { id: 'pinkie', emoji: '🎈' },
  { id: 'fluttershy', emoji: '🦋' },
  { id: 'rarity', emoji: '💎' }
]

export default {
  name: 'GameHubView',
  components: { CutieMark },
  data() {
    return {
      state: loadLearn(),
      // 可用积分统一读账户余额（和赚积分页 / 商店 / 大富翁同一份）
      walletPoints: 0,
      walletOnline: false,
      pony: ''
    }
  },
  async created() {
    const r = await fetchBalance()
    this.walletPoints = r.value
    this.walletOnline = r.online
  },
  computed: {
    GAMES: () => GAMES,
    PONIES: () => PONIES,
    ponyList() {
      return PONY_TABS.map(p => ({ ...p, name: PONIES[p.id].name }))
    },
    shown() {
      return this.pony ? GAMES.filter(g => g.pony === this.pony) : GAMES
    },
    /** 按日期算出一个固定的下标，保证同一天推荐不变、隔天自动换 */
    daily() {
      const d = new Date()
      const n = d.getFullYear() * 372 + d.getMonth() * 31 + d.getDate()
      return GAMES[n % GAMES.length]
    },
    playedCount() {
      return GAMES.filter(g => this.bestOf(g.id) > 0).length
    },
    totalBest() {
      return GAMES.reduce((sum, g) => sum + this.bestOf(g.id), 0)
    }
  },
  methods: {
    bestOf(id) {
      return ((this.state.games || {})[id] || {}).best || 0
    },
    countOf(id) {
      return GAMES.filter(g => g.pony === id).length
    },
    pick(id) {
      this.pony = id
      sound.click()
    },
    open(g) {
      sound.click()
      this.$router.push({ name: 'game', params: { id: g.id }, query: { from: 'games' } })
    },
    hover() {
      sound.hover()
    },
    goHome() {
      sound.click()
      this.$router.push({ name: 'home' })
    }
  }
}
</script>

<style scoped>
.hub-logo { font-size: 23px; }
.hub-back {
  text-decoration: none; color: var(--muted); font-size: 22px;
  width: 30px; height: 30px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; transition: color .15s, background .15s;
}
.hub-back:hover { color: var(--primary); background: var(--panel); }

/* 今日推荐 */
.hub-hero {
  display: flex; align-items: center; gap: 12px; cursor: pointer;
  border-radius: 16px; padding: 14px 16px; margin-top: 4px;
  background: linear-gradient(120deg, var(--pc), var(--card) 88%);
  border: 1.5px solid var(--border);
  box-shadow: 0 5px 14px rgba(160,108,213,.14);
  transition: transform .15s, box-shadow .15s;
}
.hub-hero:hover { transform: translateY(-2px); box-shadow: 0 9px 20px rgba(160,108,213,.22); }
.hero-face {
  width: 48px; height: 48px; border-radius: 50%; flex-shrink: 0;
  background: #ffffffaa; display: flex; align-items: center; justify-content: center;
}
.hero-face svg { width: 32px; height: 32px; }
.hero-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.hero-tag { font-style: normal; font-size: 10px; color: var(--muted); letter-spacing: .5px; }
.hero-main b { font-size: 15px; color: var(--text); }
.hero-main em:last-child {
  font-style: normal; font-size: 11px; color: var(--muted);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hero-go {
  flex-shrink: 0; font-size: 12px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border-radius: 999px; padding: 6px 13px;
}

/* 游戏卡片 */
.hub-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.hub-card {
  position: relative; text-align: left; cursor: pointer;
  border: 1.5px solid var(--border); background: var(--card);
  border-radius: 16px; padding: 13px 12px 11px;
  display: flex; flex-direction: column; gap: 3px;
  transition: transform .15s, border-color .15s, box-shadow .15s;
}
.hub-card:hover {
  transform: translateY(-3px); border-color: var(--pc);
  box-shadow: 0 9px 18px rgba(160,108,213,.16);
}
.hub-face {
  width: 38px; height: 38px; border-radius: 50%; margin-bottom: 4px;
  background: linear-gradient(135deg, var(--pc), #ffffff88);
  display: flex; align-items: center; justify-content: center;
}
.hub-face svg { width: 25px; height: 25px; }
.hub-icon { position: absolute; top: 13px; right: 12px; font-size: 20px; }
.hub-name { font-size: 13.5px; color: var(--text); }
.hub-teacher { font-style: normal; font-size: 10.5px; color: var(--muted); }
.hub-rule {
  font-size: 11px; color: var(--muted); line-height: 1.5;
  margin-top: 5px; min-height: 33px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.hub-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 4px;
  border-top: 1px dashed var(--border); padding-top: 8px; margin-top: 4px;
}
.hub-foot i {
  font-style: normal; font-size: 10px; color: var(--primary); font-weight: 700;
}
.hub-foot i.none { color: var(--muted); font-weight: 400; }
.hub-foot b {
  font-size: 10.5px; color: #fff; padding: 3px 9px; border-radius: 999px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}

.hub-tip {
  font-size: 11.5px; color: var(--muted); line-height: 1.7;
  background: var(--panel); border-radius: 12px; padding: 10px 12px; margin-top: 16px;
}
.hub-link {
  display: block; text-align: center; margin-top: 14px;
  font-size: 12.5px; font-weight: 700; color: var(--primary); text-decoration: none;
}
.hub-link:hover { text-decoration: underline; }

@media (max-width: 480px) {
  .hub-grid { grid-template-columns: 1fr; }
}
</style>
