<template>
  <div class="jp-wrap">
    <!-- 准备页 -->
    <div v-if="stage === 'ready'" class="jp-ready">
      <div class="jp-mascot">🦄</div>
      <h3>小马跳跳跑</h3>
      <p class="jp-sub">
        按 <b>空格 / ↑ / W</b> 或点一下场地让小马起跳，跨过 🌵🪵🍄🪨 这些障碍～<br>
        吃 ⭐ 加分，撞到掉一颗心；开局 8 秒慢慢加速，这 8 秒里撞到不掉心。
      </p>

      <div class="jp-diffs">
        <button
          v-for="d in DIFF_LIST"
          :key="d.key"
          class="jp-diff"
          :class="{ on: diff === d.key }"
          @click="pickDiff(d.key)"
        >
          <span class="d-emoji">{{ d.emoji }}</span>
          <b>{{ d.name }}</b>
          <small>{{ d.desc }}</small>
        </button>
      </div>

      <div class="jp-models">
        <span class="jp-model-label">🎨 主角建模</span>
        <div class="jp-model-row">
          <button
            v-for="m in MODEL_LIST"
            :key="m.key"
            class="jp-model"
            :class="{ on: model === m.key }"
            @click="pickModel(m.key)"
          >
            <span>{{ m.emoji }}</span>
            <b>{{ m.name }}</b>
            <small>{{ m.desc }}</small>
          </button>
        </div>
        <em class="jp-model-note">3D 建模为 Three.js 实时渲染；设备不支持 WebGL 会自动用经典造型</em>
      </div>

      <div class="jp-how">
        <span>⬆️ 空格 / 点击起跳</span>
        <span>⭐ 吃星 +15</span>
        <span>🏁 坚持 40 秒</span>
      </div>
      <button class="jp-btn" @click="start">出发！✨</button>
    </div>

    <!-- 游戏中 / 结算 -->
    <template v-else>
      <div class="jp-hud">
        <div class="hud-chip"><span>❤️ 生命</span><b>{{ heartText }}</b></div>
        <div class="hud-chip"><span>⏱️ 剩余</span><b>{{ Math.max(0, Math.ceil(cfg.duration - time)) }}s</b></div>
        <div class="hud-chip"><span>⭐ 星星</span><b>{{ starCount }}</b></div>
        <div class="hud-chip"><span>🏅 得分</span><b>{{ curScore }}</b></div>
      </div>

      <div
        class="jp-field"
        :class="{ shake: shake, warm: time < 8 }"
        @click="jump"
        @mousedown.prevent="jump"
        @touchstart.prevent="jump"
      >
        <div class="jp-sky-glow"></div>
        <div class="jp-cloud c1">☁️</div>
        <div class="jp-cloud c2">☁️</div>
        <div class="jp-cloud c3">☁️</div>

        <div class="jp-live-tip">
          {{ time < 8 ? '🌤 热身中 · 慢慢跑，撞到不掉心' : (inv > 0 ? '✨ 短暂无敌' : '⬆️ 点一下 / 空格起跳') }}
        </div>

        <!-- 小马：3D 建模主角（WebGL 不可用时回退 emoji） -->
        <div class="jp-pony" :style="{ bottom: (16 + py) + '%' }">
          <canvas v-if="model !== 'classic' && model3d" ref="cv" class="jp-pony-canvas"></canvas>
          <span v-else class="jp-pony-emoji" :class="{ jumping: py > 0.5 }">🦄</span>
          <span v-if="model !== 'classic' && model3d" class="jp-pony-tag">{{ model === 'star' ? '星澜' : '云驰' }}</span>
        </div>

        <!-- 障碍 -->
        <div
          v-for="o in obstacles"
          :key="'o' + o.id"
          class="jp-obstacle"
          :class="{ hit: o.hit }"
          :style="{ left: o.x + '%' }"
        >{{ o.type }}</div>

        <!-- 星星 -->
        <div
          v-for="s in stars"
          :key="'s' + s.id"
          class="jp-star"
          :style="{ left: s.x + '%', bottom: (16 + s.y) + '%' }"
        >⭐</div>

        <div class="jp-ground"></div>
        <div class="jp-ground-line"></div>
      </div>

      <div class="jp-mobile">
        <button class="jp-jump-btn" @click="jump">⬆️ 跳！</button>
        <span>躲过 {{ dodged }} 个障碍 · 难度 {{ cfg.name }} ×{{ cfg.mul }} · {{ modelName }}</span>
      </div>

      <div v-if="stage === 'over'" class="jp-result" :class="result">
        <div class="r-emoji">{{ result === 'win' ? '🏆' : '💫' }}</div>
        <h3>{{ result === 'win' ? '跑完全程啦！' : '小马累啦～' }}</h3>
        <p>
          {{ result === 'win'
            ? `坚持 40 秒 · 躲过 ${dodged} 个障碍 · 吃到 ${starCount} 颗星`
            : `躲过 ${dodged} 个障碍 · 吃到 ${starCount} 颗星，再来一局？` }}
        </p>
        <div class="r-score">+{{ score }} 分</div>
        <div class="r-btns">
          <button class="jp-btn" @click="restart">🔁 再来一局</button>
          <button class="jp-ghost" @click="backToReady">换个难度 / 建模</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import sound from '../../sound'

