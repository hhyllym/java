<template>
  <div class="home chat-page">
    <!-- 顶部：返回 + 好友名字（有备注显示备注）+ 快捷打电话 -->
    <div class="home-top">
      <button class="chat-back" title="返回好友列表" @click="goBack">‹</button>
      <div class="friend-face chat-head-face" :style="{ background: faceBg }">{{ name.slice(0, 1) }}</div>
      <div class="home-greet">
        <h2>{{ remark || name }}</h2>
        <p>{{ remark ? '备注 ' + name + ' · ' : '' }}ID: {{ id }} · {{ typingHint || onlineText }}</p>
      </div>
      <span class="online-badge" :class="onlineClass" :title="friendOnline ? '对方当前在线' : (friendOnline === false ? '对方暂时离线' : '在线状态未知')">
        {{ onlineClass === 'on' ? '🟢 在线' : onlineClass === 'off' ? '⚪ 离线' : '💬 状态未知' }}
      </span>
      <button class="chat-call-btn" title="语音通话" @click="startVoiceCall">📞</button>
    </div>

    <!-- 消息区 -->
    <div ref="msgBox" class="msg-box" @click="closeMenu">
      <div v-if="loading" class="empty">正在加载聊天记录…</div>
      <template v-else>
        <div v-if="!messages.length" class="chat-empty">
          <span>🦄</span>
          <p>还没有消息，跟 {{ name }} 打个招呼吧～</p>
        </div>
        <template v-for="(m, i) in messages" :key="m.id">
          <!-- 时间分隔线：第一条，或与上一条间隔 ≥5 分钟 -->
          <div v-if="needTimeSep(m, i)" class="time-sep"><span>{{ m.time }}</span></div>
          <div class="bubble-row" :class="{ mine: m.mine }">
            <span v-if="!m.mine" class="bubble-face" :style="{ background: faceBg }">{{ name.slice(0, 1) }}</span>
            <div class="bubble" :class="{ mine: m.mine, call: m.type === 'call' }">
              <!-- 撤回态 -->
              <span v-if="m.recalled" class="bubble-text recalled">{{ m.mine ? '你撤回了一条消息' : '对方撤回了一条消息' }}</span>
              <!-- 通话卡片 -->
              <div v-else-if="m.type === 'call'" class="call-card">
                <span class="call-icon">{{ callAnswered(m) ? '📞' : '📕' }}</span>
                <div class="call-info">
                  <b>{{ callAnswered(m) ? '语音通话' : '未接来电' }}</b>
                  <em>{{ callAnswered(m) ? '通话时长 ' + fmtDur(callSeconds(m)) : '无人接听' }}</em>
                </div>
                <button class="call-again" @click.stop="startVoiceCall">再打一次</button>
              </div>
              <!-- 图片消息 -->
              <div v-else-if="m.type === 'image'" class="bubble-img-wrap" @click.stop="previewImage(m)">
                <img class="bubble-img" :src="m.content" alt="聊天图片" />
              </div>
              <!-- 语音消息 -->
              <div v-else-if="m.type === 'audio'" class="bubble-audio">
                <span class="audio-ico">🎵</span>
                <audio controls preload="none" :src="m.content"></audio>
              </div>
              <!-- 文本 -->
              <span v-else class="bubble-text">{{ m.content }}</span>
              <span v-if="!m.recalled && m.type !== 'call'" class="bubble-time">{{ m.time }}</span>
              <!-- 自己的消息：hover 出操作按钮 -->
              <button v-if="m.mine && !m.recalled && m.type !== 'call'" class="bubble-more" title="消息操作" @click.stop="openMenu(m, $event)">⋯</button>
            </div>
          </div>
        </template>
        <div v-if="typingHint" class="typing-hint">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
        </div>
      </template>
    </div>

    <!-- 输入区 -->
    <div class="chat-input-bar">
      <button class="chat-tool" title="发语音" :class="{ on: recording }" @click="toggleRecord">🎤</button>
      <button class="chat-tool" title="表情" :class="{ on: emojiOpen }" @click="emojiOpen = !emojiOpen">😊</button>
      <button class="chat-tool" title="发图片" @click="pickImage">🖼️</button>
      <input ref="imgInput" type="file" accept="image/*" hidden @change="onImagePicked" />
      <input
        v-model="draft"
        type="text"
        placeholder="说点什么…"
        maxlength="500"
        @input="onTyping"
        @keyup.enter="send"
      />
      <button class="chat-send" :disabled="!draft.trim() || sending" @click="send">发送</button>
    </div>

    <!-- 录音中提示条 -->
    <div v-if="recording" class="rec-bar">
      <span class="rec-dot"></span>
      <span class="rec-time">{{ fmtDur(recSeconds) }}</span>
      <span class="rec-tip">正在录音…</span>
      <button class="rec-cancel" @click="cancelRecord">✕ 取消</button>
      <button class="rec-send" @click="stopRecord">⏹ 发送</button>
    </div>

    <!-- 表情面板 -->
    <div v-if="emojiOpen" class="emoji-panel">
      <button v-for="e in EMOJIS" :key="e" class="emoji-btn" @click="sendEmoji(e)">{{ e }}</button>
    </div>

    <!-- 消息操作菜单（复制 / 撤回） -->
    <div v-if="menu" class="msg-menu" :style="{ top: menu.y + 'px', left: menu.x + 'px' }" @click.stop>
      <button @click="copyMsg(menu.msg)">📋 复制</button>
      <button v-if="menu.msg.mine" class="danger" @click="recallMsg(menu.msg)">↩️ 撤回</button>
    </div>

    <!-- 图片全屏预览 -->
    <Teleport to="body">
      <div v-if="preview" class="img-preview" @click.self="preview = null">
        <img :src="preview" alt="图片预览" />
        <button class="img-preview-close" title="关闭" @click="preview = null">✕</button>
      </div>
    </Teleport>

    <!-- 语音通话全屏界面 -->
    <Teleport to="body">
      <div v-if="call" class="call-mask">
        <div class="call-body">
          <div class="call-avatar" :class="{ ring: call.state === 'calling' }" :style="{ background: faceBg }">
            {{ name.slice(0, 1) }}
          </div>
          <h3>{{ name }}</h3>
          <p class="call-status">{{ callStatusText }}</p>
          <p v-if="call.state === 'connected'" class="call-timer">{{ callTimerText }}</p>
          <div class="call-actions">
            <button v-if="call.state === 'connected'" class="call-ctl" :class="{ on: call.muted }" title="静音" @click="call.muted = !call.muted">{{ call.muted ? '🔇' : '🎙️' }}</button>
            <button class="call-hangup" title="挂断" @click="hangUp">📵 {{ call.state === 'connected' ? '挂断' : (call.state === 'missed' ? '知道了' : '取消') }}</button>
            <button v-if="call.state === 'connected'" class="call-ctl" title="免提" :class="{ on: call.speaker }" @click="call.speaker = !call.speaker">{{ call.speaker ? '🔊' : '🔈' }}</button>
          </div>
          <p class="call-tip">语音通话（界面演示）· 挂断后会在聊天里留下记录</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { apiHistory, apiSend, apiSendImage, apiRecall, apiCallLog, apiMarkRead } from '../chat'
