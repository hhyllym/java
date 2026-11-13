<template>
  <div class="wh-wrap lk-wrap">
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
        :disabled="busy || finished"
      />
      <span class="lk-bet-unit">积分</span>
      <button
        v-for="p in BET_PRESETS" :key="p"
        class="lk-bet" :class="{ on: betAmount === p }"
        :disabled="busy || finished || points < p" @click="betAmount = p"
      >{{ p >= 1000 ? (p / 1000) + 'k' : p }}</button>
      <span class="lk-bet-hint">每次投入 <b>{{ cost }}</b> 积分 · 想投多少投多少</span>
    </div>

    <!-- 转盘：16 格彩色扇区 + 顶部指针；结果按公开概率独立抽取，转盘只是演出 -->
    <div class="wh-stage">
      <div class="wh-pointer">▼</div>
      <div class="wh-wheel" :style="wheelStyle">
        <span
          v-for="(s, i) in SECTORS" :key="i"
          class="wh-label" :style="labelStyle(i)"
        >{{ s.emoji }}</span>
      </div>
      <div class="wh-hub" :class="{ hit: resultKind === 'jackpot' }">{{ hubEmoji }}</div>
    </div>

    <p class="lk-result" :class="resultKind">{{ resultText }}</p>

    <button
      class="lk-play" :disabled="busy || finished || pointsLoading || points < cost"
      @click="play"
    >{{ playLabel }}</button>

    <!-- 公开概率表 -->
    <div class="lk-odds">
      <div class="lk-odds-head">📋 公开概率表 <small v-if="big">（大注模式 · 头奖概率 30%）</small><small v-else>（16 格彩色扇区，每次独立抽取）</small></div>
      <div v-for="p in displayPrizes" :key="p.key" class="lk-odds-row">
        <span class="lk-chip" :style="{ background: p.color }">{{ p.emoji }} {{ p.name }}</span>
        <span>概率 <b>{{ p.pct }}</b></span>
        <span>奖金 <b>赌注 ×{{ p.mult }}</b></span>
      </div>
      <p class="lk-odds-foot">
        <template v-if="big">大注模式（≥5000）：头奖概率 <b>30%</b> · 整体返还约 <b>95%</b>，量力而行～</template>
        <template v-else>整体返还约 <b>87.5%</b> · 头奖仅 <b>0.6%</b>，别上头 ⭐</template>
      </p>
    </div>

    <div v-if="history.length" class="lk-history">
      <span class="lk-history-label">最近：</span>
      <span v-for="(h, i) in history" :key="i" class="lk-history-item" :class="{ win: h.gain > 0 }">
        {{ h.gain > 0 ? '+' + h.gain : '0' }}
      </span>
    </div>

    <div v-if="finished && !busy" class="lk-final">
      <b>本局净收益 {{ net >= 0 ? '+' : '' }}{{ net }} 积分</b>
      <button class="lk-final-btn" @click="restart">🔁 再来一局</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'
import { luckyBase, pickWeighted, isBigBet, applyBigBet, BET_PRESETS } from './luckyBase'

/** 权重合计 1000 → 期望返还 (6*50+25*7+90*2+220*1)/1000 = 87.5% */
const PRIZES = [
  { key: 'jackpot', name: '星辉大奖',   emoji: '⭐', mult: 50, w: 6,   pct: '0.6%',  color: '#ffd76a' },
  { key: 'big',     name: '宝石二等奖', emoji: '💎', mult: 7,  w: 25,  pct: '2.5%',  color: '#cdb0f5' },
  { key: 'small',   name: '糖豆三等奖', emoji: '🍬', mult: 2,  w: 90,  pct: '9%',    color: '#ffb8d6' },
  { key: 'consol',  name: '安慰奖',     emoji: '🧁', mult: 1,  w: 150, pct: '15%',   color: '#a9e2cb' },
  { key: 'lucky',   name: '糖果幸运奖', emoji: '🍭', mult: 1,  w: 70,  pct: '7%',    color: '#ffd0e0' },
  { key: 'miss',    name: '谢谢参与',   emoji: '☁️', mult: 0,  w: 659, pct: '65.9%', color: '#e6ecf8' }
]
/** 20 格布局：1 头奖 / 1 二等 / 3 三等 / 3 安慰 / 3 糖果幸运 / 9 未中，视觉上「大奖格很少」 */
const LAYOUT = ['jackpot', 'miss', 'consol', 'big', 'miss', 'lucky', 'small', 'miss', 'miss', 'consol', 'small', 'miss', 'lucky', 'consol', 'miss', 'small', 'miss', 'miss', 'lucky', 'miss']
const SECTORS = LAYOUT.map(k => PRIZES.find(p => p.key === k))
const SPIN_MS = 2600

