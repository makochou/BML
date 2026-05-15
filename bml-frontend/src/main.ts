import { createApp } from 'vue';
// 主题引擎全局 Design Token 默认值（必须先于其它样式引入，
// 以保证 `:root` 上的 `--bml-*` / `--arcoblue-*` 出厂默认值进入级联，
// 杜绝主题首屏未上色 / FOUC。详见 styles/tokens.scss 文件头注释。
import './styles/tokens.scss';
import './style.css';
import './assets/business-system.css';
import App from './App.vue';
import ArcoVue from '@arco-design/web-vue';
import ArcoVueIcon from '@arco-design/web-vue/es/icon';
import '@arco-design/web-vue/dist/arco.css';
import { createPinia } from 'pinia';
import router from './router';

/**
 * 应用初始化入口。
 *
 * 初始化顺序：
 * 1. 创建 Vue 应用实例
 * 2. 注册 Arco Design UI 组件库（含图标库）
 * 3. 注册 Pinia 状态管理
 * 4. 注册 Vue Router 路由
 * 5. 挂载到 DOM
 *
 * 全局错误处理：
 * - 捕获 Vue 组件内部未处理的异常，输出到控制台
 * - 应用初始化失败时，在页面显示友好的错误提示
 */
try {
  const app = createApp(App);

  // 注册 Arco Design 组件库
  app.use(ArcoVue);
  // 注册 Arco Design 图标库（支持 <icon-xxx /> 全局使用）
  app.use(ArcoVueIcon);
  // 注册 Pinia 状态管理
  app.use(createPinia());
  // 注册 Vue Router
  app.use(router);

  // 全局 Vue 错误处理器
  app.config.errorHandler = (err, _instance, info) => {
    console.error('[Vue 全局错误]', err);
    console.error('[错误信息]', info);
  };

  app.mount('#app');
} catch (error) {
  console.error('[应用启动失败]', error);
  // 启动失败时在页面显示友好提示，方便排查问题
  document.body.innerHTML = `
    <div style="
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      background: #f2f3f5;
      color: #1d2129;
    ">
      <div style="
        background: white;
        border-radius: 12px;
        padding: 40px;
        max-width: 500px;
        width: 90%;
        box-shadow: 0 4px 20px rgba(0,0,0,0.08);
        text-align: center;
      ">
        <div style="font-size: 48px; margin-bottom: 16px;">⚠️</div>
        <h2 style="color: #f53f3f; margin: 0 0 12px 0;">应用启动失败</h2>
        <p style="color: #86909c; margin: 0 0 20px 0;">请打开浏览器控制台（F12）查看详细错误信息</p>
        <pre style="
          background: #f7f8fa;
          padding: 12px;
          border-radius: 8px;
          font-size: 12px;
          text-align: left;
          overflow: auto;
          color: #f53f3f;
        ">${error}</pre>
      </div>
    </div>
  `;
}
