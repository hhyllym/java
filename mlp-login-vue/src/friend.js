/**
 * 好友接口封装（Spring Boot /api/friend）
 *   POST /api/friend/search  { meId, keyword }      按名字/ID 搜索用户
 *   POST /api/friend/apply   { fromId, toId, msg } 发申请（附言可自定义）
 *   POST /api/friend/applies { meId }              收到的申请
 *   POST /api/friend/handle  { applyId, meId, accept }  同意(true)/拒绝(false)
 *   POST /api/friend/list    { meId }              好友列表
 *   POST /api/friend/delete  { meId, friendId }    删除好友
 *   POST /api/friend/remark  { meId, friendId, remark }  设置备注
 *   POST /api/friend/sent    { meId }              我发出的申请
 *   POST /api/friend/cancel  { applyId, meId }     撤回申请
 *   POST /api/friend/profile { meId, friendId }    好友资料卡
 *   POST /api/friend/suggest { meId }              可能认识的人
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

/** 搜索用户，data: [{ id, name, avatar, relation: none|pending-out|pending-in|friend }] */
export function apiSearchUsers(meId, keyword) {
  return post(`${FRIEND_BASE}/search`, { meId, keyword })
}

/** 发好友申请（msg 为附言，最长 60 字） */
export function apiApply(fromId, toId, msg) {
  return post(`${FRIEND_BASE}/apply`, { fromId, toId, msg })
}

/** 收到的申请，data: [{ id, userId, name, avatar, applyMsg, time }] */
export function apiApplies(meId) {
  return post(`${FRIEND_BASE}/applies`, { meId })
}

/** 同意/拒绝申请 */
export function apiHandle(applyId, meId, accept) {
  return post(`${FRIEND_BASE}/handle`, { applyId, meId, accept })
}

/** 好友列表，data: [{ id, name, avatar, since, relationType, remark }] */
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

/** 我发出的申请，data: [{ id, userId, name, avatar, applyMsg, time, state: waiting|rejected }] */
export function apiSentApplies(meId) {
  return post(`${FRIEND_BASE}/sent`, { meId })
}

/** 撤回自己发出的、还没处理的申请 */
export function apiCancelApply(applyId, meId) {
  return post(`${FRIEND_BASE}/cancel`, { applyId, meId })
}

/**
 * 好友资料卡：基本信息、认识天数、聊天条数、互动徽章、共同好友、游戏战绩
 * data: { id, name, avatar, points, joined, since, days, relationType, remark,
 *         msgCount, badges[], mutualCount, mutualNames[], game{...} }
 */
export function apiFriendProfile(meId, friendId) {
  return post(`${FRIEND_BASE}/profile`, { meId, friendId })
}

/** 可能认识的人，data: [{ id, name, avatar, mutual, reason, relation }] */
export function apiFriendSuggest(meId) {
  return post(`${FRIEND_BASE}/suggest`, { meId })
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
