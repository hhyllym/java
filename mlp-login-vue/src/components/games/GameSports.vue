<template>
  <div class="sp-wrap">
    <!-- HUD：项目进度 + 当前总分 -->
    <div class="sp-hud">
      <span class="sp-round">第 {{ roundNo }} / 3 项</span>
      <span class="sp-name">{{ stageName }}</span>
      <span class="sp-total">总分 {{ total }}</span>
    </div>

    <!-- ===== 准备阶段 ===== -->
    <div v-if="stage === 'ready'" class="sp-stage sp-ready">
      <div class="sp-mascot">🏅</div>
      <h3>趣味运动会</h3>
      <p>云宝主办的的三项连赛，冲高分拿积分！</p>
      <ul class="sp-menu-list">
        <li>🪢 <b>跳绳 30 秒</b> —— 圈缩到最小时点击「跳」</li>
        <li>📏 <b>跳远 3 次</b> —— 力量条停在金色区跳最远</li>
        <li>🏃 <b>60 米短跑</b> —— 10 秒内左右交替狂点</li>
      </ul>
      <button class="sp-btn big" @click="startAll">开始比赛！</button>
    </div>

    <!-- ===== 项目一：跳绳 ===== -->
    <div v-else-if="stage === 'rope'" class="sp-stage">
      <div class="sp-timer">⏱ {{ rope.time.toFixed(1) }}s · 得分 {{ rope.score }} · 连击 x{{ rope.combo }}</div>
      <div class="sp-rope-area">
        <div class="sp-rope" :style="{ transform: 'scale(' + ropeScale + ')' }"></div>
        <div class="sp-rope-dot"></div>
      </div>
      <div v-if="rope.judged" class="sp-judge" :class="rope.judged">{{ rope.judgedText }}</div>
      <button class="sp-btn big" @click="ropeJump">跳！🦶</button>
    </div>

    <!-- ===== 项目二：跳远 ===== -->
    <div v-else-if="stage === 'jump'" class="sp-stage">
      <div class="sp-timer">第 {{ 3 - jump.tries + 1 }} / 3 次 · 最远 {{ jump.best.toFixed(2) }}m</div>
      <div class="sp-jump-area">
        <div class="sp-power-track">
          <div class="sp-power-zone"></div>
          <div class="sp-power-fill" :style="{ width: jump.power + '%' }"></div>
        </div>
        <div class="sp-pit">🕳️🕳️🕳️</div>
      </div>
      <div v-if="jump.last" class="sp-judge" :class="jump.last.cls">{{ jump.last.text }}</div>
      <button class="sp-btn big" :disabled="jump.locking" @click="jumpLock">起跳！💨</button>
    </div>

    <!-- ===== 项目三：短跑 ===== -->
    <div v-else-if="stage === 'run'" class="sp-stage">
      <div class="sp-timer">⏱ {{ run.time.toFixed(1) }}s · 已跑 {{ run.dist.toFixed(1) }}m · 有效 {{ run.steps }} 步</div>
      <div class="sp-run-track">
        <div class="sp-run-fill" :style="{ width: runPct + '%' }"></div>
        <span class="sp-runner" :style="{ left: runPct + '%' }">🏃‍♀️</span>
        <span class="sp-flag">🏁</span>
      </div>
      <div class="sp-run-btns">
        <button class="sp-btn foot" @click="runTap('L')">左脚 👟</button>
        <button class="sp-btn foot" @click="runTap('R')">右脚 👟</button>
      </div>
      <p class="sp-run-tip">左右交替点击，同一边连点会踩脚！</p>
    </div>

    <!-- ===== 结算 ===== -->
    <div v-else-if="stage === 'done'" class="sp-stage sp-done">
      <div class="sp-mascot">{{ total >= 240 ? '🥇' : total >= 180 ? '🥈' : '🥉' }}</div>
      <h3>{{ rankTitle }}</h3>
      <div class="sp-scores">
        <div class="sp-score-item"><b>{{ rope.score }}</b><span>🪢 跳绳</span></div>
        <div class="sp-score-item"><b>{{ jump.best.toFixed(2) }}m</b><span>📏 跳远</span></div>
        <div class="sp-score-item"><b>{{ run.dist.toFixed(1) }}m</b><span>🏃 短跑</span></div>
      </div>
      <div class="sp-final">总分 <b>{{ total }}</b> / 300</div>
      <button class="sp-btn big" @click="restart">再来一轮！</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'

