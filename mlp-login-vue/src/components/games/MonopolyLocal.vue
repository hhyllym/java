<template>
  <div class="mp-wrap">
    <!-- ===== 开局设置：四人组队 / 混战、真人 / 电脑 ===== -->
    <template v-if="stage === 'setup'">
      <div class="mp-setup">
        <h3 class="mp-setup-title">🎲 四人组队 · 糖霜镇大富翁</h3>
        <p class="mp-setup-sub">选玩法，再决定每个座位由谁来玩；四个人可以共用一台电脑轮流掷骰～</p>

        <div class="mp-mode">
          <button :class="{ on: mode === 'team' }" @click="setMode('team')">🤝 2v2 组队</button>
          <button :class="{ on: mode === 'solo' }" @click="setMode('solo')">🏆 四人混战</button>
        </div>
        <p class="mp-mode-tip">
          {{ mode === 'team'
            ? '星光队（星澜 + 泡芙）对 彩虹队（云驰 + 金穗）；队友互不收租，按队伍总分决胜。'
            : '四人各自为战，按个人总分排名；踩到别人的店都要付租金。' }}
        </p>

        <div class="mp-seats">
          <div
            v-for="p in players" :key="p.id"
            class="mp-seat" :class="{ active: p.id === 0 }"
            :style="{ '--seat': p.color }"
          >
            <span class="seat-ico">{{ p.icon }}</span>
            <b>{{ label(p) }}</b>
            <span v-if="mode === 'team'" class="seat-team">{{ teamName(p.team) }}</span>
            <button v-if="p.id !== 0" class="seat-toggle" @click="toggleHuman(p)">
              {{ p.human ? '🙋 真人' : '🤖 电脑' }}
            </button>
            <span v-else class="seat-you">🙋 你</span>
          </div>
        </div>

        <div class="mp-quick">
          <span>快速设置：</span>
          <button @click="preset('ai')">一个人玩（3 电脑）</button>
          <button @click="preset('hotseat')">四人同屏轮流</button>
          <button @click="preset('duo')">两人 + 2 电脑</button>
        </div>

        <p class="mp-tip">💡 每人各走 {{ MAX_ROUND }} 轮，共 {{ MAX_ROUND * 4 }} 次掷骰；经过城堡 +20 分、+10 币，买店收租赚分。</p>
        <button class="mp-btn big" @click="start">🎲 开始巡游！</button>
      </div>
    </template>

    <!-- ===== 终局：四人排名 / 队伍胜负 ===== -->
    <template v-else-if="stage === 'result'">
      <div class="mp-result">
        <h3 class="mp-result-title">🏁 巡游结束！</h3>
        <p v-if="mode === 'team'" class="mp-result-banner">
          {{ winnerTeam === 1 ? '🌟 星光队获胜！' : winnerTeam === 2 ? '🌈 彩虹队获胜！' : '🤝 平局，友谊第一！' }}
          星光队 {{ teamScore(1) }} 分 · 彩虹队 {{ teamScore(2) }} 分
        </p>
        <p v-else class="mp-result-banner">
          {{ rankOf(players[0]) === 1 ? '🏆 你拿下第一名！' : `你这局第 ${rankOf(players[0])} 名，再接再厉～` }}
        </p>
        <div class="mp-rank">
          <div v-for="(p, i) in ranking" :key="p.id" class="mp-rank-row" :class="{ me: p.id === 0 }">
            <span class="rank-no">{{ i + 1 }}</span>
            <span class="rank-ico">{{ p.icon }}</span>
            <b>{{ label(p) }}</b>
            <span v-if="mode === 'team'" class="rank-team">{{ teamName(p.team) }}</span>
            <em>{{ scoreOf(p) }} 分</em>
            <small>💎 {{ p.coins }} · 🏠 {{ p.shops.length }}</small>
          </div>
        </div>
        <p class="mp-tip">本局计入成绩：<b>{{ myScore }} 分</b>{{ mode === 'team' ? '（你所在队伍的总分）' : '（你的个人总分）' }}</p>
        <button class="mp-btn big" @click="claim">🎉 领取成绩并结算</button>
      </div>
    </template>

    <!-- ===== 对局中 ===== -->
    <template v-else>
      <div class="mp-hud">
        <span>第 <b>{{ round }}</b> / {{ MAX_ROUND}} 轮</span>
        <span>{{ mode === 'team' ? '🤝 2v2 组队' : '🏆 四人混战' }}</span>
        <span>{{ mode === 'team' ? '🌟 队伍' : '🦄 我的' }} <b>{{ myScore }}</b> 分</span>
      </div>

      <div class="mp-players">
        <div
          v-for="p in players" :key="p.id"
          class="mp-player" :class="{ turn: p.id === turnIdx, ai: !p.human }"
          :style="{ '--seat': p.color }"
        >
          <span class="pl-ico">{{ p.icon }}</span>
          <div class="pl-info">
            <b>{{ label(p) }}<small v-if="!p.human">🤖</small></b>
            <span>{{ scoreOf(p) }} 分 · 💎 {{ p.coins }} · 🏠 {{ p.shops.length }}</span>
          </div>
          <span v-if="p.id === turnIdx" class="pl-turn">行动中</span>
        </div>
      </div>

      <div class="mp-board">
        <div
          v-for="t in tiles" :key="t.i"
          class="mp-tile" :class="[t.type, { here: t.pawns.length }]"
          :style="[t.style, t.owner >= 0 ? { borderColor: players[t.owner].color } : {}]"
          :title="tileTitle(t)"
        >
          <span class="mp-ico">{{ t.icon }}</span>
          <span class="mp-name">{{ t.name }}</span>
          <span v-if="t.type === 'shop'" class="mp-price">{{ t.price }}币</span>
          <span v-if="t.owner >= 0" class="mp-owner">{{ players[t.owner].icon }}</span>
          <span
            v-for="p in t.pawns" :key="p.id"
            class="mp-pawn" :style="pawnStyle(p.id)"
          >{{ p.icon }}</span>
        </div>

        <div class="mp-center">
          <div class="mp-turn-label">
            {{ busy ? `${players[turnIdx].icon} ${label(players[turnIdx])} 行动中…` : `${players[turnIdx].icon} 轮到 ${label(players[turnIdx])}` }}
          </div>
          <div class="mp-dice" :class="{ rolling: busy }">{{ dice ? dice : '🎲' }}</div>
          <p class="mp-log">{{ log }}</p>

          <div class="mp-actions">
            <template v-if="stage === 'buy' && buyTile">
              <span class="mp-buy-who">{{ players[turnIdx].icon }} {{ label(players[turnIdx]) }}：要买下{{ buyTile.name }}吗？</span>
              <button class="mp-btn" @click="buy(true)">🏠 买下 · {{ buyTile.price }}币</button>
              <button class="mp-btn ghost" @click="buy(false)">跳过</button>
            </template>
            <template v-else>
              <button class="mp-btn" :disabled="busy || !players[turnIdx].human" @click="roll">
                {{ busy ? '移动中…' : `🎲 ${label(players[turnIdx])} 掷骰子` }}
              </button>
            </template>
          </div>
        </div>
      </div>

      <div class="mp-legend">
        <span>🏰 经过起点 +20 分 / +10 币</span>
        <span>🏠 买店后自己踩收营收，对手踩收租金</span>
        <span>🎁 机会卡 · ✨ 魔法事件 · 💸 税务所</span>
      </div>
      <p class="mp-tip">💡 真人座位轮到自己时点「掷骰子」；电脑托管会自动走。{{ mode === 'team' ? '队友之间免租金，队伍总分高的一队获胜。' : '四个人里总分最高的获胜。' }}</p>
    </template>
  </div>
