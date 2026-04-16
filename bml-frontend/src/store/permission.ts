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

/** 前端静态菜单项（不依赖后端动态路由） */
const STATIC_SIDEBAR_ITEMS: SidebarMenuItem[] = [
    {
        name: 'LicenseManagement',
        title: '授权管理',
        icon: 'safe',
        hidden: false,
        children: []
    }
];

export const usePermissionStore = defineStore('permission', {
    state: (): PermissionState => ({
        dynamicRoutesLoaded: false,
        backendRoutes: [],
        sidebarMenus: []
    }),
    actions: {
        setBackendRoutes(routes: BackendRouteItem[]) {
            this.backendRoutes = routes;
            const dynamicMenus = buildSidebarMenus(routes);
            // 将静态菜单项「授权管理」插入到「工作台」之后、「全源资产目录」之前（索引 1）
            const insertIdx = Math.min(1, dynamicMenus.length);
            dynamicMenus.splice(insertIdx, 0, ...STATIC_SIDEBAR_ITEMS);
            this.sidebarMenus = dynamicMenus;
            this.dynamicRoutesLoaded = true;
        },
        resetRoutes() {
            this.dynamicRoutesLoaded = false;
            this.backendRoutes = [];
            this.sidebarMenus = [];
        }
    }
});
