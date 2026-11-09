<template>
  <div class="m3-wrap">
    <!-- HUD：剩余步数 + 当前得分 -->
    <div class="m3-hud">
      <span class="m3-moves">剩余步数 <b>{{ moves }}</b></span>
      <span class="m3-total">得分 {{ score }}</span>
    </div>

    <!-- ===== 准备阶段 ===== -->
    <div v-if="stage === 'ready'" class="m3-stage m3-ready">
      <div class="m3-mascot">🍬</div>
      <h3>糖豆消消乐</h3>
      <p>泡芙的糖果罐打翻啦！把 <b>3 颗以上同色糖豆</b> 连成一线就能消除，<br>步数内尽量刷高分，连锁消除还有翻倍奖励 ✨</p>
      <ul class="m3-tip-list">
        <li>👆 先点一颗糖豆，再点 <b>上下左右相邻</b> 的那颗交换</li>
        <li>💥 能连成 3+ 才会消除；连不动就换回来，不扣步数</li>
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
          :class="{ sel: i === sel, breaking: cell.breaking }"
          :style="{ background: cell.type == null ? 'transparent' : TYPES[cell.type].bg }"
          @click="tap(i)"
        ><span v-if="cell.type != null">{{ TYPES[cell.type].e }}</span></button>
      </div>
      <p class="m3-hint">{{ busy ? '消除中…' : (sel == null ? '点一颗糖豆开始' : '再点相邻的那颗交换') }}</p>
      <button class="m3-btn ghost" @click="restart">重开一局</button>
    </div>

    <!-- ===== 结算 ===== -->
    <div v-else-if="stage === 'done'" class="m3-stage m3-done">
      <div class="m3-mascot">{{ score >= 600 ? '🏆' : score >= 350 ? '🥈' : '🍬' }}</div>
      <h3>{{ score >= 600 ? '糖果大师！泡芙都想拜你为师' : score >= 350 ? '手感不错，糖豆都怕你' : '甜甜的一局，再来刷高点！' }}</h3>
      <div class="m3-final">本局得分 <b>{{ score }}</b></div>
      <div class="m3-final sub">用了 {{ usedMoves }} 步 · 连锁最高 x{{ maxCascade }}</div>
      <button class="m3-btn big" @click="restart">再来一局！</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'

export default {
  name: 'GameMatch3',
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
      alive: true
    }
  },
  beforeUnmount() { this.alive = false },
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
      this.sel = null
      this.busy = false
      this.genBoard()
      this.$emit('score', 0)
    },
    restart() {
      this.stage = 'ready'
      this.$emit('score', 0)
    },

    /* 生成无初始三连的棋盘 */
    genBoard() {
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
    },

    /* 找出所有处于 3+ 连线上的格子下标 */
    findMatches() {
      const set = new Set()
      const { R, C } = this
      // 横向
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
      // 纵向
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

    async tap(i) {
      if (this.busy || this.stage !== 'play' || !this.alive) return
      if (this.sel == null) { this.sel = i; sound.hover(); return }
      if (this.sel === i) { this.sel = null; return }
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
        this.busy = false
        return
      }
      this.moves -= 1
      this.usedMoves += 1
      await this.cascade()
      this.busy = false
      if (this.moves <= 0 && this.alive) this.finish()
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
    },

    finish() {
      this.stage = 'done'
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
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 12px;
  background: var(--panel); font-size: 12.5px;
}
.m3-moves { color: var(--muted); }
.m3-moves b { color: var(--danger); font-size: 15px; }
.m3-total { flex: 1; text-align: right; font-weight: 800; color: var(--primary); }

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
.m3-cell.breaking {
  transform: scale(.2) rotate(20deg);
  opacity: 0;
}
.m3-board.busy .m3-cell { cursor: default; }

.m3-hint { margin: 0; font-size: 12px; color: var(--muted); min-height: 16px; }
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
</style>
