#!/usr/bin/env node
/**
 * 批处理语法体检 —— 专门抓「此时不应有 ...。」这类 cmd 括号语法错误
 *
 * 背景：
 *   cmd.exe 在解析 if (...) / for (...) 代码块时，会把块内 echo 行里的
 *   裸括号当成块边界。一旦 echo 文本里出现成对的 ( ) 且右边括号先闭合了块，
 *   cmd 就会把后面的内容当成命令，报「此时不应有 xxx。」并且整个脚本崩掉。
 *
 *   实例：启动后端.bat 里这一行曾经把后端直接卡死：
 *     echo [2.9/4] Fast path: run compiled classes directly (skip Maven) ...
 *     —— 行尾的 ... 被 cmd 当成命令，于是报「此时不应有 ...。」
 *
 * 用法：
 *   node check-bat-syntax.cjs            体检当前目录下所有 .bat
 *   node check-bat-syntax.cjs <目录>      体检指定目录
 *
 * 退出码：0 = 干净，1 = 有问题
 */
const fs = require('fs')
const path = require('path')

// 默认扫当前工作目录（这样从别处调用也能指定目录）；不传参数时用脚本所在目录
const targetDir = process.argv[2] ? path.resolve(process.argv[2]) : process.cwd()

// --- 单行解析：统计未转义、未被引号包裹的括号 -------------------------------
function bareParens(line) {
  let s = ''
  let inQuote = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    if (ch === '^') { i++; continue }          // 转义符，下一个字符跳过
    if (ch === '"') { inQuote = !inQuote; s += '""'; continue }
    if (!inQuote) s += ch
  }
  const out = []
  for (const ch of s) if (ch === '(' || ch === ')') out.push(ch)
  return out
}

function parenBalance(line) {
  const bp = bareParens(line)
  const o = bp.filter(c => c === '(').length
  const c = bp.filter(c => c === ')').length
  return { o, c }
}

// --- 体检单个文件 ------------------------------------------------------------
function checkFile(file) {
  let raw
  try {
    raw = fs.readFileSync(file)
  } catch (e) {
    return { file, errors: [`读取失败: ${e.message}`], warnings: [] }
  }

  // bat 常见 GBK / UTF-8。GBK 字节用 utf-8 解会变成替换符，所以靠替换符判断
  let text = null
  const asUtf8 = raw.toString('utf-8')
  if (!asUtf8.includes('\uFFFD')) {
    text = asUtf8
  } else {
    try {
      text = new TextDecoder('gbk').decode(raw)
    } catch (e) {
      text = raw.toString('latin1')
    }
  }

  const lines = text.split(/\r?\n/)
  const errors = []
  const warnings = []

  let depth = 0
  let blockStart = 0

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].replace(/\r$/, '')
    const st = line.trim()
    const low = st.toLowerCase()

    if (low.startsWith('rem') || st.startsWith('::') || st === '') {
      continue
    }

    const { o, c } = parenBalance(line)
    const bp = bareParens(line)
    const inside = depth > 0

    // 规则 1：块内 echo/call 带裸括号 —— cmd 会误判为块边界
    if (inside && bp.length && (low.startsWith('echo') || low.startsWith('call'))) {
      errors.push({
        line: i + 1,
        text: st,
        msg: `处于 ( ) 块内（始于第 ${blockStart} 行）却含未转义括号 ` +
             `${bp.join('')}，cmd 会误判块边界并报「此时不应有 ...。」\n` +
             `         修法：用 ^( 和 ^) 转义，或改成中文括号/去掉括号。`
      })
    }

    // 规则 2：本行括号不配平（排除正常的块起始行，如 "if xxx (" ）
    const opensBlock = /\(\s*$/.test(st) && o === c + 1
    const closesBlock = /^\s*\)/.test(st) && c === o + 1
    if (o !== c && !inside && o > c && !opensBlock) {
      warnings.push({ line: i + 1, text: st, msg: `本行有 ${o} 个 ( 但只有 ${c} 个 )` })
    }
    if (o !== c && inside && o > c && !opensBlock && !closesBlock) {
      warnings.push({ line: i + 1, text: st, msg: `块内本行有 ${o} 个 ( 但只有 ${c} 个 )` })
    }

    depth += o - c
    if (depth === 1) blockStart = i + 1
    if (depth < 0) { depth = 0 }
  }

  // 规则 3：文件整体括号不配平
  if (depth !== 0) {
    errors.push({ line: 0, text: '', msg: `整个文件括号不配平（结束时深度 = ${depth}）` })
  }

  return { file, errors, warnings }
}

