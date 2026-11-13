<template>
  <div class="home hall">
    <!-- 顶部 -->
    <div class="home-top">
      <div class="logo">🏠</div>
      <div class="home-greet">
        <h2>星糖大厅</h2>
        <p>所有小马都在这儿 · 聊聊天 · 约一局</p>
      </div>
      <button class="logout" @click="goHome">返回</button>
    </div>

    <!-- 概览 -->
    <div class="hall-stats">
      <div class="hs-item"><b>{{ online.length }}</b><span>在线</span></div>
      <div class="hs-item"><b class="warn">{{ rooms.length }}</b><span>邀请</span></div>
      <div class="hs-item"><b>{{ messages.length }}</b><span>大厅消息</span></div>
    </div>

    <!-- 两个标签页 -->
    <div class="tabs">
      <div class="tab" :class="{ active: tab === 'chat' }" @click="tab = 'chat'">
        💬 大厅聊天<em v-if="online.length">({{ online.length }})</em>
      </div>
      <div class="tab" :class="{ active: tab === 'game' }" @click="tab = 'game'">
        🎮 发起游戏<em v-if="rooms.length" class="red-dot">{{ rooms.length }}</em>
      </div>
    </div>

    <!-- ============ 大厅聊天 ============ -->
    <div v-show="tab === 'chat'" class="tab-pane hall-chat-pane">
      <!-- 在线的小马 -->
      <div class="hall-side">
        <div class="section-title">🌟 在线的小马 <em>{{ online.length }}</em></div>
        <div v-if="!online.length" class="empty small">暂时只有你在大厅里</div>
        <div v-else class="online-list">
          <div v-for="u in online" :key="u.id" class="online-item">
            <UserAvatar :user="u" :size="26" />
            <span class="online-name">{{ u.name }}</span>
          </div>
        </div>
      </div>

      <!-- 消息流 -->
      <div class="hall-chat">
        <div class="hall-msg-list" ref="msgBox">
          <div v-if="!messages.length" class="empty">大厅还很安静，来发第一条消息吧～</div>
          <div
            v-for="m in messages"
            :key="m.id + '' + m.time"
            class="hall-msg"
            :class="{ mine: m.userId === session.id, sys: m.type === 'system' }"
          >
            <template v-if="m.type === 'system'">
              <span class="sys-tip">{{ m.content }}</span>
            </template>
            <template v-else>
              <UserAvatar :user-id="m.userId" :avatar="m.avatar" :name="m.name" :size="30" />
              <div class="msg-body">
                <div class="msg-head">
                  <b>{{ m.name }}</b>
                  <em>{{ m.time }}</em>
                </div>
                <div class="msg-bubble">{{ m.content }}</div>
              </div>
            </template>
          </div>
        </div>

        <form class="hall-input-row" @submit.prevent="send">
          <input
            v-model.trim="draft"
            type="text"
            maxlength="500"
            placeholder="在大厅里说点什么…（Enter 发送）"
          />
          <button type="submit" :disabled="sending || !draft">发送</button>
        </form>
        <p class="pane-tip">💡 消息每 3 秒自动刷新，刷新页面也不会丢</p>
      </div>
    </div>

    <!-- ============ 发起游戏 ============ -->
    <div v-show="tab === 'game'" class="tab-pane">
      <!-- 发起邀请 -->
      <div class="section-title">🎮 发起一个邀请</div>
      <div class="invite-form">
        <select v-model="newRoom.game" class="mini-select" title="选游戏">
          <option v-for="g in gameOptions" :key="g.id" :value="g.id">{{ g.icon }} {{ g.name }}</option>
        </select>
        <input
          v-model.trim="newRoom.title"
          type="text"
          maxlength="30"
          placeholder="房间标题（留空用游戏名）"
        />
        <select v-model.number="newRoom.maxMembers" class="mini-select" title="人数">
          <option :value="2">2 人</option>
          <option :value="3">3 人</option>
          <option :value="4">4 人</option>
        </select>
        <button class="invite-btn" :disabled="creating" @click="createRoom">
          {{ creating ? '发起中…' : '发起邀请' }}
        </button>
      </div>

      <!-- 邀请列表 -->
      <div class="section-title gap">📣 正在进行的邀请 <em>{{ rooms.length }}</em></div>
      <div v-if="!rooms.length" class="empty small">还没有人发起邀请，你先来？ 🐴</div>
      <div v-else class="room-list">
        <div v-for="r in rooms" :key="r.id" class="room-item">
          <div class="room-main">
            <b>
              {{ gameIcon(r.game) }} {{ r.title }}
              <span class="room-code">#{{ r.id }}</span>
            </b>
            <em>
              {{ r.hostName }} 发起 · {{ r.count }}/{{ r.maxMembers }} 人
              · {{ r.gameName }}
            </em>
            <div class="room-members">
              <span v-for="m in r.members" :key="m.userId" class="room-member">
                <UserAvatar :user-id="m.userId" :avatar="m.avatar" :name="m.name" :size="20" />
                {{ m.name }}
                <i v-if="m.host">👑</i>
              </span>
            </div>
          </div>
          <div class="room-ops">
            <button class="op-join" @click="joinRoom(r)">加入</button>
            <button class="op-go" title="进游戏" @click="enterGame(r)">进入</button>
            <button
              v-if="r.hostId === session.id"
              class="room-close"
              title="关闭邀请"
              @click="closeRoom(r)"
            >✕</button>
          </div>
        </div>
      </div>
      <p class="pane-tip">💡 邀请 2 小时内有效；点「进入」跳到游戏页，大富翁还能直接进同一间房</p>
    </div>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { GAMES } from '../courses'
