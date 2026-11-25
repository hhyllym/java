#!/usr/bin/env node
/**
 * 星糖谷 后端启动器（免 Maven 版）
 *
 * 为什么需要它：
 *   这台机器没有独立安装 Maven，只剩一个 Zulu JDK 21。
 *   但 target/classes 里的编译产物是最新的，且 ~/.m2 里依赖 jar 齐全 ——
 *   于是完全可以跳过 `mvn spring-boot:run`，直接 `java -cp` 启动，几秒钟就绪，
 *   不用联网下载依赖、不用重新编译。
 *
 * 环境变量：
 *   BACKEND_DIR  后端工程目录（默认 D:\workSpace\java\backend-src.bak）
 *   APP_PORT     后端端口（默认 8081）
 *   JAVA_HOME    指定 JDK；不指定则自动探测
 *
 * 用法：
 *   node start-backend-jbr.cjs              前台启动（Ctrl+C 停）
 *   node start-backend-jbr.cjs --bg         后台启动（写 pid 文件到 run-backend.pid）
 */
const { spawn, execSync } = require('child_process')
const fs = require('fs')
const os = require('os')
const path = require('path')

const BACKEND_DIR = process.env.BACKEND_DIR || 'D:\\workSpace\\java\\backend-src.bak'
const APP_PORT = process.env.APP_PORT || '8081'
const MAIN_CLASS = 'com.fenglin.springboottest.SpringBootTestApplication'
const LOG = path.join(BACKEND_DIR, 'run-backend.log')
const PID_FILE = path.join(BACKEND_DIR, 'run-backend.pid')

// --- 探测 JDK ---------------------------------------------------------------
// 之前这里写死了 IntelliJ 的 JBR 路径，换机器就失效。这里改成按优先级探测，
// 谁能用就用谁，全部失败才报错。
function detectJavaHome() {
  const candidates = []

  // 1) 调用方显式指定
  if (process.env.JAVA_HOME) candidates.push(process.env.JAVA_HOME)

  // 2) 常见安装位置（含通配目录）
  const roots = [
    'C:\\Program Files\\Zulu',
    'C:\\Program Files\\Java',
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\JetBrains',
    'D:\\Program Files\\Java',
    'D:\\Program Files\\JetBrains',
    'D:\\Java',
  ]
  for (const root of roots) {
    try {
      for (const name of fs.readdirSync(root)) {
        const full = path.join(root, name)
        // IntelliJ 的 jbr 在 <安装目录>\jbr，JDK 则直接是目录本身
        candidates.push(path.join(full, 'jbr'))
        candidates.push(full)
      }
    } catch (_) { /* 目录不存在，跳过 */ }
  }

  // 3) PATH 里的 java
  try {
    const where = execSync('where java', { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] })
    for (const line of where.split(/\r?\n/)) {
      const exe = line.trim()
      if (!exe) continue
      // ...\bin\java.exe  ->  ...\
      candidates.push(path.dirname(path.dirname(exe)))
    }
  } catch (_) { /* where 找不到，忽略 */ }

  // 4) ~/.jdks（IDEA 托管的 JDK）
  const jdks = path.join(os.homedir(), '.jdks')
  try {
    for (const name of fs.readdirSync(jdks)) candidates.push(path.join(jdks, name))
  } catch (_) { /* 没有 .jdks，忽略 */ }

  const seen = new Set()
  for (const dir of candidates) {
    if (!dir || seen.has(dir)) continue
    seen.add(dir)
    const exe = path.join(dir, 'bin', 'java.exe')
    if (!fs.existsSync(exe)) continue
    // 用 -version 实际验证一下，确保不是坏目录
    try {
      execSync(`"${exe}" -version`, { stdio: 'ignore' })
      return dir
    } catch (_) { /* 不是可用的 JDK，继续找下一个 */ }
  }
  return null
}

