/**
 * 好友聊天接口封装（Spring Boot /api/chat）
 *   GET  /api/chat/conversations?meId=    会话列表（最后一条消息 + 未读数）
 *   GET  /api/chat/history?meId=&friendId= 聊天记录（正序）
 *   POST /api/chat/send  { fromId, toId, content, msgType }  发消息（text/image）
 *   POST /api/chat/recall { meId, msgId }  撤回自己的消息（5 分钟内）
 *   POST /api/chat/call  { fromId, toId, duration, answered }  保存通话记录
 *   POST /api/chat/read  { meId, friendId }  标记已读
 *   GET  /api/chat/unread?meId=              未读总数
 */

const CHAT_BASE = '/api/chat'

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

/** 会话列表，data: [{ id, name, lastContent, lastTime, lastMine, unread }] */
export function apiConversations(meId) {
  return get(`${CHAT_BASE}/conversations?meId=${encodeURIComponent(meId)}`)
}

/** 聊天记录，data: [{ id, mine, content, time }] */
export function apiHistory(meId, friendId) {
  return get(`${CHAT_BASE}/history?meId=${encodeURIComponent(meId)}&friendId=${encodeURIComponent(friendId)}`)
}

/** 发消息，msgType: 'text' | 'image'，data: { id, mine, type, recalled, extra, content, time } */
export function apiSend(fromId, toId, content, msgType = 'text') {
  return post(`${CHAT_BASE}/send`, { fromId, toId, content, msgType })
}

/** 发图片消息，dataUrl 为 base64（data:image/...） */
export function apiSendImage(fromId, toId, dataUrl) {
  return post(`${CHAT_BASE}/send`, { fromId, toId, content: dataUrl, msgType: 'image' })
}

/** 撤回自己的消息（5 分钟内） */
export function apiRecall(meId, msgId) {
  return post(`${CHAT_BASE}/recall`, { meId, msgId })
}

/** 保存通话记录，duration 为秒数，answered=false 表示未接 */
export function apiCallLog(fromId, toId, duration, answered) {
  return post(`${CHAT_BASE}/call`, { fromId, toId, duration, answered })
}

/** 标记对方发来的消息为已读 */
export function apiMarkRead(meId, friendId) {
  return post(`${CHAT_BASE}/read`, { meId, friendId })
}

/** 未读消息总数（导航栏红点） */
export function apiChatUnread(meId) {
  return get(`${CHAT_BASE}/unread?meId=${encodeURIComponent(meId)}`)
}