import {
  apiHallMessages, apiHallSend, apiHallOnline, apiHallRooms,
  apiHallCreateRoom, apiHallJoinRoom, apiHallCloseRoom
} from '../hall'
import { notify, HALL_SEEN_KEY } from '../notify'
import { apiPresenceBeat } from '../presence'
import UserAvatar from '../components/UserAvatar.vue'

export default {
  name: 'HallView',
  components: { UserAvatar },
  data() {
    return {
      session: getSession() || {},
      tab: 'chat',
      messages: [],
      online: [],
      rooms: [],
      draft: '',
      sending: false,
      creating: false,
      newRoom: { game: 'monopoly', title: '', maxMembers: 4 },
      msgTimer: null,
      roomTimer: null
    }
  },
  computed: {
    /** 可邀请的游戏：排除单人答题类 */
    gameOptions() {
      const skip = ['twilight', 'applejack', 'dash', 'match3']
      return GAMES.filter(g => !skip.includes(g.id))
    }
  },
  async created() {
    // 进大厅先报个到，这样「在线的小马」里立刻能看到自己
    if (this.session.id) apiPresenceBeat(this.session.id)
    await this.refreshAll()
    // 聊天 3 秒一轮、房间和在线 6 秒一轮
    this.msgTimer = setInterval(this.refreshMessages, 3000)
    this.roomTimer = setInterval(this.refreshRooms, 6000)
  },
  beforeUnmount() {
    if (this.msgTimer) clearInterval(this.msgTimer)
    if (this.roomTimer) clearInterval(this.roomTimer)
  },
  methods: {
    async refreshAll() {
      await Promise.all([this.refreshMessages(), this.refreshRooms(), this.refreshOnline()])
    },
    async refreshMessages() {
      const res = await apiHallMessages(100)
      if (res.code === 200 && Array.isArray(res.data)) {
        this.messages = res.data
        this.markSeen()
        this.$nextTick(this.scrollBottom)
      }
    },
    /** 记下「已读到哪一条」，导航栏的大厅红点随之熄灭 */
    markSeen() {
      const last = this.messages.length ? this.messages[this.messages.length - 1].id : 0
      if (!last) return
      try { localStorage.setItem(HALL_SEEN_KEY, String(last)) } catch (e) { /* 隐私模式忽略 */ }
      if (notify.hallLatest < last) notify.hallLatest = last
    },
    async refreshOnline() {
      const res = await apiHallOnline()
      if (res.code === 200 && Array.isArray(res.data)) this.online = res.data
    },
    async refreshRooms() {
      const res = await apiHallRooms()
      if (res.code === 200 && Array.isArray(res.data)) this.rooms = res.data
    },
    scrollBottom() {
      const el = this.$refs.msgBox
      if (el) el.scrollTop = el.scrollHeight
    },
    async send() {
      const text = this.draft
      if (!text || this.sending) return
      this.sending = true
      const res = await apiHallSend(this.session.id, text)
      this.sending = false
      if (res.code === 200) {
        this.draft = ''
        await this.refreshMessages()
      } else {
        this.toast(res.msg || '发送失败')
      }
    },
    async createRoom() {
      if (this.creating) return
      this.creating = true
      const res = await apiHallCreateRoom(
        this.session.id, this.newRoom.game, this.newRoom.title, this.newRoom.maxMembers
      )
      this.creating = false
      if (res.code === 200) {
        this.newRoom.title = ''
        await this.refreshRooms()
        this.toast('🎉 邀请发出去啦！')
      } else {
        this.toast(res.msg || '发起失败')
      }
    },
    async joinRoom(r) {
      const res = await apiHallJoinRoom(this.session.id, r.id)
      if (res.code === 200) {
        await this.refreshRooms()
        this.toast('🎉 加入成功！')
      } else {
        this.toast(res.msg || '加入失败')
      }
    },
    async closeRoom(r) {
      const res = await apiHallCloseRoom(this.session.id, r.id)
      if (res.code === 200) {
        await this.refreshRooms()
        this.toast('邀请已关闭')
      } else {
        this.toast(res.msg || '关闭失败')
      }
    },
    /** 进游戏：大富翁带 room 参数直接进同一间房，其它游戏进游戏页即可 */
    enterGame(r) {
      if (r.game === 'monopoly') {
        this.$router.push({ name: 'game', params: { id: 'monopoly' }, query: { room: r.id } })
      } else {
        this.$router.push({ name: 'game', params: { id: r.game } })
      }
    },
    gameIcon(id) {
      const g = GAMES.find(x => x.id === id)
      return g ? g.icon : '🎮'
    },
    goHome() {
      this.$router.push({ name: 'home' })
    },
    toast(msg) {
      const el = document.querySelector('.app-shell .toast')
      if (el) {
        el.textContent = msg
        el.classList.add('show')
        clearTimeout(this._toastTimer)
        this._toastTimer = setTimeout(() => el.classList.remove('show'), 2200)
      }
    }
  }
}
</script>

