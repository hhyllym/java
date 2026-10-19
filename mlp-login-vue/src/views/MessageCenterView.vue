<template>
  <div class="home">
    <div class="home-top">
      <div class="logo">💬</div>
      <div class="home-greet">
        <h2>消息中心</h2>
        <p>留言板 · 私信 · 系统通知</p>
      </div>
      <button class="logout" @click="goHome">返回</button>
    </div>

    <div class="tabs">
      <a class="tab" :class="{ active: tab === 'board' }" @click="tab = 'board'">📝 留言板</a>
      <a class="tab" :class="{ active: tab === 'pm' }" @click="switchTab('pm')">
        💌 私信<span v-if="pmUnread" class="tbadge">{{ pmUnread }}</span>
      </a>
      <a class="tab" :class="{ active: tab === 'notify' }" @click="switchTab('notify')">
        🔔 通知<span v-if="notifyUnread" class="tbadge">{{ notifyUnread }}</span>
      </a>
    </div>

    <!-- ============ 留言板 ============ -->
    <div v-if="tab === 'board'">
      <form class="msg-compose" @submit.prevent="postBoard">
        <textarea
          v-model="boardText"
          maxlength="500"
          rows="3"
          placeholder="在小马信箱里留句话吧～（最多 500 字）"
        ></textarea>
        <div class="compose-bar">
          <span class="count">{{ boardText.length }}/500</span>
          <button class="submit mini" type="submit" :disabled="posting">
            {{ posting ? '发送中…' : '📮 发布' }}
          </button>
        </div>
      </form>

      <div v-if="board.loading && board.page === 1" class="empty">正在加载留言板…</div>
      <div v-else-if="!board.list.length" class="empty">还没有留言，来当第一个吧 ✨</div>

      <div class="msg-list">
        <div v-for="m in board.list" :key="m.id" class="msg-item">
          <span class="msg-avatar">{{ firstChar(m.fromName) }}</span>
          <div class="msg-body">
            <div class="msg-head">
              <b>{{ m.fromName }}</b>
              <span class="msg-time">{{ m.createTime }}</span>
            </div>
            <div class="msg-content">{{ m.content }}</div>
          </div>
        </div>
      </div>
      <button v-if="board.hasMore" class="more" :disabled="board.loading" @click="loadBoard(board.page + 1)">
        {{ board.loading ? '加载中…' : `加载更多（${board.list.length}/${board.total}）` }}
      </button>
    </div>

    <!-- ============ 私信 ============ -->
    <div v-if="tab === 'pm'">
      <div class="subtabs">
        <a class="tab mini" :class="{ active: pmBox === 'inbox' }" @click="switchPmBox('inbox')">📥 收件箱</a>
        <a class="tab mini" :class="{ active: pmBox === 'sent' }" @click="switchPmBox('sent')">📤 已发送</a>
        <a class="tab mini" :class="{ active: pmMode === 'compose' }" @click="pmMode = 'compose'">✏️ 写私信</a>
      </div>

      <form v-if="pmMode === 'compose'" class="msg-compose" @submit.prevent="sendPm">
        <input v-model.trim="pmTo" maxlength="20" placeholder="收件人小马名字" />
        <textarea v-model="pmText" maxlength="500" rows="3" placeholder="写点悄悄话…"></textarea>
        <div class="compose-bar">
          <span class="count">{{ pmText.length }}/500</span>
          <button class="submit mini" type="submit" :disabled="posting">
            {{ posting ? '发送中…' : '📨 发送' }}
          </button>
        </div>
      </form>

      <div v-if="pm.loading && pm.page === 1" class="empty">正在加载…</div>
      <div v-else-if="!pm.list.length" class="empty">
        {{ pmBox === 'inbox' ? '收件箱空空如也' : '还没有发出私信' }}
      </div>

      <div class="msg-list">
        <div
          v-for="m in pm.list"
          :key="m.id"
          class="msg-item"
          :class="{ unread: m.readFlag === 0 && pmBox === 'inbox' }"
          @click="openPm(m)"
        >
          <span class="msg-avatar">{{ firstChar(pmBox === 'inbox' ? m.fromName : m.toName) }}</span>
          <div class="msg-body">
            <div class="msg-head">
              <b>{{ pmBox === 'inbox' ? m.fromName : '→ ' + m.toName }}</b>
              <span class="msg-time">{{ m.createTime }}</span>
            </div>
            <div class="msg-content">{{ m.content }}</div>
          </div>
          <span v-if="m.readFlag === 0 && pmBox === 'inbox'" class="dot"></span>
        </div>
      </div>
      <button v-if="pm.hasMore" class="more" :disabled="pm.loading" @click="loadPm(pm.page + 1)">
        {{ pm.loading ? '加载中…' : `加载更多（${pm.list.length}/${pm.total}）` }}
      </button>
    </div>

    <!-- ============ 通知 ============ -->
    <div v-if="tab === 'notify'">
      <div v-if="notify.loading && notify.page === 1" class="empty">正在加载…</div>
      <div v-else-if="!notify.list.length" class="empty">暂无通知</div>

      <div class="msg-list">
        <div
          v-for="m in notify.list"
          :key="m.id"
          class="msg-item notify"
          :class="{ unread: m.readFlag === 0 }"
          @click="readNotify(m)"
        >
          <span class="msg-avatar sys">🔔</span>
          <div class="msg-body">
            <div class="msg-head">
              <b>{{ m.fromName }}</b>
              <span class="msg-time">{{ m.createTime }}</span>
            </div>
            <div class="msg-content">{{ m.content }}</div>
          </div>
          <span v-if="m.readFlag === 0" class="dot"></span>
        </div>
      </div>
      <button v-if="notify.hasMore" class="more" :disabled="notify.loading" @click="loadNotify(notify.page + 1)">
        {{ notify.loading ? '加载中…' : `加载更多（${notify.list.length}/${notify.total}）` }}
      </button>
    </div>
  </div>
