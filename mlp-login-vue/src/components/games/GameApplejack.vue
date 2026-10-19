<template>
  <div class="game-applejack">
    <!-- 算式 + 倒计时 -->
    <div class="farm-hud">
      <span class="farm-eq">🧮 {{ a }} + {{ b }} = ?</span>
      <span class="farm-timer" :class="{ urgent: left <= 10 }">⏱ {{ left }}s</span>
    </div>

    <!-- 农场 -->
    <div ref="field" class="farm-field" @pointermove="moveBasket" @pointerdown="moveBasket">
      <!-- 掉落的苹果 -->
      <div
        v-for="ap in apples"
        :key="ap.id"
        class="farm-apple"
        :class="{ caught: ap.caught }"
        :style="{ left: ap.x + '%', top: ap.y + 'px' }"
      >🍎<em>{{ ap.n }}</em></div>

      <!-- 篮子 -->
      <div class="farm-basket" :class="{ shake: shaking }" :style="{ left: basketX + '%' }">🧺</div>
    </div>

    <div class="farm-tip">💡 左右移动鼠标（或手指）接住正确答案的苹果；接到错的会扣分哦</div>
  </div>
</template>

<script>
const DURATION = 60        // 游戏时长（秒）
const SPAWN_MS = 1100      // 苹果生成间隔
const FALL_SPEED = 1.6     // 每帧下落像素（60fps）
const FIELD_H = 340        // 农场高度（与 CSS 一致）

export default {
  name: 'GameApplejack',
  emits: ['score', 'over'],
  data() {
    return {
      a: 0,
      b: 0,
      target: 0,
      apples: [],
      basketX: 50,
      score: 0,
      left: DURATION,
      shaking: false,
      raf: null,
      spawnTimer: null,
      tickTimer: null,
      nextId: 1
    }
  },
  created() {
    this.newEquation()
    this.start()
  },
  beforeUnmount() {
    this.stop()
  },
  methods: {
    newEquation() {
      this.a = 2 + Math.floor(Math.random() * 9)
      this.b = 2 + Math.floor(Math.random() * 9)
      this.target = this.a + this.b
    },
    start() {
      this.spawnTimer = setInterval(this.spawn, SPAWN_MS)
      this.tickTimer = setInterval(() => {
        this.left -= 1
        if (this.left <= 0) this.finish()
      }, 1000)
      const loop = () => {
        this.fall()
        this.raf = requestAnimationFrame(loop)
      }
      this.raf = requestAnimationFrame(loop)
    },
    stop() {
      clearInterval(this.spawnTimer)
      clearInterval(this.tickTimer)
      if (this.raf) cancelAnimationFrame(this.raf)
    },
    /** 生成一颗苹果：70% 是正确答案，30% 是干扰项 */
    spawn() {
      const correct = Math.random() < 0.7
      let n = this.target
      if (!correct) {
        do {
          n = this.target + (Math.floor(Math.random() * 7) - 3)
        } while (n === this.target || n < 1)
      }
      this.apples.push({
        id: this.nextId++,
        n,
        x: 10 + Math.random() * 80,
        y: -30,
        correct,
        caught: false
      })
    },
    fall() {
      const survivors = []
      for (const ap of this.apples) {
        if (ap.caught) continue
        ap.y += FALL_SPEED
        // 落到篮子区域判定接住
        if (ap.y >= FIELD_H - 78 && ap.y <= FIELD_H - 20) {
          const basketCenter = this.basketX
          if (Math.abs(ap.x - basketCenter) < 9) {
            this.catch(ap)
            continue
          }
        }
        if (ap.y > FIELD_H) continue // 落地消失
        survivors.push(ap)
      }
      this.apples = survivors
    },
    catch(ap) {
      ap.caught = true
      if (ap.correct) {
        this.score += 15
        this.newEquation()
        this.$emit('score', this.score)
      } else {
        this.score = Math.max(0, this.score - 10)
        this.shaking = true
        setTimeout(() => { this.shaking = false }, 350)
        this.$emit('score', this.score)
      }
    },
    moveBasket(e) {
      const field = this.$refs.field
      if (!field) return
      const rect = field.getBoundingClientRect()
      const x = ((e.clientX - rect.left) / rect.width) * 100
      this.basketX = Math.max(6, Math.min(94, x))
    },
    finish() {
      this.stop()
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.farm-hud {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.farm-eq {
  font-size: 17px; font-weight: 800; color: var(--primary);
  background: var(--panel); border-radius: 10px; padding: 6px 14px;
}
.farm-timer { font-size: 13px; font-weight: 700; color: var(--muted); }
.farm-timer.urgent { color: var(--danger); }

.farm-field {
  position: relative; height: 340px;
  background: linear-gradient(180deg, #fff 0%, var(--panel) 100%);
  border: 1.5px solid var(--border); border-radius: 14px;
  overflow: hidden; cursor: none; touch-action: none;
}

.farm-apple {
  position: absolute; top: 0; transform: translate(-50%, 0);
  font-size: 26px; line-height: 1;
  filter: drop-shadow(0 3px 4px rgba(0,0,0,.15));
}
.farm-apple em {
  font-style: normal; font-size: 10px; font-weight: 800;
  color: #fff; background: rgba(0,0,0,.45);
  border-radius: 999px; padding: 0 5px;
  position: absolute; left: 50%; top: -14px; transform: translateX(-50%);
  white-space: nowrap;
}
.farm-apple.caught { animation: applePop .25s ease forwards; }
@keyframes applePop {
  to { transform: translate(-50%, 0) scale(1.8); opacity: 0; }
}

.farm-basket {
  position: absolute; bottom: 10px; transform: translate(-50%, 0);
  font-size: 38px; line-height: 1;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,.18));
  transition: left .05s linear;
}
.farm-basket.shake { animation: basketShake .35s ease; }
@keyframes basketShake {
  0%, 100% { margin-left: 0; }
  25% { margin-left: -10px; }
  75% { margin-left: 10px; }
}

.farm-tip {
  margin-top: 10px; font-size: 11px; color: var(--muted);
  text-align: center; line-height: 1.6;
}
</style>
