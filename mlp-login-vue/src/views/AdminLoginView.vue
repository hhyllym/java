<template>
  <div>
    <div class="particle-layer" ref="particleLayer"></div>
    <div class="head">
      <div class="logo">🦄</div>
      <h1>管理员登录</h1>
      <p>仅限管理员进入 · 普通用户请回首页</p>
    </div>

    <div class="tabs">
      <router-link to="/login" class="tab">用户登录</router-link>
      <span class="tab active">管理员登录</span>
    </div>

    <div v-if="netError" class="net-err">{{ netError }}</div>

    <form novalidate @submit.prevent="handleLogin">
      <div class="field">
        <label for="adminName">管理员账号</label>
        <div class="input-wrap">
          <input
            id="adminName"
            v-model="form.username"
            type="text"
            placeholder="请输入管理员账号"
            autocomplete="username"
            :class="{ invalid: errors.username }"
          />
        </div>
        <div class="err">{{ errors.username }}</div>
      </div>

      <div class="field">
        <label for="adminPw">管理员密码</label>
        <div class="input-wrap">
          <input
            id="adminPw"
            v-model="form.password"
            :type="show ? 'text' : 'password'"
            placeholder="请输入管理员密码"
            autocomplete="current-password"
            :class="{ invalid: errors.password }"
          />
          <button type="button" class="toggle-pw" @click="show = !show">
            {{ show ? '隐藏' : '显示' }}
          </button>
        </div>
        <div class="err">{{ errors.password }}</div>
      </div>

      <div class="submit-wrap">
        <button type="submit" class="submit" ref="loginBtn" :class="{ popping: rainbow }" :disabled="submitting">
          {{ submitting ? '登录中…' : '进入后台管理系统' }}
        </button>
      </div>
    </form>

    <router-link to="/home" class="back-link">« 返回首页</router-link>
  </div>
</template>

<script>
import { apiAdminLogin, setAdminSession } from '../adminAuth'

export default {
  name: 'AdminLoginView',
  inject: ['toast'],
  data() {
    return {
      form: { username: '', password: '' },
      show: false,
      errors: {},
      netError: '',
      submitting: false,
      rainbow: false
    }
  },
  methods: {
    burstParticles() {
      const layer = this.$refs.particleLayer
      const btn = this.$refs.loginBtn
      if (!layer || !btn) return
      const rect = btn.getBoundingClientRect()
      const cx = rect.left + rect.width / 2
      const cy = rect.top + rect.height / 2
      const colors = ['#ff5d8f', '#ffb347', '#ffe14d', '#7be36a', '#4dd0e1', '#7b6cff', '#e07bff']
      const N = 30
      for (let i = 0; i < N; i++) {
        const p = document.createElement('span')
        p.className = 'particle'
        const color = colors[i % colors.length]
        const size = 6 + Math.random() * 6
        p.style.background = color
        p.style.left = cx + 'px'
        p.style.top = cy + 'px'
        p.style.width = size + 'px'
        p.style.height = size + 'px'
        layer.appendChild(p)
        const angle = (Math.PI * 2 * i) / N + Math.random() * 0.6
        const dist = 70 + Math.random() * 110
        const dx = Math.cos(angle) * dist
        const dy = Math.sin(angle) * dist - 40
        const dur = 650 + Math.random() * 450
        p.animate([
          { transform: 'translate(-50%, -50%) translate(0, 0) scale(1)', opacity: 1 },
          { transform: `translate(-50%, -50%) translate(${dx}px, ${dy}px) scale(.3)`, opacity: 0 }
        ], { duration: dur, easing: 'cubic-bezier(.22,.61,.36,1)' }).onfinish = () => p.remove()
      }
    },
    async handleLogin() {
      if (this.submitting) return
      this.burstParticles()
      this.rainbow = false
      this.$nextTick(() => {
        this.rainbow = true
        setTimeout(() => { this.rainbow = false }, 450)
      })
      const username = this.form.username.trim()
      const password = this.form.password
      this.errors = {}
      this.netError = ''
      if (!username) { this.errors.username = '请输入管理员账号'; return }
      if (!password) { this.errors.password = '请输入管理员密码'; return }

      this.submitting = true
      const res = await apiAdminLogin(username, password)
      this.submitting = false

      if (res.code !== 200) {
        // 连不上后端（code=-1）：单独用提示条展示，不挤进密码框下面的小红字
        if (res.code === -1) {
          this.netError = '📡 连不上后端（端口 8081）——请先把 Spring Boot 启动起来，再回来登录'
        } else {
          this.errors.password = res.msg || '登录失败'
        }
        return
      }
      const d = res.data || {}
      setAdminSession({ name: d.name || username, token: d.token, permission: (d.permission === 0 || d.permission === 2) ? d.permission : 1, ts: Date.now() })
      this.toast('登录成功，欢迎来到后台 🛠', 'ok')
      this.$router.replace({ name: 'admin' })
    }
  }
}
</script>

<style scoped>
/* 与前台登录页一致：粒子层全屏覆盖、不挡点击 */
.particle-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  overflow: hidden;
}
.submit-wrap { position: relative; }
.submit { position: relative; z-index: 1; }
/* 点击：按钮轻微弹一下（与前台登录一致） */
.submit.popping { animation: pop .45s ease; }
@keyframes pop {
  0% { transform: scale(.95); }
  45% { transform: scale(1.04); }
  100% { transform: none; }
}

/* 连不上后端的提示条（后台登录独有） */
.net-err {
  margin: 14px 28px 0;
  padding: 10px 13px;
  border-radius: 12px;
  background: #fdeaea;
  border: 1px solid #f5c2c2;
  color: #b23b3b;
  font-size: 12.5px;
  line-height: 1.55;
  text-align: left;
  animation: netErrIn .25s ease;
}
@keyframes netErrIn { from { opacity: 0; transform: translateY(-5px); } }

/* 返回首页：仿照前台登录页底部入口 */
.back-link {
  display: block;
  margin: 4px auto 0;
  padding: 0 28px 18px;
  text-align: center;
  font-size: 12.5px;
  color: var(--muted);
  text-decoration: none;
  letter-spacing: .5px;
  transition: color .15s ease;
}
.back-link:hover { color: var(--primary); text-decoration: underline; }

/* 网页版宽屏：顶部间距与前台登录页一致（宽度由 .card-plain 统一限制为 460px） */
@media (min-width: 768px) {
  .head { padding-top: 40px; }
}
</style>

<style>
/* 粒子是 JS 动态创建的 DOM，带不上 scoped 的 data-v 属性，必须用全局样式 */
.particle {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  box-shadow: 0 0 6px rgba(255, 255, 255, .6);
}
</style>
