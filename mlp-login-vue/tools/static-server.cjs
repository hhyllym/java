#!/usr/bin/env node
/*
 * 零依赖静态服务器（兜底用）
 * 1) 把 dist/ 当静态站点跑起来（支持 Vue Router 的 history 模式回退到 index.html）
 * 2) 把 /api/** 反向代理到后端 http://localhost:8081
 *
 * 用法:  node tools/static-server.cjs [端口] [--no-open]
 * 依赖:  只用 Node 内置模块，不需要 node_modules
 */
'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const DIST = path.join(ROOT, 'dist');
const BACK_PORT = Number(process.env.BACK_PORT || 8081);

const argv = process.argv.slice(2);
const PORT = Number(argv.find((a) => /^\d+$/.test(a)) || 5173);
const NO_OPEN = argv.includes('--no-open');

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8'
};

if (!fs.existsSync(path.join(DIST, 'index.html'))) {
  console.error('[X] 没有找到 ' + DIST + '\\index.html');
  console.error('    先用 npm run build 生成 dist，或者直接 npm run dev 用 Vite 开发服务器。');
  process.exit(1);
}

function sendFile(res, file) {
  fs.readFile(file, (err, buf) => {
    if (err) {
      res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('500 读取文件失败: ' + file);
      return;
    }
    res.writeHead(200, {
      'Content-Type': MIME[path.extname(file).toLowerCase()] || 'application/octet-stream',
      'Cache-Control': 'no-cache'
    });
    res.end(buf);
  });
}

function proxy(req, res) {
  const opts = {
    host: '127.0.0.1',
    port: BACK_PORT,
    method: req.method,
    path: req.url,
    headers: Object.assign({}, req.headers, { host: 'localhost:' + BACK_PORT })
  };
  const up = http.request(opts, (upRes) => {
    res.writeHead(upRes.statusCode || 502, upRes.headers);
    upRes.pipe(res);
  });
  up.on('error', (e) => {
    res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({
      code: 502,
      msg: '后端 ' + BACK_PORT + ' 端口没起来，接口代理失败: ' + e.code,
      data: null
    }));
  });
  req.pipe(up);
}

const server = http.createServer((req, res) => {
  let pathname = '/';
  try {
    pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  } catch (e) {
    pathname = '/';
  }

  if (pathname === '/api' || pathname.startsWith('/api/')) {
    proxy(req, res);
    return;
  }

  // 防目录穿越
  const target = path.normalize(path.join(DIST, pathname));
  if (!target.startsWith(DIST)) {
    res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('403');
    return;
  }

  fs.stat(target, (err, st) => {
    if (!err && st.isFile()) {
      sendFile(res, target);
    } else {
      // SPA 回退
      sendFile(res, path.join(DIST, 'index.html'));
    }
  });
});

server.on('error', (e) => {
  if (e.code === 'EADDRINUSE') {
    console.error('[X] 端口 ' + PORT + ' 已被占用，先双击 停止全部.bat 停掉旧服务。');
  } else {
    console.error('[X] 启动失败: ' + e.message);
  }
  process.exit(1);
});

server.listen(PORT, () => {
  console.log('');
  console.log('  小马王国 - 静态预览（兜底模式）已启动');
  console.log('  网页地址 : http://localhost:' + PORT + '/');
  console.log('  接口代理 : /api  ->  http://localhost:' + BACK_PORT);
  console.log('  静态目录 : ' + DIST);
  console.log('  要停止   : 在本窗口按 Ctrl+C');
  console.log('');
  if (!NO_OPEN) {
    exec('start "" "http://localhost:' + PORT + '/"');
  }
});
