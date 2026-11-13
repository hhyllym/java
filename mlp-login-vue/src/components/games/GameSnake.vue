<template>
  <div class="sn-wrap">
    <div class="sn-hud">
      <span class="sn-diff-tag">{{ cfg.emoji }} {{ cfg.name }}</span>
      <span>🍎 吃了 {{ eaten }} 个</span>
      <span v-if="food.gold && stage === 'playing'" class="sn-gold">✨ 金苹果剩 {{ food.ttl }} 步</span>
      <span>得分 <b>{{ score }}</b></span>
      <button v-if="model3d" class="sn-3d-btn" :class="{ on: use3d }" @click="toggle3D">
        {{ use3d ? '🧊 Q版3D' : '🎨 经典' }}
      </button>
    </div>

    <div
      class="sn-board"
      :class="{ 'sn-3d': use3d && model3d }"
      ref="board"
      @touchstart="onTouchStart"
      @touchend="onTouchEnd"
    >
      <div
        v-for="c in cells"
        :key="c.i"
        class="sn-cell"
        :class="c.cls"
      ></div>

      <!-- Q 版 3D 小蛇：正交相机与 12×12 网格逐格对齐；WebGL 不可用自动回退 CSS -->
      <canvas v-show="use3d && model3d" ref="boardCv" class="sn-canvas"></canvas>

      <!-- 吃到东西时飘一下分数，key 变了动画就重播 -->
      <div v-if="eaten" :key="eaten" class="sn-flash">+{{ lastGain }}</div>

      <div v-show="stage !== 'playing'" class="sn-mask">
        <div class="sn-face">
          <canvas v-show="use3d && model3d" ref="faceCv" class="sn-face3d"></canvas>
          <span v-show="!use3d || !model3d" class="sn-face-emoji">{{ faceEmoji }}</span>
        </div>

        <template v-if="stage === 'ready'">
          <h3>彩虹苹果贪吃蛇</h3>
          <p>金穗的果园里来了一条 Q 版馋嘴小蛇～</p>

          <!-- 难度选择：开局前挑，选完会记住 -->
          <div class="sn-diff-label">选个难度开局 👇</div>
          <div class="sn-diff-row">
            <button
              v-for="d in DIFF_LIST"
              :key="d.key"
              class="sn-diff-btn"
              :class="{ on: diff === d.key }"
              @click="pickDiff(d.key)"
            >
              <b>{{ d.emoji }} {{ d.name }}</b>
              <em>{{ d.desc }}</em>
            </button>
          </div>

          <ul class="sn-rules">
            <li>🍎 吃苹果 +10 分，身体变长、速度变快</li>
            <li>✨ 每吃 4 个冒出金苹果，+30 分（会过期）</li>
            <li>🎉 每吃满 5 个，额外奖励 +20 分</li>
            <li>💥 撞到墙或自己就结束，不能直接掉头</li>
          </ul>
          <button class="sn-btn big" @click="start">开始游戏！</button>
        </template>
        <template v-else-if="stage === 'paused'">
          <h3>歇一口气</h3>
          <p>小蛇原地等你回来～按空格也能继续</p>
          <button class="sn-btn big" @click="togglePause">继续游戏</button>
        </template>
        <template v-else>
          <h3>本局结束</h3>
          <p>{{ cfg.emoji }} {{ cfg.name }}难度 · 吃掉 {{ eaten }} 个苹果，拿到 {{ score }} 分！</p>
          <p class="sn-over-hint">{{ overHint }}</p>
          <button class="sn-btn big" @click="backToReady">换个难度 / 再来一局</button>
        </template>
      </div>
    </div>

    <div class="sn-bottom">
      <div class="sn-pad">
        <button class="sn-dir up" @click="turn(0, -1)" title="上">↑</button>
        <button class="sn-dir left" @click="turn(-1, 0)" title="左">←</button>
        <button class="sn-dir down" @click="turn(0, 1)" title="下">↓</button>
        <button class="sn-dir right" @click="turn(1, 0)" title="右">→</button>
      </div>
      <button
        class="sn-btn ghost"
        :disabled="stage !== 'playing' && stage !== 'paused'"
        @click="togglePause"
      >{{ stage === 'paused' ? '▶ 继续' : '⏸ 暂停' }}</button>
    </div>
    <p class="sn-tip">💡 方向键 / WASD、下方按钮、或在棋盘上滑动都能控制；空格暂停 / 继续</p>
  </div>
