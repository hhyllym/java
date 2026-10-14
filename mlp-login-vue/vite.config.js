import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    // 关掉 vite 自带的开浏览器，改由 启动前端.bat 调 打开网页.bat 在端口起来之后再开一次，
    // 这样「vite 正常启动」和「兜底静态预览」两条路都只开一个标签页，不会开重。
    open: false,
    // strictPort: 端口被占用时直接报错，而不是悄悄换 5174（换了端口代理还是通的，但脚本会等不到 5173）
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
