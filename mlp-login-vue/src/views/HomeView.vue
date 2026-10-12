<template>
  <div class="home">
    <!-- 顶部：logo + 问候 + 退出 -->
    <div class="home-top">
      <div class="logo">🦄</div>
      <div class="home-greet">
        <h2>你好，{{ session.name || '小马' }}！</h2>
        <p>欢迎回到小马王国 ✨</p>
      </div>
      <button class="logout" title="退出登录" @click="handleLogout">退出</button>
    </div>

    <!-- 导航栏 -->
    <nav class="navbar">
      <router-link to="/home" class="nav-item active">🏠 首页</router-link>
      <router-link to="/dashboard" class="nav-item">🌟 我的账号</router-link>
      <a class="nav-item disabled" @click="comingSoon('小马图鉴')">📚 图鉴</a>
      <a class="nav-item disabled" @click="comingSoon('魔法商店')">🍭 商店</a>
    </nav>

    <!-- 搜索栏 -->
    <form class="search" @submit.prevent="doSearch">
      <input
        v-model.trim="keyword"
        type="text"
        placeholder="搜索小马、魔法、地点…"
        maxlength="20"
      />
      <button type="submit" title="搜索">🔍</button>
    </form>

    <!-- 热搜词 -->
    <div class="chips">
      <button
        v-for="c in hotWords"
        :key="c"
        class="chip"
        :class="{ on: keyword === c }"
        @click="searchFor(c)"
      >{{ c }}</button>
    </div>

    <!-- 搜索结果 / 全部小马（纯前端演示数据） -->
    <div class="result-tip">
      <template v-if="searched">
        「{{ searched }}」的搜索结果：{{ results.length }} 位小马
      </template>
      <template v-else>王国成员 · 点上面的词条试试 👆</template>
    </div>

    <div class="pony-grid">
      <div v-for="p in results" :key="p.name" class="pony-card" :style="{ '--pc': p.color }">
        <div class="pony-face">{{ p.emoji }}</div>
        <div class="pony-name">{{ p.name }}</div>
        <div class="pony-tags">{{ p.tags.join(' · ') }}</div>
      </div>
      <div v-if="results.length === 0" class="empty">
        没找到相关小马，换个词试试吧 🥺
      </div>
    </div>
  </div>
</template>

<script>
import { getSession, clearSession } from '../auth'

/** 首页演示数据：纯前端写死，不走后端接口 */
const PONIES = [
  { emoji: '🩷', name: '碧琪',   tags: ['欢笑', '派对', '甜点'], color: '#f7a8c4' },
  { emoji: '💜', name: '紫悦',   tags: ['魔法', '读书', '星光'], color: '#b79cf0' },
  { emoji: '💙', name: '云宝',   tags: ['飞行', '忠诚', '竞速'], color: '#8fc7f0' },
  { emoji: '🧡', name: '苹果嘉儿', tags: ['苹果', '诚实', '农场'], color: '#f0a06a' },
  { emoji: '💚', name: '柔柔',   tags: ['温柔', '小动物', '森林'], color: '#a7e3c9' },
  { emoji: '🤍', name: '珍奇',   tags: ['时尚', '慷慨', '宝石'], color: '#f7e08a' }
]

export default {
  name: 'HomeView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      keyword: '',
      searched: '',
      hotWords: ['魔法', '飞行', '派对', '苹果', '时尚'],
      ponies: PONIES
    }
  },
  computed: {
    results() {
      const kw = (this.searched || '').toLowerCase()
      if (!kw) return this.ponies
      return this.ponies.filter(
        p =>
          p.name.includes(this.searched) ||
          p.tags.some(t => t.toLowerCase().includes(kw))
      )
    }
  },
  methods: {
    doSearch() {
      this.searched = this.keyword
      if (!this.searched) {
        this.toast('先输入要搜索的内容哦～', 'err')
      } else if (this.results.length === 0) {
        this.toast('没有找到「' + this.searched + '」相关的小马', 'err')
      } else {
        this.toast('找到 ' + this.results.length + ' 位小马 🎉', 'ok')
      }
    },
    searchFor(word) {
      this.keyword = word
      this.doSearch()
    },
    comingSoon(name) {
      this.toast(name + ' 还在建设中，敬请期待 🚧', 'ok')
    },
    handleLogout() {
      clearSession()
      this.toast('已退出登录，拜拜 👋', 'ok')
      this.$router.push({ name: 'login' })
    }
  }
}
</script>