const JAVA_HOME = detectJavaHome()
const javaExe = JAVA_HOME ? path.join(JAVA_HOME, 'bin', 'java.exe') : null
const classes = path.join(BACKEND_DIR, 'target', 'classes')
const cpFile = path.join(BACKEND_DIR, 'cp.txt')

// --- 前置检查 -------------------------------------------------------------
if (!JAVA_HOME) {
  console.error('[X] 找不到可用的 JDK（需要 JDK 17 或 21）。')
  console.error('    本机已装 Zulu 21 的情况下请检查 JAVA_HOME，')
  console.error('    或者安装一个 JDK 后重试。')
  process.exit(1)
}
for (const [label, p] of [['java.exe', javaExe], ['target/classes', classes], ['cp.txt', cpFile]]) {
  if (!fs.existsSync(p)) {
    console.error(`[X] 缺少 ${label}: ${p}`)
    if (label === 'target/classes') {
      console.error('    target 目录会被清掉，需要先用 Maven 编译一次')
    }
    process.exit(1)
  }
}

// --- 组装 classpath --------------------------------------------------------
// cp.txt 是在另一台机器上导出的，里面的路径写死了那个用户的家目录
// （例如 C:\Users\18017\.m2\...）。这里把家目录重写成当前用户，
// 这样换机器/换账户也能直接用，不用手工改 cp.txt。
const HOME = os.homedir()
const rawCp = fs.readFileSync(cpFile, 'utf8')

const jars = rawCp
  .split(/[;\r\n]/)
  .map(s => s.trim())
  .filter(Boolean)
  .map(p => {
    // 把任何 <盘符>:\Users\<某人>\.m2\... 里的用户名换成当前用户
    return p.replace(/^[A-Za-z]:[\\/]Users[\\/][^\\/]+/, HOME)
  })

const missing = jars.filter(j => !fs.existsSync(j))
if (missing.length) {
  console.error(`[X] 有 ${missing.length} 个依赖 jar 不在本地仓库（~/.m2），需要先跑一次 Maven 下载：`)
  missing.slice(0, 8).forEach(m => console.error('    ' + m))
  if (missing.length > 8) console.error(`    ... 还有 ${missing.length - 8} 个`)
  console.error("    （启动脚本检测到退出码 2，会自动回退 Maven 联网补齐）")
  process.exit(2)
}
const cp = [classes, ...jars].join(';')

// --- 环境变量清理 ----------------------------------------------------------
// 宽松绑定陷阱：SERVER__PORT 会被 Spring 当成 server.port，Tomcat 就去监听随机端口
const env = { ...process.env }
delete env.SERVER_PORT
delete env.SERVER__PORT
env.JAVA_HOME = JAVA_HOME
env.SERVER_PORT = APP_PORT

