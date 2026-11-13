<template>
  <div class="m3-wrap">
    <!-- HUD：剩余步数 + 提示按钮 + 当前得分 -->
    <div class="m3-hud">
      <span class="m3-moves">剩余步数 <b>{{ moves }}</b></span>
      <button class="m3-auto-btn" :class="{ on: auto }" @click="toggleAuto">
        {{ auto ? '✨ 智能点消' : '✋ 手动交换' }}
      </button>
      <button class="m3-hint-btn" :disabled="busy || stage !== 'play'" @click="showHint">💡 提示</button>
      <span class="m3-total">得分 {{ score }}</span>
    </div>

    <!-- ===== 准备阶段 ===== -->
    <div v-if="stage === 'ready'" class="m3-stage m3-ready">
      <div class="m3-mascot">🍬</div>
      <h3>糖豆消消乐</h3>
      <p>泡芙的糖果罐打翻啦！把 <b>3 颗以上同色糖豆</b> 连成一线就能消除，<br>步数内尽量刷高分，连锁消除还有翻倍奖励 ✨</p>
      <ul class="m3-tip-list">
        <li>👆 <b>点一下任意糖豆</b>，泡芙会自动帮你换到能消的位置 —— <b>点哪颗都能消</b>！</li>
        <li>✋ 想自己找？点上面的「✨ 智能点消」切到手动，先点一颗再点相邻那颗交换</li>
        <li>💡 点「提示」会<b>发光标出一对</b>；全场没组合了会自动重排，不会卡死</li>
        <li>🔥 一次交换引发连锁，每多消一轮得分翻倍</li>
      </ul>
      <button class="m3-btn big" @click="start">开始消除！</button>
    </div>

    <!-- ===== 棋盘 ===== -->
    <div v-else-if="stage === 'play'" class="m3-stage">
      <div class="m3-board" :style="{ '--cols': C }" :class="{ busy: busy }">
        <button
          v-for="(cell, i) in cells"
          :key="i"
          class="m3-cell"
          :class="{
            sel: i === sel,
            breaking: cell.breaking,
            hint: hintCells.includes(i),
            autoPick: swapCells.includes(i),
            shake: shakeCells.includes(i)
          }"
          :style="{ background: cell.type == null ? 'transparent' : TYPES[cell.type].bg }"
          @click="tap(i)"
        ><span v-if="cell.type != null">{{ TYPES[cell.type].e }}</span></button>
      </div>
      <p class="m3-hint">{{ busy ? '消除中…' : (auto ? '点任意一颗糖豆，泡芙自动帮你消 ✨' : (sel == null ? '点一颗糖豆开始' : '再点相邻的那颗交换')) }}</p>
      <p v-if="notice" class="m3-notice">{{ notice }}</p>
      <button class="m3-btn ghost" @click="restart">重开一局</button>
    </div>

    <!-- ===== 结算 ===== -->
    <div v-else-if="stage === 'done'" class="m3-stage m3-done">
      <div class="m3-mascot">{{ score >= 600 ? '🏆' : score >= 350 ? '🥈' : '🍬' }}</div>
      <h3>{{ score >= 600 ? '糖果大师！泡芙都想拜你为师' : score >= 350 ? '手感不错，糖豆都怕你' : '甜甜的一局，再来刷高点！' }}</h3>
      <div class="m3-final">本局得分 <b>{{ score }}</b></div>
      <div class="m3-final sub">用了 {{ usedMoves }} 步 · 连锁最高 x{{ maxCascade }} · 提示 {{ hintsUsed }} 次</div>
      <button class="m3-btn big" @click="restart">再来一局！</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'

