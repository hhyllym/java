/**
 * 🎲 糖霜镇大富翁「四人联机」房间 API
 * 服务器是唯一裁判，前端只发动作 + 轮询状态；走 Vite 的 /api 代理，不需要 WebSocket。
 */

const BASE = '/api/monopoly'

async function req(path, body, method = 'POST') {
  try {
    const opt = { method, cache: 'no-store' }
    if (body) {
      opt.headers = { 'Content-Type': 'application/json' }
      opt.body = JSON.stringify(body)
    }
    const res = await fetch(BASE + path, opt)
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      if (res.status === 500 || res.status === 502 || res.status === 504) {
        return { code: -1, msg: '后端没响应，先确认 Spring Boot（8081）已启动', data: null }
      }
      return { code: -1, msg: `后端返回了非 JSON 内容（HTTP ${res.status}）`, data: null }
    }
  } catch (e) {
    return { code: -1, msg: '连不上后端，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

/** 主机在局域网里的 IP，用来拼「发给同学的邀请网址」 */
export function mpLan() {
  return req('/lan', null, 'GET')
}

/** 创建房间（创建者自动坐 0 号位，即房主） */
export function mpCreate(nickname, userId, mode) {
  return req('/create', { nickname, userId, mode })
}

/** 加入房间；同名重进会回到原座位，不会多占坑 */
export function mpJoin(roomId, nickname, userId) {
  return req('/join', { roomId: String(roomId).toUpperCase(), nickname, userId })
}

/** 房主开始游戏；fillAi=true 时不足 4 人由电脑补位 */
export function mpStart(roomId, seatId, token, mode, fillAi) {
  return req('/start', { roomId, seatId, token, mode, fillAi })
}

/** 轮询房间状态（带时间戳防缓存） */
export function mpState(roomId, seatId, token) {
  const q = `roomId=${encodeURIComponent(roomId)}&seatId=${seatId}&token=${encodeURIComponent(token)}&t=${Date.now()}`
  return req('/state?' + q, null, 'GET')
}

/** 动作：type = roll | buy，buy 时 value 表示买不买 */
export function mpAction(roomId, seatId, token, type, value) {
  return req('/action', { roomId, seatId, token, type, value: !!value })
}

/** 离开房间 */
export function mpLeave(roomId, seatId, token) {
  return req('/leave', { roomId, seatId, token })
}
