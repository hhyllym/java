const BASE = 'http://localhost:8081'
const out = []

async function api(path, opts = {}) {
  const r = await fetch(BASE + path, {
    method: opts.method || 'GET',
    headers: { 'Content-Type': 'application/json' },
    body: opts.body ? JSON.stringify(opts.body) : undefined
  })
  const t = await r.text()
  try { return JSON.parse(t) } catch (e) { return { raw: t.slice(0, 200) } }
}

async function main() {
  // 1. 商店商品列表
  let r = await api('/api/shop/items')
  out.push('[1] 商品数=' + (r.data || []).length + ' ' + JSON.stringify((r.data || []).slice(0, 2).map(i => i.name)))

  // 2. 今日任务
  r = await api('/api/task/today?userId=1')
  out.push('[2] 任务数=' + (r.data || []).length + ' 第一个=' + JSON.stringify((r.data || [])[0] || {}))

  // 3. 上报进度 + 领奖
  r = await api('/api/task/bump', { method: 'POST', body: { userId: 1, type: 'msg', n: 3 } })
  out.push('[3] bump=' + JSON.stringify(r))
  r = await api('/api/task/claim', { method: 'POST', body: { userId: 1, taskId: 'daily_msg' } })
  out.push('[4] claim=' + JSON.stringify(r))

  // 4. 积分余额 + 流水 + 排行
  r = await api('/api/point/balance?userId=1')
  out.push('[5] 哇哇哇积分=' + r.data)
  r = await api('/api/point/logs?userId=1')
  out.push('[6] 流水数=' + (r.data || []).length + ' 最新=' + JSON.stringify((r.data || [])[0] || {}))
  r = await api('/api/point/rank')
  out.push('[7] 排行=' + JSON.stringify((r.data || []).slice(0, 3).map(p => p.name + ':' + p.points)))

  // 5. 个人资料：换头像 + 改特殊符号名字
  r = await api('/api/account/profile?userId=2')
  out.push('[8] 喂喂喂资料=' + JSON.stringify(r.data))
  r = await api('/api/account/update', { method: 'POST', body: { userId: 2, avatar: '🌟', name: '喂喂喂·夜之王女' } })
  out.push('[9] 改名=' + JSON.stringify(r))
  r = await api('/api/account/profile?userId=2')
  out.push('[10] 改名后=' + JSON.stringify(r.data))

  // 6. 绑定关系（双方送积分）
  r = await api('/api/account/bind', { method: 'POST', body: { userId: 1, friendId: 2, type: 'bestie' } })
  out.push('[11] 绑定=' + JSON.stringify(r))
  r = await api('/api/friend/list', { method: 'POST', body: { meId: 1 } })
  out.push('[12] 好友列表=' + JSON.stringify((r.data || []).map(f => f.name + ':' + f.relationType)))

  // 7. 互动徽章
  r = await api('/api/friend/badges?meId=1&friendId=2')
  out.push('[13] 互动徽章=' + JSON.stringify(r.data))

  // 8. 游戏成绩 + 对比
  r = await api('/api/game/score', { method: 'POST', body: { gameId: 'sports', userId: 1, score: 260 } })
  out.push('[14] 提交成绩=' + JSON.stringify(r))
  r = await api('/api/game/score', { method: 'POST', body: { gameId: 'sports', userId: 2, score: 180 } })
  r = await api('/api/game/vs?meId=1&friendId=2&gameId=sports')
  out.push('[15] 对战对比=' + JSON.stringify(r.data))
  r = await api('/api/game/rank?gameId=sports')
  out.push('[16] 运动会排行=' + JSON.stringify((r.data || []).map(g => g.name + ':' + g.score)))

  // 9. 宠物：领养 + 喂食 + 玩耍
  r = await api('/api/pet/mine?userId=1')
  out.push('[17] 宠物(领养前)=' + JSON.stringify(r.data))
  r = await api('/api/pet/adopt', { method: 'POST', body: { userId: 1, kind: 'dragon', name: '小火花' } })
  out.push('[18] 领养=' + JSON.stringify(r))
  r = await api('/api/shop/buy', { method: 'POST', body: { userId: 1, itemId: 'pet-food' } })
  out.push('[19] 买口粮=' + JSON.stringify(r))
  r = await api('/api/pet/feed', { method: 'POST', body: { userId: 1 } })
  out.push('[20] 喂食=' + JSON.stringify(r))
  r = await api('/api/pet/play', { method: 'POST', body: { userId: 1 } })
  out.push('[21] 玩耍=' + JSON.stringify(r))
  r = await api('/api/pet/mine?userId=1')
  out.push('[22] 宠物(现在)=' + JSON.stringify(r.data))
  r = await api('/api/pet/rename', { method: 'POST', body: { userId: 1, name: '小火龙·焰焰' } })
  out.push('[23] 宠物改名=' + JSON.stringify(r))

  // 10. 送好友积分
  r = await api('/api/point/gift', { method: 'POST', body: { fromId: 1, toId: 2 } })
  out.push('[24] 送积分=' + JSON.stringify(r))
  r = await api('/api/point/balance?userId=2')
  out.push('[25] 喂喂喂余额=' + r.data)

  // 11. 背包
  r = await api('/api/shop/bag?userId=1')
  out.push('[26] 背包=' + JSON.stringify((r.data || []).map(b => b.name + 'x' + b.count)))

  require('fs').writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/ext-test.txt', out.join('\n'), 'utf8')
  console.log('DONE')
}

main().catch(e => {
  require('fs').writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/ext-test.txt', out.join('\n') + '\nERROR: ' + e.message, 'utf8')
  console.log('ERROR', e.message)
})