export default {
  name: 'GameMatch3',
  emits: ['score', 'over'],
  data() {
    return {
      R: 8, C: 8, KIND: 6,
      TYPES: [
        { e: '🍬', bg: 'linear-gradient(135deg,#ffd1e3,#ff9ec4)' },
        { e: '🍭', bg: 'linear-gradient(135deg,#ffb3b3,#ff6b6b)' },
        { e: '🍫', bg: 'linear-gradient(135deg,#d9a066,#a9743b)' },
        { e: '🍩', bg: 'linear-gradient(135deg,#e3c6ff,#b079ff)' },
        { e: '🧁', bg: 'linear-gradient(135deg,#bfe3ff,#6ec6e6)' },
        { e: '🍓', bg: 'linear-gradient(135deg,#c6f0c2,#7fd4a8)' }
      ],
      stage: 'ready',
      cells: [],          // 长度 R*C，元素 { type:0..5|null, breaking:bool }
      sel: null,
      moves: 20,
      usedMoves: 0,
      score: 0,
      maxCascade: 1,
      busy: false,
      // ↓ 本次新增：提示 / 无效交换反馈 / 死局洗牌
      hintCells: [],
      swapCells: [],
      auto: true,
      queuedTap: null,
      shakeCells: [],
      hintsUsed: 0,
      notice: '',
      noticeTimer: null,
      idleTimer: null,
      alive: true
    }
  },
  created() {
    // 默认「点一下就消」；想自己交换的玩家可切手动，选择记在本地
    try { this.auto = localStorage.getItem('mlp_m3_auto') !== '0' } catch (e) { this.auto = true }
  },
  beforeUnmount() {
    this.alive = false
    clearTimeout(this.noticeTimer)
    clearTimeout(this.idleTimer)
  },
  methods: {
    idx(r, c) { return r * this.C + c },
    cellAt(r, c) { return this.cells[this.idx(r, c)] },
    randType() { return Math.floor(Math.random() * this.KIND) },
    sleep(ms) { return new Promise(res => setTimeout(res, ms)) },

    start() {
      this.stage = 'play'
      sound.click()
      this.score = 0
      this.moves = 20
      this.usedMoves = 0
      this.maxCascade = 1
      this.hintsUsed = 0
      this.sel = null
      this.busy = false
      this.hintCells = []
      this.swapCells = []
      this.queuedTap = null
      this.shakeCells = []
      this.notice = ''
      this.genBoard()
      this.resetIdle()
      this.$emit('score', 0)
    },
    restart() {
      this.stage = 'ready'
      this.notice = ''
      this.hintCells = []
      this.swapCells = []
      this.queuedTap = null
      this.shakeCells = []
      clearTimeout(this.noticeTimer)
      clearTimeout(this.idleTimer)
      this.$emit('score', 0)
    },

    /* 生成无初始三连、且「保证有可行交换」的棋盘 */
    genBoard() {
      for (let attempt = 0; attempt < 40; attempt++) {
        const arr = new Array(this.R * this.C)
        for (let r = 0; r < this.R; r++) {
          for (let c = 0; c < this.C; c++) {
            let t
            do {
              t = this.randType()
            } while (
              (c >= 2 && arr[this.idx(r, c - 1)].type === t && arr[this.idx(r, c - 2)].type === t) ||
              (r >= 2 && arr[this.idx(r - 1, c)].type === t && arr[this.idx(r - 2, c)].type === t)
            )
            arr[this.idx(r, c)] = { type: t, breaking: false }
          }
        }
        this.cells = arr
        if (this.findHint()) return
      }
      // 极端兜底：直接重排现有糖豆，直到有解
      this.shuffleBoard()
    },

    /* 找出所有处于 3+ 连线上的格子下标 */
    findMatches() {
      const set = new Set()
      const { R, C } = this
      for (let r = 0; r < R; r++) {
        let run = 1
        for (let c = 1; c <= C; c++) {
          const a = c < C ? this.cellAt(r, c).type : null
          const b = this.cellAt(r, c - 1).type
          if (a != null && a === b) run++
          else {
            if (run >= 3) for (let k = c - run; k < c; k++) set.add(this.idx(r, k))
            run = 1
          }
        }
      }
      for (let c = 0; c < C; c++) {
        let run = 1
        for (let r = 1; r <= R; r++) {
          const a = r < R ? this.cellAt(r, c).type : null
          const b = this.cellAt(r - 1, c).type
          if (a != null && a === b) run++
          else {
            if (run >= 3) for (let k = r - run; k < r; k++) set.add(this.idx(k, c))
            run = 1
          }
        }
      }
      return [...set]
    },

    /**
     * 找一组「交换后能形成 3 连」的相邻格子，返回 [a, b] 或 null。
     * 只在类型数组的副本上试，不动真实棋盘。
     */
    findHint() {
      const { R, C } = this
      const types = this.cells.map(c => c.type)
      const at = (r, c) => types[r * C + c]
      const makes3 = (r, c) => {
        const t = at(r, c)
        if (t == null) return false
        let n = 1
        for (let k = c - 1; k >= 0 && at(r, k) === t; k--) n++
        for (let k = c + 1; k < C && at(r, k) === t; k++) n++
        if (n >= 3) return true
        n = 1
        for (let k = r - 1; k >= 0 && at(k, c) === t; k--) n++
        for (let k = r + 1; k < R && at(k, c) === t; k++) n++
        return n >= 3
      }
      const trySwap = (a, b, r1, c1, r2, c2) => {
        const t = types[a]; types[a] = types[b]; types[b] = t
        const ok = makes3(r1, c1) || makes3(r2, c2)
        const t2 = types[a]; types[a] = types[b]; types[b] = t2
        return ok
      }
      for (let r = 0; r < R; r++) {
        for (let c = 0; c < C; c++) {
          if (c + 1 < C) {
            const a = this.idx(r, c), b = this.idx(r, c + 1)
            if (trySwap(a, b, r, c, r, c + 1)) return [a, b]
          }
          if (r + 1 < R) {
            const a = this.idx(r, c), b = this.idx(r + 1, c)
            if (trySwap(a, b, r, c, r + 1, c)) return [a, b]
          }
        }
      }
      return null
    },

    /** 洗牌：随机重排现有糖豆，直到「无自动三连」且「有可行交换」 */
    shuffleBoard() {
      const types = this.cells.map(c => c.type)
      for (let attempt = 0; attempt < 80; attempt++) {
        for (let i = types.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          const t = types[i]; types[i] = types[j]; types[j] = t
        }
        this.cells = types.map(t => ({ type: t, breaking: false }))
        if (this.findMatches().length === 0 && this.findHint()) return
      }
      // 实在洗不出来就整盘重生成
      this.cells = types.map(t => ({ type: t, breaking: false }))
    },

    /** 每轮连锁结束后兜底：全场没组合就自动洗牌，不让玩家卡死 */
    async ensurePlayable() {
      if (this.findHint()) return
      this.notice = '🌀 没有能消的组合啦，糖豆自动重排一下～'
      sound.magic()
      await this.sleep(420)
      this.shuffleBoard()
      this.cells = this.cells.slice()
    },

    /** 卡住 6 秒自动给个提示 */
    resetIdle() {
      clearTimeout(this.idleTimer)
      if (this.stage !== 'play' || this.busy) return
      this.idleTimer = setTimeout(() => {
        if (!this.alive || this.stage !== 'play' || this.busy || this.sel != null) return
        const h = this.findHint()
        if (h) {
          this.hintCells = h
          this.notice = this.auto ? '💡 点任意一颗糖豆，泡芙就会帮你消～' : '💡 卡住了？发光的这两颗换一下就能消～'
        }
      }, 6000)
    },

    showHint() {
      if (this.busy || this.stage !== 'play') return
      const h = this.findHint()
      if (!h) { this.ensurePlayable(); return }
      this.hintCells = h
      this.hintsUsed += 1
      this.notice = this.auto
        ? '✨ 智能点消已开，直接点任意一颗就能消～'
        : '✨ 发光的这两颗换一下就能消啦（不扣步数）'
      sound.twinkle()
      clearTimeout(this.noticeTimer)
      this.noticeTimer = setTimeout(() => {
        this.hintCells = []
        this.notice = ''
        this.resetIdle()
      }, 3200)
      clearTimeout(this.idleTimer)
    },

    clearFeedback() {
      this.hintCells = []
      this.swapCells = []
      this.shakeCells = []
      this.notice = ''
      clearTimeout(this.noticeTimer)
      clearTimeout(this.idleTimer)
    },

    /* 下落 + 顶部补糖 */
    gravityRefill() {
      const { R, C } = this
      const cells = this.cells
      for (let c = 0; c < C; c++) {
        const col = []
        for (let r = R - 1; r >= 0; r--) {
          const cell = cells[this.idx(r, c)]
          if (cell.type != null) col.push(cell)
        }
        for (let r = R - 1; r >= 0; r--) {
          cells[this.idx(r, c)] = col.length
            ? col.shift()
            : { type: this.randType(), breaking: false }
        }
      }
      this.cells = cells.slice()
    },

    swapData(a, b) {
      const t = this.cells[a]
      this.cells[a] = this.cells[b]
      this.cells[b] = t
      this.cells = this.cells.slice()
    },

    tap(i) {
      if (this.stage !== 'play' || !this.alive) return
      // 消除动画期间的点击不丢：记下最后一次，动画结束自动补消
      if (this.busy) { this.queuedTap = i; return }
      clearTimeout(this.idleTimer)
      if (!this.auto) return this.manualTap(i)
      return this.autoPlay(i)
    },

    /* 一次操作结束：解锁 + 结算检查 + 补上动画期间排队的点击 */
    afterAction() {
      this.busy = false
      if (this.moves <= 0 && this.alive) { this.finish(); return }
      this.resetIdle()
      if (this.queuedTap != null) {
        const q = this.queuedTap
        this.queuedTap = null
        this.$nextTick(() => this.tap(q))
      }
    },

    toggleAuto() {
      this.auto = !this.auto
      try { localStorage.setItem('mlp_m3_auto', this.auto ? '1' : '0') } catch (e) { /* 隐私模式忽略 */ }
      this.sel = null
      this.queuedTap = null
      this.clearFeedback()
      this.notice = this.auto ? '✨ 智能点消已开：点任意一颗就能消～' : '✋ 手动交换：先点一颗，再点上下左右相邻的那颗'
      sound.click()
      clearTimeout(this.noticeTimer)
      this.noticeTimer = setTimeout(() => { this.notice = '' }, 2600)
      this.resetIdle()
    },

    /* 纯函数：给一份类型数组，算出 3+ 连的格子数（不碰真实棋盘） */
    countMatches(types) {
      const { R, C } = this
      const set = new Set()
      for (let r = 0; r < R; r++) {
        let run = 1
        for (let c = 1; c <= C; c++) {
          const a = c < C ? types[r * C + c] : null
          const b = types[r * C + c - 1]
          if (a != null && a === b) run++
          else { if (run >= 3) for (let k = c - run; k < c; k++) set.add(r * C + k); run = 1 }
        }
      }
      for (let c = 0; c < C; c++) {
        let run = 1
        for (let r = 1; r <= R; r++) {
          const a = r < R ? types[r * C + c] : null
          const b = types[(r - 1) * C + c]
          if (a != null && a === b) run++
          else { if (run >= 3) for (let k = r - run; k < r; k++) set.add(k * C + c); run = 1 }
        }
      }
      return set.size
    },

    /* 找最优交换：优先「包含玩家点的那颗」，否则全场能消最多的一组 */
    bestSwap(prefer) {
      const { R, C } = this
      const types = this.cells.map(c => c.type)
      let best = null, bestN = 0, fallback = null, fallbackN = 0
      const test = (a, b) => {
        const t = types[a]; types[a] = types[b]; types[b] = t
        const n = this.countMatches(types)
        const t2 = types[a]; types[a] = types[b]; types[b] = t2
        return n
      }
      for (let r = 0; r < R; r++) {
        for (let c = 0; c < C; c++) {
          const pairs = []
          if (c + 1 < C) pairs.push([this.idx(r, c), this.idx(r, c + 1)])
          if (r + 1 < R) pairs.push([this.idx(r, c), this.idx(r + 1, c)])
          for (const [a, b] of pairs) {
            const n = test(a, b)
            if (n <= 0) continue
            if (n > fallbackN) { fallback = [a, b]; fallbackN = n }
            if ((a === prefer || b === prefer) && n > bestN) { best = [a, b]; bestN = n }
          }
        }
      }
      return best || fallback
    },

    /* 智能点消：点任意一颗 → 自动换到能消的位置 → 消除连锁 */
    async autoPlay(i) {
      this.busy = true
      this.clearFeedback()
      let pair = this.bestSwap(i)
      if (!pair) {
        this.notice = '🌀 没有能消的组合啦，糖豆自动重排一下～'
        sound.magic()
        await this.sleep(420)
        this.shuffleBoard()
        pair = this.bestSwap(i)
      }
      if (!pair) { this.afterAction(); return }

      const [a, b] = pair
      const elsewhere = a !== i && b !== i
      this.swapCells = [a, b]
      this.swapData(a, b)
      this.notice = elsewhere ? '✨ 这颗暂时换不出，泡芙帮你换了另一组～' : '✨ 泡芙帮你换好啦，消！'
      await this.sleep(200)
      this.swapCells = []
      this.moves -= 1
      this.usedMoves += 1
      await this.cascade()
      this.afterAction()
    },

    /* 手动模式：先点一颗，再点相邻那颗交换（保留原玩法） */
    async manualTap(i) {
      if (this.sel == null) { this.sel = i; sound.hover(); return }
      if (this.sel === i) { this.sel = null; this.resetIdle(); return }
      const r1 = Math.floor(this.sel / this.C), c1 = this.sel % this.C
      const r2 = Math.floor(i / this.C), c2 = i % this.C
      const adjacent = Math.abs(r1 - r2) + Math.abs(c1 - c2) === 1
      if (!adjacent) { this.sel = i; sound.hover(); return }

      this.busy = true
      const a = this.sel, b = i
      this.sel = null
      this.swapData(a, b)
      if (this.findMatches().length === 0) {
        await this.sleep(160)
        this.swapData(a, b)          // 换不动，换回来，不扣步数
        this.clearFeedback()
        this.shakeCells = [a, b]
        this.notice = '🤔 这两颗换不出 3 连，不扣步数～点「💡 提示」看看'
        sound.fail()
        this.noticeTimer = setTimeout(() => {
          this.shakeCells = []
          this.notice = ''
          this.resetIdle()
        }, 2000)
        this.afterAction()
        return
      }
      this.clearFeedback()
      this.moves -= 1
      this.usedMoves += 1
      await this.cascade()
      this.afterAction()
    },

    /* 连锁消除，每多一轮得分翻倍 */
    async cascade() {
      let chain = 0
      while (this.alive) {
        const matches = this.findMatches()
        if (matches.length === 0) break
        chain += 1
        this.maxCascade = Math.max(this.maxCascade, chain)
        this.score += matches.length * 10 * chain
        this.$emit('score', this.score)
        if (chain >= 2) sound.magic(); else sound.pop()
        for (const m of matches) this.cells[m].breaking = true
        this.cells = this.cells.slice()
        await this.sleep(220)
        for (const m of matches) this.cells[m].type = null
        this.cells = this.cells.slice()
        this.gravityRefill()
        await this.sleep(120)
      }
      await this.ensurePlayable()
    },

    finish() {
      this.stage = 'done'
      clearTimeout(this.idleTimer)
      this.$emit('score', this.score)
      this.$emit('over', this.score)
      if (this.score >= 350) sound.success(); else sound.fail()
    }
  }
}
</script>

