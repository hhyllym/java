<template>
  <div class="home">
    <!-- 顶部：logo + 问候 + 退出 -->
    <div class="home-top">
      <div class="logo">🦄</div>
      <div class="home-greet">
        <h2>你好，{{ session.name || '小马' }}！</h2>
        <p>小马王国图鉴 · 欢迎回来 ✨</p>
      </div>
      <button class="logout" title="退出登录" @click="handleLogout">退出</button>
    </div>

    <!-- 导航栏 -->
    <nav class="navbar">
      <router-link to="/home" class="nav-item active">🏠 首页</router-link>
      <router-link to="/home" class="nav-item" @click.native="scrollTop">📚 图鉴</router-link>
      <router-link to="/learn" class="nav-item">🎓 学院</router-link>
      <router-link to="/fortune" class="nav-item">🎲 幸运</router-link>
      <router-link to="/shop" class="nav-item">🧧 商店</router-link>
      <router-link to="/friends" class="nav-item">
        👥 好友<span v-if="chatUnread > 0" class="nav-badge">{{ chatUnread > 99 ? '99+' : chatUnread }}</span>
      </router-link>
      <router-link to="/message" class="nav-item">
        💬 消息<span v-if="unread > 0" class="nav-badge">{{ unread > 99 ? '99+' : unread }}</span>
      </router-link>
      <router-link to="/dashboard" class="nav-item">🌟 我的</router-link>
      <router-link v-if="session.permission === 0 || session.permission === 2" to="/admin" class="nav-item admin-entry">🛠 后台管理</router-link>
    </nav>

    <!-- 统计条（全部来自后端 /api/home/overview） -->
    <div class="stat-bar">
      <div class="stat"><b>{{ stats.ponyTotal || 0 }}</b><span>图鉴条目</span></div>
      <div class="stat"><b>{{ stats.categoryTotal || 0 }}</b><span>分类</span></div>
      <div class="stat"><b>{{ stats.tagTotal || 0 }}</b><span>标签</span></div>
      <div class="stat"><b>{{ stats.memberTotal || 0 }}</b><span>注册小马</span></div>
    </div>

    <!-- 搜索栏 -->
    <form class="search" @submit.prevent="doSearch">
      <input
        v-model.trim="keyword"
        type="text"
        placeholder="搜名字 / 标签 / 英文名…"
        maxlength="20"
      />
      <button type="submit" title="搜索">🔍</button>
    </form>

    <!-- 热搜词：读后端 t_search_word，每次搜索都会自己长 -->
    <div class="chips" v-if="hotWords.length">
      <span class="chips-label">热搜</span>
      <button
        v-for="w in hotWords"
        :key="w.word"
        class="chip"
        :class="{ on: keyword === w.word }"
        @click="searchWord(w.word)"
      >{{ w.word }}<i>{{ w.hitCount }}</i></button>
    </div>

    <!-- 分级分类筛选：先点一级分类，再点它下面的二级分类 -->
    <div class="filters">
      <div class="filter-label">按分类浏览 · 一级 → 二级</div>
      <div class="filter-row">
        <button class="fx" :class="{ on: !level1Id }" @click="pickLevel1(null)">全部</button>
        <button
          v-for="c in categoryTree"
          :key="c.id"
          class="fx"
          :class="{ on: level1Id === c.id }"
          @click="pickLevel1(c.id)"
        >{{ c.icon }} {{ c.name }}<em>{{ c.count }}</em></button>
      </div>
      <div class="filter-row" v-if="subCategories.length">
        <button class="fx sub" :class="{ on: !categoryId }" @click="pickLevel2(null)">不限</button>
        <button
          v-for="c in subCategories"
          :key="c.id"
          class="fx sub"
          :class="{ on: categoryId === c.id }"
          @click="pickLevel2(c.id)"
        >{{ c.icon }} {{ c.name }}<em>{{ c.count }}</em></button>
      </div>
      <div class="crumbs">
        <a @click="resetFilter">« 全部小马</a>
        <template v-for="(cr, i) in crumbs" :key="cr.id">
          <span class="sep">›</span>
          <a :class="{ cur: i === crumbs.length - 1 }" @click="jumpCrumb(cr)">{{ cr.name }}</a>
        </template>
      </div>
    </div>

    <!-- 热门小马（后端按被查看次数排的热度榜） -->
    <div class="hot-row" v-if="recommends.length">
      <div class="filter-label">🔥 热门小马 · 点开看详情</div>
      <div class="hot-scroll">
        <button v-for="p in recommends" :key="p.id" class="hot-item" @click="openDetail(p)">
          <span class="hot-face" :style="{ '--pc': p.color }">
            <CutieMark v-if="hasMark(p.mark)" :mark="p.mark" />
            <template v-else>{{ p.emoji }}</template>
          </span>
          <em>{{ p.name }}</em>
        </button>
      </div>
    </div>

    <!-- 结果提示 -->
    <div class="result-tip">{{ tipText }}</div>

    <!-- 结果网格 -->
    <div class="pony-grid">
      <div
        v-for="p in list"
        :key="p.id"
        class="pony-card"
        :style="{ '--pc': p.color }"
        @click="openDetail(p)"
      >
        <span class="rarity" :class="rarityClass(p.rarity)">{{ p.rarity }}</span>
        <div class="pony-face">
          <CutieMark v-if="hasMark(p.mark)" :mark="p.mark" />
          <template v-else>{{ p.emoji }}</template>
        </div>
        <div class="pony-name">{{ p.name }}</div>
        <div class="pony-tags">{{ labelOf(p) }}</div>
      </div>
      <div v-if="!loading && list.length === 0" class="empty">{{ emptyText }}</div>
    </div>

    <button class="more" v-if="hasMore" :disabled="loading" @click="loadMore">
      {{ loading ? '加载中…' : `加载更多（${list.length}/${total}）` }}
    </button>

    <!-- 详情弹窗：Teleport 到 body，避免被 .card 的滚动容器裁掉 -->
    <Teleport to="body">
      <div v-if="detail" class="sheet-mask" @click.self="detail = null">
        <div class="sheet">
          <div class="sheet-top">
            <span class="sheet-face" :style="{ '--pc': detail.pony.color }">
              <CutieMark v-if="hasMark(detail.pony.mark)" :mark="detail.pony.mark" />
              <template v-else>{{ detail.pony.emoji }}</template>
            </span>
            <div class="sheet-title">
              <h3>{{ detail.pony.name }}</h3>
              <p>{{ detail.pony.alias }}</p>
            </div>
            <span class="rarity inline" :class="rarityClass(detail.pony.rarity)">{{ detail.pony.rarity }}</span>
            <button class="sheet-close" title="关闭" @click="detail = null">✕</button>
          </div>

          <p class="sheet-intro">{{ detail.pony.intro }}</p>

          <div class="sheet-meta">
            <span>👀 被查看 {{ detail.pony.viewCount }} 次</span>
            <span>🏷 {{ (detail.pony.tags || []).length }} 个标签</span>
            <span>📚 {{ (detail.pony.categories || []).length }} 个分类</span>
          </div>

          <div class="sheet-sec" v-if="(detail.pony.categories || []).length">
            <h4>所属分类（点一下按它筛选）</h4>
            <div class="kv">
              <button
                v-for="c in detail.pony.categories"
                :key="c.id"
                @click="searchByCategory(c)"
              >{{ c.parentIcon }}{{ c.parentName }} · {{ c.icon }}{{ c.name }}</button>
            </div>
          </div>

          <div class="sheet-sec" v-if="(detail.pony.tags || []).length">
            <h4>关键词标签</h4>
            <div class="kv">
              <button v-for="t in detail.pony.tags" :key="t" @click="searchByWord(t)">#{{ t }}</button>
            </div>
          </div>

          <div class="sheet-sec" v-if="(detail.related || []).length">
            <h4>同类小马</h4>
            <div class="kv">
              <button v-for="r in detail.related" :key="r.id" @click="openDetail(r)">
                {{ r.emoji }} {{ r.name }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { getSession, clearSession } from '../auth'
import { apiOverview, apiSearch, apiPonyDetail } from '../home'
import { apiUnread } from '../message'
import { apiPresenceOffline } from '../presence'
import { apiChatUnread } from '../chat'
import CutieMark from '../components/CutieMark.vue'

/** CutieMark.vue 里真正画了图案的几种可爱标志，其余用 emoji 兜底 */
const MARKS = ['pinkie', 'twilight', 'dash', 'applejack', 'fluttershy', 'rarity']
const RARITY_CLASS = { 常见: 'rr-common', 稀有: 'rr-rare', 传说: 'rr-legend' }
const PAGE_SIZE = 12

export default {
  name: 'HomeView',
  components: { CutieMark },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},

      // 列表与搜索状态
      keyword: '',
      list: [],
      total: 0,
      page: 1,
      hasMore: false,
      loading: false,
      tip: '',

      // 首页展示数据（后端）
      stats: {},
      categoryTree: [],
      hotWords: [],
      recommends: [],

      // 分级分类筛选：level1Id 是一级分类，categoryId 是它下面的二级分类
      level1Id: null,
      categoryId: null,

      detail: null,

      /** 导航栏「消息」未读红点 */
      unread: 0,

      /** 导航栏「好友」聊天未读红点 */
      chatUnread: 0,
      chatTimer: null,

      /** 请求序号：切筛选切得快时，用它丢弃过期响应 */
      seq: 0
    }
  },
  computed: {
    /** 当前一级分类下的二级分类 */
    subCategories() {
      const node = this.categoryTree.find(c => c.id === this.level1Id)
      return node ? node.children : []
    },
    /** 面包屑：不限 › 种族 › 飞马 */
    crumbs() {
      const arr = []
      const l1 = this.categoryTree.find(c => c.id === this.level1Id)
      if (l1) arr.push({ type: 'l1', id: l1.id, name: l1.name })
      if (this.categoryId) {
        const l2 = this.subCategories.find(c => c.id === this.categoryId)
        if (l2) arr.push({ type: 'l2', id: l2.id, name: l2.name })
      }
      return arr
    },
    tipText() {
      if (this.tip) return this.tip
      if (this.loading && this.page === 1) return '正在从后端捞数据…'
      const parts = []
      if (this.keyword) parts.push(`关键词「${this.keyword}」`)
      if (this.crumbs.length) parts.push('分类 ' + this.crumbs.map(c => c.name).join(' › '))
      if (parts.length) return `${parts.join(' + ')}：共 ${this.total} 位小马`
      return `王国成员 · 全部 ${this.total} 位`
    },
    emptyText() {
      return this.keyword
        ? `没找到和「${this.keyword}」有关的小马，换个词试试 🥺`
        : '这个分类下还没有小马 🥺'
    }
  },
  async created() {
    this.loadPage(true)
    await this.loadOverview()   // 先拿分类树，applyQuery 要用它判断一级/二级
    this.applyQuery()
    this.refreshUnread()
    this.refreshChatUnread()
    // 每 10 秒刷一次聊天未读红点（离开页面时清掉，见 beforeUnmount）
    this.chatTimer = setInterval(this.refreshChatUnread, 10000)
  },
  beforeUnmount() {
    if (this.chatTimer) clearInterval(this.chatTimer)
  },
  methods: {
    /** 支持从别的页面带参进来：/home?pony=2 直接开详情，/home?category=12 直接筛分类 */
    async applyQuery() {
      const q = this.$route.query || {}
      if (q.category) {
        const id = Number(q.category)
        const l1 = this.categoryTree.find(c => c.id === id)
        const parent = this.categoryTree.find(c => (c.children || []).some(s => s.id === id))
        if (parent) {
          this.level1Id = parent.id
          this.categoryId = id
          this.loadPage(true)
        } else if (l1) {
          this.pickLevel1(id)
        }
      }
      if (q.pony) {
        const res = await apiPonyDetail(Number(q.pony))
        if (res.code === 200) this.detail = res.data
      }
    },
    hasMark(mark) {
      return MARKS.indexOf(mark) >= 0
    },
    rarityClass(rarity) {
      return RARITY_CLASS[rarity] || 'rr-common'
    },
    /** 卡片上那行小字：飞马 · 忠诚 · 云中城 */
    labelOf(pony) {
      const names = (pony.categories || []).filter(c => c.level >= 2).map(c => c.name)
      return names.length ? names.join(' · ') : (pony.alias || '')
    },

    /** 首页展示数据（统计 / 分类树 / 热搜词 / 热门小马） */
    async loadOverview() {
      const res = await apiOverview()
      if (res.code !== 200) {
        this.toast(res.msg || '首页数据加载失败', 'err')
        return
      }
      const d = res.data || {}
      this.stats = d.stats || {}
      this.categoryTree = d.categoryTree || []
      this.hotWords = d.hotWords || []
      this.recommends = d.recommends || []
    },

    /** 拉列表；reset=true 表示从第一页重新来 */
    async loadPage(reset) {
      // 不拦「正在加载」——用户快速切换筛选时，后面的请求应该生效
      const seq = ++this.seq
      this.loading = true
      if (reset) {
        this.page = 1
        this.list = []
      }
      const res = await apiSearch({
        keyword: this.keyword,
        // 没选二级分类就用一级分类（后端会自动把子孙分类一起算上）
        categoryId: this.categoryId || this.level1Id,
        page: this.page,
        size: PAGE_SIZE
      })
      if (seq !== this.seq) return          // 已经有更新的请求了，这次结果作废
      this.loading = false
      if (res.code !== 200) {
        this.toast(res.msg || '搜索失败', 'err')
        return
      }
      const d = res.data || {}
      this.total = d.total || 0
      this.hasMore = !!d.hasMore
      this.tip = d.tip || ''
      this.list = reset ? (d.list || []) : this.list.concat(d.list || [])
    },

    loadMore() {
      if (!this.hasMore || this.loading) return
      this.page += 1
      this.loadPage(false)
    },

    doSearch() {
      this.loadPage(true)
      this.refreshHotWords()
    },
    searchWord(word) {
      this.keyword = word
      this.loadPage(true)
      this.refreshHotWords()
    },

    pickLevel1(id) {
      this.level1Id = id
      this.categoryId = null
      this.loadPage(true)
    },
    pickLevel2(id) {
      this.categoryId = id
      this.loadPage(true)
    },
    resetFilter() {
      this.level1Id = null
      this.categoryId = null
      this.keyword = ''
      this.loadPage(true)
    },
    jumpCrumb(crumb) {
      if (crumb.type === 'l1') this.pickLevel1(crumb.id)
      else this.pickLevel2(crumb.id)
    },

    /** 详情弹窗里点分类 → 关掉弹窗，按这个分类筛 */
    searchByCategory(category) {
      this.detail = null
      if (category.level >= 2) {
        this.level1Id = category.parentId
        this.categoryId = category.id
      } else {
        this.level1Id = category.id
        this.categoryId = null
      }
      this.keyword = ''
      this.loadPage(true)
      this.scrollTop()
    },
    /** 详情弹窗里点标签 → 关掉弹窗，按这个关键词搜 */
    searchByWord(tag) {
      this.detail = null
      this.keyword = tag
      this.loadPage(true)
      this.refreshHotWords()
      this.scrollTop()
    },

    /** 搜索会累加热搜次数，重新拉一下让首页热搜词保持最新 */
    async refreshHotWords() {
      const res = await apiOverview()
      if (res.code === 200 && res.data) {
        this.hotWords = res.data.hotWords || this.hotWords
        this.stats = res.data.stats || this.stats
      }
    },

    async openDetail(pony) {
      const res = await apiPonyDetail(pony.id)
      if (res.code !== 200) {
        this.toast(res.msg || '详情加载失败', 'err')
        return
      }
      this.detail = res.data
    },

    scrollTop() {
      const card = this.$el && this.$el.closest ? this.$el.closest('.card') : null
      if (card && card.scrollTo) card.scrollTo({ top: 0, behavior: 'smooth' })
    },

    /** 消息中心未读红点（原来只有调用没定义，消息红点一直不刷新，补上） */
    async refreshUnread() {
      const res = await apiUnread(this.session.id)
      if (res.code === 200) this.unread = res.data || 0
    },

    /** 好友聊天未读红点 */
    async refreshChatUnread() {
      const res = await apiChatUnread(this.session.id)
      if (res.code === 200) this.chatUnread = res.data || 0
    },

    comingSoon(name) {
      this.toast(name + ' 还在建设中，敬请期待 🚧', 'ok')
    },
    handleLogout() {
      apiPresenceOffline(this.session.id)
      clearSession()
      this.toast('已退出登录，拜拜 👋', 'ok')
      this.$router.push({ name: 'login' })
    }
  }
}
</script>

<style scoped>
.nav-item.admin-entry {
  background: linear-gradient(135deg, #a06cd5, #c89bf0);
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(160, 108, 213, .35);
}
.nav-item.admin-entry:hover {
  filter: brightness(1.05);
}
</style>
