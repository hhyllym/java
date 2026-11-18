<template>
  <div class="pz-wrap">
    <div class="pz-hud">
      <div class="hud-chip"><span>🧩 步数</span><b>{{ steps }}</b></div>
      <div class="hud-chip"><span>⏱️ 用时</span><b>{{ seconds }}s</b></div>
      <div class="hud-chip"><span>💡 提示</span><b>{{ hintsLeft > 0 ? hintsLeft + ' 次' : '用完啦' }}</b></div>
      <div class="hud-chip"><span>🎯 块数</span><b>{{ goal.length }}</b></div>
    </div>

    <!-- 难度切换 -->
    <div class="pz-levels">
      <span class="lv-label">难度</span>
      <button
        v-for="l in LEVELS" :key="l.key"
        class="lv-btn" :class="{ on: level === l.key }"
        :disabled="busy"
        @click="pickLevel(l.key)"
      >{{ l.name }} {{ l.rows }}×{{ l.cols }}</button>
    </div>

    <div v-if="!done" class="pz-body">
      <div class="pz-goal">
        <span class="goal-label">✨ 目标图案</span>
        <div class="pz-mini" :style="miniStyle">
          <span v-for="t in goal" :key="t.id" :style="{ background: t.color }">{{ t.emoji }}</span>
        </div>
      </div>

      <TransitionGroup name="pz" tag="div" class="pz-board" :style="boardStyle">
        <button
          v-for="(t, i) in tiles"
          :key="t.id"
          class="pz-tile"
          :class="{ sel: selected === i, ok: t.id === i }"
          :style="{ background: t.color, fontSize: tileFont + 'px' }"
          @click="tap(i)"
        >
          <span>{{ t.emoji }}</span>
        </button>
      </TransitionGroup>

      <p class="pz-tip">点两块交换位置，把心印碎片拼回目标图 ✨</p>
      <div class="pz-btns">
        <button class="pz-mini-btn" :disabled="hintsLeft <= 0" @click="hint">💡 提示 <small>-25 分</small></button>
        <button class="pz-mini-btn" :disabled="!canUndo" @click="undo">↩️ 撤销</button>
        <button class="pz-mini-btn" @click="restart">🔀 重新洗牌</button>
      </div>
    </div>

    <div v-else class="pz-done">
      <div class="pz-mascot">🎉</div>
      <h3>心印拼好啦！</h3>
      <p>用了 <b>{{ steps }}</b> 步 · <b>{{ seconds }}</b> 秒 · 提示 <b>{{ hintsUsed }}</b> 次 · {{ goal.length }} 块</p>
      <div class="pz-score">+{{ score }} 分</div>
      <button class="pz-btn" @click="restart">🔁 再拼一次</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'

/** 一百枚原创「星糖谷心印碎片」：可爱系 emoji，底色按金色角螺旋取粉彩色 */
const ALL_EMOJI = [
  '🌈', '⭐', '🍬', '🦄', '🦋', '💎', '🧁', '🍓',
  '🍩', '🌸', '🍇', '🍀', '🌺', '🎀', '🍒', '🐝',
  '🌙', '🔮', '🍄', '🪁', '🦊', '🐧', '🐬', '🌊',
  '🎸', '🎨', '🧸', '🌻', '🪺', '🦩', '🌼', '🌷',
  '🦔', '🦉', '🐢', '🦕', '🐘', '🦓', '🐳', '🐠',
  '🐡', '🦀', '🍥', '🍘', '🍙', '🍜', '🍣', '🍰',
  '🍪', '🥧', '🍯', '🍭', '🥛', '🍫', '🥐', '🧀',
  '🥚', '🍳', '🥞', '🍕', '🌭', '🥗', '🌽', '🥕',
  '🍅', '🥒', '🍆', '🌶️', '🥑', '🧄', '🍋', '🥝',
  '🍈', '🍉', '🍑', '🍍', '🍌', '🧃', '☕', '🍵',
  '🧋', '🥂', '🍹', '🧊', '🎁', '🎈', '🪄', '🪅',
  '🐥', '🐣', '🐰', '🐻', '🐨', '🐱', '🐶', '🚀',
  '🎃', '🍁', '🐙', '🦞', '🐚', '🥥', '🍎', '🎠',
  '🍿', '🫧', '🌟', '🎪', '🎡', '🩷', '🩵', '🩶'
]