<style scoped>
.m3-wrap { display: flex; flex-direction: column; gap: 14px; }
.m3-hud {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  padding: 10px 14px; border-radius: 12px;
  background: var(--panel); font-size: 12.5px;
}
.m3-moves { color: var(--muted); }
.m3-moves b { color: var(--danger); font-size: 15px; }
.m3-total { flex: 1; text-align: right; font-weight: 800; color: var(--primary); }
.m3-hint-btn {
  border: 1px solid var(--border); border-radius: 999px; cursor: pointer;
  background: var(--card); color: var(--primary); font-weight: 800; font-size: 11.5px;
  padding: 5px 12px; transition: filter .15s, transform .12s;
}
.m3-hint-btn:hover:not(:disabled) { filter: brightness(1.05); transform: translateY(-1px); }
.m3-hint-btn:disabled { opacity: .5; cursor: default; }
.m3-auto-btn {
  border: 1px solid var(--border); border-radius: 999px; cursor: pointer;
  background: var(--card); color: var(--muted); font-weight: 800; font-size: 11.5px;
  padding: 5px 12px; transition: filter .15s, transform .12s, box-shadow .15s;
}
.m3-auto-btn.on {
  color: #fff; border-color: transparent;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 10px rgba(160, 108, 213, .25);
}
.m3-auto-btn:hover { filter: brightness(1.06); transform: translateY(-1px); }

