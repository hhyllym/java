<template>
  <div class="game-dash">
    <!-- 进度 -->
    <div class="dash-hud">
      <span>第 {{ idx + 1 }} / {{ total }} 轮</span>
      <span v-if="results.length" class="dash-avg">平均 {{ avg }} ms</span>
    </div>

    <!-- 反应区 -->
    <div
      class="dash-stage"
      :class="state"
      @pointerdown="tap"
    >
      <template v-if="state === 'idle'">
        <div class="dash-emoji">⚡</div>
        <p class="dash-title">闪电反应</p>
        <p class="dash-sub">屏幕变<b class="go">绿</b>的瞬间，立刻点下去！</p>
        <p class="dash-sub small">提前点击算抢跑，本轮回炉重来</p>
        <button class="dash-btn" @pointerdown.stop="begin">开始第一轮</button>
      </template>

      <template v-else-if="state === 'ready'">
        <div class="dash-emoji waiting">🛑</div>
        <p class="dash-title">等待…</p>
        <p class="dash-sub">别急，绿了再点！</p>
      </template>

      <template v-else-if="state === 'go'">
        <div class="dash-emoji go">💚</div>
        <p class="dash-title go">就是现在，点！</p>
      </template>

      <template v-else-if="state === 'result'">
        <div class="dash-emoji">{{ lastMs }}<small>ms</small></div>
        <p class="dash-title">{{ rating }}</p>
        <p class="dash-sub">{{ tip }}</p>
        <button class="dash-btn" @pointerdown.stop="nextRound">
          {{ idx + 1 >= total ? '查看成绩' : '下一轮' }}
        </button>
      </template>

      <template v-else-if="state === 'foul'">
        <div class="dash-emoji">🚫</div>
        <p class="dash-title">抢跑啦！</p>
        <p class="dash-sub">这一轮不算，重新来</p>
        <button class="dash-btn" @pointerdown.stop="retry">重试本轮</button>
      </template>
    </div>

    <!-- 每轮成绩 -->
    <div v-if="results.length" class="dash-list">
      <span v-for="(r, i) in results" :key="i" class="dash-chip" :class="{ foul: r === 0 }">
        第{{ i + 1 }}轮 {{ r === 0 ? '抢跑' : r + 'ms' }}
      </span>
    </div>
  </div>
</template>

<script>
const TOTAL = 5

export default {
  name: 'GameDash',
  emits: ['score', 'over'],
  data() {
    return {
      state: 'idle',     // idle | ready | go | result | foul
      idx: 0,
      total: TOTAL,
      results: [],
      lastMs: 0,
      goAt: 0,
      waitTimer: null
    }
  },
  computed: {
    avg() {
      const valid = this.results.filter(r => r > 0)
      if (!valid.length) return 0
      return Math.round(valid.reduce((s, r) => s + r, 0) / valid.length)
    },
    rating() {
      const a = this.avg
      if (a === 0) return '再试一次！'
      if (a < 250) return '音速彩虹！🦄'
      if (a < 350) return '极速飞行！⚡'
      if (a < 500) return '相当快！💨'
      return '继续加油！🐴'
    },
    tip() {
      const a = this.avg
      if (a === 0) return ''
      if (a < 250) return '这反应速度，云宝本宝！'
      if (a < 350) return '职业选手的水平了！'
      if (a < 500) return '比大多数人快，多练几轮还能提升'
      return '试试盯住屏幕中央，别分心～'
    }
  },
  beforeUnmount() {
    clearTimeout(this.waitTimer)
  },
  methods: {
    begin() {
      this.idx = 0
      this.results = []
      this.readyRound()
    },
    readyRound() {
      this.state = 'ready'
      this.waitTimer = setTimeout(() => {
        this.state = 'go'
        this.goAt = performance.now()
      }, 800 + Math.random() * 2200)
    },
    retry() {
      this.readyRound()
    },
    nextRound() {
      if (this.idx + 1 >= this.total) {
        this.finish()
      } else {
        this.idx += 1
        this.readyRound()
      }
    },
    tap() {
      if (this.state === 'ready') {
        // 抢跑
        clearTimeout(this.waitTimer)
        this.state = 'foul'
        this.results[this.idx] = 0
      } else if (this.state === 'go') {
        const ms = Math.round(performance.now() - this.goAt)
        this.lastMs = ms
        this.results[this.idx] = ms
        this.state = 'result'
      }
    },
    finish() {
      // 计分：平均反应越快分越高；抢跑按 0 分处理
      const valid = this.results.filter(r => r > 0)
      let score = 0
      if (valid.length) {
        const a = valid.reduce((s, r) => s + r, 0) / valid.length
        score = Math.max(0, Math.round(650 - a))
      }
      this.$emit('over', score)
    }
  }
}
</script>

<style scoped>
.dash-hud {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.dash-avg { color: var(--primary); font-weight: 700; }

.dash-stage {
  border-radius: 16px; padding: 26px 16px;
  text-align: center; min-height: 280px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 4px;
  background: var(--panel); border: 2px solid var(--border);
  cursor: pointer; user-select: none; touch-action: manipulation;
  transition: background .1s, border-color .1s;
}
.dash-stage.ready { background: #fbe3e3; border-color: #f0b8b8; }
.dash-stage.go { background: #b8ecd2; border-color: #58b98a; }

.dash-emoji { font-size: 44px; line-height: 1.2; }
.dash-emoji small { font-size: 18px; margin-left: 2px; }
.dash-emoji.go { animation: pop .25s ease; }
.dash-emoji.waiting { animation: pulse 1s infinite; }
@keyframes pop { from { transform: scale(.4); } }
@keyframes pulse { 50% { opacity: .6; } }

.dash-title { margin: 6px 0 2px; font-size: 18px; font-weight: 800; }
.dash-title.go { color: #2b6b4e; }
.dash-sub { margin: 0; font-size: 12.5px; color: var(--muted); line-height: 1.6; }
.dash-sub.small { font-size: 11px; opacity: .75; }
.dash-sub .go { color: var(--success); }

.dash-btn {
  margin-top: 14px; border: none; cursor: pointer;
  padding: 10px 26px; border-radius: 999px;
  color: #fff; font-size: 14px; font-weight: 700;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160,108,213,.3);
}

.dash-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.dash-chip {
  font-size: 11px; padding: 4px 10px; border-radius: 999px;
  background: var(--panel); color: var(--primary); font-weight: 700;
}
.dash-chip.foul { color: var(--danger); }
</style>
