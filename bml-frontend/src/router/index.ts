import { createRouter, createWebHistory } from 'vue-router';

const routes = [
    {
        path: '/',
        redirect: '/api/list'
    },
    {
        path: '/api',
        component: () => import('../layout/Layout.vue'),
        children: [
            {
                path: 'list',
                name: 'ApiList',
                component: () => import('../views/api/ApiList.vue'),
                meta: { title: 'API 管理' }
            },
            {
                path: 'debug',
                name: 'ApiDebug',
                component: () => import('../views/api/ApiDebug.vue'),
                meta: { title: '在线调试' }
            },
            {
                path: 'app',
                name: 'AppList',
                component: () => import('../views/app/AppList.vue'),
                meta: { title: '应用管理' }
            }
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
