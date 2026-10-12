<template>
  <div>
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
      <div class="field">
        <label for="loginCaptcha">验证码</label>
        <div class="captcha-row">
          <div class="input-wrap" style="flex:1">
            <input
              id="loginCaptcha"
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
      <div class="row">
        <label><input type="checkbox" v-model="form.remember" /> 记住我</label>
        <a href="#" @click.prevent="handleForgot">忘记魔法密码？</a>
      </div>
      <button type="submit" class="submit">进入小马王国 🌈</button>
    </form>
  </div>
</template>

<script>
import { setSession, apiLogin, captchaUrl } from '../auth'

export default {
  name: 'LoginView',
  inject: ['toast'],
  data() {
    return {
      form: { name: '', password: '', captcha: '', remember: false },
      show: { password: false },
      errors: {},
      submitting: false,
      captchaUrl: ''
    }
  },
  mounted() {
    this.refreshCaptcha()
    // 注册成功跳转会带上用户名，预填登录框
    const n = this.$route.query.name
    if (typeof n === 'string' && n) this.form.name = n
  },
  methods: {
    refreshCaptcha() {
      // 加时间戳防止浏览器缓存
      this.captchaUrl = '/api/auth/captcha?t=' + Date.now()
    },
    async handleLogin() {
      if (this.submitting) return
      const name = this.form.name.trim()
      const pw = this.form.password
      const captcha = this.form.captcha.trim()
      this.errors = {}
      if (!name) { this.errors.name = '请输入小马名字'; return }
      if (!pw) { this.errors.password = '请输入密码'; return }
      if (captcha.length !== 4) { this.errors.captcha = '请输入 4 位验证码'; return }

      this.submitting = true
      const res = await apiLogin({ name, password: pw, captcha })
      this.submitting = false

      if (res.code !== 200) {
        if (res.msg.includes('验证码')) this.errors.captcha = res.msg
        else if (res.msg.includes('还没注册') || res.msg.includes('名字')) this.errors.name = res.msg
        else this.errors.password = res.msg
        // 验证码一次性有效，失败后必须换新的
        this.form.captcha = ''
        this.refreshCaptcha()
        return
      }

      const user = res.data
      setSession({ id: user.id, name: user.name, ts: Date.now() }, this.form.remember)
      this.toast('登录成功，欢迎回家！🌟', 'ok')
      this.$router.push({ name: 'home' })
    },
    handleForgot() {
      const name = prompt('请输入你的小马名字，我们会发送重置链接（演示）：')
      if (name && name.trim()) {
        this.toast('重置链接已发送至 ' + name.trim() + ' 的预留渠道（演示）', 'ok')
      }
    }
  }
}
</script>