import { apiPresenceStatus } from '../presence'
import sound from '../sound'

/** 表情面板：小马王国常用萌系表情 */
const EMOJIS = ['😊', '😂', '🥰', '😍', '🤩', '😘', '😜', '🤗', '😴', '😎',
  '🦄', '🐴', '🐲', '🐱', '🐶', '🐰', '🐦', '🦋', '🐝', '🌟',
  '⭐', '🌈', '💖', '💎', '🎀', '🧁', '🍎', '🍰', '🎉', '🎁',
  '👍', '👋', '🙏', '💪', '✌️', '🤔', '😅', '🥳', '😇', '🤍']

export default {
  name: 'ChatView',
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      id: this.$route.params.id,
      name: this.$route.query.name || '小马',
      remark: this.$route.query.remark || '',
      messages: [],
      draft: '',
      loading: true,
      sending: false,
      pollTimer: null,
      EMOJIS,
      // 表情面板
      emojiOpen: false,
      // 图片预览
      preview: null,
      // 消息操作菜单
      menu: null,
      // 「对方正在输入」本地演示
      typingHint: '',
      typingTimer: null,
      // 对方在线状态：null=未知, true=在线, false=离线
      friendOnline: null,
      // 通话状态
      call: null,
      callTimer: null,
      // 录音状态
      recording: false,
      recSeconds: 0,
      recChunks: [],
      mediaRecorder: null,
      recStream: null,
      recTimer: null,
      recDiscard: false
    }
  },
  computed: {
    faceBg() {
      let h = 0
      const s = this.name || ''
      for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 997
      const colors = ['#f7a8c4', '#b79cf0', '#8fc7f0', '#a7e3c9', '#f7e08a', '#f0a06a', '#c9a0e8']
      return colors[h % colors.length]
    },
    callStatusText() {
      if (!this.call) return ''
      if (this.call.state === 'calling') return '正在呼叫…'
      if (this.call.state === 'missed') return '对方暂时不在线'
      return '通话中'
    },
    callTimerText() {
      return this.fmtDur(this.call ? this.call.seconds : 0)
    },
    onlineText() {
      if (this.friendOnline === true) return '在线'
      if (this.friendOnline === false) return '离线'
      return '好友'
    },
    onlineClass() {
      if (this.friendOnline === true) return 'on'
      if (this.friendOnline === false) return 'off'
      return 'unknown'
    }
  },
  created() {
    this.loadHistory(true)
    // 每 3 秒轮询一次新消息（简单可靠，课程项目够用）
    this.pollTimer = setInterval(this.poll, 3000)
  },
  beforeUnmount() {
    if (this.pollTimer) clearInterval(this.pollTimer)
    if (this.typingTimer) clearTimeout(this.typingTimer)
    if (this.callTimer) clearInterval(this.callTimer)
    if (this.recTimer) clearInterval(this.recTimer)
    // 离开聊天页时若仍在录音，停掉麦克风
    this.forceStopRecorder()
  },
  methods: {
    goBack() {
      this.$router.push({ name: 'friends' })
    },
    fmtDur(sec) {
      const m = Math.floor(sec / 60)
      const s = sec % 60
      return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
    },
    callSeconds(m) {
      const n = parseInt(m.extra || '0', 10)
      return isNaN(n) ? 0 : n
    },
    callAnswered(m) {
      return this.callSeconds(m) > 0
    },
    async loadHistory(first) {
      const res = await apiHistory(this.session.id, this.id)
      this.loading = false
      if (res.code === 200) {
        this.messages = res.data || []
        this.$nextTick(this.scrollBottom)
      } else if (first) {
        this.toast(res.msg || '聊天记录加载失败', 'err')
      }
      // 打开聊天就把对方发来的消息标为已读
      apiMarkRead(this.session.id, this.id)
      this.refreshPresence()
    },
    async poll() {
      const res = await apiHistory(this.session.id, this.id)
      if (res.code !== 200) return
      const list = res.data || []
      if (list.length !== this.messages.length) {
        const lastId = this.messages.length ? this.messages[this.messages.length - 1].id : 0
        this.messages = list
        this.$nextTick(this.scrollBottom)
        // 收到新消息就标记已读
        if (list.length && list[list.length - 1].id !== lastId) {
          apiMarkRead(this.session.id, this.id)
        }
      }
      this.refreshPresence()
    },
    async send() {
      const text = this.draft.trim()
      if (!text || this.sending) return
      this.sending = true
      const res = await apiSend(this.session.id, this.id, text)
      this.sending = false
      if (res.code === 200) {
        this.draft = ''
        this.messages.push(res.data)
        this.$nextTick(this.scrollBottom)
      } else {
        this.toast(res.msg || '发送失败', 'err')
      }
    },
    /** 点表情直接发出去 */
    async sendEmoji(e) {
      this.emojiOpen = false
      const res = await apiSend(this.session.id, this.id, e)
      if (res.code === 200) {
        this.messages.push(res.data)
        this.$nextTick(this.scrollBottom)
      } else {
        this.toast(res.msg || '发送失败', 'err')
      }
    },
    /** 输入时显示「对方正在输入」演示提示（3 秒后消失） */
    onTyping() {
      this.typingHint = '对方正在输入…'
      clearTimeout(this.typingTimer)
      this.typingTimer = setTimeout(() => { this.typingHint = '' }, 3000)
    },
    pickImage() {
      this.$refs.imgInput && this.$refs.imgInput.click()
    },
    async onImagePicked(ev) {
      const file = ev.target.files && ev.target.files[0]
      ev.target.value = ''
      if (!file) return
      if (!file.type.startsWith('image/')) {
        this.toast('只能发送图片文件哦', 'err')
        return
      }
      if (file.size > 3 * 1024 * 1024) {
        this.toast('图片超过 3MB，换一张小一点的吧', 'err')
        return
      }
      const dataUrl = await new Promise((resolve, reject) => {
        const r = new FileReader()
        r.onload = () => resolve(r.result)
        r.onerror = reject
        r.readAsDataURL(file)
      }).catch(() => null)
      if (!dataUrl) {
        this.toast('图片读取失败', 'err')
        return
      }
      this.sending = true
      const res = await apiSendImage(this.session.id, this.id, dataUrl)
      this.sending = false
      if (res.code === 200) {
        this.messages.push(res.data)
        this.$nextTick(this.scrollBottom)
        this.toast('图片已发送 📷', 'ok')
      } else {
        this.toast(res.msg || '图片发送失败', 'err')
      }
    },
    previewImage(m) {
      this.preview = m.content
    },
    /** 消息操作菜单 */
    openMenu(m, ev) {
      const rect = ev.target.getBoundingClientRect()
      this.menu = { msg: m, x: rect.left - 90, y: rect.bottom + 6 }
    },
    closeMenu() {
      this.menu = null
    },
    async copyMsg(m) {
      this.menu = null
      const text = m.type === 'image' ? '[图片]' : m.content
      try {
        await navigator.clipboard.writeText(text)
        this.toast('已复制', 'ok')
      } catch (e) {
        // 剪贴板 API 不可用时降级
        const ta = document.createElement('textarea')
        ta.value = text
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.toast('已复制', 'ok')
      }
    },
    async recallMsg(m) {
      this.menu = null
      const res = await apiRecall(this.session.id, m.id)
      if (res.code === 200) {
        const target = this.messages.find(x => x.id === m.id)
        if (target) {
          target.recalled = true
          target.content = ''
        }
        this.toast('消息已撤回', 'ok')
      } else {
        this.toast(res.msg || '撤回失败', 'err')
      }
    },
    /** 时间分隔线：第一条显示；与上一条间隔 ≥5 分钟显示 */
    needTimeSep(m, i) {
      if (i === 0) return true
      const prev = this.messages[i - 1]
      const a = this.toMinutes(prev.time)
      const b = this.toMinutes(m.time)
      if (a === null || b === null) return prev.time !== m.time
      return b - a >= 5
    },
    /** 'HH:mm' → 当天分钟数；'MM-dd HH:mm' → null（跨天直接显示） */
    toMinutes(t) {
      const m = /^(\d{2}):(\d{2})$/.exec(t || '')
      return m ? parseInt(m[1], 10) * 60 + parseInt(m[2], 10) : null
    },
    /** ========== 语音通话（界面演示 + 记录落库） ==========
     *  修复点：先查对方是否在线，离线就直接提示"无法拨通"，不再自己接通 */
    async startVoiceCall() {
      this.closeMenu()
      this.emojiOpen = false
      if (this.call) return
      // 实时查对方在线状态
      let online = this.friendOnline
      try {
        const res = await apiPresenceStatus([this.id])
        if (res.code === 200 && res.data) online = !!res.data[String(this.id)]
      } catch (e) {}
      this.friendOnline = online
      if (!online) {
        // 对方离线：不能拨通，提示并落一条"未接来电"记录
        this.toast('好友暂时不在线，无法拨通 💤', 'err')
        this.call = { state: 'missed', seconds: 0, muted: false, speaker: false }
        return
      }
      // 对方在线：先响铃 2.5 秒再接通
      this.call = { state: 'calling', seconds: 0, muted: false, speaker: false }
      setTimeout(() => {
        if (!this.call || this.call.state !== 'calling') return
        this.call.state = 'connected'
        this.toast('通话已接通 📞', 'ok')
        this.callTimer = setInterval(() => {
          if (this.call && this.call.state === 'connected') this.call.seconds += 1
        }, 1000)
      }, 2500)
    },
    async hangUp() {
      if (!this.call) return
      const state = this.call.state
      const answered = state === 'connected'
      const missed = state === 'missed'
      const seconds = this.call.seconds || 0
      clearInterval(this.callTimer)
      this.callTimer = null
      this.call = null
      const res = await apiCallLog(this.session.id, this.id, seconds, answered)
      if (res.code === 200) {
        this.messages.push({
          id: 'call-' + Date.now(),
          mine: true,
          type: 'call',
          recalled: false,
          extra: String(answered ? seconds : 0),
          content: '',
          time: this.nowText()
        })
        this.$nextTick(this.scrollBottom)
        if (answered) this.toast(`通话结束，时长 ${this.fmtDur(seconds)}`, 'ok')
        else if (missed) this.toast('已记录未接来电', 'ok')
        else this.toast('已取消呼叫', 'ok')
      } else {
        this.toast(res.msg || '通话记录保存失败', 'err')
      }
    },
    /** ========== 发语音 ========== */
    toggleRecord() {
      if (this.recording) this.stopRecord()
      else this.startRecord()
    },
    async startRecord() {
      if (this.recording) return
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || typeof MediaRecorder === 'undefined') {
        this.toast('当前浏览器不支持录音，换 Chrome / Edge 试试', 'err')
        return
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        this.recStream = stream
        this.recChunks = []
        const mr = new MediaRecorder(stream)
        this.mediaRecorder = mr
        mr.ondataavailable = (e) => { if (e.data && e.data.size) this.recChunks.push(e.data) }
        mr.onstop = () => this.onRecStop()
        mr.start()
        this.recording = true
        this.recSeconds = 0
        this.recDiscard = false
        this.recTimer = setInterval(() => { this.recSeconds += 1 }, 1000)
        sound.click()
      } catch (e) {
        this.toast('麦克风权限被拒绝，无法录音', 'err')
        this.cleanupRecorder()
      }
    },
    stopRecord() {
      if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') this.mediaRecorder.stop()
      else this.onRecStop()
    },
    cancelRecord() {
      this.recDiscard = true
      if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') this.mediaRecorder.stop()
      else this.cleanupRecorder()
    },
    onRecStop() {
      if (this.recTimer) { clearInterval(this.recTimer); this.recTimer = null }
      const stream = this.recStream
      if (stream) stream.getTracks().forEach(t => t.stop())
      this.recStream = null
      const mr = this.mediaRecorder
      this.mediaRecorder = null
      const chunks = this.recChunks
      const seconds = this.recSeconds
      const discard = this.recDiscard
      this.recording = false
      this.recDiscard = false
      this.recChunks = []
      if (discard) return
      if (!chunks.length || seconds < 1) {
        this.toast('说得太短啦，再录久一点', 'err')
        return
      }
      const type = (mr && mr.mimeType) || 'audio/webm'
      const blob = new Blob(chunks, { type })
      const reader = new FileReader()
      reader.onload = () => this.sendAudio(reader.result)
      reader.onerror = () => this.toast('语音读取失败', 'err')
      reader.readAsDataURL(blob)
    },
    /** 离开页面时强制停掉麦克风，避免设备一直被占用 */
    forceStopRecorder() {
      if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
        try { this.mediaRecorder.stop() } catch (e) {}
      }
      this.cleanupRecorder()
    },
    cleanupRecorder() {
      if (this.recTimer) { clearInterval(this.recTimer); this.recTimer = null }
      if (this.recStream) { this.recStream.getTracks().forEach(t => t.stop()); this.recStream = null }
      this.mediaRecorder = null
      this.recording = false
    },
    async sendAudio(dataUrl) {
      const res = await apiSend(this.session.id, this.id, dataUrl, 'audio')
      if (res.code === 200) {
        this.messages.push(res.data)
        this.$nextTick(this.scrollBottom)
        sound.click()
      } else {
        this.toast(res.msg || '语音发送失败', 'err')
      }
    },
    nowText() {
      const d = new Date()
      return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
    },
    scrollBottom() {
      const box = this.$refs.msgBox
      if (box) box.scrollTop = box.scrollHeight
    }
  }
}
</script>

