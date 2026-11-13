<template>
  <div class="gl-wrap">
    <div class="gl-hud">
      <div class="hud-chip"><span>⭐ 得分</span><b>{{ score }}</b></div>
      <div class="hud-chip"><span>☁️ 穿过</span><b>{{ passed }}</b></div>
      <div class="hud-chip"><span>❤️ 小心心</span><b>{{ '❤️'.repeat(lives) || '—' }}</b></div>
      <div class="hud-chip"><span>⏱️ 剩余</span><b>{{ Math.max(0, 45 - Math.floor(time)) }}s</b></div>
    </div>

    <div
      v-if="stage !== 'done'"
      class="gl-sky"
      @pointerdown.prevent="hold(true)"
      @pointerup="hold(false)"
      @pointerleave="hold(false)"
      @pointercancel="hold(false)"
    >
      <span class="gl-deco d1"></span><span class="gl-deco d2"></span><span class="gl-deco d3"></span>
      <div class="gl-ground"></div>

      <div v-for="p in pipes" :key="p.id" class="gl-pipe" :style="{ left: p.x + '%' }">
        <div class="gl-cloud top" :style="{ height: p.gapTop + '%' }"></div>
        <span v-if="!p.starTaken" class="gl-star" :style="{ top: (p.gapTop + p.gapH / 2) + '%' }">⭐</span>
        <div class="gl-cloud bottom" :style="{ height: (100 - p.gapTop - p.gapH) + '%' }"></div>
      </div>

      <div class="gl-player" :class="{ hit: invTicks > 0 }" :style="{ top: playerY + '%' }">🦄</div>

      <div v-if="stage === 'ready'" class="gl-start">
        <b>☁️ 流光穿越</b>
        <span>按住屏幕 / 空格 起飞，松开缓缓落下</span>
        <em>前 5 秒慢慢飞 · 前 3 秒撞云不掉心 · 穿云缝 +10 · 吃星 +15</em>
      </div>
      <div v-else class="gl-live-tip">
        {{ time < 3 ? '🌤 热身中 · 撞云不掉心' : time < 5 ? '🌤 热身中 · 云朵慢慢飘' : (holding ? '↑ 上升中' : '↓ 下落中') }}
      </div>
    </div>

    <div v-else class="gl-done">
      <div class="gl-mascot">{{ score >= 120 ? '🏆' : '☁️' }}</div>
      <h3>{{ score >= 120 ? '飞得漂亮！' : '这趟飞行不错～' }}</h3>
      <p>穿过 <b>{{ passed }}</b> 层云 · 吃到 <b>{{ stars }}</b> 颗星 · 剩 <b>{{ lives }}</b> 颗心</p>
      <div class="gl-score">+{{ score }} 分</div>
      <button class="gl-btn" @click="restart">🔁 再飞一次</button>
    </div>

    <p class="gl-foot">💡 开局 5 秒慢慢加速、前 3 秒撞云不掉心，放心试手感；轻点轻放比一直按住更好控～</p>
  </div>
</template>

<script>
import sound from '../../sound'

const PLAYER_X = 15        // 玩家水平位置（%）
const PIPE_W = 14          // 云柱宽度（%）
const MAX_TIME = 45        // 一局 45 秒

