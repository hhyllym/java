<template>
  <div>
    <div class="particle-layer" ref="particleLayer"></div>
    <div class="head">
      <div class="logo">🦄</div>
      <h1>小马乐园</h1>
      <p>创建你的小马账号，一起住进星糖谷 ✨</p>
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
      <!-- 用户协议勾选：不勾不允许注册 -->
      <label class="agree-row">
        <input type="checkbox" v-model="form.agree" />
        <span>
          我已阅读并同意
          <a href="#" @click.prevent.stop="openAgreement">《用户协议》</a>和<a href="#" @click.prevent.stop="openAgreement">《隐私政策》</a>
        </span>
      </label>
      <div class="err">{{ errors.agree }}</div>

      <button type="submit" class="submit" ref="registerBtn" :class="{ popping: rainbow }" :disabled="submitting">
        {{ submitting ? '正在创建…' : '加入我们 🎀' }}
      </button>
    </form>

    <!-- 协议弹窗 -->
    <Teleport to="body">
      <div v-if="agreeOpen" class="agree-mask" @click.self="agreeOpen = false">
        <div class="agree-dialog">
          <h3>小马乐园 · 用户协议与隐私政策</h3>
          <div class="agree-body">
            <p><b>【用户协议】</b>本站为课程实训演示项目。注册即视为同意友善使用：不冒充他人、不发不当内容、不利用漏洞刷积分。管理员可对违规账号进行处理。</p>
            <p><b>【隐私政策】</b>我们仅收集昵称与密码（本地数据库存储，密码按需求明文保存，仅用于教学演示），不收集真实姓名、手机号等敏感信息，也不会向任何第三方提供你的数据。聊天与游戏记录仅保存在本地数据库中。</p>
            <p>仍在施工中的小镇规则会不定期更新，更新后会在首页公告～ 🐴</p>
          </div>
          <button class="agree-btn" @click="agreeOpen = false">我知道了</button>
        </div>
      </div>
    </Teleport>

  </div>
</template>

<script>
import { apiRegister } from '../auth'

export default {
  name: 'RegisterView',
  inject: ['toast'],
  data() {
    return {
      form: { name: '', password: '', password2: '', captcha: '', agree: false },
      show: { password: false, password2: false },
      errors: {},
      submitting: false,
      rainbow: false,
      captchaUrl: '',
      agreeOpen: false
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
/* 用户协议勾选行 */
.agree-row {
  display: flex; align-items: flex-start; gap: 7px;
  font-size: 12.5px; color: var(--muted);
  margin: 2px 0 0; cursor: pointer; line-height: 1.55;
  user-select: none;
}
.agree-row input { margin-top: 2px; accent-color: var(--primary); cursor: pointer; }
.agree-row a { color: var(--primary); text-decoration: none; font-weight: 700; }
.agree-row a:hover { text-decoration: underline; }
/* 协议弹窗 */
.agree-mask {
  position: fixed; inset: 0; z-index: 9998;
  background: rgba(60, 40, 90, .38);
  display: flex; align-items: center; justify-content: center; padding: 20px;
  animation: agreeFade .18s ease;
}
@keyframes agreeFade { from { opacity: 0; } to { opacity: 1; } }
.agree-dialog {
  background: var(--card, #fff); border-radius: 18px;
  max-width: 420px; width: 100%; padding: 20px 22px;
  box-shadow: 0 18px 50px rgba(60, 40, 90, .3);
  animation: agreePop .22s cubic-bezier(.22,.61,.36,1);
}
@keyframes agreePop { from { transform: translateY(14px) scale(.96); opacity: 0; } to { transform: none; opacity: 1; } }
.agree-dialog h3 { margin: 0 0 10px; font-size: 15.5px; color: var(--primary, #a06cd5); }
.agree-body { max-height: 46vh; overflow-y: auto; }
.agree-body p { margin: 0 0 9px; font-size: 12.5px; line-height: 1.7; color: #6b5b85; }
.agree-body b { color: var(--primary, #a06cd5); }
.agree-btn {
  width: 100%; margin-top: 8px; padding: 10px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, var(--pink, #f7a8c4), var(--purple, #b79cf0));
  color: #fff; font-weight: 700; font-size: 13.5px; cursor: pointer;
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
