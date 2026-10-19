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
      <div v-if="loadingList" class="empty">正在加载好友…</div>
      <div v-else-if="!friends.length" class="empty">
        还没有好友，去「添加」标签搜小马名字加好友吧 🐴
      </div>
      <div v-else class="friend-list">
        <div v-for="f in friends" :key="f.id" class="friend-item" @click="openChat(f)">
          <div class="friend-face-wrap">
            <span class="friend-face" :style="{ background: faceBg(f.name) }">{{ f.avatar || f.name.slice(0, 1) }}</span>
            <span v-if="onlineMap[f.id]" class="online-dot" title="在线"></span>
          </div>
          <div class="friend-main">
            <b>
              {{ displayName(f) }}
              <span v-if="f.relationType" class="rel-badge" :title="RELATIONS[f.relationType].name">
                {{ RELATIONS[f.relationType].emoji }} {{ RELATIONS[f.relationType].name }}
              </span>
            </b>
            <em v-if="f.remark">原名：{{ f.name }} · ID: {{ f.id }} · {{ f.since }} 成为好友</em>
            <em v-else>ID: {{ f.id }} · {{ f.since }} 成为好友</em>
          </div>
          <div class="friend-ops">
            <button class="op-remark" title="设置备注" @click.stop="openRemark(f)">📝</button>
            <button class="op-gift" title="送 50 积分" @click.stop="giftPoints(f)">🧧</button>
            <button class="op-bind" title="绑定关系" @click.stop="openBind(f)">💞</button>
            <button class="friend-del" title="删除好友" @click.stop="askDelete(f)">✕</button>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 备注弹层 ========== -->
    <Teleport to="body">
      <div v-if="remarkTarget" class="sheet-mask" @click.self="remarkTarget = null">
        <div class="sheet">
          <h3>📝 设置备注</h3>
          <p class="sheet-tip">
            备注只有你能看到，对方不知道。给「{{ remarkTarget.name }}」起个顺口的名字吧（最多 16 字）
          </p>
          <input
            v-model.trim="remarkText"
            class="remark-input"
            maxlength="16"
            placeholder="输入备注名，留空则清除备注"
            @keyup.enter="saveRemark"
          />
          <div class="sheet-btns">
            <button class="primary" :disabled="savingRemark" @click="saveRemark">
              {{ savingRemark ? '保存中…' : '保存' }}
            </button>
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
            <button
              v-for="(r, key) in RELATIONS"
              :key="key"
              :class="{ on: bindTarget.relationType === key }"
              @click="doBind(key)"
            >
              <span>{{ r.emoji }}</span>
              <b>{{ r.name }}</b>
            </button>
          </div>
          <div v-if="bindBadges.length" class="rel-badges">
            <div class="rel-badges-title">已获得的互动徽章</div>
            <span v-for="b in bindBadges" :key="b.id" class="badge-chip" :title="b.desc">
              {{ b.emoji }} {{ b.name }}
            </span>
          </div>
          <div class="sheet-btns">
            <button v-if="bindTarget.relationType" class="ghost" @click="doUnbind">解除绑定</button>
            <button class="ghost" @click="bindTarget = null">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ========== 新的朋友（申请） ========== -->
    <div v-show="tab === 'apply'" class="tab-pane">
      <div v-if="loadingApply" class="empty">正在加载申请…</div>
      <div v-else-if="!applies.length" class="empty">暂时没有新的好友申请 📭</div>
      <div v-else class="friend-list">
        <div v-for="a in applies" :key="a.id" class="friend-item">
          <span class="friend-face" :style="{ background: faceBg(a.name) }">{{ a.name.slice(0, 1) }}</span>
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
    </div>

    <!-- ========== 添加好友 ========== -->
    <div v-show="tab === 'add'" class="tab-pane">
      <form class="search" @submit.prevent="doSearch">
        <input v-model.trim="keyword" type="text" placeholder="输入小马名字或 ID 搜索…" maxlength="20" />
        <button type="submit" title="搜索">🔍</button>
      </form>

      <div v-if="searching" class="empty">正在搜索…</div>
      <div v-else-if="results.length" class="friend-list">
        <div v-for="u in results" :key="u.id" class="friend-item">
          <span class="friend-face" :style="{ background: faceBg(u.name) }">{{ u.name.slice(0, 1) }}</span>
          <div class="friend-main">
            <b>{{ u.name }}</b>
            <em>ID: {{ u.id }}</em>
          </div>
          <button
            class="btn-add"
            :disabled="u.relation !== 'none'"
            @click="applyTo(u)"
          >{{ relationText(u.relation) }}</button>
        </div>
      </div>
      <div v-else-if="searched" class="empty">没找到这只小马，换个名字或 ID 试试 🥺</div>
      <div v-else class="empty">输入小马名字或 ID，开始寻找好友吧 ✨</div>
    </div>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { apiFriendList, apiApplies, apiSearchUsers, apiApply, apiHandle, apiDeleteFriend, apiFriendBadges, apiSetRemark } from '../friend'
