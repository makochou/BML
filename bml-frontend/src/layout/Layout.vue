<template>
  <a-layout class="layout-demo">
    <!-- 不使用 collapsible / breakpoint，避免 Arco 进入 drawer 模式生成 mask 遮罩层拦截点击事件。
         折叠由自定义 collapse-btn 按钮控制。 -->
    <a-layout-sider 
        :collapsed="collapsed" 
        :class="[`sidebar-${appStore.sidebarTheme}`]"
    >
      <!-- 点击 Logo 返回中台工作台首页 -->
      <div class="logo" style="cursor: pointer;" @click="$router.push('/admin')">BML中台管理</div>
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
                <!-- 主题配置 -->
                <div class="dock-item" @click="appStore.toggleSettings(true)" title="主题设置">
                    <icon-palette />
                </div>

                <!-- 全屏 -->
                <div class="dock-item" @click="toggleFullscreen">
                    <component :is="isFullscreen ? IconFullscreenExit : IconFullscreen" />
                </div>

                <!-- 通知铃铛（带未读徽标） -->
                <a-badge :count="notificationStore.unreadCount" :max-count="99" dot :offset="[-2, 2]">
                    <div class="dock-item" @click="notificationStore.openDrawer()" title="通知中心">
                        <icon-notification />
                    </div>
                </a-badge>

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
                        <!-- 用户信息头部 -->
                        <div class="bml-dropdown-user-header">
                            <div class="bml-dropdown-user-avatar">
                                <a-avatar :size="32"><icon-user /></a-avatar>
                            </div>
                            <div class="bml-dropdown-user-info">
                                <span class="bml-dropdown-user-name">管理员</span>
                                <span class="bml-dropdown-user-role">中台管理员</span>
                            </div>
                        </div>
                        <!-- 分割线 -->
                        <div class="bml-dropdown-divider"></div>
                        <!-- 退出登录 -->
                        <a-doption @click="handleLogout" class="bml-dropdown-item-danger">
                            <template #icon><icon-export /></template>
                            退出登录
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
          <router-view v-slot="{ Component, route: currentRoute }">
            <!--
              keep-alive 不限制 include，缓存所有打开过的页面组件。
              通过 :max 限制最大缓存数量，防止内存占用过多。
              :key 使用路由 name，确保同一路由复用同一组件实例。
            -->
            <keep-alive :max="20">
              <component :is="Component" :key="String(currentRoute.name)" />
            </keep-alive>
          </router-view>
        </div>
      </a-layout-content>
    </a-layout>
    <ThemeSettings />

    <!-- 右上角告警弹窗（新告警自动弹出） -->
    <AlertToast />

    <!-- 通知中心侧边抽屉 -->
    <a-drawer
      :visible="notificationStore.drawerVisible"
      :width="960"
      :footer="false"
      :header="false"
      :mask-closable="true"
      unmount-on-close
      @cancel="notificationStore.closeDrawer()"
    >
      <NotificationPanel />
    </a-drawer>
  </a-layout>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import {
    IconList, IconBug, IconApps, IconExport, IconDashboard,
    IconLeft, IconRight, IconNotification, IconFullscreen, IconFullscreenExit, IconUser, IconDown,
    IconSettings, IconPalette, IconSafe, IconLayers, IconDesktop, IconIdcard, IconBranch
} from '@arco-design/web-vue/es/icon';
import request from '../utils/request';
import TagsView from '../components/TagsView.vue';
import ThemeSettings from '../components/ThemeSettings.vue';
import AlertToast from '../components/AlertToast.vue';
import NotificationPanel from '../components/NotificationPanel.vue';
import { useTagsViewStore } from '../store/tagsView';
import { useAppStore } from '../store/app';
import { useNotificationStore } from '../store/notification';
import { usePermissionStore, type SidebarMenuItem } from '../store/permission';
import { clearAuthTokens } from '../utils/auth';
import { resetDynamicRoutes } from '../router';
import { useAdminIdleTimeout } from '../composables/useAdminIdleTimeout';

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

// 调试：监听路由变化和缓存列表，帮助排查 keep-alive 问题
watch(
  () => route.name,
  (newName) => {
    console.debug('[Layout] 路由切换 →', newName);
    console.debug('[Layout] cachedViews:', JSON.stringify(cachedViews.value));
  },
  { immediate: true }
);

