<template>
  <div class="game-dash">
    <!-- 进度 -->
    <div class="dash-hud">
      <span>第 {{ idx + 1 }} / {{ total }} 轮</span>
      <span v-if="results.length" class="dash-avg">平均 {{ avg }} ms</span>
    </div>

    <!-- 反应区（整个场景都可以点） -->
    <div
      class="dash-stage"
      :class="state"
      @pointerdown="tap"
    >
      <!-- 天空背景 -->
      <div class="sky">
        <div class="sun"></div>
        <div class="cloud c1"></div>
        <div class="cloud c2"></div>
        <div class="cloud c3"></div>
      </div>

      <!-- 文案层（盖在天空上） -->
      <div class="stage-text">
        <template v-if="state === 'idle'">
          <p class="dash-title">⚡ 闪电反应</p>
          <p class="dash-sub">天空变<b class="go">绿</b>的瞬间，帮云驰第一个冲线！</p>
          <p class="dash-sub small">提前点击算抢跑，本轮回炉重来</p>
          <button class="dash-btn" @pointerdown.stop="begin">开始第一轮</button>
        </template>

        <template v-else-if="state === 'ready'">
          <p class="dash-title">各就各位…</p>
          <p class="dash-sub">别急，绿了再点！</p>
        </template>

        <template v-else-if="state === 'go'">
          <p class="dash-title go">就是现在，点！</p>
        </template>

        <template v-else-if="state === 'result'">
          <p class="dash-title">{{ lastMs }}<small class="ms">ms</small></p>
          <p class="dash-sub">{{ rating }}</p>
          <button class="dash-btn" @pointerdown.stop="nextRound">
            {{ idx + 1 >= total ? '查看成绩' : '下一轮' }}
          </button>
        </template>

        <template v-else-if="state === 'foul'">
          <p class="dash-title foul-t">抢跑啦！</p>
          <p class="dash-sub">云驰被吹回起跑线，这一轮不算</p>
          <button class="dash-btn" @pointerdown.stop="retry">重试本轮</button>
        </template>
      </div>

      <!-- 场地：草地跑道 -->
      <div class="ground">
        <div class="flower f1">🌼</div>
        <div class="flower f2">🌸</div>
        <div class="flower f3">🌼</div>
        <div class="lane">
          <div class="start-line"></div>
          <div class="flag">🏁</div>
          <!-- 彩虹尾迹（冲刺时出现） -->
          <div class="trail"></div>
          <!-- 云驰 -->
          <div class="pony" :class="state">
            <svg class="pony-svg" viewBox="0 0 150 110" aria-hidden="true">
              <!-- 尾巴：彩虹飘带 -->
              <path d="M30 50 Q10 46 5 36 Q16 40 26 38 Z" fill="#ff7b9c"/>
              <path d="M30 57 Q8 55 3 47 Q15 49 26 47 Z" fill="#ffb56b"/>
              <path d="M30 64 Q8 66 4 58 Q16 58 26 56 Z" fill="#ffe066"/>
              <path d="M30 71 Q11 75 7 68 Q18 64 27 62 Z" fill="#8fdc9f"/>
              <!-- 后腿 -->
              <rect x="44" y="70" width="9" height="28" rx="4.5" fill="#57b8e8"/>
              <rect x="58" y="73" width="9" height="25" rx="4.5" fill="#6fc9f5"/>
              <!-- 身体 -->
              <ellipse cx="72" cy="57" rx="30" ry="19" fill="#6fc9f5"/>
              <!-- 前腿 -->
              <rect x="80" y="69" width="9" height="29" rx="4.5" fill="#57b8e8"/>
              <rect x="92" y="71" width="9" height="27" rx="4.5" fill="#6fc9f5"/>
              <!-- 翅膀 -->
              <path class="wing" d="M64 40 Q76 16 98 20 Q85 27 81 42 Z" fill="#dcf1ff" stroke="#a8d8f0" stroke-width="1.5"/>
              <!-- 鬃毛：彩虹三连 -->
              <path d="M84 29 Q100 16 120 22 L116 33 Q101 27 88 35 Z" fill="#ff7b9c"/>
              <path d="M86 36 Q103 26 122 31 L118 40 Q103 34 90 42 Z" fill="#ffb56b"/>
              <path d="M88 43 Q105 35 124 40 L120 48 Q105 42 92 49 Z" fill="#ffe066"/>
              <!-- 头 -->
              <circle cx="108" cy="40" r="17" fill="#6fc9f5"/>
              <!-- 耳朵 -->
              <path d="M99 25 L103 13 L110 23 Z" fill="#6fc9f5"/>
              <!-- 前发帘 -->
              <path d="M95 28 Q106 19 121 25 Q112 30 103 34 Z" fill="#ff7b9c"/>
              <!-- 眼睛 -->
              <circle cx="114" cy="38" r="4.6" fill="#fff"/>
              <circle cx="115.6" cy="38.4" r="2.4" fill="#2b3a4a"/>
              <circle cx="116.6" cy="36.8" r="0.9" fill="#fff"/>
              <!-- 嘴 -->
              <path d="M119 46 q3 2.2 5.5 0" stroke="#3a6b8a" stroke-width="1.6" fill="none" stroke-linecap="round"/>
              <!-- 心印：彗星穿过流云 -->
              <g transform="translate(64,52)">
                <ellipse cx="0" cy="4" rx="7.5" ry="3.6" fill="#fff"/>
                <path d="M-7 -4 Q-2 -1 3 -1" stroke="#9fd4ef" stroke-width="1.6" fill="none" stroke-linecap="round"/>
                <circle cx="4" cy="-1.5" r="2.6" fill="#ffe9a8"/>
              </g>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <!-- 每轮成绩 -->
    <div v-if="results.length" class="dash-list">
      <span v-for="(r, i) in results" :key="i" class="dash-chip" :class="{ foul: r === 0 }">
        第{{ i + 1 }}轮 {{ r === 0 ? '抢跑' : r + 'ms' }}
      </span>
    </div>
  </div>