import { apiPointGift } from '../point'
import { apiAccountBind, apiAccountUnbind } from '../account'
import { apiPresenceStatus } from '../presence'
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
      // 新的朋友
      applies: [],
      loadingApply: false,
      // 添加
      keyword: '',
      results: [],
      searching: false,
      searched: false,
      // 绑定关系弹层
      bindTarget: null,
      bindBadges: [],
      // 备注弹层
      remarkTarget: null,
      remarkText: '',
      savingRemark: false,
      RELATIONS,
      // 在线状态：{ [friendId]: true/false }
      onlineMap: {},
      presenceTimer: null
    }
  },
  created() {
    this.loadFriends()
    this.loadApplies()
    this.refreshPresence()
    // 每 20 秒刷新一次好友在线状态
    this.presenceTimer = setInterval(this.refreshPresence, 20000)
  },
  beforeDestroy() {
    if (this.presenceTimer) clearInterval(this.presenceTimer)
  },
  methods: {
    async refreshPresence() {
      const ids = this.friends.map(f => f.id)
      if (!ids.length) return
      const res = await apiPresenceStatus(ids)
      if (res.code === 200 && res.data) {
        this.onlineMap = res.data
      }
    },
    goHome() {
      this.$router.push({ name: 'home' })
    },
    faceBg(name) {
      let h = 0
      for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) % 997
      return FACE_COLORS[h % FACE_COLORS.length]
    },
    async loadFriends() {
      this.loadingList = true
      const res = await apiFriendList(this.session.id)
      this.loadingList = false
      if (res.code === 200) this.friends = res.data || []
      else this.toast(res.msg || '好友加载失败', 'err')
    },
    async loadApplies() {
      this.loadingApply = true
      const res = await apiApplies(this.session.id)
      this.loadingApply = false
      if (res.code === 200) this.applies = res.data || []
    },
    async doSearch() {
      if (!this.keyword) {
        this.toast('先输入小马名字或 ID', 'err')
        return
      }
      this.searching = true
      this.searched = true
      const res = await apiSearchUsers(this.session.id, this.keyword)
      this.searching = false
      if (res.code === 200) {
        this.results = res.data || []
      } else {
        this.toast(res.msg || '搜索失败', 'err')
        this.results = []
      }
    },
    relationText(rel) {
      return { none: '加好友', 'pending-out': '已申请', 'pending-in': '待你同意', friend: '已是好友' }[rel] || '加好友'
    },
    async applyTo(u) {
      const res = await apiApply(this.session.id, u.id, '很高兴认识你！')
      if (res.code === 200) {
        this.toast(`已向「${u.name}」发出好友申请 🎉`, 'ok')
        u.relation = 'pending-out'
      } else {
        this.toast(res.msg || '申请失败', 'err')
        // 状态可能变了，刷新搜索结果
        this.doSearch()
      }
    },
    async handleApply(a, accept) {
      const res = await apiHandle(a.id, this.session.id, accept)
      if (res.code === 200) {
        this.toast(accept ? `已同意「${a.name}」的好友申请 🎉` : `已拒绝「${a.name}」`, 'ok')
        this.applies = this.applies.filter(x => x.id !== a.id)
        if (accept) this.loadFriends()
      } else {
        this.toast(res.msg || '操作失败', 'err')
        this.loadApplies()
      }
    },
    askDelete(f) {
      if (!confirm(`确定要删除好友「${f.name}」吗？聊天记录会保留。`)) return
      this.deleteFriend(f)
    },
    async deleteFriend(f) {
      const res = await apiDeleteFriend(this.session.id, f.id)
      if (res.code === 200) {
        this.toast(`已删除好友「${f.name}」`, 'ok')
        this.friends = this.friends.filter(x => x.id !== f.id)
      } else {
        this.toast(res.msg || '删除失败', 'err')
      }
    },
    /** 送 50 积分给好友（后端：送方 -50、收方 +50） */
    async giftPoints(f) {
      const res = await apiPointGift(this.session.id, f.id)
      if (res.code === 200) {
        sound.success()
        this.toast(res.data, 'ok')
      } else {
        sound.fail()
        this.toast(res.msg || '送礼失败', 'err')
      }
    },
    /** 打开绑定弹层：带上当前徽章 */
    async openBind(f) {
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
      } else {
        sound.fail()
        this.toast(res.msg || '绑定失败', 'err')
      }
    },
    async doUnbind() {
      const f = this.bindTarget
      if (!f) return
      const res = await apiAccountUnbind(this.session.id, f.id)
      if (res.code === 200) {
        this.toast('已解除绑定', 'ok')
        f.relationType = ''
        this.bindTarget = null
      } else {
        this.toast(res.msg || '解绑失败', 'err')
      }
    },
    openChat(f) {
      this.$router.push({
        name: 'chat',
        params: { id: f.id },
        query: { name: f.name, remark: f.remark || '' }
      })
    },

    // ---------- 好友备注 ----------
    /** 列表显示名：有备注优先显示备注 */
    displayName(f) {
      return f.remark || f.name
    },
    openRemark(f) {
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
        this.toast(this.remarkText.trim() ? `备注已改为「${f.remark}」` : '备注已清除', 'ok')
        this.remarkTarget = null
      } else {
        this.toast(res.msg || '备注保存失败', 'err')
      }
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
.friends .tabs { margin: 16px 0 14px; }
.tab-pane { min-height: 200px; }

