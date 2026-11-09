<template>
  <!-- 全局点击特效层：点击 / 轻触时在指针位置炸开一簇马卡龙粒子，不挡任何交互 -->
  <canvas ref="cv" class="click-sparkle" aria-hidden="true"></canvas>
</template>

<script>
/**
 * 点击粒子特效
 *  - 三种造型：圆点光斑 / 四角星光 / 小爱心
 *  - 炸开时附带一圈扩散的光环
 *  - 颜色实时跟随当前主题（读 --pink / --purple / --blue / --mint / --yellow / --primary）
 *  - 只有真正"点一下"才触发（按下到松开的位移超过 10px 视为滑动/滚动，不炸）
 *  - 没有粒子时不跑 rAF；连点有数量上限，防止卡顿
 *  - pointer-events:none 完全不挡点击；系统开了"减少动态效果"时只留一圈淡光环
 */
const FALLBACK = ['#f7a8c4', '#b79cf0', '#8fc7f0', '#a7e3c9', '#f7e08a', '#ffd6e7']
const MAX_PARTS = 420

export default {
  name: 'ClickSparkle',
  mounted() {
    const cv = this.$refs.cv
    const ctx = cv.getContext('2d')
    const rand = (a, b) => a + Math.random() * (b - a)
    const reduceMotion = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches)

    let w = 0
    let h = 0
    let raf = 0
    let running = false
    const parts = []
    const rings = []

    const resize = () => {
      const dpr = Math.min(2, window.devicePixelRatio || 1)
      w = window.innerWidth
      h = window.innerHeight
      cv.width = Math.round(w * dpr)
      cv.height = Math.round(h * dpr)
      cv.style.width = w + 'px'
      cv.style.height = h + 'px'
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }
    resize()
    window.addEventListener('resize', resize)

    // ---- 调色板：跟随主题，400ms 内复用，避免每次点击都重算样式 ----
    let palette = FALLBACK
    let palAt = 0
    const readPalette = () => {
      const now = performance.now()
      if (now - palAt < 400) return palette
      palAt = now
      try {
        const cs = getComputedStyle(document.body)
        const names = ['--pink', '--purple', '--blue', '--mint', '--yellow', '--primary']
        const arr = names
          .map((n) => (cs.getPropertyValue(n) || '').trim())
          .filter((v) => v && v !== 'rgba(0, 0, 0, 0)')
        if (arr.length >= 3) palette = arr
      } catch (e) {}
      return palette
    }

    const drawShape = (p, r) => {
      ctx.beginPath()
      if (p.kind === 'star') {
        // 四角星光：四条边往里凹，中间胖，转起来像闪一闪
        ctx.moveTo(0, -r)
        ctx.quadraticCurveTo(r * 0.15, -r * 0.15, r, 0)
        ctx.quadraticCurveTo(r * 0.15, r * 0.15, 0, r)
        ctx.quadraticCurveTo(-r * 0.15, r * 0.15, -r, 0)
        ctx.quadraticCurveTo(-r * 0.15, -r * 0.15, 0, -r)
        ctx.closePath()
      } else if (p.kind === 'heart') {
        const s = r / 5
        ctx.moveTo(0, 3.1 * s)
        ctx.bezierCurveTo(-5 * s, -0.9 * s, -3.1 * s, -6.1 * s, 0, -2.9 * s)
        ctx.bezierCurveTo(3.1 * s, -6.1 * s, 5 * s, -0.9 * s, 0, 3.1 * s)
        ctx.closePath()
      } else {
        ctx.arc(0, 0, r, 0, Math.PI * 2)
      }
      ctx.fill()
    }

    const spawn = (x, y) => {
      const pal = readPalette()
      const small = window.innerWidth < 768
      const count = reduceMotion ? 0 : small ? 11 : 17

      for (let i = 0; i < count; i++) {
        const ang = rand(0, Math.PI * 2)
        const sp = rand(1.5, small ? 5 : 6.8)
        const roll = Math.random()
        const kind = roll < 0.4 ? 'star' : roll < 0.72 ? 'heart' : 'dot'
        parts.push({
          kind,
          x,
          y,
          vx: Math.cos(ang) * sp,
          vy: Math.sin(ang) * sp - rand(0.3, 2.1),
          size: kind === 'dot' ? rand(2.1, 4.6) : rand(4.4, 9.2),
          color: pal[(Math.random() * pal.length) | 0],
          life: 1,
          decay: rand(0.011, 0.023),
          rot: rand(0, Math.PI * 2),
          vr: rand(-0.24, 0.24)
        })
      }

      rings.push({
        x,
        y,
        r: rand(2, 5),
        rMax: rand(26, 52),
        life: 1,
        color: pal[(Math.random() * pal.length) | 0]
      })

      if (parts.length > MAX_PARTS) parts.splice(0, parts.length - MAX_PARTS)
      if (rings.length > 60) rings.splice(0, rings.length - 60)
      if (!running) {
        running = true
        raf = requestAnimationFrame(tick)
      }
    }

    const tick = () => {
      ctx.clearRect(0, 0, w, h)

      // 光环：向外扩散 + 淡出
      for (let i = rings.length - 1; i >= 0; i--) {
        const g = rings[i]
        g.r += (g.rMax - g.r) * 0.17
        g.life -= 0.05
        if (g.life <= 0) {
          rings.splice(i, 1)
          continue
        }
        ctx.globalAlpha = Math.max(0, g.life) * 0.5
        ctx.strokeStyle = g.color
        ctx.lineWidth = Math.max(0.5, 2.6 * g.life)
        ctx.beginPath()
        ctx.arc(g.x, g.y, g.r, 0, Math.PI * 2)
        ctx.stroke()
      }

      // 粒子：阻尼 + 轻微重力 + 缩小淡出
      for (let i = parts.length - 1; i >= 0; i--) {
        const p = parts[i]
        p.vx *= 0.955
        p.vy = p.vy * 0.955 + 0.17
        p.x += p.vx
        p.y += p.vy
        p.rot += p.vr
        p.life -= p.decay
        if (p.life <= 0) {
          parts.splice(i, 1)
          continue
        }
        ctx.globalAlpha = Math.min(1, p.life * 1.55)
        ctx.fillStyle = p.color
        ctx.save()
        ctx.translate(p.x, p.y)
        ctx.rotate(p.rot)
        drawShape(p, Math.max(0.6, p.size * (0.35 + 0.65 * p.life)))
        ctx.restore()
      }

      ctx.globalAlpha = 1
      if (parts.length || rings.length) {
        raf = requestAnimationFrame(tick)
      } else {
        running = false
        ctx.clearRect(0, 0, w, h)
      }
    }

    // ---- 判定"点一下"：按下 → 松开位移小、时间短，才算点击 ----
    let downX = 0
    let downY = 0
    let downT = 0
    let downId = null

    const onDown = (e) => {
      if (e.button !== undefined && e.button > 0) return
      downId = e.pointerId
      downX = e.clientX
      downY = e.clientY
      downT = performance.now()
    }
    const onUp = (e) => {
      if (downId === null) return
      if (e.pointerId !== undefined && e.pointerId !== downId) return
      const dist = Math.hypot(e.clientX - downX, e.clientY - downY)
      const dur = performance.now() - downT
      downId = null
      if (dist > 10 || dur > 800) return // 滑动 / 长按不算点击
      spawn(e.clientX, e.clientY)
    }
    const onCancel = () => {
      downId = null
    }
    window.addEventListener('pointerdown', onDown, { passive: true })
    window.addEventListener('pointerup', onUp, { passive: true })
    window.addEventListener('pointercancel', onCancel, { passive: true })

    this._cleanup = () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('resize', resize)
      window.removeEventListener('pointerdown', onDown)
      window.removeEventListener('pointerup', onUp)
      window.removeEventListener('pointercancel', onCancel)
    }
  },
  beforeUnmount() {
    if (this._cleanup) this._cleanup()
  }
}
</script>

<style scoped>
.click-sparkle {
  position: fixed;
  inset: 0;
  z-index: 9990;
  pointer-events: none;
}
</style>
