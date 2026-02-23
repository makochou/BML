import { createRouter, createWebHistory } from 'vue-router';

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
                meta: { title: '仪表台', affix: true }
            },
            {
                path: 'api/list',
                name: 'ApiList',
                component: () => import('../views/api/ApiList.vue'),
                meta: { title: 'API 管理' }
            },
            {
                path: 'api/debug',
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

// 路由守卫
router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token');
    if (to.meta.title) {
        document.title = `${to.meta.title} - BML中台管理`;
    } else {
        document.title = 'BML中台管理';
    }

    // 访问后台管理相关页面 (/admin 开头) 时需要校验权限
    if (to.path.startsWith('/admin')) {
        // 如果是去登录页，直接放行
        if (to.path === '/admin/login') {
            next();
        } else {
            // 如果没有 Token，跳转到登录页
            if (!token) {
                next('/admin/login');
            } else {
                next();
            }
        }
    } else {
        // 访问业务前台，不需要 Token
        next();
    }
});

export default router;