</template>

<script>
import sound from '../../sound'

/** 🐍 彩虹苹果贪吃蛇：12×12 网格，普通苹果 +10、金苹果 +30；主角为 Q 版 3D 建模 */
const N = 12
const VIEW_KEY = 'mlp_snake_3d'
let snakeMod = null

/** 三档难度：起始速度、每 3 个苹果提速、速度下限、金苹果存活步数、得分倍率 */
const DIFFS = {
  easy:   { key: 'easy',   name: '轻松', emoji: '🌱', base: 210, step: 8,  min: 125, goldTtl: 50, mul: 1,   desc: '慢一点，金苹果留得久' },
  normal: { key: 'normal', name: '普通', emoji: '🌈', base: 175, step: 12, min: 95,  goldTtl: 38, mul: 1,   desc: '原汁原味的手感' },
  hard:   { key: 'hard',   name: '挑战', emoji: '🔥', base: 130, step: 16, min: 70,  goldTtl: 26, mul: 1.5, desc: '更快，但得分 ×1.5' }
}
const DIFF_LIST = [DIFFS.easy, DIFFS.normal, DIFFS.hard]
const DIFF_KEY = 'mlp_snake_diff'

function savedDiff() {
  try {
    const k = localStorage.getItem(DIFF_KEY)
    return DIFFS[k] ? k : 'normal'
  } catch (e) { return 'normal' }
}

function saved3d() {
  try { return localStorage.getItem(VIEW_KEY) !== '0' } catch (e) { return true }
}

