<template>
  <PonyBackground />

  <div class="card">
    <div class="rainbow"></div>
    <div class="themes">
      <button
        class="theme-dot sound-toggle"
        :class="{ muted: soundMuted }"
        :title="soundMuted ? '音效已关闭 · 点我开启' : '音效开启中 · 点我静音'"
        @click="toggleSound"
      >{{ soundMuted ? '🔇' : '🔊' }}</button>
      <button
        class="theme-dot auto"
        :class="{ active: theme === 'auto' }"
        title="自动变色：每 8 秒 smooth 切换一套主题"
        @click="startAuto"
      ></button>
      <button
        v-for="t in themeList"
        :key="t.name"
        class="theme-dot"
        :class="{ active: theme === t.name }"
        :style="{ '--dot': t.color }"
        :title="t.title"
        @click="pickTheme(t.name)"
      ></button>
    </div>

    <router-view />
  </div>

  <div class="toast" :class="toastClass">{{ toastMsg }}</div>
</template>

<script>
import PonyBackground from './components/PonyBackground.vue'
import sound from './sound'
import { initPresence, stopPresence } from './presence'

const THEME_KEY = 'mlp_theme'
const AUTO_INTERVAL = 8000 // 自动变色：每套主题停留 8 秒（颜色用 CSS 过渡平滑渐变，见 style.css）

export default {
  name: 'App',
  components: { PonyBackground },
  provide() {
    return { toast: this.toast, sound }
  },
  data() {
    return {
      theme: 'auto',
      autoTimer: null,
      autoIndex: 0,
      soundMuted: sound.muted,
      themeList: [
        { name: 'default', color: '#f7a8c4', title: '碧琪粉（默认）' },
        { name: 'twilight', color: '#8f7ae8', title: '紫悦紫' },
        { name: 'dash', color: '#6ec6e6', title: '云宝蓝' },
        { name: 'applejack', color: '#f0a06a', title: '苹果橙' },
        { name: 'fluttershy', color: '#7fd4a8', title: '柔柔绿' },
        { name: 'night', color: '#3a3153', title: '坎特洛特之夜' }
      ],
      toastMsg: '',
      toastClass: '',
      toastTimer: null
    }
  },
  mounted() {
    // 默认就是自动变色：每 8 秒在 6 套主题间循环一圈
    this.startAuto()
    this.detectDevice()
    window.addEventListener('resize', this.detectDevice)
    // 在线状态心跳：登录后每 30 秒上报一次
    initPresence()
    window.addEventListener('beforeunload', this.onUnload)
  },
  beforeUnmount() {
    this.stopAuto()
    window.removeEventListener('resize', this.detectDevice)
    window.removeEventListener('beforeunload', this.onUnload)
    stopPresence()
  },
  methods: {
    /** 页面关闭 / 刷新：尽最大努力上报离线 */
    onUnload() {
      stopPresence()
    },
    /** 设备识别：给 body 打 mobile/desktop 标记（横竖屏切换、缩放窗口会更新） */
    detectDevice() {
      const ua = navigator.userAgent || ''
      const uaMobile = /Android|iPhone|iPad|iPod|Mobile|HarmonyOS/i.test(ua)
      const isMobile = uaMobile || window.innerWidth < 768
      document.body.setAttribute('data-device', isMobile ? 'mobile' : 'desktop')
    },
    setBodyTheme(name) {
      if (name === 'default') document.body.removeAttribute('data-theme')
      else document.body.setAttribute('data-theme', name)
    },
    applyTheme(t, persist = true) {
      this.theme = t
      this.setBodyTheme(t)
      if (persist) {
        try { localStorage.setItem(THEME_KEY, t) } catch (e) {}
      }
    },
    startAuto() {
      this.stopAuto()
      this.theme = 'auto'
      this.autoIndex = 0
      this.setBodyTheme(this.themeList[0].name)
      this.autoTimer = setInterval(() => {
        this.autoIndex = (this.autoIndex + 1) % this.themeList.length
        this.setBodyTheme(this.themeList[this.autoIndex].name)
      }, AUTO_INTERVAL)
      try { localStorage.setItem(THEME_KEY, 'auto') } catch (e) {}
    },
    toggleSound() {
      this.soundMuted = sound.toggle()
    },
    stopAuto() {
      if (this.autoTimer) {
        clearInterval(this.autoTimer)
        this.autoTimer = null
      }
    },
    pickTheme(name) {
      // 手动点主题圆点：锁定该主题，停止自动变色
      sound.click()
      this.stopAuto()
      this.applyTheme(name)
    },
    toast(msg, type) {
      this.toastMsg = msg
      this.toastClass = 'show ' + (type || '')
      clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => {
        this.toastClass = type || ''
      }, 2200)
    }
  }
}
</script>
