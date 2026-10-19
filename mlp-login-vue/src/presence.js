/**
 * 好友在线状态接口封装（Spring Boot /api/presence，内存实现）
 *   POST /api/presence/heartbeat { userId }   上报心跳（登录后每 30 秒）
 *   POST /api/presence/offline   { userId }   主动下线（退出 / 切换账号）
 *   GET  /api/presence/status?ids=1,2,3         批量查询 { userId: online }
 */

const PRESENCE_BASE = '/api/presence'

async function post(url, body) {
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body || {})
    })
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      return { code: -1, msg: '后端返回非 JSON', data: null }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function get(url) {
  try {
    const res = await fetch(url)
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      return { code: -1, msg: '后端返回非 JSON', data: null }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 上报心跳 */
export function apiPresenceBeat(userId) {
  return post(`${PRESENCE_BASE}/heartbeat`, { userId })
}

/** 主动下线 */
export function apiPresenceOffline(userId) {
  return post(`${PRESENCE_BASE}/offline`, { userId })
}

/** 批量查询在线状态，返回 { "1": true, "2": false } */
export function apiPresenceStatus(ids) {
  const list = (ids || []).filter(Boolean)
  return get(`${PRESENCE_BASE}/status?ids=${list.join(',')}`)
}

let beatTimer = null

/**
 * 启动全局心跳：每 30 秒给"当前登录用户"上报一次。
 * 用 getSession 实时读取，所以切换账号 / 退出后自然会对新用户生效。
 */
export function initPresence() {
  stopPresence()
  beatTimer = setInterval(() => {
    try {
      const raw = localStorage.getItem('mlp_session') || sessionStorage.getItem('mlp_session')
      if (!raw) return
      const s = JSON.parse(raw)
      if (s && s.id) apiPresenceBeat(s.id)
    } catch (e) {}
  }, 30000)
}

/** 停止心跳并上报离线（页面卸载 / 应用销毁时调用） */
export function stopPresence() {
  if (beatTimer) {
    clearInterval(beatTimer)
    beatTimer = null
  }
  try {
    const raw = localStorage.getItem('mlp_session') || sessionStorage.getItem('mlp_session')
    if (raw) {
      const s = JSON.parse(raw)
      if (s && s.id) {
        // 页面卸载时只能发同步请求才可靠，这里尽力而为
        const xhr = new XMLHttpRequest()
        xhr.open('POST', `${PRESENCE_BASE}/offline`, false)
        xhr.setRequestHeader('Content-Type', 'application/json')
        xhr.send(JSON.stringify({ userId: s.id }))
      }
    }
  } catch (e) {}
}
