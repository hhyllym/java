const fs = require('fs')
const BASE = 'C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/'

async function probe() {
  try {
    const r = await fetch('http://localhost:8081/api/shop/items')
    return { ok: r.status === 200 }
  } catch (e) {
    return { ok: false }
  }
}

async function main() {
  for (let i = 0; i < 60; i++) {
    const p = await probe()
    if (p.ok) {
      fs.writeFileSync(BASE + 'wait-result.txt', 'UP', 'utf8')
      console.log('UP')
      return
    }
    await new Promise(r => setTimeout(r, 2000))
  }
  fs.writeFileSync(BASE + 'wait-result.txt', 'TIMEOUT', 'utf8')
  console.log('TIMEOUT')
}
main()
