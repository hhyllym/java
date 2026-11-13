/**
 * 统一钱包 · 全站「可用积分」的唯一入口
 *
 * 以前的问题：游戏中心 / 云驰冲刺赛 / 糖豆老虎机读的是本地学习存档
 * (mlp_learn_v1.points)，而赚积分页、个人主页、商店、联机大富翁读的是
 * 后端账户余额 (/api/point/balance)。于是「账户里明明有积分，进游戏却显示 0」。
 *
 * 现在统一成：
 *   在线  = 后端账户余额 + 游戏内本地流水（报名费 / 奖金；后端没有通用扣款接口）
 *   离线  = 本地学习积分（兼容老存档，保证没起 Spring Boot 时也能玩）
 *
 * 游戏内消费 / 收入会同步写一份到本地学习存档，这样离线显示与在线一致；
 * 破纪录奖励仍由 GameView 走 apiGameScore 存进后端，不在这里重复记。
 */
import { apiPointBalance } from './point'
import { loadLearn, addPoints } from './learn'
import { getSession } from './auth'

const KEY = 'mlp_wallet_v1'

function loadDelta() {
  try {
    const n = Number(localStorage.getItem(KEY))
    return Number.isFinite(n) ? n : 0
  } catch (e) { return 0 }
}

function saveDelta(n) {
  try { localStorage.setItem(KEY, String(n)) } catch (e) {}
}

/** 本地学习积分（离线时的余额来源） */
export function localBalance() {
  return Math.max(0, loadLearn().points || 0)
}

/**
 * 拉取统一余额，返回 { value, online }。
 * online=false 表示后端没连上，已自动回退到本地学习积分。
 */
export async function fetchBalance() {
  const s = getSession() || {}
  if (s.id) {
    try {
      const res = await apiPointBalance(s.id)
      if (res && res.code === 200) {
        const n = Number(res.data)
        if (Number.isFinite(n)) {
          return { value: Math.max(0, n + loadDelta()), online: true }
        }
      }
    } catch (e) { /* 后端没起就走本地兜底 */ }
  }
  return { value: localBalance(), online: false }
}

/** 花积分（报名费 / 老虎机投入）。会同步本地存档，保证离线也扣得到 */
export function spend(n) {
  const v = Math.max(0, Number(n) || 0)
  saveDelta(loadDelta() - v)
  addPoints(-v)
  return v
}

/** 收积分（比赛奖金等） */
export function earn(n) {
  const v = Math.max(0, Number(n) || 0)
  saveDelta(loadDelta() + v)
  addPoints(v)
  return v
}

/** 刷新显示用：进页面时调一次 */
export async function refreshBalance() {
  const r = await fetchBalance()
  return r.value
}
