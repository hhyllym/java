<template>
  <div class="game-whack">
    <div class="wk-hud">
      <span>⏱ 剩余 {{ left }}s</span>
      <span>摸到 {{ hits }} 只</span>
      <span>得分 {{ score }}</span>
    </div>

    <div class="wk-tip">🐰 轻轻摸一下冒头的小动物，别碰蘑菇 🍄</div>

    <div class="wk-grid">
      <button
        v-for="i in 9"
        :key="i"
        class="wk-hole"
        :class="{ pop: cell === i - 1, bad: cell === i - 1 && !item.good }"
        @click="tap(i - 1)"
      >
        <span v-if="cell === i - 1" class="wk-item">{{ item.icon }}</span>
        <span v-else class="wk-grass">🌿</span>
      </button>
    </div>

    <div v-if="flash" class="wk-feedback" :class="flash === 'good' ? 'ok' : 'no'">
      {{ flash === 'good' ? '摸到啦 +10' : '哎呀是蘑菇 -8' }}
    </div>
    <div class="wk-note">小动物每 0.8 秒换个草丛冒头，点中立刻换位置</div>
  </div>
</template>

<script>
/** 🐰 森林躲猫猫：3×3 草丛随机冒头，点小动物得分、点到蘑菇扣分，30 秒一局 */
const GOOD = ['🐰', '🐿️', '🦔', '🐥', '🦌']
const BAD = ['🍄', '🌵']
const DURATION = 30

export default {
  name: 'GameWhack',
  emits: ['score', 'over'],
  data() {
    return {
      left: DURATION,
      score: 0,
      hits: 0,
      cell: 0,
      item: { good: true, icon: GOOD[0] },
      flash: '',
      countTimer: null,
      spawnTimer: null,
      flashTimer: null
    }
  },
  created() {
    this.spawn()
    this.spawnTimer = setInterval(this.spawn, 820)
    this.countTimer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.finish()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.countTimer)
    clearInterval(this.spawnTimer)
    clearTimeout(this.flashTimer)
  },
  methods: {
    spawn() {
      this.cell = Math.floor(Math.random() * 9)
      const good = Math.random() < 0.78
      const pool = good ? GOOD : BAD
      this.item = { good, icon: pool[Math.floor(Math.random() * pool.length)] }
    },
    tap(i) {
      if (this.left <= 0 || i !== this.cell) return
      if (this.item.good) {
        this.score += 10
        this.hits += 1
        this.flash = 'good'
      } else {
        this.score = Math.max(0, this.score - 8)
        this.flash = 'bad'
      }
      this.$emit('score', this.score)
      clearTimeout(this.flashTimer)
      this.flashTimer = setTimeout(() => { this.flash = '' }, 420)
      this.spawn()
    },
    finish() {
      clearInterval(this.countTimer)
      clearInterval(this.spawnTimer)
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.wk-hud {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 8px;
}
.wk-tip {
  text-align: center; font-size: 13px; font-weight: 700;
  color: var(--text); margin-bottom: 12px;
}
.wk-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px;
  background: var(--panel); border-radius: 16px; padding: 14px;
}
.wk-hole {
  border: 2px dashed var(--border); background: var(--card);
  border-radius: 14px; aspect-ratio: 1 / 1; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; transition: transform .12s, border-color .12s, background .12s;
}
.wk-hole.pop {
  transform: scale(1.04); border-style: solid; border-color: var(--mint);
  background: var(--card);
}
.wk-hole.pop.bad { border-color: var(--danger); }
.wk-item { animation: popIn .18s ease-out; }
@keyframes popIn { from { transform: translateY(8px) scale(.6); opacity: 0; } }
.wk-grass { font-size: 18px; opacity: .35; }
.wk-feedback {
  text-align: center; font-size: 12.5px; font-weight: 700; margin-top: 12px;
}
.wk-feedback.ok { color: var(--success); }
.wk-feedback.no { color: var(--danger); }
.wk-note {
  text-align: center; font-size: 11px; color: var(--muted); margin-top: 8px;
}
</style>
