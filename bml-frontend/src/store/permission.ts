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
];

export const usePermissionStore = defineStore('permission', {
    state: (): PermissionState => ({
        dynamicRoutesLoaded: false,
        backendRoutes: [],
        sidebarMenus: [],
        buttonPermissions: [],
        buttonPermissionsLoaded: false,
    }),
    actions: {
        setBackendRoutes(routes: BackendRouteItem[]) {
            this.backendRoutes = routes;
            const dynamicMenus = buildSidebarMenus(routes);
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