</template>

<script>
import { getSession } from '../auth'
import {
  apiBoardList, apiBoardPost, apiPmList, apiPmSend, apiPmRead,
  apiNotifyList, apiNotifyRead, apiUnread
} from '../message'

function emptyPage() {
  return { list: [], total: 0, page: 1, hasMore: false, loading: false }
}

export default {
  name: 'MessageCenterView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      tab: 'board',
      posting: false,

      boardText: '',
      board: emptyPage(),

      pmBox: 'inbox',
      pmMode: 'list',
      pmTo: '',
      pmText: '',
      pm: emptyPage(),

      notify: emptyPage(),

      pmUnread: 0,
      notifyUnread: 0
    }
  },
  created() {
    this.loadBoard(1)
    this.refreshUnread()
  },
  methods: {
    goHome() {
      this.$router.push({ name: 'home' })
    },
    firstChar(name) {
      return name ? name.charAt(0) : '?'
    },
    async refreshUnread() {
      if (!this.session.id) return
      const r = await apiUnread(this.session.id)
      if (r.code === 200 && r.data) {
        this.pmUnread = r.data.pm || 0
        this.notifyUnread = r.data.notify || 0
      }
    },

    // ---------- 留言板 ----------
    async loadBoard(page) {
      this.board.loading = true
      const r = await apiBoardList(page, 20)
      this.board.loading = false
      if (r.code !== 200) {
        this.toast(r.msg || '留言板加载失败', 'err')
        return
      }
      const d = r.data || {}
      this.board.list = page === 1 ? (d.list || []) : this.board.list.concat(d.list || [])
      this.board.total = d.total || 0
      this.board.page = page
      this.board.hasMore = !!d.hasMore
    },
    async postBoard() {
      const content = this.boardText.trim()
      if (!content) {
        this.toast('留言内容不能为空', 'err')
        return
      }
      this.posting = true
      const r = await apiBoardPost(this.session.id, this.session.name, content)
      this.posting = false
      if (r.code !== 200) {
        this.toast(r.msg || '发布失败', 'err')
        return
      }
      this.toast('留言成功 🎉', 'ok')
      this.boardText = ''
      this.board.page = 1
      this.loadBoard(1)
    },

    // ---------- 私信 ----------
    switchTab(name) {
      this.tab = name
      if (name === 'pm' && !this.pm.list.length) this.loadPm(1)
      if (name === 'notify' && !this.notify.list.length) this.loadNotify(1)
    },
    switchPmBox(box) {
      this.pmBox = box
      this.pmMode = 'list'
      this.pm = emptyPage()
      this.loadPm(1)
    },
    async loadPm(page) {
      if (!this.session.id) return
      this.pm.loading = true
      const r = await apiPmList(this.pmBox, this.session.id, page, 15)
      this.pm.loading = false
      if (r.code !== 200) {
        this.toast(r.msg || '私信加载失败', 'err')
        return
      }
      const d = r.data || {}
      this.pm.list = page === 1 ? (d.list || []) : this.pm.list.concat(d.list || [])
      this.pm.total = d.total || 0
      this.pm.page = page
      this.pm.hasMore = !!d.hasMore
      this.refreshUnread()
    },
    async sendPm() {
      const toName = this.pmTo.trim()
      const content = this.pmText.trim()
      if (!toName) {
        this.toast('请填写收件人', 'err')
        return
      }
      if (!content) {
        this.toast('私信内容不能为空', 'err')
        return
      }
      this.posting = true
      const r = await apiPmSend(this.session.id, this.session.name, toName, content)
      this.posting = false
      if (r.code !== 200) {
        this.toast(r.msg || '发送失败', 'err')
        return
      }
      this.toast('已发送给 ' + toName + ' 💌', 'ok')
      this.pmTo = ''
      this.pmText = ''
      this.pmMode = 'list'
      this.pmBox = 'sent'
      this.pm = emptyPage()
      this.loadPm(1)
    },
    async openPm(m) {
      if (this.pmBox === 'inbox' && m.readFlag === 0) {
        const r = await apiPmRead(m.id, this.session.id)
        if (r.code === 200) {
          m.readFlag = 1
          this.refreshUnread()
        }
      }
    },

    // ---------- 通知 ----------
    async loadNotify(page) {
      if (!this.session.id) return
      this.notify.loading = true
      const r = await apiNotifyList(this.session.id, page, 15)
      this.notify.loading = false
      if (r.code !== 200) {
        this.toast(r.msg || '通知加载失败', 'err')
        return
      }
      const d = r.data || {}
      this.notify.list = page === 1 ? (d.list || []) : this.notify.list.concat(d.list || [])
      this.notify.total = d.total || 0
      this.notify.page = page
      this.notify.hasMore = !!d.hasMore
      this.refreshUnread()
    },
    async readNotify(m) {
      if (m.readFlag === 0 && this.session.id) {
        const r = await apiNotifyRead(m.id, this.session.id)
        if (r.code === 200) {
          m.readFlag = 1
          this.refreshUnread()
        }
      }
    }
  }
}
</script>