export default {
  name: 'GameSnake',
  emits: ['score', 'over'],
  data() {
    return {
      DIFF_LIST,
      diff: savedDiff(),
      stage: 'ready',          // ready | playing | paused | over
      snake: [{ x: 4, y: 6 }, { x: 3, y: 6 }, { x: 2, y: 6 }],
      dir: { x: 1, y: 0 },
      queued: [],              // 转向队列（最多 2 个），一帧内连按也能依次生效
      food: { x: 8, y: 6, gold: false, ttl: 0 },
      score: 0,
      eaten: 0,
      lastGain: 0,
      speed: DIFFS.normal.base,
      timer: null,
      touchStart: null,
      overHint: '',
      // Q 版 3D 建模
      use3d: true,
      model3d: false,
      instBoard: null,
      instFace: null,
      ro: null
    }
  },
  computed: {
    cfg() { return DIFFS[this.diff] || DIFFS.normal },
    faceEmoji() { return this.stage === 'paused' ? '⏸️' : this.stage === 'over' ? '💫' : '🐍' },
    /** 144 个格子的样式：蛇头 / 蛇身 / 苹果 / 金苹果（用 Set 比逐个 find 快） */
    cells() {
      const body = new Set(this.snake.map(s => s.y * N + s.x))
      const head = this.snake.length ? this.snake[0].y * N + this.snake[0].x : -1
      const foodIdx = this.food.y * N + this.food.x
      const out = []
      for (let i = 0; i < N * N; i++) {
        // 按 (行+列) 奇偶着色，才是真正的棋盘格；nth-child 在 12 列下会变成竖条纹
        let cls = (Math.floor(i / N) + (i % N)) % 2 === 0 ? 'alt' : ''
        if (i === head) cls += (cls ? ' ' : '') + 'head'
        else if (body.has(i)) cls += (cls ? ' ' : '') + 'body'
        if (i === foodIdx) cls += (cls ? ' ' : '') + (this.food.gold ? 'gold' : 'apple')
        out.push({ i, cls })
      }
      return out
    }
  },
  mounted() {
    window.addEventListener('keydown', this.onKey)
    this.init3D()
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.onKey)
    this.stop()
    if (this.ro) { this.ro.disconnect(); this.ro = null }
    if (this.instBoard) { this.instBoard.dispose(); this.instBoard = null }
    if (this.instFace) { this.instFace.dispose(); this.instFace = null }
  },
  methods: {
    /* ---------- Q 版 3D 建模（WebGL 不可用时自动回退经典 CSS） ---------- */
    async init3D() {
      try {
        if (!snakeMod) snakeMod = await import('../../pet3d/snake.js')
        const bcv = this.$refs.boardCv
        if (!bcv) return
        this.instBoard = snakeMod.createSnakeScene(bcv, {
          width: bcv.clientWidth || 320, height: bcv.clientHeight || 320
        })
        const fcv = this.$refs.faceCv
        if (fcv) this.instFace = snakeMod.createSnakeScene(fcv, {
          portrait: true,
          width: fcv.clientWidth || 140, height: fcv.clientHeight || 140
        })
        this.model3d = true
        this.use3d = saved3d()
        this.$nextTick(() => { this.resize3D(); this.sync3D() })
        if (window.ResizeObserver && this.$refs.board) {
          this.ro = new ResizeObserver(() => this.resize3D())
          this.ro.observe(this.$refs.board)
        }
      } catch (e) {
        this.model3d = false
        this.use3d = false
      }
    },
    resize3D() {
      const cv = this.$refs.boardCv
      if (cv && this.instBoard && cv.clientWidth) this.instBoard.resize(cv.clientWidth, cv.clientHeight)
    },
    sync3D() {
      if (!this.use3d || !this.instBoard) return
      this.instBoard.setSegments(this.snake)
      this.instBoard.setFood(this.food)
      this.instBoard.setDirection(this.dir)
      this.instBoard.setState(this.stage === 'playing' ? 'play' : (this.stage === 'over' ? 'over' : 'idle'))
    },
    toggle3D() {
      if (!this.model3d) return
      this.use3d = !this.use3d
      try { localStorage.setItem(VIEW_KEY, this.use3d ? '1' : '0') } catch (e) { /* 隐私模式忽略 */ }
      sound.click()
      this.$nextTick(() => { this.resize3D(); this.sync3D() })
    },
    stop() {
      if (this.timer) { clearInterval(this.timer); this.timer = null }
    },
    run() {
      this.stop()
      this.timer = setInterval(this.tick, this.speed)
    },
    pickDiff(k) {
      if (!DIFFS[k]) return
      if (this.stage === 'playing' || this.stage === 'paused') return
      this.diff = k
      try { localStorage.setItem(DIFF_KEY, k) } catch (e) {}
      sound.click()
    },
    start() {
      sound.click()
      this.snake = [{ x: 4, y: 6 }, { x: 3, y: 6 }, { x: 2, y: 6 }]
      this.dir = { x: 1, y: 0 }
      this.queued = []
      this.score = 0
      this.eaten = 0
      this.lastGain = 0
      this.speed = this.cfg.base
      this.overHint = ''
      this.placeFood()
      this.stage = 'playing'
      this.$emit('score', 0)
      this.run()
      this.sync3D()
    },
    backToReady() {
      this.stop()
      this.stage = 'ready'
      sound.click()
      this.sync3D()
    },
    togglePause() {
      if (this.stage === 'playing') {
        this.stop()
        this.stage = 'paused'
        sound.click()
      } else if (this.stage === 'paused') {
        this.stage = 'playing'
        this.run()
        sound.click()
      }
      this.sync3D()
    },
    onKey(e) {
      // 空格：开局 / 暂停 / 继续
      if (e.code === 'Space' || e.key === ' ') {
        e.preventDefault()
        if (this.stage === 'playing' || this.stage === 'paused') this.togglePause()
        else if (this.stage === 'ready' || this.stage === 'over') this.start()
        return
      }
      const map = {
        ArrowUp: [0, -1], w: [0, -1], W: [0, -1],
        ArrowDown: [0, 1], s: [0, 1], S: [0, 1],
        ArrowLeft: [-1, 0], a: [-1, 0], A: [-1, 0],
        ArrowRight: [1, 0], d: [1, 0], D: [1, 0]
      }
      const d = map[e.key]
      if (!d) return
      e.preventDefault()
      this.turn(d[0], d[1])
    },
    turn(x, y) {
      if (this.stage !== 'playing') return
      // 相对「最后一次排队的转向」判断，禁止 180° 掉头，也挡掉重复方向
      const last = this.queued.length ? this.queued[this.queued.length - 1] : this.dir
      if (x === -last.x && y === -last.y) return
      if (x === last.x && y === last.y) return
      if (this.queued.length >= 2) return
      this.queued.push({ x, y })
    },
    onTouchStart(e) {
      const t = e.changedTouches && e.changedTouches[0]
      if (t) this.touchStart = { x: t.clientX, y: t.clientY }
    },
    onTouchEnd(e) {
      if (!this.touchStart) return
      const t = e.changedTouches && e.changedTouches[0]
      if (!t) { this.touchStart = null; return }
      const dx = t.clientX - this.touchStart.x
      const dy = t.clientY - this.touchStart.y
      this.touchStart = null
      if (Math.max(Math.abs(dx), Math.abs(dy)) < 24) return
      if (Math.abs(dx) > Math.abs(dy)) this.turn(dx > 0 ? 1 : -1, 0)
      else this.turn(0, dy > 0 ? 1 : -1)
    },
    placeFood() {
      const busy = new Set(this.snake.map(s => s.y * N + s.x))
      const free = []
      for (let i = 0; i < N * N; i++) if (!busy.has(i)) free.push(i)
      // 蛇铺满全屏（几乎不可能）时直接判胜利式结算，避免死循环
      if (!free.length) { this.gameOver(true); return }
      const idx = free[Math.floor(Math.random() * free.length)]
      const gold = this.eaten > 0 && this.eaten % 4 === 0
      this.food = {
        x: idx % N,
        y: Math.floor(idx / N),
        gold,
        ttl: gold ? this.cfg.goldTtl : 0
      }
    },
    tick() {
      if (this.stage !== 'playing') return
      if (this.queued.length) { this.dir = this.queued.shift() }

      const h = this.snake[0]
      const nx = h.x + this.dir.x
      const ny = h.y + this.dir.y
      if (nx < 0 || ny < 0 || nx >= N || ny >= N) return this.gameOver(false)

      const eating = nx === this.food.x && ny === this.food.y
      // 不吃的时候尾巴会挪走，所以尾巴那一格不算撞
      const body = eating ? this.snake : this.snake.slice(0, -1)
      if (body.some(s => s.x === nx && s.y === ny)) return this.gameOver(false)

      this.snake.unshift({ x: nx, y: ny })

      if (eating) {
        let gain = Math.round((this.food.gold ? 30 : 10) * this.cfg.mul)
        this.eaten += 1
        if (this.eaten % 5 === 0) {
          gain += Math.round(20 * this.cfg.mul)
          sound.twinkle()
        } else if (this.food.gold) {
          sound.magic()
        } else {
          sound.pop()
        }
        this.lastGain = gain
        this.score += gain
        this.$emit('score', this.score)

        if (this.eaten % 3 === 0) {
          this.speed = Math.max(this.cfg.min, this.cfg.base - Math.floor(this.eaten / 3) * this.cfg.step)
          this.run()
        }
        this.placeFood()
      } else {
        this.snake.pop()
        if (this.food.gold) {
          this.food.ttl -= 1
          if (this.food.ttl <= 0) this.food.gold = false
        }
      }
      this.sync3D()
    },
    gameOver(full) {
      this.stop()
      this.stage = 'over'
      sound.fail()
      if (full) this.overHint = '🎉 整块果园都被你吃光啦，太强了！'
      else if (this.eaten < 3) this.overHint = '别急，先熟悉一下转向～也可以换「轻松」难度'
      else if (this.diff === 'hard') this.overHint = '挑战难度能拿 ×1.5 分，再多撑一会儿就破纪录啦！'
      else this.overHint = '换个「挑战」难度试试，得分 ×1.5 哦～'
      this.$emit('over', this.score)
      this.sync3D()
    }
  }
}
</script>

