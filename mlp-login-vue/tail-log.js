const fs = require('fs')
const src = __dirname + '/backend-run.log'
let text
try {
  text = fs.readFileSync(src, 'utf16le')
} catch (e) {
  text = 'READ-ERROR: ' + e.message
}
// 去掉可能的 BOM/控制字符，取最后 50 行
const lines = text.split(/\r?\n/).filter((l) => l.trim() !== '')
const tail = lines.slice(-50).join('\n')
fs.writeFileSync(__dirname + '/backend-run-tail.txt', tail, 'utf8')
console.log('lines=' + lines.length)
