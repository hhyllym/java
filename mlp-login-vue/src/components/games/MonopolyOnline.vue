<template>
  <div class="mo-wrap">
    <!-- 🎈 氛围层：漂浮气球 + 彩虹粒子（纯装饰，不挡点击） -->
    <div class="mo-sky" aria-hidden="true">
      <span v-for="(b, i) in balloons" :key="'b' + i" class="mo-balloon" :style="b.style">{{ b.icon }}</span>
      <i v-for="(p, i) in sparkles" :key="'s' + i" class="mo-spark" :style="p.style"></i>
    </div>
    <!-- ============ 未入房：创建 / 加入 ============ -->
    <template v-if="!joined">
      <div class="mo-card mo-setup">
        <h3>🌐 四人联机 · 糖霜镇大富翁</h3>
        <p class="mo-sub">四个同学各用各的电脑 / 手机，连同一个 WiFi，打开同一个网址、进同一个房间就能组队开玩，不用轮流用一台机器。</p>

        <div class="mo-field">
          <label>你的昵称</label>
          <input v-model.trim="nickname" maxlength="12" placeholder="显示在棋盘上的名字" />
        </div>

        <div class="mo-lobby">
          <div class="mo-lobby-head">
            <b>🎮 匹配大厅 · 正在等待的玩家</b>
            <button class="mo-mini" @click="loadRooms">🔄 刷新</button>
          </div>
          <p v-if="waitingRooms.length === 0" class="mo-lobby-empty">
            暂时没有等待中的房间——点下面的「创建房间」当第一个房主吧！
          </p>
          <div v-else class="mo-room-list">
            <div v-for="r in waitingRooms" :key="r.roomId" class="mo-room-item">
              <span class="mo-room-code">{{ r.roomId }}</span>
              <span class="mo-room-host">👑 {{ r.host }}</span>
              <span class="mo-room-meta">{{ r.humans }}/4 人 · {{ r.mode === 'team' ? '2v2 组队' : '四人混战' }}</span>
              <button class="mo-mini go" :disabled="loading" @click="joinDirect(r)">一键加入</button>
            </div>
          </div>

          <div v-if="onlineUsers.length" class="mo-online">
            <b>👥 在线的小伙伴 · {{ onlineUsers.length }}</b>
            <span v-for="u in onlineUsers" :key="u.userId" class="mo-online-chip">
              {{ u.avatar || '🐴' }} {{ u.name }}{{ u.userId === userId ? '（我）' : '' }}
            </span>
          </div>
        </div>

        <button class="mo-btn big" :disabled="loading" @click="createRoom">🎉 创建房间（当房主）</button>

        <div class="mo-or">—— 或输入 4 位房间号加入 ——</div>
        <div class="mo-join">
          <input
            v-model.trim="joinCode" maxlength="4" placeholder="4 位房间号"
            @input="joinCode = joinCode.toUpperCase()" @keyup.enter="joinRoom"
          />
          <button class="mo-btn" :disabled="loading" @click="joinRoom">加入房间</button>
        </div>

        <p v-if="err" class="mo-err">{{ err }}</p>
        <p class="mo-tip">💡 每人用自己的账号登录（没有账号先注册一个）。只要连同一个 WiFi 就行，不需要公网；大厅里的房间所有人都能看到，点「一键加入」直接上桌。</p>
      </div>
    </template>

    <!-- ============ 已入房 ============ -->
    <template v-else-if="st">
      <div class="mo-hud">
        <span>房间 <b>{{ roomId }}</b></span>
        <span>{{ st.mode === 'team' ? '🤝 2v2 组队' : '🏆 四人混战' }}</span>
        <span>我的成绩 <b>{{ st.myScore }}</b> 分</span>
      </div>

      <!-- ---- 等人 / 房主开局 ---- -->
      <div v-if="st.status === 'waiting'" class="mo-card mo-wait">
        <h3>🏠 {{ st.humanCount }} / 4 人就位</h3>
        <p class="mo-sub">把房间号发给同学，各自输入后点「加入房间」；四人到齐由房主开始。</p>
        <div class="mo-stake" :class="{ low: balance < minStake }">
          <span>🎫 上桌本金</span>
          <b>每人需 {{ minStake }} 可用积分</b>
          <em>我的余额 💎 {{ balance }}</em>
          <router-link v-if="balance < minStake" to="/earn">积分不够？去赚积分 →</router-link>
        </div>
        <p class="mo-sub">8 轮后按名次结算（4 人局：第 1 名 +45、第 2 名 +15、第 3 名 -15、第 4 名 -45），
          赢的积分<b>直接进账号余额</b>，马上能在商店花；积分不够就上不了桌。</p>
        <div class="mo-code">
          <b>{{ roomId }}</b>
          <button class="mo-mini" @click="copy(inviteUrl)">{{ copied ? '已复制 ✓' : '复制邀请链接' }}</button>
        </div>

        <div class="mo-seats">
          <div
            v-for="p in st.players" :key="p.id"
            class="mo-seat" :class="{ empty: p.empty, me: p.isMe }"
            :style="{ '--seat': p.color }"
          >
            <span class="seat-ico">{{ p.icon }}</span>
            <b>{{ p.empty ? '等待加入' : p.name }}</b>
            <span class="seat-tag">
              {{ p.empty ? '空位' : p.ai ? '🤖 电脑' : p.online ? '✅ 在线' : '⏳ 掉线' }}
              <template v-if="p.isMe"> · 我</template>
            </span>
            <span v-if="st.mode === 'team'" class="seat-team">{{ teamName(p.team) }}</span>
          </div>
        </div>

        <template v-if="isHost">
          <div class="mo-mode">
            <button :class="{ on: roomMode === 'team' }" @click="roomMode = 'team'">🤝 2v2 组队</button>
            <button :class="{ on: roomMode === 'solo' }" @click="roomMode = 'solo'">🏆 四人混战</button>
          </div>
          <p class="mo-tip center">
            {{ roomMode === 'team'
              ? '星光队（星澜 + 泡芙）对 彩虹队（云驰 + 金穗）；队友免租，按队伍总分决胜。'
              : '四人各自为战，踩到别人的店都要付租，按个人总分排名。' }}
          </p>
        </template>

        <div class="mo-actions">
          <button v-if="isHost" class="mo-btn big" :disabled="loading" @click="startGame(false)">🎲 人齐了，开始！</button>
          <button v-if="isHost" class="mo-btn ghost" :disabled="loading" @click="startGame(true)">🤖 人没齐？电脑补位</button>
          <button class="mo-btn ghost" @click="quit">退出房间</button>
        </div>
        <p v-if="!isHost" class="mo-tip center">等房主点「开始」～ {{ st.log }}</p>
        <p v-if="err" class="mo-err">{{ err }}</p>
        <div class="mo-poll">🔄 自动同步中 · {{ online ? '已连接' : '连接中 / 断开' }}</div>
      </div>

      <!-- ---- 对局中 ---- -->
      <template v-else-if="st.status === 'playing' || st.status === 'buy'">
        <div class="mo-hud sub">
          <span>第 <b>{{ st.round }}</b> / {{ st.maxRound }} 轮</span>
          <span>🎲 {{ st.dice || '—' }}</span>
          <span>{{ myTurn ? '👉 轮到你' : `等 ${nameOf(st.turn)} 行动` }}</span>
        </div>

        <div class="mo-players">
          <div
            v-for="p in st.players" :key="p.id"
            class="mo-player" :class="{ turn: p.id === st.turn, ai: p.ai }"
            :style="{ '--seat': p.color }"
          >
            <span class="pl-ico">{{ p.icon }}</span>
            <div class="pl-info">
              <b>{{ p.name }}<small v-if="p.isMe">（我）</small></b>
              <span>{{ p.score }} 分 · 💎 {{ p.coins }} · 🏠 {{ p.shops.length }}</span>
            </div>
            <span v-if="p.ai" class="pl-badge">🤖</span>
            <span v-else-if="!p.online" class="pl-badge warn">掉线</span>
            <span v-else-if="p.id === st.turn" class="pl-badge">行动中</span>
          </div>
        </div>

        <div class="mo-board">
          <div
            v-for="t in st.tiles" :key="t.i"
            class="mo-tile" :class="[t.type, { here: pawnsAt(t.i).length }]"
            :style="[tileStyle(t.i), t.owner >= 0 ? { borderColor: st.players[t.owner].color } : {}]"
            :title="tileTitle(t)"
          >
            <span class="mo-ico">{{ t.icon }}</span>
            <span class="mo-name">{{ t.name }}</span>
            <span v-if="t.type === 'shop'" class="mo-price">{{ t.price }}币</span>
            <span v-if="t.owner >= 0" class="mo-owner">{{ st.players[t.owner].icon }}</span>
            <span
              v-for="p in pawnsAt(t.i)" :key="p.id"
              class="mo-pawn" :style="pawnStyle(p.id)"
            >{{ p.icon }}</span>
          </div>

          <div class="mo-center" :class="{ dimmed: diceShow }">
            <div class="mo-turn-label">{{ st.status === 'buy' ? '🏠 买店决策' : `${st.players[st.turn].icon} ${nameOf(st.turn)} 的回合` }}</div>
            <Dice3D mini :rolling="acting" :value="Number(st.dice) || 0" />
            <p class="mo-log">{{ st.log }}</p>

            <div class="mo-actions">
              <template v-if="st.status === 'buy'">
                <template v-if="st.buySeat === mySeat && st.buyTile">
                  <span class="mo-buy-who">🏠 花 {{ st.buyTile.price }} 币买下「{{ st.buyTile.name }}」吗？</span>
                  <button class="mo-btn" :disabled="acting" @click="doBuy(true)">买下</button>
                  <button class="mo-btn ghost" :disabled="acting" @click="doBuy(false)">跳过</button>
                </template>
                <span v-else class="mo-buy-who">等 {{ nameOf(st.buySeat) }} 决定要不要买店…</span>
              </template>
              <template v-else>
                <button class="mo-btn" :disabled="acting || !myTurn" @click="doRoll">
                  {{ myTurn ? '🎲 掷骰子' : `等 ${nameOf(st.turn)} 掷骰…` }}
                </button>
              </template>
            </div>
            <div class="mo-poll small">🔄 {{ online ? '实时同步中' : '连接中 / 断开' }} · 掉线 20 秒后系统自动代走</div>
          </div>
          <!-- 掷骰时浮在棋盘上方的 3D 骰子（自己或别人掷骰都会翻滚） -->
          <Dice3D :show="diceShow" :rolling="diceRolling" :value="diceValue" />
        </div>

        <div class="mo-legend">
          <span>🏰 经过起点 +20 分 / +10 币</span>
          <span>🏠 买店收租 · 组队时队友免租</span>
          <span>🎁 机会卡 · ✨ 魔法事件 · 💸 税务所</span>
        </div>
        <p v-if="err" class="mo-err">{{ err }}</p>
        <p class="mo-tip">💡 每人各走 {{ st.maxRound }} 轮。轮到自己时按钮才会亮；四个人看到的是同一张棋盘。</p>
      </template>

      <!-- ---- 终局 ---- -->
      <div v-else class="mo-card mo-result">
        <h3>🏁 巡游结束！</h3>
        <p v-if="st.mode === 'team'" class="mo-banner">
          {{ st.winnerTeam === 0 ? '🤝 平局，友谊第一！' : (st.winnerTeam === 1 ? '🌟 星光队获胜！' : '🌈 彩虹队获胜！') }}
          星光队 {{ st.teamScore1 }} 分 · 彩虹队 {{ st.teamScore2 }} 分
        </p>
        <p v-else class="mo-banner">🏆 {{ myRank === 1 ? '你拿下第一名！' : `你这局第 ${myRank} 名` }}</p>

        <div class="mo-rank">
          <div v-for="(r, i) in st.ranking" :key="r.id" class="mo-rank-row" :class="{ me: r.id === mySeat }">
            <span class="rank-no">{{ i + 1 }}</span>
            <span class="rank-ico">{{ r.icon }}</span>
            <b>{{ r.name }}<small v-if="r.id === mySeat">（我）</small></b>
            <span v-if="st.mode === 'team'" class="rank-team">{{ teamName(r.team) }}</span>
            <em>{{ r.score }} 分</em>
            <small>💎 {{ r.coins }} · 🏠 {{ r.shops }} · 积分 {{ (r.pointDelta || 0) > 0 ? '+' : '' }}{{ r.pointDelta || 0 }}</small>
          </div>
        </div>
        <p class="mo-tip center">本局计入你账号的成绩：<b>{{ st.myScore }} 分</b>{{ st.mode === 'team' ? '（你所在队伍的总分）' : '' }}</p>
        <p class="mo-point" :class="myPointDelta > 0 ? 'up' : (myPointDelta < 0 ? 'down' : '')">
          💎 本局积分结算 <b>{{ myPointDelta > 0 ? '+' : '' }}{{ myPointDelta }}</b>
          · 已直接进账号余额（现在 {{ balance }} 积分）
        </p>
        <div class="mo-actions">
          <button class="mo-btn big" @click="claim">🎉 领取成绩并结算</button>
          <button v-if="isHost" class="mo-btn ghost" :disabled="acting" @click="again">🔁 再来一局</button>
          <button class="mo-btn ghost" @click="quit">退出房间</button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { getSession } from '../../auth'
