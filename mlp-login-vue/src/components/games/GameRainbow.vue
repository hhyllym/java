<template>
  <div class="game-rainbow">
    <div class="rb-hud">
      <span>⏱ 剩余 {{ left }}s</span>
      <span v-if="combo >= 2" class="rb-combo">🔥 连对 {{ combo }}</span>
      <span>得分 {{ score }}</span>
    </div>

    <div class="rb-stage" :class="{ right: flash === 'right', wrong: flash === 'wrong' }">
      <p class="rb-ask">点出这个词<b>写的是</b>哪种颜色（别被字的颜色骗到）</p>
      <p class="rb-word" :style="{ color: ink.hex }">{{ word.name }}</p>
      <p class="rb-hint">字是「{{ word.name }}」，但被涂成了别的颜色，按字的意思选色块 👇</p>
    </div>

    <div class="rb-colors">
      <button
        v-for="c in COLORS"
        :key="c.name"
        class="rb-color"
        :style="{ '--cc': c.hex }"
        :disabled="locked"
        :title="c.name"
        @click="pick(c)"
      ></button>
    </div>

    <div v-if="flash === 'right'" class="rb-feedback ok">🌈 答对了！+{{ lastGain }} 分</div>
    <div v-else-if="flash === 'wrong'" class="rb-feedback no">看走眼啦，-5 分</div>
  </div>
</template>

<script>
/** 🌈 彩虹速配：字的内容和字的颜色故意不一致，30 秒内按「字的意思」点色块 */
const COLORS = [
  { name: '红色', hex: '#e8697d' },
  { name: '蓝色', hex: '#4a9fe0' },
  { name: '绿色', hex: '#57b98b' },
  { name: '紫色', hex: '#9a7ae0' }
]
const DURATION = 30

export default {
  name: 'GameRainbow',
  emits: ['score', 'over'],
  data() {
    return {
      COLORS,
      left: DURATION,
      score: 0,
      combo: 0,
      lastGain: 0,
      word: COLORS[0],
      ink: COLORS[1],
      locked: false,
      flash: '',
      timer: null,
      timers: []
    }
  },
  created() {
    this.newRound()
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.finish()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
    this.timers.forEach(clearTimeout)
  },
  methods: {
    newRound() {
      this.word = COLORS[Math.floor(Math.random() * COLORS.length)]
      // 字体颜色故意挑一个和字义不同的，制造干扰
      const others = COLORS.filter(c => c.name !== this.word.name)
      this.ink = others[Math.floor(Math.random() * others.length)]
    },
    pick(c) {
      if (this.locked || this.left <= 0) return
      this.locked = true
      if (c.name === this.word.name) {
        this.combo += 1
        this.lastGain = 15 + (this.combo >= 3 ? 5 : 0)
        this.score += this.lastGain
        this.flash = 'right'
      } else {
        this.combo = 0
        this.score = Math.max(0, this.score - 5)
        this.flash = 'wrong'
      }
      this.$emit('score', this.score)
      this.timers.push(setTimeout(() => {
        this.locked = false
        this.flash = ''
        this.newRound()
      }, 360))
    },
    finish() {
      clearInterval(this.timer)
      this.timers.forEach(clearTimeout)
      this.timers = []
      this.locked = true
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.rb-hud {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.rb-combo { color: var(--primary); font-weight: 700; }
.rb-stage {
  background: var(--panel); border-radius: 16px; padding: 18px 14px;
  text-align: center; transition: background .18s;
}
.rb-stage.right { background: rgba(76,175,125,.14); }
.rb-stage.wrong { background: rgba(224,102,102,.12); }
.rb-ask { font-size: 12.5px; color: var(--text); margin: 0 0 14px; }
.rb-ask b { color: var(--primary); }
.rb-word {
  font-size: 44px; font-weight: 800; line-height: 1.2; margin: 0 0 10px;
  letter-spacing: 4px;
}
.rb-hint { font-size: 11px; color: var(--muted); margin: 0; line-height: 1.6; }
.rb-colors {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 16px;
}
.rb-color {
  border: none; cursor: pointer; height: 62px; border-radius: 14px;
  background: var(--cc); transition: transform .12s, box-shadow .12s;
}
.rb-color:hover:not(:disabled) { transform: translateY(-3px); box-shadow: 0 6px 14px rgba(0,0,0,.16); }
.rb-color:disabled { cursor: default; }
.rb-feedback {
  text-align: center; font-size: 12.5px; font-weight: 700; margin-top: 12px;
}
.rb-feedback.ok { color: var(--success); }
.rb-feedback.no { color: var(--danger); }
</style>