<style scoped>
.hall-stats {
  display: flex;
  gap: 10px;
  margin: 12px 0;
}
.hs-item {
  flex: 1;
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 10px 6px;
  text-align: center;
}
.hs-item b {
  display: block;
  font-size: 20px;
  color: var(--primary);
}
.hs-item b.warn { color: var(--pink); }
.hs-item span {
  font-size: 12px;
  color: var(--muted);
}

/* ---------- 聊天页：左在线、右消息 ---------- */
.hall-chat-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.hall-side {
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 10px 12px;
}
.online-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.online-item {
  display: flex;
  align-items: center;
  gap: 5px;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 3px 10px 3px 3px;
  font-size: 13px;
}
.online-name { color: var(--text); }

.hall-chat {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hall-msg-list {
  height: 46vh;
  min-height: 240px;
  overflow-y: auto;
  background: var(--panel);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hall-msg {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.hall-msg.mine {
  flex-direction: row-reverse;
}
.msg-body { max-width: 76%; }
.msg-head {
  display: flex;
  gap: 6px;
  align-items: baseline;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 2px;
}
.hall-msg.mine .msg-head { flex-direction: row-reverse; }
.msg-head b { color: var(--text); font-size: 13px; }
.msg-bubble {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 7px 11px;
  font-size: 14px;
  color: var(--text);
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
.hall-msg.mine .msg-bubble {
  background: var(--primary);
  color: #fff;
  border-color: transparent;
}
.hall-msg.sys {
  justify-content: center;
}
.sys-tip {
  font-size: 12px;
  color: var(--muted);
  background: var(--card);
  border: 1px dashed var(--border);
  border-radius: 999px;
  padding: 3px 12px;
}

.hall-input-row {
  display: flex;
  gap: 8px;
}
.hall-input-row input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 9px 14px;
  font-size: 14px;
  background: var(--card);
  color: var(--text);
}
.hall-input-row button {
  border: none;
  border-radius: 999px;
  padding: 9px 20px;
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.hall-input-row button:disabled {
  opacity: .5;
  cursor: not-allowed;
}

/* ---------- 发起邀请 ---------- */
.invite-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.invite-form input {
  flex: 1 1 140px;
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 8px 13px;
  font-size: 14px;
  background: var(--card);
  color: var(--text);
}
.invite-form .invite-btn {
  border: none;
  border-radius: 999px;
  padding: 8px 18px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}
.invite-form .invite-btn:disabled { opacity: .5; }

.room-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.room-item {
  display: flex;
  gap: 10px;
  align-items: center;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 11px 13px;
}
.room-main { flex: 1; min-width: 0; }
.room-main b {
  display: block;
  font-size: 15px;
  color: var(--text);
}
.room-code {
  font-size: 12px;
  color: var(--primary);
  background: var(--panel);
  border-radius: 6px;
  padding: 1px 6px;
  margin-left: 4px;
}
.room-main em {
  display: block;
  font-style: normal;
  font-size: 12px;
  color: var(--muted);
  margin: 3px 0 5px;
}
.room-members {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.room-member {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text);
  background: var(--panel);
  border-radius: 999px;
  padding: 2px 9px 2px 2px;
}
.room-member i { font-style: normal; }
.room-ops {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.room-ops button {
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--text);
  border-radius: 10px;
  padding: 6px 11px;
  font-size: 13px;
  cursor: pointer;
}
.room-ops .op-join {
  background: var(--primary);
  color: #fff;
  border-color: transparent;
}
.room-ops .op-go {
  background: var(--mint);
  border-color: transparent;
  color: #2f4f42;
}
.room-ops .room-close { color: var(--danger); }
</style>