/* 好友条目 */
.friend-list { display: flex; flex-direction: column; gap: 10px; }
.friend-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px; border-radius: 14px;
  background: var(--panel); border: 1.5px solid transparent;
  transition: border-color .15s, transform .15s;
}
.friend-item:hover { border-color: var(--primary); transform: translateY(-2px); }
.friend-face {
  width: 44px; height: 44px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 18px; font-weight: 700;
  box-shadow: 0 3px 8px rgba(160,108,213,.22);
}
.friend-face-wrap { position: relative; flex-shrink: 0; }
.online-dot {
  position: absolute; right: 0; bottom: 0;
  width: 12px; height: 12px; border-radius: 50%;
  background: #44d07f; border: 2px solid var(--card);
  box-shadow: 0 0 0 1px rgba(68,208,127,.4);
}
.friend-main { flex: 1; min-width: 0; }
.friend-main b { font-size: 14px; display: block; }
.friend-main em {
  font-style: normal; font-size: 11.5px; color: var(--muted);
  display: block; margin-top: 2px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.friend-del {
  border: none; background: var(--card); color: var(--muted);
  width: 26px; height: 26px; border-radius: 50%; cursor: pointer;
  font-size: 12px; flex-shrink: 0; transition: all .15s;
}
.friend-del:hover { color: var(--danger); }

/* 申请操作按钮 */
.apply-btns { display: flex; gap: 6px; flex-shrink: 0; }
.apply-btns button {
  border: none; cursor: pointer; font-size: 12px; font-weight: 700;
  padding: 6px 13px; border-radius: 999px; transition: filter .15s;
}
.btn-ok { color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple)); }
.btn-ok:hover { filter: brightness(1.06); }
.btn-no { color: var(--muted); background: var(--card); border: 1.5px solid var(--border) !important; }
.btn-no:hover { color: var(--danger); border-color: var(--danger) !important; }

