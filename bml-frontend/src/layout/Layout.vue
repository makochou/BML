<template>
  <a-layout class="layout-demo">
    <a-layout-sider 
        collapsible 
        breakpoint="xl" 
        :collapsed="collapsed" 
        @collapse="onCollapse"
        :class="[`sidebar-${appStore.sidebarTheme}`]"
    >
      <div class="logo">BML中台管理</div>
      <!-- 展开状态：使用 Arco 标准菜单 -->
      <a-menu
        v-if="!collapsed"
        :default-selected-keys="['Dashboard']"
        :selected-keys="[currentRouteName]"
        :style="{ width: '100%' }"
        @menu-item-click="onClickMenuItem"
      >
        <template v-for="menu in sidebarMenus" :key="menu.name">
          <a-sub-menu v-if="menu.children.length > 0" :key="menu.name">
            <template #icon>
              <component :is="resolveMenuIcon(menu.icon)" />
            </template>
            <template #title>{{ menu.title }}</template>
            <a-menu-item
              v-for="child in menu.children"
              :key="child.name"
            >
              <template #icon>
                <component :is="resolveMenuIcon(child.icon)" />
              </template>
              {{ child.title }}
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="menu.name">
            <template #icon>
              <component :is="resolveMenuIcon(menu.icon)" />
            </template>
            {{ menu.title }}
          </a-menu-item>
        </template>
      </a-menu>

      <!-- 收起状态：完全自定义的 Mini Dock (Vision Pro 风格) -->
      <div v-else class="mini-menu">
        <div 
            v-for="menu in sidebarMenus"
            :key="menu.name"
            class="mini-item" 
            :class="{ active: isMenuActive(menu) }"
            @click="onClickMenuItem(resolveMiniRouteName(menu))"
        >
            <component :is="resolveMenuIcon(menu.icon)" />
        </div>
      </div>
      
      <!-- 自定义悬浮伸缩按钮 (绝对定位) -->
      <div class="collapse-btn" @click="toggleCollapse">
        <component :is="collapsed ? IconRight : IconLeft" />
      </div>

    </a-layout-sider>
    <a-layout>
      <a-layout-header :class="[`header-${appStore.headerTheme}`]">
        <!-- 左侧：纯净标题区 -->
        <div class="header-left">
          <div class="page-title-area">
             <div class="title-decoration"></div>
             <a-breadcrumb class="modern-breadcrumb">
                <a-breadcrumb-item>
                    <span class="breadcrumb-root">BML</span>
                </a-breadcrumb-item>
                <a-breadcrumb-item v-if="currentRouteName !== 'Dashboard'">
                    <span class="breadcrumb-active">{{ route.meta.title }}</span>
                </a-breadcrumb-item>
            </a-breadcrumb>
          </div>
        </div>

        <!-- 右侧：悬浮控制坞 (Glass Dock) -->
        <div class="header-right">
            <div class="glass-dock">
                <!-- 搜索 -->
                <div class="dock-item search-trigger">
                    <icon-search />
                </div>
                
                <div class="dock-divider"></div>

                <!-- 消息通知 -->
                <div class="dock-item" @click="notificationStore.openDrawer()">
                    <a-badge :count="notificationStore.unreadCount" :max-count="99" dot :offset="[4, -4]">
                        <icon-notification />
                    </a-badge>
                </div>



                <!-- 主题配置 -->
                <div class="dock-item" @click="appStore.toggleSettings(true)">
                    <icon-settings />
                </div>

                <!-- 全屏 -->
                <div class="dock-item" @click="toggleFullscreen">
                    <component :is="isFullscreen ? IconFullscreenExit : IconFullscreen" />
                </div>

                <div class="dock-divider"></div>

                <!-- 用户 -->
                <a-dropdown trigger="hover">
                    <div class="dock-item user-item">
                        <a-avatar :size="28" class="user-avatar">
                            <icon-user />
                        </a-avatar>
                        <icon-down class="user-arrow" />
                    </div>
                     <template #content>
                        <a-doption><template #icon><icon-user /></template>个人中心</a-doption>
                        <a-doption><template #icon><icon-settings /></template>系统设置</a-doption>
                        <a-divider style="margin: 4px 0;" />
                        <a-doption @click="handleLogout" style="color: #f53f3f;">
                            <template #icon><icon-export /></template>退出登录
                        </a-doption>
                    </template>
                </a-dropdown>
            </div>
        </div>
      </a-layout-header>

      <a-layout-content>
        <div class="tags-view-wrapper">
          <TagsView />
        </div>
        <div class="page-container">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <keep-alive :include="cachedViews">
                <component :is="Component" :key="route.fullPath" />
              </keep-alive>
            </transition>
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
    <ThemeSettings />

    <!-- 通知中心右侧抽屉（与 ThemeSettings 同级） -->
    <a-drawer
      class="ultra-premium-drawer notify-drawer"
      :width="860"
      :visible="notificationStore.drawerVisible"
      unmount-on-close
      :footer="false"
      :header="false"
      @cancel="notificationStore.closeDrawer()"
    >
      <NotificationPanel />
    </a-drawer>

    <!-- 右下角告警弹窗（Teleport 到 body，不影响布局） -->
    <AlertToast />
  </a-layout>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import {
    IconList, IconBug, IconApps, IconExport, IconDashboard,
    IconLeft, IconRight, IconNotification, IconFullscreen, IconFullscreenExit, IconUser, IconDown,
    IconSettings, IconSearch
} from '@arco-design/web-vue/es/icon';
import request from '../utils/request';
import TagsView from '../components/TagsView.vue';
import ThemeSettings from '../components/ThemeSettings.vue';
import NotificationPanel from '../components/NotificationPanel.vue';
import AlertToast from '../components/AlertToast.vue';
import { useTagsViewStore } from '../store/tagsView';
import { useAppStore } from '../store/app';
import { useNotificationStore } from '../store/notification';
import { usePermissionStore, type SidebarMenuItem } from '../store/permission';
import { clearAuthTokens } from '../utils/auth';
import { resetDynamicRoutes } from '../router';

