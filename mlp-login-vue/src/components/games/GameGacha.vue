<template>
  <div class="gc-wrap lk-wrap">
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

    <!-- 扭蛋机 -->
    <div class="gc-stage">
      <div class="gc-globe">
        <i v-for="(b, i) in BUBBLES" :key="i" class="gc-bubble" :style="b"></i>
      </div>
      <div class="gc-base"><span class="gc-knob" :class="{ turning: phase === 'rolling' }">✦</span></div>
      <div class="gc-egg" :class="{ rolling: phase === 'rolling', opened: phase === 'opened' }">
        <div class="gc-shell top" :style="current ? { background: current.color } : {}"></div>
        <div class="gc-shell bottom" :style="current ? { background: current.color } : {}"></div>
        <div class="gc-prize">
          <span class="gc-prize-badge" :class="{ ssr: !!(current && current.key === 'ssr') }">{{ current ? current.emoji : '❔' }}</span>
        </div>
      </div>
    </div>

    <div v-if="phase === 'opened' && current" class="gc-prize-name" :style="{ background: current.color }">
      {{ current.emoji }} {{ current.name }} · 赌注 ×{{ current.mult }}
    </div>

    <p class="lk-result" :class="resultKind">{{ resultText }}</p>

    <button
      class="lk-play" :disabled="busy || finished || pointsLoading || points < cost"
      @click="play"
    >{{ playLabel }}</button>

    <!-- 收集图鉴 -->
    <div class="gc-collect">
      <div class="gc-collect-head">📖 扭蛋图鉴 · 已收集 <b>{{ collected.length }}</b>/{{ RARITY.length }}</div>
      <div class="gc-collect-row">
        <span
          v-for="r in RARITY" :key="r.key"
          class="gc-collect-chip" :class="{ got: collected.includes(r.key) }"
          :style="collected.includes(r.key) ? { background: r.color } : {}"
        >{{ collected.includes(r.key) ? r.emoji + ' ' + r.name : '❔ 未解锁' }}</span>
      </div>
    </div>

    <!-- 公开概率表 -->
    <div class="lk-odds">
      <div class="lk-odds-head">📋 公开概率表 <small v-if="big">（大注模式 · 至臻款概率 30%）</small><small v-else>（每次独立抽取，抽到什么款式就领对应积分）</small></div>
      <div v-for="r in displayRarity" :key="r.key" class="lk-odds-row">
        <span class="lk-chip" :style="{ background: r.color }">{{ r.emoji }} {{ r.name }}</span>
        <span>概率 <b>{{ r.pct }}</b></span>
        <span>奖金 <b>赌注 ×{{ r.mult }}</b></span>
      </div>
      <p class="lk-odds-foot"><template v-if="big">大注模式（≥5000）：至臻款概率 <b>30%</b> · 整体返还约 <b>95%</b>，量力而行～</template><template v-else>整体返还约 <b>89.8%</b> · 至臻款仅 <b>1%</b>，别上头 ⭐</template></p>
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

const COLLECT_KEY = 'mlp_gacha_collection'

/** 权重合计 1000 → 期望返还 (10*20+50*5+200*1.5+740*0.2)/1000 = 89.8% */
const RARITY = [
  { key: 'ssr', name: '星光至臻', emoji: '⭐', mult: 20,  w: 10,  pct: '1%',  color: '#ffd76a' },
  { key: 'sr',  name: '稀有宝石', emoji: '💎', mult: 5,   w: 50,  pct: '5%',  color: '#cdb0f5' },
  { key: 'r',   name: '糖豆款',   emoji: '🍬', mult: 1.5, w: 200, pct: '20%', color: '#ffb8d6' },
  { key: 'n',   name: '普通款',   emoji: '🧁', mult: 0.2, w: 740, pct: '74%', color: '#a9e2cb' }
]
/** 玻璃罩里的彩色小球，纯装饰 */
const BUBBLES = [
  { left: '18%', top: '58%', background: '#ff9ec4' },
  { left: '36%', top: '70%', background: '#8fd0ee' },
  { left: '55%', top: '62%', background: '#ffd76a' },
  { left: '72%', top: '72%', background: '#b79cf0' },
  { left: '28%', top: '76%', background: '#a9e2cb' },
  { left: '62%', top: '78%', background: '#ffb8d6' },
  { left: '46%', top: '80%', background: '#cdb0f5' }
]
const ROLL_MS = 900

