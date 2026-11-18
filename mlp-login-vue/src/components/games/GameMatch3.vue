<template>
  <div class="m3-wrap">
    <!-- HUD：步数 + 目标进度 + 提示 -->
    <div class="m3-hud">
      <span class="m3-moves">剩余步数 <b>{{ moves }}</b></span>
      <div class="m3-progress">
        <div class="m3-progress-top">
          <span>目标 {{ target }}</span>
          <span>{{ score }} / {{ target }}</span>
        </div>
        <div class="m3-bar"><div class="m3-bar-fill" :style="{ width: barWidth }"></div></div>
      </div>
      <button class="m3-hint-btn" :disabled="busy || stage !== 'play'" @click="showHint">💡 提示</button>
    </div>

    <!-- ===== 准备阶段 ===== -->
    <div v-if="stage === 'ready'" class="m3-stage m3-ready">
      <div class="m3-mascot">🍬</div>
      <h3>糖豆消消乐</h3>
      <p>泡芙的糖果罐打翻啦！<b>交换相邻两颗糖豆，连成 3 颗同色</b> 就能消除。<br>在 {{ movesMax }} 步内冲到 <b>{{ target }}</b> 分，还能拿三星 ✨</p>
      <ul class="m3-tip-list">
        <li>🔁 <b>交换相邻两颗</b>（点两下或拖动），连成 3 颗同色就消除；连不成会自动弹回，不扣步数</li>
        <li>🍬 <b>4 连</b> 生成条纹糖（消整行/整列），<b>5 连</b> 生成彩虹糖（交换后消全场同色）</li>
        <li>🔥 消除后糖豆下落，若又连成 3 颗会<b>连锁消除</b>，每多一轮得分翻倍</li>
        <li>💡 点「提示」会发光标出一步可消的交换；实在没组合会自动重排，不会卡死</li>
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
            shake: shakeCells.includes(i),
            special: !!cell.special,
            'sp-h': cell.special === 'h',
            'sp-v': cell.special === 'v',
            'sp-rainbow': cell.special === 'rainbow'
          }"
          :style="{ background: cell.type == null ? 'transparent' : TYPES[cell.type].bg }"
          @click="tap(i)"
          @pointerdown="onDragStart(i)"
          @pointerenter="onDragEnter(i)"
          @pointerup="onDragEnd"
          @pointercancel="onDragEnd"
        ><span class="m3-emoji">{{ cell.type != null ? TYPES[cell.type].e : '' }}</span><span v-if="cell.special" class="m3-spark">✨</span></button>
      </div>
      <p class="m3-hint">{{ busy ? '消除中…' : (notice || bottomHint) }}</p>
      <button class="m3-btn ghost" :disabled="busy" @click="restart">重开一局</button>
    </div>

    <!-- ===== 结算 ===== -->
    <div v-else-if="stage === 'done'" class="m3-stage m3-done">
      <div class="m3-mascot">{{ stars >= 3 ? '🏆' : stars >= 2 ? '🥈' : stars >= 1 ? '🥉' : '🍬' }}</div>
      <h3>{{ stars >= 3 ? '糖果大师！满分三星 🏆' : stars >= 2 ? '手感不错，两星到手！' : stars >= 1 ? '过关啦，再冲冲三星～' : '差一点点，再来一局！' }}</h3>
      <div class="m3-stars"><span v-for="n in 3" :key="n" :class="{ on: n <= stars }">★</span></div>
      <div class="m3-final">本局得分 <b>{{ score }}</b></div>
      <div class="m3-final sub">目标 {{ target }} · 用了 {{ usedMoves }} 步 · 连锁最高 x{{ maxCascade }} · 提示 {{ hintsUsed }} 次</div>
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
      cells: [],          // { type:0..5|null, special:null|'h'|'v'|'rainbow', breaking:bool }
      sel: null,
      movesMax: 25,
      moves: 25,
      target: 1000,
      usedMoves: 0,
      score: 0,
      maxCascade: 1,
      busy: false,
      hintCells: [],
      shakeCells: [],
      hintsUsed: 0,
      notice: '',
      noticeTimer: null,
      idleTimer: null,
      dragFrom: null,
      dragged: false,
      alive: true
    }
  },
  computed: {
    barWidth() {
      return Math.min(100, (this.score / this.target) * 100) + '%'
    },
    bottomHint() {
      return this.sel == null ? '点两颗相邻糖豆交换（也可以拖动），连成 3 颗同色就消除 ✨' : '再点相邻的那颗交换，或点自己取消'
    },
    stars() {
      if (this.score >= this.target * 1.6) return 3
      if (this.score >= this.target) return 2
      if (this.score >= this.target * 0.5) return 1
      return 0
    }
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
      this.moves = this.movesMax
      this.usedMoves = 0
      this.maxCascade = 1
      this.hintsUsed = 0
      this.sel = null
      this.busy = false
      this.hintCells = []
      this.shakeCells = []
      this.notice = ''
      this.dragFrom = null
      this.genBoard()
      this.resetIdle()
      this.$emit('score', 0)
    },
    restart() {
      this.stage = 'ready'
      this.notice = ''
      this.hintCells = []
      this.shakeCells = []
      this.sel = null
      this.dragFrom = null
      clearTimeout(this.noticeTimer)
      clearTimeout(this.idleTimer)
      this.$emit('score', 0)
    },

    /* 生成无初始三连、且保证存在可行交换的棋盘 */
    genBoard() {
      for (let attempt = 0; attempt < 60; attempt++) {
        const arr = new Array(this.R * this.C)
        for (let r = 0; r < this.R; r++) {
          for (let c = 0; c < this.C; c++) {
            let t, guard = 0
            do {
              t = this.randType(); guard++
            } while (
              guard < 30 && (
                (c >= 2 && arr[this.idx(r, c - 1)].type === t && arr[this.idx(r, c - 2)].type === t) ||
                (r >= 2 && arr[this.idx(r - 1, c)].type === t && arr[this.idx(r - 2, c)].type === t)
              )
            )
            arr[this.idx(r, c)] = { type: t, special: null, breaking: false }
          }
        }
        this.cells = arr
        if (this.findRuns().length === 0 && this.findSwapHint()) return
      }
      this.shuffleBoard()
    },

    /* 找出所有 3+ 连线，按段返回（含方向、长度，用于生成特殊糖） */
    findRuns() {
      const { R, C } = this
      const runs = []
      for (let r = 0; r < R; r++) {
        let c = 0
        while (c < C) {
          const t = this.cellAt(r, c)?.type
          if (t == null) { c++; continue }
          let e = c + 1
          while (e < C && this.cellAt(r, e)?.type === t) e++
          if (e - c >= 3) {
            const cells = []
            for (let k = c; k < e; k++) cells.push(this.idx(r, k))
            runs.push({ cells, dir: 'h', len: e - c, type: t })
          }
          c = e
        }
      }
      for (let c = 0; c < C; c++) {
        let r = 0
        while (r < R) {
          const t = this.cellAt(r, c)?.type
          if (t == null) { r++; continue }
          let e = r + 1
          while (e < R && this.cellAt(e, c)?.type === t) e++
          if (e - r >= 3) {
            const cells = []
            for (let k = r; k < e; k++) cells.push(this.idx(k, c))
            runs.push({ cells, dir: 'v', len: e - r, type: t })
          }
          r = e
        }
      }
      return runs
    },

    /* 找一个「交换后能形成三连」的相邻对（提示 / 死局检测） */
    findSwapHint(anchor) {
      const { R, C } = this
      const types = this.cells.map(c => c.type)
      const hasRun = () => {
        for (let r = 0; r < R; r++) for (let c = 0; c + 2 < C; c++) {
          const t = types[r * C + c]
          if (t != null && types[r * C + c + 1] === t && types[r * C + c + 2] === t) return true
        }
        for (let c = 0; c < C; c++) for (let r = 0; r + 2 < R; r++) {
          const t = types[r * C + c]
          if (t != null && types[(r + 1) * C + c] === t && types[(r + 2) * C + c] === t) return true
        }
        return false
      }
      const tryAt = (a, b) => {
        const t = types[a]; types[a] = types[b]; types[b] = t
        const ok = hasRun()
        const t2 = types[a]; types[a] = types[b]; types[b] = t2
        return ok
      }
      // 若指定锚点，优先找包含它的交换
      if (anchor != null) {
        const r = Math.floor(anchor / C), c = anchor % C
        const near = []
        if (c + 1 < C) near.push([anchor, anchor + 1])
        if (c > 0) near.push([anchor, anchor - 1])
        if (r + 1 < R) near.push([anchor, anchor + C])
        if (r > 0) near.push([anchor, anchor - C])
        for (const [a, b] of near) if (tryAt(a, b)) return [a, b]
      }
      for (let r = 0; r < R; r++) {
        for (let c = 0; c < C; c++) {
          if (c + 1 < C && tryAt(this.idx(r, c), this.idx(r, c + 1))) return [this.idx(r, c), this.idx(r, c + 1)]
          if (r + 1 < R && tryAt(this.idx(r, c), this.idx(r + 1, c))) return [this.idx(r, c), this.idx(r + 1, c)]
        }
      }
      return null
    },

    /* 洗牌：重排现有糖豆，直到无三连且有解 */
    shuffleBoard() {
      const types = this.cells.map(c => c.type)
      for (let attempt = 0; attempt < 120; attempt++) {
        for (let i = types.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          const t = types[i]; types[i] = types[j]; types[j] = t
        }
        this.cells = types.map(t => ({ type: t, special: null, breaking: false }))
        if (this.findRuns().length === 0 && this.findSwapHint()) return
      }
    },

    /* 每轮结束兜底：没有可交换的组合就自动重排 */
    async ensurePlayable() {
      if (this.findRuns().length) return
      const hasSpecial = this.cells.some(c => c.special)
      if (hasSpecial || this.findSwapHint()) return
      this.notice = '🌀 没有能消的组合啦，糖豆自动重排一下～'
      sound.magic()
      await this.sleep(420)
      this.shuffleBoard()
      this.cells = this.cells.slice()
    },

    resetIdle() {
      clearTimeout(this.idleTimer)
      if (this.stage !== 'play' || this.busy) return
      this.idleTimer = setTimeout(() => {
        if (!this.alive || this.stage !== 'play' || this.busy || this.sel != null) return
        const h = this.findSwapHint()
        if (h) {
          this.hintCells = h
          this.notice = '💡 卡住了？发光的这两颗换一下就能消～'
          clearTimeout(this.noticeTimer)
          this.noticeTimer = setTimeout(() => { this.hintCells = []; this.notice = '' }, 2800)
        }
      }, 7000)
    },

    showHint() {
      if (this.busy || this.stage !== 'play') return
      const special = this.cells.findIndex(c => c.special)
      const h = special >= 0 ? null : this.findSwapHint()
      if (special < 0 && !h) { this.ensurePlayable(); return }
      this.hintCells = special >= 0 ? [special] : h
      this.hintsUsed += 1
      this.notice = special >= 0
        ? '✨ 发光的这颗是特殊糖，点它或跟相邻糖交换就能引爆～'
        : '✨ 发光的这两颗换一下就能消（提示不扣步数）'
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
      this.shakeCells = []
      clearTimeout(this.noticeTimer)
      clearTimeout(this.idleTimer)
    },

    /* 重力下落 + 顶部补充（特殊糖随糖一起移动） */
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
            : { type: this.randType(), special: null, breaking: false }
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

    /* 把被消除的特殊糖效果展开进消除集合（条纹整行/整列、彩虹全场同色） */
    expandClears(base) {
      const cleared = new Set(base)
      const queue = [...base]
      const fired = new Set()
      while (queue.length) {
        const i = queue.shift()
        const cell = this.cells[i]
        if (!cell || !cell.special || fired.has(i)) continue
        fired.add(i)
        let extra = []
        if (cell.special === 'h') {
          const r = Math.floor(i / this.C)
          for (let c = 0; c < this.C; c++) extra.push(this.idx(r, c))
        } else if (cell.special === 'v') {
          const c = i % this.C
          for (let r = 0; r < this.R; r++) extra.push(this.idx(r, c))
        } else if (cell.special === 'rainbow') {
          const t = cell.type
          for (let k = 0; k < this.R * this.C; k++) {
            if (this.cells[k].type === t) extra.push(k)
          }
        }
        for (const j of extra) {
          if (!cleared.has(j)) { cleared.add(j); queue.push(j) }
        }
      }
      return cleared
    },

    /* 点击交互：点两颗相邻糖交换 */
    tap(i) {
      if (this.stage !== 'play' || !this.alive || this.busy) return
      if (this.dragged) { this.dragged = false; return }
      // 点到特殊糖：直接点一下也能引爆（比交换更直观）
      if (this.cells[i] && this.cells[i].special) {
        this.sel = null
        sound.click()
        this.moves -= 1
        this.usedMoves += 1
        this.cascade([{ cells: [i], len: 1, dir: 'h' }], null, null, null, true)
        return
      }
      if (this.sel == null) { this.sel = i; sound.hover(); this.resetIdle(); return }
      if (this.sel === i) { this.sel = null; sound.hover(); return }
      const r1 = Math.floor(this.sel / this.C), c1 = this.sel % this.C
      const r2 = Math.floor(i / this.C), c2 = i % this.C
      const adjacent = Math.abs(r1 - r2) + Math.abs(c1 - c2) === 1
      if (!adjacent) { this.sel = i; sound.hover(); return }
      const a = this.sel, b = i
      this.sel = null
      this.trySwap(a, b)
    },

    /* 拖拽交互：按住一颗，拖到相邻格松开即交换（开心消消乐手感） */
    onDragStart(i) {
      if (this.stage !== 'play' || this.busy) return
      this.dragFrom = i
      this.dragged = false
    },
    onDragEnter(i) {
      if (this.dragFrom == null || this.busy || this.dragged) return
      const a = this.dragFrom
      if (a === i) return
      const r1 = Math.floor(a / this.C), c1 = a % this.C
      const r2 = Math.floor(i / this.C), c2 = i % this.C
      if (Math.abs(r1 - r2) + Math.abs(c1 - c2) !== 1) return
      this.dragged = true
      this.dragFrom = null
      this.sel = null
      this.trySwap(a, i)
    },
    onDragEnd() {
      this.dragFrom = null
    },

    /* 交换两颗：能形成三连（或涉及特殊糖）就消除，否则弹回不扣步 */
    async trySwap(a, b) {
      if (this.busy || this.stage !== 'play') return
      this.busy = true
      this.clearFeedback()
      const ca = this.cells[a], cb = this.cells[b]
      // 彩虹糖与相邻糖交换 → 消全场对方颜色；双彩虹 → 全场清除
      let rainbowType = null, bothRainbow = false
      if (ca.special === 'rainbow' && cb.special === 'rainbow') bothRainbow = true
      else if (ca.special === 'rainbow') rainbowType = cb.type
      else if (cb.special === 'rainbow') rainbowType = ca.type

      this.shakeCells = []
      this.cells[a].breaking = false
      this.cells[b].breaking = false
      this.swapData(a, b)
      await this.sleep(170)

      let runs = this.findRuns()
      const isStripe = (c) => c && (c.special === 'h' || c.special === 'v')
      if (!runs.length && rainbowType == null && !bothRainbow) {
        // 条纹糖跟任意糖交换都能引爆（开心消消乐规则）
        const trig = []
        if (isStripe(this.cells[a])) trig.push(a)
        if (isStripe(this.cells[b])) trig.push(b)
        if (trig.length) {
          runs = [{ cells: trig, len: 1, dir: 'h' }]
        } else {
          this.swapData(a, b)             // 换不消，弹回，不扣步
          await this.sleep(150)
          this.shakeCells = [a, b]
          sound.fail()
          this.notice = '这样换消不掉，换个方向试试～'
          this.busy = false
          clearTimeout(this.noticeTimer)
          this.noticeTimer = setTimeout(() => { this.shakeCells = []; this.notice = ''; this.resetIdle() }, 1500)
          return
        }
      }

      this.moves -= 1
      this.usedMoves += 1
      await this.cascade(runs, rainbowType, a, b, false, bothRainbow)
      this.afterAction()
    },

    /* 连锁消除主循环 */
    async cascade(firstRuns, rainbowType, anchorA, anchorB, directBlast, bothRainbow) {
      let chain = 0
      let runs = firstRuns
      let rt = rainbowType
      let clearAll = bothRainbow
      let first = true

      while (this.alive) {
        const base = new Set()
        if (clearAll) {
          for (let k = 0; k < this.R * this.C; k++) base.add(k)
          clearAll = false
        } else if (rt != null) {
          for (let k = 0; k < this.R * this.C; k++) {
            const c = this.cells[k]
            if (c.type === rt || c.special === 'rainbow') base.add(k)
          }
          rt = null
        } else {
          for (const run of runs) for (const i of run.cells) base.add(i)
        }
        if (base.size === 0) break
        chain += 1
        this.maxCascade = Math.max(this.maxCascade, chain)

        // 只在玩家主动的第一次消除里生成特殊糖
        const spawns = new Map()
        if (first && !directBlast) {
          for (const run of runs) {
            const anchor = [anchorA, anchorB].find(x => x != null && run.cells.includes(x))
            const at = anchor != null ? anchor : run.cells[Math.floor(run.cells.length / 2)]
            let sp = null
            if (run.len >= 5) sp = 'rainbow'
            else if (run.len === 4) sp = run.dir === 'h' ? 'h' : 'v'
            if (sp && !spawns.has(at)) spawns.set(at, sp)
          }
        }

        // 展开被消除的特殊糖（含对方引爆、彩虹波及）
        const cleared = this.expandClears(base)
        for (const at of spawns.keys()) cleared.delete(at)   // 生成位置保留下来
        if (cleared.size === 0 && spawns.size === 0) break

        const gain = cleared.size * 10 * chain
        this.score += gain
        this.$emit('score', this.score)
        if (chain >= 2) sound.magic(); else sound.pop()

        for (const i of cleared) this.cells[i].breaking = true
        this.cells = this.cells.slice()
        await this.sleep(220)

        for (const i of cleared) { this.cells[i] = { type: null, special: null, breaking: false } }
        for (const [at, sp] of spawns) {
          if (this.cells[at].type == null) this.cells[at].type = this.randType()
          this.cells[at].special = sp
          this.cells[at].breaking = false
        }
        this.cells = this.cells.slice()
        this.gravityRefill()
        await this.sleep(140)

        first = false
        directBlast = false
        runs = this.findRuns()
      }
      await this.ensurePlayable()
    },

    /* 一次操作结束：解锁 + 检查结算 + 空闲提示 */
    afterAction() {
      this.busy = false
      this.sel = null
      if (this.moves <= 0 && this.alive) { this.finish(); return }
      this.resetIdle()
    },

    finish() {
      this.stage = 'done'
      clearTimeout(this.idleTimer)
      clearTimeout(this.noticeTimer)
      this.hintCells = []
      this.shakeCells = []
      this.notice = ''
      this.$emit('score', this.score)
      this.$emit('over', this.score)
      if (this.stars >= 2) sound.success(); else sound.fail()
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
.m3-moves { color: var(--muted); white-space: nowrap; }
.m3-moves b { color: var(--danger); font-size: 15px; }
.m3-progress { flex: 1; min-width: 130px; }
.m3-progress-top { display: flex; justify-content: space-between; font-size: 11px; color: var(--muted); margin-bottom: 4px; }
.m3-bar { height: 7px; border-radius: 999px; background: var(--track); overflow: hidden; }
.m3-bar-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, var(--pink), var(--purple)); transition: width .3s ease; }
.m3-hint-btn {
  border: 1px solid var(--border); border-radius: 999px; cursor: pointer;
  background: var(--card); color: var(--primary); font-weight: 800; font-size: 11.5px;
  padding: 5px 12px; transition: filter .15s, transform .12s; white-space: nowrap;
}
.m3-hint-btn:hover:not(:disabled) { filter: brightness(1.05); transform: translateY(-1px); }
.m3-hint-btn:disabled { opacity: .5; cursor: default; }

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
  font-size: 12.5px; color: var(--text); max-width: 420px;
}
.m3-tip-list li {
  padding: 9px 12px; border-radius: 10px;
  background: var(--card); border: 1px solid var(--border);
}