</template>

<script>
import sound from '../../sound'

/** 🎲 糖霜镇大富翁：本地四人同桌（练习模式），可 2v2 组队，可本地轮流 / AI 托管 */
const MAX_ROUND = 8

/** 四位固定座位：星澜（你）、云驰、泡芙、金穗 */
const SEATS = [
  { id: 0, name: '星澜',     icon: '🦄', color: '#a06cd5', team: 1, human: true },
  { id: 1, name: '云驰',     icon: '🌈', color: '#6ec6e6', team: 2, human: false },
  { id: 2, name: '泡芙',     icon: '🎈', color: '#f39ac0', team: 1, human: false },
  { id: 3, name: '金穗', icon: '🍎', color: '#eda14e', team: 2, human: false }
]
const TEAMS = { 1: '🌟 星光队', 2: '🌈 彩虹队' }

/** 24 格环形棋盘 */
const TILES = [
  { type: 'start',  icon: '🏰', name: '星辉城堡' },
  { type: 'shop',   icon: '🍎', name: '金穗果园', price: 20, rent: 10 },
  { type: 'event',  icon: '✨', name: '魔法事件' },
  { type: 'bonus',  icon: '🎈', name: '派对广场', value: 18 },
  { type: 'shop',   icon: '🧁', name: '甜品小屋', price: 24, rent: 12 },
  { type: 'chance', icon: '🎁', name: '机会卡' },
  { type: 'bonus',  icon: '🌈', name: '彩虹瀑布', value: 25 },
  { type: 'shop',   icon: '💎', name: '珍品时装', price: 32, rent: 16 },
  { type: 'bonus',  icon: '⚡', name: '闪电赛道', value: 20 },
  { type: 'shop',   icon: '🍬', name: '糖果小铺', price: 18, rent: 9 },
  { type: 'event',  icon: '✨', name: '魔法事件' },
  { type: 'shop',   icon: '🏰', name: '云顶城', price: 40, rent: 20 },
  { type: 'bonus',  icon: '🎪', name: '超级派对', value: 30 },
  { type: 'chance', icon: '🎁', name: '机会卡' },
  { type: 'shop',   icon: '🦋', name: '动物之家', price: 22, rent: 11 },
  { type: 'tax',    icon: '💸', name: '税务所', value: 14 },
  { type: 'shop',   icon: '📚', name: '金橡树书屋', price: 28, rent: 14 },
  { type: 'event',  icon: '✨', name: '魔法事件' },
  { type: 'shop',   icon: '🥤', name: '苹果汁摊', price: 16, rent: 8 },
  { type: 'chance', icon: '🎁', name: '机会卡' },
  { type: 'bonus',  icon: '🌸', name: '茸茸花园', value: 22 },
  { type: 'shop',   icon: '🎀', name: '礼品店', price: 26, rent: 13 },
  { type: 'event',  icon: '🌩️', name: '疾风云' },
  { type: 'bonus',  icon: '🎠', name: '彩带旋转台', value: 15 }
]