const router = useRouter();
const route = useRoute();
const collapsed = ref(false);
const isFullscreen = ref(false);

const tagsViewStore = useTagsViewStore();
const appStore = useAppStore();
const notificationStore = useNotificationStore();
const permissionStore = usePermissionStore();
const cachedViews = computed(() => tagsViewStore.cachedViews);
const sidebarMenus = computed(() => permissionStore.sidebarMenus);

const currentRouteName = computed(() => (route.name as string) || '');

const menuIconMap: Record<string, any> = {
  dashboard: IconDashboard,
  monitor: IconDashboard,
  list: IconList,
  bug: IconBug,
  apps: IconApps,
  notification: IconNotification,
  setting: IconSettings,
  settings: IconSettings,
  user: IconUser,
  peoples: IconUser,
  tree: IconApps,
  'tree-table': IconApps,
  dict: IconApps,
  edit: IconSettings,
  form: IconApps,
  logininfor: IconApps
};

const resolveMenuIcon = (icon?: string) => {
  if (!icon) {
    return IconApps;
  }
  return menuIconMap[icon.toLowerCase()] || IconApps;
};

const onCollapse = (val: boolean) => {
  collapsed.value = val;
};

const toggleCollapse = () => {
    collapsed.value = !collapsed.value;
}

const onClickMenuItem = (key: string) => {
  router.push({ name: key });
};

const resolveMiniRouteName = (menu: SidebarMenuItem): string => {
  if (menu.children.length > 0) {
    const firstChild = menu.children[0];
    return firstChild ? firstChild.name : menu.name;
  }
  return menu.name;
};

const isMenuActive = (menu: SidebarMenuItem): boolean => {
  if (menu.name === currentRouteName.value) {
    return true;
  }
  return menu.children.some(child => child.name === currentRouteName.value);
};



const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
    isFullscreen.value = true;
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
      isFullscreen.value = false;
    }
  }
};

const handleLogout = async () => {
  try {
    await request.post('/auth/logout');
  } catch (e) {
    // 忽略登出失败
  } finally {
    clearAuthTokens();
    resetDynamicRoutes();
    permissionStore.resetRoutes();
    notificationStore.stopPolling(); // 退出时停止通知轮询
    tagsViewStore.delAllViews();
    Message.success('已退出登录');
    router.push('/admin/login');
  }
};

/**
 * 生命周期：启动通知轮询 + 恢复主题
 */
onMounted(() => {
    notificationStore.startPolling();
    appStore.initTheme();
});

/**
 * 生命周期：停止通知轮询
 * 离开 Layout（如刷新页面）时清理定时器
 */