</template>

<script>
const TOTAL = 5

export default {
  name: 'GameReflex',
  emits: ['score', 'over'],
  data() {
    return {
      state: 'idle',     // idle | ready | go | result | foul
      idx: 0,
      total: TOTAL,
      results: [],
      lastMs: 0,
      goAt: 0,
      waitTimer: null
    }
  },
  computed: {
    avg() {
      const valid = this.results.filter(r => r > 0)
      if (!valid.length) return 0
      return Math.round(valid.reduce((s, r) => s + r, 0) / valid.length)
    },
    rating() {
      const a = this.avg
      if (a === 0) return '再试一次！'
      if (a < 250) return '风一样快！🦄'
      if (a < 350) return '极速飞行！⚡'
      if (a < 500) return '相当快！💨'
      return '继续加油！🐴'
    },
    tip() {
      const a = this.avg
      if (a === 0) return ''
      if (a < 250) return '这反应速度，云驰本人！'
      if (a < 350) return '职业选手的水平了！'
      if (a < 500) return '比大多数人快，多练几轮还能提升'
      return '试试盯住屏幕中央，别分心～'
    }
  },
  beforeUnmount() {
    clearTimeout(this.waitTimer)
  },
  methods: {
    begin() {
      this.idx = 0
      this.results = []
      this.readyRound()
    },
    readyRound() {
      this.state = 'ready'
      this.waitTimer = setTimeout(() => {
        this.state = 'go'
        this.goAt = performance.now()
      }, 800 + Math.random() * 2200)
    },
    retry() {
      this.readyRound()
    },
    nextRound() {
      if (this.idx + 1 >= this.total) {
        this.finish()
      } else {
        this.idx += 1
        this.readyRound()
      }
    },
    tap() {
      if (this.state === 'ready') {
        // 抢跑
        clearTimeout(this.waitTimer)
        this.state = 'foul'
        this.results[this.idx] = 0
      } else if (this.state === 'go') {
        const ms = Math.round(performance.now() - this.goAt)
        this.lastMs = ms
        this.results[this.idx] = ms
        this.state = 'result'
      }
    },
    finish() {
      // 计分：平均反应越快分越高；抢跑按 0 分处理
      const valid = this.results.filter(r => r > 0)
      let score = 0
      if (valid.length) {
        const a = valid.reduce((s, r) => s + r, 0) / valid.length
        score = Math.max(0, Math.round(650 - a))
      }
      this.$emit('over', score)
    }
  }
}
</script>