<style scoped>
/* 聊天页内容区撑满：收窄 .home 的左右内边距，消息/输入区可用宽度最大化 */
.chat-page { display: flex; flex-direction: column; height: 100%; padding-left: 10px; padding-right: 10px; }
.chat-back {
  border: none; background: var(--panel); color: var(--muted);
  width: 32px; height: 32px; border-radius: 50%; cursor: pointer;
  font-size: 20px; line-height: 1; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  transition: all .15s;
}
.chat-back:hover { color: var(--primary); }
.chat-head-face {
  width: 40px; height: 40px; font-size: 17px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700;
}
.chat-call-btn {
  border: none; background: var(--panel); color: var(--primary);
  width: 34px; height: 34px; border-radius: 50%; cursor: pointer;
  font-size: 16px; flex-shrink: 0; transition: all .15s;
}
.chat-call-btn:hover { background: #e8f7ee; transform: scale(1.08); }
.chat-call-btn.video:hover { background: #e8f0fd; }

/* 消息区：加宽加高，气泡最大 85% */
.msg-box {
  flex: 1; min-height: 340px; max-height: 58vh;
  overflow-y: auto; padding: 16px 10px;
  background: var(--panel); border-radius: 16px;
  margin: 14px 0 12px;
}
.chat-empty { text-align: center; padding: 48px 0; color: var(--muted); }
.chat-empty span { font-size: 44px; display: block; margin-bottom: 12px; }
.chat-empty p { font-size: 13px; margin: 0; }

/* 时间分隔线 */
.time-sep { text-align: center; margin: 12px 0 14px; }
.time-sep span {
  font-size: 10.5px; color: var(--muted);
  background: var(--card); border-radius: 999px; padding: 3px 12px;
}

.bubble-row { display: flex; align-items: flex-end; gap: 8px; margin-bottom: 12px; }
.bubble-row.mine { justify-content: flex-end; }
.bubble-face {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; font-weight: 700;
}
.bubble {
  position: relative; max-width: 85%; padding: 9px 13px 7px;
  border-radius: 16px 16px 16px 4px;
  background: var(--card);
  box-shadow: 0 2px 6px rgba(160,108,213,.08);
  animation: bubbleIn .2s ease;
}
@keyframes bubbleIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: none; }
}
.bubble.mine {
  border-radius: 16px 16px 4px 16px;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}
