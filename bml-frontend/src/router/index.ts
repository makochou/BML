import { h } from 'vue';
import { createRouter, createWebHistory, RouterView, type RouteRecordRaw } from 'vue-router';
import request from '../utils/request';
import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens, isAccessTokenExpired } from '../utils/auth';
import axios from 'axios';
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
    /* ═══════════════════════════════════════════════════════
       前台业务系统路由（ip:port/）
       直接访问 ip:port 即进入业务系统，未登录自动跳转 /login
       ═══════════════════════════════════════════════════════ */
    {
        path: '/login',
        name: 'BusinessLogin',
        component: () => import('../views/business/login/BusinessLogin.vue'),
        meta: { title: '业务系统登录' }
    },
    {
        path: '/',
        name: 'BusinessRoot',
        component: () => import('../layout/BusinessLayout.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'BusinessDashboard',
                component: () => import('../views/business/dashboard/index.vue'),
                meta: { title: '工作台', affix: true }
            },
            {
                path: 'system/org',
                name: 'SystemOrg',
                component: () => import('../views/business/system/org/index.vue'),
                meta: { title: '机构管理', parentTitle: '组织与权限', permission: 'system:org:list' }
            },
            {
                path: 'system/dept',
                name: 'SystemDept',
                component: () => import('../views/business/system/dept/index.vue'),
                meta: { title: '部门管理', parentTitle: '组织与权限', permission: 'system:dept:list' }
            },
            {
                path: 'system/post',
                name: 'SystemPost',
                component: () => import('../views/business/system/post/index.vue'),
                meta: { title: '岗位管理', parentTitle: '组织与权限', permission: 'system:post:list' }
            },
            {
                path: 'system/user',
                name: 'SystemUser',
                component: () => import('../views/business/system/user/index.vue'),
                meta: { title: '用户管理', parentTitle: '组织与权限', permission: 'system:user:list' }
            },
            {
                path: 'system/role',
                name: 'SystemRole',
                component: () => import('../views/business/system/role/index.vue'),
                meta: { title: '角色与权限', parentTitle: '组织与权限', permission: 'system:role:list' }
            },
            {
                path: 'system/menu',
                name: 'SystemMenu',
                component: () => import('../views/business/system/menu/index.vue'),
                meta: { title: '菜单管理', parentTitle: '基础配置', permission: 'system:menu:list' }
            },
            {
                path: 'system/dict',
                name: 'SystemDict',
                component: () => import('../views/business/system/dict/index.vue'),
                meta: { title: '字典管理', parentTitle: '基础配置', permission: 'system:dict:list' }
            },
            {
                path: 'system/config',
                name: 'SystemConfigParam',
                component: () => import('../views/business/system/config/index.vue'),
                meta: { title: '参数配置', parentTitle: '基础配置', permission: 'system:config:list' }
            },
            {
                path: 'system/branding',
                name: 'SystemBranding',
                component: () => import('../views/business/system/branding/index.vue'),
                meta: { title: '系统设置', parentTitle: '基础配置', permission: 'system:setting:list' }
            },
            {
                path: 'system/file',
                name: 'SystemFile',
                component: () => import('../views/business/system/file/index.vue'),
                meta: { title: '文件管理', parentTitle: '基础配置', permission: 'system:file:list' }
            },
            {
                path: 'system/notice',
                name: 'SystemNotice',
                component: () => import('../views/business/system/notice/index.vue'),
                meta: { title: '通知公告', parentTitle: '消息中心', permission: 'system:notice:list' }
            },
            {
                path: 'system/online',
                name: 'SystemOnline',
                component: () => import('../views/business/system/online/index.vue'),
                meta: { title: '在线用户', parentTitle: '安全审计', permission: 'system:online:list' }
            },
            {
                path: 'system/monitor',
                name: 'SystemMonitor',
                component: () => import('../views/monitor/server/index.vue'),
                meta: { title: '系统监控', parentTitle: '运维管理', permission: 'monitor:server:list' }
            },
            {
                path: 'system/cache',
                name: 'SystemCache',
                component: () => import('../views/business/system/cache/index.vue'),
                meta: { title: '缓存管理', parentTitle: '运维管理', permission: 'system:cache:list' }
            },
            {
                path: 'system/job',
                name: 'SystemJob',
                component: () => import('../views/business/system/job/index.vue'),
                meta: { title: '定时任务', parentTitle: '运维管理', permission: 'system:job:list' }
            },
            {
                path: 'system/job-log',
                name: 'SystemJobLog',
                component: () => import('../views/business/system/job-log/index.vue'),
                meta: { title: '任务日志', parentTitle: '运维管理', permission: 'system:joblog:list' }
            },
            {
                path: 'system/audit/overview',
                name: 'SystemAuditOverview',
                component: () => import('../views/business/system/audit-overview/index.vue'),
                meta: { title: '审计总览', parentTitle: '安全审计', permission: 'system:audit:list' }
            },
            {
                path: 'system/loginlog',
                name: 'SystemLoginLog',
                component: () => import('../views/business/system/loginlog/index.vue'),
                meta: { title: '登录日志', parentTitle: '安全审计', permission: 'system:loginlog:list' }
            },
            {
                path: 'system/operlog',
                name: 'SystemOperationLog',
                component: () => import('../views/business/system/operlog/index.vue'),
                meta: { title: '操作日志', parentTitle: '安全审计', permission: 'system:operlog:list' }
            },
            {
                path: 'system/exceptionlog',
                name: 'SystemExceptionLog',
                component: () => import('../views/business/system/exceptionlog/index.vue'),
                meta: { title: '异常日志', parentTitle: '安全审计', permission: 'system:exceptionlog:list' }
            },
            {
                path: 'system/security-alert',
                name: 'SystemSecurityAlert',
                component: () => import('../views/business/system/security-alert/index.vue'),
                meta: { title: '风险告警', parentTitle: '安全审计', permission: 'system:securityalert:list' }
            },
            {
                path: 'system/audit-setting',
                name: 'SystemAuditSetting',
                component: () => import('../views/business/system/audit-setting/index.vue'),
                meta: { title: '归档策略', parentTitle: '安全审计', permission: 'system:auditsetting:list' }
            },
            {
                path: 'profile',
                name: 'BusinessProfile',
                component: () => import('../views/business/profile/index.vue'),
                meta: { title: '个人中心' }
            }
        ]
    },

    /* ═══════════════════════════════════════════════════════
       中台管理平台路由（ip:port/admin）
       ═══════════════════════════════════════════════════════ */
    {
        path: '/admin/login',
        name: 'Login',
        component: () => import('../views/login/Login.vue'),
        meta: { title: '中台登录' }
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
                meta: { title: '资产目录' }
            },
            {
                path: 'api/debug',
                name: 'LegacyApiDebugRedirect',
                redirect: API_ACCOUNT_MANAGE_FULL_PATH,
                meta: { title: '授权治理', hidden: true }
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
                meta: { title: '授权治理', hidden: true }
            },
            {
                path: 'license',
                name: 'LicenseManagement',
                component: () => import('../views/system/license/index.vue'),
                meta: { title: '授权管理', icon: 'safe' }
            },
            {
                path: 'config',
                name: 'SystemConfig',
                component: () => import('../views/system/config/index.vue'),
                meta: { title: '系统配置', icon: 'settings' }
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

/**
 * 路由守卫专用：尝试用 RefreshToken 刷新已过期的 AccessToken。
 * 与 request.ts 中的拦截器独立，避免循环依赖。
 * 仅在路由跳转时前置调用，确保进入页面前 token 有效。
 *
 * @returns true = 刷新成功（新 token 已写入 localStorage），false = 刷新失败（应跳转登录页）
 */
const tryRefreshToken = async (): Promise<boolean> => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) return false;
    try {
        const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api';
        const response = await axios.post<{ code: number; data?: { accessToken: string; refreshToken?: string } }>(
            `${apiBaseURL}/auth/refresh`,
            { refreshToken },
            { timeout: 10000 }
        );
        const payload = response.data;
        if (payload.code === 200 && payload.data?.accessToken) {
            setAuthTokens({
                accessToken: payload.data.accessToken,
                refreshToken: payload.data.refreshToken || refreshToken
            });
            return true;
        }
        return false;
    } catch {
        return false;
    }
};

