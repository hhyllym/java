<template>
  <div class="lc-wrap lk-wrap">
    <div class="lk-notice">🎮 只用游戏内虚拟积分 · 纯娱乐 · 概率全公开 · 大奖很难中，量力而行～</div>

    <div class="lk-hud">
      <span>💎 可用积分 <b>{{ points }}</b></span>
      <span>本局投入 <b>{{ invested }}</b></span>
      <span>本局赢得 <b>{{ won }}</b></span>
      <span>净收益 <b :class="net >= 0 ? 'up' : 'down'">{{ net >= 0 ? '+' : '' }}{{ net }}</b></span>
      <span>剩余 <b>{{ spinsLeft }}</b>/{{ ROUNDS }} 次</span>
    </div>

    <div class="lk-bets">
      <span class="lk-bets-label">投入</span>
      <input
        class="lk-bet-input" type="number" min="10" step="10"
        v-model.number="betAmount" @change="clampBet"
        :disabled="busy || finished || phase === 'choose'"
      />
      <span class="lk-bet-unit">积分</span>
      <button
        v-for="p in BET_PRESETS" :key="p"
        class="lk-bet" :class="{ on: betAmount === p }"
        :disabled="busy || finished || points < p" @click="betAmount = p"
      >{{ p >= 1000 ? (p / 1000) + 'k' : p }}</button>
      <span class="lk-bet-hint">每次投入 <b>{{ cost }}</b> 积分 · 想投多少投多少</span>
    </div>

    <!-- 16 张牌：1 张 ⭐ / 2 张 💎 / 3 张 🍬 / 10 张 ☁️ -->
    <div class="lc-board">
      <button
        v-for="(c, i) in cards" :key="i"
        class="lc-card"
        :class="{
          flipped: showAll,
          picked: picked === i,
          star: showAll && c.key === 'star',
          hit: showAll && picked === i && c.mult > 0
        }"
        :style="{ animationDelay: (i * 26) + 'ms' }"
        :disabled="phase !== 'choose' || busy"
        @click="pick(i)"
      >
        <span class="lc-back"><i>✦</i></span>
        <span class="lc-face" :style="{ background: c.color }">
          <span class="lc-badge">{{ c.emoji }}</span>
          <span class="lc-name">{{ c.name }}</span>
        </span>
      </button>
    </div>

    <p class="lk-result" :class="resultKind">{{ resultText }}</p>

    <button
      class="lk-play" :disabled="playDisabled"
      @click="primary"
    >{{ playLabel }}</button>

    <!-- 公开概率表 -->
    <div class="lk-odds">
      <div class="lk-odds-head">📋 公开概率表 <small v-if="big">（大注模式 · 头奖约 30%）</small><small v-else>（16 张牌里只有 1 张 ⭐，每次重新洗牌）</small></div>
      <div v-for="c in displayCards" :key="c.key" class="lk-odds-row">
        <span class="lk-chip" :style="{ background: c.color }">{{ c.emoji }} {{ c.name }} ×{{ c.count }} 张</span>
        <span>概率 <b>{{ c.pct }}</b></span>
        <span>奖金 <b>赌注 ×{{ c.mult }}</b></span>
      </div>
      <p class="lk-odds-foot"><template v-if="big">大注模式（≥5000）：头奖概率 <b>约 31%</b> · 整体返还约 <b>93.75%</b>，量力而行～</template><template v-else>整体返还约 <b>93.75%</b> · 头奖仅 <b>6.25%</b>，别上头 ⭐</template></p>
    </div>

    <div v-if="history.length" class="lk-history">
      <span class="lk-history-label">最近：</span>
      <span v-for="(h, i) in history" :key="i" class="lk-history-item" :class="{ win: h.gain > 0 }">
        {{ h.gain > 0 ? '+' + h.gain : '0' }}
      </span>
    </div>

    <div v-if="finished && !busy && phase === 'result'" class="lk-final">
      <b>本局净收益 {{ net >= 0 ? '+' : '' }}{{ net }} 积分</b>
      <button class="lk-final-btn" @click="restart">🔁 再来一局</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'
import { luckyBase, shuffle, isBigBet, BET_PRESETS } from './luckyBase'