export default {
  name: 'GameLuckyWheel',
  emits: ['score', 'over'],
  mixins: [luckyBase],
  data() {
    return {
      PRIZES, SECTORS,
      angle: 0,
      dur: 0,
      resultText: '选好赌注，投入积分，转一次试试手气～',
      resultKind: '',
      hubEmoji: '🎡',
      lastGain: 0
    }
  },
  computed: {
    displayPrizes() {
      return isBigBet(this.betAmount) ? applyBigBet(PRIZES, 'jackpot') : PRIZES
    },
    wheelStyle() {
      const step = 360 / SECTORS.length
      const stops = SECTORS.map((s, i) => `${s.color} ${i * step}deg ${(i + 1) * step}deg`)
      return {
        background: `conic-gradient(from 0deg, ${stops.join(',')})`,
        transform: `rotate(${this.angle}deg)`,
        transitionDuration: this.dur + 'ms'
      }
    },
    playLabel() {
      if (this.pointsLoading) return '正在读取账户积分…'
      if (this.busy) return '转动中…'
      if (this.finished) return '本局次数已用完'
      if (this.points < this.cost) return '积分不足，先去赚点积分'
      return `🎡 投入 ${this.cost} 积分 · 转一次`
    }
  },
  methods: {
    labelStyle(i) {
      const step = 360 / SECTORS.length
      const deg = i * step + step / 2
      return { transform: `rotate(${deg}deg) translateY(-74px) rotate(${-deg}deg)` }
    },
    /** 让第 idx 格的中心对准顶部指针，并额外多转 5 圈 */
    aimAt(idx) {
      const cur = ((this.angle % 360) + 360) % 360
      const step = 360 / SECTORS.length
      const target = ((-(idx * step + step / 2)) % 360 + 360) % 360
      const delta = ((target - cur) % 360 + 360) % 360
      this.dur = SPIN_MS
      this.angle = this.angle + 360 * 5 + delta
    },
    async play() {
      if (!(await this.placeBet())) return
      sound.click()
      const prize = pickWeighted(this.displayPrizes)
      // 在转盘上找一个该类型的格子停住（视觉与结果一致）
      const candidates = SECTORS.map((s, i) => (s.key === prize.key ? i : -1)).filter(i => i >= 0)
      const idx = candidates[Math.floor(Math.random() * candidates.length)]
      this.hubEmoji = '🎡'
      this.resultKind = ''
      this.resultText = '叮——转盘转起来啦！'
      this.aimAt(idx)
      await this.sleep(SPIN_MS + 120)
      if (!this.alive) return

      const gain = this.cost * prize.mult
      this.lastGain = gain
      this.hubEmoji = prize.emoji
      this.resultKind = prize.key === 'miss' ? 'lose' : (prize.key === 'jackpot' ? 'jackpot' : 'win')
      this.resultText = gain > 0
        ? `${prize.emoji} ${prize.name}！赌注 ×${prize.mult} → +${gain} 积分`
        : '☁️ 谢谢参与，差一点点～再试一次？'
      if (prize.key === 'jackpot') { sound.magic(); sound.success() }
      else if (gain > 0) sound.pop()
      else sound.fail()
      await this.settle(gain, prize.name)
    },
    async restart() {
      await this.resetRound()
      this.angle = 0
      this.dur = 0
      this.hubEmoji = '🎡'
      this.resultKind = ''
      this.resultText = '新的一局开始，选好赌注再转～'
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

/* ---- 转盘 ---- */
.wh-stage { position: relative; width: 226px; height: 226px; display: flex; align-items: center; justify-content: center; }
.wh-pointer {
  position: absolute; top: -6px; left: 50%; transform: translateX(-50%);
  font-size: 22px; color: var(--pink); z-index: 3;
  filter: drop-shadow(0 2px 3px rgba(160,108,213,.4));
}
.wh-wheel {
  position: absolute; inset: 0; border-radius: 50%;
  border: 6px solid #fff; box-shadow: 0 10px 28px rgba(160,108,213,.28);
  transition-property: transform; transition-timing-function: cubic-bezier(.15,.75,.2,1);
  will-change: transform;
}
.wh-label {
  position: absolute; top: 50%; left: 50%; width: 26px; height: 26px;
  margin: -13px 0 0 -13px; font-size: 16px; line-height: 26px; text-align: center;
  border-radius: 50%; background: rgba(255,255,255,.92);
  box-shadow: 0 1px 5px rgba(120,90,160,.3);
}
.wh-hub {
  position: relative; z-index: 2; width: 62px; height: 62px; border-radius: 50%;
  background: #fff; border: 3px solid var(--border);
  box-shadow: 0 4px 12px rgba(160,108,213,.25);
  display: flex; align-items: center; justify-content: center; font-size: 30px;
  transition: transform .3s ease;
}
.wh-hub.hit { transform: scale(1.12); box-shadow: 0 0 0 6px rgba(255,215,106,.5); }

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
  min-width: 128px; text-align: left; padding: 3px 9px; border-radius: 999px;
  font-size: 10.5px; font-weight: 800; color: #6a4a6a;
}
.lk-odds-foot { margin: 8px 0 0; font-size: 10.5px; color: var(--muted); text-align: center; }
.lk-odds-foot b { color: var(--primary); }

.lk-history { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; justify-content: center; font-size: 10.5px; color: var(--muted); }
.lk-history-item {
  padding: 2px 8px; border-radius: 999px; background: var(--panel);
  border: 1px solid var(--border); font-weight: 800;
}
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
  .wh-wheel { transition-duration: .01ms !important; }
}
</style>
