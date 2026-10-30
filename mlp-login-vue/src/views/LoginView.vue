<template>
  <div>
    <div class="particle-layer" ref="particleLayer"></div>
    <div class="head">
      <div class="logo">🦄</div>
      <h1>小马王国</h1>
      <p>友谊的魔法，从登录开始 ✨</p>
    </div>

    <div class="tabs">
      <router-link to="/login" class="tab active">登录</router-link>
      <router-link to="/register" class="tab">注册</router-link>
    </div>

    <form novalidate @submit.prevent="handleLogin">
      <div class="field">
        <label for="loginName">小马名字</label>
        <div class="input-wrap">
          <input
            id="loginName"
            v-model="form.name"
            type="text"
            placeholder="请输入你的小马名字"
            autocomplete="username"
            :class="{ invalid: errors.name }"
          />
        </div>
        <div class="err">{{ errors.name }}</div>
      </div>
      <div class="field">
        <label for="loginPw">魔法密码</label>
        <div class="input-wrap">
          <input
            id="loginPw"
            v-model="form.password"
            :type="show.password ? 'text' : 'password'"
            placeholder="请输入密码"
            autocomplete="current-password"
            :class="{ invalid: errors.password }"
          />
          <button type="button" class="toggle-pw" @click="show.password = !show.password">
            {{ show.password ? '隐藏' : '显示' }}
          </button>
        </div>
        <div class="err">{{ errors.password }}</div>
      </div>
      <div class="row">
        <label><input type="checkbox" v-model="form.remember" /> 记住我</label>
        <a href="#" @click.prevent="handleForgot">忘记魔法密码？</a>
      </div>
      <div class="submit-wrap">
        <button type="submit" class="submit" ref="loginBtn" :class="{ popping: rainbow }">进入小马王国 🌈</button>
      </div>
    </form>

    <p class="admin-entry">
      <router-link to="/admin/login">管理员登录 ›</router-link>
    </p>

    <!-- 切换账号提示条 -->
    <div v-if="switchHint" class="switch-hint">
      🔁 正在切换账号，上次登录的是「{{ lastAccount }}」<br />
      <span>要换别的账号，直接把名字和密码改成另一个即可</span>
    </div>
  </div>
</template>

<script>
import { setSession, apiLogin, rememberAccount, getRememberedAccount } from '../auth'
export default {
  name: 'LoginView',
  inject: ['toast'],
  data() {
    return {
      form: { name: '', password: '', remember: false },
      show: { password: false },
      errors: {},
      submitting: false,
      rainbow: false,
      switchHint: false,
      lastAccount: ''
    }
  },
  mounted() {
    // 注册成功跳转会带上用户名，预填登录框
    const n = this.$route.query.name
    if (typeof n === 'string' && n) {
      this.form.name = n
    } else {
      // 切换账号过来的：预填上次登录的账号名，改密码即可登另一个
      this.lastAccount = getRememberedAccount()
      if (this.lastAccount) this.form.name = this.lastAccount
    }
    this.switchHint = this.$route.query.switch === '1' && !!this.lastAccount
  },
  methods: {
    async handleLogin() {
      if (this.submitting) return
      // 点击即从按钮中心炸开一簇彩虹粒子（无论校验是否通过，先来一发炫的）
      this.burstParticles()
      this.rainbow = false
      this.$nextTick(() => {
        this.rainbow = true
        setTimeout(() => { this.rainbow = false }, 450)
      })
      const name = this.form.name.trim()
      const pw = this.form.password
      this.errors = {}
      if (!name) { this.errors.name = '请输入小马名字'; return }
      if (!pw) { this.errors.password = '请输入密码'; return }

      this.submitting = true
      const res = await apiLogin({ name, password: pw })
      this.submitting = false

      if (res.code !== 200) {
        if (res.msg.includes('验证码')) {
          // 新版登录已经不需要验证码；还会出现这句话，说明后端跑的是改动前的旧代码
          this.errors.password = '后端还是旧版本（登录仍在要求验证码），请完全停止后端并重新编译启动'
        }
        else if (res.msg.includes('还没注册') || res.msg.includes('名字')) this.errors.name = res.msg
        else this.errors.password = res.msg
        return
      }

      const user = res.data
      // 记住账号名：下次「切换账号」回登录页时可直接预填
      rememberAccount(user.name)
      // 把权限一起存进会话：首页据此决定是否展示「后台管理」入口
      setSession({
        id: user.id,
        name: user.name,
        permission: (user.permission === 0 || user.permission === 2) ? user.permission : 1,
        ts: Date.now()
      }, this.form.remember)
      this.toast('登录成功，欢迎回家！🌟', 'ok')
      this.$router.push({ name: 'home' })
    },
    handleForgot() {
      const name = prompt('请输入你的小马名字，我们会发送重置链接（演示）：')
      if (name && name.trim()) {
        this.toast('重置链接已发送至 ' + name.trim() + ' 的预留渠道（演示）', 'ok')
      }
    },
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
    }
  }
}
</script>

<style scoped>
/* 粒子层：全屏覆盖、不挡点击 */
.particle-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  overflow: hidden;
}
.submit-wrap {
  position: relative;
}
.submit {
  position: relative;
  z-index: 1;
}
/* 点击：按钮轻微弹一下 */
.submit.popping {
  animation: pop .45s ease;
}
@keyframes pop {
  0% { transform: scale(.95); }
  45% { transform: scale(1.04); }
  100% { transform: scale(1); }
}
.admin-entry {
  margin: 14px 0 14px;
  padding-bottom: 10px;
  text-align: center;
  font-size: 12.5px;
}
.admin-entry a {
  color: var(--muted);
  text-decoration: none;
  letter-spacing: .5px;
  transition: color .15s ease;
}
.admin-entry a:hover {
  color: var(--primary);
  text-decoration: underline;
}
.switch-hint {
  margin-top: 14px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff8e6;
  border: 1.5px solid #f3d77a;
  color: #8a6d1f;
  font-size: 12.5px;
  font-weight: 700;
  line-height: 1.6;
  text-align: center;
}
.switch-hint span {
  font-weight: 400;
  font-size: 11.5px;
  color: #a08a4d;
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
