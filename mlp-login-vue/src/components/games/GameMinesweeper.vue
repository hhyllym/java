<template>
  <div class="ms-wrap">
    <!-- 准备页：选难度 -->
    <div v-if="stage === 'ready'" class="ms-ready">
      <div class="ms-mascot">💣</div>
      <h3>星糖扫雷</h3>
      <p class="ms-sub">
        点开格子找安全区，数字是周围 8 格藏了几颗雷～<br>
        首点保证不踩雷，翻开所有安全格就赢！
      </p>
      <div class="ms-diffs">
        <button
          v-for="d in DIFF_LIST"
          :key="d.key"
          class="ms-diff"
          :class="{ on: diff === d.key }"
          @click="pickDiff(d.key)"
        >
          <span class="d-emoji">{{ d.emoji }}</span>
          <b>{{ d.name }}</b>
          <small>{{ d.desc }}</small>
        </button>
      </div>
      <div class="ms-how">
        <span>👆 点格子翻开</span>
        <span>🚩 右键 / 长按 / 插旗模式标雷</span>
        <span>✨ 空白自动展开</span>
      </div>
      <button class="ms-btn" @click="start">开始扫雷 ✨</button>
    </div>

    <!-- 游戏中 / 结算 -->
    <template v-else>
      <div class="ms-hud">
        <div class="hud-chip"><span>🚩 剩余</span><b>{{ leftMines }}</b></div>
        <div class="hud-chip"><span>⏱️ 用时</span><b>{{ seconds }}s</b></div>
        <div class="hud-chip"><span>{{ cfg.emoji }}</span><b>{{ cfg.name }}</b></div>
        <button class="ms-face" title="重新开局" @click="restart">
          {{ stage === 'won' ? '😎' : stage === 'lost' ? '😵' : '🙂' }}
        </button>
      </div>

      <div class="ms-toolbar">
        <button class="ms-tool" :class="{ on: flagMode }" @click="toggleFlagMode">
          {{ flagMode ? '🚩 插旗中' : '🚩 插旗模式' }}
        </button>
        <span class="ms-hint">{{ flagMode ? '现在点格子 = 插旗，再点一次关闭' : '手机上不方便右键？打开插旗模式' }}</span>
      </div>

      <div class="ms-board" :style="{ gridTemplateColumns: 'repeat(' + cfg.cols + ', 1fr)' }">
        <button
          v-for="(c, i) in cells"
          :key="i"
          class="ms-cell"
          :class="[
            { open: c.open, flag: c.flag && !c.open, boom: i === boom, mine: c.open && c.mine },
            c.open && !c.mine && c.adj > 0 ? 'n' + c.adj : ''
          ]"
          @click="onTap(i)"
          @contextmenu.prevent="toggleFlagAt(i)"
          @touchstart.passive="onTouchStart(i)"
          @touchend.passive="onTouchEnd"
          @touchmove.passive="onTouchEnd"
        >
          <template v-if="c.flag && !c.open">🚩</template>
          <template v-else-if="c.open && c.mine">💣</template>
          <template v-else-if="c.open && c.adj > 0">{{ c.adj }}</template>
        </button>
      </div>

      <p class="ms-foot">🌈 首点安全 · 已插旗 {{ flags }}/{{ cfg.mines }} · {{ cfg.rows }}×{{ cfg.cols }}</p>

      <div v-if="stage === 'won' || stage === 'lost'" class="ms-result" :class="stage">
        <div class="r-emoji">{{ stage === 'won' ? '🎉' : '💥' }}</div>
        <h3>{{ stage === 'won' ? '全部扫清啦！' : '踩到雷啦～' }}</h3>
        <p>
          {{ stage === 'won'
            ? `用了 ${seconds} 秒 · ${cfg.name}难度 · 得分 ×${cfg.mul}`
            : `已翻开 ${openedSafe} 个安全格，下次小心～` }}
        </p>
        <div class="r-score">+{{ score }} 分</div>
        <div class="r-btns">
          <button class="ms-btn" @click="restart">🔁 再来一局</button>
          <button class="ms-ghost" @click="backToReady">换个难度</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import sound from '../../sound'

