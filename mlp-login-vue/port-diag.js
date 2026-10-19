const fs = require('fs')
const out = []
const BASE = 'http://localhost:8081'

async function probe(url, method, body) {
  try {
    const opt = { method }
    if (body) {
      opt.headers = { 'Content-Type': 'application/json' }
      opt.body = JSON.stringify(body)
    }
    const r = await fetch(url, opt)
    const t = await r.text()
    out.push(url + ' [' + method + '] => status=' + r.status +
      ' type=' + (r.headers.get('content-type') || '') +
      ' len=' + t.length +
      ' body(300): ' + t.slice(0, 300).replace(/\s+/g, ' '))
  } catch (e) {
    out.push(url + ' [' + method + '] => ERROR: ' + e.message)
  }
}

;(async () => {
  await probe(BASE + '/api/auth/captcha', 'GET')
  await probe(BASE + '/', 'GET')
  await probe(BASE + '/api/auth/register', 'POST', { name: 'probe', password: 'abc12345' })
  fs.writeFileSync(__dirname + '/port-diag.txt', out.join('\n'), 'utf8')
  console.log('done')
})()