/** 事件 / 机会卡池，四位玩家共用同一套效果 */
const CARDS = [
  { text: '🎉 泡芙请你吃蛋糕，+25 分', score: 25 },
  { text: '🍎 帮金穗收苹果，+20 宝石币', coins: 20 },
  { text: '📚 星澜送你一本魔法书，+30 分', score: 30 },
  { text: '💎 绮罗送了件新礼服，+15 分', score: 15 },
  { text: '🌈 一阵疾风吹跑了 10 宝石币', coins: -10 },
  { text: '🦋 陪茸茸找小动物，-8 分', score: -8 },
  { text: '🧁 甜品屋大促销，+15 宝石币', coins: 15 },
  { text: '🎈 派对抽奖中奖啦，+20 分', score: 20 }
]

/** 顺时针把第 i 格映射到 7×7 网格的 (行, 列) */
function cellOf(i) {
  if (i <= 6) return { r: 1, c: i + 1 }
  if (i <= 12) return { r: i - 5, c: 7 }
  if (i <= 18) return { r: 7, c: 19 - i }
  return { r: 25 - i, c: 1 }
}

/** 四个棋子在同一格时的四角站位 */
const PAWN_POS = [
  { left: '1px', top: '0' },
  { right: '1px', top: '0' },
  { left: '1px', bottom: '0' },
  { right: '1px', bottom: '0' }
]

function freshSeats() {
  return SEATS.map(s => ({ ...s, pos: 0, coins: 80, score: 0, shops: [] }))
}

