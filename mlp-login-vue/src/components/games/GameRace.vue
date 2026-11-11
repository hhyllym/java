<template>
  <div class="race-wrap">
    <div class="race-hud">
      <span>我的积分 <b>{{ points }}</b></span>
      <span>本局得分 <b>{{ score }}</b></span>
    </div>

    <!-- 报名阶段：扣积分参赛 -->
    <div v-if="stage === 'ready'" class="race-stage">
      <div class="race-mascot">🏁</div>
      <h3>云驰冲刺赛</h3>
      <p>报名费 <b>{{ FEE }}</b> 积分，和三位小马比谁先冲线！<br>名次越靠前，奖金越高——冠军 {{ PRIZE[0] }} 分、亚军 {{ PRIZE[1] }} 分、季军 {{ PRIZE[2] }} 分。</p>
      <ul class="race-tips">
        <li>🏃 开赛后<b>疯狂点击「冲刺」</b>按钮加速</li>
        <li>⚡ 体力有限，狂点会累——看时机匀速点更稳</li>
        <li>💸 没进前三也拿不到奖金，报名费不退哦</li>
      </ul>
      <button class="race-btn big" :disabled="points < FEE" @click="join">
        {{ points < FEE ? '积分不够报名（还差 ' + (FEE - points) + '）' : '交 ' + FEE + ' 积分，开跑！' }}
      </button>
    </div>

    <!-- 比赛进行中 -->
    <div v-else-if="stage === 'run'" class="race-stage">
      <div class="race-track">
        <div
          v-for="(r, i) in racers"
          :key="i"
          class="lane"
          :class="{ me: r.me }"
        >
          <span class="lane-name">{{ r.me ? '我' : r.name }}</span>
          <div class="lane-bar"><span class="lane-pony" :style="{ left: r.pos + '%' }">{{ r.icon }}</span></div>
        </div>
      </div>
      <div class="race-progress"><span :style="{ width: progress + '%' }"></span></div>
      <button class="race-btn big dash" @click="push">🏃‍♀️ 冲刺！（狂点！）</button>
    </div>

    <!-- 结算 -->
    <div v-else class="race-stage race-done">
      <div class="race-mascot">{{ rank === 1 ? '🏆' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : '💦' }}</div>
      <h3>{{ rankTitle }}</h3>
      <p>第 {{ rank }} 名 · 报名费 -{{ FEE }} · 奖金 +{{ prize }}</p>
      <p class="race-net">净收益 <b :class="{ neg: prize - FEE < 0 }">{{ prize - FEE >= 0 ? '+' : '' }}{{ prize - FEE }}</b> 积分</p>
      <button class="race-btn big" @click="reset">再来一局！</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'
import { loadLearn, addPoints } from '../../learn'

const FEE = 20
const PRIZE = [80, 45, 25, 0]          // 冠/亚/季/落选
const RIVALS = [
  { name: '云驰', icon: '🦄', speed: 1.0 },
  { name: '金穗', icon: '🍎', speed: 0.92 },
  { name: '茸茸', icon: '🦋', speed: 0.84 }
]
const FINISH = 100                     // 冲线距离

