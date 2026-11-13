<template>
  <div class="race-wrap">
    <!-- 顶部信息条：所有阶段共用 -->
    <div class="race-hud">
      <div class="hud-chip">
        <span>💎 我的积分</span><b>{{ points }}</b>
      </div>
      <div class="hud-chip">
        <span>🎯 本局得分</span><b>{{ score }}</b>
      </div>
      <div class="hud-chip">
        <span>{{ stage === 'run' ? '🏅 当前名次' : '🏁 状态' }}</span>
        <b>{{ stage === 'run' ? '第 ' + myRank + ' 名' : stageText }}</b>
      </div>
    </div>

    <!-- ① 报名阶段 -->
    <div v-if="stage === 'ready'" class="race-stage ready-stage">
      <div class="race-mascot">🏇</div>
      <h3>云驰冲刺赛</h3>
      <p class="race-lead">交 <b>{{ FEE }}</b> 积分报名，和三位小马抢头名！</p>

      <div class="prize-row">
        <div class="prize-card gold"><span>🥇</span><b>+{{ PRIZE[0] }}</b><small>冠军</small></div>
        <div class="prize-card silver"><span>🥈</span><b>+{{ PRIZE[1] }}</b><small>亚军</small></div>
        <div class="prize-card bronze"><span>🥉</span><b>+{{ PRIZE[2] }}</b><small>季军</small></div>
      </div>

      <ul class="race-tips">
        <li><i>🏇</i><span>点一下冲一下，<b>节奏比手速更重要</b></span></li>
        <li><i>⚡</i><span>体力见底会变慢，喘口气再冲更稳</span></li>
        <li><i>💎</i><span>前三有奖，没进前三报名费不退哦</span></li>
      </ul>

      <button class="race-btn big" :disabled="pointsLoading || points < FEE" @click="join">
        {{ pointsLoading ? '正在读取账户积分…' : (points < FEE ? '积分不够报名（还差 ' + (FEE - points) + '）' : '🏇 交 ' + FEE + ' 积分，开跑！') }}
      </button>
      <p class="race-fine">按 <kbd>空格</kbd> 也能冲刺 · 手机直接点按钮</p>
    </div>

    <!-- ② 起跑倒计时 -->
    <div v-else-if="stage === 'countdown'" class="race-stage countdown-stage">
      <div class="race-track">
        <div v-for="(r, i) in racers" :key="i" class="lane" :class="{ me: r.me }">
          <span class="lane-rank">{{ i + 1 }}</span>
          <span class="lane-name">{{ r.me ? '我' : r.name }}</span>
          <div class="lane-bar">
            <span class="lane-pony" :style="{ left: '5%' }">{{ r.icon }}</span>
            <span class="finish-flag">🏁</span>
          </div>
          <span class="lane-pct">0%</span>
        </div>
      </div>
      <div class="countdown-mask">
        <div :key="countdown" class="countdown-num">{{ countdown > 0 ? countdown : '冲呀！' }}</div>
        <div class="countdown-tip">各就位 · 预备…</div>
      </div>
    </div>

    <!-- ③ 比赛进行中 -->
    <div v-else-if="stage === 'run'" class="race-stage run-stage">
      <div class="status-line">
        <span class="status-pill">{{ statusText }}</span>
        <span v-if="combo >= 3" class="combo-pill">🔥 {{ combo }} 连击</span>
        <span class="dist-pill">🏁 还差 {{ Math.max(0, Math.ceil(FINISH - me.pos)) }} 米</span>
      </div>

      <div class="energy-row">
        <span class="energy-label">⚡ 体力</span>
        <div class="energy-bar"><i :class="{ low: energy < 30 }" :style="{ width: energy + '%' }"></i></div>
        <b :class="{ low: energy < 30 }">{{ Math.round(energy) }}%</b>
      </div>

      <div class="race-track">
        <div
          v-for="(r, i) in racers"
          :key="i"
          class="lane"
          :class="{ me: r.me, leader: rankOf(r) === 1 && r.pos > 8 }"
        >
          <span class="lane-rank" :class="{ top: rankOf(r) <= 3 }">{{ rankOf(r) }}</span>
          <span class="lane-name">{{ r.me ? '我' : r.name }}</span>
          <div class="lane-bar">
            <span class="lane-stripes"></span>
            <span
              class="lane-pony running"
              :class="{ fast: r.me && me.v > 0.7, tired: r.me && energy < 30 }"
              :style="{ left: (4 + r.pos * 0.9) + '%', animationDelay: (i * -0.17) + 's' }"
            >{{ r.icon }}</span>
            <span class="finish-flag">🏁</span>
          </div>
          <span class="lane-pct">{{ Math.round(r.pos) }}%</span>
        </div>
      </div>

      <div class="race-progress"><span :style="{ width: me.pos + '%' }"></span></div>

      <button
        ref="dashBtn"
        class="race-btn dash"
        :class="{ tapped: tapped, tired: energy < 30 }"
        @pointerdown.prevent="push"
      >
        <span class="dash-emoji">🏇</span>
        <span class="dash-main">冲刺！</span>
        <span class="dash-sub">{{ energy < 30 ? '喘口气，缓一下～' : '狂点我 · 空格键也行' }}</span>
        <span
          v-for="f in flashes"
          :key="f.id"
          class="flash"
          :style="{ left: f.x + '%' }"
        >{{ f.text }}</span>
      </button>
    </div>

    <!-- ④ 结算 -->
    <div v-else class="race-stage done-stage">
      <div class="race-mascot">{{ medal }}</div>
      <h3>{{ rankTitle }}</h3>
      <p class="done-sub">第 {{ rank }} 名 · 奖金 +{{ prize }} · 报名费 -{{ FEE }}</p>
      <div class="net-line">
        净收益
        <b :class="{ neg: prize - FEE < 0 }">{{ prize - FEE >= 0 ? '+' : '' }}{{ prize - FEE }}</b>
        积分
      </div>

      <div class="final-board">
        <div v-for="(r, i) in finalOrder" :key="i" class="board-row" :class="{ me: r.me }">
          <span class="board-rank">{{ ['🥇', '🥈', '🥉', '4'][i] }}</span>
          <span class="board-icon">{{ r.icon }}</span>
          <b>{{ r.me ? '我' : r.name }}</b>
          <em>{{ Math.round(r.pos) }} 米</em>
        </div>
      </div>

      <button class="race-btn big" @click="reset">🔁 再来一局</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'