function loadCollected() {
  try {
    const a = JSON.parse(localStorage.getItem(COLLECT_KEY) || '[]')
    return Array.isArray(a) ? a : []
  } catch (e) { return [] }
}

export default {
  name: 'GameGacha',
  emits: ['score', 'over'],
  mixins: [luckyBase],
  data() {
    return {
      RARITY, BUBBLES, BET_PRESETS,
      phase: 'idle',       // idle | rolling | opened
      current: null,
      collected: loadCollected(),
      resultText: '投入积分，扭一颗看看能开出什么款～',
      resultKind: ''
    }
  },
  computed: {
    displayRarity() {
      return isBigBet(this.betAmount) ? applyBigBet(RARITY, 'ssr') : RARITY
    },
    playLabel() {
      if (this.pointsLoading) return '正在读取账户积分…'
      if (this.busy) return '咕噜咕噜…'
      if (this.finished) return '本局次数已用完'
      if (this.points < this.cost) return '积分不足，先去赚点积分'
      return `🎰 投入 ${this.cost} 积分 · 扭一次`
    }
  },
  methods: {
    async play() {
      if (!(await this.placeBet())) return
      sound.click()
      this.phase = 'rolling'
      this.current = null
      this.resultKind = ''
      this.resultText = '咕噜咕噜——扭蛋滚出来啦…'
      await this.sleep(ROLL_MS)
      if (!this.alive) return

      const r = pickWeighted(this.displayRarity)
      this.current = r
      this.phase = 'opened'
      const gain = Math.round(this.cost * r.mult)
      this.resultKind = r.key === 'ssr' ? 'jackpot' : (r.key === 'n' ? 'lose' : 'win')
      this.resultText = `${r.emoji} 开出「${r.name}」！赌注 ×${r.mult} → +${gain} 积分`
      this.collect(r.key)
      if (r.key === 'ssr') { sound.magic(); sound.success() }
      else if (r.key === 'sr') sound.success()
      else sound.pop()
      await this.settle(gain, r.name)
    },
    collect(key) {
      if (this.collected.includes(key)) return
      this.collected.push(key)
      try { localStorage.setItem(COLLECT_KEY, JSON.stringify(this.collected)) } catch (e) {}
    },
    async restart() {
      await this.resetRound()
      this.phase = 'idle'
      this.current = null
      this.resultKind = ''
      this.resultText = '新的一局开始，再扭一颗吧～'
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

/* ---- 扭蛋机 ---- */
.gc-stage {
  position: relative; width: 210px; height: 220px;
  display: flex; flex-direction: column; align-items: center; justify-content: flex-start;
  padding-top: 10px; box-sizing: border-box;
  border-radius: 20px; background: linear-gradient(160deg, #fff0f6, #eef0ff);
  border: 1.5px solid var(--border); box-shadow: 0 10px 26px rgba(160,108,213,.2);
}
.gc-globe {
  position: relative; width: 152px; height: 152px; border-radius: 50%;
  background: radial-gradient(circle at 34% 28%, rgba(255,255,255,.95), rgba(232,240,255,.55) 55%, rgba(210,225,250,.5));
  border: 5px solid #fff; box-shadow: inset 0 6px 16px rgba(160,108,213,.18), 0 6px 16px rgba(160,108,213,.22);
  overflow: hidden;
}
.gc-bubble {
  position: absolute; width: 24px; height: 24px; border-radius: 50%;
  transform: translate(-50%, -50%); opacity: .85;
  box-shadow: inset -3px -4px 6px rgba(120,90,160,.18), 0 2px 4px rgba(120,90,160,.18);
}
.gc-base {
  margin-top: 8px; width: 130px; height: 30px; border-radius: 10px 10px 14px 14px;
  background: linear-gradient(135deg, #f7a8c4, #b79cf0);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 5px 12px rgba(183,156,240,.35);
}
.gc-knob {
  width: 24px; height: 24px; border-radius: 50%; background: #fff;
  color: var(--primary); font-size: 14px; line-height: 24px; text-align: center;
}
.gc-knob.turning { animation: gcTurn .3s linear infinite; }
@keyframes gcTurn { to { transform: rotate(360deg); } }

.gc-egg {
  position: absolute; left: 50%; top: 58px; width: 78px; height: 88px;
  margin-left: -39px; z-index: 2;
}
.gc-egg.rolling { animation: gcShake .22s ease-in-out infinite alternate; }
@keyframes gcShake { to { transform: translateY(6px) rotate(7deg); } }
.gc-shell {
  position: absolute; left: 0; width: 78px; height: 44px;
  background: linear-gradient(135deg, #ffd1e3, #b79cf0);
  border: 2px solid #fff; box-shadow: 0 3px 8px rgba(160,108,213,.25);
  transition: transform .45s cubic-bezier(.2,.8,.3,1);
}
.gc-shell.top { top: 0; border-radius: 39px 39px 6px 6px; border-bottom: none; }
.gc-shell.bottom { bottom: 0; border-radius: 6px 6px 39px 39px; border-top: none; }
.gc-egg.opened .gc-shell.top { transform: translateY(-30px) rotate(-22deg); }
.gc-prize {
  position: absolute; inset: 0; z-index: 3;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transform: scale(.5);
  transition: opacity .35s ease .1s, transform .35s ease .1s;
}
.gc-prize-badge {
  width: 62px; height: 62px; border-radius: 50%; line-height: 1;
  display: flex; align-items: center; justify-content: center; font-size: 36px;
  background: radial-gradient(circle at 34% 28%, #fff, #fff4fa);
  border: 3px solid #fff;
  box-shadow: 0 0 0 4px rgba(255,255,255,.55), 0 6px 16px rgba(160,108,213,.45);
  animation: gcPop .45s ease both;
}
.gc-prize-badge.ssr {
  box-shadow: 0 0 0 5px rgba(255,215,106,.8), 0 0 24px 8px rgba(255,199,80,.75);
}
@keyframes gcPop { from { transform: scale(.4) rotate(-12deg); } to { transform: none; } }
.gc-prize-name {
  padding: 5px 16px; border-radius: 999px; font-size: 12px; font-weight: 800;
  color: #6a4a6a; border: 2px solid #fff;
  box-shadow: 0 3px 10px rgba(160,108,213,.25);
  animation: gcPop .45s ease both;
}
.gc-egg.opened .gc-prize, .gc-egg:not(.opened):not(.rolling) .gc-prize { opacity: 1; transform: none; }
.gc-egg.rolling .gc-prize { opacity: 0; }

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

/* ---- 图鉴 ---- */
.gc-collect {
  width: 100%; max-width: 460px; box-sizing: border-box;
  padding: 9px 12px; border-radius: 14px;
  background: var(--panel); border: 1.5px solid var(--border);
}
.gc-collect-head { font-size: 11.5px; font-weight: 800; color: var(--primary); margin-bottom: 7px; }
.gc-collect-head b { color: var(--pink); }
.gc-collect-row { display: flex; flex-wrap: wrap; gap: 6px; }
.gc-collect-chip {
  padding: 4px 10px; border-radius: 999px; font-size: 10.5px; font-weight: 800;
  color: var(--muted); background: var(--card); border: 1.5px dashed var(--border);
}
.gc-collect-chip.got { color: #6a4a6a; border: 1.5px solid #fff; }

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
  .gc-egg.rolling, .gc-knob.turning, .gc-prize-badge, .gc-prize-name { animation: none; }
  .gc-shell, .gc-prize { transition-duration: .01ms; }
}
</style>