export default {
  name: 'GameRace',
  emits: ['score', 'over'],
  data() {
    return {
      FEE, PRIZE,
      stage: 'ready',
      points: 0,
      score: 0,
      racers: [],
      progress: 0,
      rank: 4,
      prize: 0,
      timer: null,
      tapping: false,
      alive: true
    }
  },
  computed: {
    rankTitle() {
      return ['', '冠军！全场都在喊你的名字 🎉', '亚军，差一点点就翻盘！', '季军，也有奖金拿～', '没进前三……下次注意节奏！'][this.rank]
    }
  },
  mounted() { this.points = (loadLearn().points || 0) },
  beforeUnmount() { this.alive = false; clearInterval(this.timer) },
  methods: {
    join() {
      if (this.points < FEE) return
      addPoints(-FEE)
      this.points = loadLearn().points || 0
      sound.click()

      this.racers = [
        { name: '我', icon: '🧑‍🎤', me: true, pos: 0, v: 0, boost: 0 },
        ...RIVALS.map(r => ({ ...r, me: false, pos: 0, v: 0 }))
      ]
      this.score = 0
      this.stage = 'run'
      this.tapping = false

      // AI 按各自速度匀速前进（带一点随机抖动），玩家靠手速
      this.timer = setInterval(() => {
        if (!this.alive) return clearInterval(this.timer)
        let done = false
        for (const r of this.racers) {
          if (r.me) {
            // 体力：连点快了会衰减，松手恢复
            r.boost = this.tapping ? Math.min(3.2, r.boost + 0.5) : Math.max(0.6, r.boost - 0.35)
            r.v = r.boost
          } else {
            r.v = r.speed * (0.9 + Math.random() * 0.35)
          }
          r.pos = Math.min(FINISH, r.pos + r.v * 0.9)
          if (r.pos >= FINISH) done = true
        }
        this.tapping = false
        const me = this.racers[0]
        this.progress = me.pos
        if (done) this.finish()
      }, 60)
    },

    push() {
      this.tapping = true
      sound.hover()
    },

    finish() {
      clearInterval(this.timer)
      const sorted = [...this.racers].sort((a, b) => b.pos - a.pos)
      this.rank = sorted.findIndex(r => r.me) + 1
      this.prize = PRIZE[this.rank - 1] || 0
      this.score = Math.max(0, this.prize - FEE)
      if (this.prize > 0) addPoints(this.prize)
      this.points = loadLearn().points || 0
      this.stage = 'done'
      this.emitScore()
      this.$emit('over', this.score)
      if (this.rank === 1) sound.success(); else if (this.prize > 0) sound.pop(); else sound.fail()
    },

    reset() {
      this.stage = 'ready'
      this.emitScore()
    },
    emitScore() { this.$emit('score', this.score) }
  }
}
</script>

<style scoped>
.race-wrap { display: flex; flex-direction: column; gap: 14px; }
.race-hud {
  display: flex; gap: 14px; padding: 10px 14px; border-radius: 12px;
  background: var(--panel); font-size: 12.5px;
}
.race-hud b { color: var(--primary); font-size: 15px; }
.race-stage { display: flex; flex-direction: column; align-items: center; gap: 10px; text-align: center; }
.race-mascot { font-size: 52px; line-height: 1.1; }
.race-stage h3 { margin: 0; font-size: 17px; color: var(--primary); }
.race-stage p { margin: 0; font-size: 13px; color: var(--muted); line-height: 1.7; }
.race-tips { text-align: left; font-size: 12.5px; color: var(--muted); margin: 0; padding-left: 18px; line-height: 1.9; }

.race-track { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.lane { display: flex; align-items: center; gap: 8px; }
.lane-name { width: 38px; font-size: 12px; color: var(--muted); text-align: right; flex: none; }
.lane.me .lane-name { color: var(--primary); font-weight: 800; }
.lane-bar {
  position: relative; flex: 1; height: 34px; border-radius: 17px;
  background: repeating-linear-gradient(90deg, #fff 0 26px, #fdf3f8 26px 52px);
  box-shadow: inset 0 0 0 1.5px rgba(150,120,180,.18);
}
.lane-pony {
  position: absolute; top: 50%; transform: translateY(-50%);
  font-size: 24px; transition: left .1s linear;
}
.race-progress {
  width: 100%; height: 6px; border-radius: 3px; background: var(--panel); overflow: hidden;
}
.race-progress span { display: block; height: 100%; background: linear-gradient(90deg, #f7a8c4, #b79cf0); }

.race-btn {
  border: none; border-radius: 14px; cursor: pointer; color: #fff;
  font-weight: 800; background: linear-gradient(135deg, #f7a8c4, #b79cf0);
  box-shadow: 0 8px 20px rgba(183,156,240,.4);
}
.race-btn.big { padding: 12px 30px; font-size: 15.5px; }
.race-btn.dash { padding: 16px 46px; font-size: 17px; }
.race-btn:disabled { opacity: .55; cursor: default; }
.race-net b { color: #4caf7d; font-size: 16px; }
.race-net b.neg { color: #e06666; }
</style>
