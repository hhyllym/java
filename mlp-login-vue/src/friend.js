/**
 * 好友接口封装（Spring Boot /api/friend）
 *   POST /api/friend/search  { meId, keyword }      按名字/ID 搜索用户
 *   POST /api/friend/apply   { fromId, toId, msg } 发申请
 *   POST /api/friend/applies { meId }              收到的申请
 *   POST /api/friend/handle  { applyId, meId, accept }  同意(true)/拒绝(false)
 *   POST /api/friend/list    { meId }              好友列表
 *   POST /api/friend/delete  { meId, friendId }    删除好友
 * 返回格式与其它模块一致：{ code, msg, data }
 */

const FRIEND_BASE = '/api/friend'

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
      return { code: -1, msg: `后端返回非 JSON（HTTP ${res.status}），去后端窗口看报错日志`, data: null }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

/** 搜索用户，data: [{ id, name, relation: none|pending-out|pending-in|friend }] */
export function apiSearchUsers(meId, keyword) {
  return post(`${FRIEND_BASE}/search`, { meId, keyword })
}

/** 发好友申请 */
export function apiApply(fromId, toId, msg) {
  return post(`${FRIEND_BASE}/apply`, { fromId, toId, msg })
}

/** 收到的申请，data: [{ id, userId, name, applyMsg, time }] */
export function apiApplies(meId) {
  return post(`${FRIEND_BASE}/applies`, { meId })
}

/** 同意/拒绝申请 */
export function apiHandle(applyId, meId, accept) {
  return post(`${FRIEND_BASE}/handle`, { applyId, meId, accept })
}

/** 好友列表，data: [{ id, name, since }] */
export function apiFriendList(meId) {
  return post(`${FRIEND_BASE}/list`, { meId })
}

/** 删除好友 */
export function apiDeleteFriend(meId, friendId) {
  return post(`${FRIEND_BASE}/delete`, { meId, friendId })
}

/** 设置好友备注（remark 传空串 = 清除备注） */
export function apiSetRemark(meId, friendId, remark) {
  return post(`${FRIEND_BASE}/remark`, { meId, friendId, remark })
}

/**
 * 互动徽章（GET）：按聊天条数 / 好友天数 / 绑定关系计算
 * data: [{ id, name, emoji, desc }]
 */
export async function apiFriendBadges(meId, friendId) {
  try {
    const res = await fetch(`/api/friend/badges?meId=${meId}&friendId=${friendId}`)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}