import { fetchBalance, spend, earn } from '../../wallet'

const FEE = 20
const PRIZE = [80, 45, 25, 0]          // 冠 / 亚 / 季 / 落选
const RIVALS = [
  { name: '云驰', icon: '🦄', speed: 1.06 },
  { name: '金穗', icon: '🍎', speed: 0.95 },
  { name: '茸茸', icon: '🦋', speed: 0.86 }
]
const FINISH = 100                     // 冲线距离（米）
const ENERGY_MAX = 100
const TAP_COST = 5                     // 每次冲刺消耗体力
const ENERGY_RECOVER = 1.2             // 每个 tick 自然恢复（每秒约 20 点）

export default {
  name: 'GameRace',
  emits: ['score', 'over'],
  data() {
    return {
      FEE, PRIZE, FINISH,
      stage: 'ready',
      points: 0,
      pointsLoading: true,
      score: 0,
      racers: [],
      finalOrder: [],
      energy: ENERGY_MAX,
      countdown: 3,
      combo: 0,
      flashes: [],
      tapped: false,
      rank: 4,
      prize: 0,
      timer: null,
      cdTimer: null,
      flashTimers: [],
      tapTimer: null,
      lastTap: 0,
      flashId: 0,
      alive: true
    }
  },
  computed: {
    me() {
      return this.racers.find(r => r.me) || { pos: 0, v: 0 }
    },
    myRank() {
      return this.rankOf(this.me)
    },
    stageText() {
      return { ready: '待报名', countdown: '准备起跑', done: '已完赛' }[this.stage] || '待报名'
    },
    statusText() {
      if (this.me.pos > 82) return '最后冲刺！🔥'
      if (this.energy < 30) return '有点喘…缓一下 ⛅'
      if (this.me.v > 0.7) return '全速冲刺中！✨'
      return '稳住节奏～'
    },
    medal() {
      return ['💦', '🏆', '🥈', '🥉'][this.rank] || '💦'
    },
    rankTitle() {
      return [
        '', '冠军！全场都在喊你的名字 🎉',
        '亚军，差一点点就翻盘！',
        '季军，也有奖金拿～',
        '没进前三……下次注意节奏！'
      ][this.rank] || '比赛结束'
    }
  },
  mounted() {
    this.refreshPoints()
    window.addEventListener('keydown', this.onKey)
  },
  beforeUnmount() {
    this.alive = false
    clearInterval(this.timer)
    clearTimeout(this.cdTimer)
    clearTimeout(this.tapTimer)
    this.flashTimers.forEach(t => clearTimeout(t))
    window.removeEventListener('keydown', this.onKey)
  },
  methods: {
    async refreshPoints() {
      this.pointsLoading = true
      const r = await fetchBalance()
      this.points = r.value
      this.pointsLoading = false
    },
    rankOf(r) {
      return [...this.racers].sort((a, b) => b.pos - a.pos).findIndex(x => x === r) + 1
    },

    async join() {
      if (this.points < FEE) return
      spend(FEE)
      this.points = (await fetchBalance()).value
      sound.click()

      this.racers = [
        { name: '我', icon: '🏇', me: true, pos: 0, v: 0, energy: ENERGY_MAX },
        ...RIVALS.map(r => ({ ...r, me: false, pos: 0, v: 0 }))
      ]
      this.score = 0
      this.energy = ENERGY_MAX
      this.combo = 0
      this.flashes = []
      this.finalOrder = []
      this.lastTap = 0
      this.stage = 'countdown'
      this.countdown = 3

      // 3 → 2 → 1 → 冲呀！
      const step = () => {
        if (!this.alive) return
        this.countdown -= 1
        if (this.countdown > 0) {
          sound.hover()
          this.cdTimer = setTimeout(step, 700)
        } else {
          sound.pop()
          this.cdTimer = setTimeout(() => { if (this.alive) this.startRun() }, 560)
        }
      }
      this.cdTimer = setTimeout(step, 700)
    },

    startRun() {
      this.stage = 'run'
      // 一个 tick 同时推进四条道；玩家靠点击注入「冲量」，AI 匀速带轻微抖动
      this.timer = setInterval(() => {
        if (!this.alive) return clearInterval(this.timer)
        let done = false
        for (const r of this.racers) {
          if (r.me) {
            r.energy = Math.min(ENERGY_MAX, r.energy + ENERGY_RECOVER)
            r.v *= 0.82                                 // 冲量自然衰减
            if (r.energy < 30) r.v = Math.min(r.v, 0.6) // 气喘：压住速度，狂点反而慢
            r.pos = Math.min(FINISH, r.pos + (0.40 + r.v) * 0.62)
          } else {
            r.pos = Math.min(FINISH, r.pos + r.speed * 0.62 * (0.9 + Math.random() * 0.22))
          }
          if (r.pos >= FINISH) done = true
        }
        this.energy = this.me.energy
        if (done) this.finish()
      }, 60)
    },

    push() {
      if (this.stage !== 'run' || !this.alive) return
      const me = this.racers[0]
      if (!me) return

      // 节奏判定：太急的乱点不涨连击，还额外掉体力
      const now = performance.now()
      const gap = now - this.lastTap
      this.lastTap = now
      let bonus = 0
      if (gap < 85) {
        me.energy = Math.max(0, me.energy - 3)
        this.combo = 0
        this.addFlash('太急啦～')
      } else if (gap < 520) {
        this.combo += 1
        bonus = Math.min(0.12, this.combo * 0.02)     // 稳住节奏的小奖励
        this.addFlash(this.combo >= 3 ? '✨ ' + this.combo + ' 连击' : '+ 冲刺')
      } else {
        this.combo = 1
        this.addFlash('+ 冲刺')
      }

      if (me.energy < TAP_COST) {
        this.energy = me.energy
        this.addFlash('喘口气～')
        sound.hover()
        return
      }
      me.energy = Math.max(0, me.energy - TAP_COST)
      this.energy = me.energy
      const eff = me.energy < 30 ? 0.35 : 1           // 体力见底，冲刺大幅打折
      me.v = Math.min(1.8, me.v + (0.40 * eff) + bonus)
      sound.hover()

      // 按钮按下的即时反馈
      this.tapped = true
      clearTimeout(this.tapTimer)
      this.tapTimer = setTimeout(() => { this.tapped = false }, 110)
    },

    addFlash(text) {
      const id = ++this.flashId
      this.flashes.push({ id, text, x: 18 + Math.random() * 60 })
      const t = setTimeout(() => {
        this.flashes = this.flashes.filter(f => f.id !== id)
      }, 720)
      this.flashTimers.push(t)
    },

    onKey(e) {
      if (this.stage !== 'run') return
      if (e.code === 'Space' || e.key === ' ' || e.key === 'Enter') {
        e.preventDefault()
        this.push()
      }
    },

    async finish() {
      clearInterval(this.timer)
      const sorted = [...this.racers].sort((a, b) => b.pos - a.pos)
      this.finalOrder = sorted.map(r => ({ ...r }))
      this.rank = sorted.findIndex(r => r.me) + 1
      this.prize = PRIZE[this.rank - 1] || 0
      this.score = Math.max(0, this.prize - FEE)
      if (this.prize > 0) earn(this.prize)
      // 先把结算页渲染出来，再异步刷账户余额，避免网络拖慢结算
      this.stage = 'done'
      this.emitScore()
      this.$emit('over', this.score)
      if (this.rank === 1) sound.success()
      else if (this.prize > 0) sound.pop()
      else sound.fail()
      this.points = (await fetchBalance()).value
    },

    reset() {
      this.stage = 'ready'
      this.score = 0
      this.energy = ENERGY_MAX
      this.combo = 0
      this.flashes = []
      this.racers = []
      this.finalOrder = []
      this.refreshPoints()
      this.emitScore()
      sound.click()
    },
    emitScore() { this.$emit('score', this.score) }
  }
}
</script>

