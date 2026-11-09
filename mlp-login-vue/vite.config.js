import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 绑 0.0.0.0：本机照常用 127.0.0.1:5180，同时让同一 WiFi 下的其他电脑/手机
    // 能用「主机 IP:5180」访问（大富翁四人联机需要）。只绑 127.0.0.1 时别人连不上。
    host: '0.0.0.0',
    port: 5180,
    // 关掉 vite 自带的开浏览器，改由 启动前端.bat 调 打开网页.bat 在端口起来之后再开一次，
    // 这样「vite 正常启动」和「兜底静态预览」两条路都只开一个标签页，不会开重。
    open: false,
    // strictPort: 端口被占用时直接报错，而不是悄悄换 5174（换了端口代理还是通的，但脚本会等不到 5180）
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
