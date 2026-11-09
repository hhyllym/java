/**
 * 游戏成绩 API：提交、排行榜、好友对战对比
 */

async function get(path) {
  try {
    const res = await fetch('/api/game' + path)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function post(path, body) {
  try {
    const res = await fetch('/api/game' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 提交一局成绩（同时把破纪录增量作为真实积分发放到账号） */
export function apiGameScore(gameId, userId, score, name) {
  return post('/score', { gameId, userId, score, name })
}

/** 某游戏排行榜 top 20 */
export function apiGameRank(gameId) {
  return get('/rank?gameId=' + gameId)
}

/** 我和好友的最高分对比 */
export function apiGameVs(meId, friendId, gameId) {
  return get('/vs?meId=' + meId + '&friendId=' + friendId + '&gameId=' + gameId)
}
