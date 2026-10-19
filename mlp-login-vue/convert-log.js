const fs = require('fs')
const src = 'C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/backend-run.log'
let t = fs.readFileSync(src, 'utf16le')
try { JSON.parse(t) } catch (e) {}
fs.writeFileSync('C:/Users/18017/Desktop/1java-main/1java-main/mlp-login-vue/backend-run-utf8.txt', t, 'utf8')
const lines = t.split(/\r?\n/).filter(l => l.trim())
console.log('total lines:', lines.length)
console.log('--- last 25 ---')
console.log(lines.slice(-25).join('\n'))