export default {
  name: 'GameSports',
  data() {
    return {
      stage: 'ready',   // ready | rope | jump | run | done
      timer: null,
      rope: { phase: 0, score: 0, combo: 0, time: 30, judged: null, judgedText: '' },
      jump: { power: 0, dir: 1, tries: 3, best: 0, last: null, locking: false },
      run:  { dist: 0, time: 10, steps: 0, last: '', wrong: 0 },
      total: 0
    }
  },
  computed: {
    roundNo() {
      return { rope: 1, jump: 2, run: 3, done: 3 }[this.stage] || 0
    },
    stageName() {
      return { ready: '准备', rope: '跳绳', jump: '跳远', run: '短跑', done: '结算' }[this.stage] || ''
    },
    ropeScale() {
      // 绳子圈缩放：phase 0.5 时缩到最小（脚底），是起拍时机
      return (1.3 - 0.6 * Math.sin(this.rope.phase * Math.PI)).toFixed(3)
    },
    runPct() {
      return Math.min(100, this.run.dist / 60 * 100)
    },
    rankTitle() {
      if (this.total >= 240) return '彩虹音爆级！云宝本人亲自颁奖 🏆'
      if (this.total >= 180) return '闪电天马队候补队员！'
      if (this.total >= 120) return '状态不错，再练练就快了！'
      return '重在参与，友谊第一比赛第二！'
    }
  },
  beforeUnmount() {
    this.clearTimer()
  },
  methods: {
    clearTimer() {
      if (this.timer) { clearInterval(this.timer); this.timer = null }
    },
    restart() {
      this.clearTimer()
      this.rope = { phase: 0, score: 0, combo: 0, time: 30, judged: null, judgedText: '' }
      this.jump = { power: 0, dir: 1, tries: 3, best: 0, last: null, locking: false }
      this.run = { dist: 0, time: 10, steps: 0, last: '', wrong: 0 }
      this.total = 0
      this.stage = 'ready'
      this.$emit('score', 0)
    },
    startAll() {
      this.stage = 'rope'
      sound.click()
      this.$emit('score', 0)
      this.timer = setInterval(this.ropeTick, 60)
    },

    /* ===== 跳绳 ===== */
    ropeTick() {
      const r = this.rope
      r.phase += 60 / 750          // 750ms 一个节拍周期
      if (r.phase >= 1) r.phase -= 1
      r.time -= 0.06
      if (r.time <= 0) {
        r.time = 0
        this.toJump()
      }
    },
    ropeJump() {
      const r = this.rope
      const off = Math.abs(r.phase - 0.5)   // 离"绳子过脚底"的偏差
      if (off < 0.09) {
        r.combo += 1
        const bonus = r.combo >= 5 ? 2 : 0
        r.score += 10 + bonus
        r.judged = 'perfect'
        r.judgedText = r.combo >= 5 ? `完美 x${r.combo}！+${10 + bonus}` : '完美起拍！+10'
        sound.pop()
      } else if (off < 0.22) {
        r.score += 4
        r.judged = 'good'
        r.judgedText = '踩到了！+4'
        sound.hover()
      } else {
        r.combo = 0
        r.judged = 'miss'
        r.judgedText = '绳子绊倒了…'
        sound.fail()
      }
      this.$emit('score', this.calcTotal())
      setTimeout(() => { r.judged = null }, 650)
    },
    toJump() {
      this.clearTimer()
      this.total = this.calcTotal()
      this.stage = 'jump'
      this.$emit('score', this.total)
      this.timer = setInterval(this.jumpTick, 40)
    },

    /* ===== 跳远 ===== */
    jumpTick() {
      const j = this.jump
      j.power += j.dir * 2.6
      if (j.power >= 100) { j.power = 100; j.dir = -1 }
      if (j.power <= 0) { j.power = 0; j.dir = 1 }
    },
    jumpLock() {
      const j = this.jump
      if (j.locking) return
      j.locking = true
      const p = j.power
      let dist, cls, text
      if (p >= 99) {                       // 踩线犯规
        dist = 0; cls = 'miss'; text = '踩线犯规！本次无效 😵'
        sound.fail()
      } else if (Math.abs(p - 90) <= 5) {  // 金色完美区
        dist = 5.0 + Math.random() * 0.8
        cls = 'perfect'; text = `腾空而起！${dist.toFixed(2)} 米 🎉`
        sound.magic()
      } else if (Math.abs(p - 90) <= 15) {
        dist = 3.2 + Math.random() * 1.5
        cls = 'good'; text = `跳得不错：${dist.toFixed(2)} 米`
        sound.pop()
      } else if (Math.abs(p - 90) <= 32) {
        dist = 1.5 + Math.random() * 1.5
        cls = 'ok'; text = `一般发挥：${dist.toFixed(2)} 米`
        sound.hover()
      } else {
        dist = 0.6 + Math.random() * 0.7
        cls = 'miss'; text = `力量没控制好：${dist.toFixed(2)} 米`
        sound.fail()
      }
      if (dist > j.best) j.best = dist
      j.last = { text, cls }
      j.tries -= 1
      j.power = 0
      this.$emit('score', this.calcTotal())
      setTimeout(() => {
        j.locking = false
        j.last = null
        if (j.tries <= 0) this.toRun()
      }, 900)
    },
    toRun() {
      this.clearTimer()
      this.total = this.calcTotal()
      this.stage = 'run'
      this.$emit('score', this.total)
      this.timer = setInterval(this.runTick, 100)
    },

    /* ===== 短跑 ===== */
    runTick() {
      const r = this.run
      r.time -= 0.1
      if (r.time <= 0 || r.dist >= 60) {
        r.time = Math.max(0, r.time)
        this.toDone()
      }
    },
    runTap(side) {
      const r = this.run
      if (r.last === side) {          // 同边连点 = 踩脚
        r.wrong += 1
        r.dist = Math.max(0, r.dist - 0.3)
        r.last = ''
        sound.fail()
        return
      }
      r.last = side
      r.steps += 1
      r.dist += 0.55 + (r.steps > 40 ? 0.1 : 0)   // 步频起来了有冲刺加成
      if (r.steps % 6 === 0) sound.hover()
      if (r.dist >= 60) {
        r.dist = 60
        this.toDone()
      }
    },
    toDone() {
      this.clearTimer()
      this.total = this.calcTotal()
      this.stage = 'done'
      this.$emit('score', this.total)
      this.$emit('over', this.total)
      if (this.total >= 180) sound.success()
      else sound.fail()
    },

    /* ===== 总分：三项各自映射到百分制再相加 ===== */
    calcTotal() {
      const ropePct = Math.min(100, Math.round(this.rope.score / 4))
      const jumpPct = Math.min(100, Math.round(this.jump.best / 5.8 * 100))
      const usedTime = 10 - this.run.time
      let runPct
      if (this.run.dist >= 60) {
        runPct = Math.min(100, 60 + Math.round((10 - usedTime) * 4))
      } else {
        runPct = Math.round(this.run.dist / 60 * 60)
      }
      return ropePct + jumpPct + runPct
    }
  }
}
</script>

