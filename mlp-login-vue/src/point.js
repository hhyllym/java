/**
 * 积分 API：余额、流水、排行榜、送给好友
 */

async function get(path) {
  try {
    const res = await fetch('/api/point' + path)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function post(path, body) {
  try {
    const res = await fetch('/api/point' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 积分余额 */
export function apiPointBalance(userId) {
  return get('/balance?userId=' + userId)
}

/** 最近积分流水 */
export function apiPointLogs(userId) {
  return get('/logs?userId=' + userId)
}

/** 积分榜 top 20 */
export function apiPointRank() {
  return get('/rank')
}

/** 送积分给好友 */
export function apiPointGift(fromId, toId) {
  return post('/gift', { fromId, toId })
}

/** 领取新人大礼包（每人限一次） */
export function apiClaimBonus(userId) {
  return post('/claim-bonus', { userId })
}

/** 查询新人大礼包是否已领过（领过的账号不再显示横幅） */
export function apiBonusStatus(userId) {
  return get('/bonus-status?userId=' + userId)
}