.m3-stage {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  padding: 22px 16px; border-radius: 16px;
  background: var(--panel); text-align: center; min-height: 320px;
  justify-content: center;
}
.m3-mascot { font-size: 52px; }
.m3-stage h3 { margin: 0; font-size: 18px; }
.m3-stage p { margin: 0; font-size: 12.5px; color: var(--muted); line-height: 1.7; }

.m3-tip-list {
  margin: 4px 0 8px; padding: 0; list-style: none;
  display: flex; flex-direction: column; gap: 8px; text-align: left;
  font-size: 12.5px; color: var(--text);
}
.m3-tip-list li {
  padding: 9px 12px; border-radius: 10px;
  background: var(--card); border: 1px solid var(--border);
}

.m3-board {
  display: grid;
  grid-template-columns: repeat(var(--cols), 1fr);
  gap: 6px;
  width: min(320px, 86vw);
  padding: 10px;
  border-radius: 16px;
  background: var(--track);
  border: 1px solid var(--border);
}
.m3-cell {
  aspect-ratio: 1 / 1;
  border: none; border-radius: 12px;
  font-size: clamp(18px, 6vw, 26px);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: transform .12s ease, box-shadow .12s ease, opacity .18s ease;
  box-shadow: inset 0 -3px 0 rgba(0,0,0,.12);
}
.m3-cell:hover { transform: translateY(-1px); }
.m3-cell.sel {
  box-shadow: 0 0 0 3px #fff, 0 0 0 6px var(--primary);
  transform: scale(1.06);
}
.m3-cell.hint {
  z-index: 2;
  box-shadow: 0 0 0 3px #fff, 0 0 0 6px var(--yellow), 0 0 14px rgba(247,224,138,.9);
  animation: m3Hint .75s ease-in-out infinite alternate;
}
.m3-cell.autoPick {
  z-index: 3;
  box-shadow: 0 0 0 3px #fff, 0 0 0 6px #7fd4a8, 0 0 16px rgba(126,212,168,.95);
  animation: m3Hint .5s ease-in-out infinite alternate;
}
.m3-cell.shake { animation: m3Shake .34s ease; }
.m3-cell.breaking {
  transform: scale(.2) rotate(20deg);
  opacity: 0;
}
.m3-board.busy .m3-cell { cursor: default; }