.bubble-text {
  font-size: 14px; line-height: 1.65;
  word-break: break-word; white-space: pre-wrap;
}
.bubble-text.recalled { font-size: 12px; opacity: .6; font-style: italic; }
.bubble-time {
  display: block; font-size: 9.5px; margin-top: 3px;
  text-align: right; opacity: .65;
}
.bubble-more {
  position: absolute; top: -10px; right: -6px;
  width: 20px; height: 20px; border: none; border-radius: 50%;
  background: var(--card); color: var(--muted); cursor: pointer;
  font-size: 12px; line-height: 1; box-shadow: 0 2px 6px rgba(0,0,0,.12);
  opacity: 0; transition: opacity .15s;
}
.bubble:hover .bubble-more { opacity: 1; }

/* 图片消息 */
.bubble-img-wrap { cursor: zoom-in; border-radius: 10px; overflow: hidden; margin: -2px 0; }
.bubble-img { display: block; max-width: 220px; max-height: 260px; object-fit: cover; }

/* 通话卡片消息 */
.bubble.call { padding: 11px 14px; min-width: 200px; }
.call-card { display: flex; align-items: center; gap: 10px; }
.call-icon { font-size: 22px; }
.call-info { flex: 1; min-width: 0; }
.call-info b { display: block; font-size: 13.5px; }
.call-info em { font-style: normal; font-size: 11px; opacity: .7; }
.call-again {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 5px 11px; font-size: 11px; font-weight: 700;
  color: var(--primary); background: var(--panel);
}
.call-again:hover { filter: brightness(.96); }