export default {
  name: 'GameGlide',
  emits: ['score', 'over'],
  data() {
    return {
      stage: 'ready',
      score: 0,
      passed: 0,
      stars: 0,
      lives: 3,
      time: 0,
      playerY: 46,
      vy: 0,
      holding: false,
      invTicks: 0,
      pipes: [],
      pipeId: 0,
      timer: null,
      alive: true
    }
  },
  mounted() {
    window.addEventListener('keydown', this.onKeyDown)
    window.addEventListener('keyup', this.onKeyUp)
  },
  beforeUnmount() {
    this.alive = false
    clearInterval(this.timer)
    window.removeEventListener('keydown', this.onKeyDown)
    window.removeEventListener('keyup', this.onKeyUp)
  },
  methods: {
    spawn(x) {
      // 热身期云缝更宽，之后慢慢收窄（46% → 32%），给新手反应的余地
      const gapH = 46 - 14 * Math.min(1, Math.max(0, this.time - 3) / 25)
      const gapTop = 12 + Math.random() * (86 - gapH - 12)
      this.pipes.push({ id: ++this.pipeId, x, gapTop, gapH, scored: false, starTaken: false })
    },
    start() {
      this.stage = 'run'
      this.score = 0
      this.passed = 0
      this.stars = 0
      this.lives = 3
      this.time = 0
      this.playerY = 46
      this.vy = 0
      this.invTicks = 0
      this.pipes = []
      this.spawn(100)
      this.spawn(148)
      this.emitScore()
      sound.click()
      clearInterval(this.timer)
      this.timer = setInterval(this.tick, 30)
    },
    hold(on) {
      if (this.stage === 'done') return
      if (on && this.stage === 'ready') this.start()
      this.holding = on
    },
    onKeyDown(e) {
      if (e.code !== 'Space' && e.key !== ' ') return
      e.preventDefault()
      if (this.stage === 'done') return
      if (this.stage === 'ready') this.start()
      this.holding = true
    },
    onKeyUp(e) {
      if (e.code === 'Space' || e.key === ' ') this.holding = false
    },
    tick() {
      if (!this.alive || this.stage !== 'run') return
      const dt = 0.03
      this.time += dt

      // 玩家：按住上升，松开受重力下落（手感放缓，更好控）
      this.vy = this.holding ? -1.15 : this.vy + 0.09
      this.playerY = Math.max(3, Math.min(90, this.playerY + this.vy))
      if (this.invTicks > 0) this.invTicks -= 1

      // 开局 5 秒热身缓冲：0.85 → 2.2 平滑提速，之后缓慢加到 2.6，不会一上来就糊脸
      const w = Math.min(1, this.time / 5)
      const warm = w * w * (3 - 2 * w)
      const speed = 0.85 + 1.35 * warm + Math.min(0.4, Math.max(0, this.time - 8) * 0.012)
      for (const p of this.pipes) {
        p.x -= speed
        const overlap = p.x < PLAYER_X + 7 && p.x + PIPE_W > PLAYER_X - 2
        const starY = p.gapTop + p.gapH / 2
        const inGap = this.playerY + 4 > p.gapTop + 1 && this.playerY + 4 < p.gapTop + p.gapH - 1

        // 吃到缝隙里的星星
        if (!p.starTaken && overlap && Math.abs(this.playerY + 4 - starY) < 12) {
          p.starTaken = true
          this.stars += 1
          this.score += 15
          sound.twinkle()
          this.emitScore()
        }
        // 穿过一整根云柱
        if (!p.scored && p.x + PIPE_W < PLAYER_X) {
          p.scored = true
          this.passed += 1
          this.score += 10
          sound.pop()
          this.emitScore()
        }
        // 撞云：前 3 秒是热身宽限，只扶回云缝不扣心；之后才扣一颗心
        if (overlap && !inGap && this.invTicks <= 0) {
          this.playerY = p.gapTop + p.gapH / 2 - 4
          this.vy = 0
          this.invTicks = this.time < 3 ? 30 : 50
          if (this.time < 3) {
            sound.hover()
          } else {
            this.lives -= 1
            sound.fail()
            if (this.lives <= 0) { this.end(); return }
          }
        }
      }
      this.pipes = this.pipes.filter(p => p.x > -PIPE_W - 6)
      if (this.pipes.length && this.pipes[this.pipes.length - 1].x < 68) this.spawn(105)
      if (!this.pipes.length) this.spawn(105)
      if (this.time >= MAX_TIME) this.end()
    },
    end() {
      clearInterval(this.timer)
      this.stage = 'done'
      this.holding = false
      this.$emit('over', this.score)
      if (this.score >= 120) sound.success()
      else sound.pop()
    },
    restart() {
      clearInterval(this.timer)
      this.pipes = []
      this.playerY = 46
      this.vy = 0
      this.invTicks = 0
      this.time = 0
      this.score = 0
      this.passed = 0
      this.stars = 0
      this.lives = 3
      this.stage = 'ready'
      this.emitScore()
    },
    emitScore() { this.$emit('score', this.score) }
  }
}
</script>

<style scoped>
.gl-wrap { display: flex; flex-direction: column; gap: 10px; align-items: center; }
.gl-hud { display: flex; gap: 7px; width: 100%; }
.hud-chip {
  flex: 1; min-width: 0; padding: 7px 6px; border-radius: 13px;
  background: var(--panel); border: 1px solid var(--border);
  display: flex; flex-direction: column; gap: 2px; text-align: center;
}
.hud-chip span { font-size: 10px; color: var(--muted); white-space: nowrap; }
.hud-chip b { font-size: 13.5px; color: var(--primary); white-space: nowrap; }

