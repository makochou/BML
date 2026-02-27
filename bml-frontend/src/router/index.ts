import { createRouter, createWebHistory } from 'vue-router';
import { getAccessToken } from '../utils/auth';

const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('../views/home/Home.vue'),
        meta: { title: '业务前台' }
    },
    {
        path: '/admin/login',
        name: 'Login',
        component: () => import('../views/login/Login.vue'),
        meta: { title: '后台登录' }
    },
    {
        path: '/admin',
        component: () => import('../layout/Layout.vue'),
        redirect: '/admin/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('../views/dashboard/Workplace.vue'),
                meta: { title: '仪表盘', affix: true }
            },
            {
                path: 'api/list',
                name: 'ApiList',
                component: () => import('../views/common/FeatureDisabled.vue'),
                meta: { title: 'API 管理', featureName: 'API 管理' }
            },
            {
                path: 'api/debug',
                name: 'ApiDebug',
                component: () => import('../views/common/FeatureDisabled.vue'),
                meta: { title: '在线调试', featureName: '在线调试' }
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

router.beforeEach((to, _from, next) => {
    const token = getAccessToken();
    if (to.meta.title) {
        document.title = `${to.meta.title} - BML中台管理`;
    } else {
        document.title = 'BML中台管理';
    }

    if (to.path.startsWith('/admin')) {
        if (to.path === '/admin/login') {
            next();
        } else if (!token) {
            next('/admin/login');
        } else {
            next();
        }
    } else {
        next();
    }
});

export default router;
