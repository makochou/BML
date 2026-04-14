import { h } from 'vue';
import { createRouter, createWebHistory, RouterView, type RouteRecordRaw } from 'vue-router';
import request from '../utils/request';
import { clearAuthTokens, getAccessToken } from '../utils/auth';
import { usePermissionStore, type BackendRouteItem } from '../store/permission';
import { fetchLicenseStatus } from '../api/license';

const viewModules = import.meta.glob('../views/**/*.vue');
const FeatureDisabledView = () => import('../views/common/FeatureDisabled.vue');
const API_ACCOUNT_MANAGE_FULL_PATH = '/admin/api/account';

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
        path: '/admin/license',
        name: 'LicenseActivation',
        component: () => import('../views/system/license/index.vue'),
        meta: { title: '许可证激活' }
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
                meta: { title: '工作台', affix: true }
            },
            {
                path: 'api/list',
                name: 'ApiList',
                component: () => import('../views/api/ApiList.vue'),
                meta: { title: '全源资产目录' }
            },
            {
                path: 'api/debug',
                name: 'LegacyApiDebugRedirect',
                redirect: API_ACCOUNT_MANAGE_FULL_PATH,
                meta: { title: '授权治理中心', hidden: true }
            },
            {
                path: 'api/account/:id',
                name: 'ApiAccountDetail',
                component: () => import('../views/api/ApiAccountDetail.vue'),
                meta: { title: '凭证画像详情', hidden: true }
            },
            {
                path: 'app',
                name: 'LegacyAppListRedirect',
                redirect: API_ACCOUNT_MANAGE_FULL_PATH,
                meta: { title: '授权治理中心', hidden: true }
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

/** 许可证状态缓存，避免每次路由切换都请求后端 */
let licenseChecked = false;
let licenseValid = false;

/** 检查许可证状态（仅首次或手动重置时请求后端） */
const ensureLicenseValid = async (): Promise<boolean> => {
    if (licenseChecked) {
        return licenseValid;
    }
    try {
        const res = await fetchLicenseStatus() as any;
        const status = res.data;
        // 未启用许可证校验时视为有效
        if (!status.enabled) {
            licenseValid = true;
        } else {
            licenseValid = status.activated && !status.expired;
        }
    } catch {
        licenseValid = false;
    }
    licenseChecked = true;
    return licenseValid;
};

/** 重置许可证缓存（上传新许可证后调用） */
export const resetLicenseCache = () => {
    licenseChecked = false;
    licenseValid = false;
};

router.beforeEach(async (to, _from, next) => {
    const token = getAccessToken();
    const rawTitle = typeof to.meta?.title === 'string' ? to.meta.title : 'BML中台管理';
    document.title = `${rawTitle} - BML中台管理`;

    // 许可证激活页面始终放行
    if (to.path === '/admin/license') {
        next();
        return;
    }

    // 所有页面（包括前台业务 / 和中台管理 /admin）都需要许可证校验
    // 未激活时统一跳转到许可证激活页面
    const hasLicense = await ensureLicenseValid();
    if (!hasLicense) {
        next('/admin/license');
        return;
    }

    // 非 /admin 路径（前台业务页面）：许可证有效即放行
    if (!to.path.startsWith('/admin')) {
        next();
        return;
    }

    // 以下为 /admin 中台管理路径的认证逻辑
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
