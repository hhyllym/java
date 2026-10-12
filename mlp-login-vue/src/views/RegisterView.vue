<template>
  <div>
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
      <button type="submit" class="submit" :disabled="submitting">
        {{ submitting ? '正在创建…' : '加入我们 🎀' }}
      </button>
    </form>
  </div>
</template>

<script>
import { apiRegister } from '../auth'

export default {
  name: 'RegisterView',
  inject: ['toast'],
  data() {
    return {
      form: { name: '', password: '', password2: '' },
      show: { password: false, password2: false },
      errors: {},
      submitting: false
    }
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
    async handleRegister() {
      const name = this.form.name.trim()
      const pw = this.form.password
      const pw2 = this.form.password2
      this.errors = {}
      if (name.length < 2 || name.length > 20) { this.errors.name = '名字需 2-20 个字符'; return }
      if (pw.length < 6 || !(/[a-z]/i.test(pw) && /[0-9]/.test(pw))) {
        this.errors.password = '密码至少 6 位，且含字母和数字'; return
      }
      if (pw !== pw2) { this.errors.password2 = '两次输入的密码不一致'; return }

      if (this.submitting) return
      this.submitting = true
      // 交给后端：Spring Boot /api/auth/register -> MyBatis-Plus -> MySQL(t_user)
      const res = await apiRegister({ name, password: pw })
      this.submitting = false

      if (res.code !== 200) {
        const msg = res.msg || '注册失败，请稍后再试'
        if (msg.includes('密码')) this.errors.password = msg
        else this.errors.name = msg
        this.toast(msg, 'err')
        return
      }

      this.toast('注册成功，快去登录吧 🎉', 'ok')
      // 登录页会读 query.name 预填进去，省得再手打一遍
      this.$router.push({ name: 'login', query: { name } })
    }
  }
}
</script>
