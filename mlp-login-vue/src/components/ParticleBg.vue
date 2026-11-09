<template>
  <!-- 彩色粒子飘散背景：全局装饰层，canvas 绘制，不抢任何交互 -->
  <canvas ref="cv" class="particle-bg" aria-hidden="true"></canvas>
</template>

<script>
/** 马卡龙色粒子：缓缓上飘 + 左右摇摆 + 轻微闪烁 */
const COLORS = ['#f7a8c4', '#b79cf0', '#8fc7f0', '#a7e3c9', '#f7d774', '#f0a06a', '#ffd6e7', '#c9e4ff']

export default {
  name: 'ParticleBg',
  mounted() {
    const cv = this.$refs.cv
    const ctx = cv.getContext('2d')
    let w = 0, h = 0, raf = 0, running = true
    const rand = (a, b) => a + Math.random() * (b - a)

    const resize = () => {
      w = cv.width = window.innerWidth
      h = cv.height = window.innerHeight
    }
    resize()
    window.addEventListener('resize', resize)

    // 数量随屏宽自适应，小屏少一点省性能
    const COUNT = Math.min(72, Math.max(34, Math.round(window.innerWidth / 20)))
    const make = (anywhere) => ({
      x: rand(0, w),
      y: anywhere ? rand(0, h) : h + 12,
      r: rand(1.6, 4.4),
      c: COLORS[(Math.random() * COLORS.length) | 0],
      vy: rand(0.14, 0.5),            // 上升速度
      swayAmp: rand(8, 26),           // 左右摆幅
      swayFreq: rand(0.002, 0.006),
      phase: rand(0, Math.PI * 2),
      alpha: rand(0.22, 0.62),
      twFreq: rand(0.004, 0.012),     // 闪烁频率
      twPhase: rand(0, Math.PI * 2)
    })
    const particles = []
    for (let i = 0; i < COUNT; i++) particles.push(make(true))

    let t = 0
    const tick = () => {
      if (!running) return
      t += 1
      ctx.clearRect(0, 0, w, h)
      for (const p of particles) {
        p.y -= p.vy
        if (p.y < -14) Object.assign(p, make(false))
        const x = p.x + Math.sin(t * p.swayFreq + p.phase) * p.swayAmp
        const a = p.alpha * (0.72 + 0.28 * Math.sin(t * p.twFreq + p.twPhase))
        ctx.globalAlpha = Math.max(0.05, a)
        ctx.fillStyle = p.c
        ctx.beginPath()
        ctx.arc(x, p.y, p.r, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.globalAlpha = 1
      raf = requestAnimationFrame(tick)
    }
    tick()

    // 切后台暂停省电，回来接着飘
    const onVis = () => {
      if (document.hidden) { running = false; cancelAnimationFrame(raf) }
      else if (!running) { running = true; tick() }
    }
    document.addEventListener('visibilitychange', onVis)

    this._cleanup = () => {
      running = false
      cancelAnimationFrame(raf)
      window.removeEventListener('resize', resize)
      document.removeEventListener('visibilitychange', onVis)
    }
  },
  beforeUnmount() {
    if (this._cleanup) this._cleanup()
  }
}
</script>

<style scoped>
.particle-bg {
  position: fixed; inset: 0; z-index: 0;
  pointer-events: none;
}
</style>
