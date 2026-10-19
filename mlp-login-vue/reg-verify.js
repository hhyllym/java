const fs = require('fs')

;(async () => {
  const out = []
  // 1) 验证码（证明后端 MVC 通了）
  try {
    const c = await fetch('http://localhost:5173/api/auth/captcha')
    const buf = await c.arrayBuffer()
    out.push('CAPTCHA status=' + c.status + ' type=' + (c.headers.get('content-type') || '') + ' bytes=' + buf.byteLength)
  } catch (e) {
    out.push('CAPTCHA ERROR: ' + e.message)
  }
  // 2) 注册（截图里同款账号：哇哇哇 / Www123456）
  try {
    const r = await fetch('http://localhost:5173/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: '哇哇哇', password: 'Www123456' })
    })
    const t = await r.text()
    out.push('REGISTER status=' + r.status + ' type=' + (r.headers.get('content-type') || ''))
    out.push('REGISTER body: ' + t)
  } catch (e) {
    out.push('REGISTER ERROR: ' + e.message)
  }
  fs.writeFileSync(__dirname + '/reg-verify.txt', out.join('\n'), 'utf8')
  console.log('done')
})()