/** 全面调慢：速度、障碍密度、提速幅度都降下来 */
const DIFFS = {
  easy:   { key: 'easy',   name: '轻松', emoji: '🌱', base: 0.46, step: 0.005, gap: 60, mul: 1,   hearts: 4, duration: 40, desc: '慢慢跑 · 4 颗心' },
  normal: { key: 'normal', name: '普通', emoji: '🌈', base: 0.56, step: 0.007, gap: 52, mul: 1.3, hearts: 3, duration: 40, desc: '悠闲节奏 · 3 颗心 · ×1.3' },
  hard:   { key: 'hard',   name: '挑战', emoji: '🔥', base: 0.68, step: 0.009, gap: 44, mul: 1.6, hearts: 3, duration: 40, desc: '稍快一点 · ×1.6' }
}
const DIFF_LIST = [DIFFS.easy, DIFFS.normal, DIFFS.hard]
const DIFF_KEY = 'mlp_jump_diff'

const MODEL_LIST = [
  { key: 'cloud',   name: '云驰', emoji: '🌈', desc: '彩虹鬃毛 · 3D' },
  { key: 'star',    name: '星澜', emoji: '💫', desc: '星翼小马 · 3D' },
  { key: 'classic', name: '经典', emoji: '🦄', desc: '原版 emoji' }
]
const MODEL_KEY = 'mlp_jump_model'
const TYPES = ['🌵', '🪵', '🍄', '🪨']
const WARM = 8          // 热身 / 保护期（秒）

let ponyMod = null      // three.js 只在第一次开局时动态加载

function readDiff() {
  try {
    const k = localStorage.getItem(DIFF_KEY)
    return DIFFS[k] ? k : 'normal'
  } catch (e) { return 'normal' }
}
function readModel() {
  try {
    const k = localStorage.getItem(MODEL_KEY)
    return MODEL_LIST.some(m => m.key === k) ? k : 'cloud'
  } catch (e) { return 'cloud' }
}

