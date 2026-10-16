/**
 * 「我的账号」接口封装（Spring Boot）
 *   GET /api/user/profile?id=1   → { id, name, createTime, days, pony }
 *
 * pony = 图鉴里跟账号同名的小马（可能为 null，表示图鉴里还没有同名的你）
 * 返回格式跟 auth.js / home.js 一样：{ code, msg, data }
 */

const USER_BASE = '/api/user'

async function get(url) {
  try {
    const res = await fetch(url)
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
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

/** 账号资料：注册时间 / 加入天数 / 同名图鉴小马 */
export function apiProfile(id) {
  return get(USER_BASE + '/profile?id=' + encodeURIComponent(id))
}
