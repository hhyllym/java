<template>
  <div class="game-pinkie">
    <!-- 步数 + 倒计时 -->
    <div class="party-hud">
      <span>已配对 {{ matched }} / {{ pairs }} 对</span>
      <span>步数 {{ steps }}</span>
      <span class="party-timer" :class="{ urgent: left <= 15 }">⏱ {{ left }}s</span>
    </div>

    <!-- 牌桌 -->
    <div class="party-board">
      <button
        v-for="(c, i) in cards"
        :key="c.uid"
        class="party-card"
        :class="{ flipped: c.flipped || c.done, done: c.done, miss: c.miss }"
        @click="flip(i)"
      >
        <span class="party-card-inner">
          <span class="party-back">🎀</span>
          <span class="party-front">{{ c.emoji }}</span>
        </span>
      </button>
    </div>

    <div class="party-tip">💡 碧琪的秘诀：给卡片位置编个故事，记住更快哦</div>
  </div>
</template>

<script>
const PAIRS = 6
const DURATION = 60
const EMOJIS = ['🎈', '🎂', '🎁', '🌟', '🍬', '🌈']

export default {
  name: 'GamePinkie',
  emits: ['score', 'over'],
  data() {
    return {
      cards: this.buildCards(),
      pairs: PAIRS,
      matched: 0,
      steps: 0,
      left: DURATION,
      busy: false,
      first: -1,     // 已翻开待配对的第一张下标
      timer: null,
      finished: false
    }
  },
  created() {
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.timeUp()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    buildCards() {
      const all = EMOJIS.slice(0, PAIRS).concat(EMOJIS.slice(0, PAIRS))
      // 洗牌
      for (let i = all.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[all[i], all[j]] = [all[j], all[i]]
      }
      return all.map((emoji, i) => ({ uid: i, emoji, flipped: false, done: false, miss: false }))
    },
    flip(i) {
      if (this.busy || this.finished) return
      const c = this.cards[i]
      if (c.flipped || c.done) return

      c.flipped = true
      if (this.first < 0) {
        this.first = i
        return
      }
      // 翻第二张：配对判定
      this.steps += 1
      const prev = this.cards[this.first]
      if (prev.emoji === c.emoji) {
        prev.done = true
        c.done = true
        this.matched += 1
        this.first = -1
        if (this.matched >= this.pairs) this.win()
      } else {
        this.busy = true
        prev.miss = true
        c.miss = true
        setTimeout(() => {
          prev.flipped = false
          c.flipped = false
          prev.miss = false
          c.miss = false
          this.first = -1
          this.busy = false
        }, 650)
      }
    },
    win() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      // 计分：基础 50 + 剩余时间×2 - 多余步数×3（不低于 10）
      const score = Math.max(10, 50 + this.left * 2 - Math.max(0, this.steps - this.pairs) * 3)
      setTimeout(() => this.$emit('over', score), 500)
    },
    timeUp() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      // 时间到：每对 10 分保底
      this.$emit('over', this.matched * 10)
    }
  }
}
</script>

<style scoped>
.party-hud {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--muted); margin-bottom: 12px;
}
.party-timer { font-weight: 700; color: var(--primary); }
.party-timer.urgent { color: var(--danger); }

.party-board {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
}
.party-card {
  border: none; background: none; padding: 0; cursor: pointer;
  aspect-ratio: 3 / 4; perspective: 600px;
}
.party-card-inner {
  position: relative; width: 100%; height: 100%;
  display: block; transform-style: preserve-3d;
  transition: transform .35s cubic-bezier(.4,.1,.3,1.2);
}
.party-card.flipped .party-card-inner { transform: rotateY(180deg); }
.party-back, .party-front {
  position: absolute; inset: 0; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
}
.party-back {
  font-size: 22px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 3px 8px rgba(160,108,213,.25);
}
.party-front {
  font-size: 26px; background: var(--card);
  border: 1.5px solid var(--border);
  transform: rotateY(180deg);
}
.party-card.done .party-front {
  border-color: var(--success);
  background: rgba(76,175,125,.12);
  animation: matchPop .3s ease;
}
.party-card.miss .party-front { border-color: var(--danger); }
@keyframes matchPop {
  50% { transform: rotateY(180deg) scale(1.12); }
}

.party-tip {
  margin-top: 12px; font-size: 11px; color: var(--muted); text-align: center;
}
</style>