<style scoped>
.sp-wrap { display: flex; flex-direction: column; gap: 14px; }

.sp-hud {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 12px;
  background: var(--panel); font-size: 12.5px;
}
.sp-round { color: var(--muted); }
.sp-name { flex: 1; font-weight: 800; color: var(--primary); }
.sp-total { font-weight: 800; }

.sp-stage {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  padding: 22px 16px; border-radius: 16px;
  background: var(--panel); text-align: center; min-height: 300px;
  justify-content: center;
}
.sp-mascot { font-size: 52px; }
.sp-stage h3 { margin: 0; font-size: 18px; }
.sp-stage p { margin: 0; font-size: 12.5px; color: var(--muted); }

.sp-menu-list {
  margin: 4px 0 8px; padding: 0; list-style: none;
  display: flex; flex-direction: column; gap: 8px; text-align: left;
  font-size: 12.5px; color: var(--text);
}
.sp-menu-list li {
  padding: 9px 12px; border-radius: 10px;
  background: var(--card); border: 1px solid var(--border);
}

.sp-timer { font-size: 13px; font-weight: 700; color: var(--muted); }

/* 跳绳 */
.sp-rope-area {
  position: relative; width: 150px; height: 150px;
  display: flex; align-items: center; justify-content: center;
}
.sp-rope {
  position: absolute; width: 140px; height: 140px;
  border: 5px solid var(--primary); border-radius: 50%;
  box-shadow: 0 0 12px rgba(160, 108, 213, .3);
}
.sp-rope-dot {
  width: 26px; height: 26px; border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 3px 8px rgba(0, 0, 0, .2);
}
.sp-judge { font-size: 14px; font-weight: 800; min-height: 20px; }
.sp-judge.perfect { color: var(--success); }
.sp-judge.good { color: var(--primary); }
.sp-judge.ok { color: var(--muted); }
.sp-judge.miss { color: var(--danger); }