/* 添加按钮 */
.btn-add {
  border: none; cursor: pointer; flex-shrink: 0;
  font-size: 12px; font-weight: 700; padding: 7px 15px; border-radius: 999px;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 10px rgba(160,108,213,.25);
}
.btn-add:disabled {
  background: var(--track); color: var(--muted);
  cursor: default; box-shadow: none;
}

/* 标签上的红点 */
.tab em { font-style: normal; }
.red-dot {
  display: inline-block; min-width: 16px; height: 16px; line-height: 16px;
  border-radius: 999px; background: var(--danger); color: #fff;
  font-size: 10px; text-align: center; padding: 0 4px; margin-left: 3px;
}

/* 名字旁的关系徽章 */
.rel-badge {
  display: inline-block; vertical-align: middle;
  font-size: 10px; font-weight: 700; color: var(--primary);
  background: var(--card); border: 1px solid var(--primary);
  border-radius: 999px; padding: 1px 8px; margin-left: 6px;
}

/* 好友行右侧操作组 */
.friend-ops { display: flex; gap: 6px; flex-shrink: 0; }
.friend-ops button {
  width: 30px; height: 30px; border-radius: 50%;
  border: none; cursor: pointer; font-size: 14px;
  background: var(--card); transition: all .15s;
}
.op-gift:hover { background: #ffe9f1; transform: scale(1.12); }
.op-bind:hover { background: #f3e8ff; transform: scale(1.12); }
.op-remark:hover { background: #e8f3ff; transform: scale(1.12); }

/* 备注输入框 */
.remark-input {
  width: 100%; box-sizing: border-box;
  border: 1.5px solid var(--border); border-radius: 12px;
  padding: 11px 13px; font-size: 14px; color: var(--text);
  background: var(--card); font-family: inherit;
  transition: border-color .15s, box-shadow .15s;
}
.remark-input:focus {
  outline: none; border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(160, 108, 213, .15);
}
.sheet-btns button.primary {
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}
.sheet-btns button.primary:disabled { opacity: .6; cursor: default; }

/* 绑定弹层 */
.sheet-mask {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(60, 30, 90, .35);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}
.sheet {
  width: 100%; max-width: var(--app-w); max-height: var(--app-h); overflow-y: auto;
  background: var(--card); border-radius: 20px;
  padding: 20px; border: 1.5px solid var(--border);
}
.sheet h3 { margin: 0 0 4px; font-size: 17px; }
.sheet-tip { margin: 0 0 14px; font-size: 12px; color: var(--muted); }
.rel-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.rel-grid button {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  border: 1.5px solid var(--border); border-radius: 12px;
  background: var(--panel); color: var(--text);
  padding: 10px 2px; cursor: pointer; transition: all .15s;
}
.rel-grid button:hover { border-color: var(--primary); }
.rel-grid button.on {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}
.rel-grid button span { font-size: 20px; }
.rel-grid button b { font-size: 11px; }
.rel-badges { margin-top: 14px; }
.rel-badges-title { font-size: 11.5px; color: var(--muted); margin-bottom: 6px; }
.badge-chip {
  display: inline-block; font-size: 11px; font-weight: 700;
  background: var(--panel); border: 1px solid var(--border);
  border-radius: 999px; padding: 3px 10px; margin: 0 5px 5px 0;
}
.sheet-btns { display: flex; gap: 8px; margin-top: 16px; }
.sheet-btns button {
  flex: 1; border: none; cursor: pointer; border-radius: 10px;
  padding: 9px 0; font-size: 13px; font-weight: 800;
  background: var(--panel); color: var(--muted);
}
.sheet-btns button.ghost:hover { color: var(--danger); }
</style>
