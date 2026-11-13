#!/usr/bin/env node
/**
 * 星糖谷 后端启动器（免 Maven 版）
 *
 * 为什么需要它：
 *   本机 Maven 已被卸载（IDEA 之外没装），Zulu JDK 也卸了，
 *   只剩 IntelliJ 自带的 JBR 21。但 target/classes 里的编译产物是最新的，
 *   且 ~/.m2 里 83 个依赖 jar 齐全 —— 于是完全可以跳过 `mvn spring-boot:run`，
 *   直接 `java -cp` 启动，几秒钟就绪，不用联网下载依赖。
 *
 * 环境变量：
 *   BACKEND_DIR  后端工程目录（默认 D:\workSpace\java\backend-src.bak）
 *   APP_PORT     后端端口（默认 8081）
 *
 * 用法：
 *   node start-backend-jbr.cjs              前台启动（Ctrl+C 停）
 *   node start-backend-jbr.cjs --bg         后台启动（写 pid 文件到 run-backend.pid）
 */
const { spawn } = require('child_process')
const fs = require('fs')
const path = require('path')

const BACKEND_DIR = process.env.BACKEND_DIR || 'D:\\workSpace\\java\\backend-src.bak'
const APP_PORT = process.env.APP_PORT || '8081'
const JBR = 'C:\\Program Files\\JetBrains\\IntelliJ IDEA 2024.3.2.2\\jbr'
const MAIN_CLASS = 'com.fenglin.springboottest.SpringBootTestApplication'
const LOG = path.join(BACKEND_DIR, 'run-backend.log')
const PID_FILE = path.join(BACKEND_DIR, 'run-backend.pid')

const javaExe = path.join(JBR, 'bin', 'java.exe')
const classes = path.join(BACKEND_DIR, 'target', 'classes')
const cpFile = path.join(BACKEND_DIR, 'cp.txt')

// --- 前置检查 -------------------------------------------------------------
for (const [label, p] of [['JBR java.exe', javaExe], ['target/classes', classes], ['cp.txt', cpFile]]) {
  if (!fs.existsSync(p)) {
    console.error(`[X] 缺少 ${label}: ${p}`)
    console.error(`    如果这是首次运行，请先用 Maven 构建一次（或把 BACKEND_DIR 指向已编译好的工程目录）`)
    process.exit(1)
  }
}

// --- 组装 classpath --------------------------------------------------------
const jars = fs.readFileSync(cpFile, 'utf8').split(/[;\r\n]/).map(s => s.trim()).filter(Boolean)
const missing = jars.filter(j => !fs.existsSync(j))
if (missing.length) {
  console.error(`[X] 有 ${missing.length} 个依赖 jar 不在本地仓库（~/.m2），需要先跑一次 Maven 下载：`)
  missing.slice(0, 5).forEach(m => console.error('    ' + m))
  process.exit(1)
}
const cp = [classes, ...jars].join(';')

// --- 环境变量清理 ----------------------------------------------------------
// 宽松绑定陷阱：SERVER__PORT 会被 Spring 当成 server.port，Tomcat 就去监听随机端口
const env = { ...process.env }
delete env.SERVER_PORT
delete env.SERVER__PORT
env.JAVA_HOME = JBR
env.SERVER_PORT = APP_PORT

const args = ['-Dfile.encoding=UTF-8', '-cp', cp, MAIN_CLASS]
const bg = process.argv.includes('--bg')

if (!bg) {
  console.log(`[后端] 启动中 ... http://localhost:${APP_PORT}/api`)
  console.log(`[后端] 工程目录 : ${BACKEND_DIR}`)
  console.log(`[后端] 日志     : ${LOG}`)
  const child = spawn(javaExe, args, { cwd: BACKEND_DIR, env, stdio: 'inherit' })
  child.on('exit', code => process.exit(code))
} else {
  const out = fs.openSync(LOG, 'a')
  const child = spawn(javaExe, args, { cwd: BACKEND_DIR, env, stdio: ['ignore', out, out] })
  child.unref()
  fs.writeFileSync(PID_FILE, String(child.pid), 'utf8')
  console.log(`[后端] 后台启动 PID=${child.pid}  http://localhost:${APP_PORT}/api`)
  console.log(`[后端] 日志: ${LOG}`)
}
