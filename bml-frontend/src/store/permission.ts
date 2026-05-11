import { defineStore } from 'pinia';

export interface BackendRouteMeta {
    title: string;
    icon?: string;
    noCache?: boolean;
}

export interface BackendRouteItem {
    name: string;
    path: string;
    component: string;
    redirect?: string;
    hidden?: boolean;
    alwaysShow?: boolean;
    meta?: BackendRouteMeta;
    children?: BackendRouteItem[];
}

export interface SidebarMenuItem {
    name: string;
    title: string;
    icon: string;
    hidden: boolean;
    children: SidebarMenuItem[];
}

interface PermissionState {
    dynamicRoutesLoaded: boolean;
    backendRoutes: BackendRouteItem[];
    sidebarMenus: SidebarMenuItem[];
    /**
     * 用户按钮级权限标识列表。
     * 来自后端 /auth/info 接口返回的 permissions 字段，
     * 格式如 ['system:org:list', 'system:org:edit', ...]
     * 超级管理员为 ['*:*:*']，表示拥有全部权限。
     */
    buttonPermissions: string[];
    /**
     * 标记按钮权限是否已从后端加载完成。
     * - false：权限尚未加载（首次导航 / 登录后未请求 /auth/info）
     * - true：已调用 setButtonPermissions，即使 permissions 为空数组也表示“加载完成”
     * 用于区分“未加载”与“加载完成但用户无 B/F 权限”两种状态。
     */
    buttonPermissionsLoaded: boolean;
}

/**
 * 中台管理平台侧边栏禁止展示的顶级菜单标识集合。
 * <p>
 * 业务系统的所有菜单均挂载在后端 {@code path='system'} 的顶级 M 目录下，
 * 后端 {@code SysMenuServiceImpl#normalizePath} 会为根节点补前导斜杠，因此
 * 匹配时需同时覆盖 {@code system} 与 {@code /system} 两种形态。
 * 虽然后端已针对中台管理员过滤该子树（见 {@code AdminRouterFilter}），
 * 但前端仍保留一道客户端防线，避免以下场景漏网：
 * <ul>
 *   <li>浏览器缓存了过期的 {@code /auth/routers} 响应</li>
 *   <li>后端滚动发布期间个别节点尚未升级</li>
 *   <li>后续有人在数据库新增其它业务菜单的顶级别名</li>
 * </ul>
 * 若将来需要屏蔽更多顶级菜单，直接在此处追加对应路径即可。
 * </p>
 */
const ADMIN_SIDEBAR_BLOCKED_PATHS = new Set<string>(['system']);

/**
 * 将路由路径规范化为"去前导斜杠的小写字符串"，便于统一匹配。
 *
 * @param path 原始路径
 * @returns 规范化后的字符串（{@code ''} 表示空路径）
 */
const normalizeRoutePath = (path?: string): string => {
    if (!path) return '';
    const trimmed = path.trim().toLowerCase();
    return trimmed.startsWith('/') ? trimmed.substring(1) : trimmed;
};

/**
 * 过滤掉中台管理平台不允许展示的业务系统菜单子树。
 *
 * @param routes 后端 /auth/routers 返回的顶级路由列表
 * @returns 过滤后的路由列表（保持原有顺序与嵌套结构）
 */
const stripBusinessSystemMenus = (routes: BackendRouteItem[]): BackendRouteItem[] => {
    if (!Array.isArray(routes) || routes.length === 0) {
        return [];
    }
    return routes.filter(route => {
        if (!route) return false;
        return !ADMIN_SIDEBAR_BLOCKED_PATHS.has(normalizeRoutePath(route.path));
    });
};

const buildSidebarMenus = (routes: BackendRouteItem[]): SidebarMenuItem[] => {
    return routes
        .filter(route => !route.hidden)
        .map(route => {
            const children = buildSidebarMenus(route.children || []);
            return {
                name: route.name,
                title: route.meta?.title || route.name,
                icon: route.meta?.icon || 'apps',
                hidden: !!route.hidden,
                children
            };
        });
};

/** 前端静态菜单项 — 靠前位置（插入到「工作台」之后） */
const STATIC_SIDEBAR_ITEMS_FRONT: SidebarMenuItem[] = [
    {
        name: 'LicenseManagement',
        title: '授权管理',
        icon: 'safe',
        hidden: false,
        children: []
    }
];

/** 前端静态菜单项 — 末尾位置（追加到菜单列表最后） */
const STATIC_SIDEBAR_ITEMS_TAIL: SidebarMenuItem[] = [
    {
        name: 'SystemConfig',
        title: '系统配置',
        icon: 'settings',
        hidden: false,
        children: []
    }
]; export const usePermissionStore = defineStore('permission', {
    state: (): PermissionState => ({
        dynamicRoutesLoaded: false,
        backendRoutes: [],
        sidebarMenus: [],
        buttonPermissions: [],
        buttonPermissionsLoaded: false,
    }),
    actions: {
        setBackendRoutes(routes: BackendRouteItem[]) {
            // 客户端防线：剔除业务系统菜单子树，防止其出现在中台侧边栏
            const safeRoutes = stripBusinessSystemMenus(routes || []);
            this.backendRoutes = safeRoutes;
            const dynamicMenus = buildSidebarMenus(safeRoutes);
            // 将「授权管理」插入到「工作台」之后（索引 1）
            const insertIdx = Math.min(1, dynamicMenus.length);
            dynamicMenus.splice(insertIdx, 0, ...STATIC_SIDEBAR_ITEMS_FRONT);
            // 将「系统配置」追加到菜单列表最末尾
            dynamicMenus.push(...STATIC_SIDEBAR_ITEMS_TAIL);
            this.sidebarMenus = dynamicMenus;
            this.dynamicRoutesLoaded = true;
        },
        resetRoutes() {
            this.dynamicRoutesLoaded = false;
            this.backendRoutes = [];
            this.sidebarMenus = [];
            this.buttonPermissions = [];
            this.buttonPermissionsLoaded = false;
        },

        /**
         * 设置用户按钮级权限列表。
         * 在 BusinessLayout 获取 /auth/info 后调用此方法缓存权限。
         * @param perms 后端返回的权限标识数组，如 ['system:org:list', 'system:org:edit']
         */
        setButtonPermissions(perms: string[]) {
            this.buttonPermissions = perms || [];
            this.buttonPermissionsLoaded = true;
        },

        /**
         * 判断当前用户是否拥有指定按钮权限。
         *
         * 匹配规则（与后端 PermissionService 一致）：
         * 1. 超级管理员（含 '*:*:*'）→ 拥有全部权限
         * 2. 精确匹配 → permissions 中包含该字符串
         *
         * @param perm 权限标识，如 'system:org:edit'
         * @returns true 表示有权限
         */
        hasPermission(perm: string): boolean {
            if (!perm) return true;
            if (this.buttonPermissions.includes('*:*:*')) return true;
            return this.buttonPermissions.includes(perm);
        }
    }
});