<style scoped>
.race-wrap { display: flex; flex-direction: column; gap: 12px; }

/* ── 顶部信息条 ───────────────────────────── */
.race-hud { display: flex; gap: 8px; }
.hud-chip {
  flex: 1; min-width: 0; padding: 8px 10px; border-radius: 14px;
  background: var(--panel); border: 1px solid var(--border);
  display: flex; flex-direction: column; gap: 2px;
}
.hud-chip span { font-size: 10.5px; color: var(--muted); white-space: nowrap; }
.hud-chip b { font-size: 15px; color: var(--primary); line-height: 1.25; white-space: nowrap; }

.race-stage { display: flex; flex-direction: column; align-items: center; gap: 10px; text-align: center; }
.race-mascot { font-size: 50px; line-height: 1.1; filter: drop-shadow(0 6px 10px rgba(160,108,213,.22)); }
.race-stage h3 { margin: 0; font-size: 18px; color: var(--primary); font-weight: 800; }
.race-stage p { margin: 0; font-size: 13px; color: var(--muted); line-height: 1.7; }
.race-lead b { color: var(--primary); font-size: 15px; }

/* ── 报名 ─────────────────────────────────── */
.prize-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; width: 100%; }
.prize-card {
  border-radius: 14px; padding: 10px 4px 8px;
  background: var(--panel); border: 1px solid var(--border);
  display: flex; flex-direction: column; align-items: center; gap: 1px;
}
.prize-card span { font-size: 24px; line-height: 1.2; }
.prize-card b { font-size: 17px; color: var(--primary); }
.prize-card small { font-size: 10.5px; color: var(--muted); }
.prize-card.gold { background: linear-gradient(160deg, color-mix(in srgb, var(--yellow) 32%, var(--card)), var(--card)); }
.prize-card.silver { background: linear-gradient(160deg, color-mix(in srgb, var(--blue) 24%, var(--card)), var(--card)); }
.prize-card.bronze { background: linear-gradient(160deg, color-mix(in srgb, var(--pink) 26%, var(--card)), var(--card)); }

