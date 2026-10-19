<template>
  <div class="game-rarity">
    <!-- 轮次 -->
    <div class="gem-hud">
      <span>第 {{ idx + 1 }} / {{ total }} 轮设计</span>
      <span class="gem-timer">⏱ {{ left }}s</span>
    </div>

    <!-- 设计主题 -->
    <div class="gem-theme">
      <div class="gem-theme-label">本期设计主题</div>
      <div class="gem-theme-name">{{ theme.name }}</div>
      <div class="gem-theme-desc">{{ theme.desc }}</div>
    </div>

    <!-- 宝石盘 -->
    <div class="gem-board">
      <button
        v-for="(g, i) in gems"
        :key="i"
        class="gem"
        :class="{ picked: picked.includes(i), right: revealed && g.ok, wrong: revealed && picked.includes(i) && !g.ok }"
        :style="{ '--gc': g.color }"
        :disabled="revealed"
        @click="pick(i)"
      >💎</button>
    </div>

    <!-- 操作 -->
    <div class="gem-actions">
      <template v-if="!revealed">
        <p class="gem-hint">选出属于「{{ theme.name }}」色系的宝石，然后提交</p>
        <button class="gem-btn" :disabled="!picked.length" @click="submit">提交设计</button>
      </template>
      <template v-else>
        <p class="gem-result" :class="{ good: roundScore > 0 }">
          {{ roundResult }}
        </p>
        <button class="gem-btn" @pointerdown.stop="next">
          {{ idx + 1 >= total ? '查看成绩' : '下一轮' }}
        </button>
      </template>
    </div>
  </div>
</template>

<script>
const TOTAL = 3
const DURATION = 90

/** 色系主题：ok 颜色属于该主题，干扰色来自其它主题 */
const THEMES = [
  {
    name: '海洋之歌', desc: '蓝绿色系，像晴朗的地中海',
    palette: ['#6ec6e6', '#8fc7f0', '#7fd4d8', '#5b9bd5']
  },
  {
    name: '暖阳午后', desc: '橙黄色系，像秋天的苹果园',
    palette: ['#f0a06a', '#f7e08a', '#f5c08a', '#f2a950']
  },
  {
    name: '森林漫步', desc: '绿色系，像柔柔家后的小树林',
    palette: ['#7fd4a8', '#a7e3c9', '#58b98a', '#b8ecd2']
  },
  {
    name: '星空之夜', desc: '紫色系，像紫悦的魔法夜空',
    palette: ['#8f7ae8', '#b79cf0', '#c9a0e8', '#a06cd5']
  }
]

export default {
  name: 'GameRarity',
  emits: ['score', 'over'],
  data() {
    return {
      idx: 0,
      total: TOTAL,
      left: DURATION,
      theme: THEMES[0],
      gems: [],
      picked: [],
      revealed: false,
      roundScore: 0,
      score: 0,
      timer: null,
      finished: false,
      roundNo: 0   // 变 key 用
    }
  },
  computed: {
    roundResult() {
      if (this.roundScore > 0) return `设计通过！本轮 +${this.roundScore} 分`
      if (this.roundScore < 0) return `宝石选错啦，本轮 ${this.roundScore} 分`
      return '没有选出宝石，本轮不得分'
    }
  },
  created() {
    this.buildRound()
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.timeUp()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    buildRound() {
      this.roundNo += 1
      // 主题轮换 + 随机
      const t = THEMES[Math.floor(Math.random() * THEMES.length)]
      this.theme = t
      const others = THEMES.filter(x => x.name !== t.name)
      // 3 颗对的 + 6 颗干扰，打乱
      const okCount = 3
      const gems = []
      t.palette.slice(0, okCount).forEach(c => gems.push({ color: c, ok: true }))
      while (gems.length < 9) {
        const o = others[Math.floor(Math.random() * others.length)]
        gems.push({ color: o.palette[Math.floor(Math.random() * o.palette.length)], ok: false })
      }
      for (let i = gems.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[gems[i], gems[j]] = [gems[j], gems[i]]
      }
      this.gems = gems
      this.picked = []
      this.revealed = false
      this.roundScore = 0
    },
    pick(i) {
      if (this.revealed || this.finished) return
      const at = this.picked.indexOf(i)
      if (at >= 0) this.picked.splice(at, 1)
      else this.picked.push(i)
    },
    submit() {
      if (!this.picked.length || this.revealed) return
      this.revealed = true
      let s = 0
      for (const i of this.picked) {
        s += this.gems[i].ok ? 15 : -8
      }
      this.roundScore = s
      this.score += s
      this.$emit('score', this.score)
    },
    next() {
      if (this.idx + 1 >= this.total) {
        this.finish()
      } else {
        this.idx += 1
        this.buildRound()
      }
    },
    finish() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      this.$emit('over', Math.max(0, this.score))
    },
    timeUp() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      this.$emit('over', Math.max(0, this.score))
    }
  }
}
</script>

<style scoped>
.gem-hud {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 12px;
}
.gem-timer { font-weight: 700; color: var(--primary); }

.gem-theme {
  text-align: center; padding: 14px;
  background: var(--panel); border-radius: 14px; margin-bottom: 14px;
}
.gem-theme-label { font-size: 10.5px; color: var(--muted); }
.gem-theme-name {
  font-size: 19px; font-weight: 800; color: var(--primary);
  margin: 4px 0 3px;
}
.gem-theme-desc { font-size: 11.5px; color: var(--muted); }

.gem-board {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;
  max-width: 300px; margin: 0 auto;
}
.gem {
  border: none; background: none; cursor: pointer; padding: 0;
  font-size: 44px; line-height: 1;
  filter: drop-shadow(0 4px 6px rgba(0,0,0,.15));
  transition: transform .15s, filter .15s;
  position: relative;
}
.gem::before {
  content: ""; position: absolute; inset: 6px;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #ffffffaa, transparent 55%), var(--gc);
  opacity: .55; z-index: -1;
}
.gem:hover:not(:disabled) { transform: translateY(-4px) scale(1.08); }
.gem.picked {
  transform: translateY(-4px) scale(1.12);
  filter: drop-shadow(0 0 10px var(--gc));
}
.gem.picked::after {
  content: "✓"; position: absolute; right: -4px; top: -6px;
  width: 20px; height: 20px; border-radius: 50%;
  background: var(--success); color: #fff;
  font-size: 12px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.gem.right { animation: gemRight .4s ease; }
.gem.wrong { animation: gemWrong .4s ease; }
@keyframes gemRight { 50% { transform: scale(1.3); } }
@keyframes gemWrong {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}

.gem-actions { text-align: center; margin-top: 16px; }
.gem-hint { font-size: 12px; color: var(--muted); margin: 0 0 10px; }
.gem-result { font-size: 13px; font-weight: 700; color: var(--danger); margin: 0 0 10px; }
.gem-result.good { color: var(--success); }

.gem-btn {
  border: none; cursor: pointer;
  padding: 10px 30px; border-radius: 999px;
  color: #fff; font-size: 14px; font-weight: 700;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160,108,213,.3);
}
.gem-btn:disabled { opacity: .55; cursor: default; }
</style>