/** 100 种粉彩色（HSL 黄金角螺旋），相邻块色差明显，好辨认 */
const PALETTE = Array.from({ length: ALL_EMOJI.length }, (_, i) => {
  const hue = Math.round((i * 137.508) % 360)
  const sat = 72 + (i % 3) * 6
  const light = 78 + (i % 4) * 3
  return `hsl(${hue}, ${sat}%, ${light}%)`
})

const ALL_PIECES = ALL_EMOJI.map((emoji, i) => ({
  id: i,
  emoji,
  color: PALETTE[i]
}))

/** 十三档难度：9 → 100 块，块数越多越难，步数与得分同步放宽 */
const LEVELS = [
  { key: 'easy',    name: '简单', rows: 3,  cols: 3  },
  { key: 'normal',  name: '普通', rows: 3,  cols: 4  },
  { key: 'hard',    name: '困难', rows: 4,  cols: 4  },
  { key: 'expert',  name: '高手', rows: 4,  cols: 5  },
  { key: 'master',  name: '大师', rows: 5,  cols: 5  },
  { key: 'star',    name: '星耀', rows: 5,  cols: 6  },
  { key: 'legend',  name: '传奇', rows: 6,  cols: 6  },
  { key: 'myth',    name: '神话', rows: 6,  cols: 7  },
  { key: 'ultra',   name: '终极', rows: 7,  cols: 7  },
  { key: 'endless', name: '无尽', rows: 7,  cols: 8  },
  { key: 'cosmic',  name: '宇宙', rows: 8,  cols: 8  },
  { key: 'divine',  name: '神级', rows: 9,  cols: 9  },
  { key: 'supreme', name: '至尊', rows: 10, cols: 10 }
]

