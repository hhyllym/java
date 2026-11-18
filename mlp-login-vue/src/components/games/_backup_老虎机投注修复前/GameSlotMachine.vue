<template>
  <div class="slot-wrap">
    <div class="slot-hud">
      <span>我的积分 <b>{{ points }}</b></span>
      <span>本局投入 <b>{{ invested }}</b></span>
      <span>本局得分 <b>{{ score }}</b></span>
    </div>

    <!-- 投入积分 -->
    <div class="slot-bets">
      <span>投入</span>
      <input
        class="bet-input" type="number" min="10" step="10"
        v-model.number="betAmount" @change="clampBet"
        :disabled="spinning"
      />
      <span>积分</span>
      <button
        v-for="p in BET_PRESETS" :key="p"
        class="bet" :class="{ on: betAmount === p }"
        :disabled="spinning || points < p" @click="betAmount = p"
      >{{ p >= 1000 ? (p / 1000) + 'k' : p }}</button>
      <span class="bet-hint">每转投入 {{ cost }} 积分 · 大注≥5000 时三星头奖概率 30%</span>
    </div>

    <!-- 三转轮 -->
    <div class="slot-machine" :class="{ spin: spinning }">
      <div v-for="(r, ri) in reels" :key="ri" class="reel">
        <div
          class="reel-strip"
          :style="{ transform: 'translateY(' + (-r.pos * CELL) + 'px)', transition: r.dur + 'ms cubic-bezier(.2,.7,.3,1)' }"
        >
          <div
            v-for="(s, si) in STRIP"
            :key="si"
            class="cell"
            :style="{ height: CELL + 'px', background: s.c }"
          ><span>{{ s.e }}</span></div>
        </div>
      </div>
    </div>

    <p class="slot-result" :class="resultKind">{{ resultText }}</p>

    <button class="slot-spin" :disabled="pointsLoading || spinning || stage === 'done' || points < cost" @click="spin">
      {{ pointsLoading ? '正在读取账户积分…' : (spinning ? '转动中…' : (points < cost ? '积分不足，先去赚点积分' : '🎰 投入 ' + cost + ' 积分 · 拉杆！')) }}
    </button>

    <div v-if="stage === 'done'" class="slot-final">
      <b>净收益 {{ net >= 0 ? '+' : '' }}{{ net }} 积分</b>
      <button class="ghost" @click="restart">再来一局</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'
import { fetchBalance, spend } from '../../wallet'
import { isBigBet, BET_PRESETS } from './luckyBase'

// 每个符号：emoji + 底色（底色确保即使 emoji 缺字形也不会空着）
const SYM = [
  { e: '🍬', c: 'linear-gradient(135deg,#ffd1e3,#ff9ec4)' },
  { e: '🍭', c: 'linear-gradient(135deg,#ffb3b3,#ff6b6b)' },
  { e: '🍫', c: 'linear-gradient(135deg,#d9a066,#a9743b)' },
  { e: '🍩', c: 'linear-gradient(135deg,#e3c6ff,#b079ff)' },
  { e: '🧁', c: 'linear-gradient(135deg,#bfe3ff,#6ec6e6)' },
  { e: '🍓', c: 'linear-gradient(135deg,#c6f0c2,#7fd4a8)' },
  { e: '⭐', c: 'linear-gradient(135deg,#fff3c4,#ffd54a)' }
]
// 转轮上的条带：把符号复制多遍，保证正向转很多圈也不越界
const REPS = 14
const STRIP = Array.from({ length: REPS * SYM.length }, (_, i) => SYM[i % SYM.length])