onUnmounted(() => {
    notificationStore.stopPolling();
});
</script>

<style scoped>
.layout-demo {
  height: 100vh;
  width: 100vw;
  background-color: #f7f8fa; /* Extremely light grey for depth */
  display: flex;
  overflow: hidden;
  /* 背景纹理 - 更加细腻 */
  background-image: 
    radial-gradient(circle at 10% 20%, rgba(226, 240, 255, 0.4) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(240, 230, 255, 0.4) 0%, transparent 40%);
}

/* 侧边栏 - Vision Pro 毛玻璃风格 */
.layout-demo :deep(.arco-layout-sider) {
    background: rgba(255, 255, 255, 0.65);
    backdrop-filter: blur(24px) saturate(180%);
    -webkit-backdrop-filter: blur(24px) saturate(180%);
    border-right: 1px solid rgba(255, 255, 255, 0.4);
    box-shadow: 20px 0 40px rgba(0, 0, 0, 0.02);
    z-index: 100;
    height: 100%;
    position: relative;
}

/* 隐藏所有默认滚动条 */
.layout-demo :deep(.arco-layout-sider-children) {
    overflow-y: auto !important;
    overflow-x: hidden !important;
    padding-bottom: 60px;
    scrollbar-width: none;
    -ms-overflow-style: none;
}
.layout-demo :deep(.arco-layout-sider-children)::-webkit-scrollbar {
    display: none;
    width: 0 !important;
    height: 0 !important;
}
.layout-demo :deep(.arco-layout-sider)::-webkit-scrollbar {
    display: none;
    width: 0 !important;
    height: 0 !important;
}

/* 隐藏默认 Trigger */
.layout-demo :deep(.arco-layout-sider-trigger) {
    display: none;
}

/* 自定义悬浮收缩按钮 */
.collapse-btn {
    position: absolute;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08), inset 0 0 0 1px rgba(255,255,255,0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #4e5969;
    font-size: 16px;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    z-index: 101;
}