import { bump } from '../../quest'
import { apiPointBalance } from '../../point'
import { mpLan, mpCreate, mpJoin, mpStart, mpState, mpAction, mpLeave, mpRooms, mpHeartbeat, mpOnline } from '../../monopolyRoom'
import sound from '../../sound'
import Dice3D from '../Dice3D.vue'

/** 顺时针把第 i 格映射到 7×7 网格的 (行, 列) */
function cellOf(i) {
  if (i <= 6) return { r: 1, c: i + 1 }
  if (i <= 12) return { r: i - 5, c: 7 }
  if (i <= 18) return { r: 7, c: 19 - i }
  return { r: 25 - i, c: 1 }
}

const PAWN_POS = [
  { left: '1px', top: '0' },
  { right: '1px', top: '0' },
  { left: '1px', bottom: '0' },
  { right: '1px', bottom: '0' }
]

/** 房间凭据：好友页「邀请组队」建房后跳过来、或刷新页面，都能自动回到原座位 */
/** 氛围层装饰：气球 + 彩虹粒子的确定性伪随机布局（避免每次渲染跳变） */
function makeSky() {
  const bIcons = ['🎈', '🎈', '🎈', '🎀', '🎈', '🌈', '🎈']
  const balloons = bIcons.map((icon, i) => ({
    icon,
    style: {
      left: (4 + i * 13.5 + (i % 3) * 3) + '%',
      fontSize: (24 + ((i * 7) % 20)) + 'px',
      animationDuration: (15 + ((i * 5) % 11)) + 's',
      animationDelay: (-i * 3.4) + 's'
    }
  }))
  const colors = ['#ff8fa8', '#ffb86b', '#ffe066', '#8fd694', '#7cc4ff', '#b79cf0']
  const sparkles = Array.from({ length: 30 }, (_, i) => ({
    style: {
      left: (2 + ((i * 37) % 95)) + '%',
      top: (4 + ((i * 53) % 88)) + '%',
      width: (5 + (i % 3) * 3) + 'px',
      height: (5 + (i % 3) * 3) + 'px',
      background: colors[i % colors.length],
      animationDuration: (3 + (i % 5)) + 's',
      animationDelay: (-(i % 7) * 0.7) + 's'
    }
  }))
  return { balloons, sparkles }
}

