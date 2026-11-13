<template>
  <!-- 掷骰时浮在棋盘上方的大骰子：翻滚 → 定格点数 → 数秒后淡出 -->
  <div v-if="!mini" class="d3-overlay" :class="{ on: show }" aria-hidden="true">
    <div class="d3-glow"></div>
    <div class="d3-scene">
      <div class="d3-cube" :class="{ tumbling: rolling }" :style="cubeStyle">
        <div
          v-for="f in faces"
          :key="f.key"
          class="d3-face"
          :style="{ transform: f.transform }"
        >
          <i v-for="(d, i) in dotsFor(f.n)" :key="i" class="d3-dot" :style="d"></i>
        </div>
      </div>
    </div>
    <div class="d3-caption" :class="{ on: show && !rolling && value }">
      🎲 掷出 <b>{{ value }}</b> 点
    </div>
  </div>

  <!-- 常驻小骰子面：替换原来只显示数字的小方块 -->
  <div v-else class="d3-mini" :class="{ shaking: rolling }">
    <i v-for="(d, i) in dotsFor(value)" :key="i" class="d3-dot" :style="d"></i>
    <span v-if="!value" class="d3-mini-emoji">🎲</span>
  </div>
</template>

<script>
/**
 * 🎲 Q 版 3D 骰子（纯 CSS，零依赖、无需 WebGL）
 * - 默认：掷骰时浮在棋盘上方的立方体骰子，六个面都有点数；正面点数 = 实际掷出的点数，保证所见即所得。
 * - mini：常驻的小骰子面，用于棋盘中央状态区。
 */
const POS = (c, r) => ({ gridColumn: c, gridRow: r })
const LAYOUT = {
  0: [],
  1: [POS(2, 2)],
  2: [POS(1, 1), POS(3, 3)],
  3: [POS(1, 1), POS(2, 2), POS(3, 3)],
  4: [POS(1, 1), POS(3, 1), POS(1, 3), POS(3, 3)],
  5: [POS(1, 1), POS(3, 1), POS(2, 2), POS(1, 3), POS(3, 3)],
  6: [POS(1, 1), POS(3, 1), POS(1, 2), POS(3, 2), POS(1, 3), POS(3, 3)]
}
// 立方体边长 80px → 半边长 40px
const HALF = 40
const SIDES = [
  { key: 'front', n: 0, transform: `translateZ(${HALF}px)` },
  { key: 'back', n: 6, transform: `rotateY(180deg) translateZ(${HALF}px)` },
  { key: 'right', n: 2, transform: `rotateY(90deg) translateZ(${HALF}px)` },
  { key: 'left', n: 5, transform: `rotateY(-90deg) translateZ(${HALF}px)` },
  { key: 'top', n: 3, transform: `rotateX(90deg) translateZ(${HALF}px)` },
  { key: 'bottom', n: 4, transform: `rotateX(-90deg) translateZ(${HALF}px)` }
]

export default {
  name: 'Dice3D',
  props: {
    show: { type: Boolean, default: false },
    rolling: { type: Boolean, default: false },
    value: { type: Number, default: 0 },
    mini: { type: Boolean, default: false }
  },
  computed: {
    faces() {
      // 正面点数跟着实际结果走，其余五面是固定装饰，怎么看都是一只真骰子
      return SIDES.map(s => ({ ...s, n: s.key === 'front' ? (this.value || 1) : s.n }))
    },
    cubeStyle() {
      // 翻滚交给 CSS animation；定格时轻微倾斜，让正面朝观众、又能看到侧面
      return this.rolling ? {} : { transform: 'rotateX(-16deg) rotateY(20deg)' }
    }
  },
  methods: {
    dotsFor(n) { return LAYOUT[n] || [] }
  }
}
</script>

<style scoped>
/* ---------- 浮在棋盘上方的大骰子 ---------- */
.d3-overlay {
  position: absolute; inset: 0; z-index: 6;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 14px; padding-bottom: 8%; box-sizing: border-box; pointer-events: none;
  opacity: 0; visibility: hidden;
  transition: opacity .25s ease, visibility .25s ease;
}
.d3-overlay.on { opacity: 1; visibility: visible; }
/* 白色实心核心挡住棋盘中央的文字，边缘渐隐成粉色光晕，像聚光灯下的骰子 */
.d3-glow {
  position: absolute; width: 230px; height: 230px; border-radius: 50%;
  background: radial-gradient(circle,
    rgba(255, 255, 255, .97) 0%, rgba(255, 255, 255, .93) 35%,
    rgba(255, 214, 238, .58) 58%, rgba(255, 214, 238, 0) 73%);
}
.d3-scene { position: relative; z-index: 1; width: 80px; height: 80px; perspective: 520px; }
.d3-cube {
  width: 100%; height: 100%; position: relative;
  transform-style: preserve-3d;
  transition: transform .6s cubic-bezier(.2, .85, .25, 1);
}
.d3-cube.tumbling {
  animation: d3-tumble .55s linear infinite;
  transition: none;
}
@keyframes d3-tumble {
  from { transform: rotateX(0deg) rotateY(0deg) rotateZ(0deg); }
  to   { transform: rotateX(360deg) rotateY(360deg) rotateZ(180deg); }
}
.d3-face {
  position: absolute; inset: 0; box-sizing: border-box;
  border-radius: 15px; padding: 11px;
  background: linear-gradient(150deg, #ffffff, #fff2fa);
  border: 2px solid #f3cfe6;
  box-shadow: inset 0 0 14px rgba(214, 150, 196, .18), 0 3px 10px rgba(160, 108, 213, .18);
  backface-visibility: hidden;
  display: grid;
  grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr);
}
.d3-dot {
  width: 11px; height: 11px; border-radius: 50%;
  align-self: center; justify-self: center;
  background: radial-gradient(circle at 35% 30%, #ff9dc6, #d8569e);
  box-shadow: inset 0 -1px 2px rgba(150, 40, 100, .35);
}
.d3-caption {
  position: relative; z-index: 1;
  font-size: 13px; font-weight: 800; color: var(--primary);
  background: rgba(255, 255, 255, .92); border: 1.5px solid var(--border);
  border-radius: 999px; padding: 5px 14px;
  box-shadow: 0 4px 12px rgba(160, 108, 213, .2);
  opacity: 0; transform: translateY(6px);
  transition: opacity .25s ease, transform .25s ease;
}
.d3-caption.on { opacity: 1; transform: none; }
.d3-caption b { font-size: 16px; color: var(--pink); }

/* ---------- 常驻小骰子面 ---------- */
.d3-mini {
  position: relative; width: 48px; height: 48px; flex-shrink: 0;
  box-sizing: border-box; padding: 7px; border-radius: 13px;
  background: var(--card); border: 2px solid var(--border);
  box-shadow: 0 4px 10px rgba(160, 108, 213, .18);
  display: grid;
  grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr);
}
.d3-mini .d3-dot { width: 8px; height: 8px; }
.d3-mini.shaking { animation: d3-shake .5s ease-in-out infinite alternate; }
@keyframes d3-shake { to { transform: rotate(7deg) scale(.94); } }
.d3-mini-emoji {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}

@media (prefers-reduced-motion: reduce) {
  .d3-cube.tumbling, .d3-mini.shaking { animation: none; }
}
</style>