.race-tips {
  list-style: none; margin: 2px 0 0; padding: 0; width: 100%;
  display: flex; flex-direction: column; gap: 6px; text-align: left;
}
.race-tips li {
  display: flex; align-items: center; gap: 9px;
  background: var(--panel); border: 1px solid var(--border);
  border-radius: 12px; padding: 8px 10px;
  font-size: 12.5px; color: var(--muted); line-height: 1.45;
}
.race-tips i {
  font-style: normal; font-size: 17px; flex: none;
  width: 30px; height: 30px; border-radius: 10px;
  background: var(--card); display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(160,108,213,.12);
}
.race-tips b { color: var(--primary); }
.race-fine { font-size: 11px !important; opacity: .8; }
kbd {
  background: var(--card); border: 1px solid var(--border); border-bottom-width: 2px;
  border-radius: 5px; padding: 0 5px; font-size: 10.5px; font-family: inherit;
}

/* ── 按钮 ─────────────────────────────────── */
.race-btn {
  position: relative; overflow: hidden; border: none; cursor: pointer; color: #fff;
  font-weight: 800; border-radius: 16px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 8px 20px color-mix(in srgb, var(--purple) 42%, transparent);
  transition: transform .12s ease, filter .12s ease, box-shadow .12s ease;
  -webkit-tap-highlight-color: transparent; touch-action: manipulation;
}
.race-btn:hover { filter: brightness(1.04); }
.race-btn:active { transform: scale(.965); }
.race-btn:disabled { opacity: .5; cursor: default; box-shadow: none; }
.race-btn.big { padding: 13px 30px; font-size: 15.5px; width: 100%; max-width: 340px; }