export default {
  name: 'MonopolyLocal',
  emits: ['score', 'over'],
  data() {
    return {
      MAX_ROUND,
      stage: 'setup',        // setup | play | buy | result
      mode: 'team',          // team | solo
      turnIdx: 0,
      busy: false,
      round: 1,
      dice: 0,
      log: '',
      players: freshSeats(),
      ranking: [],
      buyTile: null,
      buyResolve: null,
      timers: [],
      dead: false
    }
  },
  computed: {
    tiles() {
      return TILES.map((t, i) => {
        const cell = cellOf(i)
        return {
          ...t,
          i,
          style: { gridRow: cell.r, gridColumn: cell.c },
          owner: this.ownerOf(i),
          pawns: this.players.filter(p => p.pos === i)
        }
      })
    },
    /** 计入成绩的分：组队按队伍合计，混战按个人 */
    myScore() {
      const me = this.players[0]
      if (this.mode === 'team') return this.teamScore(me.team)
      return this.scoreOf(me)
    },
    winnerTeam() {
      if (this.mode !== 'team') return 0
      const a = this.teamScore(1)
      const b = this.teamScore(2)
      return a === b ? 0 : (a > b ? 1 : 2)
    }
  },
  beforeUnmount() {
    this.dead = true
    this.timers.forEach(clearTimeout)
    this.timers = []
    // 卸载时若卡在「买不买」的等待里，放行让异步流程自然收尾
    if (this.buyResolve) {
      const r = this.buyResolve
      this.buyResolve = null
      r(false)
    }
  },
  methods: {
    label(p) { return p.id === 0 ? '你 · 星澜' : p.name },
    teamName(t) { return TEAMS[t] },
    pawnStyle(id) { return PAWN_POS[id] },
    scoreOf(p) { return Math.max(0, p.score + p.shops.length * 12) },
    teamScore(t) {
      return this.players.filter(p => p.team === t)
        .reduce((sum, p) => sum + this.scoreOf(p), 0)
    },
    rankOf(p) { return this.ranking.findIndex(x => x.id === p.id) + 1 },
    ownerOf(i) {
      const p = this.players.find(x => x.shops.includes(i))
      return p ? p.id : -1
    },
    tileTitle(t) {
      const base = t.icon + ' ' + t.name
      if (t.type === 'shop') {
        const o = this.ownerOf(t.i)
        const owner = o >= 0 ? ` · 属于 ${this.label(this.players[o])}` : ' · 无人经营'
        return `${base} · 售价 ${t.price} 币 / 租金 ${t.rent} 分${owner}`
      }
      if (t.type === 'bonus') return `${base} · +${t.value} 分`
      return base
    },
    sleep(ms) {
      return new Promise(resolve => { this.timers.push(setTimeout(resolve, ms)) })
    },
    emitScore() { this.$emit('score', this.myScore) },

    /* ---------- 开局设置 ---------- */
    setMode(m) {
      if (this.mode === m) return
      this.mode = m
      sound.click()
    },
    toggleHuman(p) {
      p.human = !p.human
      sound.click()
    },
    preset(kind) {
      sound.click()
      this.players.forEach((p, i) => {
        if (i === 0) return
        p.human = kind === 'hotseat' ? true : kind === 'duo' ? i === 1 : false
      })
    },
    start() {
      sound.click()
      // 只重置对局数据，保留设置页里选好的「真人 / 电脑」
      this.players = freshSeats().map((p, i) => ({ ...p, human: this.players[i].human }))
      this.stage = 'play'
      this.round = 1
      this.turnIdx = 0
      this.dice = 0
      this.busy = false
      this.log = `第 1 / ${MAX_ROUND} 轮开始，${this.label(this.players[0])}先掷骰子吧！`
      this.$emit('score', 0)
      this.advance()
    },

    /* ---------- 回合调度 ---------- */
    /** 推进到当前座位：真人等待点击，电脑自动走 */
    async advance() {
      if (this.dead || this.stage !== 'play') return
      const p = this.players[this.turnIdx]
      if (p.human) {
        this.busy = false
        this.log = `轮到 ${p.icon} ${this.label(p)}（第 ${this.round} / ${MAX_ROUND} 轮），点「掷骰子」～`
        return
      }
      this.busy = true
      await this.sleep(520)
      if (this.dead || this.stage !== 'play') return
      await this.takeTurn(p)
      if (this.dead || this.stage !== 'play') return
      await this.sleep(280)
      if (this.dead) return
      this.afterTurn()
    },
    /** 真人点击掷骰 */
    async roll() {
      if (this.busy || this.stage !== 'play') return
      const p = this.players[this.turnIdx]
      if (!p.human) return
      this.busy = true
      sound.click()
      await this.takeTurn(p)
      if (this.dead || this.stage !== 'play') return
      await this.sleep(280)
      if (this.dead) return
      this.afterTurn()
    },
    /** 一次完整走棋：掷骰 → 移动 → 落地结算 */
    async takeTurn(p) {
      const d = 1 + Math.floor(Math.random() * 6)
      this.dice = d
      this.log = `${p.icon} ${this.label(p)} 掷出 ${d} 点，出发！`
      await this.walk(p, d)
      if (this.dead) return
      await this.land(p)
      if (this.dead) return
      this.emitScore()
    },
    /** 四人都走完算一轮 */
    afterTurn() {
      if (this.turnIdx >= this.players.length - 1) {
        if (this.round >= MAX_ROUND) { this.finish(); return }
        this.round += 1
        this.turnIdx = 0
      } else {
        this.turnIdx += 1
      }
      this.advance()
    },

    /* ---------- 移动 ---------- */
    async walk(p, steps) {
      for (let k = 0; k < steps; k++) {
        await this.sleep(110)
        if (this.dead) return
        const next = (p.pos + 1) % TILES.length
        if (next === 0) {
          p.score += 20
          p.coins += 10
          this.log = `🏰 ${this.label(p)} 经过星辉城堡，+20 分、+10 币！`
          sound.twinkle()
          this.emitScore()
        }
        p.pos = next
        if (p.human) sound.hover()
      }
    },

    /* ---------- 落地结算 ---------- */
    async land(p) {
      const t = TILES[p.pos]
      const who = `${p.icon} ${this.label(p)}`
      const label = t.icon + ' ' + t.name

      if (t.type === 'shop') {
        const owner = this.ownerOf(t.i)
        if (owner < 0) {
          if (p.coins < t.price) {
            this.log = `${who} 想买${t.name}，可惜宝石币不够`
          } else {
            let yes
            if (p.human) {
              this.buyTile = t
              this.stage = 'buy'
              this.log = `🏠 ${who} 来到${t.name}，要花 ${t.price} 币买下吗？`
              yes = await new Promise(resolve => { this.buyResolve = resolve })
              this.buyResolve = null
              this.buyTile = null
              this.stage = 'play'
              if (this.dead) return
            } else {
              // 电脑：留点周转金，越便宜越想买
              yes = p.coins - t.price >= 12 && Math.random() < 0.78
            }
            if (yes) {
              p.coins -= t.price
              p.shops.push(t.i)
              this.log = `🏠 ${who} 买下${t.name}，以后踩到要付 ${t.rent} 分租金`
              sound.magic()
            } else {
              this.log = `${who} 路过${t.name}，先不买～`
            }
          }
        } else if (owner === p.id) {
          const income = Math.round(t.rent / 2)
          p.coins += income
          this.log = `🏠 ${who} 回到自己的${t.name}，营收 +${income} 币`
        } else {
          const lord = this.players[owner]
          const teammate = this.mode === 'team' && lord.team === p.team
          if (teammate) {
            const gift = Math.round(t.rent / 2)
            p.coins += gift
            this.log = `🤝 ${who} 踩到队友 ${lord.icon} 的${t.name}，免租金还收到 ${gift} 币补给`
          } else {
            const pay = Math.min(t.rent, p.coins)
            p.coins -= pay
            lord.score += pay
            lord.coins += pay
            this.log = `💰 ${who} 踩到 ${lord.icon} ${this.label(lord)} 的${t.name}，付租 ${pay} 币（对方 +${pay} 分）`
            if (p.human) sound.fail()
            else sound.magic()
          }
        }
      } else if (t.type === 'bonus') {
        p.score += t.value
        this.log = `${label} 打卡成功，${who} +${t.value} 分！`
        sound.twinkle()
      } else if (t.type === 'tax') {
        const pay = Math.min(t.value, p.coins)
        p.coins -= pay
        this.log = `${label}，${who} 交了 ${pay} 币`
        sound.fail()
      } else if (t.type === 'start') {
        p.coins += 10
        this.log = `🏰 ${who} 停在星辉城堡，补给 +10 币`
      } else {
        const card = CARDS[Math.floor(Math.random() * CARDS.length)]
        p.score = Math.max(0, p.score + (card.score || 0))
        p.coins = Math.max(0, p.coins + (card.coins || 0))
        this.log = `${who} 抽卡：${card.text}`
        if ((card.score || 0) + (card.coins || 0) > 0) sound.magic()
        else sound.fail()
      }
      this.emitScore()
    },
    buy(yes) {
      sound.click()
      if (this.buyResolve) {
        const r = this.buyResolve
        this.buyResolve = null
        r(yes)
      }
    },

    /* ---------- 终局 ---------- */
    finish() {
      this.busy = true
      this.stage = 'result'
      this.ranking = [...this.players].sort((a, b) => this.scoreOf(b) - this.scoreOf(a))
      this.log = '🏁 巡游结束！'
      this.emitScore()
      sound.success()
    },
    claim() {
      sound.click()
      const total = this.myScore
      this.$emit('score', total)
      this.$emit('over', total)
    }
  }
}
</script>

