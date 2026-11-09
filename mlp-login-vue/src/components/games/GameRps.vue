<template>
  <div class="game-rps">
    <div class="rps-hud">
      <span>第 {{ round }} / {{ TOTAL }} 回合</span>
      <span>连胜 ×{{ streak }}</span>
      <span>得分 {{ score }}</span>
    </div>

    <div class="rps-arena">
      <div class="rps-hand">
        <span class="rps-emoji">{{ meHand || '❔' }}</span>
        <em>你</em>
      </div>
      <span class="rps-vs">VS</span>
      <div class="rps-hand">
        <span class="rps-emoji">{{ foeHand || '❔' }}</span>
        <em>AI</em>
      </div>
    </div>

    <div class="rps-result" :class="lastResult">{{ lastText }}</div>

    <div v-if="!over" class="rps-actions">
      <button
        v-for="(h, i) in HANDS"
        :key="h.icon"
        class="rps-btn"
        :disabled="busy"
        @click="play(i)"
      >{{ h.icon }} {{ h.name }}</button>
    </div>

    <div v-else class="rps-over">
      <b>{{ score >= 60 ? '🏆 手气爆棚！' : score >= 30 ? '👍 不错哦' : '💦 就这？再来！' }}</b>
      <span>最终得分 <em>{{ score }}</em></span>
      <button class="rps-again" @click="reset">🔄 再来一局</button>
    </div>

    <div class="rps-note">赢 +10，平 +3，输了不扣；连续赢有连胜加成</div>
  </div>
</template>

<script>
/** ✊ 猜拳争霸：10 回合跟 AI 猜拳，连胜加成，结束 emit over 自动换积分 */
const HANDS = [
  { icon: '✊', name: '石头' },
  { icon: '✋', name: '剪刀' },
  { icon: '✌️', name: '布' }
]
const TOTAL = 10

export default {
  name: 'GameRps',
  emits: ['score', 'over'],
  data() {
    return {
      HANDS,
      TOTAL,
      round: 1,
      score: 0,
      streak: 0,
      bestStreak: 0,
      meHand: '',
      foeHand: '',
      busy: false,
      over: false,
      lastResult: '',
      lastText: '出拳吧！'
    }
  },
  beforeUnmount() {
    if (this._t) clearTimeout(this._t)
  },
  methods: {
    play(i) {
      if (this.busy || this.over) return
      this.busy = true
      const foe = Math.floor(Math.random() * 3)
      this.meHand = HANDS[i].icon
      this.foeHand = HANDS[foe].icon
      const diff = (i - foe + 3) % 3 // 0平 2赢 1输（石0 剪1 布2：赢局差恒为 2）
      let res
      if (diff === 0) res = 'draw'
      else if (diff === 2) res = 'win'
      else res = 'lose'
      if (res === 'win') {
        this.streak += 1
        this.bestStreak = Math.max(this.bestStreak, this.streak)
        const bonus = this.streak >= 3 ? 5 + this.streak * 2 : 0
        this.score += 10 + bonus
        this.lastResult = 'win'
        this.lastText = bonus ? `赢了！连胜 ×${this.streak}，+${10 + bonus} 🔥` : '赢了！+10'
      } else if (res === 'draw') {
        this.streak = 0
        this.score += 3
        this.lastResult = 'draw'
        this.lastText = '平局，+3'
      } else {
        this.streak = 0
        this.lastResult = 'lose'
        this.lastText = '输了…'
      }
      this.$emit('score', this.score)
      this._t = setTimeout(() => {
        this.busy = false
        if (this.round >= this.TOTAL) {
          this.over = true
          this.lastText = `打完了！最高连胜 ×${this.bestStreak}`
          this.$emit('over', this.score)
        } else {
          this.round += 1
        }
      }, 700)
    },
    reset() {
      this.round = 1
      this.score = 0
      this.streak = 0
      this.bestStreak = 0
      this.meHand = ''
      this.foeHand = ''
      this.over = false
      this.busy = false
      this.lastResult = ''
      this.lastText = '出拳吧！'
      this.$emit('score', 0)
    }
  }
}
</script>

<style scoped>
.rps-hud {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.rps-arena {
  display: flex; align-items: center; justify-content: center; gap: 18px;
  background: var(--panel); border-radius: 16px; padding: 18px 10px;
}
.rps-hand { text-align: center; }
.rps-emoji { font-size: 44px; display: block; line-height: 1.2; }
.rps-hand em { font-style: normal; font-size: 11px; color: var(--muted); }
.rps-vs { font-weight: 800; color: var(--primary); }
.rps-result {
  text-align: center; font-size: 13px; font-weight: 700;
  margin: 12px 0; min-height: 20px; color: var(--text);
}
.rps-result.win { color: var(--success); }
.rps-result.lose { color: var(--danger); }
.rps-actions { display: flex; gap: 9px; justify-content: center; }
.rps-btn {
  border: 2px dashed var(--border); background: var(--card);
  border-radius: 14px; cursor: pointer; padding: 12px 14px;
  font-size: 15px; font-weight: 700; color: var(--text);
  transition: transform .12s, border-color .12s;
}
.rps-btn:not(:disabled):hover { transform: translateY(-2px); border-color: var(--primary); }
.rps-btn:disabled { opacity: .5; cursor: wait; }
.rps-over { text-align: center; padding: 4px 0; }
.rps-over b { display: block; font-size: 15px; color: var(--text); margin-bottom: 4px; }
.rps-over span { font-size: 12.5px; color: var(--muted); }
.rps-over em { font-style: normal; font-weight: 800; color: var(--primary); font-size: 15px; }
.rps-again {
  display: block; margin: 10px auto 0; border: none; border-radius: 999px;
  padding: 9px 20px; font-weight: 700; font-size: 13px; cursor: pointer;
  background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff;
}
.rps-note { text-align: center; font-size: 11px; color: var(--muted); margin-top: 10px; }
</style>