/** 三档难度：格子数、雷数、基础分、得分倍率 */
const DIFFS = {
  easy:   { key: 'easy',   name: '轻松', emoji: '🌱', rows: 8,  cols: 8,  mines: 8,  base: 260, mul: 1,   desc: '8×8 · 8 颗雷' },
  normal: { key: 'normal', name: '普通', emoji: '🌈', rows: 9,  cols: 9,  mines: 12, base: 320, mul: 1.3, desc: '9×9 · 12 颗雷 · ×1.3' },
  hard:   { key: 'hard',   name: '挑战', emoji: '🔥', rows: 12, cols: 12, mines: 26, base: 420, mul: 1.6, desc: '12×12 · 26 颗雷 · ×1.6' }
}
const DIFF_LIST = [DIFFS.easy, DIFFS.normal, DIFFS.hard]
const DIFF_KEY = 'mlp_mine_diff'

function readDiff() {
  try {
    const k = localStorage.getItem(DIFF_KEY)
    return DIFFS[k] ? k : 'normal'
  } catch (e) { return 'normal' }
}

export default {
  name: 'GameMinesweeper',
  emits: ['score', 'over'],
  data() {
    return {
      DIFF_LIST,
      diff: readDiff(),
      stage: 'ready',          // ready | playing | won | lost
      cells: [],
      started: false,          // 是否已布雷（首次点击后才布）
      flags: 0,
      seconds: 0,
      score: 0,
      boom: -1,
      flagMode: false,
      timer: null,
      longTimer: null,
      longFired: false
    }
  },
  computed: {
    cfg() { return DIFFS[this.diff] },
    leftMines() { return this.cfg.mines - this.flags },
    openedSafe() { return this.cells.filter(c => c.open && !c.mine).length }
  },
  beforeUnmount() {
    this.stopTimer()
    if (this.longTimer) clearTimeout(this.longTimer)
  },
  methods: {
    pickDiff(k) {
      if (!DIFFS[k] || this.stage === 'playing') return
      this.diff = k
      try { localStorage.setItem(DIFF_KEY, k) } catch (e) {}
      sound.click()
    },
    start() {
      this.build()
      this.stage = 'playing'
      sound.magic()
    },
    restart() {
      this.build()
      this.stage = 'playing'
      sound.click()
    },
    backToReady() {
      this.stopTimer()
      this.stage = 'ready'
      sound.click()
    },
    /** 重置棋盘（雷等首次点击再布，保证首点安全） */
    build() {
      const n = this.cfg.rows * this.cfg.cols
      this.cells = Array.from({ length: n }, () => ({ mine: false, adj: 0, open: false, flag: false }))
      this.started = false
      this.flags = 0
      this.seconds = 0
      this.score = 0
      this.boom = -1
      this.flagMode = false
      this.longFired = false
      this.stopTimer()
    },
    neighbors(i) {
      const { rows, cols } = this.cfg
      const r = Math.floor(i / cols), c = i % cols
      const out = []
      for (let dr = -1; dr <= 1; dr++) {
        for (let dc = -1; dc <= 1; dc++) {
          if (!dr && !dc) continue
          const nr = r + dr, nc = c + dc
          if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) out.push(nr * cols + nc)
        }
      }
      return out
    },
    /** 首次点击后布雷：避开点击格及其周围 8 格 */
    placeMines(safeIdx) {
      const { rows, cols, mines } = this.cfg
      const forbidden = new Set([safeIdx, ...this.neighbors(safeIdx)])
      const pool = []
      for (let i = 0; i < rows * cols; i++) if (!forbidden.has(i)) pool.push(i)
      for (let i = pool.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[pool[i], pool[j]] = [pool[j], pool[i]]
      }
      const mineSet = new Set(pool.slice(0, mines))
      this.cells.forEach((c, i) => { c.mine = mineSet.has(i) })
      this.cells.forEach((c, i) => {
        if (!c.mine) c.adj = this.neighbors(i).filter(n => this.cells[n].mine).length
      })
    },
    reveal(i) {
      if (this.stage !== 'playing') return
      const c = this.cells[i]
      if (c.open || c.flag) return
      if (!this.started) {
        this.placeMines(i)
        this.started = true
        this.startTimer()
      }
      if (c.mine) {
        c.open = true
        this.boom = i
        this.lose()
        return
      }
      // 空白（adj=0）自动向外展开
      const stack = [i], seen = new Set()
      while (stack.length) {
        const k = stack.pop()
        if (seen.has(k)) continue
        seen.add(k)
        const cell = this.cells[k]
        if (cell.open || cell.flag || cell.mine) continue
        cell.open = true
        if (cell.adj === 0) {
          for (const n of this.neighbors(k)) {
            if (!this.cells[n].open && !this.cells[n].flag && !this.cells[n].mine) stack.push(n)
          }
        }
      }
      sound.pop()
      this.checkWin()
    },
    checkWin() {
      const safeTotal = this.cells.length - this.cfg.mines
      if (this.openedSafe >= safeTotal) this.win()
    },
    win() {
      this.stage = 'won'
      this.stopTimer()
      // 把剩下的雷自动插旗，看着更爽
      this.cells.forEach(c => { if (c.mine && !c.flag) c.flag = true })
      this.flags = this.cfg.mines
      const wrong = this.cells.filter(c => c.flag && !c.mine).length
      this.score = Math.max(20, Math.round((this.cfg.base - this.seconds * 2 - wrong * 8) * this.cfg.mul))
      this.$emit('score', this.score)
      this.$emit('over', this.score)
      sound.success()
    },
    lose() {
      this.stage = 'lost'
      this.stopTimer()
      this.cells.forEach(c => { if (c.mine) c.open = true })
      // 踩雷也按已翻开的安全格给点安慰分
      this.score = Math.max(0, Math.round(this.openedSafe * 2 * this.cfg.mul))
      this.$emit('score', this.score)
      this.$emit('over', this.score)
      sound.fail()
    },
    toggleFlagAt(i) {
      if (this.stage !== 'playing') return
      const c = this.cells[i]
      if (c.open) return
      if (!c.flag && this.flags >= this.cfg.mines) return
      c.flag = !c.flag
      this.flags += c.flag ? 1 : -1
      sound.click()
    },
    toggleFlagMode() {
      if (this.stage !== 'playing') return
      this.flagMode = !this.flagMode
      sound.click()
    },
    onTap(i) {
      if (this.longFired) { this.longFired = false; return }
      if (this.flagMode) this.toggleFlagAt(i)
      else this.reveal(i)
    },
    // 移动端长按 = 插旗
    onTouchStart(i) {
      if (this.longTimer) clearTimeout(this.longTimer)
      this.longFired = false
      this.longTimer = setTimeout(() => {
        this.longFired = true
        this.toggleFlagAt(i)
        if (navigator.vibrate) navigator.vibrate(30)
      }, 380)
    },
    onTouchEnd() {
      if (this.longTimer) { clearTimeout(this.longTimer); this.longTimer = null }
    },
    startTimer() {
      this.stopTimer()
      this.timer = setInterval(() => { this.seconds++ }, 1000)
    },
    stopTimer() {
      if (this.timer) { clearInterval(this.timer); this.timer = null }
    }
  }
}
</script>