<style scoped>
.sn-wrap { display: flex; flex-direction: column; gap: 12px; }

.sn-hud {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  flex-wrap: wrap;
  padding: 9px 13px; border-radius: 12px;
  background: var(--panel); font-size: 11.5px; color: var(--muted);
}
.sn-hud b { font-size: 15px; color: var(--primary); }
.sn-diff-tag {
  font-weight: 800; color: var(--primary);
  background: var(--card); border: 1px solid var(--border);
  border-radius: 999px; padding: 3px 10px; font-size: 11px;
}
.sn-gold {
  color: #b07d14; font-weight: 800;
  background: #fff5d6; border-radius: 999px; padding: 3px 9px; font-size: 11px;
}
.sn-3d-btn {
  border: 1px solid var(--border); border-radius: 999px; cursor: pointer;
  background: var(--card); color: var(--muted); font-weight: 800;
  font-size: 10.5px; padding: 3px 9px; white-space: nowrap;
}
.sn-3d-btn.on {
  color: #fff; border-color: transparent;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}

.sn-board {
  position: relative; width: 100%; aspect-ratio: 1;
  display: grid; grid-template-columns: repeat(12, 1fr); grid-template-rows: repeat(12, 1fr);
  gap: 0; padding: 6px; box-sizing: border-box;
  border-radius: 16px; border: 1.5px solid var(--border);
  background: var(--panel); overflow: hidden;
  touch-action: none;
}
.sn-canvas {
  position: absolute; inset: 6px; z-index: 1;
  width: calc(100% - 12px); height: calc(100% - 12px);
  display: block; pointer-events: none;
}
.sn-cell { border-radius: 3px; background: #ffffff; }
.sn-cell.alt { background: #f6f0fb; }
.sn-cell.body {
  background: linear-gradient(135deg, var(--mint), var(--blue));
  border-radius: 5px; box-shadow: inset 0 0 0 1px #ffffff66;
}
.sn-cell.head {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  border-radius: 6px; box-shadow: 0 0 8px rgba(160, 108, 213, .5);
}
.sn-cell.apple {
  background: radial-gradient(circle at 35% 30%, #ff9d9d, #e0555f);
  border-radius: 50%; box-shadow: 0 1px 5px rgba(224, 85, 95, .5);
}
.sn-cell.gold {
  background: radial-gradient(circle at 35% 30%, #fff0a8, #f5be37);
  border-radius: 50%; box-shadow: 0 0 12px rgba(245, 190, 55, .9);
  animation: sn-pulse .7s ease-in-out infinite alternate;
}
@keyframes sn-pulse { to { transform: scale(.8); } }

/* 3D 模式下所有格子回到棋盘格底色，蛇 / 苹果交给 canvas 渲染 */
.sn-board.sn-3d .sn-cell {
  background: #ffffff; box-shadow: none;
  border-radius: 3px; animation: none;
}
.sn-board.sn-3d .sn-cell.alt { background: #f6f0fb; }

.sn-flash {
  position: absolute; top: 12px; left: 50%; transform: translateX(-50%);
  font-size: 17px; font-weight: 800; color: var(--primary);
  text-shadow: 0 1px 4px #fff; pointer-events: none;
  animation: sn-float .85s ease-out forwards;
}
@keyframes sn-float {
  0% { opacity: 0; margin-top: 8px; }
  25% { opacity: 1; }
  100% { opacity: 0; margin-top: -18px; }
}

.sn-mask {
  position: absolute; inset: 0; z-index: 2;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 7px; padding: 14px; text-align: center;
  background: rgba(246, 241, 251, .94); backdrop-filter: blur(2px);
  overflow-y: auto;
}
.sn-face {
  height: 132px; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sn-face3d { width: 140px; height: 140px; display: block; }
.sn-face-emoji { font-size: 38px; line-height: 1; }
.sn-mask h3 { margin: 0; font-size: 17px; color: var(--text); }
.sn-mask p { margin: 0; font-size: 11.5px; color: var(--muted); line-height: 1.5; }
.sn-over-hint { font-size: 11px !important; color: var(--primary) !important; font-weight: 700; }

.sn-diff-label { font-size: 11px; color: var(--muted); font-weight: 700; margin-top: 2px; }
.sn-diff-row { display: flex; gap: 6px; width: 100%; }
.sn-diff-btn {
  flex: 1; min-width: 0; cursor: pointer; text-align: center;
  border: 1.5px solid var(--border); border-radius: 11px;
  background: var(--card); padding: 7px 3px;
  display: flex; flex-direction: column; gap: 2px;
  transition: transform .12s, border-color .15s, background .15s;
}
.sn-diff-btn b { font-size: 11.5px; color: var(--text); white-space: nowrap; }
.sn-diff-btn em { font-style: normal; font-size: 9.5px; color: var(--muted); line-height: 1.3; }
.sn-diff-btn.on {
  border-color: var(--primary);
  background: linear-gradient(160deg, color-mix(in srgb, var(--pink) 22%, var(--card)), var(--card));
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary) 14%, transparent);
}
.sn-diff-btn.on b { color: var(--primary); }
.sn-diff-btn:active { transform: scale(.96); }

.sn-rules {
  margin: 2px 0 4px; padding: 0; list-style: none;
  display: flex; flex-direction: column; gap: 4px;
  font-size: 11px; color: var(--text); text-align: left; width: 100%;
}
.sn-rules li {
  padding: 5px 9px; border-radius: 9px;
  background: var(--card); border: 1px solid var(--border);
}

.sn-bottom { display: flex; align-items: center; gap: 12px; }
.sn-pad {
  flex: 1; display: grid; gap: 5px;
  grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(2, 38px);
}
.sn-dir {
  border: 1.5px solid var(--border); border-radius: 11px;
  background: var(--card); color: var(--primary);
  font-size: 19px; font-weight: 800; cursor: pointer;
  transition: transform .1s, background .15s;
}
.sn-dir:hover { background: var(--panel); }
.sn-dir:active { transform: scale(.93); }
.sn-dir.up { grid-column: 2; grid-row: 1; }
.sn-dir.left { grid-column: 1; grid-row: 2; }
.sn-dir.down { grid-column: 2; grid-row: 2; }
.sn-dir.right { grid-column: 3; grid-row: 2; }

.sn-btn {
  border: none; cursor: pointer; font-weight: 800;
  padding: 10px 20px; border-radius: 999px; font-size: 13.5px;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 6px 14px rgba(160, 108, 213, .3);
  transition: transform .12s, filter .12s;
}
.sn-btn:hover { filter: brightness(1.07); transform: translateY(-1px); }
.sn-btn:active { transform: scale(.96); }
.sn-btn:disabled { opacity: .5; cursor: not-allowed; transform: none; }
.sn-btn.big { padding: 10px 26px; font-size: 14px; }
.sn-btn.ghost {
  flex-shrink: 0; color: var(--primary); background: var(--card);
  border: 1.5px solid var(--border); box-shadow: none; padding: 10px 14px; font-size: 12.5px;
}
.sn-tip { margin: 0; font-size: 11.5px; color: var(--muted); text-align: center; }
</style>