/** 1 + 2 + 3 + 10 = 16 张；期望返还 (1*8 + 2*2 + 3*1)/16 = 93.75% */
const CARDS = [
  { key: 'star',  name: '星辉大奖', emoji: '⭐', mult: 8, count: 1,  pct: '6.25%',  color: '#ffd76a' },
  { key: 'gem',   name: '宝石好礼', emoji: '💎', mult: 2, count: 2,  pct: '12.5%',  color: '#cdb0f5' },
  { key: 'small', name: '糖豆小奖', emoji: '🍬', mult: 1, count: 3,  pct: '18.75%', color: '#ffb8d6' },
  { key: 'miss',  name: '谢谢参与', emoji: '☁️', mult: 0, count: 10, pct: '62.5%',  color: '#e6ecf8' }
]
const DECK = CARDS.flatMap(c => Array.from({ length: c.count }, () => c))

/** 大注模式（≥5000）：⭐ 牌变 5 张（约 30%），奖金倍数调到约 1.6，整体返还仍约 93.75% */
const BIG_CARDS = [
  { key: 'star',  name: '星辉大奖', emoji: '⭐', mult: 1.6, count: 5,  pct: '31%',    color: '#ffd76a' },
  { key: 'gem',   name: '宝石好礼', emoji: '💎', mult: 2,   count: 2,  pct: '12.5%',  color: '#cdb0f5' },
  { key: 'small', name: '糖豆小奖', emoji: '🍬', mult: 1,   count: 3,  pct: '18.75%', color: '#ffb8d6' },
  { key: 'miss',  name: '谢谢参与', emoji: '☁️', mult: 0,   count: 6,  pct: '37.5%',  color: '#e6ecf8' }
]

export default {
  name: 'GameLuckyCard',
  emits: ['score', 'over'],
  mixins: [luckyBase],
  data() {
    return {
      CARDS,
      phase: 'idle',     // idle | shuffle | choose | result
      cards: [],
      picked: -1,
      showAll: false,
      resultText: '投入积分洗牌，从 16 张里找出那颗 ⭐ 吧～',
      resultKind: ''
    }
  },
  computed: {
    displayCards() {
      return isBigBet(this.betAmount) ? BIG_CARDS : CARDS
    },
    playLabel() {
      if (this.pointsLoading) return '正在读取账户积分…'
      if (this.finished && this.phase === 'result') return '本局结束，点「再来一局」'
      if (this.phase === 'shuffle') return '洗牌中…'
      if (this.phase === 'choose') return '👆 点一张牌'
      if (this.phase === 'result') return '🔁 盖牌 · 下一把'
      if (this.points < this.cost) return '积分不足，先去赚点积分'
      return `🃏 投入 ${this.cost} 积分 · 洗牌`
    },
    playDisabled() {
      return this.busy || this.pointsLoading || this.finished ||
        this.phase === 'choose' || this.phase === 'shuffle' ||
        (this.phase === 'idle' && this.points < this.cost)
    }
  },
  methods: {
    primary() {
      if (this.phase === 'idle') this.deal()
      else if (this.phase === 'result') this.next()
    },
    async deal() {
      if (!(await this.placeBet())) return
      sound.click()
      this.phase = 'shuffle'
      this.picked = -1
      this.showAll = false
      this.resultKind = ''
      this.resultText = '唰唰唰——洗牌中…'
      // 强制重新生成 key，让发牌动画每次都重播
      this.cards = shuffle(this.displayCards.flatMap(c => Array.from({ length: c.count }, () => c))).map(c => ({ ...c }))
      await this.sleep(540)
      if (!this.alive) return
      this.phase = 'choose'
      this.busy = false
      this.resultText = '👆 点一张牌，找出藏起来的 ⭐ 吧！'
    },
    async pick(i) {
      if (this.phase !== 'choose' || this.busy) return
      this.busy = true
      this.picked = i
      this.showAll = true
      this.phase = 'result'
      const c = this.cards[i]
      const gain = this.cost * c.mult
      this.resultKind = gain > 0 ? (c.key === 'star' ? 'jackpot' : 'win') : 'lose'
      this.resultText = gain > 0
        ? `${c.emoji} 翻到${c.name}！赌注 ×${c.mult} → +${gain} 积分`
        : `☁️ 翻到谢谢参与～⭐ 其实藏在${this.starWhere()}，差一点点！`
      if (c.key === 'star') { sound.magic(); sound.success() }
      else if (gain > 0) sound.pop()
      else sound.fail()
      await this.settle(gain, c.name)
    },
    starWhere() {
      const i = this.cards.findIndex(c => c.key === 'star')
      return i >= 0 ? `第 ${i + 1} 张` : '牌堆里'
    },
    next() {
      if (this.finished) return
      this.phase = 'idle'
      this.showAll = false
      this.picked = -1
      this.cards = []
      this.resultKind = ''
      this.resultText = '再洗一把？选好赌注就开工～'
    },
    async restart() {
      await this.resetRound()
      this.phase = 'idle'
      this.showAll = false
      this.picked = -1
      this.cards = []
      this.resultKind = ''
      this.resultText = '新的一局开始，投入积分洗牌吧～'
    }
  }
}
</script>

