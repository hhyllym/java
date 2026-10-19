/**
 * 账号与关系 API：资料（头像/改名）、绑定关系
 * 注意路径是 /api/account（/api/user 被「我的账号」旧接口占着）
 */

async function get(path) {
  try {
    const res = await fetch('/api/account' + path)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function post(path, body) {
  try {
    const res = await fetch('/api/account' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 账号资料：id / name / avatar / points */
export function apiAccountProfile(userId) {
  return get('/profile?userId=' + userId)
}

/** 更新资料：{ name?, avatar? } */
export function apiAccountUpdate(userId, patch) {
  return post('/update', Object.assign({ userId }, patch))
}

/** 绑定关系：bestie / bro / master / partner / rival */
export function apiAccountBind(userId, friendId, type) {
  return post('/bind', { userId, friendId, type })
}

/** 解除绑定 */
export function apiAccountUnbind(userId, friendId) {
  return post('/unbind', { userId, friendId })
}
