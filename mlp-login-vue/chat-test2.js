/* 聊天新功能端到端测试：图片消息 / 撤回 / 通话记录 / 会话摘要 */
const BASE = 'http://localhost:8081/api/chat'
const out = []
const log = (s) => { out.push(s); console.log(s) }

async function api(path, body) {
  const opt = body
    ? { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
    : {}
  const res = await fetch(BASE + path, opt)
  return res.json()
}

async function main() {
  // 0. 确保两个账号是好友
  const f = await fetch('http://localhost:8081/api/friend/search', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ meId: 1, keyword: '喂喂喂' })
  })
  const fs = await f.json()
  log('[0] 好友搜索: ' + JSON.stringify(fs.data ? fs.data.map(u => u.name) : fs.msg))

  // 1. 文本消息（带 msgType）
  let r = await api('/send', { fromId: 1, toId: 2, content: '聊天改造测试文本', msgType: 'text' })
  log('[1] 文本发送: ' + JSON.stringify(r.data || r.msg))

  // 2. 图片消息（1x1 红色 base64 PNG）
  const img = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
  r = await api('/send', { fromId: 1, toId: 2, content: img, msgType: 'image' })
  const imgId = r.data && r.data.id
  log('[2] 图片发送: ' + JSON.stringify(r.data || r.msg))

  // 3. 图片消息不允许空内容 / 错误类型
  r = await api('/send', { fromId: 1, toId: 2, content: '', msgType: 'image' })
  log('[3] 空图片拦截: ' + r.code + ' ' + (r.msg || ''))
  r = await api('/send', { fromId: 1, toId: 2, content: 'x', msgType: 'sticker' })
  log('[4] 非法类型拦截: ' + r.code + ' ' + (r.msg || ''))

  // 5. 撤回：只能撤自己的
  r = await api('/recall', { meId: 2, msgId: imgId })
  log('[5] 撤别人的消息(应失败): ' + r.code + ' ' + (r.msg || ''))
  r = await api('/recall', { meId: 1, msgId: imgId })
  log('[6] 撤回自己的图片: ' + r.code + ' ' + (r.msg || ''))
  r = await api('/recall', { meId: 1, msgId: imgId })
  log('[7] 重复撤回(应失败): ' + r.code + ' ' + (r.msg || ''))

  // 8. 通话记录：接通 83 秒
  r = await api('/call', { fromId: 1, toId: 2, duration: 83, answered: true })
  log('[8] 通话记录83s: ' + r.code + ' ' + (r.msg || ''))
  // 9. 未接来电 0 秒
  r = await api('/call', { fromId: 1, toId: 2, duration: 0, answered: false })
  log('[9] 未接来电: ' + r.code + ' ' + (r.msg || ''))

  // 10. 历史记录：带 type / recalled / extra
  r = await api('/history?meId=2&friendId=1')
  const rows = r.data || []
  const imgRow = rows.find(x => x.id === imgId)
  const callRows = rows.filter(x => x.type === 'call')
  log('[10] 历史总数: ' + rows.length)
  log('[10a] 图片行: type=' + (imgRow && imgRow.type) + ' recalled=' + (imgRow && imgRow.recalled))
  log('[10b] 通话行: ' + JSON.stringify(callRows.map(x => ({ extra: x.extra, type: x.type }))))

  // 11. 会话列表摘要
  r = await api('/conversations?meId=2')
  const me = (r.data || []).find(c => c.id === 1)
  log('[11] 会话摘要: ' + (me ? me.lastContent : 'N/A'))

  // 12. 历史里 time 字段存在（时间分隔线用）
  log('[12] 末条时间: ' + (rows.length ? rows[rows.length - 1].time : 'N/A'))
}

main().then(() => {
  require('fs').writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/chat-test2.txt', out.join('\n'), 'utf8')
}).catch(e => {
  require('fs').writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/chat-test2.txt', out.join('\n') + '\nERROR: ' + e.message, 'utf8')
})