const ROOM_CRED_KEY = 'mlp_mp_room'
function saveRoomCred(roomId, seatId, token) {
  try {
    sessionStorage.setItem(ROOM_CRED_KEY, JSON.stringify({ roomId, seatId, token, ts: Date.now() }))
  } catch (e) { /* 隐私模式下忽略 */ }
}
function readRoomCred() {
  try {
    const c = JSON.parse(sessionStorage.getItem(ROOM_CRED_KEY) || 'null')
    if (!c || !c.roomId || !c.token || c.seatId === undefined) return null
    if (c.ts && Date.now() - c.ts > 2 * 60 * 60 * 1000) { clearRoomCred(); return null }
    return c
  } catch (e) { return null }
}
function clearRoomCred() {
  try { sessionStorage.removeItem(ROOM_CRED_KEY) } catch (e) { /* ignore */ }
}

export default {
  name: 'MonopolyOnline',
  components: { Dice3D },
  emits: ['score', 'over'],
  data() {
    const s = getSession() || {}
    const sky = makeSky()
    return {
      balloons: sky.balloons,
      sparkles: sky.sparkles,
      nickname: s.name || '',
      userId: s.id || null,
      joinCode: '',
      roomId: '',
      seatId: -1,
      token: '',
      joined: false,
      st: null,
      roomMode: 'team',
      lanIp: '',
      lanIps: [],
      waitingRooms: [],
      onlineUsers: [],
      roomsTimer: null,
      err: '',
      loading: false,
      acting: false,
      // 掷骰骰子：浮在棋盘上方，翻滚 → 定格 → 数秒后淡出
      diceShow: false,
      diceRolling: false,
      diceValue: 0,
      diceTimer: null,
      lastDice: 0,
      online: false,
      copied: false,
      lastScore: 0,
      balance: 0,
      minStake: 45,
      polling: false,
      pollTimer: null,
      copyTimer: null,
      dead: false,
      isReloading: false,
      unloadHandler: null
    }
  },
  computed: {
    isHost() { return this.seatId === 0 },
    mySeat() { return this.seatId },
    myTurn() {
      if (!this.st) return false
      const me = this.st.players[this.mySeat]
      return this.st.status === 'playing' && this.st.turn === this.mySeat && me && !me.ai
    },
    myRank() {
      if (!this.st || !this.st.ranking) return 0
      return this.st.ranking.findIndex(r => r.id === this.mySeat) + 1
    },
    myPointDelta() {
      return this.st ? (Number(this.st.myPointDelta) || 0) : 0
    },
    inviteBase() {
      return this.lanIp ? `http://${this.lanIp}:5180` : window.location.origin
    },
    inviteUrl() {
      return this.inviteBase + '/game/monopoly?room=' + this.roomId
    },
    lanTip() {
      if (this.lanIp) {
        return this.lanIps.length > 1
          ? `检测到多个网卡，备选：${this.lanIps.join(' / ')}；连不上就换一个试试`
          : '同学用这个网址打开（同一 WiFi）；若连不上，检查 Windows 防火墙是否放行 5180 端口'
      }
      return '正在获取主机局域网 IP… 也可以自己在本机运行 ipconfig 查 WiFi 的 IPv4 地址'
    }
  },
  mounted() {
    const q = this.$route && this.$route.query ? this.$route.query.room : null
    const urlRoom = q ? String(q).toUpperCase().slice(0, 4) : ''
    if (urlRoom) this.joinCode = urlRoom
    this.loadLan()
    this.loadBalance()
    this.loadRooms()
    this.roomsTimer = setInterval(this.loadRooms, 4000)
    this.pollTimer = setInterval(this.poll, 1200)
    // 区分「刷新页面」与「路由离开」：刷新时保留座位与凭据，回来自动恢复
    this.unloadHandler = () => { this.isReloading = true }
    window.addEventListener('beforeunload', this.unloadHandler)
    this.restoreRoom(urlRoom)
  },
  beforeUnmount() {
    this.dead = true
    if (this.pollTimer) clearInterval(this.pollTimer)
    if (this.roomsTimer) clearInterval(this.roomsTimer)
    if (this.copyTimer) clearTimeout(this.copyTimer)
    if (this.diceTimer) clearTimeout(this.diceTimer)
    if (this.unloadHandler) window.removeEventListener('beforeunload', this.unloadHandler)
    if (!this.isReloading && this.joined && this.roomId) {
      mpLeave(this.roomId, this.seatId, this.token).catch(() => {})
      clearRoomCred()
    }
  },
  methods: {
    teamName(t) { return t === 1 ? '🌟 星光队' : '🌈 彩虹队' },
    nameOf(i) {
      if (!this.st || !this.st.players[i]) return '小马'
      const p = this.st.players[i]
      return p.empty ? '空位' : p.name
    },
    tileStyle(i) {
      const c = cellOf(i)
      return { gridRow: c.r, gridColumn: c.c }
    },
    pawnStyle(id) { return PAWN_POS[id] },
    pawnsAt(i) {
      return this.st ? this.st.players.filter(p => !p.empty && p.pos === i) : []
    },
    tileTitle(t) {
      const base = t.icon + ' ' + t.name
      if (t.type === 'shop') {
        const o = t.owner >= 0 && this.st.players[t.owner] ? ' · 属于 ' + this.st.players[t.owner].name : ' · 无人经营'
        return `${base} · 售价 ${t.price} 币 / 租金 ${t.rent} 分${o}`
      }
      if (t.type === 'bonus') return `${base} · +${t.value} 分`
      return base
    },
    copy(text) {
      sound.click()
      const done = () => {
        this.copied = true
        if (this.copyTimer) clearTimeout(this.copyTimer)
        this.copyTimer = setTimeout(() => { this.copied = false }, 1800)
      }
      // 局域网是 http（非安全上下文），navigator.clipboard 常常不可用，用老接口兜底
      const legacy = () => {
        try {
          const ta = document.createElement('textarea')
          ta.value = text
          ta.style.position = 'fixed'
          ta.style.opacity = '0'
          document.body.appendChild(ta)
          ta.select()
          const ok = document.execCommand('copy')
          document.body.removeChild(ta)
          if (ok) done()
          else this.err = '复制失败，请手动选中网址复制'
        } catch (e) {
          this.err = '复制失败，请手动选中网址复制'
        }
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done).catch(legacy)
      } else {
        legacy()
      }
    },
    async loadBalance() {
      if (!this.userId) return
      const r = await apiPointBalance(this.userId)
      if (r && r.code === 200) this.balance = Number(r.data) || 0
    },
    async loadLan() {
      const r = await mpLan()
      if (r && r.code === 200 && r.data) {
        this.lanIp = r.data.ip || ''
        this.lanIps = r.data.ips || []
      }
    },
    applyState(state) {
      if (!state || this.dead) return
      this.st = state
      this.online = true
      this.err = ''
      if (state.myScore !== this.lastScore) {
        this.lastScore = state.myScore
        this.$emit('score', state.myScore)
      }
      // 骰子点数变了 = 有人刚掷过骰，浮出 3D 骰子翻滚并定格到真实点数
      const d = Number(state.dice) || 0
      if (d && d !== this.lastDice) {
        this.lastDice = d
        this.showDice(d)
      }
    },
    /** 有人掷骰：先翻滚一会儿，再定格到真实点数，停留数秒后淡出 */
    showDice(d) {
      if (this.diceTimer) clearTimeout(this.diceTimer)
      this.diceShow = true
      this.diceRolling = true
      this.diceValue = 0
      this.diceTimer = setTimeout(() => {
        if (this.dead) return
        this.diceValue = d
        this.diceRolling = false
        this.diceTimer = setTimeout(() => { this.diceShow = false }, 1800)
      }, 420)
    },
    handleFail(r, fallback) {
      this.err = (r && r.msg) || fallback
      if (r && r.code === -1) this.online = false
    },

    /* ---------- 房间 ---------- */
    /** 匹配大厅：拉取等待中的房间 + 发心跳上报在线 + 拉在线名单（未入房时每 4 秒自动刷新） */
    async loadRooms() {
      if (this.joined || this.dead) return
      // 心跳上报：让大厅里能看到「我在线」
      if (this.userId) {
        const s = getSession() || {}
        mpHeartbeat(this.userId, s.name || this.nickname, s.avatar || '').catch(() => {})
      }
      const [r, o] = await Promise.all([mpRooms(), mpOnline()])
      if (r && r.code === 200 && Array.isArray(r.data)) this.waitingRooms = r.data
      if (o && o.code === 200 && Array.isArray(o.data)) this.onlineUsers = o.data
    },
    /** 大厅一键加入：直接带上房间号走 joinRoom */
    async joinDirect(room) {
      if (this.loading || !room) return
      this.joinCode = room.roomId
      await this.joinRoom()
    },
    async createRoom() {
      if (this.loading) return
      this.loading = true
      this.err = ''
      sound.click()
      const r = await mpCreate(this.nickname || '小马', this.userId, this.roomMode)
      this.loading = false
      if (r.code !== 200) { this.handleFail(r, '创建房间失败'); return }
      this.enterRoom(r.data)
    },
    async joinRoom() {
      if (this.loading) return
      if (!this.joinCode || this.joinCode.length !== 4) { this.err = '请输入 4 位房间号'; return }
      this.loading = true
      this.err = ''
      sound.click()
      const r = await mpJoin(this.joinCode, this.nickname || '小马', this.userId)
      this.loading = false
      if (r.code !== 200) { this.handleFail(r, '加入房间失败'); return }
      this.enterRoom(r.data)
    },
    enterRoom(data) {
      if (this.roomsTimer) { clearInterval(this.roomsTimer); this.roomsTimer = null }
      this.roomId = data.roomId
      this.seatId = data.seatId
      this.token = data.token
      this.joined = true
      this.online = true
      this.lastScore = 0
      this.applyState(data.state)
      if (data.state && data.state.minStake) this.minStake = data.state.minStake
      this.roomMode = data.state ? data.state.mode : 'team'
      this.loadBalance()
      saveRoomCred(data.roomId, data.seatId, data.token)
      this.$emit('score', 0)
    },
    /** 从好友页建房跳转 / 刷新页面后，用凭据找回原座位 */
    async restoreRoom(urlRoom) {
      const c = readRoomCred()
      if (!c) return
      // 打开了别人的邀请链接：不抢回旧房间，让用户去点「加入房间」
      if (urlRoom && String(c.roomId).toUpperCase() !== urlRoom) return
      const r = await mpState(c.roomId, c.seatId, c.token)
      if (this.dead || this.joined || r.code !== 200 || !r.data) {
        if (!this.joined) clearRoomCred()
        return
      }
      this.roomId = c.roomId
      this.seatId = c.seatId
      this.token = c.token
      this.joined = true
      this.online = true
      this.lastScore = 0
      this.applyState(r.data)
      if (r.data.minStake) this.minStake = r.data.minStake
      this.roomMode = r.data.mode || 'team'
      this.loadBalance()
      this.$emit('score', r.data.myScore || 0)
    },
    async startGame(fillAi) {
      if (this.loading) return
      this.loading = true
      this.err = ''
      sound.click()
      const r = await mpStart(this.roomId, this.seatId, this.token, this.roomMode, fillAi)
      this.loading = false
      if (r.code !== 200) { this.handleFail(r, '开始失败'); return }
      this.applyState(r.data)
      sound.magic()
    },
    quit() {
      sound.click()
      if (this.roomId) mpLeave(this.roomId, this.seatId, this.token).catch(() => {})
      clearRoomCred()
      this.joined = false
      this.st = null
      this.roomId = ''
      this.seatId = -1
      this.token = ''
      this.err = ''
      this.lastScore = 0
      this.$emit('score', 0)
    },

    /* ---------- 动作 ---------- */
    async act(type, value) {
      if (this.acting) return
      this.acting = true
      this.err = ''
      if (type === 'roll') sound.click()
      const r = await mpAction(this.roomId, this.seatId, this.token, type, value)
      this.acting = false
      if (r.code !== 200) { this.handleFail(r, '操作失败，稍等一下再试'); return }
      this.applyState(r.data)
    },
    doRoll() { this.act('roll', false) },
    doBuy(yes) { this.act('buy', yes) },
    async again() {
      if (this.acting) return
      this.acting = true
      const r = await mpAction(this.roomId, this.seatId, this.token, 'again', false)
      this.acting = false
      if (r.code !== 200) { this.handleFail(r, '开下一局失败'); return }
      this.applyState(r.data)
    },

    /* ---------- 轮询 ---------- */
    async poll() {
      if (!this.joined || this.polling || this.dead || !this.roomId) return
      this.polling = true
      const r = await mpState(this.roomId, this.seatId, this.token)
      this.polling = false
      if (this.dead || !this.joined) return
      if (r.code !== 200) {
        this.online = false
        // 房间不存在 / 身份失效时不直接踢人，给出明确提示 + 退出按钮
        if (r.code !== -1) this.err = r.msg + '（可点「退出房间」重新加入）'
        else this.err = r.msg
        return
      }
      this.applyState(r.data)
    },

    claim() {
      sound.click()
      bump('room')
      const total = this.st ? this.st.myScore : 0
      this.$emit('score', total)
      this.$emit('over', total)
      this.loadBalance()
    }
  }
}
</script>