/* 正在输入提示 */
.typing-hint { display: flex; gap: 4px; padding: 4px 6px; }
.typing-hint .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--muted); opacity: .5;
  animation: typingBounce 1.2s ease-in-out infinite;
}
.typing-hint .dot:nth-child(2) { animation-delay: .15s; }
.typing-hint .dot:nth-child(3) { animation-delay: .3s; }
@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-5px); }
}

/* 输入栏 */
.chat-input-bar {
  display: flex; gap: 8px; align-items: center;
  padding-top: 2px;
}
.chat-tool {
  border: none; background: var(--panel); cursor: pointer;
  width: 38px; height: 38px; border-radius: 50%; flex-shrink: 0;
  font-size: 17px; transition: all .15s;
}
.chat-tool:hover, .chat-tool.on { background: var(--card); transform: scale(1.08); box-shadow: 0 2px 8px rgba(160,108,213,.15); }
.chat-input-bar input[type="text"] {
  flex: 1; min-width: 0; padding: 11px 14px;
  border: 1.5px solid var(--border); border-radius: 999px;
  font-size: 14px; color: var(--text); background: var(--card);
  transition: border-color .15s, box-shadow .15s;
}
.chat-input-bar input[type="text"]:focus {
  outline: none; border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(160,108,213,.15);
}
.chat-send {
  flex-shrink: 0; padding: 0 20px; height: 38px;
  border: none; border-radius: 999px; cursor: pointer;
  color: #fff; font-size: 14px; font-weight: 700;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160,108,213,.3);
  transition: filter .15s;
}
.chat-send:hover { filter: brightness(1.06); }
.chat-send:disabled { opacity: .55; cursor: default; }