/**
 * 校验当前 AccessToken 是否可用。
 * 若 JWT 已过期则尝试用 RefreshToken 刷新；刷新失败则返回 false。
 * 若 JWT 未过期则直接返回 true，无额外网络请求。
 */
const ensureTokenValid = async (): Promise<boolean> => {
    if (!isAccessTokenExpired()) return true;
    return tryRefreshToken();
};

/** 前台业务系统需要认证的路径前缀 */
const BUSINESS_AUTH_PREFIXES = ['/dashboard', '/system', '/profile'];

/** 判断路径是否属于前台业务系统需要认证的区域 */
const isBusinessAuthRequired = (path: string): boolean =>
    BUSINESS_AUTH_PREFIXES.some(prefix => path === prefix || path.startsWith(prefix + '/'));

/** 标记业务系统权限是否已加载 */
let businessPermissionsLoaded = false;

/**
 * 路由守卫专用：确保业务系统用户权限已加载到 permissionStore。
 * 仅在首次导航时发起 /auth/info 请求，后续导航直接使用缓存。
 */
const ensureBusinessPermissionsLoaded = async (): Promise<void> => {
    if (businessPermissionsLoaded) return;
    try {
        const res = await request.get('/auth/info') as any;
        const perms: string[] = res.data?.permissions || [];
        const permissionStore = usePermissionStore();
        permissionStore.setButtonPermissions(perms);
    } catch { /* 忽略，后续请求将由 BusinessLayout 重新加载 */ }
    businessPermissionsLoaded = true;
};