export default {
  name: 'GameSlotMachine',
  emits: ['score', 'over'],
  data() {
    return {
      STRIP,
      CELL: 72,
      spins: 8,               // 多转几圈再停
      reels: [
        { pos: 7, dur: 0 },    // 初始停在中间那段，保证首屏有糖块
        { pos: 14, dur: 0 },
        { pos: 21, dur: 0 }
      ],
      bet: 1,
      points: 0,
      pointsLoading: true,
      invested: 0,
      score: 0,                // 本局赢到的分（用于结算奖励）
      net: 0,                  // 净收益（赢 - 投入）
      spinning: false,
      stage: 'play',
      resultText: '选好赌注，先投入积分，再拉杆试手气～',
      resultKind: '',
      alive: true
    }
  },
  computed: {
    cost() { return Math.max(0, Math.floor(Number(this.betAmount) || 0)) },
    big() { return isBigBet(this.betAmount) }
  },
  async mounted() { await this.refreshPoints() },
  beforeUnmount() { this.alive = false },
  methods: {
    async refreshPoints() {
      this.pointsLoading = true
      this.points = (await fetchBalance()).value
      this.pointsLoading = false
      if (this.betAmount > this.points) this.clampBet()
    },
    clampBet() {
      let v = Math.floor(Number(this.betAmount) || 0)
      if (!Number.isFinite(v) || v < 10) v = 10
      if (v > this.points) v = Math.max(10, this.points)
      this.betAmount = v
    },
    start() {
      this.stage = 'play'; this.score = 0; this.invested = 0; this.net = 0
      this.refreshPoints()
      this.emitScore()
    },
    restart() { this.start() },

    sleep(ms) { return new Promise(r => setTimeout(r, ms)) },

    async spin() {
      if (this.spinning || this.points < this.cost) return
      this.clampBet()
      if (this.points < this.cost) return
      spend(this.cost)
      this.points = (await fetchBalance()).value
      this.invested += this.cost
      this.spinning = true
      this.resultKind = ''
      this.resultText = '叮铃铃——转起来！'
      sound.click()

      // 大注模式（≥5000）：30% 直接三星头奖；否则正常随机
      let stops
      if (isBigBet(this.betAmount) && Math.random() < 0.30) {
        const si = SYM.findIndex(s => s.e === '⭐')
        stops = [si, si, si]
      } else {
        stops = [0, 1, 2].map(() => Math.floor(Math.random() * SYM.length))
      }

      // 瞬间归位到靠前的起点（无动画），避免位置无限累加滑出空白区
      for (let i = 0; i < 3; i++) {
        this.reels[i].dur = 0
        this.reels[i].pos = 7 + Math.floor(Math.random() * SYM.length)
      }
      await this.sleep(20)

      for (let i = 0; i < 3; i++) {
        const target = stops[i]
        const base = this.reels[i].pos
        const aligned = base - (base % SYM.length)
        const pos = aligned + SYM.length * this.spins + target
        const dur = 700 + i * 420
        await new Promise(res => {
          setTimeout(() => {
            if (this.alive) { this.reels[i].pos = pos; this.reels[i].dur = dur }
            res()
          }, i * 260)
        })
      }
      await this.sleep(780)

      this.evaluate(stops)
      this.spinning = false
      this.emitScore()
    },

    evaluate(stops) {
      const [a, b, c] = stops.map(i => SYM[i].e)
      const hit = a === b && b === c
      const pair = a === b || b === c || a === c
      const M = this.betAmount
      let gain = 0, kind = '', text = ''

      if (hit) {
        if (a === '⭐') { gain = Math.round(M * 1.6); kind = 'jackpot'; text = '💫 三星连珠 · 超级大奖！+' + gain }
        else { gain = Math.round(M * 0.6); kind = 'win'; text = '🎉 三连 ' + a + ' · 大奖！+' + gain }
        sound.success()
      } else if (pair) {
        gain = Math.round(M * 0.2); kind = 'win'; text = '✨ 双连 · 小奖 +' + gain; sound.pop()
      } else if (stops.includes(SYM.findIndex(s => s.e === '⭐'))) {
        gain = Math.round(M * 0.08); kind = 'win'; text = '⭐ 落单星也被你捞到 +' + gain; sound.pop()
      } else {
        gain = 0; kind = 'lose'; text = '🙈 差一点～再投一把试试！'; sound.fail()
      }

      this.score += gain
      this.net = this.score - this.invested
      this.resultKind = kind
      this.resultText = text + '（已投入 ' + this.invested + '）'
    },

    emitScore() { this.$emit('score', this.score) }
  }
}
</script>

<style scoped>
.slot-wrap { display: flex; flex-direction: column; align-items: center; gap: 14px; }
.slot-hud { display: flex; gap: 14px; font-size: 12.5px; color: var(--muted); flex-wrap: wrap; justify-content: center; }
.slot-hud b { color: var(--primary); font-size: 14px; }
.slot-bets { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: center; }
.bet-input {
  width: 92px; height: 38px; border: none; border-radius: 10px; padding: 0 10px;
  background: var(--card); color: var(--text); font-weight: 800; font-size: 13px;
  box-shadow: inset 0 0 0 1.5px rgba(150,120,180,.25);
}
.bet-input:disabled { opacity: .55; }
.bet {
  min-width: 44px; height: 38px; border: none; border-radius: 10px; cursor: pointer;
  background: var(--panel); color: var(--text); font-weight: 700; font-size: 14px;
  box-shadow: inset 0 0 0 1.5px rgba(150,120,180,.2);
}
.bet.on { background: linear-gradient(135deg, #f7a8c4, #b79cf0); color: #fff; }
.bet-hint { font-size: 11.5px; color: var(--muted); }

.slot-machine {
  display: flex; gap: 10px; padding: 14px;
  background: linear-gradient(160deg, #fff0f6, #eef0ff);
  border-radius: 18px; box-shadow: 0 14px 36px rgba(150,110,180,.28);
}
.reel {
  width: 64px; height: 72px; overflow: hidden; border-radius: 12px;
  background: #fff; box-shadow: inset 0 6px 10px rgba(120,90,160,.16), inset 0 -6px 10px rgba(120,90,160,.16);
}
.reel-strip { will-change: transform; }
.cell {
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; line-height: 1;
}

.slot-result { min-height: 22px; font-weight: 700; font-size: 13.5px; text-align: center; }
.slot-result.win { color: #4caf7d; }
.slot-result.jackpot { color: #e08a2b; text-shadow: 0 0 10px rgba(224,138,43,.4); }
.slot-result.lose { color: var(--muted); }

.slot-spin {
  padding: 12px 30px; border: none; border-radius: 14px; cursor: pointer;
  font-size: 15px; font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #f7a8c4, #b79cf0);
  box-shadow: 0 8px 20px rgba(183,156,240,.4);
}
.slot-spin:disabled { opacity: .6; cursor: default; }
.slot-final { display: flex; align-items: center; gap: 12px; font-size: 14px; }
.slot-final .ghost, .slot-bets .ghost { padding: 8px 16px; border-radius: 10px; border: none; cursor: pointer; background: var(--panel); color: var(--text); }
</style>