export default {
  name: 'GameJump',
  emits: ['score', 'over'],
  data() {
    return {
      DIFF_LIST,
      MODEL_LIST,
      diff: readDiff(),
      model: readModel(),
      model3d: true,          // WebGL 是否可用
      stage: 'ready',         // ready | playing | over
      result: 'win',
      py: 0,                  // 离地高度（% of field）
      vy: 0,
      time: 0,
      hearts: 3,
      inv: 0,
      dodged: 0,
      starCount: 0,
      score: 0,
      obstacles: [],
      stars: [],
      dist: 0,
      nextGap: 0,
      oid: 0,
      sid: 0,
      shake: false,
      raf: null,
      last: 0
    }
  },
  computed: {
    cfg() { return DIFFS[this.diff] },
    heartText() { return '❤️'.repeat(Math.max(0, this.hearts)) || '—' },
    modelName() { return (MODEL_LIST.find(m => m.key === this.model) || MODEL_LIST[0]).name },
    curScore() {
      return Math.round((this.dodged * 10 + this.starCount * 15 + Math.floor(this.dist / 40)) * this.cfg.mul)
    }
  },
  mounted() {
    window.addEventListener('keydown', this.onKey)
  },
  beforeUnmount() {
    this._gone = true
    window.removeEventListener('keydown', this.onKey)
    this.stopLoop()
    this.disposePony()
  },
  methods: {
    pickDiff(k) {
      if (!DIFFS[k] || this.stage === 'playing') return
      this.diff = k
      try { localStorage.setItem(DIFF_KEY, k) } catch (e) {}
      sound.click()
    },
    pickModel(k) {
      if (this.stage === 'playing') return
      this.model = k
      try { localStorage.setItem(MODEL_KEY, k) } catch (e) {}
      if (this.inst && k !== 'classic') { try { this.inst.setVariant(k) } catch (e) {} }
      sound.click()
    },
    onKey(e) {
      if (e.code === 'Space' || e.code === 'ArrowUp' || e.key === 'w' || e.key === 'W') {
        if (this.stage === 'playing') { e.preventDefault(); this.jump() }
      }
    },
    start() { this.begin(true) },
    restart() { this.begin(false) },
    begin(first) {
      this.resetRun()
      this.stage = 'playing'
      this.model3d = true
      if (first) sound.magic(); else sound.click()
      this.runLoop()
      this.$nextTick(() => this.initPony())
    },
    backToReady() {
      this.stopLoop()
      this.disposePony()
      this.stage = 'ready'
      this.syncPony()
      sound.click()
    },
    resetRun() {
      this.stopLoop()
      this.disposePony()
      this.py = 0; this.vy = 0; this.time = 0
      this.hearts = this.cfg.hearts
      this.inv = 0; this.dodged = 0; this.starCount = 0; this.score = 0
      this.obstacles = []; this.stars = []
      this.dist = 0; this.nextGap = this.cfg.gap * 0.55
      this.shake = false; this.last = 0
    },
    jump() {
      if (this.stage !== 'playing') return
      if (this.py <= 0.01) { this.vy = 3.3; sound.pop() }
    },

    // ---------- 3D 建模主角 ----------
    async initPony() {
      if (this.model === 'classic' || !this.model3d) return
      const cv = this.$refs.cv
      if (!cv) return
      try {
        if (!ponyMod) ponyMod = await import('../../pet3d/pony.js')
      } catch (e) {
        this.model3d = false
        return
      }
      if (this._gone || !this.$refs.cv) return
      try {
        const r = cv.getBoundingClientRect()
        this.inst = ponyMod.createPonyScene(cv, {
          variant: this.model,
          width: Math.max(64, r.width || 92),
          height: Math.max(64, r.height || 80),
          state: 'run'
        })
        if (typeof ResizeObserver !== 'undefined') {
          this.ro = new ResizeObserver(entries => {
            const b = entries[0].contentRect
            if (this.inst && b.width && b.height) this.inst.resize(b.width, b.height)
          })
          this.ro.observe(cv)
        }
        this.syncPony()
      } catch (e) {
        this.model3d = false
        this.disposePony()
      }
    },
    syncPony() {
      if (!this.inst) return
      const s = this.stage !== 'playing' ? 'idle' : (this.py > .5 ? 'jump' : 'run')
      try { this.inst.setState(s) } catch (e) {}
    },
    disposePony() {
      if (this.ro) { try { this.ro.disconnect() } catch (e) {} this.ro = null }
      if (this.inst) { try { this.inst.dispose() } catch (e) {} this.inst = null }
    },

    // ---------- 游戏循环 ----------
    runLoop() {
      this.raf = requestAnimationFrame((t) => this.loop(t))
    },
    stopLoop() {
      if (this.raf) cancelAnimationFrame(this.raf)
      this.raf = null
      this.last = 0
    },
    loop(now) {
      if (this.stage !== 'playing') return
      const dt = this.last ? Math.min(2.5, (now - this.last) / 16.67) : 1
      this.last = now
      this.time += dt / 60
      this.tick(dt)
      if (this.stage === 'playing') this.raf = requestAnimationFrame((t) => this.loop(t))
    },
    tick(dt) {
      const cfg = this.cfg
      // 速度：前 8 秒 smoothstep 热身（起步只有一半速），之后非常缓慢地提速
      const w = Math.min(1, this.time / WARM)
      const warm = w * w * (3 - 2 * w)
      const speed = cfg.base * 0.5 + cfg.base * 0.5 * warm
        + Math.min(0.28, Math.max(0, this.time - WARM) * cfg.step)

      // 玩家抛物线：重力调小，滞空更久、更好跨过障碍
      if (this.py > 0 || this.vy > 0) {
        this.vy -= 0.15 * dt
        this.py += this.vy * dt
        if (this.py <= 0) { this.py = 0; this.vy = 0 }
      }
      if (this.inv > 0) this.inv -= dt

      // 生成障碍与星星（间隔加大，同屏不再挤成一排）
      this.dist += speed * dt
      if (this.dist >= this.nextGap) {
        this.dist = 0
        this.nextGap = cfg.gap + (Math.random() * 10 - 5)
        this.obstacles.push({ id: ++this.oid, x: 106, type: TYPES[Math.floor(Math.random() * TYPES.length)], passed: false, hit: false })
        if (Math.random() < 0.6) {
          this.stars.push({ id: ++this.sid, x: 122, y: 28 + Math.random() * 22 })
        }
      }

      // 障碍移动 + 碰撞
      for (const o of this.obstacles) {
        o.x -= speed * dt
        const overlapX = o.x < 24 && o.x + 7 > 10
        if (overlapX && !o.hit && !o.passed && this.py < 26) {
          o.hit = true
          if (this.inv > 0 || this.time < WARM) {
            sound.hover()   // 热身期 / 无敌期：只轻轻扶一下
          } else {
            this.hearts -= 1
            this.inv = 120
            sound.fail()
            this.shake = true
            setTimeout(() => { this.shake = false }, 300)
            if (this.hearts <= 0) { this.lose(); return }
          }
        }
        if (o.x + 7 < 10 && !o.passed) {
          o.passed = true
          if (!o.hit) { this.dodged++; sound.hover() }
        }
      }
      this.obstacles = this.obstacles.filter(o => o.x > -12)

      // 星星移动 + 拾取
      for (const s of this.stars) {
        s.x -= speed * dt
        if (s.x < 24 && s.x + 5 > 10 && Math.abs(this.py + 7 - s.y) < 15) {
          this.starCount++
          sound.twinkle()
          s.x = -99
        }
      }
      this.stars = this.stars.filter(s => s.x > -8)

      this.syncPony()
      if (this.time >= cfg.duration) this.win()
    },
    emitScore() {
      this.$emit('score', this.score)
      this.$emit('over', this.score)
    },
    win() {
      this.stage = 'over'; this.result = 'win'
      this.stopLoop()
      this.score = Math.round((this.dodged * 10 + this.starCount * 15 + 120) * this.cfg.mul)
      this.syncPony()
      this.emitScore()
      sound.success()
    },
    lose() {
      this.stage = 'over'; this.result = 'lose'
      this.stopLoop()
      this.score = Math.round((this.dodged * 10 + this.starCount * 15) * this.cfg.mul)
      this.syncPony()
      this.emitScore()
      sound.fail()
    }
  }
}
</script>

