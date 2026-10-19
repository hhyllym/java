/**
 * 消息中心接口封装（Spring Boot /api/message）
 *   POST /api/message/board  { fromId, fromName, content }
 *   GET  /api/message/board?page=&size=
 *   POST /api/message/pm      { fromId, fromName, toName, content }
 *   GET  /api/message/pm?box=inbox|sent&userId=&page=&size=
 *   POST /api/message/pm/read { id, userId }
 *   GET  /api/message/notify?userId=&page=&size=
 *   POST /api/message/notify/read { id, userId }
 *   GET  /api/message/unread?userId=
 * 返回格式跟 auth.js / home.js 一样：{ code, msg, data }
 */

const MSG_BASE = '/api/message'

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
      return { code: -1, msg: `后端返回非 JSON（HTTP ${res.status}）`, data: null }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

async function get(url) {
  try {
    const res = await fetch(url)
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      return { code: -1, msg: `后端返回非 JSON（HTTP ${res.status}）`, data: null }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

export function apiBoardList(page = 1, size = 20) {
  return get(`${MSG_BASE}/board?page=${page}&size=${size}`)
}
export function apiBoardPost(fromId, fromName, content) {
  return post(`${MSG_BASE}/board`, { fromId, fromName, content })
}
export function apiPmList(box, userId, page = 1, size = 15) {
  return get(`${MSG_BASE}/pm?box=${box}&userId=${encodeURIComponent(userId)}&page=${page}&size=${size}`)
}
export function apiPmSend(fromId, fromName, toName, content) {
  return post(`${MSG_BASE}/pm`, { fromId, fromName, toName, content })
}
export function apiPmRead(id, userId) {
  return post(`${MSG_BASE}/pm/read`, { id, userId })
}
export function apiNotifyList(userId, page = 1, size = 15) {
  return get(`${MSG_BASE}/notify?userId=${encodeURIComponent(userId)}&page=${page}&size=${size}`)
}
export function apiNotifyRead(id, userId) {
  return post(`${MSG_BASE}/notify/read`, { id, userId })
}
export function apiUnread(userId) {
  return get(`${MSG_BASE}/unread?userId=${encodeURIComponent(userId)}`)
}
