/**
 * 用户与会话管理
 * 注册 / 登录走 Spring Boot 后端（MyBatis-Plus + MySQL）
 * 会话仍保存在浏览器本地（localStorage / sessionStorage）
 */

const SESSION_KEY = 'mlp_session'
const API_BASE = '/api/auth'

export function getSession() {
  const s = localStorage.getItem(SESSION_KEY) || sessionStorage.getItem(SESSION_KEY)
  if (!s) return null
  try { return JSON.parse(s) } catch (e) { return null }
}

export function setSession(session, remember) {
  if (remember) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    sessionStorage.removeItem(SESSION_KEY)
  } else {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
    localStorage.removeItem(SESSION_KEY)
  }
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
  sessionStorage.removeItem(SESSION_KEY)
}

/** 记住上次登录的账号名（切换账号后回登录页可一键预填） */
const LAST_ACCOUNT_KEY = 'mlp_last_account'

export function rememberAccount(name) {
  if (name) localStorage.setItem(LAST_ACCOUNT_KEY, name)
}

export function getRememberedAccount() {
  return localStorage.getItem(LAST_ACCOUNT_KEY) || ''
}

export function forgetAccount() {
  localStorage.removeItem(LAST_ACCOUNT_KEY)
}

/** 统一请求封装，返回 { code, msg, data } */
async function post(path, body) {
  try {
    const res = await fetch(API_BASE + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      // 500 且非 JSON，多半是 Vite 代理连不上 8081（后端没启动）返回的假 500；
      // 后端真抛异常时通常也有监听、日志有堆栈
      if (res.status === 500 || res.status === 502 || res.status === 504) {
        return { code: -1, msg: '后端没有响应（多半是 Spring Boot 没启动），请先把后端跑起来再试', data: null }
      }
      return {
        code: -1,
        msg: `后端返回了非 JSON 内容（HTTP ${res.status}），去后端窗口看报错日志`,
        data: null
      }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

/** 注册：{ name, password, captcha } —— captcha 是一次性的，失败后要换新的 */
export function apiRegister(payload) {
  return post('/register', payload)
}

/** 登录：{ name, password } —— 验证码已移到注册页 */
export function apiLogin(payload) {
  return post('/login', payload)
}

/** 验证码图片地址（加时间戳防止浏览器缓存） */
export function captchaUrl() {
  return API_BASE + '/captcha?t=' + Date.now()
}
