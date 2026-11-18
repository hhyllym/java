<template>
  <div class="pz-wrap">
    <div class="pz-hud">
      <div class="hud-chip"><span>🧩 步数</span><b>{{ steps }}</b></div>
      <div class="hud-chip"><span>⏱️ 用时</span><b>{{ seconds }}s</b></div>
      <div class="hud-chip"><span>💡 提示</span><b>{{ 2 - hintsLeft > 0 ? 2 - hintsLeft + ' 次' : '用完啦' }}</b></div>
    </div>

    <div v-if="!done" class="pz-body">
      <div class="pz-goal">
        <span class="goal-label">✨ 目标图案</span>
        <div class="pz-mini">
          <span v-for="t in GOAL" :key="t.id" :style="{ background: t.color }">{{ t.emoji }}</span>
        </div>
      </div>

      <TransitionGroup name="pz" tag="div" class="pz-board">
        <button
          v-for="(t, i) in tiles"
          :key="t.id"
          class="pz-tile"
          :class="{ sel: selected === i, ok: t.id === i }"
          :style="{ background: t.color }"
          @click="tap(i)"
        >
          <span>{{ t.emoji }}</span>
        </button>
      </TransitionGroup>

      <p class="pz-tip">点两块交换位置，把心印碎片拼回目标图 ✨</p>
      <div class="pz-btns">
        <button class="pz-mini-btn" :disabled="hintsLeft <= 0" @click="hint">💡 提示 <small>-25 分</small></button>
        <button class="pz-mini-btn" @click="restart">🔀 重新洗牌</button>
      </div>
    </div>

    <div v-else class="pz-done">
      <div class="pz-mascot">🎉</div>
      <h3>心印拼好啦！</h3>
      <p>用了 <b>{{ steps }}</b> 步 · <b>{{ seconds }}</b> 秒 · 提示 <b>{{ hintsUsed }}</b> 次</p>
      <div class="pz-score">+{{ score }} 分</div>
      <button class="pz-btn" @click="restart">🔁 再拼一次</button>
    </div>
  </div>
</template>

<script>
import sound from '../../sound'

/** 九枚原创「星糖谷心印碎片」：颜色固定，任何主题下都好辨认 */
const GOAL = [
  { id: 0, emoji: '🌈', color: '#f7a8c4' },
  { id: 1, emoji: '⭐', color: '#f7c08a' },
  { id: 2, emoji: '🍬', color: '#f7e08a' },
  { id: 3, emoji: '☁️', color: '#a7e3c9' },
  { id: 4, emoji: '🦄', color: '#8fc7f0' },
  { id: 5, emoji: '🍎', color: '#b79cf0' },
  { id: 6, emoji: '🦋', color: '#e08ac0' },
  { id: 7, emoji: '💎', color: '#a8d8f7' },
  { id: 8, emoji: '🧁', color: '#f5b7d0' }
]

export default {
  name: 'GamePuzzle',
  emits: ['score', 'over'],
  data() {
    return {
      GOAL,
      tiles: [],
      selected: -1,
      steps: 0,
      seconds: 0,
      hintsLeft: 2,
      hintsUsed: 0,
      done: false,
      score: 0,
      timer: null,
      alive: true
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
      const arr = GOAL.slice()
      // Fisher-Yates，保证至少 6 块错位，别一上来就快拼好了
      let guard = 0
      do {
        for (let i = arr.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[arr[i], arr[j]] = [arr[j], arr[i]]
        }
        guard += 1
      } while (guard < 40 && arr.filter((t, i) => t.id !== i).length < 6)
      return arr
    },
    restart() {
      this.tiles = this.shuffle()
      this.selected = -1
      this.steps = 0
      this.seconds = 0
      this.hintsLeft = 2
      this.hintsUsed = 0
      this.done = false
      this.score = 0
      this.emitScore()
      sound.click()
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
      const a = this.selected
      ;[this.tiles[a], this.tiles[i]] = [this.tiles[i], this.tiles[a]]
      this.selected = -1
      this.steps += 1
      sound.pop()
      if (this.tiles.every((t, idx) => t.id === idx)) this.finish()
    },
    hint() {
      if (this.done || this.hintsLeft <= 0) return
      const i = this.tiles.findIndex((t, idx) => t.id !== idx)
      if (i < 0) return
      const j = this.tiles.findIndex(t => t.id === i)
      ;[this.tiles[i], this.tiles[j]] = [this.tiles[j], this.tiles[i]]
      this.hintsLeft -= 1
      this.hintsUsed += 1
      this.steps += 1
      this.selected = -1
      sound.magic()
      if (this.tiles.every((t, idx) => t.id === idx)) this.finish()
    },
    finish() {
      this.done = true
      this.score = Math.max(20, 300 - this.steps * 8 - this.seconds * 3 - this.hintsUsed * 25)
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
.pz-body { width: 100%; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.pz-goal { display: flex; align-items: center; gap: 10px; }
.goal-label { font-size: 11.5px; color: var(--muted); font-weight: 700; }
.pz-mini {
  display: grid; grid-template-columns: repeat(3, 22px); gap: 3px;
  padding: 5px; border-radius: 10px; background: var(--panel); border: 1px solid var(--border);
}
.pz-mini span {
  width: 22px; height: 22px; border-radius: 6px; font-size: 11px;
  display: flex; align-items: center; justify-content: center;
}
.pz-board {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
  width: 100%; max-width: 300px;
}
.pz-tile {
  aspect-ratio: 1; border: 2px solid rgba(255,255,255,.75); border-radius: 16px;
  font-size: 30px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 5px 12px rgba(90,60,120,.16);
  transition: transform .16s ease, box-shadow .16s ease;
  -webkit-tap-highlight-color: transparent;
}
.pz-tile:hover { transform: translateY(-2px); }
.pz-tile.sel {
  transform: scale(1.06);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--primary) 55%, transparent), 0 8px 18px rgba(90,60,120,.24);
}
.pz-tile.ok { outline: 2px solid color-mix(in srgb, var(--success) 60%, transparent); outline-offset: -2px; }
.pz-move { transition: transform .26s cubic-bezier(.22,1,.36,1); }
.pz-tip { margin: 0; font-size: 12px; color: var(--muted); }
.pz-btns { display: flex; gap: 8px; }
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
@media (max-width: 380px) { .pz-tile { font-size: 25px; border-radius: 13px; } }
@media (prefers-reduced-motion: reduce) { .pz-tile, .pz-move, .pz-mascot { transition: none; animation: none; } }
</style>
