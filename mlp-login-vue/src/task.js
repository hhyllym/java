/**
 * 每日任务 API：今日任务、上报进度、领奖
 */

async function get(path) {
  try {
    const res = await fetch('/api/task' + path)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function post(path, body) {
  try {
    const res = await fetch('/api/task' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 今日任务列表（带进度与领奖状态） */
export function apiTaskToday(userId) {
  return get('/today?userId=' + userId)
}

/** 上报进度：type = login / msg / game / view / checkin */
export function apiTaskBump(userId, type, n = 1) {
  return post('/bump', { userId, type, n })
}

/** 领奖 */
export function apiTaskClaim(userId, taskId) {
  return post('/claim', { userId, taskId })
}