<style scoped>
/* 复用全局 .home / .tabs / .tab / .more / .empty，这里只补消息卡片专用样式 */
.msg-compose {
  margin: 14px 0 6px;
  padding: 0;
}
.msg-compose textarea,
.msg-compose input {
  width: 100%;
  border: 1.5px solid var(--border);
  border-radius: 12px;
  padding: 11px 13px;
  font-size: 14px;
  color: var(--text);
  background: var(--card);
  resize: none;
  font-family: inherit;
  transition: border-color .15s, box-shadow .15s;
}
.msg-compose textarea:focus,
.msg-compose input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(160, 108, 213, .15);
}
.compose-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.compose-bar .count {
  font-size: 11.5px;
  color: var(--muted);
}
.submit.mini {
  width: auto;
  padding: 8px 18px;
  font-size: 13px;
  border-radius: 999px;
}
.submit.mini:disabled {
  opacity: .6;
  cursor: default;
}

.subtabs {
  display: flex;
  gap: 4px;
  background: var(--panel);
  border-radius: 12px;
  padding: 4px;
  margin: 12px 0 4px;
}
.tab.mini {
  font-size: 12.5px;
  padding: 7px 0;
}

.msg-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.msg-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  background: var(--panel);
  border-radius: 14px;
  padding: 11px 12px;
  border: 1.5px solid transparent;
  transition: border-color .15s, transform .12s;
  cursor: pointer;
}
.msg-item:hover {
  border-color: var(--primary);
}
.msg-item.unread {
  background: #fff;
  border-color: var(--pink);
}
.msg-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
.msg-avatar.sys {
  background: linear-gradient(135deg, var(--yellow), var(--purple));
}
.msg-body {
  flex: 1;
  min-width: 0;
}
.msg-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.msg-head b {
  font-size: 13.5px;
}
.msg-time {
  font-size: 10.5px;
  color: var(--muted);
  flex-shrink: 0;
}
.msg-content {
  font-size: 13px;
  line-height: 1.55;
  margin-top: 3px;
  word-break: break-word;
  white-space: pre-wrap;
}
.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--danger);
  flex-shrink: 0;
  align-self: center;
  box-shadow: 0 0 0 3px rgba(224, 102, 102, .18);
}
.tbadge {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  padding: 0 4px;
  margin-left: 4px;
  border-radius: 999px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}
</style>
