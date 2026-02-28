import { h } from 'vue';
import { createRouter, createWebHistory, RouterView, type RouteRecordRaw } from 'vue-router';
import request from '../utils/request';
import { clearAuthTokens, getAccessToken } from '../utils/auth';
import { usePermissionStore, type BackendRouteItem } from '../store/permission';

const viewModules = import.meta.glob('../views/**/*.vue');
const FeatureDisabledView = () => import('../views/common/FeatureDisabled.vue');

const ParentView = {
    name: 'ParentView',
    render: () => h(RouterView)
};

const staticRoutes: RouteRecordRaw[] = [
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
        name: 'AdminRoot',
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
    routes: staticRoutes
});

const dynamicRouteNames = new Set<string>();

const normalizeChildPath = (path?: string): string => {
    const value = (path || '').trim();
    if (!value) {
        return 'index';
    }
    return value.replace(/^\/+/, '');
};

const normalizeRedirect = (redirect?: string): string | undefined => {
    if (!redirect) {
        return undefined;
    }
    const value = redirect.trim();
    if (!value) {
        return undefined;
    }
    if (value.startsWith('/admin')) {
        return value;
    }
    if (value.startsWith('/')) {
        return `/admin${value}`;
    }
    return `/admin/${value}`;
};

const resolveViewComponent = (component?: string) => {
    if (!component || component === 'common/FeatureDisabled') {
        return FeatureDisabledView;
    }
    if (component === 'Layout') {
        return ParentView;
    }
    const normalized = component.replace(/^\/+/, '').replace(/\.vue$/i, '');
    const candidate = `../views/${normalized}.vue`;
    return viewModules[candidate] || FeatureDisabledView;
};

const toRouteRecord = (route: BackendRouteItem): RouteRecordRaw | null => {
    if (!route.name) {
        return null;
    }

    const children: RouteRecordRaw[] = [];
    for (const child of route.children || []) {
        const childRecord = toRouteRecord(child);
        if (childRecord) {
            children.push(childRecord);
        }
    }

    const record: RouteRecordRaw = {
        path: normalizeChildPath(route.path),
        name: route.name,
        component: resolveViewComponent(route.component),
        meta: {
            title: route.meta?.title || route.name,
            icon: route.meta?.icon || 'apps',
            noCache: !!route.meta?.noCache
        },
        ...(children.length ? { children } : {})
    };

    const redirect = normalizeRedirect(route.redirect);
    if (redirect) {
        record.redirect = redirect;
    }
    if (route.name === 'Dashboard') {
        record.meta = { ...record.meta, affix: true };
    }
    return record;
};

const collectRouteNames = (record: RouteRecordRaw, bucket: Set<string>) => {
    if (record.name) {
        bucket.add(String(record.name));
    }
    for (const child of record.children || []) {
        collectRouteNames(child, bucket);
    }
};

const addDynamicRoutes = (routes: BackendRouteItem[]): boolean => {
    let added = false;
    for (const route of routes) {
        if (!route.name || router.hasRoute(route.name)) {
            continue;
        }
        const record = toRouteRecord(route);
        if (!record) {
            continue;
        }
        router.addRoute('AdminRoot', record);
        collectRouteNames(record, dynamicRouteNames);
        added = true;
    }
    return added;
};

const ensureDynamicRoutesLoaded = async (): Promise<boolean> => {
    const permissionStore = usePermissionStore();
    if (permissionStore.dynamicRoutesLoaded) {
        return false;
    }
    const response = await request.get('/auth/routers') as { data: BackendRouteItem[] };
    const backendRoutes = Array.isArray(response.data) ? response.data : [];
    permissionStore.setBackendRoutes(backendRoutes);
    return addDynamicRoutes(backendRoutes);
};

export const resetDynamicRoutes = () => {
    const names = Array.from(dynamicRouteNames).reverse();
    for (const name of names) {
        if (router.hasRoute(name)) {
            router.removeRoute(name);
        }
    }
    dynamicRouteNames.clear();
};

const resetPermissionState = () => {
    const permissionStore = usePermissionStore();
    permissionStore.resetRoutes();
    resetDynamicRoutes();
};

router.beforeEach(async (to, _from, next) => {
    const token = getAccessToken();
    const rawTitle = typeof to.meta?.title === 'string' ? to.meta.title : 'BML中台管理';
    document.title = `${rawTitle} - BML中台管理`;

    if (!to.path.startsWith('/admin')) {
        next();
        return;
    }

    if (to.path === '/admin/login') {
        if (token) {
            next('/admin');
            return;
        }
        resetPermissionState();
        next();
        return;
    }

    if (!token) {
        resetPermissionState();
        next('/admin/login');
        return;
    }

    try {
        const added = await ensureDynamicRoutesLoaded();
        if (added && to.matched.length <= 1) {
            next({ ...to, replace: true });
            return;
        }
        next();
    } catch (error) {
        clearAuthTokens();
        resetPermissionState();
        next('/admin/login');
    }
});

export default router;
