<template>
  <PonyBackground />

  <div class="card">
    <div class="rainbow"></div>
    <div class="themes">
      <button
        v-for="t in themeList"
        :key="t.name"
        class="theme-dot"
        :class="{ active: theme === t.name }"
        :style="{ '--dot': t.color }"
        :title="t.title"
        @click="applyTheme(t.name)"
      ></button>
    </div>

    <router-view />
  </div>

  <div class="toast" :class="toastClass">{{ toastMsg }}</div>
</template>

<script>
import PonyBackground from './components/PonyBackground.vue'

const THEME_KEY = 'mlp_theme'

export default {
  name: 'App',
  components: { PonyBackground },
  provide() {
    return { toast: this.toast }
  },
  data() {
    return {
      theme: 'default',
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
    let saved = null
    try { saved = localStorage.getItem(THEME_KEY) } catch (e) {}
    this.applyTheme(saved || 'default', false)
  },
  methods: {
    applyTheme(t, persist = true) {
      this.theme = t
      if (t === 'default') document.body.removeAttribute('data-theme')
      else document.body.setAttribute('data-theme', t)
      if (persist) {
        try { localStorage.setItem(THEME_KEY, t) } catch (e) {}
      }
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
