/**
 * 🏠 星糖大厅接口封装（Spring Boot /api/hall）
 *   GET  /api/hall/messages?limit=            大厅聊天记录（正序，最后一条是最新）
 *   POST /api/hall/messages  { userId, content }    发一条公共消息
 *   GET  /api/hall/online                      当前在线的小马
 *   GET  /api/hall/rooms                       游戏邀请列表
 *   POST /api/hall/rooms     { userId, game, title, maxMembers }   发起邀请
 *   POST /api/hall/rooms/join   { userId, roomId }
 *   POST /api/hall/rooms/leave  { userId, roomId }
 *   POST /api/hall/rooms/close  { userId, roomId }
 */

const HALL_BASE = '/api/hall'

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

/** 大厅聊天记录，data: [{ id, userId, name, avatar, content, type, time }] */
export function apiHallMessages(limit = 100) {
  return get(`${HALL_BASE}/messages?limit=${encodeURIComponent(limit)}`)
}

/** 发一条公共消息 */
export function apiHallSend(userId, content) {
  return post(`${HALL_BASE}/messages`, { userId, content })
}

/** 在线的小马，data: [{ id, name, avatar, points }] */
export function apiHallOnline() {
  return get(`${HALL_BASE}/online`)
}

/** 游戏邀请列表，data: [{ id, game, gameName, title, hostId, hostName, maxMembers, count, members }] */
export function apiHallRooms() {
  return get(`${HALL_BASE}/rooms`)
}

/** 发起一个游戏邀请 */
export function apiHallCreateRoom(userId, game, title, maxMembers) {
  return post(`${HALL_BASE}/rooms`, { userId, game, title, maxMembers })
}

/** 加入邀请房间 */
export function apiHallJoinRoom(userId, roomId) {
  return post(`${HALL_BASE}/rooms/join`, { userId, roomId })
}

/** 离开邀请房间 */
export function apiHallLeaveRoom(userId, roomId) {
  return post(`${HALL_BASE}/rooms/leave`, { userId, roomId })
}

/** 房主关闭自己的邀请 */
export function apiHallCloseRoom(userId, roomId) {
  return post(`${HALL_BASE}/rooms/close`, { userId, roomId })
}
