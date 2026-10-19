// 把 PowerShell 重定向产生的 UTF-16 日志转成 UTF-8 方便阅读
const fs = require('fs');
const src = process.argv[2];
const dst = src.replace(/\.txt$/, '-utf8.txt');
const buf = fs.readFileSync(src);
// 去掉 BOM，统一按 UTF-16LE 解码（PowerShell 5.1 重定向默认 UTF-16LE）
let text;
if (buf[0] === 0xFF && buf[1] === 0xFE) {
  text = buf.toString('utf16le').slice(1);
} else {
  text = buf.toString('utf8');
}
fs.writeFileSync(dst, text, 'utf8');
console.log('converted ->', dst, '| lines:', text.split(/\r?\n/).length);