/* 在线状态徽标 */
.online-badge {
  flex-shrink: 0; font-size: 11px; padding: 3px 9px; border-radius: 999px;
  background: var(--panel); color: var(--muted); font-weight: 700;
  transition: all .2s;
}
.online-badge.on { color: #2faf63; background: #e3f8ec; }
.online-badge.off { color: var(--muted); }

/* 录音提示条 */
.rec-bar {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; margin: 0 0 10px;
  background: var(--panel); border: 1.5px solid var(--border);
  border-radius: 14px;
  animation: sheetFade .16s ease;
}
.rec-dot {
  width: 12px; height: 12px; border-radius: 50%;
  background: #e8556b; flex-shrink: 0;
  animation: recBlink 1s ease-in-out infinite;
}
@keyframes recBlink { 0%, 100% { opacity: 1; } 50% { opacity: .3; } }
.rec-time { font-size: 14px; font-weight: 800; color: var(--text); font-variant-numeric: tabular-nums; }
.rec-tip { flex: 1; font-size: 12px; color: var(--muted); }
.rec-cancel, .rec-send {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 7px 14px; font-size: 12.5px; font-weight: 700;
}
.rec-cancel { background: var(--card); color: var(--muted); }
.rec-cancel:hover { color: var(--danger); }
.rec-send { color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple)); }
.rec-send:hover { filter: brightness(1.06); }

