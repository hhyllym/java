const BASE = 'http://localhost:8081'
const out = []

async function api(path, opts = {}) {
  const r = await fetch(BASE + path, {
    method: opts.method || 'GET',
    headers: { 'Content-Type': 'application/json' },
    body: opts.body ? JSON.stringify(opts.body) : undefined
  })
  const t = await r.text()
  try { return JSON.parse(t) } catch (e) { return { raw: t.slice(200) } }
}

async function main() {
  // 先给哇哇哇发 200 积分（模拟做任务），再送 50 给喂喂喂
  await api('/api/account/update', { method: 'POST', body: { userId: 1 } })
  // 用任务领奖Tamper不行，直接连库太越界；改用连领多个任务：bump 全部类型
  for (const t of ['login', 'msg', 'game', 'view', 'checkin']) {
    await api('/api/task/bump', { method: 'POST', body: { userId: 1, type: t, n: 5 } })
  }
  let r = await api('/api/task/today?userId=1')
  for (const task of (r.data || [])) {
    if (task.done && !task.claimed) {
      await api('/api/task/claim', { method: 'POST', body: { userId: 1, taskId: task.id } })
    }
  }
  r = await api('/api/point/balance?userId=1')
  out.push('哇哇哇领完全部任务后积分=' + r.data)

  r = await api('/api/point/gift', { method: 'POST', body: { fromId: 1, toId: 2 } })
  out.push('送积分=' + JSON.stringify(r))
  r = await api('/api/point/balance?userId=1')
  out.push('哇哇哇余额=' + r.data)
  r = await api('/api/point/balance?userId=2')
  out.push('喂喂喂余额=' + r.data)
  r = await api('/api/point/logs?userId=2')
  out.push('喂喂喂流水最新2条=' + JSON.stringify((r.data || []).slice(0, 2).map(l => l.amount + ':' + l.remark)))

  // 删好友再加回：验证数据保留（聊天记录 + since 日期）
  r = await api('/api/chat/history?meId=1&friendId=2')
  const msgCountBefore = (r.data || []).length
  r = await api('/api/friend/delete', { method: 'POST', body: { meId: 1, friendId: 2 } })
  out.push('删好友=' + JSON.stringify(r))
  r = await api('/api/friend/apply', { method: 'POST', body: { fromId: 1, toId: 2, msg: '我们和好吧！' } })
  out.push('再加回(申请)=' + JSON.stringify(r))
  r = await api('/api/friend/applies', { method: 'POST', body: { meId: 2 } })
  const applyId = (r.data || [])[0] && (r.data || [])[0].id
  r = await api('/api/friend/handle', { method: 'POST', body: { applyId, meId: 2, accept: true } })
  out.push('同意=' + JSON.stringify(r))
  r = await api('/api/friend/list', { method: 'POST', body: { meId: 1 } })
  out.push('好友列表=' + JSON.stringify((r.data || []).map(f => f.name + ' since=' + f.since + ' rel=' + f.relationType)))
  r = await api('/api/chat/history?meId=1&friendId=2')
  out.push('聊天记录：删前=' + msgCountBefore + ' 条，加回后=' + (r.data || []).length + ' 条')

  require('fs').writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/ext-test.txt', out.join('\n'), 'utf8')
  console.log('DONE')
}

main().catch(e => {
  require('fs').writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/ext-test.txt', out.join('\n') + '\nERROR: ' + e.message, 'utf8')
  console.log('ERROR', e.message)
})
