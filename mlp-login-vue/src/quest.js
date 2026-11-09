/**
 * 每日任务进度上报（统一入口）
 *
 * 各页面在「操作成功」后调 bump(type) 即可，不用关心会话和网络错误：
 * - 自动取当前登录用户，未登录直接跳过；
 * - fire-and-forget，失败静默，绝不影响原本的业务流程；
 * - 返回 Promise，需要等进度刷新时（如个人主页）可以 .then() 串起来。
 *
 * type 与后端 ShopDefs.dailyTasks() 里的 type 一一对应：
 * login / msg / game / view / checkin / lesson / quiz / friend / pet / room / gift / shop
 * bonus（全勤宝箱）不接受上报，由后端领奖时自动解锁。
 */
import { getSession } from './auth'
import { apiTaskBump } from './task'

export function bump(type, n = 1) {
  try {
    const s = getSession()
    if (!s || !s.id || !type) return Promise.resolve()
    return apiTaskBump(s.id, type, n).catch(() => {})
  } catch (e) {
    return Promise.resolve()
  }
}
