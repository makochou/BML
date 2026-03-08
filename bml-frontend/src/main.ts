import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import ArcoVue from '@arco-design/web-vue';
import '@arco-design/web-vue/dist/arco.css';
import { createPinia } from 'pinia';
import router from './router';

try {
    const app = createApp(App);

    app.use(ArcoVue);
    app.use(createPinia());
    app.use(router);

    // 全局错误处理
    app.config.errorHandler = (err, instance, info) => {
        console.error('Global Error Handler:', err);
        console.error('Component:', instance);
        console.error('Error Info:', info);
    };

    app.mount('#app');
} catch (error) {
    console.error('Failed to initialize app:', error);
    // 显示错误信息给用户
    document.body.innerHTML = `
        <div style="padding: 20px; font-family: sans-serif;">
            <h1 style="color: #f53f3f;">应用启动失败</h1>
            <p>请打开浏览器控制台查看详细错误信息</p>
            <pre style="background: #f7f8fa; padding: 10px; border-radius: 4px; overflow: auto;">${error}</pre>
        </div>
    `;
}
