import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }
          if (id.includes('echarts') || id.includes('zrender') || id.includes('vue-echarts')) {
            return 'vendor-echarts'
          }
          if (id.includes('@arco-design/web-vue')) {
            return 'vendor-arco'
          }
          if (id.includes('vue-router') || id.includes('pinia')) {
            return 'vendor-router-state'
          }
          if (id.includes('node_modules/vue/')) {
            return 'vendor-router-state'
          }
          if (id.includes('axios') || id.includes('crypto-js')) {
            return 'vendor-utils'
          }
          return 'vendor-misc'
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, '') // 后端context-path已是/api，无需重写
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
