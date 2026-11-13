/**
 * 全局消息 / 好友申请状态中心（响应式单例）
 * ---------------------------------------------------------------
 * 原来未读红点散落在 HomeView（消息）、HomeView（好友聊天）、FriendsView（好友申请）
 * 各自轮询，导航栏还把 apiUnread 返回的对象当数字用 → 消息红点永不亮。
 * 这里统一拉一份：私信未读(pm) + 系统通知未读(notify) + 好友聊天未读(chat) + 收到的好友申请(applies)，
 * 由 App.vue 挂载时每 10 秒轮询一次，导航栏、全局弹窗都直接读这个 store，实时同步。
 *
 * 后端接口返回结构（见 message.js / chat.js / friend.js）：
 *   GET /api/message/unread?userId=  -> { code, data: { pm, notify } }   // data 是对象！
 *   GET /api/chat/unread?meId=       -> { code, data: <number> }          // data 是数字
 *   POST /api/friend/applies         -> { code, data: [{ id, userId, name, avatar, applyMsg, time }] }
 */

import { reactive } from 'vue'
import { getSession } from './auth'
import { apiUnread } from './message'
import { apiChatUnread } from './chat'
import { apiApplies } from './friend'
import { apiHallMessages } from './hall'

/** 大厅「已读到的最后一条消息 id」，存在本地，进大厅看过就不会再亮红点 */
export const HALL_SEEN_KEY = 'mlp_hall_seen'

/** 全局唯一的响应式状态，所有页面/组件 import 到的是同一个对象 */
export const notify = reactive({
  pmUnread: 0,
  notifyUnread: 0,
  chatUnread: 0,
  /** 收到的好友申请列表（导航栏红点 + 弹窗用） */
  friendApplies: [],
  /** 大厅最新一条消息的 id（导航栏「大厅」红点用） */
  hallLatest: 0,
  /** 私信 + 系统通知 的未读总数（导航栏「消息」红点用） */
  get messageTotal() {
    return (this.pmUnread || 0) + (this.notifyUnread || 0)
  },
  /** 是否有未读消息（私信或通知） */
  get hasMessage() {
    return this.messageTotal > 0
  },
  /** 是否有收到的好友申请 */
  get hasApply() {
    return (this.friendApplies || []).length > 0
  },
  /** 好友入口红点：聊天未读 或 收到申请 任一来一点亮 */
  get friendTotal() {
    return (this.chatUnread || 0) + (this.friendApplies || []).length
  },
  get hasFriend() {
    return this.friendTotal > 0
  },
  /** 大厅红点：本地已读位置之后还有多少条新消息 */
  get hallUnread() {
    let seen = 0
    try { seen = Number(localStorage.getItem(HALL_SEEN_KEY) || 0) } catch (e) { seen = 0 }
    const n = (this.hallLatest || 0) - seen
    return n > 0 ? n : 0
  }
})

let timer = null

/** 拉一次最新未读 / 申请，写入 store。登录态缺失时直接跳过（不浪费请求）。 */
export async function refreshNotify() {
  const s = getSession()
  if (!s || !s.id) return
  const uid = s.id
  try {
    const r = await apiUnread(uid)
    if (r && r.code === 200 && r.data) {
      notify.pmUnread = r.data.pm || 0
      notify.notifyUnread = r.data.notify || 0
    }
  } catch (e) { /* 网络抖动忽略，下次轮询再试 */ }
  try {
    const c = await apiChatUnread(uid)
    if (c && c.code === 200) notify.chatUnread = c.data || 0
  } catch (e) { /* ignore */ }
  try {
    const a = await apiApplies(uid)
    if (a && a.code === 200) notify.friendApplies = a.data || []
  } catch (e) { /* ignore */ }
  try {
    const h = await apiHallMessages(1)
    if (h && h.code === 200 && Array.isArray(h.data) && h.data.length) {
      notify.hallLatest = h.data[h.data.length - 1].id || 0
    }
  } catch (e) { /* ignore */ }
}

/** App 挂载时调用：立刻拉一次，然后每 interval 毫秒轮询 */
export function startNotifyPolling(interval = 10000) {
  refreshNotify()
  if (timer) clearInterval(timer)
  timer = setInterval(refreshNotify, interval)
}

/** App 卸载时调用：清掉轮询，避免泄漏 */
export function stopNotifyPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}