.race-btn.dash {
  width: 100%; max-width: 340px; padding: 12px 20px 13px;
  display: flex; flex-direction: column; align-items: center; gap: 1px;
  background: linear-gradient(135deg, var(--pink), var(--purple), var(--primary));
  background-size: 160% 160%;
  animation: raceFlow 5s ease infinite;
  box-shadow: 0 10px 26px color-mix(in srgb, var(--primary) 38%, transparent);
}
.race-btn.dash.tapped { transform: scale(.955); filter: brightness(1.1); }
.race-btn.dash.tired { filter: saturate(.65); }
.dash-emoji { font-size: 22px; line-height: 1.1; }
.dash-main { font-size: 19px; letter-spacing: 2px; }
.dash-sub { font-size: 10.5px; font-weight: 600; opacity: .88; }
.flash {
  position: absolute; bottom: 55%; transform: translateX(-50%);
  font-size: 13px; font-weight: 800; white-space: nowrap;
  color: #fff; text-shadow: 0 2px 6px rgba(0,0,0,.28);
  pointer-events: none; animation: raceFloat .72s ease-out forwards;
}

/* ── 赛道 ─────────────────────────────────── */
.race-track { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.lane {
  display: grid; grid-template-columns: 22px 38px minmax(0, 1fr) 34px;
  align-items: center; gap: 6px;
}
.lane-rank {
  width: 21px; height: 21px; border-radius: 50%; font-size: 11px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
  background: var(--panel); color: var(--muted); border: 1px solid var(--border);
}
.lane-rank.top { background: var(--primary); color: #fff; border-color: transparent; }
.lane-name { font-size: 12px; color: var(--muted); text-align: right; white-space: nowrap; }
.lane.me .lane-name { color: var(--primary); font-weight: 800; }
.lane-pct { font-size: 10.5px; color: var(--muted); text-align: right; font-variant-numeric: tabular-nums; }

.lane-bar {
  position: relative; height: 38px; border-radius: 19px; overflow: hidden;
  background: var(--panel);
  border: 1.5px solid var(--border);
}
.lane.me .lane-bar {
  border-color: color-mix(in srgb, var(--primary) 55%, var(--border));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--primary) 12%, transparent);
}
.lane.leader .lane-bar { box-shadow: 0 0 0 2px color-mix(in srgb, var(--yellow) 60%, transparent); }
.lane-stripes {
  position: absolute; inset: 0;
  background: repeating-linear-gradient(90deg,
    transparent 0 26px,
    color-mix(in srgb, var(--pink) 20%, transparent) 26px 52px);
  opacity: .8;
}
.lane-pony {
  position: absolute; top: 50%; transform: translate(-50%, -50%);
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 21px; background: var(--card);
  box-shadow: 0 3px 9px rgba(80,55,110,.22);
  transition: left .09s linear;
  z-index: 2;
}
.lane-pony.running { animation: raceBob .5s ease-in-out infinite alternate; }
.lane-pony.fast { box-shadow: 0 0 0 4px color-mix(in srgb, var(--yellow) 45%, transparent), 0 3px 9px rgba(80,55,110,.22); }
.lane-pony.tired { filter: grayscale(.35); }
.finish-flag {
  position: absolute; right: 3px; top: 50%; transform: translateY(-50%);
  font-size: 15px; opacity: .75; z-index: 1;
}

