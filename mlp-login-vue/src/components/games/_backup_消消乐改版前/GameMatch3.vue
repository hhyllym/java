<template>
  <div class="m3-wrap">
    <!-- HUD：剩余步数 + 提示按钮 + 当前得分 -->
    <div class="m3-hud">
      <span class="m3-moves">剩余步数 <b>{{ moves }}</b></span>
      <button class="m3-auto-btn" :class="{ on: mode === 'pick' }" @click="toggleAuto">
        {{ mode === 'pick' ? '👆 点击消除' : (mode === 'auto' ? '✨ 智能点消' : '✋ 手动交换') }}
      </button>
      <button class="m3-hint-btn" :disabled="busy || stage !== 'play'" @click="showHint">💡 提示</button>
      <span class="m3-total">得分 {{ score }}</span>
    </div>

    <!-- ===== 准备阶段 ===== -->
    <div v-if="stage === 'ready'" class="m3-stage m3-ready">
      <div class="m3-mascot">🍬</div>
      <h3>糖豆消消乐</h3>
      <p>泡芙的糖果罐打翻啦！点 <b>2 颗以上相邻的同色糖豆</b> 就能消除，<br>步数内尽量刷高分，连锁消除还有翻倍奖励 ✨</p>
      <ul class="m3-tip-list">
        <li>👆 <b>点第一颗，再点相邻的同色糖豆</b> —— 凑够 <b>2 颗</b> 就消除，连成 <b>3 颗以上</b> 整条一起消！</li>
        <li>✋ 想省事？点上面的按钮切到「✨ 智能点消」，点任意一颗泡芙就帮你消</li>
        <li>🔁 想自己找？再点一次按钮切到「✋ 手动交换」，先点一颗再点相邻那颗交换</li>
        <li>💡 点「提示」会<b>发光标出</b>能消的一组；全场没组合了会自动重排，不会卡死</li>
        <li>🔥 一次消除引发连锁，每多消一轮得分翻倍</li>
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
            sel: i === sel || picked.includes(i),
            breaking: cell.breaking,
            hint: hintCells.includes(i),
            autoPick: swapCells.includes(i),
            shake: shakeCells.includes(i)
          }"
          :style="{ background: cell.type == null ? 'transparent' : TYPES[cell.type].bg }"
          @click="tap(i)"
        ><span v-if="cell.type != null">{{ TYPES[cell.type].e }}</span></button>
      </div>
      <p class="m3-hint">{{ busy ? '消除中…' : bottomHint }}</p>
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
      // 点选模式下已选中的格子下标（相邻同色链）
      picked: [],
      moves: 20,
      usedMoves: 0,
      score: 0,
      maxCascade: 1,
      busy: false,
      // ↓ 本次新增：提示 / 无效交换反馈 / 死局洗牌
      hintCells: [],
      swapCells: [],
      // 玩法模式：'pick'=点击消除（默认） 'auto'=智能点消 'swap'=手动交换
      mode: 'pick',
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
    // 默认「点选消除」；玩家切过模式就记在本地
    // （兼容老版本存下来的 mlp_m3_auto：'0'=手动交换, '1'=智能点消）
    try {
      const saved = localStorage.getItem('mlp_m3_mode')
      if (saved === 'pick' || saved === 'auto' || saved === 'swap') {
        this.mode = saved
      } else {
        const old = localStorage.getItem('mlp_m3_auto')
        this.mode = old === '0' ? 'swap' : 'pick'
      }
    } catch (e) { this.mode = 'pick' }
  },
  computed: {
    /** 底部提示文案：跟着当前模式走 */
    bottomHint() {
      if (this.mode === 'pick') {
        if (this.picked.length === 0) return '点一颗糖豆，再点相邻的同色糖豆，凑够 2 颗就消除 👆'
        return `已选 ${this.picked.length} 颗，再点相邻同色的就消除 ✨`
      }
      if (this.mode === 'auto') return '点任意一颗糖豆，泡芙自动帮你消 ✨'
      return this.sel == null ? '点一颗糖豆开始' : '再点相邻的那颗交换'
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
      this.moves = 20
      this.usedMoves = 0
      this.maxCascade = 1
      this.hintsUsed = 0
      this.sel = null
      this.picked = []
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
      this.picked = []
      this.sel = null
      this.queuedTap = null
      this.shakeCells = []
      clearTimeout(this.noticeTimer)
      clearTimeout(this.idleTimer)
      this.$emit('score', 0)
    },

    /* 生成无初始三连、且「保证有可行组合」的棋盘 */
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
        // 点选模式：只要有相邻同色就能消；其它模式要能交换出三连
        if (this.mode === 'pick') {
          if (this.findClickablePair()) return
        } else if (this.findHint()) {
          return
        }
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

    /** 洗牌：随机重排现有糖豆，直到「无自动三连」且「有可行组合」 */
    shuffleBoard() {
      const types = this.cells.map(c => c.type)
      for (let attempt = 0; attempt < 80; attempt++) {
        for (let i = types.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          const t = types[i]; types[i] = types[j]; types[j] = t
        }
        this.cells = types.map(t => ({ type: t, breaking: false }))
        // 点选模式：只要有相邻同色即可消
        if (this.mode === 'pick') {
          if (this.findClickablePair()) return
        } else if (this.findMatches().length === 0 && this.findHint()) {
          return
        }
      }
      // 实在洗不出来就整盘重生成
      this.cells = types.map(t => ({ type: t, breaking: false }))
    },

    /** 每轮连锁结束后兜底：全场没组合就自动洗牌，不让玩家卡死 */
    async ensurePlayable() {
      // 点选模式：只要有「相邻同色」就能消；其它模式需要「能交换出三连」
      const hasCombo = this.mode === 'pick'
        ? !!this.findClickablePair()
        : !!this.findHint()
      if (hasCombo) return
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
        // 点选模式下找「能直接点消的一对」，其它模式沿用原来的交换提示
        const h = this.mode === 'pick' ? this.findClickablePair() : this.findHint()
        if (h) {
          this.hintCells = h
          this.notice = this.mode === 'pick'
            ? '💡 卡住了？发光的这两颗点一下就能消～'
            : (this.mode === 'auto'
              ? '💡 点任意一颗糖豆，泡芙就会帮你消～'
              : '💡 卡住了？发光的这两颗换一下就能消～')
        }
      }, 6000)
    },

    /**
     * 找一对「相邻且同色」的糖豆 —— 点选模式下这俩直接点就能消除。
     * 顺带把整条同色连线都标出来（3 颗以上整条消）。
     */
    findClickablePair() {
      const { R, C } = this
      for (let r = 0; r < R; r++) {
        for (let c = 0; c < C; c++) {
          const a = this.idx(r, c)
          const ta = this.cellAt(r, c).type
          if (ta == null) continue
          // 先看左右
          if (c + 1 < C && this.cellAt(r, c + 1).type === ta) {
            return this.expandLine([a, this.idx(r, c + 1)])
          }
          // 再看上下
          if (r + 1 < R && this.cellAt(r + 1, c).type === ta) {
            return this.expandLine([a, this.idx(r + 1, c)])
          }
        }
      }
      return null
    },

    showHint() {
      if (this.busy || this.stage !== 'play') return
      // 点选模式：直接点亮可以点消的一对（不需要交换）
      const h = this.mode === 'pick' ? this.findClickablePair() : this.findHint()
      if (!h) { this.ensurePlayable(); return }
      this.hintCells = h
      this.hintsUsed += 1
      this.notice = this.mode === 'pick'
        ? '✨ 发光的这几颗点一下就能消啦（不扣步数）'
        : (this.mode === 'auto'
          ? '✨ 智能点消已开，直接点任意一颗就能消～'
          : '✨ 发光的这两颗换一下就能消啦（不扣步数）')
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
      // 三种玩法：pick=点击消除（默认） / auto=智能点消 / swap=手动交换
      if (this.mode === 'pick') return this.tapPick(i)
      if (this.mode === 'auto') return this.autoPlay(i)
      return this.manualTap(i)
    },

    /* ---------- 点选消除模式 ---------- */
    // 规则：点第一颗 → 再点相邻同色的糖豆把它们连起来，
    //       凑够 2 颗就消除，连成 3 颗以上则整条一起消。
    //       点非相邻的糖豆 = 重新开始选；再点已选的 = 取消选中。

    /** 与格子 i 相邻的格子下标 */
    neighborsOf(i) {
      const r = Math.floor(i / this.C), c = i % this.C
      const out = []
      if (r > 0) out.push(this.idx(r - 1, c))
      if (r < this.R - 1) out.push(this.idx(r + 1, c))
      if (c > 0) out.push(this.idx(r, c - 1))
      if (c < this.C - 1) out.push(this.idx(r, c + 1))
      return out
    },

    /**
     * 以已选链的最后一颗为端点，试着把 i 接上去。
     * 只有「与链尾相邻」且「同色」才接得上。
     */
    canExtendChain(i) {
      if (this.picked.length === 0) return true
      if (this.picked.includes(i)) return false
      const tail = this.picked[this.picked.length - 1]
      const t1 = this.cellAt(Math.floor(tail / this.C), tail % this.C).type
      const t2 = this.cellAt(Math.floor(i / this.C), i % this.C).type
      if (t1 == null || t2 == null || t1 !== t2) return false
      return this.neighborsOf(tail).includes(i)
    },

    /** 点选消除的主入口 */
    async tapPick(i) {
      const cell = this.cellAt(Math.floor(i / this.C), i % this.C)
      if (!cell || cell.type == null) return

      // 已经选中的 → 取消选中
      if (this.picked.includes(i)) {
        this.picked = this.picked.filter(x => x !== i)
        sound.hover()
        this.updatePickHint()
        return
      }

      // 不能接到当前链尾 → 重新开始选
      if (!this.canExtendChain(i)) {
        this.picked = [i]
        sound.hover()
        this.updatePickHint()
        return
      }

      this.picked = [...this.picked, i]
      sound.click()

      // 满 2 颗就消除
      if (this.picked.length >= 2) {
        await this.eliminatePicked()
        return
      }
      this.updatePickHint()
    },

    /** 提示文字：告诉玩家现在选了几颗、还能怎么点 */
    updatePickHint() {
      if (this.picked.length === 0) {
        this.notice = ''
        return
      }
      this.notice = `已选 ${this.picked.length} 颗，再点一颗相邻同色的就消除 ✨`
    },

    /** 把选中的那几颗消掉，并结算连锁 */
    async eliminatePicked() {
      this.busy = true
      const picked = [...this.picked]
      this.picked = []
      this.hintCells = []
      this.swapCells = []
      this.shakeCells = []

      this.moves -= 1
      this.usedMoves += 1

      // 选中的是 2 颗时，再看看能不能顺带连成更长的线（3+ 整条消）
      const line = this.expandLine(picked)
      this.score += line.length * 10
      this.$emit('score', this.score)
      if (line.length >= 3) sound.magic(); else sound.pop()

      for (const m of line) this.cells[m].breaking = true
      this.cells = this.cells.slice()
      await this.sleep(220)
      for (const m of line) this.cells[m].type = null
      this.cells = this.cells.slice()
      this.gravityRefill()
      await this.sleep(120)

      // 消除后可能又自动连出新的一组，继续连锁
      await this.cascade()

      this.busy = false
      this.afterAction()
    },

    /**
     * 把选中的几颗往两端扩展，找出一条完整的同色连线。
     * 例如选中 (2,2) 和 (2,3)，若 (2,1)(2,4) 也同色，则 4 颗整条消除。
     */
    expandLine(picked) {
      const { R, C } = this
      const type = this.cellAt(Math.floor(picked[0] / C), picked[0] % C).type
      if (type == null) return picked

      const inSet = new Set(picked)
      const typeAt = (r, c) => {
        if (r < 0 || r >= R || c < 0 || c >= C) return null
        return this.cellAt(r, c).type
      }

      // 选中的是竖直相邻 → 找竖直连线；横向相邻 → 找横向连线
      const r0 = Math.floor(picked[0] / C), c0 = picked[0] % C
      const r1 = Math.floor(picked[picked.length - 1] / C), c1 = picked[picked.length - 1] % C
      const vertical = r0 !== r1

      const line = new Set(picked)
      if (vertical) {
        const col = c0
        let up = r0
        while (typeAt(up - 1, col) === type) { up--; line.add(this.idx(up, col)) }
        let down = r1
        while (typeAt(down + 1, col) === type) { down++; line.add(this.idx(down, col)) }
      } else {
        const row = r0
        let left = c0
        while (typeAt(row, left - 1) === type) { left--; line.add(this.idx(row, left)) }
        let right = c1
        while (typeAt(row, right + 1) === type) { right++; line.add(this.idx(row, right)) }
      }
      // 去掉选中的链里非同色的（理论上不会有，保险）
      for (const p of [...line]) {
        const r = Math.floor(p / C), c = p % C
        if (typeAt(r, c) !== type) line.delete(p)
      }
      return [...line]
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
      // 三态循环：点击消除 → 智能点消 → 手动交换 → 点击消除
      const order = ['pick', 'auto', 'swap']
      const next = order[(order.indexOf(this.mode) + 1) % order.length]
      this.mode = next
      try { localStorage.setItem('mlp_m3_mode', next) } catch (e) { /* 隐私模式忽略 */ }
      this.sel = null
      this.picked = []
      this.queuedTap = null
      this.clearFeedback()
      this.notice = next === 'pick'
        ? '👆 点击消除：点一颗，再点相邻同色的，凑够 2 颗就消'
        : (next === 'auto'
          ? '✨ 智能点消已开：点任意一颗就能消～'
          : '✋ 手动交换：先点一颗，再点上下左右相邻的那颗')
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

    /**
     * 找出当前模式下应该被消除的格子。
     * - pick 模式：相邻 2 颗同色就算一组（点一下就能消）
     * - 其它模式：3 颗以上连成一线
     */
    findEliminations() {
      if (this.mode !== 'pick') return this.findMatches()
      const { R, C } = this
      const set = new Set()
      for (let r = 0; r < R; r++) {
        for (let c = 0; c < C; c++) {
          const t = this.cellAt(r, c).type
          if (t == null) continue
          const right = c + 1 < C ? this.cellAt(r, c + 1).type : null
          const down = r + 1 < R ? this.cellAt(r + 1, c).type : null
          if (right === t || down === t) {
            set.add(this.idx(r, c))
            if (right === t) set.add(this.idx(r, c + 1))
            if (down === t) set.add(this.idx(r + 1, c))
          }
        }
      }
      return [...set]
    },

    /* 连锁消除，每多一轮得分翻倍 */
    async cascade() {
      let chain = 0
      while (this.alive) {
        const matches = this.findEliminations()
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
