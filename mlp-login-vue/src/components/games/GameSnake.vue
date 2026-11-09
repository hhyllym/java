<template>
  <div class="sn-wrap">
    <div class="sn-hud">
      <span>🍎 吃了 {{ eaten }} 个</span>
      <span v-if="food.gold && stage === 'playing'" class="sn-gold">✨ 金苹果剩 {{ food.ttl }} 步</span>
      <span>得分 <b>{{ score }}</b></span>
    </div>

    <div class="sn-board">
      <div
        v-for="c in cells"
        :key="c.i"
        class="sn-cell"
        :class="c.cls"
      ></div>

      <!-- 吃到东西时飘一下分数，key 变了动画就重播，不用额外定时器 -->
      <div v-if="eaten" :key="eaten" class="sn-flash">+{{ lastGain }}</div>

      <div v-if="stage !== 'playing'" class="sn-mask">
        <template v-if="stage === 'ready'">
          <div class="sn-face">🐍</div>
          <h3>彩虹苹果贪吃蛇</h3>
          <p>金穗的果园里来了一条馋嘴小蛇～</p>
          <ul class="sn-rules">
            <li>🍎 吃苹果 +10 分，身体变长、速度变快</li>
            <li>✨ 每吃 4 个冒出金苹果，+30 分（会过期）</li>
            <li>🎉 每吃满 5 个，额外奖励 +20 分</li>
            <li>💥 撞到墙或自己就结束，不能直接掉头</li>
          </ul>
          <button class="sn-btn big" @click="start">开始游戏！</button>
        </template>
        <template v-else-if="stage === 'paused'">
          <div class="sn-face">⏸️</div>
          <h3>歇一口气</h3>
          <p>小蛇原地等你回来～</p>
          <button class="sn-btn big" @click="togglePause">继续游戏</button>
        </template>
        <template v-else>
          <div class="sn-face">💫</div>
          <h3>本局结束</h3>
          <p>一共吃掉 {{ eaten }} 个苹果，拿到 {{ score }} 分！</p>
          <button class="sn-btn big" @click="start">再来一局</button>
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
    <p class="sn-tip">💡 方向键 / WASD 或下方按钮都能控制，别急着掉头哦</p>
  </div>
</template>

<script>
import sound from '../../sound'

/** 🐍 彩虹苹果贪吃蛇：12×12 网格，普通苹果 +10、金苹果 +30，撞墙撞身体结束 */
const N = 12
const BASE_SPEED = 175