export default {
  name: 'GamePuzzle',
  emits: ['score', 'over'],
  data() {
    return {
      ALL_PIECES,
      LEVELS,
      level: 'master',
      tiles: [],
      selected: -1,
      steps: 0,
      seconds: 0,
      hintsLeft: 5,
      hintsUsed: 0,
      done: false,
      score: 0,
      busy: false,
      history: [],        // 撤销栈：记录每次交换的两个下标
      timer: null,
      alive: true
    }
  },
  computed: {
    cfg() { return LEVELS.find(l => l.key === this.level) || LEVELS[4] },
    rows() { return this.cfg.rows },
    cols() { return this.cfg.cols },
    /** 目标图案：按难度取前 N 枚，id 即目标位置索引 */
    goal() { return this.ALL_PIECES.slice(0, this.rows * this.cols) },
    maxHints() { return Math.max(2, Math.round(this.goal.length / 5)) },
    canUndo() { return this.history.length > 0 && !this.done },
    /** 目标图预览：整体收窄成一列小图，格子随列数缩小，但 emoji 仍认得出 */
    miniStyle() {
      const gap = 2
      const cell = this.cols >= 10 ? 12 : (this.cols >= 8 ? 14 : (this.cols >= 6 ? 16 : (this.cols >= 5 ? 18 : 20)))
      return {
        gridTemplateColumns: `repeat(${this.cols}, 1fr)`,
        gap: gap + 'px',
        '--mc': cell + 'px',
        '--mfs': Math.round(cell * 0.66) + 'px'
      }
    },
    /**
     * 棋盘：撑满可用宽度（不缩成一小坨），列数越多格子自然越小，
     * 但保证最小 44px、emoji 字号不低于 16px，肉眼始终可辨。
     */
    boardStyle() {
      const gap = this.cols >= 9 ? 5 : (this.cols >= 6 ? 7 : 8)
      // 小棋盘别撑成巨格：按列数给出棋盘上限宽度
      const cap = this.cols <= 3 ? 340 : (this.cols <= 4 ? 420 : (this.cols <= 5 ? 500 : (this.cols <= 6 ? 580 : 760)))
      return {
        gridTemplateColumns: `repeat(${this.cols}, minmax(0, 1fr))`,
        gap: gap + 'px',
        maxWidth: cap + 'px'
      }
    },
    /** 字号跟着格子走：用 clamp 兜底，窄屏也不会小到看不清 */
    tileFont() {
      if (this.cols >= 10) return 'clamp(14px, 2.4vw, 22px)'
      if (this.cols >= 9) return 'clamp(15px, 2.6vw, 24px)'
      if (this.cols >= 8) return 'clamp(16px, 2.9vw, 26px)'
      if (this.cols >= 7) return 'clamp(18px, 3.2vw, 28px)'
      if (this.cols >= 6) return 'clamp(20px, 3.6vw, 30px)'
      if (this.cols >= 5) return 'clamp(22px, 4vw, 32px)'
      if (this.cols >= 4) return 'clamp(24px, 4.4vw, 34px)'
      return 'clamp(26px, 5vw, 38px)'
    }
  },
  mounted() {
    this.restart()
    this.timer = setInterval(() => {
      if (!this.alive || this.done) return
      this.seconds += 1
    }, 1000)
  },
  beforeUnmount() {
    this.alive = false
    clearInterval(this.timer)
  },
  methods: {
    shuffle() {
      const arr = this.goal.slice()
      // Fisher-Yates，保证足够多块错位，别一上来就快拼好了
      const minWrong = Math.max(4, Math.floor(arr.length * 0.6))
      let guard = 0
      do {
        for (let i = arr.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[arr[i], arr[j]] = [arr[j], arr[i]]
        }
        guard += 1
      } while (guard < 150 && arr.filter((t, i) => t.id !== i).length < minWrong)
      return arr
    },
    pickLevel(k) {
      if (this.level === k || this.busy) return
      this.level = k
      sound.click()
      this.restart()
    },
    restart() {
      this.tiles = this.shuffle()
      this.selected = -1
      this.steps = 0
      this.seconds = 0
      this.hintsLeft = this.maxHints
      this.hintsUsed = 0
      this.done = false
      this.score = 0
      this.history = []
      this.emitScore()
      sound.click()
    },
    /** 交换两块并记录，支持撤销 */
    swapTiles(a, b) {
      ;[this.tiles[a], this.tiles[b]] = [this.tiles[b], this.tiles[a]]
      this.history.push([a, b])
    },
    tap(i) {
      if (this.done) return
      sound.hover()
      if (this.selected === -1) {
        this.selected = i
        return
      }
      if (this.selected === i) {
        this.selected = -1
        return
      }
      this.swapTiles(this.selected, i)
      this.selected = -1
      this.steps += 1
      sound.pop()
      if (this.tiles.every((t, idx) => t.id === idx)) this.finish()
    },
    /** 撤销上一步交换；步数同步回退，不会刷分 */
    undo() {
      if (!this.canUndo) return
      const [a, b] = this.history.pop()
      ;[this.tiles[a], this.tiles[b]] = [this.tiles[b], this.tiles[a]]
      this.steps = Math.max(0, this.steps - 1)
      this.selected = -1
      sound.hover()
    },
    hint() {
      if (this.done || this.hintsLeft <= 0) return
      const i = this.tiles.findIndex((t, idx) => t.id !== idx)
      if (i < 0) return
      const j = this.tiles.findIndex(t => t.id === i)
      this.swapTiles(i, j)
      this.hintsLeft -= 1
      this.hintsUsed += 1
      this.steps += 1
      this.selected = -1
      sound.magic()
      if (this.tiles.every((t, idx) => t.id === idx)) this.finish()
    },
    finish() {
      this.done = true
      const n = this.goal.length
      // 块数越多基础分越高、每步扣分越少，难度升级但依然鼓励少步数
      const base = 180 + n * 30
      let perStep = 10
      if (n >= 72) perStep = 3
      else if (n >= 48) perStep = 4
      else if (n >= 36) perStep = 5
      else if (n >= 24) perStep = 6
      else if (n >= 20) perStep = 7
      else if (n >= 16) perStep = 8
      else if (n >= 12) perStep = 9
      this.score = Math.max(20, base - this.steps * perStep - this.seconds * 3 - this.hintsUsed * 25)
      this.emitScore()
      this.$emit('over', this.score)
      sound.success()
    },
    emitScore() { this.$emit('score', this.score) }
  }
}
</script>