<style scoped>
.jp-wrap { padding: 14px 12px 20px; text-align: center; color: var(--text); }

/* 准备页 */
.jp-ready { max-width: 480px; margin: 0 auto; }
.jp-mascot { font-size: 52px; line-height: 1; margin-bottom: 6px; filter: drop-shadow(0 4px 8px rgba(160,108,213,.22)); }
.jp-ready h3 { margin: 4px 0 8px; font-size: 22px; color: var(--primary); }
.jp-sub { margin: 0 0 16px; font-size: 13px; line-height: 1.8; color: var(--muted); }
.jp-sub b { color: var(--primary); }
.jp-diffs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.jp-diff {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  padding: 14px 6px; border: 2px solid var(--border); border-radius: 16px;
  background: var(--card); cursor: pointer; font-family: inherit; color: var(--text);
  transition: transform .18s, border-color .18s, box-shadow .18s;
}
.jp-diff:hover { transform: translateY(-3px); }
.jp-diff.on { border-color: var(--primary); box-shadow: 0 6px 18px color-mix(in srgb, var(--primary) 22%, transparent); background: var(--panel); }
.d-emoji { font-size: 26px; }
.jp-diff b { font-size: 15px; }
.jp-diff small { font-size: 10px; color: var(--muted); line-height: 1.4; }

