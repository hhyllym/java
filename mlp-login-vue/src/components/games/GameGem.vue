<template>
  <div class="game-gem">
    <div class="gem-hud">
      <span>第 {{ round }} / {{ TOTAL }} 轮</span>
      <span v-if="combo >= 2" class="gem-combo">🔥 连对 {{ combo }}</span>
      <span>得分 {{ score }}</span>
    </div>

    <div class="gem-target">
      <p class="gem-ask">💎 找出和这颗<b>一模一样</b>的宝石</p>
      <div class="gem-big">
        <span class="gem-shape" :class="'shape-' + target.shape" :style="{ '--gc': target.color }">
          <i>{{ target.facets }}</i>
        </span>
      </div>
      <p class="gem-hint">形状、颜色、刻面数字都要对上才算数</p>
    </div>

    <div class="gem-options">
      <button
        v-for="o in options"
        :key="o.key"
        class="gem-opt"
        :class="{ right: locked && o.key === target.key, wrong: locked && o.key === picked && flash === 'wrong' }"
        :disabled="locked"
        @click="pick(o)"
      >
        <span class="gem-shape" :class="'shape-' + o.shape" :style="{ '--gc': o.color }">
          <i>{{ o.facets }}</i>
        </span>
      </button>
    </div>

    <div v-if="flash === 'right'" class="gem-feedback ok">✨ 火眼金睛！+{{ lastGain }} 分</div>
    <div v-else-if="flash === 'wrong'" class="gem-feedback no">差了一点点，-8 分</div>
  </div>
</template>

<script>
/** 💎 宝石鉴宝：形状 + 颜色 + 刻面数字三项全一致才算找到，8 轮一局 */
const SHAPES = ['diamond', 'round', 'square', 'star']
const COLORS = ['#e8697d', '#4a9fe0', '#57b98b', '#9a7ae0', '#f0a06a', '#e3b93f']
const TOTAL = 8

export default {
  name: 'GameGem',
  emits: ['score', 'over'],
  data() {
    return {
      TOTAL,
      round: 1,
      score: 0,
      combo: 0,
      lastGain: 0,
      target: { shape: 'diamond', color: COLORS[0], facets: 1 },
      options: [],
      locked: false,
      picked: '',
      flash: '',
      timers: []
    }
  },
  created() {
    this.newRound()
  },
  beforeUnmount() {
    this.timers.forEach(clearTimeout)
  },
  methods: {
    keyOf(g) {
      return [g.shape, g.color, g.facets].join('|')
    },
    randomGem() {
      return {
        shape: SHAPES[Math.floor(Math.random() * SHAPES.length)],
        color: COLORS[Math.floor(Math.random() * COLORS.length)],
        facets: 1 + Math.floor(Math.random() * 6)
      }
    },
    /** 6 个候选里只有 1 个和目标完全相同，其余至少差一项且互不重复 */
    newRound() {
      const t = this.randomGem()
      t.key = this.keyOf(t)
      this.target = t
      const keys = [t.key]
      const opts = []
      let guard = 0
      while (opts.length < 5 && guard++ < 300) {
        const o = this.randomGem()
        const k = this.keyOf(o)
        if (keys.includes(k)) continue
        keys.push(k)
        o.key = k
        opts.push(o)
      }
      const all = opts.concat([t])
      for (let i = all.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        const tmp = all[i]; all[i] = all[j]; all[j] = tmp
      }
      this.options = all
      this.locked = false
      this.picked = ''
      this.flash = ''
    },
    pick(o) {
      if (this.locked) return
      this.locked = true
      this.picked = o.key
      if (o.key === this.target.key) {
        this.combo += 1
        this.lastGain = 20 + (this.combo >= 3 ? 5 : 0)
        this.score += this.lastGain
        this.flash = 'right'
      } else {
        this.combo = 0
        this.score = Math.max(0, this.score - 8)
        this.flash = 'wrong'
      }
      this.$emit('score', this.score)
      this.timers.push(setTimeout(() => {
        if (this.round >= TOTAL) {
          this.$emit('over', this.score)
          return
        }
        this.round += 1
        this.newRound()
      }, 520))
    }
  }
}
</script>

<style scoped>
.gem-hud {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.gem-combo { color: var(--primary); font-weight: 700; }
.gem-target {
  background: var(--panel); border-radius: 16px; padding: 14px;
  text-align: center;
}
.gem-ask { font-size: 13px; color: var(--text); margin: 0 0 12px; }
.gem-ask b { color: var(--primary); }
.gem-big { display: flex; justify-content: center; margin-bottom: 10px; }
.gem-big .gem-shape { width: 64px; height: 64px; }
.gem-big .gem-shape i { font-size: 20px; }
.gem-hint { font-size: 11px; color: var(--muted); margin: 0; }

.gem-shape {
  display: inline-flex; align-items: center; justify-content: center;
  width: 46px; height: 46px; background: var(--gc);
  position: relative; flex-shrink: 0;
}
.gem-shape i {
  font-style: normal; font-size: 14px; font-weight: 800; color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,.28);
}
.shape-diamond { clip-path: polygon(50% 0, 100% 38%, 50% 100%, 0 38%); }
.shape-round { border-radius: 50%; }
.shape-square { border-radius: 9px; }
.shape-star {
  clip-path: polygon(50% 0, 61% 34%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 34%);
}

.gem-options {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 14px;
}
.gem-opt {
  border: 2px solid var(--border); background: var(--card);
  border-radius: 14px; min-height: 76px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: transform .12s, border-color .12s;
}
.gem-opt:hover:not(:disabled) { transform: translateY(-3px); border-color: var(--primary); }
.gem-opt:disabled { cursor: default; }
.gem-opt.right { border-color: var(--success); background: rgba(76,175,125,.1); }
.gem-opt.wrong { border-color: var(--danger); background: rgba(224,102,102,.1); }
.gem-feedback {
  text-align: center; font-size: 12.5px; font-weight: 700; margin-top: 12px;
}
.gem-feedback.ok { color: var(--success); }
.gem-feedback.no { color: var(--danger); }
</style>
