import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

/**
 * 将 Arco Design 图标独立拆包。
 * 说明：
 * 1. 当前构建体积超预算的主要来源是图标资源；
 * 2. 将图标单独抽离后，主 UI 运行时代码仍保持单块缓存，
 *    既能通过 bundle budget，又能减少复杂拆包带来的循环依赖噪音；
 * 3. 其余 Arco 组件统一落到 vendor-arco-core，策略简单且稳定。
 */
function resolveArcoChunkName(id: string): string {
  if (id.includes('/es/icon/') || id.includes('/es/icon/index')) {
    return 'vendor-arco-icon';
  }
  return 'vendor-arco-core';
}

/**
 * Vite 构建配置
 *
 * 主要配置项：
 * 1. 路径别名：@ 指向 src 目录，简化导入路径
 * 2. 开发代理：将 /api 请求转发到后端服务，解决跨域问题
 * 3. 生产分包：将大型第三方库拆分为独立 chunk，优化首屏加载速度
 * 4. 构建优化：关闭 sourcemap，压缩代码
 *
 * @see https://vitejs.dev/config/
 */
export default defineConfig({
  plugins: [
    vue(),
  ],

  // ── 路径别名 ──
  resolve: {
    alias: {
      // @ 指向 src 目录，避免相对路径层级过深
      '@': resolve(__dirname, 'src'),
    },
  },

  // ── 生产构建配置 ──
  build: {
    // chunk 大小警告阈值（KB），超过此值会在构建时输出警告
    chunkSizeWarningLimit: 800,
    // 生产环境关闭 sourcemap，减小产物体积
    sourcemap: false,
    rollupOptions: {
      output: {
        /**
         * 手动分包策略：
         * 将大型第三方库拆分为独立 chunk，利用浏览器缓存机制，
         * 避免业务代码更新时用户需要重新下载所有依赖。
         *
         * 分包原则：
         * - 按功能域分组（图表、UI 组件库、路由状态、工具库）
         * - 每个 chunk 控制在合理大小范围内
         */
        manualChunks(id: string) {
          if (!id.includes('node_modules')) {
            return undefined;
          }
          // ECharts 图表库（体积较大，单独分包）
          if (id.includes('echarts') || id.includes('zrender') || id.includes('vue-echarts')) {
            return 'vendor-echarts';
          }
          // Arco Design UI 组件库
          if (id.includes('@arco-design/web-vue')) {
            return resolveArcoChunkName(id);
          }
          // Vue 核心 + 路由 + 状态管理（频繁访问，合并减少请求数）
          if (id.includes('vue-router') || id.includes('pinia') || id.includes('node_modules/vue/')) {
            return 'vendor-vue-core';
          }
          // 工具库（axios、crypto-js、dayjs）
          if (id.includes('axios') || id.includes('crypto-js') || id.includes('dayjs')) {
            return 'vendor-utils';
          }
          // 其他第三方库
          return 'vendor-misc';
        },
      },
    },
  },

  // ── 开发服务器配置 ──
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      /**
       * 将 /api 前缀的请求代理到后端服务。
       * 后端 context-path 已配置为 /api，因此无需 rewrite。
       * 生产环境通过 Nginx 反向代理实现，无需此配置。
       */
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Actuator 健康检查端点代理
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
