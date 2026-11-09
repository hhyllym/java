<template>
  <div class="home friends">
    <!-- 顶部 -->
    <div class="home-top">
      <div class="logo">👥</div>
      <div class="home-greet">
        <h2>好友</h2>
        <p>加好友 · 一起冒险 · 随时聊天</p>
      </div>
      <button class="logout" @click="goHome">返回</button>
    </div>

    <!-- 概览 -->
    <div class="friend-stats">
      <div class="fs-item"><b>{{ friends.length }}</b><span>好友</span></div>
      <div class="fs-item"><b class="ok">{{ onlineCount }}</b><span>在线</span></div>
      <div class="fs-item"><b class="warn">{{ applies.length }}</b><span>待处理</span></div>
      <div class="fs-item"><b>{{ sentWaiting }}</b><span>已申请</span></div>
    </div>

    <!-- 三个标签页 -->
    <div class="tabs">
      <div class="tab" :class="{ active: tab === 'list' }" @click="tab = 'list'">
        我的好友<em v-if="friends.length">({{ friends.length }})</em>
      </div>
      <div class="tab" :class="{ active: tab === 'apply' }" @click="tab = 'apply'">
        新的朋友<em v-if="applies.length" class="red-dot">{{ applies.length }}</em>
      </div>
      <div class="tab" :class="{ active: tab === 'add' }" @click="tab = 'add'">➕ 添加</div>
    </div>

    <!-- ========== 好友列表 ========== -->
    <div v-show="tab === 'list'" class="tab-pane">
      <div class="list-toolbar">
        <input v-model.trim="listKeyword" class="mini-input" type="text" placeholder="🔍 找好友（备注 / 名字 / ID）" />
        <select v-model="sortBy" class="mini-select" title="排序方式">
          <option value="online">在线优先</option>
          <option value="recent">最近添加</option>
          <option value="name">按名字</option>
        </select>
        <button class="invite-btn" title="开一间四人联机大富翁房间" @click="openInvite()">🎲 邀请组队</button>
      </div>

      <div v-if="loadingList" class="empty">正在加载好友…</div>
      <div v-else-if="!friends.length" class="empty">还没有好友，去「添加」标签搜小马名字加好友吧 🐴</div>
      <div v-else-if="!visibleFriends.length" class="empty">没找到这只小马，换个关键词试试 🥺</div>
      <div v-else class="friend-list">
        <div v-for="f in visibleFriends" :key="f.id" class="friend-item" @click="openProfile(f)">
          <div class="friend-face-wrap">
            <span class="friend-face" :style="{ background: faceBg(f.name) }">{{ f.avatar || f.name.slice(0, 1) }}</span>
            <span v-if="onlineMap[f.id]" class="online-dot" title="在线"></span>
          </div>
          <div class="friend-main">
            <b>
              {{ displayName(f) }}
              <span v-if="relOf(f.relationType)" class="rel-badge" :title="relOf(f.relationType).name">
                {{ relOf(f.relationType).emoji }} {{ relOf(f.relationType).name }}
              </span>
            </b>
            <em>
              <span :class="onlineMap[f.id] ? 'st-on' : 'st-off'">{{ onlineMap[f.id] ? '● 在线' : '○ 离线' }}</span>
              · ID: {{ f.id }} · {{ f.since }} 成为好友<span v-if="f.remark"> · 原名 {{ f.name }}</span>
            </em>
          </div>
          <div class="friend-ops">
            <button class="op-chat" title="去聊天" @click.stop="openChat(f)">💬</button>
            <button class="op-gift" title="送 50 积分" @click.stop="giftPoints(f)">🧧</button>
            <button class="friend-del" title="删除好友" @click.stop="askDelete(f)">✕</button>
          </div>
        </div>
      </div>
      <p class="pane-tip">💡 点好友卡片可以看资料、互动徽章和游戏战绩对比</p>
    </div>

    <!-- ========== 新的朋友 ========== -->
    <div v-show="tab === 'apply'" class="tab-pane">
      <div class="section-title">📥 收到的申请 <em>{{ applies.length }}</em></div>
      <div v-if="loadingApply" class="empty">正在加载申请…</div>
      <div v-else-if="!applies.length" class="empty small">暂时没有新的好友申请 📭</div>
      <div v-else class="friend-list">
        <div v-for="a in applies" :key="a.id" class="friend-item">
          <span class="friend-face" :style="{ background: faceBg(a.name) }">{{ a.avatar || a.name.slice(0, 1) }}</span>
          <div class="friend-main">
            <b>{{ a.name }}</b>
            <em>{{ a.time }} · 「{{ a.applyMsg }}」</em>
          </div>
          <div class="apply-btns">
            <button class="btn-ok" @click="handleApply(a, true)">同意</button>
            <button class="btn-no" @click="handleApply(a, false)">拒绝</button>
          </div>
        </div>
      </div>

      <div class="section-title gap">📤 我发出的申请 <em>{{ sentApplies.length }}</em></div>
      <div v-if="loadingSent" class="empty small">正在加载…</div>
      <div v-else-if="!sentApplies.length" class="empty small">还没有发出去的申请 ✨</div>
      <div v-else class="friend-list">
        <div v-for="a in sentApplies" :key="a.id" class="friend-item">
          <span class="friend-face" :style="{ background: faceBg(a.name) }">{{ a.avatar || a.name.slice(0, 1) }}</span>
          <div class="friend-main">
            <b>{{ a.name }} <span class="state-chip" :class="a.state">{{ a.state === 'waiting' ? '等待对方同意' : '对方暂未通过' }}</span></b>
            <em>{{ a.time }} · 「{{ a.applyMsg }}」</em>
          </div>
          <div class="apply-btns">
            <button v-if="a.state === 'waiting'" class="btn-no" @click="cancelApply(a)">撤回</button>
            <button v-else class="btn-ok" @click="reapply(a)">再申请</button>
          </div>
        </div>
      </div>
      <p class="pane-tip">💡 申请每 15 秒自动刷新；撤回后对方就看不到这条申请了</p>
    </div>

    <!-- ========== 添加好友 ========== -->
    <div v-show="tab === 'add'" class="tab-pane">
      <form class="search" @submit.prevent="doSearch">
        <input v-model.trim="keyword" type="text" placeholder="输入小马名字或 ID 搜索…" maxlength="20" />
        <button type="submit" title="搜索">🔍</button>
      </form>
      <div class="apply-msg-row">
        <span>招呼语</span>
        <input v-model="applyMsg" type="text" maxlength="60" placeholder="很高兴认识你！" />
      </div>

      <div v-if="searching" class="empty small">正在搜索…</div>
      <div v-else-if="results.length" class="friend-list">
        <div v-for="u in results" :key="u.id" class="friend-item">
          <span class="friend-face" :style="{ background: faceBg(u.name) }">{{ u.avatar || u.name.slice(0, 1) }}</span>
          <div class="friend-main">
            <b>{{ u.name }}</b>
            <em>ID: {{ u.id }}</em>
          </div>
          <button class="btn-add" :disabled="u.relation !== 'none'" @click="applyTo(u)">{{ relationText(u.relation) }}</button>
        </div>
      </div>
      <div v-else-if="searched" class="empty small">没找到这只小马，换个名字或 ID 试试 🥺</div>

      <div class="section-title gap">✨ 可能认识的人</div>
      <div v-if="loadingSuggest" class="empty small">正在寻找小马…</div>
      <div v-else-if="!suggestions.length" class="empty small">糖霜镇暂时没有新伙伴，搜名字加好友吧 🌈</div>
      <div v-else class="friend-list">
        <div v-for="u in suggestions" :key="u.id" class="friend-item">
          <span class="friend-face" :style="{ background: faceBg(u.name) }">{{ u.avatar || u.name.slice(0, 1) }}</span>
          <div class="friend-main">
            <b>{{ u.name }} <span v-if="u.mutual" class="mutual-chip">{{ u.mutual }} 位共同好友</span></b>
            <em>{{ u.reason }}</em>
          </div>
          <button class="btn-add" :disabled="u.relation !== 'none'" @click="applyTo(u)">{{ relationText(u.relation) }}</button>
        </div>
      </div>
    </div>

    <!-- ========== 好友资料卡 ========== -->
    <Teleport to="body">
      <div v-if="profileTarget" class="sheet-mask" @click.self="profileTarget = null">
        <div class="sheet profile-sheet">
          <div class="profile-head">
            <span class="friend-face big" :style="{ background: faceBg(profileTarget.name) }">
              {{ profileTarget.avatar || profileTarget.name.slice(0, 1) }}
            </span>
            <div class="ph-main">
              <h3>
                {{ displayName(profileTarget) }}
                <span class="online-pill" :class="onlineMap[profileTarget.id] ? 'on' : ''">
                  {{ onlineMap[profileTarget.id] ? '在线' : '离线' }}
                </span>
              </h3>
              <p>原名 {{ profileTarget.name }} · ID {{ profileTarget.id }}</p>
              <p v-if="profile">🗓 {{ profile.since }} 成为好友 · 已认识 {{ profile.days }} 天</p>
            </div>
            <button class="sheet-close" @click="profileTarget = null">✕</button>
          </div>

          <div v-if="profileLoading" class="empty small">正在读取资料…</div>
          <template v-else-if="profile">
            <div class="profile-metrics">
              <div><b>{{ profile.points }}</b><span>积分</span></div>
              <div><b>{{ profile.msgCount }}</b><span>聊天条数</span></div>
              <div><b>{{ profile.mutualCount }}</b><span>共同好友</span></div>
              <div><b>{{ profile.game.both }}</b><span>同款游戏</span></div>
            </div>

            <div class="profile-block">
              <div class="pb-title">🏅 互动徽章</div>
              <div v-if="profile.badges.length" class="badge-row">
                <span v-for="b in profile.badges" :key="b.id" class="badge-chip" :title="b.desc">{{ b.emoji }} {{ b.name }}</span>
              </div>
              <div v-else class="pb-muted">还没有徽章，多聊聊天就会解锁啦 💬</div>
            </div>

            <div v-if="profile.mutualNames && profile.mutualNames.length" class="profile-block">
              <div class="pb-title">🤝 共同好友</div>
              <div class="pb-text">
                {{ profile.mutualNames.join('、') }}<span v-if="profile.mutualCount > profile.mutualNames.length"> 等 {{ profile.mutualCount }} 位</span>
              </div>
            </div>

            <div class="profile-block">
              <div class="pb-title">🎮 游戏战绩对比 <em>按每款游戏最高分</em></div>
              <div v-if="profile.game.both" class="vs-bar">
                <div class="vs-me" :style="{ width: vsPercent(profile.game.win, profile.game.both) + '%' }">我赢 {{ profile.game.win }}</div>
                <div class="vs-tie" :style="{ width: vsPercent(profile.game.tie, profile.game.both) + '%' }"></div>
                <div class="vs-friend" :style="{ width: vsPercent(profile.game.lose, profile.game.both) + '%' }">{{ profile.game.lose }} 好友赢</div>
              </div>
              <div class="pb-text">
                我玩过 {{ profile.game.myGames }} 款 / {{ profile.game.myPlays }} 局 ·
                好友 {{ profile.game.friendGames }} 款 / {{ profile.game.friendPlays }} 局
              </div>
            </div>

            <div class="profile-actions">
              <button class="pa primary" @click="openChat(profileTarget)">💬 聊天</button>
              <button class="pa" @click="openInvite([profileTarget.id])">🎲 邀请开一局</button>
              <button class="pa" @click="openRemark(profileTarget)">📝 备注</button>
              <button class="pa" @click="openBind(profileTarget)">💞 关系</button>
              <button class="pa" @click="giftPoints(profileTarget)">🧧 送礼</button>
              <button class="pa danger" @click="askDelete(profileTarget)">✕ 删除</button>
            </div>
          </template>
          <div v-else class="empty small">资料暂时读不出来，稍后再试 🥺</div>
        </div>
      </div>
    </Teleport>

    <!-- ========== 邀请组队 ========== -->
    <Teleport to="body">
      <div v-if="inviteOpen" class="sheet-mask" @click.self="inviteOpen = false">
        <div class="sheet">
          <h3>🎲 邀请好友开一局</h3>
          <p class="sheet-tip">开一间四人联机大富翁房间，勾选最多 3 位好友，会给他们各发一条带房间号的邀请消息。</p>
          <div v-if="!friends.length" class="empty small">先加几个好友，才能邀请哦 🐴</div>
          <div v-else class="invite-list">
            <label v-for="f in friends" :key="f.id" class="invite-row" :class="{ on: inviteIds.includes(f.id) }">
              <input type="checkbox" :value="f.id" v-model="inviteIds" />
              <span class="friend-face tiny" :style="{ background: faceBg(f.name) }">{{ f.avatar || f.name.slice(0, 1) }}</span>
              <b>{{ displayName(f) }}</b>
              <em :class="onlineMap[f.id] ? 'st-on' : 'st-off'">{{ onlineMap[f.id] ? '在线' : '离线' }}</em>
            </label>
          </div>
          <div class="sheet-btns">
            <button class="primary" :disabled="creating" @click="createAndInvite">
              {{ creating ? '正在开房间…' : `创建房间并邀请 (${inviteIds.length}/3)` }}
            </button>
            <button class="ghost" @click="inviteOpen = false">取消</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== 备注弹层 ========== -->
    <Teleport to="body">
      <div v-if="remarkTarget" class="sheet-mask" @click.self="remarkTarget = null">
        <div class="sheet">
          <h3>📝 设置备注</h3>
          <p class="sheet-tip">备注只有你能看到，对方不知道。给「{{ remarkTarget.name }}」起个顺口的名字吧（最多 16 字）</p>
          <input v-model.trim="remarkText" class="remark-input" maxlength="16" placeholder="输入备注名，留空则清除备注" @keyup.enter="saveRemark" />
          <div class="sheet-btns">
            <button class="primary" :disabled="savingRemark" @click="saveRemark">{{ savingRemark ? '保存中…' : '保存' }}</button>
            <button class="ghost" :disabled="savingRemark" @click="clearRemark">清除备注</button>
            <button class="ghost" @click="remarkTarget = null">取消</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== 绑定关系弹层 ========== -->
    <Teleport to="body">
      <div v-if="bindTarget" class="sheet-mask" @click.self="bindTarget = null">
        <div class="sheet">
          <h3>💞 绑定关系 · {{ bindTarget.name }}</h3>
          <p class="sheet-tip">选一种关系，首次绑定双方各得 50 积分</p>
          <div class="rel-grid">
            <button v-for="(r, key) in RELATIONS" :key="key" :class="{ on: bindTarget.relationType === key }" @click="doBind(key)">
              <span>{{ r.emoji }}</span><b>{{ r.name }}</b>
            </button>
          </div>
          <div v-if="bindBadges.length" class="rel-badges">
            <div class="rel-badges-title">已获得的互动徽章</div>
            <span v-for="b in bindBadges" :key="b.id" class="badge-chip" :title="b.desc">{{ b.emoji }} {{ b.name }}</span>
          </div>
          <div class="sheet-btns">
            <button v-if="bindTarget.relationType" class="ghost" @click="doUnbind">解除绑定</button>
            <button class="ghost" @click="bindTarget = null">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== 删除确认 ========== -->
    <Teleport to="body">
      <div v-if="deleteTarget" class="sheet-mask" @click.self="deleteTarget = null">
        <div class="sheet confirm-sheet">
          <h3>🗑 删除好友</h3>
          <p class="sheet-tip">确定和「{{ displayName(deleteTarget) }}」解除好友关系吗？聊天记录会保留，以后再加回来还能接着聊。</p>
          <div class="sheet-btns">
            <button class="danger" @click="confirmDelete">确认删除</button>
            <button class="ghost" @click="deleteTarget = null">再想想</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { bump } from '../quest'
