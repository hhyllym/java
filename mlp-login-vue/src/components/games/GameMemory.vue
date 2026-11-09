<template>
  <div class="game-memory">
    <div class="mem-hud">
      <span>第 {{ level }} / {{ MAX_LEVEL }} 轮</span>
      <span v-if="combo >= 2" class="mem-combo">🔥 连对 {{ combo }}</span>
      <span>失误 {{ mistakes }} / 3</span>
    </div>

    <div class="mem-tip">{{ phaseText }}</div>

    <div class="mem-grid">
      <button
        v-for="(r, i) in RUNES"
        :key="i"
        class="rune"
        :class="{ on: lit === i }"
        :style="{ '--rc': r.color }"
        :disabled="phase !== 'input'"
        @click="tap(i)"
      >{{ r.icon }}</button>
    </div>

    <div class="mem-foot">
      <span>本轮 {{ seq.length }} 个符文 · 已复点 {{ step }}</span>
      <button v-if="phase === 'input'" class="mem-replay" @click="playSeq">🔁 再看一遍</button>
    </div>

    <div v-if="flash === 'good'" class="mem-feedback ok">✨ 记对了！+{{ lastGain }} 分</div>
    <div v-else-if="flash === 'bad'" class="mem-feedback no">顺序错啦，-10 分，再看一遍</div>
  </div>
</template>

<script>
/** 🪄 魔法记忆：符文依次亮起，按顺序复点；每轮多一个，共 6 轮，3 次失误结束 */
const RUNES = [
  { icon: '✨', color: '#8f7ae8' },
  { icon: '🌙', color: '#6ec6e6' },
  { icon: '⭐', color: '#f0a06a' },
  { icon: '🔮', color: '#f7a8c4' }
]
const MAX_LEVEL = 6

export default {
  name: 'GameMemory',
  emits: ['score', 'over'],
  data() {
    return {
      RUNES,
      MAX_LEVEL,
      level: 1,
      seq: [],
      step: 0,
      lit: -1,
      phase: 'show',      // show = 播放中 / input = 等玩家点 / done = 本轮结算
      flash: '',
      lastGain: 0,
      mistakes: 0,
      combo: 0,
      score: 0,
      timers: []
    }
  },
  computed: {
    phaseText() {
      if (this.phase === 'show') return '👀 看好了，记住符文亮起的顺序…'
      if (this.phase === 'input') return '👉 按刚才的顺序点亮它们'
      return '这一轮结束啦'
    }
  },
  created() {
    this.nextRound()
  },
  beforeUnmount() {
    this.clearTimers()
  },
  methods: {
    clearTimers() {
      this.timers.forEach(clearTimeout)
      this.timers = []
    },
    later(fn, ms) {
      this.timers.push(setTimeout(fn, ms))
    },
    nextRound() {
      this.clearTimers()
      this.seq.push(Math.floor(Math.random() * RUNES.length))
      this.step = 0
      this.lit = -1
      this.flash = ''
      this.phase = 'show'
      this.playSeq()
    },
    /** 依次点亮整个序列，播完才允许点击 */
    playSeq() {
      if (this.phase === 'done') return
      this.clearTimers()
      this.phase = 'show'
      this.step = 0
      this.flash = ''
      this.seq.forEach((n, i) => {
        this.later(() => { this.lit = n }, 380 + i * 700)
        this.later(() => { this.lit = -1 }, 380 + i * 700 + 430)
      })
      this.later(() => {
        this.lit = -1
        this.phase = 'input'
      }, 380 + this.seq.length * 700)
    },
    tap(i) {
      if (this.phase !== 'input') return
      this.lit = i
      this.later(() => { if (this.lit === i) this.lit = -1 }, 240)
      if (i !== this.seq[this.step]) {
        this.wrong()
        return
      }
      this.step += 1
      if (this.step < this.seq.length) return

      // 本轮全部复点正确
      this.phase = 'done'
      this.combo += 1
      this.lastGain = 20 + (this.combo >= 3 ? 10 : 0)
      this.score += this.lastGain
      this.flash = 'good'
      this.$emit('score', this.score)
      this.later(() => {
        if (this.level >= MAX_LEVEL) {
          this.$emit('over', this.score)
          return
        }
        this.level += 1
        this.nextRound()
      }, 900)
    },
    wrong() {
      this.phase = 'done'
      this.combo = 0
      this.mistakes += 1
      this.score = Math.max(0, this.score - 10)
      this.flash = 'bad'
      this.$emit('score', this.score)
      if (this.mistakes >= 3) {
        this.later(() => this.$emit('over', this.score), 900)
        return
      }
      // 同一轮重播一次，再给个机会
      this.later(() => { this.phase = 'show'; this.playSeq() }, 1100)
    }
  }
}
</script>

<style scoped>
.mem-hud {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.mem-combo { color: var(--primary); font-weight: 700; }
.mem-tip {
  text-align: center; font-size: 13px; font-weight: 700; color: var(--text);
  margin-bottom: 14px;
}
.mem-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
  background: var(--panel); border-radius: 16px; padding: 18px;
}
.rune {
  border: 2px solid transparent; cursor: pointer;
  aspect-ratio: 1.35 / 1; border-radius: 14px;
  background: linear-gradient(135deg, var(--rc), #ffffff88);
  font-size: 34px; color: #fff;
  display: flex; align-items: center; justify-content: center;
  transition: transform .12s, box-shadow .12s, filter .12s;
  filter: saturate(.75) brightness(.96);
}
.rune:disabled { cursor: default; }
.rune.on {
  transform: scale(1.09);
  filter: saturate(1.25) brightness(1.12);
  box-shadow: 0 0 0 4px var(--rc), 0 6px 16px rgba(160,108,213,.3);
}
.mem-foot {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 11.5px; color: var(--muted); margin-top: 12px;
}
.mem-replay {
  border: 1.5px solid var(--border); background: var(--card); color: var(--primary);
  font-size: 11.5px; padding: 5px 11px; border-radius: 999px; cursor: pointer;
}
.mem-feedback {
  text-align: center; font-size: 12.5px; font-weight: 700; margin-top: 12px;
}
.mem-feedback.ok { color: var(--success); }
.mem-feedback.no { color: var(--danger); }
</style>