.collapse-btn:hover {
    width: 48px;
    height: 48px;
    background: #fff;
    color: var(--bml-primary, #165dff);
    box-shadow: 0 12px 32px rgba(22, 93, 255, 0.25);
}

/* Logo */
.layout-demo :deep(.arco-layout-sider-collapsed) .logo {
    font-size: 0 !important;
    width: 100% !important;
    height: 64px !important;
    padding: 0 !important;
    display: flex !important;
    justify-content: center !important;
    align-items: center !important;
    position: relative !important;
}
.layout-demo :deep(.arco-layout-sider-collapsed) .logo::after {
    content: 'B';
    font-size: 24px;
    font-weight: 900;
    color: var(--bml-primary, #165dff);
    display: block;
    position: absolute !important;
    left: 50% !important;
    top: 50% !important;
    transform: translate(-50%, -50%) !important;
    margin: 0 !important;
    line-height: normal !important;
}

.layout-demo :deep(.arco-layout-sider) .logo {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 2px;
  background: var(--bml-gradient-alt, linear-gradient(135deg, #165dff 0%, #722ed1 100%));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  cursor: pointer;
  transition: transform 0.3s;
}
.layout-demo :deep(.arco-layout-sider) .logo:hover {
    transform: scale(1.05);
}

/* 菜单 */
.layout-demo :deep(.arco-menu) {
    background: transparent;
    padding: 12px;
}
.layout-demo :deep(.arco-menu-inner) {
    padding: 0;
}
.layout-demo :deep(.arco-menu-item) {
    height: 46px;
    line-height: normal;
    margin-bottom: 8px;
    width: 92%;
    margin-left: 4%;
    border-radius: 12px;
    color: #4e5969;
    font-weight: 500;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    border: 1px solid transparent;
    display: flex;
    align-items: center;
    padding: 0 12px !important; /* Recover space on the left */
}
.layout-demo :deep(.arco-menu-item:hover) {
    background-color: rgba(255, 255, 255, 0.6);
    color: #1d2129;
    transform: translateX(4px);
}
.layout-demo :deep(.arco-menu-item.arco-menu-selected) {
    background: var(--bml-gradient, linear-gradient(135deg, #165dff 0%, #3c7eff 100%));
    color: #fff;
    box-shadow: 0 4px 12px var(--bml-shadow, rgba(22, 93, 255, 0.3));
    font-weight: 600;
    transform: translateX(0);
}
.layout-demo :deep(.arco-menu-item.arco-menu-selected:hover) {
    background: var(--bml-gradient, linear-gradient(135deg, #165dff 0%, #3c7eff 100%));
    box-shadow: 0 6px 16px var(--bml-shadow, rgba(22, 93, 255, 0.4));
    transform: translateY(-1px);
}
.layout-demo :deep(.arco-menu-item .arco-icon) {
    font-size: 18px;
    margin-right: 4px; /* Move text closer to icon */
    flex-shrink: 0;
    transition: all 0.3s;
}
.layout-demo :deep(.arco-menu-item.arco-menu-selected .arco-icon) {
    fill: #fff;
    transform: scale(1.1);
}

/* 
 * Parent Menu Active State (Selected Sub-menu Header)
 * 核心修复：Arco Vue 实际会将 .arco-menu-selected 加在 .arco-menu-inline-header 上
 */
/* 基础一级菜单结构对齐普通 menu-item */
.layout-demo :deep(.arco-menu-inline-header) {
    height: 46px;
    line-height: normal;
    margin-bottom: 8px;
    width: 92%;
    margin-left: 4%;
    border-radius: 12px;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    display: flex;
    align-items: center;
    padding: 0 12px !important;
}

/* 覆盖子级菜单左侧间距，平衡层级感与展示空间 (12px + 16px = 28px) */
.layout-demo :deep(.arco-menu-inline-content .arco-menu-item) {
    padding-left: 28px !important; 
}

/* 彻底移除 Arco 定义的各种横向占位符，杜绝 text-align 无效的问题 */
.layout-demo :deep(.arco-menu-indent),
.layout-demo :deep(.arco-menu-icon-empty),
.layout-demo :deep(.arco-menu-indent-list) {
    display: none !important;
    width: 0 !important;
}

.layout-demo :deep(.arco-menu-inline-header.arco-menu-selected) {
    background-color: rgba(22, 93, 255, 0.08) !important; /* 加深背景，确保可见 */
    color: var(--bml-primary, #165dff) !important;
    font-weight: 700 !important;
    border: 1px solid rgba(22, 93, 255, 0.1) !important;
    box-shadow: 0 4px 10px rgba(22, 93, 255, 0.05);
}

/* 在激活的父级菜单左侧增加一个小批注/标志，使其更显眼 */
.layout-demo :deep(.arco-menu-inline-header.arco-menu-selected)::before {
    content: '';
    position: absolute;
    left: 4px;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 16px;
    background: var(--bml-primary, #165dff);
    border-radius: 2px;
}

/* 激活状态下的图标强制变色 */
.layout-demo :deep(.arco-menu-inline-header.arco-menu-selected .arco-icon) {
    color: var(--bml-primary, #165dff) !important;
    fill: var(--bml-primary, #165dff) !important;
    transform: scale(1.1);
}

/* 箭头也要变色 */
.layout-demo :deep(.arco-menu-inline-header.arco-menu-selected .arco-menu-icon-suffix) {
    color: var(--bml-primary, #165dff) !important;
}

/* 悬浮效果增强 */
.layout-demo :deep(.arco-menu-inline-header:hover) {
    background-color: rgba(255, 255, 255, 0.6);
    color: #1d2129;
    transform: translateX(4px);
}

/* =================================================================
   Header Styles V4 - Future Minimalist ("Glass Dock")
   ================================================================= */
.layout-demo :deep(.arco-layout-header) {
  height: 48px; /* Extremely compact to push content up */
  background: transparent; /* Totally clear */
  border-bottom: none;
  display: flex;
  align-items: flex-start; /* Align items to the top instead of center */
  padding: 4px 24px 0 32px; /* Reduced top padding from 12px to 4px */
  justify-content: space-between;
  z-index: 99;
}

/* Left: Page Title Area */
.page-title-area {
    display: flex;
    align-items: center;
    position: relative;
    padding-left: 12px;
    margin-top: 4px; /* Reduced from 12px to push the title further up */
}
.title-decoration {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 18px;
    background: var(--bml-primary, #165dff);
    border-radius: 4px;
    box-shadow: 0 2px 8px var(--bml-shadow, rgba(22, 93, 255, 0.4));
}
.modern-breadcrumb .breadcrumb-root {
    font-size: 14px;
    color: #86909c;
    font-weight: 400;
}
.modern-breadcrumb .breadcrumb-active {
    font-size: 18px; /* Large Title */
    color: #1d2129;
    font-weight: 600; /* Bold */
    letter-spacing: 0.2px;
}
/* Override Arcos separator */
.modern-breadcrumb :deep(.arco-breadcrumb-item-separator) {
    color: #c9cdd4;
    margin: 0 8px;
}

/* Right: Glass Dock */
.glass-dock {
    display: flex;
    align-items: center;
    height: 48px;
    padding: 0 8px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(24px);
    border: 1px solid rgba(255, 255, 255, 0.9);
    border-radius: 24px; /* Fully rounded capsule */
    box-shadow: 
        0 4px 12px -2px rgba(0, 0, 0, 0.03),
        0 12px 32px rgba(0, 0, 0, 0.05),
        inset 0 0 0 1px rgba(255, 255, 255, 0.6);
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    /* Push to top right */
    position: absolute;
    top: 8px;
    right: 12px;
}

.glass-dock:hover {
    transform: translateY(-1px);
    box-shadow: 
        0 8px 16px -2px rgba(0, 0, 0, 0.04),
        0 20px 48px rgba(0, 0, 0, 0.08),
        inset 0 0 0 1px rgba(255, 255, 255, 0.8);
    background: rgba(255, 255, 255, 0.95);
}

/* Dock Items */
.dock-item {
    width: 36px;
    height: 36px;
    border-radius: 50%; /* Circle */
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 4px;
    color: #4e5969; /* Soft grey */
    font-size: 16px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.dock-item:hover {
    background: rgba(0, 0, 0, 0.06);
    color: #1d2129;
    transform: scale(1.05); /* Gentle pop */
}

/* Special Items */
.search-trigger {
    color: #4e5969;
}
.user-item {
    width: auto; /* Allow expansion */
    padding: 0 4px 0 0;
    border-radius: 36px; /* Pill */
    margin-right: 0;
}
.user-item:hover {
    background: rgba(0, 0, 0, 0.04);
}
.user-avatar {
    margin-right: 0;
    transition: transform 0.3s;
    background-color: var(--bml-primary, #165dff) !important; /* Force brand color */
}
.user-item:hover .user-avatar {
    transform: rotate(10deg); /* Playful rotation */
}
.user-arrow {
    font-size: 10px;
    margin-left: 4px;
    color: #86909c;
}

/* Divider inside Dock */
.dock-divider {
    width: 1px;
    height: 16px;
    background: #e5e6eb;
    margin: 0 8px;
}

/* 内容区 */
.layout-demo :deep(.arco-layout-content) {
  background: transparent;
  padding: 0;
  height: calc(100vh - 48px); /* Correct calculation for taller header */
  overflow: hidden; /* Hide outer scrollbar to allow inner content to manage it */
  display: flex;
  flex-direction: column;
}

/* 标签页与内容容器 */
.tags-view-wrapper {
  flex-shrink: 0;
  height: 44px;
  margin-top: -6px; /* Pull it up aggressively to close the gap */
  z-index: 98;
}

/* 主内容区域 - 占据剩余空间 */
.page-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0; /* Clear default padding if any */
}

/* Transition */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* Mini Menu */
.mini-menu {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 10px;
}
.mini-item {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    color: #4e5969;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    position: relative;
}
.mini-item:hover {
    background: rgba(0,0,0,0.05);
    transform: scale(1.1);
}
.mini-item.active {
    background: var(--bml-gradient, linear-gradient(135deg, #165dff 0%, #3c7eff 100%));
    color: #fff;
    box-shadow: 0 4px 12px var(--bml-shadow, rgba(22, 93, 255, 0.3));
}
.layout-demo :deep(.arco-layout-sider-collapsed) .logo::after {
    content: 'B'; /* 显示缩写 */
    font-size: 24px;
    font-weight: 900;
    color: var(--bml-primary, #165dff);
    display: block;
    position: absolute !important;
    left: 50% !important;
    top: 50% !important;
    transform: translate(-50%, -50%) !important;
    margin: 0 !important;
    line-height: normal !important;
}
/* Dark Mode Header */
:global(body[arco-theme='dark']) .modern-breadcrumb .breadcrumb-active { color: #f2f3f5; }
:global(body[arco-theme='dark']) .glass-dock {
    background: rgba(35, 35, 36, 0.6);
    border: 1px solid rgba(255, 255, 255, 0.08);
    box-shadow: 
        0 4px 12px -2px rgba(0, 0, 0, 0.2),
        inset 0 0 0 1px rgba(255, 255, 255, 0.05);
}
:global(body[arco-theme='dark']) .glass-dock:hover {
    background: rgba(35, 35, 36, 0.8);
    box-shadow: 
        0 8px 16px -2px rgba(0, 0, 0, 0.3),
        inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}
:global(body[arco-theme='dark']) .dock-item { color: #86909c; }
:global(body[arco-theme='dark']) .dock-item:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #f2f3f5;
}
:global(body[arco-theme='dark']) .dock-divider { background: #3a3a3b; }

/* =================================================================
   Dynamic Theme Styles (New)
   ================================================================= */

/* Header Themes */
.header-white {
    background: rgba(255, 255, 255, 0.8) !important;
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(0,0,0,0.05) !important;
}
.header-dark {
    background: rgba(23, 23, 26, 0.9) !important;
    backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(255,255,255,0.05) !important;
}
.header-dark .breadcrumb-root { color: #86909c !important; }
.header-dark .breadcrumb-active { color: #f2f3f5 !important; }
.header-dark .dock-item { color: #c9cdd4 !important; }
.header-dark .dock-item:hover { background: rgba(255,255,255,0.1) !important; color: #fff !important; }

.header-primary {
    background: linear-gradient(90deg, var(--arcoblue-6) 0%, var(--arcoblue-5) 100%) !important;
    color: #fff !important;
}
.header-primary .breadcrumb-root { color: rgba(255,255,255,0.7) !important; }
.header-primary .breadcrumb-active { color: #fff !important; }
.header-primary .dock-item { color: rgba(255,255,255,0.9) !important; }
.header-primary .dock-item:hover { background: rgba(255,255,255,0.2) !important; }
.header-primary .glass-dock {
    background: rgba(255,255,255,0.15) !important;
    border: 1px solid rgba(255,255,255,0.3) !important;
}

/* Sidebar Themes */
.sidebar-white :deep(.arco-layout-sider) {
    background: #fff !important;
    border-right: 1px solid rgba(0,0,0,0.05) !important;
}
.sidebar-dark :deep(.arco-layout-sider) {
    background: #232324 !important;
    border-right: 1px solid rgba(255,255,255,0.05) !important;
}
.sidebar-dark :deep(.arco-menu-item) { color: #c9cdd4 !important; }
.sidebar-dark :deep(.arco-menu-item:hover) { background: rgba(255,255,255,0.05) !important; color: #fff !important; }
.sidebar-dark .logo { color: #fff !important; }

.sidebar-primary {
    background: linear-gradient(180deg, var(--arcoblue-6) 0%, var(--arcoblue-5) 100%) !important;
    border-right: none !important;
}
.sidebar-primary :deep(.arco-menu-inner) { background: transparent !important; }
.sidebar-primary :deep(.arco-menu-item) { color: rgba(255,255,255,0.8) !important; background: transparent; }
.sidebar-primary :deep(.arco-menu-item:hover) { background: rgba(255,255,255,0.1) !important; color: #fff !important; }
.sidebar-primary :deep(.arco-menu-item.arco-menu-selected) { 
    background: rgba(255,255,255,0.2) !important; 
    color: #fff !important; 
}
.sidebar-primary :deep(.arco-icon) { color: inherit !important; }
.sidebar-primary .logo { color: #fff !important; }

/* Resolve submenu icon color in primary sidebar and active state */
.sidebar-primary :deep(.arco-menu-inline-header) { 
    color: rgba(255,255,255,0.8); 
}
.sidebar-primary :deep(.arco-menu-inline-header.arco-menu-selected) {
    background: rgba(255, 255, 255, 0.15) !important;
    color: #fff !important;
    border-color: rgba(255, 255, 255, 0.2) !important;
}
.sidebar-primary :deep(.arco-menu-inline-header.arco-menu-selected .arco-icon),
.sidebar-primary :deep(.arco-menu-inline-header.arco-menu-selected .arco-menu-icon-suffix) {
    color: #fff !important;
}
.sidebar-primary :deep(.arco-menu-inline-header:hover) { 
    color: #fff; 
    background: rgba(255,255,255,0.1); 
}
</style>
