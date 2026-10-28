<template>
  <div class="admin-login">
    <div class="login-card">
      <div class="badge">🛠 后台管理系统</div>
      <div class="logo">🦄</div>
      <h1>管理员登录</h1>
      <p class="sub">仅限管理员进入 · 普通用户请回首页</p>

      <form novalidate @submit.prevent="handleLogin">
        <div class="field">
          <label for="adminName">管理员账号</label>
          <input
            id="adminName"
            v-model="form.username"
            type="text"
            placeholder="请输入管理员账号"
            autocomplete="username"
            :class="{ invalid: errors.username }"
          />
          <div class="err">{{ errors.username }}</div>
        </div>

        <div class="field">
          <label for="adminPw">管理员密码</label>
          <div class="pw-wrap">
            <input
              id="adminPw"
              v-model="form.password"
              :type="show ? 'text' : 'password'"
              placeholder="请输入管理员密码"
              autocomplete="current-password"
              :class="{ invalid: errors.password }"
            />
            <button type="button" class="toggle" @click="show = !show">{{ show ? '隐藏' : '显示' }}</button>
          </div>
          <div class="err">{{ errors.password }}</div>
        </div>

        <button type="submit" class="submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '进入后台管理系统' }}
        </button>
      </form>

      <router-link to="/home" class="back">« 返回首页</router-link>
      <div class="hint">默认超级管理员 admin · 密码 Admin@1234（明文，在 sql\mlp_login.sql 中预置，可在库里改）</div>
    </div>
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
      submitting: false
    }
  },
  methods: {
    async handleLogin() {
      if (this.submitting) return
      this.errors = {}
      const username = this.form.username.trim()
      const password = this.form.password
      if (!username) { this.errors.username = '请输入管理员账号'; return }
      if (!password) { this.errors.password = '请输入管理员密码'; return }

      this.submitting = true
      const res = await apiAdminLogin(username, password)
      this.submitting = false

      if (res.code !== 200) {
        this.errors.password = res.msg || '登录失败'
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
.admin-login {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.login-card {
  width: 100%;
  max-width: 380px;
  background: var(--card, #fff);
  border-radius: var(--radius, 18px);
  box-shadow: var(--shadow, 0 16px 40px rgba(160, 108, 213, .18));
  padding: 28px 26px 22px;
  text-align: center;
}
.badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 800;
  color: #7c5cbf;
  background: #efe9fb;
  border-radius: 999px;
  padding: 5px 12px;
  margin-bottom: 10px;
}
.logo { font-size: 46px; line-height: 1; margin-bottom: 4px; }
.login-card h1 { margin: 6px 0 2px; font-size: 22px; color: var(--text, #4a3f5c); }
.sub { margin: 0 0 18px; font-size: 12.5px; color: var(--muted, #9088a0); }
.field { text-align: left; margin-bottom: 14px; }
.field label { display: block; font-size: 13px; font-weight: 700; color: var(--text, #4a3f5c); margin-bottom: 6px; }
.field input {
  width: 100%;
  box-sizing: border-box;
  padding: 11px 13px;
  border: 1.5px solid var(--border, #ece6f3);
  border-radius: 12px;
  font-size: 14px;
  background: #fff;
  color: var(--text, #4a3f5c);
  outline: none;
}
.field input:focus { border-color: var(--primary, #a06cd5); }
.field input.invalid { border-color: var(--danger, #e06666); }
.pw-wrap { position: relative; }
.pw-wrap .toggle {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  border: none; background: transparent; color: var(--primary, #a06cd5);
  font-size: 12px; cursor: pointer; font-weight: 700;
}
.err { color: var(--danger, #e06666); font-size: 12px; min-height: 16px; margin-top: 4px; }
.submit {
  width: 100%;
  margin-top: 4px;
  padding: 12px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--primary, #a06cd5), #c89bf0);
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: .2s;
}
.submit:hover { filter: brightness(1.05); }
.submit:disabled { opacity: .7; cursor: default; }
.back { display: inline-block; margin-top: 14px; font-size: 13px; color: var(--primary, #a06cd5); text-decoration: none; font-weight: 700; }
.hint { margin-top: 10px; font-size: 11px; color: var(--muted, #9088a0); }
</style>