/* 语音消息气泡 */
.bubble-audio {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 2px; min-width: 180px;
}
.audio-ico { font-size: 18px; flex-shrink: 0; }
.bubble-audio audio {
  height: 34px; max-width: 220px; flex: 1; min-width: 0;
}
.bubble.mine .bubble-audio audio { filter: invert(.92) hue-rotate(180deg); }

/* 表情面板 */
.emoji-panel {
  display: grid; grid-template-columns: repeat(10, 1fr); gap: 2px;
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 14px; padding: 8px; margin-bottom: 8px;
  max-height: 132px; overflow-y: auto;
}
.emoji-btn {
  border: none; background: none; cursor: pointer;
  font-size: 18px; padding: 4px; border-radius: 8px;
  transition: background .12s, transform .12s;
}
.emoji-btn:hover { background: var(--panel); transform: scale(1.2); }

/* 消息操作菜单 */
.msg-menu {
  position: fixed; z-index: 120;
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 12px; padding: 4px;
  box-shadow: 0 10px 28px rgba(0,0,0,.16);
  display: flex; flex-direction: column; min-width: 104px;
}
.msg-menu button {
  border: none; background: none; cursor: pointer;
  padding: 8px 12px; font-size: 12.5px; text-align: left;
  color: var(--text); border-radius: 8px; transition: background .12s;
}
.msg-menu button:hover { background: var(--panel); }
.msg-menu button.danger { color: var(--danger); }

