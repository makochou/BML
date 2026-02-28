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

export const usePermissionStore = defineStore('permission', {
    state: (): PermissionState => ({
        dynamicRoutesLoaded: false,
        backendRoutes: [],
        sidebarMenus: []
    }),
    actions: {
        setBackendRoutes(routes: BackendRouteItem[]) {
            this.backendRoutes = routes;
            this.sidebarMenus = buildSidebarMenus(routes);
            this.dynamicRoutesLoaded = true;
        },
        resetRoutes() {
            this.dynamicRoutesLoaded = false;
            this.backendRoutes = [];
            this.sidebarMenus = [];
        }
    }
});
