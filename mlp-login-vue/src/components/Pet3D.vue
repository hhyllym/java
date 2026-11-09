<template>
  <span
    class="pet3d"
    :class="{ 'pet3d-mini': size <= 90 }"
    :style="{ width: size + 'px', height: size + 'px' }"
    :title="title || cfg.name + ' · 拖动可以转一转'"
  >
    <canvas v-show="!failed" ref="cv" class="pet3d-canvas"></canvas>
    <PetSprite v-if="failed" :kind="kind" :size="size" :state="state"
               :cosmetic="cosmetic" :cosmetic-name="cosmeticName" />
    <span v-if="cosmetic" class="pet3d-cosmetic" :style="{ fontSize: Math.max(14, size * .15) + 'px' }"
          :title="cosmeticName || ''">{{ cosmetic }}</span>
    <span v-if="!failed && hint" class="pet3d-hint">🖐️ 拖我转转</span>
    <span v-for="(f, i) in fx" :key="i" class="pet3d-fx"
          :style="{ left: (34 + i * 16) + '%', animationDelay: (i * .3) + 's' }">{{ f }}</span>
  </span>
</template>

<script>
/**
 * 真 3D 宠物：Three.js WebGL 实时渲染，可拖动旋转 / 滚轮缩放。
 * 头、耳、尾、翅膀、眼睑是独立部件，各自有动画（见 pet3d/scene.js）。
 * WebGL 不可用或初始化失败时自动回退到 2D 立绘版 PetSprite。
 */
import { pet3dConfig } from '../pet3d/config.js'
import PetSprite from './PetSprite.vue'

export default {
  name: 'Pet3D',
  components: { PetSprite },
  props: {
    kind: { type: String, default: 'cat' },
    size: { type: Number, default: 240 },
    state: { type: String, default: 'idle' },
    interactive: { type: Boolean, default: true },
    hint: { type: Boolean, default: false },
    cosmetic: { type: String, default: '' },
    cosmeticName: { type: String, default: '' },
    title: { type: String, default: '' }
  },
  data() { return { failed: false } },
  computed: {
    cfg() { return pet3dConfig(this.kind) },
    fx() {
      if (this.state === 'happy' || this.state === 'play') return ['💗', '✨', '💫']
      if (this.state === 'eat') return ['🍖', '💗']
      if (this.state === 'walk') return ['🐾']
      if (this.state === 'hungry') return ['💤']
      return []
    }
  },
  watch: {
    kind(v) { if (this.inst) { try { this.inst.setKind(v) } catch (e) { this.failed = true } } },
    state(v) { if (this.inst) this.inst.setState(v) }
  },
  async mounted() {
    // three.js 只在真正打开宠物页时才下载，不拖累首屏
    let createPetScene
    try {
      ({ createPetScene } = await import('../pet3d/scene.js'))
    } catch (e) {
      this.failed = true
      return
    }
    if (this._gone || !this.$refs.cv) return
    try {
      const cv = this.$refs.cv
      this.inst = createPetScene(cv, this.kind, {
        state: this.state,
        interactive: this.interactive,
        width: this.size,
        height: this.size
      })
    } catch (e) {
      this.failed = true
      return
    }
    if (typeof ResizeObserver !== 'undefined') {
      this.ro = new ResizeObserver(entries => {
        const r = entries[0].contentRect
        if (this.inst && r.width && r.height) this.inst.resize(r.width, r.height)
      })
      this.ro.observe(this.$el)
    }
  },
  beforeUnmount() {
    this._gone = true
    if (this.ro) this.ro.disconnect()
    if (this.inst) this.inst.dispose()
    this.inst = null
  },
  methods: {
    poke() { if (this.inst) this.inst.poke() }
  }
}
</script>

<style scoped>
.pet3d {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
  line-height: 0;
  vertical-align: middle;
  user-select: none;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 38%, #fffaf3 0%, #fdf1e6 58%, #f3e4f3 100%);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, .8), 0 8px 22px rgba(150, 106, 190, .2);
  overflow: hidden;
}
.pet3d-canvas { display: block; width: 100%; height: 100%; touch-action: none; }
.pet3d-cosmetic {
  position: absolute; top: 9%; left: 50%; margin-left: -0.5em; z-index: 3; line-height: 1;
  filter: drop-shadow(0 2px 3px rgba(160, 108, 213, .45));
  pointer-events: none;
  animation: pet3dCos 2.6s ease-in-out infinite;
}
@keyframes pet3dCos { 0%, 100% { transform: translateY(0) rotate(-7deg) } 50% { transform: translateY(-3px) rotate(8deg) } }
.pet3d-hint {
  position: absolute; bottom: 6%; left: 0; right: 0;
  text-align: center; font-size: 10px; line-height: 1.3;
  color: #a487bd; pointer-events: none;
  animation: pet3dHint 3.4s ease-in-out infinite;
}
@keyframes pet3dHint { 0%, 100% { opacity: .45 } 50% { opacity: .95 } }
.pet3d-fx {
  position: absolute; top: 6%; font-size: 14px; line-height: 1;
  pointer-events: none; opacity: 0;
  animation: pet3dFx 1.6s ease-out infinite;
}
@keyframes pet3dFx {
  0% { opacity: 0; transform: translateY(6px) scale(.5) }
  25% { opacity: 1 }
  100% { opacity: 0; transform: translateY(-32px) scale(1.1) }
}
.pet3d-mini .pet3d-fx { display: none; }
</style>