/* 图片全屏预览 */
.img-preview {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(20, 15, 35, .88);
  display: flex; align-items: center; justify-content: center;
  animation: sheetFade .18s ease;
}
@keyframes sheetFade { from { opacity: 0; } to { opacity: 1; } }
.img-preview img {
  max-width: 92vw; max-height: 88vh; border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,.5);
}
.img-preview-close {
  position: absolute; top: 20px; right: 24px;
  width: 36px; height: 36px; border: none; border-radius: 50%;
  background: rgba(255,255,255,.18); color: #fff;
  font-size: 16px; cursor: pointer;
}
.img-preview-close:hover { background: rgba(255,255,255,.32); }

/* 通话全屏界面 */
.call-mask {
  position: fixed; inset: 0; z-index: 210;
  background: linear-gradient(160deg, #3d2f5c 0%, #2b2440 60%, #1e1830 100%);
  display: flex; align-items: center; justify-content: center;
  animation: sheetFade .2s ease;
}
.call-body { text-align: center; color: #fff; padding: 24px; max-width: 320px; }
.call-avatar {
  width: 108px; height: 108px; margin: 0 auto 22px;
  border-radius: 50%; font-size: 42px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 12px 36px rgba(0,0,0,.4);
}
.call-avatar.ring { animation: callRing 1.6s ease-in-out infinite; }
@keyframes callRing {
  0%, 100% { box-shadow: 0 12px 36px rgba(0,0,0,.4), 0 0 0 0 rgba(255,255,255,.32); }
  50% { box-shadow: 0 12px 36px rgba(0,0,0,.4), 0 0 0 22px rgba(255,255,255,0); }
}
.call-body h3 { margin: 0 0 6px; font-size: 22px; }
.call-status { margin: 0; font-size: 14px; opacity: .82; }
.call-timer { margin: 6px 0 0; font-size: 30px; font-weight: 800; font-variant-numeric: tabular-nums; }
.call-actions {
  display: flex; align-items: center; justify-content: center; gap: 18px;
  margin: 36px 0 18px;
}
.call-ctl {
  width: 52px; height: 52px; border: none; border-radius: 50%;
  background: rgba(255,255,255,.14); color: #fff; font-size: 21px;
  cursor: pointer; transition: all .15s;
}
.call-ctl:hover { background: rgba(255,255,255,.26); }
.call-ctl.on { background: #fff; color: #3d2f5c; }
.call-hangup {
  border: none; cursor: pointer; border-radius: 999px;
  padding: 13px 26px; font-size: 15px; font-weight: 800;
  color: #fff; background: linear-gradient(135deg, #e06666, #c94f4f);
  box-shadow: 0 8px 24px rgba(224, 102, 102, .45);
  transition: filter .15s, transform .1s;
}
.call-hangup:hover { filter: brightness(1.08); }
.call-hangup:active { transform: scale(.96); }
.call-tip { font-size: 11px; opacity: .55; margin: 0; }

/* 手机端微调 */
@media (max-width: 767px) {
  .chat-page { padding-left: 0; padding-right: 0; }
  .msg-box { max-height: none; }
  .emoji-panel { grid-template-columns: repeat(8, 1fr); }
  .bubble-img { max-width: 180px; }
}
</style>
