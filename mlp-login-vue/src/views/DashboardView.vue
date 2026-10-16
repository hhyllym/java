<template>
  <div class="dash">
    <div class="avatar">🦄</div>
    <h2>{{ profile.name || session.name || '小马' }}</h2>
    <div class="meta">{{ lastLoginText ? '上次登录：' + lastLoginText : '欢迎回来～' }}</div>

    <!-- 账号资料（后端 /api/user/profile） -->
    <div class="dash-kv">
      <div>
        <span>注册时间</span>
        <b>{{ profile.createTime || '—' }}</b>
      </div>
      <div>
        <span>加入天数</span>
        <b>{{ profile.days ? '第 ' + profile.days + ' 天' : '—' }}</b>
      </div>
    </div>

    <!-- 同名小马：图鉴里有同名的就亮出来 -->
    <div class="dash-sec">
      <h4>你的同名小马</h4>

      <div v-if="loadingPony" class="dash-tip">正在翻图鉴…</div>

      <div v-else-if="profile.pony" class="dash-pony" @click="openPony">
        <span class="dash-face" :style="{ background: profile.pony.color || '#f3e8ff' }">
          <CutieMark v-if="hasMark(profile.pony.mark)" :mark="profile.pony.mark" />
          <span v-else class="dash-emoji">{{ profile.pony.emoji || '🐴' }}</span>
        </span>
        <div class="dash-pony-main">
          <p>
            <b>{{ profile.pony.name }}</b>
            <span class="rarity inline" :class="rarityClass(profile.pony.rarity)">{{ profile.pony.rarity }}</span>
          </p>
          <p class="dash-alias">{{ profile.pony.alias || '' }}</p>
          <div class="kv">
            <button v-for="c in (profile.pony.categories || [])" :key="c.id" @click.stop="goCategory(c)">
              {{ c.parentName ? c.parentName + ' · ' : '' }}{{ c.name }}
            </button>
          </div>
        </div>
      </div>

      <div v-else-if="apiError" class="dash-tip err">{{ apiError }}</div>

      <div v-else class="dash-tip">
        图鉴里还没有同名的你 🐴<br />
        <span>去首页逛逛，或者换个更像你的名字～</span>
      </div>
    </div>

    <div class="dash-actions">
      <button @click="$router.push({ name: 'home' })">去图鉴逛逛</button>
      <button @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<script>
import { getSession, clearSession } from '../auth'
import { apiProfile } from '../user'
import CutieMark from '../components/CutieMark.vue'

const MARKS = ['pinkie', 'twilight', 'dash', 'applejack', 'fluttershy', 'rarity']
const RARITY_CLASS = { 常见: 'rr-common', 稀有: 'rr-rare', 传说: 'rr-legend' }

export default {
  name: 'DashboardView',
  components: { CutieMark },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      loadingPony: true,
      apiError: '',
      profile: { id: null, name: '', createTime: '', days: 0, pony: null }
    }
  },
  computed: {
    lastLoginText() {
      if (!this.session.ts) return ''
      return new Date(this.session.ts).toLocaleString('zh-CN')
    }
  },
  created() {
    this.loadProfile()
  },
  methods: {
    hasMark(mark) {
      return MARKS.indexOf(mark) >= 0
    },
    rarityClass(rarity) {
      return RARITY_CLASS[rarity] || 'rr-common'
    },
    async loadProfile() {
      const id = this.session.id
      if (!id) {
        this.loadingPony = false
        this.apiError = '本地没有会话信息，重新登录一下'
        return
      }
      const r = await apiProfile(id)
      this.loadingPony = false
      if (r.code !== 200) {
        this.apiError = r.msg || '账号资料获取失败'
        return
      }
      this.profile = r.data || this.profile
    },
    openPony() {
      this._detailId = this.profile.pony.id
      // 详情弹窗在首页，这里直接跳过去并带上要打开的条目
      this.$router.push({ name: 'home', query: { pony: this.profile.pony.id } })
    },
    goCategory(c) {
      this.$router.push({ name: 'home', query: { category: c.id } })
    },
    handleLogout() {
      clearSession()
      this.toast('已退出登录，拜拜 👋', 'ok')
      this.$router.push({ name: 'login' })
    }
  }
}
</script>