import {
  apiFriendList, apiApplies, apiSearchUsers, apiApply, apiHandle, apiDeleteFriend,
  apiFriendBadges, apiSetRemark, apiSentApplies, apiCancelApply, apiFriendProfile, apiFriendSuggest
} from '../friend'
import { apiSend } from '../chat'
import { apiPointGift } from '../point'
import { apiAccountBind, apiAccountUnbind } from '../account'
import { apiPresenceStatus } from '../presence'
import { mpCreate, mpLan } from '../monopolyRoom'
import sound from '../sound'

/** 用名字 hash 生成柔和的头像底色，跟主题色系一致 */
const FACE_COLORS = ['#f7a8c4', '#b79cf0', '#8fc7f0', '#a7e3c9', '#f7e08a', '#f0a06a', '#c9a0e8']

/** 绑定关系定义（与后端 ShopDefs.relation 一致） */
const RELATIONS = {
  bestie: { name: '闺蜜', emoji: '💞' },
  bro: { name: '兄弟', emoji: '🤜' },
  master: { name: '师徒', emoji: '🎓' },
  partner: { name: '搭档', emoji: '🤝' },
  rival: { name: '对手', emoji: '⚔️' }
}

export default {
  name: 'FriendsView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      tab: 'list',
      // 好友列表
      friends: [],
      loadingList: false,
      listKeyword: '',
      sortBy: 'online',
      // 新的朋友
      applies: [],
      loadingApply: false,
      sentApplies: [],
      loadingSent: false,
      // 添加
      keyword: '',
      applyMsg: '很高兴认识你！',
      results: [],
      searching: false,
      searched: false,
      suggestions: [],
      loadingSuggest: false,
      // 资料卡
      profileTarget: null,
      profile: null,
      profileLoading: false,
      // 邀请组队
      inviteOpen: false,
      inviteIds: [],
      creating: false,
      // 绑定关系 / 备注 / 删除
      bindTarget: null,
      bindBadges: [],
      remarkTarget: null,
      remarkText: '',
      savingRemark: false,
      deleteTarget: null,
      RELATIONS,
      // 在线状态
      onlineMap: {},
      presenceTimer: null,
      applyTimer: null
    }
  },
  computed: {
    onlineCount() {
      return this.friends.filter(f => this.onlineMap[f.id]).length
    },
    sentWaiting() {
      return this.sentApplies.filter(a => a.state === 'waiting').length
    },
    visibleFriends() {
      const kw = this.listKeyword.trim().toLowerCase()
      let arr = this.friends.filter(f => {
        if (!kw) return true
        return (f.name || '').toLowerCase().includes(kw)
          || (f.remark || '').toLowerCase().includes(kw)
          || String(f.id).includes(kw)
      })
      if (this.sortBy === 'online') {
        arr = arr.slice().sort((a, b) => (this.onlineMap[b.id] ? 1 : 0) - (this.onlineMap[a.id] ? 1 : 0))
      } else if (this.sortBy === 'name') {
        arr = arr.slice().sort((a, b) => (a.remark || a.name).localeCompare(b.remark || b.name, 'zh-CN'))
      }
      return arr
    }
  },
  created() {
    this.loadFriends()
    this.loadApplies()
    this.loadSent()
    this.loadSuggest()
    this.refreshPresence()
    // 在线状态 20 秒、申请 15 秒自动刷新
    this.presenceTimer = setInterval(this.refreshPresence, 20000)
    this.applyTimer = setInterval(() => { this.loadApplies(true); this.loadSent(true) }, 15000)
  },
  // Vue 3 用 beforeUnmount（原来的 beforeDestroy 不会被调用，定时器会泄漏）
  beforeUnmount() {
    if (this.presenceTimer) clearInterval(this.presenceTimer)
    if (this.applyTimer) clearInterval(this.applyTimer)
  },
  methods: {
    relOf(key) { return RELATIONS[key] || null },
    vsPercent(n, total) { return total ? Math.round(n / total * 100) : 0 },
    async refreshPresence() {
      const ids = this.friends.map(f => f.id)
      if (!ids.length) return
      const res = await apiPresenceStatus(ids)
      if (res.code === 200 && res.data) this.onlineMap = res.data
    },
    goHome() { this.$router.push({ name: 'home' }) },
    faceBg(name) {
      let h = 0
      const s = name || '小马'
      for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 997
      return FACE_COLORS[h % FACE_COLORS.length]
    },
    displayName(f) { return (f && f.remark) || (f && f.name) || '小马' },

    /* ---------- 加载 ---------- */
    async loadFriends() {
      this.loadingList = true
      const res = await apiFriendList(this.session.id)
      this.loadingList = false
      if (res.code === 200) {
        this.friends = res.data || []
        this.refreshPresence()
      } else this.toast(res.msg || '好友加载失败', 'err')
    },
    async loadApplies(silent) {
      if (!silent) this.loadingApply = true
      const res = await apiApplies(this.session.id)
      this.loadingApply = false
      if (res.code === 200) this.applies = res.data || []
    },
    async loadSent(silent) {
      if (!silent) this.loadingSent = true
      const res = await apiSentApplies(this.session.id)
      this.loadingSent = false
      if (res.code === 200) this.sentApplies = res.data || []
    },
    async loadSuggest() {
      this.loadingSuggest = true
      const res = await apiFriendSuggest(this.session.id)
      this.loadingSuggest = false
      if (res.code === 200) this.suggestions = res.data || []
    },

    /* ---------- 添加好友 ---------- */
    async doSearch() {
      if (!this.keyword) { this.toast('先输入小马名字或 ID', 'err'); return }
      this.searching = true
      this.searched = true
      const res = await apiSearchUsers(this.session.id, this.keyword)
      this.searching = false
      if (res.code === 200) this.results = res.data || []
      else { this.toast(res.msg || '搜索失败', 'err'); this.results = [] }
    },
    relationText(rel) {
      return { none: '加好友', 'pending-out': '已申请', 'pending-in': '待你同意', friend: '已是好友' }[rel] || '加好友'
    },
    async applyTo(u) {
      const res = await apiApply(this.session.id, u.id, this.applyMsg)
      if (res.code === 200) {
        sound.magic()
        this.toast(`已向「${u.name}」发出好友申请 🎉`, 'ok')
        u.relation = 'pending-out'
        this.loadSent(true)
      } else {
        sound.fail()
        this.toast(res.msg || '申请失败', 'err')
        if (this.searched) this.doSearch()
        this.loadSuggest()
      }
    },
    async handleApply(a, accept) {
      const res = await apiHandle(a.id, this.session.id, accept)
      if (res.code === 200) {
        this.toast(accept ? `已同意「${a.name}」的好友申请 🎉` : `已拒绝「${a.name}」`, 'ok')
        this.applies = this.applies.filter(x => x.id !== a.id)
        if (accept) { bump('friend'); this.loadFriends(); this.loadSuggest() }
      } else {
        this.toast(res.msg || '操作失败', 'err')
        this.loadApplies()
      }
    },
    async cancelApply(a) {
      const res = await apiCancelApply(a.id, this.session.id)
      if (res.code === 200) {
        this.toast('已撤回好友申请', 'ok')
        this.sentApplies = this.sentApplies.filter(x => x.id !== a.id)
        this.results.forEach(u => { if (u.id === a.userId) u.relation = 'none' })
        this.suggestions.forEach(u => { if (u.id === a.userId) u.relation = 'none' })
      } else {
        this.toast(res.msg || '撤回失败', 'err')
        this.loadSent(true)
      }
    },
    async reapply(a) {
      const res = await apiApply(this.session.id, a.userId, this.applyMsg)
      if (res.code === 200) {
        this.toast(`已重新向「${a.name}」发出申请 🎉`, 'ok')
        this.loadSent(true)
      } else this.toast(res.msg || '申请失败', 'err')
    },

    /* ---------- 好友资料卡 ---------- */
    async openProfile(f) {
      this.profileTarget = f
      this.profile = null
      this.profileLoading = true
      const targetId = f.id
      const res = await apiFriendProfile(this.session.id, targetId)
      // 用户可能已经关掉弹层，别把旧结果塞进新卡片
      if (!this.profileTarget || this.profileTarget.id !== targetId) return
      this.profileLoading = false
      if (res.code === 200) this.profile = res.data
      else this.toast(res.msg || '资料加载失败', 'err')
    },
    openChat(f) {
      this.profileTarget = null
      this.$router.push({ name: 'chat', params: { id: f.id }, query: { name: f.name, remark: f.remark || '' } })
    },

    /* ---------- 邀请组队（四人联机大富翁） ---------- */
    openInvite(preset) {
      this.profileTarget = null
      this.inviteIds = Array.isArray(preset) ? preset.slice(0, 3) : []
      this.inviteOpen = true
    },
    async createAndInvite() {
      if (this.creating) return
      if (this.inviteIds.length > 3) { this.toast('一间房最多邀请 3 位好友', 'err'); return }
      this.creating = true
      const r = await mpCreate(this.session.name || '小马', this.session.id, 'team')
      if (r.code !== 200) {
        this.creating = false
        this.toast(r.msg || '创建房间失败', 'err')
        return
      }
      const { roomId, seatId, token } = r.data
      // 房主凭据存到 sessionStorage，联机页会自动恢复（刷新页面也不会掉线）
      try {
        sessionStorage.setItem('mlp_mp_room', JSON.stringify({ roomId, seatId, token, ts: Date.now() }))
      } catch (e) { /* 隐私模式下忽略 */ }

      let base = window.location.origin
      const lan = await mpLan()
      if (lan && lan.code === 200 && lan.data && lan.data.ip) base = `http://${lan.data.ip}:5180`
      const url = `${base}/game/monopoly?room=${roomId}`
      let sent = 0
      for (const id of this.inviteIds) {
        const res = await apiSend(this.session.id, id,
          `🎲 一起玩糖霜镇大富翁吧！房间号 ${roomId}，打开 ${url} 加入～`)
        if (res.code === 200) sent++
      }
      this.creating = false
      this.inviteOpen = false
      sound.magic()
      this.toast(`房间 ${roomId} 已开好，已邀请 ${sent} 位好友（记得把邀请链接也发群里）`, 'ok')
      this.$router.push({ name: 'game', params: { id: 'monopoly' }, query: { room: roomId } })
    },

    /* ---------- 删除好友 ---------- */
    askDelete(f) {
      this.profileTarget = null
      this.deleteTarget = f
    },
    async confirmDelete() {
      const f = this.deleteTarget
      if (!f) return
      const res = await apiDeleteFriend(this.session.id, f.id)
      if (res.code === 200) {
        this.toast(`已删除好友「${this.displayName(f)}」`, 'ok')
        this.friends = this.friends.filter(x => x.id !== f.id)
        this.deleteTarget = null
        this.loadSuggest()
      } else this.toast(res.msg || '删除失败', 'err')
    },

    /* ---------- 送礼 / 绑定关系 / 备注 ---------- */
    async giftPoints(f) {
      const res = await apiPointGift(this.session.id, f.id)
      if (res.code === 200) { bump('gift'); sound.success(); this.toast(res.data, 'ok') }
      else { sound.fail(); this.toast(res.msg || '送礼失败', 'err') }
    },
    async openBind(f) {
      this.profileTarget = null
      this.bindTarget = f
      this.bindBadges = []
      const res = await apiFriendBadges(this.session.id, f.id)
      if (res.code === 200) this.bindBadges = res.data || []
    },
    async doBind(type) {
      const f = this.bindTarget
      if (!f) return
      const res = await apiAccountBind(this.session.id, f.id, type)
      if (res.code === 200) {
        sound.magic()
        this.toast(res.data, 'ok')
        f.relationType = type
        this.bindTarget = null
      } else { sound.fail(); this.toast(res.msg || '绑定失败', 'err') }
    },
    async doUnbind() {
      const f = this.bindTarget
      if (!f) return
      const res = await apiAccountUnbind(this.session.id, f.id)
      if (res.code === 200) {
        this.toast('已解除绑定', 'ok')
        f.relationType = ''
        this.bindTarget = null
      } else this.toast(res.msg || '解绑失败', 'err')
    },
    openRemark(f) {
      this.profileTarget = null
      this.remarkTarget = f
      this.remarkText = f.remark || ''
    },
    async saveRemark() {
      const f = this.remarkTarget
      if (!f) return
      this.savingRemark = true
      const res = await apiSetRemark(this.session.id, f.id, this.remarkText)
      this.savingRemark = false
      if (res.code === 200) {
        f.remark = this.remarkText.trim()
        this.toast(f.remark ? `备注已改为「${f.remark}」` : '备注已清除', 'ok')
        this.remarkTarget = null
      } else this.toast(res.msg || '备注保存失败', 'err')
    },
    async clearRemark() {
      if (!this.remarkTarget) return
      this.remarkText = ''
      await this.saveRemark()
    }
  }
}
</script>