<style scoped>
.ms-wrap { padding: 14px 12px 20px; text-align: center; color: var(--text); }

/* 准备页 */
.ms-ready { max-width: 460px; margin: 0 auto; }
.ms-mascot { font-size: 52px; line-height: 1; margin-bottom: 6px; filter: drop-shadow(0 4px 8px rgba(160,108,213,.22)); }
.ms-ready h3 { margin: 4px 0 8px; font-size: 22px; color: var(--primary); }
.ms-sub { margin: 0 0 18px; font-size: 13px; line-height: 1.8; color: var(--muted); }
.ms-diffs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.ms-diff {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  padding: 14px 6px; border: 2px solid var(--border); border-radius: 16px;
  background: var(--card); cursor: pointer; transition: transform .18s, border-color .18s, box-shadow .18s;
  font-family: inherit; color: var(--text);
}
.ms-diff:hover { transform: translateY(-3px); }
.ms-diff.on { border-color: var(--primary); box-shadow: 0 6px 18px color-mix(in srgb, var(--primary) 22%, transparent); background: var(--panel); }
.d-emoji { font-size: 26px; }
.ms-diff b { font-size: 15px; }
.ms-diff small { font-size: 10px; color: var(--muted); line-height: 1.4; }
.ms-how { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px 14px; margin-bottom: 18px; font-size: 11px; color: var(--muted); }

