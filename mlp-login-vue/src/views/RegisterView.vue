<template>
  <div>
    <div class="particle-layer" ref="particleLayer"></div>
    <div class="head">
      <div class="logo">🦄</div>
      <h1>小马王国</h1>
      <p>创建你的小马账号，加入友谊的魔法 ✨</p>
    </div>

    <div class="tabs">
      <router-link to="/login" class="tab">登录</router-link>
      <router-link to="/register" class="tab active">注册</router-link>
    </div>

    <form novalidate @submit.prevent="handleRegister">
      <div class="field">
        <label for="regName">小马名字</label>
        <div class="input-wrap">
          <input
            id="regName"
            v-model="form.name"
            type="text"
            placeholder="2-20 个字符，登录时用它"
            autocomplete="nickname"
            :class="{ invalid: errors.name }"
          />
        </div>
        <div class="err">{{ errors.name }}</div>
      </div>
      <div class="field">
        <label for="regPw">设置密码</label>
        <div class="input-wrap">
          <input
            id="regPw"
            v-model="form.password"
            :type="show.password ? 'text' : 'password'"
            placeholder="至少 6 位，含字母和数字"
            autocomplete="new-password"
            :class="{ invalid: errors.password }"
          />
          <button type="button" class="toggle-pw" @click="show.password = !show.password">
            {{ show.password ? '隐藏' : '显示' }}
          </button>
        </div>
        <div class="strength">
          <span :style="{ width: strengthWidth, background: strengthColor }"></span>
        </div>
        <div class="err">{{ errors.password }}</div>
      </div>
      <div class="field">
        <label for="regPw2">确认密码</label>
        <div class="input-wrap">
          <input
            id="regPw2"
            v-model="form.password2"
            :type="show.password2 ? 'text' : 'password'"
            placeholder="再次输入密码"
            autocomplete="new-password"
            :class="{ invalid: errors.password2 }"
          />
          <button type="button" class="toggle-pw" @click="show.password2 = !show.password2">
            {{ show.password2 ? '隐藏' : '显示' }}
          </button>
        </div>
        <div class="err">{{ errors.password2 }}</div>
      </div>
      <div class="field">
        <label for="regCaptcha">验证码</label>
        <div class="captcha-row">
          <div class="input-wrap" style="flex:1">
            <input
              id="regCaptcha"
              v-model="form.captcha"
              type="text"
              placeholder="输入右图字符"
              maxlength="4"
              autocomplete="off"
              :class="{ invalid: errors.captcha }"
            />
          </div>
          <img
            class="captcha-img"
            :src="captchaUrl"
            title="看不清？点击换一张"
            alt="验证码"
            @click="refreshCaptcha"
          />
        </div>
        <div class="err">{{ errors.captcha }}</div>
      </div>
      <button type="submit" class="submit" ref="registerBtn" :class="{ popping: rainbow }" :disabled="submitting">
        {{ submitting ? '正在创建…' : '加入我们 🎀' }}
      </button>
    </form>

    <!-- AI 小马助手：登录页专属，点它开聊 -->
    <PonyAssistant />
  </div>
</template>

<script>
import { apiRegister } from '../auth'
import PonyAssistant from '../components/PonyAssistant.vue'

export default {
  name: 'RegisterView',
  components: { PonyAssistant },
  inject: ['toast'],
  data() {
    return {
      form: { name: '', password: '', password2: '', captcha: '' },
      show: { password: false, password2: false },
      errors: {},
      submitting: false,
      rainbow: false,
      captchaUrl: ''
    }
  },
  mounted() {
    this.refreshCaptcha()
  },
  computed: {
    strengthScore() {
      const v = this.form.password
      let score = 0
      if (v.length >= 6) score++
      if (/[a-z]/i.test(v) && /[0-9]/.test(v)) score++
      if (v.length >= 10 || /[^a-zA-Z0-9]/.test(v)) score++
      return score
    },
    strengthWidth() {
      return (this.strengthScore / 3) * 100 + '%'
    },
    strengthColor() {
      const colors = ['#e06666', '#f0a93a', '#4caf7d']
      return colors[this.strengthScore - 1] || '#eee'
    }
  },
  methods: {
    refreshCaptcha() {
      // 加时间戳防止浏览器缓存
      this.captchaUrl = '/api/auth/captcha?t=' + Date.now()
    },
    burstParticles() {
      const layer = this.$refs.particleLayer
      const btn = this.$refs.registerBtn
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
    async handleRegister() {
      // 点击即从按钮中心炸开一簇彩虹粒子
      this.burstParticles()
      this.rainbow = false
      this.$nextTick(() => {
        this.rainbow = true
        setTimeout(() => { this.rainbow = false }, 450)
      })
      const name = this.form.name.trim()
      const pw = this.form.password
      const pw2 = this.form.password2
      const captcha = this.form.captcha.trim()
      this.errors = {}
      if (name.length < 2 || name.length > 20) { this.errors.name = '名字需 2-20 个字符'; return }
      if (pw.length < 6 || !(/[a-z]/i.test(pw) && /[0-9]/.test(pw))) {
        this.errors.password = '密码至少 6 位，且含字母和数字'; return
      }
      if (pw !== pw2) { this.errors.password2 = '两次输入的密码不一致'; return }
      if (captcha.length !== 4) { this.errors.captcha = '请输入 4 位验证码'; return }

      if (this.submitting) return
      this.submitting = true
      // 交给后端：Spring Boot /api/auth/register -> MyBatis-Plus -> MySQL(t_user)
      const res = await apiRegister({ name, password: pw, captcha })
      this.submitting = false

      if (res.code !== 200) {
        const msg = res.msg || '注册失败，请稍后再试'
        if (msg.includes('验证码')) this.errors.captcha = msg
        else if (msg.includes('密码')) this.errors.password = msg
        else this.errors.name = msg
        this.toast(msg, 'err')
        // 验证码一次性有效，失败后必须换新的
        this.form.captcha = ''
        this.refreshCaptcha()
        return
      }

      this.toast('注册成功，快去登录吧 🎉', 'ok')
      // 登录页会读 query.name 预填进去，省得再手打一遍
      this.$router.push({ name: 'login', query: { name } })
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