// --- 源码新鲜度自检：源码改了没重编就自动编译 ----------------------------
// 背景（2026-11-18 大厅发消息 404 事故）：Git 检出的 target/classes 是旧产物，
// 新增的 HallController 等源码从未编译，fast path 直接跑旧 class -> 接口 404。
// 这里在启动前逐个比对 .java 与 .class，发现源码更新 / class 缺失就自动 mvn compile。
function walkJava(dir, out) {
  let ents = []
  try { ents = fs.readdirSync(dir, { withFileTypes: true }) } catch (_) { return out }
  for (const e of ents) {
    const full = path.join(dir, e.name)
    if (e.isDirectory()) walkJava(full, out)
    else if (e.name.endsWith('.java')) out.push(full)
  }
  return out
}
function detectMavenCmd() {
  const cands = []
  for (const k of ['MAVEN_HOME', 'M2_HOME']) {
    if (process.env[k]) cands.push(path.join(process.env[k], 'bin', 'mvn.cmd'))
  }
  const roots = ['D:\\', 'C:\\', 'D:\\Program Files\\JetBrains', 'C:\\Program Files\\JetBrains']
  for (const r of roots) {
    let names = []
    try { names = fs.readdirSync(r) } catch (_) { continue }
    for (const n of names) {
      cands.push(path.join(r, n, 'plugins', 'maven', 'lib', 'maven3', 'bin', 'mvn.cmd'))
      cands.push(path.join(r, n, 'bin', 'mvn.cmd'))
    }
  }
  try {
    const w = execSync('where mvn.cmd', { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] })
    for (const line of w.split(/\r?\n/)) if (line.trim()) cands.push(line.trim())
  } catch (_) {}
  return cands.find(p => { try { return fs.existsSync(p) } catch (_) { return false } }) || null
}
function sourceIsStale() {
  const srcRoot = path.join(BACKEND_DIR, 'src', 'main', 'java')
  if (!fs.existsSync(srcRoot)) return false
  const files = walkJava(srcRoot, [])
  for (const jf of files) {
    const name = path.basename(jf)
    if (name === 'package-info.java' || name === 'module-info.java') continue
    const rel = path.relative(srcRoot, jf).replace(/\.java$/, '.class')
    const cf = path.join(classes, rel)
    try {
      if (!fs.existsSync(cf)) {
        console.log('[后端] 发现源码未编译: ' + rel)
        return true
      }
      if (fs.statSync(jf).mtimeMs > fs.statSync(cf).mtimeMs + 1000) {
        console.log('[后端] 发现源码比 class 新: ' + rel)
        return true
      }
    } catch (_) {}
  }
  return false
}
if (sourceIsStale()) {
  const mvnCmd = detectMavenCmd()
  if (!mvnCmd) {
    console.error('[X] 源码有更新，需要重新编译，但找不到 Maven。')
    process.exit(2)  // 让 bat 回退到 Maven 路径（那条路径本身会编译）
  }
  console.log('[后端] 源码有更新，自动重新编译（约 10 秒）...')
  console.log('[后端] Maven: ' + mvnCmd)
  let compiled = false
  for (const extra of [['-o'], []]) {
    try {
      const cmd = '"' + mvnCmd + '" -f "' + path.join(BACKEND_DIR, 'pom.xml') +
                  '" -DskipTests ' + extra.join(' ') + ' compile'
      execSync(cmd, { cwd: BACKEND_DIR, stdio: 'inherit', env })
      compiled = true
      break
    } catch (e) {
      console.warn('[后端] 离线编译未成功，尝试联网... ' + (extra.length ? '' : '（最后一次）'))
    }
  }
  if (!compiled) {
    console.error('[X] 自动编译失败，请手动在后端目录执行 mvn compile。')
    process.exit(1)
  }
  console.log('[后端] 自动编译完成，继续启动。')
}
const args = ['-Dfile.encoding=UTF-8', '-cp', cp, MAIN_CLASS]
const bg = process.argv.includes('--bg')

if (!bg) {
  console.log(`[后端] 启动中 ... http://192.168.68.10:${APP_PORT}/api`)
  console.log(`[后端] 工程目录 : ${BACKEND_DIR}`)
  console.log(`[后端] JDK      : ${JAVA_HOME}`)
  console.log(`[后端] 日志     : ${LOG}`)
  const child = spawn(javaExe, args, { cwd: BACKEND_DIR, env, stdio: 'inherit' })
  child.on('exit', code => process.exit(code))
} else {
  const out = fs.openSync(LOG, 'a')
  const child = spawn(javaExe, args, { cwd: BACKEND_DIR, env, stdio: ['ignore', out, out] })
  child.unref()
  fs.writeFileSync(PID_FILE, String(child.pid), 'utf8')
  console.log(`[后端] 后台启动 PID=${child.pid}  http://192.168.68.10:${APP_PORT}/api`)
  console.log(`[后端] JDK : ${JAVA_HOME}`)
  console.log(`[后端] 日志: ${LOG}`)
}
