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

    <!-- 切换账号提示条 -->
    <div v-if="switchHint" class="switch-hint">
      🔁 正在切换账号，上次登录的是「{{ lastAccount }}」<br />
      <span>要换别的账号，直接把名字和密码改成另一个即可</span>
    </div>

    <!-- AI 小马助手：登录页专属，点它开聊 -->
    <PonyAssistant />
  </div>
</template>

<script>
import { setSession, apiLogin, captchaUrl, rememberAccount, getRememberedAccount } from '../auth'
import PonyAssistant from '../components/PonyAssistant.vue'

export default {
  name: 'LoginView',
  components: { PonyAssistant },
  inject: ['toast'],
  data() {
    return {
      form: { name: '', password: '', captcha: '', remember: false },
      show: { password: false },
      errors: {},
      submitting: false,
      captchaUrl: '',
      switchHint: false,
      lastAccount: ''
    }
  },
  mounted() {
    this.refreshCaptcha()
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
    }
  }
}
</script>

<style scoped>
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