/* HUD */
.ms-hud { display: flex; justify-content: center; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.hud-chip {
  display: flex; align-items: center; gap: 7px; padding: 6px 12px;
  border: 1px solid var(--border); border-radius: 999px; background: var(--card); font-size: 12px;
}
.hud-chip span { color: var(--muted); }
.hud-chip b { color: var(--primary); font-size: 15px; }
.ms-face {
  width: 36px; height: 36px; border-radius: 50%; border: 1px solid var(--border);
  background: var(--card); font-size: 18px; cursor: pointer; transition: transform .15s;
}
.ms-face:hover { transform: scale(1.12); }

.ms-toolbar { display: flex; align-items: center; justify-content: center; gap: 10px; flex-wrap: wrap; margin-bottom: 12px; }
.ms-tool {
  padding: 6px 14px; border: 1.5px solid var(--border); border-radius: 999px;
  background: var(--card); color: var(--text); font-size: 12px; font-family: inherit; cursor: pointer;
  transition: background .18s, border-color .18s;
}
.ms-tool.on { background: var(--yellow); border-color: var(--primary); font-weight: 700; }
.ms-hint { font-size: 11px; color: var(--muted); }

/* 棋盘 */
.ms-board {
  display: grid; gap: 4px; width: min(100%, 400px); margin: 0 auto;
  padding: 10px; border-radius: 18px; background: var(--panel);
  border: 1px solid var(--border); box-shadow: 0 8px 24px color-mix(in srgb, var(--primary) 10%, transparent);
  touch-action: manipulation;
}
.ms-cell {
  aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--border); border-radius: 9px; padding: 0;
  background: linear-gradient(145deg, #fff, var(--panel));
  box-shadow: 0 2px 0 color-mix(in srgb, var(--primary) 12%, transparent);
  font-family: inherit; font-weight: 800; font-size: clamp(11px, 2.8vw, 17px);
  color: var(--text); cursor: pointer; user-select: none; -webkit-user-select: none;
  transition: transform .12s, background .15s, box-shadow .15s;
}
.ms-cell:not(.open):hover { transform: translateY(-2px); background: linear-gradient(145deg, #fff, color-mix(in srgb, var(--pink) 30%, #fff)); }
.ms-cell.open {
  background: #fff; box-shadow: inset 0 1px 4px rgba(120,100,150,.12); cursor: default;
}
.ms-cell.flag { background: color-mix(in srgb, var(--yellow) 42%, #fff); font-size: clamp(10px, 2.4vw, 15px); }
.ms-cell.mine { background: color-mix(in srgb, var(--pink) 30%, #fff); }
.ms-cell.boom { background: #ffd9e4; box-shadow: 0 0 0 2px #f07a9c inset; animation: ms-shake .35s; }
.ms-cell.n1 { color: #5b8def; }
.ms-cell.n2 { color: #45ab7f; }
.ms-cell.n3 { color: #e0668a; }
.ms-cell.n4 { color: #9b7ede; }
.ms-cell.n5 { color: #dd8a3c; }
.ms-cell.n6 { color: #2fa8a0; }
.ms-cell.n7 { color: #8a7f9c; }
.ms-cell.n8 { color: #4a3f5c; }
@keyframes ms-shake { 0%,100% { transform: translateX(0); } 25% { transform: translateX(-4px); } 75% { transform: translateX(4px); } }

.ms-foot { margin: 10px 0 0; font-size: 11px; color: var(--muted); }

/* 结算 */
.ms-result {
  max-width: 380px; margin: 16px auto 0; padding: 18px;
  border-radius: 18px; border: 1px solid var(--border); background: var(--card);
  box-shadow: 0 8px 22px color-mix(in srgb, var(--primary) 12%, transparent);
}
.ms-result.lost { background: color-mix(in srgb, var(--pink) 12%, var(--card)); }
.r-emoji { font-size: 40px; }
.ms-result h3 { margin: 6px 0; font-size: 19px; color: var(--primary); }
.ms-result p { margin: 0 0 8px; font-size: 12px; color: var(--muted); }
.r-score { font-size: 26px; font-weight: 800; color: var(--primary); margin-bottom: 12px; }
.r-btns { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.ms-btn {
  padding: 10px 24px; border: none; border-radius: 999px; cursor: pointer;
  background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff;
  font-size: 14px; font-weight: 700; font-family: inherit;
  box-shadow: 0 5px 14px color-mix(in srgb, var(--primary) 30%, transparent);
  transition: transform .16s;
}
.ms-btn:hover { transform: translateY(-2px); }
.ms-ghost {
  padding: 10px 20px; border: 1.5px solid var(--border); border-radius: 999px;
  background: var(--card); color: var(--text); font-size: 13px; font-family: inherit; cursor: pointer;
}

@media (prefers-reduced-motion: reduce) {
  .ms-cell, .ms-diff, .ms-btn, .ms-face { transition: none; }
  .ms-cell.boom { animation: none; }
}
</style>