export default {
  name: 'GameSnake',
  emits: ['score', 'over'],
  data() {
    return {
      stage: 'ready',          // ready | playing | paused | over
      snake: [{ x: 4, y: 6 }, { x: 3, y: 6 }, { x: 2, y: 6 }],
      dir: { x: 1, y: 0 },
      queued: null,            // 下一帧才生效，避免一帧内连按导致反向
      food: { x: 8, y: 6, gold: false, ttl: 0 },
      score: 0,
      eaten: 0,
      lastGain: 0,
      speed: BASE_SPEED,
      timer: null
    }
  },
  computed: {
    /** 144 个格子的样式，蛇头 / 蛇身 / 苹果 / 金苹果 */
    cells() {
      const out = []
      for (let y = 0; y < N; y++) {
        for (let x = 0; x < N; x++) {
          const at = this.snake.findIndex(s => s.x === x && s.y === y)
          let cls = ''
          if (at === 0) cls = 'head'
          else if (at > 0) cls = 'body'
          if (this.food.x === x && this.food.y === y) {
            cls += (cls ? ' ' : '') + (this.food.gold ? 'gold' : 'apple')
          }
          out.push({ i: y * N + x, cls })
        }
      }
      return out
    }
  },
  mounted() {
    window.addEventListener('keydown', this.onKey)
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.onKey)
    this.stop()
  },
  methods: {
    stop() {
      if (this.timer) { clearInterval(this.timer); this.timer = null }
    },
    run() {
      this.stop()
      this.timer = setInterval(this.tick, this.speed)
    },
    start() {
      sound.click()
      this.snake = [{ x: 4, y: 6 }, { x: 3, y: 6 }, { x: 2, y: 6 }]
      this.dir = { x: 1, y: 0 }
      this.queued = null
      this.score = 0
      this.eaten = 0
      this.lastGain = 0
      this.speed = BASE_SPEED
      this.placeFood()
      this.stage = 'playing'
      this.$emit('score', 0)
      this.run()
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
    },
    onKey(e) {
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
      // 只能相对「当前实际方向」转，禁止 180° 掉头
      if (x === -this.dir.x && y === -this.dir.y) return
      this.queued = { x, y }
    },
    placeFood() {
      const busy = new Set(this.snake.map(s => s.y * N + s.x))
      const free = []
      for (let i = 0; i < N * N; i++) if (!busy.has(i)) free.push(i)
      // 蛇铺满全屏（几乎不可能）时直接判胜利式结算，避免死循环
      if (!free.length) { this.gameOver(); return }
      const idx = free[Math.floor(Math.random() * free.length)]
      const gold = this.eaten > 0 && this.eaten % 4 === 0
      this.food = {
        x: idx % N,
        y: Math.floor(idx / N),
        gold,
        ttl: gold ? 38 : 0
      }
    },
    tick() {
      if (this.stage !== 'playing') return
      if (this.queued) { this.dir = this.queued; this.queued = null }

      const h = this.snake[0]
      const nx = h.x + this.dir.x
      const ny = h.y + this.dir.y
      if (nx < 0 || ny < 0 || nx >= N || ny >= N) return this.gameOver()

      const eating = nx === this.food.x && ny === this.food.y
      // 不吃的时候尾巴会挪走，所以尾巴那一格不算撞
      const body = eating ? this.snake : this.snake.slice(0, -1)
      if (body.some(s => s.x === nx && s.y === ny)) return this.gameOver()

      this.snake.unshift({ x: nx, y: ny })

      if (eating) {
        let gain = this.food.gold ? 30 : 10
        this.eaten += 1
        if (this.eaten % 5 === 0) {
          gain += 20
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
          this.speed = Math.max(95, BASE_SPEED - Math.floor(this.eaten / 3) * 12)
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
    },
    gameOver() {
      this.stop()
      this.stage = 'over'
      sound.fail()
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.sn-wrap { display: flex; flex-direction: column; gap: 12px; }

.sn-hud {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 9px 13px; border-radius: 12px;
  background: var(--panel); font-size: 12px; color: var(--muted);
}
.sn-hud b { font-size: 15px; color: var(--primary); }
.sn-gold {
  color: #b07d14; font-weight: 800;
  background: #fff5d6; border-radius: 999px; padding: 3px 9px; font-size: 11px;
}

.sn-board {
  position: relative; width: 100%; aspect-ratio: 1;
  display: grid; grid-template-columns: repeat(12, 1fr); grid-template-rows: repeat(12, 1fr);
  gap: 1px; padding: 6px; box-sizing: border-box;
  border-radius: 16px; border: 1.5px solid var(--border);
  background: var(--panel); overflow: hidden;
}
.sn-cell { border-radius: 3px; background: #ffffff; }
.sn-cell:nth-child(even) { background: #fbf8fe; }
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
  gap: 8px; padding: 18px; text-align: center;
  background: rgba(246, 241, 251, .93); backdrop-filter: blur(2px);
}
.sn-face { font-size: 44px; line-height: 1; }
.sn-mask h3 { margin: 0; font-size: 18px; color: var(--text); }
.sn-mask p { margin: 0; font-size: 12px; color: var(--muted); }
.sn-rules {
  margin: 4px 0 8px; padding: 0; list-style: none;
  display: flex; flex-direction: column; gap: 5px;
  font-size: 11.5px; color: var(--text); text-align: left;
}
.sn-rules li {
  padding: 6px 10px; border-radius: 9px;
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
.sn-btn.big { padding: 12px 30px; font-size: 15px; }
.sn-btn.ghost {
  flex-shrink: 0; color: var(--primary); background: var(--card);
  border: 1.5px solid var(--border); box-shadow: none; padding: 10px 14px; font-size: 12.5px;
}
.sn-tip { margin: 0; font-size: 11.5px; color: var(--muted); text-align: center; }
</style>