const menuIconMap: Record<string, any> = {
  // 通用
  dashboard: IconDashboard,
  monitor: IconDashboard,
  list: IconList,
  bug: IconBug,
  apps: IconApps,
  notification: IconNotification,
  setting: IconSettings,
  settings: IconSettings,
  user: IconUser,
  // 新增菜单图标
  layers: IconLayers,
  safe: IconSafe,
  desktop: IconDesktop,
  idcard: IconIdcard,
  branch: IconBranch,
  menu: IconList,
  // 兜底
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
    tagsViewStore.delAllViews();
    Message.success('已退出登录');
    router.push('/admin/login');
  }
};

/**
 * 中台管理员空闲超时检测
 * 从配置文件读取超时时长，超时后自动登出
 */
const { start: startIdleWatch, stop: stopIdleWatch } = useAdminIdleTimeout({
  onIdle: () => {
    Message.warning('您已长时间未操作，系统已自动退出登录');
    handleLogout();
  }
});

/**
 * 生命周期：恢复主题并启动空闲检测
 */
onMounted(() => {
    appStore.initTheme();
    // 启动中台管理员空闲超时检测
    startIdleWatch();
    // 启动告警通知轮询（每 30 秒拉取增量告警，驱动铃铛 Badge 和 Toast 弹窗）
    notificationStore.startPolling();
});

onUnmounted(() => {
    stopIdleWatch();
    // 停止告警通知轮询
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
  /* 背景纹理跟随主题色 */
  background-image: 
    radial-gradient(circle at 10% 20%, rgba(var(--bml-primary-rgb, 22, 93, 255), 0.06) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(var(--bml-primary-rgb, 22, 93, 255), 0.04) 0%, transparent 40%);
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
/* 安全兜底：确保 Arco sider 在任何情况下都不会生成遮罩层阻断内容区交互 */
.layout-demo :deep(.arco-layout-sider-mask) {
    display: none !important;
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
    box-shadow: 0 12px 32px rgba(var(--bml-primary-rgb, 22, 93, 255), 0.25);
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
    background-color: var(--bml-selected-bg, rgba(22, 93, 255, 0.08)) !important;
    color: var(--bml-primary, #165dff) !important;
    font-weight: 700 !important;
    border: 1px solid rgba(var(--bml-primary-rgb, 22, 93, 255), 0.1) !important;
    box-shadow: 0 4px 10px rgba(var(--bml-primary-rgb, 22, 93, 255), 0.05);
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
  height: calc(100vh - 48px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 标签页与内容容器 */
.tags-view-wrapper {
  flex-shrink: 0;
  height: 44px;
  margin-top: -6px;
  z-index: 98;
}

/* 主内容区域 - 明确计算高度，避免 flex 子项高度塌陷 */
.page-container {
  /* 总高度 = 内容区高度 - 标签栏实际占用高度(44px - 6px偏移 = 38px) */
  height: calc(100vh - 48px - 38px);
  overflow: auto;
  position: relative;
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
/* ── 暗色模式下侧边栏耒合 ── */
.sidebar-dark :deep(.arco-layout-sider),
.sidebar-dark:deep(.arco-layout-sider) {
    backdrop-filter: none;
}
.sidebar-dark .collapse-btn {
    background: rgba(60, 60, 62, 0.85);
    border-color: rgba(255,255,255,0.1);
    color: #c9cdd4;
}
.sidebar-dark .collapse-btn:hover {
    background: rgba(80, 80, 84, 0.95);
    color: #fff;
}
/* 主色侧边栏下折叠按钮适配 */
.sidebar-primary .collapse-btn {
    background: rgba(255, 255, 255, 0.2);
    border-color: rgba(255,255,255,0.3);
    color: #fff;
}
.sidebar-primary .collapse-btn:hover {
    background: rgba(255, 255, 255, 0.35);
    color: #fff;
}
/* 暗色侧边栏 mini dock 活动状态 */
.sidebar-dark .mini-item { color: #c9cdd4; }
.sidebar-dark .mini-item:hover { background: rgba(255,255,255,0.08); color: #fff; }
/* 主色侧边栏 mini dock 活动状态 */
.sidebar-primary .mini-item { color: rgba(255,255,255,0.8); }
.sidebar-primary .mini-item:hover { background: rgba(255,255,255,0.1); color: #fff; }
.sidebar-primary .mini-item.active {
    background: rgba(255,255,255,0.25) !important;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
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
    background: var(--bml-gradient-alt, linear-gradient(180deg, var(--arcoblue-6) 0%, var(--arcoblue-5) 100%)) !important;
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