.m3-hint { margin: 0; font-size: 12px; color: var(--muted); min-height: 16px; }
.m3-notice {
  margin: -6px 0 0; font-size: 12px; font-weight: 700; color: var(--primary);
  background: var(--card); border: 1px solid var(--border);
  border-radius: 999px; padding: 6px 14px; text-align: center;
}
.m3-final { font-size: 13px; color: var(--muted); }
.m3-final b { font-size: 26px; color: var(--primary); }
.m3-final.sub { font-size: 11.5px; }

.m3-btn {
  border: none; cursor: pointer; font-weight: 800;
  padding: 10px 22px; border-radius: 999px; font-size: 13.5px;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 6px 14px rgba(160, 108, 213, .3);
  transition: transform .12s ease, filter .12s ease;
}
.m3-btn:hover { filter: brightness(1.07); transform: translateY(-1px); }
.m3-btn:active { transform: scale(.96); }
.m3-btn.big { padding: 13px 34px; font-size: 15px; }
.m3-btn.ghost {
  background: var(--card); color: var(--text);
  box-shadow: none; border: 1px solid var(--border);
  padding: 8px 18px; font-size: 12px;
}

@keyframes m3Hint { from { transform: scale(1); } to { transform: scale(1.13); filter: brightness(1.16); } }
@keyframes m3Shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}
@media (prefers-reduced-motion: reduce) {
  .m3-cell.hint, .m3-cell.autoPick, .m3-cell.shake { animation: none; }
  .m3-cell.hint, .m3-cell.autoPick { transform: scale(1.08); }
}
</style>