<style scoped>
.mp-wrap { display: flex; flex-direction: column; gap: 11px; }

/* ---- 开局设置 ---- */
.mp-setup, .mp-result {
  padding: 16px; border-radius: 16px;
  background: var(--panel); border: 1.5px solid var(--border);
  display: flex; flex-direction: column; gap: 11px; align-items: stretch; text-align: center;
}
.mp-setup-title, .mp-result-title { margin: 0; font-size: 17px; font-weight: 800; color: var(--primary); }
.mp-setup-sub { margin: 0; font-size: 11.5px; color: var(--muted); line-height: 1.6; }
.mp-mode { display: flex; gap: 8px; justify-content: center; }
.mp-mode button {
  flex: 1; padding: 10px 8px; border-radius: 12px; cursor: pointer;
  font-size: 12.5px; font-weight: 800; color: var(--muted);
  background: var(--card); border: 1.5px solid var(--border);
}
.mp-mode button.on {
  color: #fff; border-color: transparent;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160, 108, 213, .25);
}
.mp-mode-tip { margin: 0; font-size: 11px; color: var(--muted); line-height: 1.6; min-height: 34px; }
.mp-seats { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.mp-seat {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 6px; border-radius: 12px;
  background: var(--card); border: 1.5px solid var(--seat);
}
.mp-seat .seat-ico { font-size: 21px; line-height: 1.1; }
.mp-seat b { font-size: 12px; color: var(--text); }
.seat-team { font-size: 10px; color: var(--muted); }
.seat-toggle, .mp-quick button {
  cursor: pointer; border: 1px solid var(--border); background: var(--panel);
  color: var(--primary); font-size: 10.5px; font-weight: 700;
  padding: 4px 10px; border-radius: 999px;
}
.seat-toggle:hover, .mp-quick button:hover { filter: brightness(.96); }
.seat-you { font-size: 10.5px; color: var(--muted); }
.mp-quick { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; justify-content: center; font-size: 10.5px; color: var(--muted); }

/* ---- 终局 ---- */
.mp-result-banner { margin: 0; font-size: 13.5px; font-weight: 800; color: var(--text); line-height: 1.6; }
.mp-rank { display: flex; flex-direction: column; gap: 6px; }
.mp-rank-row {
  display: flex; align-items: center; gap: 7px; padding: 8px 10px;
  border-radius: 10px; background: var(--card); border: 1px solid var(--border);
  font-size: 11.5px; color: var(--muted);
}
.mp-rank-row.me { border-color: var(--primary); background: linear-gradient(135deg, #f6e6ff, #fff); }
.rank-no { font-weight: 800; color: var(--primary); width: 14px; }
.rank-ico { font-size: 16px; }
.mp-rank-row b { color: var(--text); }
.rank-team { font-size: 10px; }
.mp-rank-row em { margin-left: auto; font-style: normal; font-weight: 800; color: var(--primary); }
.mp-rank-row small { font-size: 9.5px; }

/* ---- 对局 HUD ---- */
.mp-hud {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  padding: 9px 12px; border-radius: 12px;
  background: var(--panel); font-size: 11.5px; color: var(--muted);
}
.mp-hud b { color: var(--primary); font-size: 14px; }
.mp-players { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
.mp-player {
  display: flex; align-items: center; gap: 7px; min-width: 0;
  padding: 7px 9px; border-radius: 11px;
  background: var(--card); border: 1.5px solid var(--border);
  transition: border-color .15s, box-shadow .15s;
}
.mp-player.turn { border-color: var(--seat); box-shadow: 0 0 0 2px rgba(160, 108, 213, .16); }
.mp-player .pl-ico { font-size: 18px; line-height: 1; flex-shrink: 0; }
.pl-info { display: flex; flex-direction: column; min-width: 0; text-align: left; }
.pl-info b { font-size: 11.5px; color: var(--text); white-space: nowrap; }
.pl-info b small { font-size: 9px; margin-left: 3px; }
.pl-info span { font-size: 9.5px; color: var(--muted); white-space: nowrap; }
.pl-turn { margin-left: auto; font-size: 9px; font-weight: 800; color: #fff; background: var(--seat); padding: 2px 6px; border-radius: 999px; white-space: nowrap; }

/* ---- 棋盘 ---- */
.mp-board {
  width: 100%; aspect-ratio: 1;
  display: grid; grid-template-columns: repeat(7, 1fr); grid-template-rows: repeat(7, 1fr);
  gap: 3px; padding: 5px; box-sizing: border-box;
  border-radius: 16px; border: 1.5px solid var(--border); background: var(--panel);
}
.mp-tile {
  position: relative; min-width: 0; overflow: hidden;
  border-radius: 8px; background: var(--card); border: 1px solid var(--border);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 1px; text-align: center;
  transition: box-shadow .15s, border-color .15s;
}
.mp-tile.here { box-shadow: 0 0 0 2px rgba(160, 108, 213, .28); }
.mp-tile.start { background: linear-gradient(135deg, #f6e6ff, #eaf4ff); }
.mp-tile.shop { background: linear-gradient(135deg, #fffaf0, #fff); }
.mp-tile.bonus { background: linear-gradient(135deg, #f0fbf4, #fff); }
.mp-tile.chance, .mp-tile.event { background: linear-gradient(135deg, #f3efff, #fff); }
.mp-tile.tax { background: linear-gradient(135deg, #fff0f0, #fff); }
.mp-ico { font-size: 15px; line-height: 1.1; }
.mp-name {
  font-size: 8px; color: var(--muted); line-height: 1.2;
  max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.mp-price { font-size: 7.5px; color: #b07d14; line-height: 1.1; }
.mp-owner { position: absolute; top: 0; right: 1px; font-size: 8px; }
.mp-pawn {
  position: absolute; font-size: 10px; line-height: 1;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,.2));
}

.mp-center {
  grid-column: 2 / 7; grid-row: 2 / 7;
  min-width: 0; border-radius: 14px; padding: 10px;
  background: linear-gradient(150deg, #ffffff, var(--panel));
  border: 1.5px solid var(--border); box-shadow: inset 0 2px 10px rgba(160,108,213,.08);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; text-align: center;
}
.mp-turn-label { font-size: 11px; font-weight: 800; color: var(--primary); }
.mp-dice {
  width: 48px; height: 48px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 13px; font-size: 24px; font-weight: 800; color: var(--primary);
  background: var(--card); border: 2px solid var(--border);
  box-shadow: 0 4px 10px rgba(160,108,213,.18);
}
.mp-dice.rolling { animation: mp-shake .5s ease-in-out infinite alternate; }
@keyframes mp-shake { to { transform: rotate(7deg) scale(.94); } }
.mp-log {
  margin: 0; min-height: 34px; max-width: 100%;
  font-size: 11px; line-height: 1.5; color: var(--text);
}
.mp-actions { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; align-items: center; }
.mp-buy-who { width: 100%; font-size: 10.5px; color: var(--muted); }
.mp-btn {
  border: none; cursor: pointer; font-weight: 800;
  padding: 9px 16px; border-radius: 999px; font-size: 12.5px;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 5px 12px rgba(160, 108, 213, .28);
  transition: transform .12s, filter .12s;
}
.mp-btn.big { padding: 11px 22px; font-size: 13.5px; }
.mp-btn:hover:not(:disabled) { filter: brightness(1.07); transform: translateY(-1px); }
.mp-btn:active { transform: scale(.96); }
.mp-btn:disabled { opacity: .55; cursor: not-allowed; transform: none; }
.mp-btn.ghost {
  color: var(--primary); background: var(--card);
  border: 1.5px solid var(--border); box-shadow: none;
}

.mp-legend { display: flex; flex-wrap: wrap; gap: 6px; font-size: 10.5px; color: var(--muted); }
.mp-legend span { padding: 4px 8px; border-radius: 8px; background: var(--panel); }
.mp-tip { margin: 0; font-size: 11px; color: var(--muted); line-height: 1.6; text-align: left; }
.mp-tip b { color: var(--primary); }
.mp-setup .mp-tip, .mp-result .mp-tip { text-align: center; }
</style>
