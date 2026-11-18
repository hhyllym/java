/**
 * 🎮 博彩风格小游戏 · 共用底座（Options API mixin）
 *
 * 统一约定（和现有「小马老虎机」一致）：
 *  - 只上报 score 用于顶部展示，**不 emit('over')**：GameView 的 onOver 会额外发「破纪录积分」，
 *    而下注类游戏的积分进出已经全部由 wallet.spend / wallet.earn 完成，再结算会重复发分。
 *  - 全程只用游戏内虚拟积分，不接任何真实货币；概率表由各组件公开显示。
 *  - 每局固定 ROUNDS 次机会，用完出结算面板，避免无限刷。
 *  - 自定义投注：betAmount 为实际投入积分（想投多少投多少，≥10，不超过余额）。
 *  - 大注模式：单笔 betAmount ≥ BIG_BET(5000) 时，头奖概率拉到约 30%，并把头奖倍数反推到整体返还≈95%，
 *    既给「30% 中头奖」的爽感，又不会让积分被刷爆。
 */
import { fetchBalance, spend, earn } from '../../wallet'

export const BIG_BET = 5000
export const BET_PRESETS = [10, 50, 100, 500, 1000, 5000]
export const ROUNDS = 10

/** 是否进入大注模式（单笔 ≥ BIG_BET） */
export function isBigBet(amount) {
  return Number(amount) >= BIG_BET
}

/**
 * 大注模式（单笔 ≥ BIG_BET）：把头奖概率拉到约 30%，
 * 并反推头奖倍数使整体返还（RTP）≈ targetRtp，避免积分被刷爆。
 * prizes: [{ key, mult, w, ... }]，其中 key === jackpotKey 的为头奖。
 * 返回新数组（不改入参）：头奖 w 与 mult 已重算，其余项 w 不变、pct 随总量变化。
 */
export function applyBigBet(prizes, jackpotKey, targetRtp = 0.95) {
  const j = prizes.find(p => p.key === jackpotKey)
  if (!j) return prizes.slice()
  const others = prizes.filter(p => p.key !== jackpotKey)
  const Wo = others.reduce((s, p) => s + p.w, 0)
  const Wj = Wo * 0.30 / 0.70
  const total = Wo + Wj
  const sumOther = others.reduce((s, p) => s + p.mult * p.w, 0)
  const targetTotal = targetRtp * total
  const jMult = Math.max(0.1, (targetTotal - sumOther) / Wj)
  return prizes.map(p => {
    if (p.key === jackpotKey) {
      return { ...p, w: Wj, mult: +jMult.toFixed(2), pct: '30%' }
    }
    return { ...p, pct: ((p.w / total) * 100).toFixed(1) + '%' }
  })
}

/** 按权重抽取：list 里每项带 w（权重），返回抽中的那一项 */
export function pickWeighted(list) {
  const total = list.reduce((s, x) => s + x.w, 0)
  let r = Math.random() * total
  for (const x of list) {
    r -= x.w
    if (r < 0) return x
  }
  return list[list.length - 1]
}

/** 洗牌（Fisher-Yates，返回新数组） */
export function shuffle(arr) {
  const a = arr.slice()
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const t = a[i]; a[i] = a[j]; a[j] = t
  }
  return a
}

export const luckyBase = {
  data() {
    return {
      BET_PRESETS, ROUNDS,
      points: 0,            // 可用积分（统一钱包）
      pointsLoading: true,
      betAmount: 10,        // 自定义投注金额（实际投入积分）
      spinsLeft: ROUNDS,    // 本局剩余次数
      invested: 0,          // 本局投入
      won: 0,               // 本局赢得
      busy: false,          // 演出中，锁住交互
      history: [],          // 最近几次结果
      alive: true
    }
  },
  computed: {
    cost() { return Math.max(0, Math.floor(Number(this.betAmount) || 0)) },
    big() { return isBigBet(this.betAmount) },
    net() { return this.won - this.invested },
    finished() { return this.spinsLeft <= 0 }
  },
  async created() { await this.refreshPoints() },
  beforeUnmount() { this.alive = false },
  methods: {
    sleep(ms) { return new Promise(r => setTimeout(r, ms)) },
    /** 夹紧投注：整数、≥10、不超过余额 */
    clampBet() {
      let v = Math.floor(Number(this.betAmount) || 0)
      if (!Number.isFinite(v) || v < 10) v = 10
      if (v > this.points) v = Math.max(10, this.points)
      this.betAmount = v
    },
    async refreshPoints() {
      this.pointsLoading = true
      this.points = (await fetchBalance()).value
      this.pointsLoading = false
      if (this.betAmount > this.points) this.clampBet()
    },
    /** 扣下注、占用一次机会；返回 true 表示可以开始演出 */
    async placeBet() {
      if (this.busy || this.finished || this.pointsLoading) return false
      this.clampBet()
      if (this.cost <= 0) return false
      if (this.points < this.cost) return false
      spend(this.cost)
      this.invested += this.cost
      this.spinsLeft -= 1
      this.busy = true
      this.points = (await fetchBalance()).value
      return true
    },
    /** 落地一次抽奖结果：prize 为本次赢得的积分（0 = 未中），label 进历史 */
    async settle(prize, label) {
      const gain = Math.max(0, Math.round(prize))
      if (gain > 0) { earn(gain); this.won += gain }
      this.history.unshift({ gain, label, bet: this.betAmount })
      if (this.history.length > 6) this.history.pop()
      this.busy = false
      this.points = (await fetchBalance()).value
      this.$emit('score', Math.max(0, this.won))
    },
    /** 重置一局；组件可在自己的 restart() 里先调它，再清自己的演出状态 */
    async resetRound() {
      this.spinsLeft = ROUNDS
      this.invested = 0
      this.won = 0
      this.history = []
      this.busy = false
      await this.refreshPoints()
      this.$emit('score', 0)
    }
  }
}
