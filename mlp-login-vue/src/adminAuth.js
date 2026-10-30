/**
 * 后台管理系统 · 管理员会话与接口封装
 * 登录成功后在 localStorage 存 { name, token, ts }，
 * 之后每个后台接口都带 X-Admin-Token 请求头（后端 AdminAuth 校验）。
 */

const ADMIN_SESSION_KEY = 'mlp_admin_session'
const API_BASE = '/api/admin'

export function getAdminSession() {
  const s = localStorage.getItem(ADMIN_SESSION_KEY)
  if (!s) return null
  try {
    return JSON.parse(s)
  } catch (e) {
    return null
  }
}

export function setAdminSession(session) {
  localStorage.setItem(ADMIN_SESSION_KEY, JSON.stringify(session))
}

export function clearAdminSession() {
  localStorage.removeItem(ADMIN_SESSION_KEY)
}

function adminToken() {
  const s = getAdminSession()
  return s && s.token ? s.token : ''
}

function adminHeaders() {
  return {
    'Content-Type': 'application/json',
    'X-Admin-Token': adminToken()
  }
}

/** 统一请求：返回 { code, msg, data }（和后端 Result 对齐） */
async function request(method, path, body) {
  const opts = { method, headers: adminHeaders() }
  if (body !== undefined) opts.body = JSON.stringify(body)
  try {
    const res = await fetch(API_BASE + path, opts)
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      // 500 且非 JSON，多半是 Vite 代理连不上 8081（后端没启动）返回的假 500
      if (res.status === 500 || res.status === 502 || res.status === 504) {
        return { code: -1, msg: '后端没有响应（多半是 Spring Boot 没启动），请先把后端跑起来再试', data: null }
      }
      return { code: -1, msg: `后端返回了非 JSON（HTTP ${res.status}），去后端窗口看报错日志`, data: null }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

// ---------- 登录 / 当前管理员 ----------
export const apiAdminLogin = (username, password) => request('POST', '/login', { username, password })
export const apiAdminMe = () => request('GET', '/me')

// ---------- 小马图鉴 ----------
export const apiAdminPonyList = (keyword = '', page = 1, size = 12) =>
  request('GET', `/pony/list?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
export const apiAdminPonyCreate = (payload) => request('POST', '/pony', payload)
export const apiAdminPonyUpdate = (payload) => request('PUT', '/pony', payload)
export const apiAdminPonyDelete = (id) => request('DELETE', `/pony/${id}`)

// ---------- 分类 ----------
export const apiAdminCategoryList = () => request('GET', '/category/list')
export const apiAdminCategoryTree = () => request('GET', '/category/tree')
export const apiAdminCategoryCreate = (payload) => request('POST', '/category', payload)
export const apiAdminCategoryUpdate = (payload) => request('PUT', '/category', payload)
export const apiAdminCategoryDelete = (id) => request('DELETE', `/category/${id}`)

// ---------- 标签 ----------
export const apiAdminTagList = () => request('GET', '/tag/list')
export const apiAdminTagAdd = (ponyId, tag) => request('POST', '/tag', { ponyId, tag })
export const apiAdminTagRename = (oldTag, newTag) => request('PUT', '/tag/rename', { oldTag, newTag })
export const apiAdminTagDelete = (tag) => request('DELETE', `/tag/${encodeURIComponent(tag)}`)

// ---------- 用户 ----------
export const apiAdminUserList = (keyword = '', page = 1, size = 12) =>
  request('GET', `/user/list?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
export const apiAdminUserDelete = (id) => request('DELETE', `/user/${id}`)
export const apiAdminUserPoints = (id, points) => request('PUT', `/user/${id}/points`, { points })
export const apiAdminUserPermission = (id, permission) => request('PUT', `/user/${id}/permission`, { permission })

// ---------- 消息 ----------
export const apiAdminMessageList = (type = '', keyword = '', page = 1, size = 12) =>
  request('GET', `/message/list?type=${encodeURIComponent(type)}&keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
export const apiAdminMessageDelete = (id) => request('DELETE', `/message/${id}`)