.m3-board {
  display: grid;
  grid-template-columns: repeat(var(--cols), 1fr);
  gap: 6px;
  width: min(360px, 88vw);
  padding: 10px;
  border-radius: 16px;
  background: var(--track);
  border: 1px solid var(--border);
  touch-action: none;
}
.m3-cell {
  position: relative;
  aspect-ratio: 1 / 1;
  border: none; border-radius: 12px;
  font-size: clamp(18px, 6vw, 26px);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: transform .12s ease, box-shadow .12s ease, opacity .18s ease;
  box-shadow: inset 0 -3px 0 rgba(0,0,0,.12);
  user-select: none;
}
.m3-cell:hover { transform: translateY(-1px); }
.m3-cell.sel {
  box-shadow: 0 0 0 3px #fff, 0 0 0 6px var(--primary);
  transform: scale(1.06); z-index: 2;
}
.m3-cell.hint {
  z-index: 2;
  box-shadow: 0 0 0 3px #fff, 0 0 0 6px var(--yellow), 0 0 14px rgba(247,224,138,.9);
  animation: m3Hint .75s ease-in-out infinite alternate;
}
.m3-cell.shake { animation: m3Shake .34s ease; }
.m3-cell.breaking { transform: scale(.2) rotate(20deg); opacity: 0; }
.m3-cell.sp-h .m3-emoji { filter: drop-shadow(0 0 5px #fff); }
.m3-cell.sp-v .m3-emoji { filter: drop-shadow(0 0 5px #fff); }
.m3-cell.sp-h::after, .m3-cell.sp-v::after {
  content: ''; position: absolute; border-radius: 3px; pointer-events: none;
  background: rgba(255,255,255,.85);
}
.m3-cell.sp-h::after { left: 12%; right: 12%; top: 48%; height: 3px; }
.m3-cell.sp-v::after { top: 12%; bottom: 12%; left: 48%; width: 3px; }
.m3-cell.sp-rainbow {
  background: conic-gradient(#ff9ec4,#ffd166,#8be08b,#6ec6e6,#b079ff,#ff9ec4) !important;
  animation: m3Rainbow 2.2s linear infinite;
}
.m3-spark {
  position: absolute; top: -4px; right: -4px; font-size: 12px;
  text-shadow: 0 1px 3px rgba(0,0,0,.3);
}
.m3-board.busy .m3-cell { cursor: default; }

.m3-hint { margin: 0; font-size: 12px; color: var(--muted); min-height: 16px; }
.m3-notice {
  margin: -6px 0 0; font-size: 12px; font-weight: 700; color: var(--primary);
  background: var(--card); border: 1px solid var(--border);
  border-radius: 999px; padding: 6px 14px; text-align: center;
}
.m3-stars { font-size: 34px; letter-spacing: 4px; color: var(--border); }
.m3-stars .on { color: #ffcf4a; text-shadow: 0 2px 6px rgba(255,207,74,.5); }
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
.m3-btn:hover:not(:disabled) { filter: brightness(1.07); transform: translateY(-1px); }
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
@keyframes m3Rainbow { to { filter: hue-rotate(360deg); } }
@media (prefers-reduced-motion: reduce) {
  .m3-cell.hint, .m3-cell.shake, .m3-cell.sp-rainbow { animation: none; }
  .m3-cell.hint { transform: scale(1.08); }
}
</style>