.jp-models { margin-bottom: 14px; padding: 12px; border-radius: 16px; background: var(--panel); border: 1px solid var(--border); }
.jp-model-label { display: block; font-size: 12px; font-weight: 700; color: var(--primary); margin-bottom: 8px; }
.jp-model-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.jp-model {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 9px 4px; border: 2px solid var(--border); border-radius: 12px;
  background: var(--card); cursor: pointer; font-family: inherit; color: var(--text);
  transition: transform .16s, border-color .16s;
}
.jp-model:hover { transform: translateY(-2px); }
.jp-model.on { border-color: var(--primary); background: color-mix(in srgb, var(--primary) 8%, #fff); }
.jp-model span { font-size: 20px; }
.jp-model b { font-size: 12px; }
.jp-model small { font-size: 9px; color: var(--muted); }
.jp-model-note { display: block; margin-top: 8px; font-size: 10px; color: var(--muted); font-style: normal; }

.jp-how { display: flex; flex-wrap: wrap; justify-content: center; gap: 6px 14px; margin-bottom: 18px; font-size: 11px; color: var(--muted); }

/* HUD */
.jp-hud { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px; }
.hud-chip {
  display: flex; align-items: center; gap: 7px; padding: 6px 12px;
  border: 1px solid var(--border); border-radius: 999px; background: var(--card); font-size: 12px;
}
.hud-chip span { color: var(--muted); }
.hud-chip b { color: var(--primary); font-size: 14px; }

/* 场地 */
.jp-field {
  position: relative; width: min(100%, 520px); height: 220px; margin: 0 auto;
  border-radius: 18px; overflow: hidden; cursor: pointer;
  border: 1px solid var(--border);
  background: linear-gradient(180deg, color-mix(in srgb, var(--blue) 28%, #fff) 0%, var(--panel) 62%, #fff 100%);
  box-shadow: 0 8px 24px color-mix(in srgb, var(--primary) 10%, transparent);
  touch-action: manipulation; user-select: none; -webkit-user-select: none;
}
.jp-field.shake { animation: jp-shake .3s; }
@keyframes jp-shake { 0%,100% { transform: translateX(0); } 25% { transform: translateX(-5px); } 75% { transform: translateX(5px); } }
.jp-sky-glow {
  position: absolute; right: 12%; top: 8%; width: 70px; height: 70px; border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--yellow) 70%, #fff), transparent 70%);
  opacity: .8;
}
.jp-cloud { position: absolute; font-size: 24px; opacity: .55; animation: jp-drift 14s linear infinite; }
.jp-cloud.c1 { top: 14%; left: 12%; }
.jp-cloud.c2 { top: 28%; left: 48%; font-size: 18px; animation-duration: 19s; }
.jp-cloud.c3 { top: 8%; left: 74%; font-size: 20px; animation-duration: 16s; }
@keyframes jp-drift { from { transform: translateX(0); } to { transform: translateX(-40px); } }

.jp-live-tip {
  position: absolute; top: 8px; left: 50%; transform: translateX(-50%);
  padding: 4px 12px; border-radius: 999px; font-size: 11px; white-space: nowrap;
  background: color-mix(in srgb, #fff 82%, transparent); color: var(--primary);
  border: 1px solid var(--border); z-index: 4;
}
.jp-ground {
  position: absolute; left: 0; right: 0; bottom: 0; height: 16%;
  background: linear-gradient(180deg, color-mix(in srgb, var(--mint) 45%, #fff), color-mix(in srgb, var(--mint) 20%, #fff));
}
.jp-ground-line {
  position: absolute; left: 0; right: 0; bottom: 16%; height: 3px;
  background: repeating-linear-gradient(90deg, var(--mint) 0 18px, transparent 18px 30px);
  opacity: .7;
}
.jp-pony {
  position: absolute; left: 9%; width: 18%; height: 34%;
  z-index: 3; pointer-events: none;
}
.jp-pony-canvas { display: block; width: 100%; height: 100%; }
.jp-pony-emoji {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%; font-size: 40px; transition: transform .1s;
}
.jp-pony-emoji.jumping { transform: rotate(-12deg); }
.jp-pony-tag {
  position: absolute; bottom: -2px; left: 50%; transform: translateX(-50%);
  font-size: 9px; color: var(--primary); background: color-mix(in srgb, #fff 80%, transparent);
  padding: 1px 7px; border-radius: 999px; border: 1px solid var(--border); white-space: nowrap;
}
.jp-obstacle {
  position: absolute; bottom: 16%; width: 7%; height: 26%;
  display: flex; align-items: flex-end; justify-content: center; font-size: clamp(16px, 4vw, 26px);
  z-index: 2;
}
.jp-obstacle.hit { filter: grayscale(.6); opacity: .5; }
.jp-star {
  position: absolute; width: 5%; height: 9%;
  display: flex; align-items: center; justify-content: center;
  font-size: clamp(14px, 3.4vw, 20px); z-index: 2;
  animation: jp-twinkle 1s ease-in-out infinite alternate;
}
@keyframes jp-twinkle { from { transform: scale(.9); opacity: .8; } to { transform: scale(1.15); opacity: 1; } }

.jp-mobile { display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap; margin-top: 10px; }
.jp-jump-btn {
  padding: 8px 22px; border: none; border-radius: 999px; cursor: pointer;
  background: linear-gradient(135deg, var(--mint), var(--blue)); color: #fff;
  font-size: 13px; font-weight: 700; font-family: inherit;
  box-shadow: 0 4px 12px color-mix(in srgb, var(--primary) 22%, transparent);
}
.jp-mobile span { font-size: 11px; color: var(--muted); }

/* 结算 */
.jp-result {
  max-width: 380px; margin: 16px auto 0; padding: 18px;
  border-radius: 18px; border: 1px solid var(--border); background: var(--card);
  box-shadow: 0 8px 22px color-mix(in srgb, var(--primary) 12%, transparent);
}
.jp-result.lose { background: color-mix(in srgb, var(--pink) 12%, var(--card)); }
.r-emoji { font-size: 40px; }
.jp-result h3 { margin: 6px 0; font-size: 19px; color: var(--primary); }
.jp-result p { margin: 0 0 8px; font-size: 12px; color: var(--muted); }
.r-score { font-size: 26px; font-weight: 800; color: var(--primary); margin-bottom: 12px; }
.r-btns { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; }
.jp-btn {
  padding: 10px 24px; border: none; border-radius: 999px; cursor: pointer;
  background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff;
  font-size: 14px; font-weight: 700; font-family: inherit;
  box-shadow: 0 5px 14px color-mix(in srgb, var(--primary) 30%, transparent);
  transition: transform .16s;
}
.jp-btn:hover { transform: translateY(-2px); }
.jp-ghost {
  padding: 10px 20px; border: 1.5px solid var(--border); border-radius: 999px;
  background: var(--card); color: var(--text); font-size: 13px; font-family: inherit; cursor: pointer;
}

@media (prefers-reduced-motion: reduce) {
  .jp-cloud, .jp-star, .jp-field.shake { animation: none; }
  .jp-pony-emoji, .jp-diff, .jp-model, .jp-btn { transition: none; }
}
</style>