.gl-sky {
  position: relative; width: 100%; height: 320px; border-radius: 20px; overflow: hidden;
  background: linear-gradient(180deg,
    color-mix(in srgb, var(--blue) 42%, var(--card)),
    color-mix(in srgb, var(--pink) 16%, var(--card)) 65%,
    color-mix(in srgb, var(--mint) 26%, var(--card)));
  border: 1.5px solid var(--border);
  cursor: pointer; user-select: none; touch-action: none;
}
.gl-deco { position: absolute; border-radius: 99px; background: rgba(255,255,255,.6); }
.gl-deco::before, .gl-deco::after { content: ''; position: absolute; border-radius: 50%; background: rgba(255,255,255,.6); }
.gl-deco::before { width: 26px; height: 26px; left: 14px; top: -12px; }
.gl-deco::after { width: 20px; height: 20px; right: 16px; top: -9px; }
.d1 { width: 78px; height: 22px; left: 18%; top: 16%; animation: glDrift 14s linear infinite; }
.d2 { width: 60px; height: 18px; left: 55%; top: 34%; animation: glDrift 19s linear infinite reverse; }
.d3 { width: 92px; height: 24px; left: 72%; top: 9%; animation: glDrift 22s linear infinite; }
.gl-ground {
  position: absolute; left: 0; right: 0; bottom: 0; height: 14px;
  background: repeating-linear-gradient(90deg, color-mix(in srgb, var(--mint) 55%, var(--card)) 0 18px, color-mix(in srgb, var(--yellow) 35%, var(--card)) 18px 36px);
  opacity: .8;
}
.gl-pipe { position: absolute; top: 0; bottom: 0; width: 14%; }
.gl-cloud {
  position: absolute; left: 0; right: 0;
  background: linear-gradient(180deg, #fff, color-mix(in srgb, var(--blue) 22%, #fff));
  box-shadow: inset 0 -6px 12px rgba(150,170,210,.18);
}
.gl-cloud.top { top: 0; border-radius: 0 0 16px 16px; }
.gl-cloud.top::after {
  content: ''; position: absolute; bottom: -9px; left: -4px; right: -4px; height: 18px;
  background: #fff; border-radius: 99px;
}
.gl-cloud.bottom { bottom: 0; border-radius: 16px 16px 0 0; }
.gl-cloud.bottom::before {
  content: ''; position: absolute; top: -9px; left: -4px; right: -4px; height: 18px;
  background: #fff; border-radius: 99px;
}
.gl-star {
  position: absolute; left: 50%; transform: translate(-50%, -50%);
  font-size: 19px; filter: drop-shadow(0 2px 5px rgba(247,224,138,.9));
  animation: glTwinkle 1.2s ease-in-out infinite alternate;
}
.gl-player {
  position: absolute; left: 15%; width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 21px;
  background: var(--card); box-shadow: 0 4px 12px rgba(80,55,110,.25);
  transform: translateX(-50%); z-index: 3;
}
.gl-player::before {
  content: ''; position: absolute; right: 100%; top: 45%; width: 34px; height: 6px;
  border-radius: 99px; background: linear-gradient(90deg, transparent, var(--pink), var(--yellow));
  opacity: .75;
}
.gl-player.hit { animation: glBlink .2s steps(2) infinite; }
.gl-start, .gl-live-tip {
  position: absolute; left: 50%; transform: translateX(-50%); z-index: 4;
  display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center;
}
.gl-start {
  top: 50%; margin-top: -46px; width: 90%; padding: 14px 10px; border-radius: 16px;
  background: color-mix(in srgb, var(--card) 88%, transparent);
  box-shadow: 0 8px 22px rgba(80,55,110,.18);
}
.gl-start b { font-size: 17px; color: var(--primary); }
.gl-start span { font-size: 12.5px; color: var(--text); font-weight: 700; }
.gl-start em { font-size: 11px; color: var(--muted); font-style: normal; }
.gl-live-tip {
  bottom: 20px; font-size: 11px; color: var(--muted); font-weight: 700;
  background: color-mix(in srgb, var(--card) 75%, transparent); padding: 3px 12px; border-radius: 99px;
}
.gl-done { display: flex; flex-direction: column; align-items: center; gap: 8px; text-align: center; padding: 10px 0; }
.gl-mascot { font-size: 50px; }
.gl-done h3 { margin: 0; font-size: 18px; color: var(--primary); }
.gl-done p { margin: 0; font-size: 12.5px; color: var(--muted); }
.gl-done p b { color: var(--primary); }
.gl-score { font-size: 30px; font-weight: 800; color: var(--primary); }
.gl-btn {
  border: none; border-radius: 14px; padding: 11px 28px; cursor: pointer;
  color: #fff; font-weight: 800; font-size: 14.5px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 8px 20px color-mix(in srgb, var(--purple) 40%, transparent);
}
.gl-foot { margin: 0; font-size: 11px; color: var(--muted); text-align: center; }

@keyframes glDrift { from { transform: translateX(-30px); } to { transform: translateX(30px); } }
@keyframes glTwinkle { from { transform: translate(-50%,-50%) scale(.85); opacity: .75; } to { transform: translate(-50%,-50%) scale(1.15); opacity: 1; } }
@keyframes glBlink { 50% { opacity: .35; } }
@media (max-width: 380px) { .gl-sky { height: 280px; } .hud-chip b { font-size: 12px; } }
@media (prefers-reduced-motion: reduce) { .gl-deco, .gl-star, .gl-player.hit { animation: none; } }
</style>
