/* 好友备注功能端到端测试：设置/列表返回/修改/清除/独立性 */
const fs = require('fs')
const OUT = 'C:\\Users\\18017\\Desktop\\1java-main\\1java-main\\mlp-login-vue\\remark-test.txt'

const BASE = 'http://localhost:8081'
const out = []

async function api(path, body) {
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body || {})
  })
  return res.json()
}

async function main() {
  const ME = 1, FRIEND = 2

  // 1. 确保两人是好友（可能之前删过，先看列表）
  let r = await api('/api/friend/list', { meId: ME })
  let friends = r.code === 200 ? (r.data || []) : []
  out.push('[1] 账号1好友列表: ' + friends.map(f => `${f.name}(remark=${f.remark || '-'})`).join(', ') || '(空)')

  if (!friends.some(f => f.id === FRIEND)) {
    // 走申请-同意流程
    await api('/api/friend/apply', { fromId: ME, toId: FRIEND, msg: '备注测试' })
    const applies = (await api('/api/friend/applies', { meId: FRIEND })).data || []
    const a = applies.find(x => x.userId === ME)
    if (a) await api('/api/friend/handle', { applyId: a.id, meId: FRIEND, accept: true })
    out.push('[1b] 重新加好友完成')
  }

  // 2. 设置备注（带特殊符号）
  r = await api('/api/friend/remark', { meId: ME, friendId: FRIEND, remark: '小仙女·夜子女王' })
  out.push('[2] 设置备注=' + JSON.stringify(r))

  // 3. 列表应带回备注
  r = await api('/api/friend/list', { meId: ME })
  friends = r.data || []
  const f2 = friends.find(f => f.id === FRIEND)
  out.push('[3] 账号1看到: name=' + (f2 && f2.name) + ' remark=' + (f2 && f2.remark))

  // 4. 账号2的列表不应看到账号1给的备注（独立性）
  r = await api('/api/friend/list', { meId: FRIEND })
  const f1 = (r.data || []).find(f => f.id === ME)
  out.push('[4] 账号2看到账号1: remark=' + (f1 && (f1.remark || '(空，正确)')))
  // 给账号2也设一个备注
  r = await api('/api/friend/remark', { meId: FRIEND, friendId: ME, remark: '铲屎官大哥' })
  out.push('[4b] 账号2设备注=' + r.code)

  // 5. 修改备注
  r = await api('/api/friend/remark', { meId: ME, friendId: FRIEND, remark: '女王大人' })
  r = await api('/api/friend/list', { meId: ME })
  const f2b = (r.data || []).find(f => f.id === FRIEND)
  out.push('[5] 改后账号1看到: remark=' + (f2b && f2b.remark))

  // 6. 超长备注截断到 32 字（后端限制）
  r = await api('/api/friend/remark', { meId: ME, friendId: FRIEND, remark: '超长备注测试'.repeat(10) })
  r = await api('/api/friend/list', { meId: ME })
  const f2c = (r.data || []).find(f => f.id === FRIEND)
  out.push('[6] 超长截断后长度=' + (f2c && (f2c.remark || '').length))

  // 7. 清除备注（空串）
  r = await api('/api/friend/remark', { meId: ME, friendId: FRIEND, remark: '' })
  out.push('[7] 清除备注=' + JSON.stringify(r))
  r = await api('/api/friend/list', { meId: ME })
  const f2d = (r.data || []).find(f => f.id === FRIEND)
  out.push('[7b] 清除后: remark=' + (f2d && (f2d.remark || '(空，正确)')))

  // 8. 数据库确认行已删除
  out.push('[8] DB检查由外部SQL完成')

  // 恢复一个好看的备注方便页面上看效果
  await api('/api/friend/remark', { meId: ME, friendId: FRIEND, remark: '小仙女·夜子女王' })

  fs.writeFileSync(OUT, out.join('\n'), 'utf8')
}

main().catch(e => {
  fs.writeFileSync(OUT, 'FATAL: ' + (e && e.stack || e), 'utf8')
})
