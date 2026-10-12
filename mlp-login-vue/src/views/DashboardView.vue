<template>
  <div class="dash">
    <div class="avatar">🦄</div>
    <h2>{{ session.name || '小马' }}</h2>
    <div class="meta">上次登录：{{ lastLoginText }}</div>
    <button @click="handleLogout">退出登录</button>
  </div>
</template>

<script>
import { getSession, clearSession } from '../auth'

export default {
  name: 'DashboardView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {}
    }
  },
  computed: {
    lastLoginText() {
      if (!this.session.ts) return ''
      return new Date(this.session.ts).toLocaleString('zh-CN')
    }
  },
  methods: {
    handleLogout() {
      clearSession()
      this.toast('已退出登录，拜拜 👋', 'ok')
      this.$router.push({ name: 'login' })
    }
  }
}
</script>