/* 跳远 */
.sp-jump-area { width: 100%; display: flex; flex-direction: column; gap: 10px; }
.sp-power-track {
  position: relative; height: 26px; border-radius: 999px;
  background: var(--track); overflow: hidden;
  border: 1px solid var(--border);
}
.sp-power-zone {
  position: absolute; top: 0; bottom: 0;
  left: calc(90% - 12px); width: 24px;
  background: linear-gradient(180deg, #ffe58a, #f7c94c);
  box-shadow: 0 0 10px rgba(247, 201, 76, .55);
}
.sp-power-fill {
  position: absolute; top: 0; bottom: 0; left: 0;
  background: linear-gradient(90deg, var(--pink), var(--purple));
  opacity: .75; border-radius: 999px 0 0 999px;
}
.sp-pit { font-size: 22px; letter-spacing: 2px; }

/* 短跑 */
.sp-run-track {
  position: relative; width: 100%; height: 30px;
  border-radius: 999px; background: var(--track);
  border: 1px solid var(--border);
}
.sp-run-fill {
  position: absolute; top: 0; bottom: 0; left: 0;
  background: linear-gradient(90deg, #8fd4a8, var(--primary));
  border-radius: 999px; transition: width .08s linear;
}
.sp-runner { position: absolute; top: 50%; transform: translate(-50%, -50%); font-size: 18px; }
.sp-flag { position: absolute; right: 6px; top: 50%; transform: translateY(-50%); font-size: 16px; }
.sp-run-btns { display: flex; gap: 12px; width: 100%; }
.sp-btn.foot { flex: 1; padding: 16px 0; font-size: 15px; }
.sp-run-tip { margin: 0; font-size: 11.5px; color: var(--muted); }

/* 结算 */
.sp-scores { display: flex; gap: 10px; }
.sp-score-item {
  flex: 1; padding: 10px 6px; border-radius: 12px;
  background: var(--card); border: 1px solid var(--border);
  display: flex; flex-direction: column; gap: 3px;
}
.sp-score-item b { font-size: 15px; color: var(--primary); }
.sp-score-item span { font-size: 11px; color: var(--muted); }
.sp-final { font-size: 13px; color: var(--muted); }
.sp-final b { font-size: 22px; color: var(--primary); }

/* 按钮 */
.sp-btn {
  border: none; cursor: pointer; font-weight: 800;
  padding: 10px 22px; border-radius: 999px; font-size: 13.5px;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 6px 14px rgba(160, 108, 213, .3);
  transition: transform .12s ease, filter .12s ease;
}
.sp-btn:hover { filter: brightness(1.07); transform: translateY(-1px); }
.sp-btn:active { transform: scale(.96); }
.sp-btn:disabled { opacity: .55; cursor: not-allowed; transform: none; }
.sp-btn.big { padding: 13px 34px; font-size: 15px; }
</style>