<style scoped>
.pz-wrap { display: flex; flex-direction: column; gap: 12px; align-items: center; }
.pz-hud { display: flex; gap: 8px; width: 100%; }
.hud-chip {
  flex: 1; min-width: 0; padding: 8px 10px; border-radius: 14px;
  background: var(--panel); border: 1px solid var(--border);
  display: flex; flex-direction: column; gap: 2px; text-align: center;
}
.hud-chip span { font-size: 10.5px; color: var(--muted); }
.hud-chip b { font-size: 15px; color: var(--primary); }
.pz-levels { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; justify-content: center; }
.lv-label { font-size: 11.5px; color: var(--muted); font-weight: 700; }
.lv-btn {
  padding: 5px 9px; border-radius: 999px; cursor: pointer;
  border: 1.5px solid var(--border); background: var(--panel);
  color: var(--text); font-size: 11px; font-weight: 700;
  transition: background .16s, border-color .16s, color .16s;
}
.lv-btn:hover:not(:disabled) { border-color: var(--primary); }
.lv-btn.on {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff; border-color: transparent;
}
.lv-btn:disabled { opacity: .5; cursor: default; }
.pz-body { width: 100%; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.pz-goal { display: flex; align-items: flex-start; gap: 10px; flex-wrap: wrap; justify-content: center; }
.goal-label { font-size: 11.5px; color: var(--muted); font-weight: 700; line-height: 28px; }
.pz-mini {
  display: grid; gap: 3px;
  padding: 5px; border-radius: 10px; background: var(--panel); border: 1px solid var(--border);
}
.pz-mini span {
  width: var(--mc, 22px); height: var(--mc, 22px); border-radius: 5px;
  font-size: var(--mfs, 11px);
  display: flex; align-items: center; justify-content: center;
}
.pz-board {
  display: grid; gap: 8px;
  width: 100%; max-width: 760px;
}
.pz-tile {
  aspect-ratio: 1; border: 2px solid rgba(255,255,255,.75); border-radius: 14px;
  font-size: 30px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 5px 12px rgba(90,60,120,.16);
  transition: transform .16s ease, box-shadow .16s ease;
  -webkit-tap-highlight-color: transparent;
  min-width: 0; min-height: 0;
}
.pz-tile:hover { transform: translateY(-2px); }
.pz-tile.sel {
  transform: scale(1.06);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--primary) 55%, transparent), 0 8px 18px rgba(90,60,120,.24);
}
.pz-tile.ok { outline: 2px solid color-mix(in srgb, var(--success) 60%, transparent); outline-offset: -2px; }
.pz-move { transition: transform .26s cubic-bezier(.22,1,.36,1); }
.pz-tip { margin: 0; font-size: 12px; color: var(--muted); }
.pz-btns { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.pz-mini-btn {
  padding: 8px 14px; border-radius: 11px; border: 1px solid var(--border);
  background: var(--panel); color: var(--primary); font-size: 12px; font-weight: 700; cursor: pointer;
}
.pz-mini-btn:disabled { opacity: .45; cursor: default; }
.pz-mini-btn small { color: var(--muted); font-weight: 400; }
.pz-done { display: flex; flex-direction: column; align-items: center; gap: 8px; text-align: center; padding: 8px 0; }
.pz-mascot { font-size: 52px; animation: pzPop .5s cubic-bezier(.22,1.4,.4,1); }
.pz-done h3 { margin: 0; font-size: 18px; color: var(--primary); }
.pz-done p { margin: 0; font-size: 12.5px; color: var(--muted); }
.pz-done p b { color: var(--primary); }
.pz-score { font-size: 30px; font-weight: 800; color: var(--primary); margin: 2px 0; }
.pz-btn {
  border: none; border-radius: 14px; padding: 11px 28px; cursor: pointer;
  color: #fff; font-weight: 800; font-size: 14.5px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 8px 20px color-mix(in srgb, var(--purple) 40%, transparent);
}
@keyframes pzPop { 0% { transform: scale(.3); } 100% { transform: scale(1); } }
@media (max-width: 380px) { .pz-tile { border-radius: 9px; } }
@media (prefers-reduced-motion: reduce) { .pz-tile, .pz-move, .pz-mascot { transition: none; animation: none; } }
</style>