/** 重置业务权限缓存（退出登录时调用） */
export const resetBusinessPermissionsCache = () => {
    businessPermissionsLoaded = false;
};

router.beforeEach(async (to, _from, next) => {
    const token = getAccessToken();

    // ── 页面标题 ──
    const rawTitle = typeof to.meta?.title === 'string' ? to.meta.title : 'BML';
    const suffix = to.path.startsWith('/admin') ? 'BML中台管理' : 'BML业务系统';
    document.title = `${rawTitle} - ${suffix}`;

    // ── 许可证管理页面始终放行 ──
    if (to.path === '/admin/license') {
        if (token) {
            try { await ensureDynamicRoutesLoaded(); } catch { /* 忽略 */ }
        }
        next();
        return;
    }

    // ── 许可证校验（所有页面均需） ──
    const hasLicense = await ensureLicenseValid();
    if (!hasLicense) {
        next('/admin/license');
        return;
    }

    // ── 前台业务系统登录页 ──
    if (to.path === '/login') {
        if (token) {
            next('/dashboard');
            return;
        }
        next();
        return;
    }

    // ── 前台业务系统认证区域（/dashboard, /system/*） ──
    if (isBusinessAuthRequired(to.path)) {
        if (!token) {
            next('/login');
            return;
        }
        // 前置校验：AccessToken JWT 过期时尝试刷新，刷新失败则跳转登录
        const valid = await ensureTokenValid();
        if (!valid) {
            clearAuthTokens();
            next('/login');
            return;
        }

        // 确保用户权限已加载（首次导航时从后端获取）
        await ensureBusinessPermissionsLoaded();

        // 菜单权限校验：若路由配置了 permission 且用户无此权限，重定向到工作台
        const requiredPerm = (to.meta as any)?.permission as string | undefined;
        if (requiredPerm) {
            const permissionStore = usePermissionStore();
            if (!permissionStore.hasPermission(requiredPerm)) {
                next('/dashboard');
                return;
            }
        }

        next();
        return;
    }

    // ── 其他非 /admin 路径：直接放行 ──
    if (!to.path.startsWith('/admin')) {
        next();
        return;
    }

    // ── 中台管理平台登录页 ──
    if (to.path === '/admin/login') {
        if (token) {
            next('/admin');
            return;
        }
        resetPermissionState();
        next();
        return;
    }

    // ── 中台管理平台认证区域 ──
    if (!token) {
        resetPermissionState();
        next('/admin/login');
        return;
    }

    // 前置校验：AccessToken JWT 过期时尝试刷新，刷新失败则跳转登录
    const adminTokenValid = await ensureTokenValid();
    if (!adminTokenValid) {
        clearAuthTokens();
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