<style scoped>
.lk-wrap { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.lk-notice {
  width: 100%; box-sizing: border-box; text-align: center;
  font-size: 11px; color: var(--muted); line-height: 1.6;
  background: var(--panel); border: 1px dashed var(--border);
  border-radius: 10px; padding: 7px 10px;
}
.lk-hud { display: flex; flex-wrap: wrap; gap: 6px 14px; justify-content: center; font-size: 12px; color: var(--muted); }
.lk-hud b { color: var(--primary); font-size: 14px; }
.lk-hud b.up { color: #4caf7d; }
.lk-hud b.down { color: #e08a6b; }
.lk-bets { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: center; }
.lk-bets-label { font-size: 12px; color: var(--muted); }
.lk-bet {
  min-width: 42px; height: 34px; border: none; border-radius: 10px; cursor: pointer;
  background: var(--panel); color: var(--text); font-weight: 800; font-size: 13px;
  box-shadow: inset 0 0 0 1.5px rgba(150,120,180,.2);
}
.lk-bet.on { background: linear-gradient(135deg, #f7a8c4, #b79cf0); color: #fff; }
.lk-bet:disabled { opacity: .55; cursor: not-allowed; }
.lk-bet-hint { font-size: 11.5px; color: var(--muted); }
.lk-bet-hint b { color: var(--primary); }
.lk-bet-input {
  width: 92px; height: 34px; border: none; border-radius: 10px; padding: 0 10px;
  background: var(--card); color: var(--text); font-weight: 800; font-size: 13px;
  box-shadow: inset 0 0 0 1.5px rgba(150,120,180,.25);
}
.lk-bet-input:disabled { opacity: .55; }
.lk-bet-unit { font-size: 12px; color: var(--muted); }

/* ---- 牌桌 ---- */
.lc-board {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
  width: 100%; max-width: 384px; padding: 12px; box-sizing: border-box;
  border-radius: 18px; background: linear-gradient(160deg, #fff0f6, #eef0ff);
  border: 1.5px solid var(--border); box-shadow: 0 10px 26px rgba(160,108,213,.18);
}
.lc-card {
  position: relative; aspect-ratio: 3 / 4; border: none; padding: 0; cursor: pointer;
  border-radius: 11px; background: transparent; perspective: 400px;
  animation: lcDeal .4s ease both;
}
@keyframes lcDeal {
  from { opacity: 0; transform: translateY(-10px) scale(.85); }
  to   { opacity: 1; transform: none; }
}
.lc-back, .lc-face {
  position: absolute; inset: 0; border-radius: 11px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5px;
  backface-visibility: hidden; transition: transform .45s ease;
  box-shadow: 0 3px 8px rgba(160,108,213,.18);
}
.lc-back {
  background: linear-gradient(145deg, #f7a8c4, #b79cf0); color: #fff;
  font-size: 22px; border: 2px solid #fff;
}
.lc-back i {
  width: 40px; height: 40px; border-radius: 50%; font-style: normal;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; border: 2px dashed rgba(255,255,255,.8);
  background: rgba(255,255,255,.16);
}
.lc-face { transform: rotateY(180deg); border: 2px solid #fff; }
.lc-badge {
  width: 46px; height: 46px; border-radius: 50%; line-height: 1;
  display: flex; align-items: center; justify-content: center; font-size: 30px;
  background: radial-gradient(circle at 34% 28%, #fff, #fff4fa);
  border: 2px solid #fff; box-shadow: 0 2px 7px rgba(120,90,160,.28);
}
.lc-name {
  font-size: 9.5px; font-weight: 800; color: #6a4a6a; white-space: nowrap;
  background: rgba(255,255,255,.88); border-radius: 999px; padding: 2px 7px;
}
.lc-card.star .lc-badge { box-shadow: 0 0 0 3px rgba(255,199,80,.8), 0 0 14px rgba(255,199,80,.8); }
.lc-card.flipped .lc-back { transform: rotateY(180deg); }
.lc-card.flipped .lc-face { transform: rotateY(0deg); }
.lc-card.picked { outline: 3px solid var(--primary); outline-offset: 2px; }
.lc-card.star { box-shadow: 0 0 0 3px #ffd76a, 0 0 16px rgba(255,199,80,.7); }
.lc-card.hit { animation: lcHit .5s ease both; }
@keyframes lcHit { 50% { transform: scale(1.12); } }
.lc-card:disabled { cursor: default; }

.lk-result {
  min-height: 22px; margin: 0; text-align: center;
  font-size: 13.5px; font-weight: 800; color: var(--muted);
}
.lk-result.win { color: #4caf7d; }
.lk-result.jackpot { color: #d98b1f; text-shadow: 0 0 10px rgba(255,199,80,.5); }
.lk-result.lose { color: var(--muted); }

.lk-play {
  padding: 11px 26px; border: none; border-radius: 999px; cursor: pointer;
  font-size: 14px; font-weight: 800; color: #fff;
  background: linear-gradient(135deg, #f7a8c4, #b79cf0);
  box-shadow: 0 7px 18px rgba(183,156,240,.42);
  transition: transform .12s, filter .12s;
}
.lk-play:hover:not(:disabled) { filter: brightness(1.06); transform: translateY(-1px); }
.lk-play:active { transform: scale(.96); }
.lk-play:disabled { opacity: .6; cursor: not-allowed; transform: none; }

/* ---- 公开概率表 ---- */
.lk-odds {
  width: 100%; max-width: 460px; box-sizing: border-box;
  padding: 10px 12px; border-radius: 14px;
  background: var(--panel); border: 1.5px solid var(--border);
}
.lk-odds-head { font-size: 11.5px; font-weight: 800; color: var(--primary); margin-bottom: 7px; }
.lk-odds-head small { font-weight: 500; color: var(--muted); }
.lk-odds-row {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 5px 0; font-size: 11px; color: var(--muted);
  border-top: 1px dashed var(--border);
}
.lk-odds-row b { color: var(--text); }
.lk-chip {
  min-width: 148px; text-align: left; padding: 3px 9px; border-radius: 999px;
  font-size: 10.5px; font-weight: 800; color: #6a4a6a;
}
.lk-odds-foot { margin: 8px 0 0; font-size: 10.5px; color: var(--muted); text-align: center; }
.lk-odds-foot b { color: var(--primary); }

.lk-history { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; justify-content: center; font-size: 10.5px; color: var(--muted); }
.lk-history-item { padding: 2px 8px; border-radius: 999px; background: var(--panel); border: 1px solid var(--border); font-weight: 800; }
.lk-history-item.win { color: #4caf7d; border-color: #a9e2cb; }

.lk-final {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap; justify-content: center;
  padding: 10px 16px; border-radius: 14px; font-size: 13.5px; color: var(--primary);
  background: linear-gradient(135deg, #fff0f7, #eef0ff); border: 1.5px solid var(--border);
}
.lk-final-btn {
  border: none; cursor: pointer; padding: 8px 16px; border-radius: 999px;
  background: var(--card); color: var(--primary); font-weight: 800; font-size: 12px;
  border: 1.5px solid var(--border);
}
@media (prefers-reduced-motion: reduce) {
  .lc-card { animation: none; }
  .lc-back, .lc-face { transition-duration: .01ms; }
}
</style>