<style scoped>
.friends .tabs { margin: 14px 0 12px; }
.tab-pane { min-height: 200px; }
.pane-tip { font-size: 11px; color: var(--muted); margin: 14px 2px 0; }
.empty.small { padding: 18px 8px; font-size: 12.5px; }
.section-title {
  font-size: 13px; font-weight: 800; color: var(--text);
  margin: 4px 0 10px; display: flex; align-items: center; gap: 6px;
}
.section-title.gap { margin-top: 24px; }
.section-title em {
  font-style: normal; font-size: 10px; color: var(--muted);
  background: var(--panel); border-radius: 999px; padding: 1px 7px;
}

/* 概览条 */
.friend-stats {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-top: 14px;
}
.fs-item {
  background: var(--panel); border-radius: 13px; padding: 10px 4px;
  text-align: center; border: 1.5px solid transparent;
}
.fs-item b { display: block; font-size: 19px; line-height: 1.1; color: var(--primary); }
.fs-item b.ok { color: #35b87a; }
.fs-item b.warn { color: var(--danger); }
.fs-item span { font-size: 10.5px; color: var(--muted); }

/* 列表工具条 */
.list-toolbar { display: flex; gap: 6px; margin-bottom: 12px; align-items: center; }
.mini-input, .mini-select {
  border: 1.5px solid var(--border); border-radius: 10px; background: var(--card);
  color: var(--text); font-family: inherit; font-size: 12px; padding: 8px 9px;
  min-width: 0;
}
.mini-input { flex: 1; }
.mini-select { width: 92px; flex-shrink: 0; }
.mini-input:focus, .mini-select:focus { outline: none; border-color: var(--primary); }
.invite-btn {
  flex-shrink: 0; border: none; cursor: pointer; font-size: 12px; font-weight: 800;
  padding: 8px 10px; border-radius: 10px; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 3px 8px rgba(160,108,213,.22);
}
.invite-btn:hover { filter: brightness(1.06); }

/* 好友条目 */
.friend-list { display: flex; flex-direction: column; gap: 9px; }
.friend-item {
  display: flex; align-items: center; gap: 11px; padding: 11px;
  border-radius: 14px; background: var(--panel); border: 1.5px solid transparent;
  transition: border-color .15s, transform .15s; cursor: pointer;
}
.friend-item:hover { border-color: var(--primary); transform: translateY(-2px); }
.friend-face {
  width: 42px; height: 42px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 17px; font-weight: 700;
  box-shadow: 0 3px 8px rgba(160,108,213,.22);
}
.friend-face.big { width: 58px; height: 58px; font-size: 24px; }
.friend-face.tiny { width: 28px; height: 28px; font-size: 12px; }
.friend-face-wrap { position: relative; flex-shrink: 0; }
.online-dot {
  position: absolute; right: 0; bottom: 0; width: 12px; height: 12px;
  border-radius: 50%; background: #44d07f; border: 2px solid var(--card);
  box-shadow: 0 0 0 1px rgba(68,208,127,.4);
}
.friend-main { flex: 1; min-width: 0; }
.friend-main b { font-size: 14px; display: block; }
.friend-main em {
  font-style: normal; font-size: 11px; color: var(--muted); display: block; margin-top: 2px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.st-on { color: #2fa86c; font-weight: 700; }
.st-off { color: var(--muted); }
.friend-del {
  border: none; background: var(--card); color: var(--muted);
  flex-shrink: 0; transition: all .15s;
}

/* 申请操作 */
.apply-btns { display: flex; gap: 6px; flex-shrink: 0; }
.apply-btns button {
  border: none; cursor: pointer; font-size: 12px; font-weight: 700;
  padding: 6px 12px; border-radius: 999px; transition: filter .15s;
}
.btn-ok { color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple)); }
.btn-ok:hover { filter: brightness(1.06); }
.btn-no { color: var(--muted); background: var(--card); border: 1.5px solid var(--border) !important; }
.btn-no:hover { color: var(--danger); border-color: var(--danger) !important; }
.state-chip, .mutual-chip {
  display: inline-block; vertical-align: middle; font-size: 10px; font-weight: 700;
  border-radius: 999px; padding: 1px 7px; margin-left: 5px;
  background: var(--card); color: var(--muted); border: 1px solid var(--border);
}
.state-chip.waiting { color: var(--primary); border-color: var(--primary); }
.state-chip.rejected { color: var(--danger); border-color: var(--danger); }
.mutual-chip { color: var(--primary); border-color: var(--primary); }

.btn-add {
  border: none; cursor: pointer; flex-shrink: 0; font-size: 12px; font-weight: 700;
  padding: 7px 14px; border-radius: 999px; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 10px rgba(160,108,213,.25);
}
.btn-add:disabled { background: var(--track); color: var(--muted); cursor: default; box-shadow: none; }
.apply-msg-row { display: flex; align-items: center; gap: 8px; margin: 10px 0 14px; }
.apply-msg-row span { font-size: 11.5px; color: var(--muted); flex-shrink: 0; }
.apply-msg-row input {
  flex: 1; min-width: 0; border: 1.5px solid var(--border); border-radius: 10px;
  padding: 8px 10px; font-size: 12px; color: var(--text); background: var(--card); font-family: inherit;
}
.apply-msg-row input:focus { outline: none; border-color: var(--primary); }

/* 标签红点 / 关系徽章 */
.tab em { font-style: normal; }
.red-dot {
  display: inline-block; min-width: 16px; height: 16px; line-height: 16px;
  border-radius: 999px; background: var(--danger); color: #fff;
  font-size: 10px; text-align: center; padding: 0 4px; margin-left: 3px;
}
.rel-badge {
  display: inline-block; vertical-align: middle; font-size: 10px; font-weight: 700;
  color: var(--primary); background: var(--card); border: 1px solid var(--primary);
  border-radius: 999px; padding: 1px 8px; margin-left: 6px;
}

/* 行内操作组 */
.friend-ops { display: flex; gap: 6px; flex-shrink: 0; align-items: center; }
.friend-ops button {
  width: 30px; height: 30px; border-radius: 50%; border: none;
  cursor: pointer; font-size: 14px; background: var(--card); transition: all .15s;
}
.friend-ops button:hover { transform: scale(1.12); }
.op-chat:hover { background: #e8f3ff; }
.op-gift:hover { background: #ffe9f1; }

/* 删除好友：做小、做淡、拉开距离，悬停卡片才浮现，防误触 */
.friend-del {
  width: 20px !important; height: 20px !important;
  font-size: 9px !important;
  margin-left: 10px;              /* 和聊天/红包拉开距离 */
  opacity: 0;                     /* 平时隐藏 */
  pointer-events: none;
  transform: scale(.8);
}
.friend-item:hover .friend-del {
  opacity: .55; pointer-events: auto; transform: scale(1);
}
.friend-del:hover { opacity: 1 !important; color: var(--danger); background: #ffeceb; }

/* 触屏设备没有 hover：删除按钮常驻小尺寸淡显，仍可点但不易误触 */
@media (hover: none) {
  .friend-del { opacity: .5; pointer-events: auto; transform: none; }
}

/* 弹层 */
.sheet-mask {
  position: fixed; inset: 0; z-index: 100; background: rgba(60, 30, 90, .35);
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.sheet {
  width: 100%; max-width: var(--app-w); max-height: var(--app-h); overflow-y: auto;
  background: var(--card); border-radius: 20px; padding: 20px;
  border: 1.5px solid var(--border); position: relative;
}
.sheet h3 { margin: 0 0 4px; font-size: 17px; }
.sheet-tip { margin: 0 0 14px; font-size: 12px; color: var(--muted); line-height: 1.5; }
.remark-input {
  width: 100%; box-sizing: border-box; border: 1.5px solid var(--border);
  border-radius: 12px; padding: 11px 13px; font-size: 14px;
  color: var(--text); background: var(--card); font-family: inherit;
}
.remark-input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(160,108,213,.15); }
.sheet-btns { display: flex; gap: 8px; margin-top: 16px; }
.sheet-btns button {
  flex: 1; border: none; cursor: pointer; border-radius: 10px;
  padding: 9px 0; font-size: 13px; font-weight: 800;
  background: var(--panel); color: var(--muted);
}
.sheet-btns button.primary { background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff; }
.sheet-btns button.danger { background: var(--danger); color: #fff; }
.sheet-btns button.primary:disabled { opacity: .6; cursor: default; }
.sheet-btns button.ghost:hover { color: var(--danger); }

/* 资料卡 */
.profile-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.ph-main { flex: 1; min-width: 0; }
.ph-main h3 { margin: 0; display: flex; align-items: center; gap: 7px; flex-wrap: wrap; }
.ph-main p { margin: 3px 0 0; font-size: 11.5px; color: var(--muted); }
.sheet-close {
  position: absolute; right: 16px; top: 14px; border: none; cursor: pointer;
  width: 26px; height: 26px; border-radius: 50%; background: var(--panel); color: var(--muted);
}
.online-pill {
  font-size: 10px; font-weight: 700; border-radius: 999px; padding: 2px 8px;
  background: var(--panel); color: var(--muted); border: 1px solid var(--border);
}
.online-pill.on { color: #2fa86c; border-color: #44d07f; background: #eafaf1; }
.profile-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 7px; margin-bottom: 14px; }
.profile-metrics > div {
  background: var(--panel); border-radius: 12px; padding: 9px 3px; text-align: center;
}
.profile-metrics b { display: block; font-size: 17px; color: var(--primary); }
.profile-metrics span { font-size: 10px; color: var(--muted); }
.profile-block { margin-bottom: 13px; }
.pb-title { font-size: 12px; font-weight: 800; margin-bottom: 7px; }
.pb-title em { font-style: normal; font-size: 10px; font-weight: 400; color: var(--muted); margin-left: 4px; }
.pb-text { font-size: 11.5px; color: var(--muted); line-height: 1.6; }
.pb-muted { font-size: 11.5px; color: var(--muted); }
.badge-row { display: flex; flex-wrap: wrap; gap: 5px; }
.badge-chip {
  display: inline-block; font-size: 11px; font-weight: 700;
  background: var(--panel); border: 1px solid var(--border);
  border-radius: 999px; padding: 3px 10px;
}
.vs-bar { display: flex; height: 22px; border-radius: 999px; overflow: hidden; background: var(--track); margin-bottom: 6px; }
.vs-bar > div { font-size: 10px; font-weight: 800; color: #fff; line-height: 22px; text-align: center; white-space: nowrap; }
.vs-me { background: #f7a8c4; }
.vs-tie { background: #d9c9ef; }
.vs-friend { background: #8fc7f0; }
.profile-actions { display: grid; grid-template-columns: repeat(3, 1fr); gap: 7px; margin-top: 16px; }
.profile-actions .pa {
  border: 1.5px solid var(--border); background: var(--panel); color: var(--text);
  border-radius: 11px; padding: 9px 2px; font-size: 11.5px; font-weight: 700;
  cursor: pointer; font-family: inherit; transition: all .15s;
}
.profile-actions .pa:hover { border-color: var(--primary); transform: translateY(-1px); }
.profile-actions .pa.primary {
  background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff; border-color: transparent;
}
.profile-actions .pa.danger { color: var(--danger); }
.confirm-sheet .sheet-tip { font-size: 13px; }

/* 邀请组队 */
.invite-list { display: flex; flex-direction: column; gap: 6px; max-height: 260px; overflow-y: auto; }
.invite-row {
  display: flex; align-items: center; gap: 9px; padding: 8px 10px;
  background: var(--panel); border-radius: 11px; border: 1.5px solid transparent;
  cursor: pointer; font-size: 12px;
}
.invite-row.on { border-color: var(--primary); background: var(--card); }
.invite-row input { accent-color: var(--primary); width: 15px; height: 15px; }
.invite-row b { flex: 1; font-size: 12.5px; }
.invite-row em { font-style: normal; font-size: 10.5px; }

/* 绑定关系 */
.rel-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.rel-grid button {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: 1.5px solid var(--border); border-radius: 12px;
  background: var(--panel); color: var(--text); padding: 10px 2px;
  cursor: pointer; transition: all .15s;
}
.rel-grid button:hover { border-color: var(--primary); }
.rel-grid button.on {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff;
}
.rel-grid button span { font-size: 20px; }
.rel-grid button b { font-size: 11px; }
.rel-badges { margin-top: 14px; }
.rel-badges-title { font-size: 11.5px; color: var(--muted); margin-bottom: 6px; }
.rel-badges .badge-chip { margin: 0 5px 5px 0; }
</style>
