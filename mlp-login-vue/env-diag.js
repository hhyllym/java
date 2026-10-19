const net = require('net')
const out = []

function checkPort(port) {
  return new Promise((resolve) => {
    const s = net.connect({ host: '127.0.0.1', port })
    s.setTimeout(1500)
    s.on('connect', () => { s.destroy(); resolve(true) })
    s.on('error', () => resolve(false))
    s.on('timeout', () => { s.destroy(); resolve(false) })
  })
}

;(async () => {
  out.push('3306(MySQL)=' + (await checkPort(3306) ? 'UP' : 'DOWN'))
  out.push('8081(后端)=' + (await checkPort(8081) ? 'UP' : 'DOWN'))
  out.push('5173(前端)=' + (await checkPort(5173) ? 'UP' : 'DOWN'))
  require('fs').writeFileSync(__dirname + '/env-diag.txt', out.join('\n'), 'utf8')
  console.log('done')
})()