// --- 主流程 ------------------------------------------------------------------
let files = []
try {
  files = fs.readdirSync(targetDir)
    .filter(f => f.toLowerCase().endsWith('.bat'))
    .map(f => path.join(targetDir, f))
} catch (e) {
  process.stderr.write(`[X] 读不了目录 ${targetDir}: ${e.message}\n`)
  process.exit(1)
}

// --- 输出缓冲 ---------------------------------------------------------------
// cmd 默认代码页是 936(GBK)，而 Node 的 stdout 输出的是 UTF-8 字节，
// 直接 console.log 会在 cmd 窗口里显示成乱码。所以这里先把内容攒起来，
// 再按 GBK 编码一次性写到 stdout。
const out = []
const say = (...args) => out.push(args.join(' '))

say('='.repeat(70))
say('  批处理语法体检 —— 专抓「此时不应有 ...。」类括号错误')
say(`  目录: ${targetDir}`)
say('='.repeat(70))
say('')

let totalErrors = 0
let totalWarnings = 0

for (const f of files.sort()) {
  const r = checkFile(f)
  const name = path.basename(f)
  if (r.errors.length === 0 && r.warnings.length === 0) {
    say(`  [OK]   ${name}`)
    continue
  }
  for (const e of r.errors) {
    totalErrors++
    say(`  [错误] ${name}:${e.line}`)
    say(`         ${e.text}`)
    say(`         ${e.msg}`)
  }
  for (const w of r.warnings) {
    totalWarnings++
    say(`  [提醒] ${name}:${w.line}  ${w.text}`)
    say(`         ${w.msg}`)
  }
}

say('')
say('='.repeat(70))
say(`  扫描 ${files.length} 个 .bat，错误 ${totalErrors} 个，提醒 ${totalWarnings} 个`)
if (totalErrors === 0) {
  say('  结论：全部通过，没有会导致「此时不应有 ...。」的语法问题。')
} else {
  say('  结论：发现语法错误，按上面的提示修好再运行。')
}
say('='.repeat(70))

// 输出编码处理：
//   cmd 窗口代码页是 936(GBK)，但 Node 的 Buffer 只支持 UTF-8 编码输出
//   （GBK 只能解码不能编码，除非编译时带 full-ICU）。
//   所以这里先输出 UTF-8，再交给 Python 转成 GBK 字节写 stdout。
//   由 .bat 入口设置 CHECK_BAT_GBK=1 来开启；不设置时保持 UTF-8
//   （在 Git Bash / VS Code 终端里直接跑才不会是乱码）。
const text = out.join('\r\n') + '\r\n'

if (!process.env.CHECK_BAT_GBK) {
  process.stdout.write(text)
  process.exit(totalErrors > 0 ? 1 : 0)
}

const { spawnSync } = require('child_process')
const py = spawnSync('python', ['-c', 'import sys;sys.stdout.buffer.write(sys.stdin.buffer.read().decode("utf-8").encode("gbk"))'], {
  input: Buffer.from(text, 'utf8'),
  maxBuffer: 32 * 1024 * 1024,
})

if (py.status === 0 && py.stdout && py.stdout.length) {
  process.stdout.write(py.stdout)
} else {
  // 没有 python 或转换失败，退回 UTF-8（顶多乱码，不影响判断）
  process.stdout.write(text)
}

process.exit(totalErrors > 0 ? 1 : 0)
