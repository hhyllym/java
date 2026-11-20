import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 固定绑本机局域网 IP 192.168.68.10：手机 / 同学用 http://192.168.68.10:5180 访问。
    // 前端 /api 代理到同机的后端 8081，也走这个 IP。
    host: '192.168.68.10',
    port: 5180,
    // 关掉 vite 自带的开浏览器，改由 启动前端.bat 调 打开网页.bat 在端口起来之后再开一次，
    // 这样「vite 正常启动」和「兜底静态预览」两条路都只开一个标签页，不会开重。
    open: false,
    // strictPort: 端口被占用时直接报错，而不是悄悄换 5174（换了端口代理还是通的，但脚本会等不到 5180）
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://192.168.68.10:8081',
        changeOrigin: true
      }
    }
  }
})