<style scoped>
.dash-hud {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.dash-avg { color: var(--primary); font-weight: 700; }

/* ---------- 场景舞台 ---------- */
.dash-stage {
  position: relative; overflow: hidden;
  border-radius: 16px;
  text-align: center; min-height: 320px;
  cursor: pointer; user-select: none; touch-action: manipulation;
  background: #cfeeff;               /* 天空基色，随状态变色 */
  border: 2px solid var(--border);
  transition: background .15s;
  display: block;
}
.dash-stage.ready { background: #ffdcdc; border-color: #f0b0b0; }
.dash-stage.go    { background: #c2efd6; border-color: #58b98a; }

/* ---------- 天空：太阳 + 云朵 ---------- */
.sky { position: absolute; inset: 0 0 30% 0; pointer-events: none; }
.sun {
  position: absolute; top: 14px; right: 18px;
  width: 34px; height: 34px; border-radius: 50%;
  background: #ffdf6b;
  box-shadow: 0 0 0 8px rgba(255, 223, 107, .35), 0 0 0 16px rgba(255, 223, 107, .15);
}
.cloud {
  position: absolute; height: 16px; border-radius: 999px;
  background: rgba(255,255,255,.92);
  box-shadow: 14px -7px 0 -2px rgba(255,255,255,.92), -12px -5px 0 -3px rgba(255,255,255,.92);
}
.cloud.c1 { width: 56px; top: 22px; left: 8%;  animation: drift 9s linear infinite; }
.cloud.c2 { width: 42px; top: 52px; left: 55%; animation: drift 13s linear infinite reverse; }
.cloud.c3 { width: 34px; top: 90px; left: 30%; animation: drift 17s linear infinite; opacity: .8; }
@keyframes drift {
  from { transform: translateX(-30px); }
  50%  { transform: translateX(30px); }
  to   { transform: translateX(-30px); }
}

/* ---------- 文案层 ---------- */
.stage-text {
  position: relative; z-index: 5;
  padding: 22px 16px 0; pointer-events: none;
  min-height: 150px;
  display: flex; flex-direction: column; align-items: center; justify-content: flex-start;
}
.dash-title { margin: 0; font-size: 19px; font-weight: 800; color: #2b4a63; }
.dash-title.go { color: #1f6b45; animation: pop .25s ease; }
.dash-title.foul-t { color: #b3424c; }
.dash-title .ms { font-size: 12px; margin-left: 2px; }
.dash-sub { margin: 4px 0 0; font-size: 12.5px; color: var(--muted); line-height: 1.6; }
.dash-sub.small { font-size: 11px; opacity: .75; }
.dash-sub .go { color: var(--success); }

.dash-btn {
  pointer-events: auto;
  margin-top: 14px; border: none; cursor: pointer;
  padding: 10px 26px; border-radius: 999px;
  color: #fff; font-size: 14px; font-weight: 700;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160,108,213,.3);
}
@keyframes pop { from { transform: scale(.4); } }

/* ---------- 场地：草地跑道 ---------- */
.ground {
  position: absolute; left: 0; right: 0; bottom: 0;
  height: 118px;
  background: linear-gradient(#a8e096 0, #8ccf7a 100%);
  border-top: 3px solid #7fbf6a;
}
.flower { position: absolute; font-size: 14px; bottom: 6px; opacity: .9; }
.flower.f1 { left: 4%; }
.flower.f2 { left: 48%; bottom: 2px; }
.flower.f3 { right: 4%; }

.lane { position: absolute; inset: 0; }
.start-line {
  position: absolute; left: 16%; top: 8px; bottom: 8px;
  width: 0; border-left: 3px dashed rgba(255,255,255,.85);
}
.flag { position: absolute; right: 5%; top: 6px; font-size: 22px; }

/* 彩虹尾迹 */
.trail {
  position: absolute; left: 14%; bottom: 52px;
  width: 0; height: 14px; border-radius: 999px;
  background: linear-gradient(90deg,
    #ff7b9c, #ffb56b, #ffe066, #8fdc9f, #7ec8f0, #b493e6);
  opacity: 0; transition: opacity .2s;
}
.dash-stage.go .trail { opacity: .85; width: 55%; transition: width .8s cubic-bezier(.2,.7,.3,1), opacity .2s; }

/* ---------- 云驰 ---------- */
.pony {
  position: absolute; left: 12%; bottom: 30px;
  width: 96px; height: 70px;
  transition: left .8s cubic-bezier(.2,.7,.3,1);
  z-index: 3;
}
.pony-svg { width: 100%; height: 100%; display: block; }
.pony.idle .pony-svg { animation: bob 2.4s ease-in-out infinite; }
.pony.ready .pony-svg { animation: tremble .3s linear infinite; transform-origin: 50% 90%; }
.pony.go    { left: 68%; }
.pony.result .pony-svg { animation: hop .45s ease 2; transform-origin: 50% 100%; }
.pony.foul  { left: 12%; }
.pony.foul .pony-svg { animation: dizzy .55s ease; }
@keyframes bob     { 50% { transform: translateY(-4px); } }
@keyframes tremble { 25% { transform: rotate(-3deg); } 75% { transform: rotate(3deg); } }
@keyframes hop     { 40% { transform: translateY(-14px) rotate(-6deg); } }
@keyframes dizzy   { 25% { transform: rotate(-14deg) translateY(-6px); } 75% { transform: rotate(14deg); } }

/* 翅膀扇动：冲刺时加速 */
.pony.go .wing { animation: flap .12s linear infinite; transform-origin: 70% 60%; }
@keyframes flap { 50% { transform: rotate(-14deg); } }

/* ---------- 成绩列表 ---------- */
.dash-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.dash-chip {
  font-size: 11px; padding: 4px 10px; border-radius: 999px;
  background: var(--panel); color: var(--primary); font-weight: 700;
}
.dash-chip.foul { color: var(--danger); }
</style>