<style scoped>
.mo-wrap { display: flex; flex-direction: column; gap: 11px; position: relative; }
/* 氛围层：气球 + 彩虹粒子，铺满整个页面，纯装饰不挡操作 */
.mo-sky { position: absolute; inset: 0; overflow: hidden; pointer-events: none; z-index: 0; border-radius: 16px; }
.mo-wrap > :not(.mo-sky) { position: relative; z-index: 1; }
.mo-balloon {
  position: absolute; bottom: -70px; opacity: 0; line-height: 1;
  animation: moRise linear infinite; will-change: transform, opacity;
  filter: drop-shadow(0 5px 8px rgba(160, 108, 213, .2));
}
@keyframes moRise {
  0%   { transform: translate(0, 0) rotate(-6deg); opacity: 0; }
  8%   { opacity: .95; }
  50%  { transform: translate(16px, -58vh) rotate(7deg); }
  90%  { opacity: .9; }
  100% { transform: translate(-4px, -112vh) rotate(-6deg); opacity: 0; }
}
.mo-spark { position: absolute; border-radius: 50%; animation: moTwinkle ease-in-out infinite; }
@keyframes moTwinkle {
  0%, 100% { opacity: .18; transform: scale(.65) translateY(0); }
  50%      { opacity: .9;  transform: scale(1.2) translateY(-9px); }
}
/* 减少动态效果：气球停用（纯装饰），彩虹粒子保留但静止可见 —— 不完全归零 */
@media (prefers-reduced-motion: reduce) {
  .mo-balloon { animation: none; opacity: 0; }
  .mo-spark { animation: none; opacity: .45; }
}
.mo-card {
  padding: 16px; border-radius: 16px; background: var(--panel);
  border: 1.5px solid var(--border);
  display: flex; flex-direction: column; gap: 11px; text-align: center;
}
.mo-card h3 { margin: 0; font-size: 17px; font-weight: 800; color: var(--primary); }
.mo-stake {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 9px 12px; border-radius: 12px; font-size: 11.5px;
  background: linear-gradient(135deg, #fff6d8, #e9f6ff); border: 1.5px solid var(--border);
}
.mo-stake.low { background: #fff0f0; border-color: #f3c0c0; }
.mo-stake span { font-weight: 800; color: var(--primary); }
.mo-stake b { color: var(--text); }
.mo-stake em { font-style: normal; color: var(--muted); }
.mo-stake a { color: var(--danger); font-weight: 800; text-decoration: underline; }
.mo-point { text-align: center; font-size: 12.5px; margin: 8px 0; color: var(--muted); }
.mo-point b { font-size: 16px; }
.mo-point.up b { color: var(--success); }
.mo-point.down b { color: var(--danger); }
.mo-sub { margin: 0; font-size: 11.5px; color: var(--muted); line-height: 1.65; }
.mo-tip { margin: 0; font-size: 11px; color: var(--muted); line-height: 1.65; text-align: left; }
.mo-tip.center { text-align: center; }
.mo-tip b { color: var(--primary); }
.mo-err { margin: 0; font-size: 11.5px; color: #c0392b; font-weight: 700; }

.mo-field { display: flex; align-items: center; gap: 8px; text-align: left; }
.mo-field label { font-size: 11.5px; color: var(--muted); white-space: nowrap; }
.mo-field input, .mo-join input {
  flex: 1; min-width: 0; padding: 9px 11px; font-size: 12.5px;
  border-radius: 10px; border: 1.5px solid var(--border);
  background: var(--card); color: var(--text); outline: none;
}
.mo-field input:focus, .mo-join input:focus { border-color: var(--primary); }
.mo-join { display: flex; gap: 8px; }
.mo-join input { text-transform: uppercase; letter-spacing: 2px; font-weight: 800; }
.mo-or { font-size: 10.5px; color: var(--muted); }

/* 匹配大厅：等待中的房间列表 */
.mo-lobby {
  padding: 10px; border-radius: 12px; background: var(--card);
  border: 1.5px solid var(--border); text-align: left;
}
.mo-lobby-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.mo-lobby-head b { font-size: 11px; color: var(--text); white-space: nowrap; }
.mo-lobby-empty { font-size: 10.5px; color: var(--muted); margin: 8px 0 2px; line-height: 1.5; }
.mo-room-list { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.mo-room-item {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 9px; border-radius: 10px;
  background: var(--panel); border: 1px solid var(--border);
}
.mo-room-code { font-weight: 800; letter-spacing: 1.5px; color: var(--primary); font-size: 12px; }
.mo-room-host { font-size: 11px; color: var(--text); font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mo-room-meta { font-size: 10px; color: var(--muted); margin-left: auto; white-space: nowrap; }
.mo-mini.go { background: linear-gradient(135deg, var(--pink), var(--purple)); color: #fff; border: none; }
.mo-mini.go:disabled { opacity: .5; cursor: not-allowed; }
/* 在线小伙伴名单 */
.mo-online {
  margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--border);
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px;
}
.mo-online b { font-size: 11px; color: var(--text); white-space: nowrap; }
.mo-online-chip {
  display: inline-flex; align-items: center; gap: 4px;
  background: var(--panel); border: 1px solid var(--border);
  border-radius: 999px; padding: 4px 10px;
  font-size: 11px; font-weight: 700; color: var(--text);
}
.mo-mini {
  cursor: pointer; border: 1px solid var(--border); background: var(--panel);
  color: var(--primary); font-size: 10.5px; font-weight: 700;
  padding: 5px 10px; border-radius: 999px;
}
.mo-mini:hover { filter: brightness(.96); }

.mo-btn {
  border: none; cursor: pointer; font-weight: 800;
  padding: 9px 16px; border-radius: 999px; font-size: 12.5px;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 5px 12px rgba(160, 108, 213, .28);
  transition: transform .12s, filter .12s;
}
.mo-btn.big { padding: 11px 22px; font-size: 13.5px; }
.mo-btn:hover:not(:disabled) { filter: brightness(1.07); transform: translateY(-1px); }
.mo-btn:active { transform: scale(.96); }
.mo-btn:disabled { opacity: .55; cursor: not-allowed; transform: none; }
.mo-btn.ghost {
  color: var(--primary); background: var(--card);
  border: 1.5px solid var(--border); box-shadow: none;
}
.mo-actions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; align-items: center; }

/* ---- HUD / 房间 ---- */
.mo-hud {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  padding: 9px 12px; border-radius: 12px;
  background: var(--panel); font-size: 11.5px; color: var(--muted);
}
.mo-hud.sub { padding: 7px 12px; font-size: 11px; }
.mo-hud b { color: var(--primary); font-size: 14px; }
.mo-code { display: flex; align-items: center; justify-content: center; gap: 10px; }
.mo-code b {
  font-size: 26px; letter-spacing: 8px; color: var(--primary);
  background: var(--card); padding: 6px 14px 6px 20px; border-radius: 12px;
  border: 1.5px solid var(--border);
}
.mo-seats { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.mo-seat {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  padding: 10px 6px; border-radius: 12px; background: var(--card);
  border: 1.5px solid var(--seat);
}
.mo-seat.empty { border-style: dashed; opacity: .65; }
.mo-seat.me { box-shadow: 0 0 0 2px rgba(160,108,213,.2); }
.mo-seat .seat-ico { font-size: 20px; }
.mo-seat b { font-size: 11.5px; color: var(--text); }
.seat-tag, .seat-team { font-size: 10px; color: var(--muted); }
.mo-mode { display: flex; gap: 8px; }
.mo-mode button {
  flex: 1; padding: 9px 8px; border-radius: 12px; cursor: pointer;
  font-size: 12px; font-weight: 800; color: var(--muted);
  background: var(--card); border: 1.5px solid var(--border);
}
.mo-mode button.on {
  color: #fff; border-color: transparent;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
.mo-poll { font-size: 10px; color: var(--muted); }
.mo-poll.small { font-size: 9.5px; }

/* ---- 四人状态卡 ---- */
.mo-players { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.mo-player {
  display: flex; align-items: center; gap: 7px; min-width: 0;
  padding: 7px 9px; border-radius: 11px;
  background: var(--card); border: 1.5px solid var(--border);
  transition: border-color .15s, box-shadow .15s;
}
.mo-player.turn { border-color: var(--seat); box-shadow: 0 0 0 2px rgba(160,108,213,.16); }
.mo-player .pl-ico { font-size: 18px; flex-shrink: 0; }
.pl-info { display: flex; flex-direction: column; min-width: 0; text-align: left; }
.pl-info b { font-size: 11.5px; color: var(--text); white-space: nowrap; }
.pl-info b small { font-size: 9px; }
.pl-info span { font-size: 9.5px; color: var(--muted); white-space: nowrap; }
.pl-badge {
  margin-left: auto; font-size: 9px; font-weight: 800; color: #fff;
  background: var(--seat); padding: 2px 6px; border-radius: 999px; white-space: nowrap;
}
.pl-badge.warn { background: #e08b6a; }

/* ---- 棋盘 ---- */
.mo-board {
  position: relative;   /* 掷骰骰子悬浮层以棋盘为基准 */
  width: 100%; aspect-ratio: 1;
  display: grid; grid-template-columns: repeat(7, 1fr); grid-template-rows: repeat(7, 1fr);
  gap: 3px; padding: 5px; box-sizing: border-box;
  border-radius: 16px; border: 1.5px solid var(--border); background: var(--panel);
}
.mo-tile {
  position: relative; min-width: 0; overflow: hidden;
  border-radius: 8px; background: var(--card); border: 1px solid var(--border);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 1px; text-align: center;
}
.mo-tile.here { box-shadow: 0 0 0 2px rgba(160,108,213,.28); }
.mo-tile.start { background: linear-gradient(135deg, #f6e6ff, #eaf4ff); }
.mo-tile.shop { background: linear-gradient(135deg, #fffaf0, #fff); }
.mo-tile.bonus { background: linear-gradient(135deg, #f0fbf4, #fff); }
.mo-tile.chance, .mo-tile.event { background: linear-gradient(135deg, #f3efff, #fff); }
.mo-tile.tax { background: linear-gradient(135deg, #fff0f0, #fff); }
.mo-ico { font-size: 15px; line-height: 1.1; }
.mo-name {
  font-size: 8px; color: var(--muted); line-height: 1.2;
  max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.mo-price { font-size: 7.5px; color: #b07d14; line-height: 1.1; }
.mo-owner { position: absolute; top: 0; right: 1px; font-size: 8px; }
.mo-pawn {
  position: absolute; font-size: 10px; line-height: 1;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,.2));
}
.mo-center {
  grid-column: 2 / 7; grid-row: 2 / 7;
  min-width: 0; border-radius: 14px; padding: 10px;
  background: linear-gradient(150deg, #ffffff, var(--panel));
  border: 1.5px solid var(--border); box-shadow: inset 0 2px 10px rgba(160,108,213,.08);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; text-align: center;
  transition: opacity .3s ease, filter .3s ease;
}
/* 掷骰时中央内容柔化淡出，让浮在棋盘上方的 3D 大骰子独占舞台 */
.mo-center.dimmed { opacity: .14; filter: blur(1.5px); }
.mo-turn-label { font-size: 11px; font-weight: 800; color: var(--primary); }
.mo-log { margin: 0; min-height: 34px; max-width: 100%; font-size: 11px; line-height: 1.5; color: var(--text); }
.mo-buy-who { width: 100%; font-size: 10.5px; color: var(--muted); }
.mo-legend { display: flex; flex-wrap: wrap; gap: 6px; font-size: 10.5px; color: var(--muted); }
.mo-legend span { padding: 4px 8px; border-radius: 8px; background: var(--panel); }

/* ---- 终局 ---- */
.mo-banner { margin: 0; font-size: 13.5px; font-weight: 800; color: var(--text); line-height: 1.6; }
.mo-rank { display: flex; flex-direction: column; gap: 6px; }
.mo-rank-row {
  display: flex; align-items: center; gap: 7px; padding: 8px 10px;
  border-radius: 10px; background: var(--card); border: 1px solid var(--border);
  font-size: 11.5px; color: var(--muted);
}
.mo-rank-row.me { border-color: var(--primary); background: linear-gradient(135deg, #f6e6ff, #fff); }
.rank-no { font-weight: 800; color: var(--primary); width: 14px; }
.rank-ico { font-size: 16px; }
.mo-rank-row b { color: var(--text); }
.mo-rank-row b small { font-size: 9px; }
.rank-team { font-size: 10px; }
.mo-rank-row em { margin-left: auto; font-style: normal; font-weight: 800; color: var(--primary); }
.mo-rank-row small { font-size: 9.5px; }
</style>