.race-progress {
  width: 100%; height: 8px; border-radius: 99px; background: var(--track); overflow: hidden;
}
.race-progress span {
  display: block; height: 100%; border-radius: 99px;
  background: linear-gradient(90deg, var(--pink), var(--purple), var(--primary));
  transition: width .09s linear;
}

/* ── 状态 / 体力 ───────────────────────────── */
.status-line { display: flex; gap: 6px; width: 100%; justify-content: center; flex-wrap: wrap; }
.status-pill, .combo-pill, .dist-pill {
  font-size: 11px; font-weight: 700; padding: 4px 10px; border-radius: 99px;
  background: var(--panel); border: 1px solid var(--border); color: var(--muted);
}
.status-pill { color: var(--primary); }
.combo-pill { color: #e2763c; background: color-mix(in srgb, var(--yellow) 22%, var(--card)); }

.energy-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.energy-label { font-size: 11px; color: var(--muted); white-space: nowrap; }
.energy-bar {
  flex: 1; height: 10px; border-radius: 99px; background: var(--track); overflow: hidden;
}
.energy-bar i {
  display: block; height: 100%; border-radius: 99px;
  background: linear-gradient(90deg, var(--mint), var(--yellow));
  transition: width .1s linear;
}
.energy-bar i.low { background: linear-gradient(90deg, #f0a868, #e06666); }
.energy-row b { font-size: 11px; color: var(--muted); width: 34px; text-align: right; font-variant-numeric: tabular-nums; }
.energy-row b.low { color: #e06666; }

/* ── 倒计时 ───────────────────────────────── */
.countdown-stage { position: relative; }
.countdown-mask {
  position: absolute; inset: -6px; border-radius: 18px;
  background: color-mix(in srgb, var(--card) 72%, transparent);
  backdrop-filter: blur(2px);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px;
}
.countdown-num {
  font-size: 68px; font-weight: 900; color: var(--primary);
  text-shadow: 0 6px 18px color-mix(in srgb, var(--primary) 30%, transparent);
  animation: racePop .6s cubic-bezier(.22,1.4,.4,1);
}
.countdown-tip { font-size: 12px; color: var(--muted); font-weight: 700; letter-spacing: 2px; }

/* ── 结算 ─────────────────────────────────── */
.done-sub { font-size: 12.5px !important; }
.net-line { font-size: 13px; color: var(--muted); }
.net-line b { font-size: 22px; color: var(--success); margin: 0 4px; }
.net-line b.neg { color: var(--danger); }
.final-board {
  width: 100%; display: flex; flex-direction: column; gap: 5px;
  background: var(--panel); border: 1px solid var(--border); border-radius: 14px; padding: 8px;
}
.board-row {
  display: grid; grid-template-columns: 26px 26px 1fr auto; align-items: center; gap: 6px;
  padding: 6px 8px; border-radius: 10px; font-size: 12.5px; color: var(--muted);
  background: var(--card);
}
.board-row.me { background: color-mix(in srgb, var(--primary) 12%, var(--card)); color: var(--primary); }
.board-rank { font-size: 15px; text-align: center; }
.board-icon { font-size: 18px; text-align: center; }
.board-row b { text-align: left; }
.board-row em { font-style: normal; font-size: 11px; font-variant-numeric: tabular-nums; }

/* ── 动画 ─────────────────────────────────── */
@keyframes raceBob { from { margin-top: 2px; } to { margin-top: -2px; } }
@keyframes raceFloat {
  0% { opacity: 0; transform: translate(-50%, 8px) scale(.8); }
  25% { opacity: 1; transform: translate(-50%, -4px) scale(1.05); }
  100% { opacity: 0; transform: translate(-50%, -38px) scale(1); }
}
@keyframes racePop {
  0% { opacity: 0; transform: scale(.3) rotate(-12deg); }
  60% { opacity: 1; transform: scale(1.15) rotate(4deg); }
  100% { opacity: 1; transform: scale(1) rotate(0); }
}
@keyframes raceFlow {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@media (max-width: 380px) {
  .lane { grid-template-columns: 20px 32px minmax(0, 1fr) 30px; gap: 4px; }
  .lane-name { font-size: 11px; }
  .race-mascot { font-size: 42px; }
}
@media (prefers-reduced-motion: reduce) {
  .lane-pony.running, .race-btn.dash, .countdown-num, .flash { animation: none; }
  .lane-pony, .race-progress span, .energy-bar i { transition: none; }
}
</style>
