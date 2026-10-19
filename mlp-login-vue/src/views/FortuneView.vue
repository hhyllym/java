<template>
  <div class="home">
    <div class="home-top">
      <div class="logo">🎲</div>
      <div class="home-greet">
        <h2>今日幸运小马</h2>
        <p>{{ todayStr }} · 每天抽一只陪你冒险</p>
      </div>
      <button class="logout" @click="goHome">返回</button>
    </div>

    <div v-if="loading" class="empty">正在摇骰子…</div>
    <div v-else-if="!list.length" class="empty">图鉴里还没有小马，先去首页看看吧 🐴</div>

    <template v-else>
      <div class="fortune-card" :style="{ '--pc': pony.color }">
        <div class="fortune-badge">今日幸运</div>
        <div class="fortune-face">
          <CutieMark v-if="hasMark(pony.mark)" :mark="pony.mark" />
          <template v-else>{{ pony.emoji }}</template>
        </div>
        <h3>{{ pony.name }}</h3>
        <p class="fortune-alias">{{ pony.alias }}</p>
        <span class="rarity inline" :class="rarityClass(pony.rarity)">{{ pony.rarity }}</span>
        <p class="fortune-text">「{{ fortune }}」</p>
        <div class="fortune-meta">
          <span>👀 {{ pony.viewCount }} 次被查看</span>
          <span>🏷 {{ (pony.tags || []).length }} 个标签</span>
        </div>
      </div>

      <button class="more" @click="reroll">🎲 再抽一次（换个心情）</button>
      <button class="more ghost" @click="openDetail">🔍 看看 TA 的图鉴详情</button>
    </template>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { apiSearch } from '../home'
import CutieMark from '../components/CutieMark.vue'

const MARKS = ['pinkie', 'twilight', 'dash', 'applejack', 'fluttershy', 'rarity']
const RARITY_CLASS = { 常见: 'rr-common', 稀有: 'rr-rare', 传说: 'rr-legend' }

const FORTUNES = [
  '今天适合勇敢地迈出第一步，彩虹就在前方！',
  '温柔对待身边的人，好运也会温柔待你。',
  '你的笑容是小马王国最强的魔法。',
  '别怕犯错，每只传奇小马都摔过跟头。',
  '今天会有意外的惊喜，保持期待吧～',
  '专注做喜欢的事，时间会给你糖吃。',
  '把烦恼分给朋友，快乐就会翻倍。',
  '你比自己想象中更了不起，记住这点。'
]

export default {
  name: 'FortuneView',
  components: { CutieMark },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      loading: true,
      list: [],
      index: 0
    }
  },
  computed: {
    pony() {
      return this.list[this.index] || {}
    },
    fortune() {
      // 同一天固定一个运势，按日期选；再抽一次时改用 index
      const seed = new Date().getDate() + this.index * 3
      return FORTUNES[seed % FORTUNES.length]
    },
    todayStr() {
      const d = new Date()
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
  },
  created() {
    this.loadAll()
  },
  methods: {
    hasMark(m) {
      return MARKS.indexOf(m) >= 0
    },
    rarityClass(r) {
      return RARITY_CLASS[r] || 'rr-common'
    },
    async loadAll() {
      this.loading = true
      const r = await apiSearch({ page: 1, size: 100 })
      this.loading = false
      if (r.code !== 200 || !r.data || !r.data.list) {
        this.toast(r.msg || '图鉴加载失败', 'err')
        return
      }
      this.list = r.data.list || []
      // 当日幸运：用「一年中的第几天」取模，保证同一天同一个人抽到的不变
      const now = new Date()
      const dayOfYear = Math.floor((now - new Date(now.getFullYear(), 0, 0)) / 86400000)
      this.index = dayOfYear % this.list.length
    },
    reroll() {
      if (!this.list.length) return
      let n = this.index
      while (n === this.index && this.list.length > 1) {
        n = Math.floor(Math.random() * this.list.length)
      }
      this.index = n
    },
    openDetail() {
      this.$router.push({ name: 'home', query: { pony: this.pony.id } })
    },
    goHome() {
      this.$router.push({ name: 'home' })
    }
  }
}
</script>

<style scoped>
.fortune-card {
  margin: 16px 0 4px;
  border-radius: 18px;
  padding: 22px 18px 20px;
  text-align: center;
  background: linear-gradient(160deg, #fff 0%, var(--panel) 100%);
  border: 2px solid var(--pc, var(--pink));
  box-shadow: 0 8px 22px rgba(160, 108, 213, .14);
  position: relative;
}
.fortune-badge {
  position: absolute;
  top: -11px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 14px;
  border-radius: 999px;
  letter-spacing: .5px;
  white-space: nowrap;
}
.fortune-face {
  width: 84px;
  height: 84px;
  margin: 6px auto 12px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  background: linear-gradient(135deg, var(--pc, var(--pink)), #ffffff55);
  box-shadow: 0 6px 16px rgba(160, 108, 213, .22);
}
.fortune-face :deep(svg) {
  width: 46px;
  height: 46px;
}
.fortune-card h3 {
  margin: 0;
  font-size: 19px;
}
.fortune-alias {
  margin: 4px 0 8px;
  font-size: 12px;
  color: var(--muted);
}
.fortune-text {
  font-size: 14px;
  line-height: 1.7;
  margin: 14px 4px 0;
  color: var(--text);
}
.fortune-meta {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 12px;
  font-size: 11.5px;
  color: var(--muted);
}
.more.ghost {
  background: var(--panel);
  color: var(--primary);
  margin-top: 10px;
  border: none;
}
</style>
