<template>
  <!-- ======================================================================
       BML 业务系统布局（与中台管理保持一致的 Vision Pro 毛玻璃风格）
       ======================================================================
       设计特点：
         1. 全局柔光纹理背景 + 毛玻璃侧边栏
         2. 渐变品牌 Logo + hover 缩放
         3. 透明 Header + Glass Dock 胶囊控制栏
         4. 菜单渐变选中态 + hover 位移动画
         5. 收缩按钮毛玻璃悬浮效果
         6. 收起模式下 Mini Dock 图标导航
       ====================================================================== -->
  <a-layout class="biz-layout">
    <!-- 注意：不使用 collapsible / breakpoint，避免 Arco 进入 drawer 模式导致生成 mask 遮罩层拦截点击事件。
         折叠由自定义 collapse-btn 按钮控制。 -->
    <a-layout-sider :collapsed="collapsed" :collapsed-width="64">
      <!-- 点击 Logo 返回业务系统工作台首页 -->
      <div class="logo" @click="goHome">
        <!-- 若配置了侧边栏 Logo 图片则显示图片，否则回退为渐变文字 -->
        <img v-if="sidebarLogoUrl && !collapsed" :src="sidebarLogoUrl" alt="Logo" class="logo-img" />
        <img v-else-if="sidebarLogoUrl && collapsed" :src="sidebarLogoUrl" alt="Logo" class="logo-img-mini" />
        <template v-else>
          <span v-if="!collapsed" class="logo-text">BML 业务系统</span>
        </template>
      </div>

      <!-- 展开状态：使用 Arco 标准菜单 -->
      <a-menu
        v-if="!collapsed"
        :selected-keys="selectedKeys"
        :default-open-keys="openKeys"
        :auto-open-selected="true"
        @menu-item-click="onMenuClick"
      >
        <a-sub-menu key="system">
          <template #icon><icon-settings /></template>
          <template #title>组织与权限</template>
          <a-menu-item key="SystemOrg"><template #icon><icon-apps /></template>机构管理</a-menu-item>
          <a-menu-item key="SystemDept"><template #icon><icon-branch /></template>部门管理</a-menu-item>
          <a-menu-item key="SystemPost"><template #icon><icon-idcard /></template>岗位管理</a-menu-item>
          <a-menu-item key="SystemUser"><template #icon><icon-user /></template>用户管理</a-menu-item>
          <a-menu-item key="SystemRole"><template #icon><icon-safe /></template>角色与权限</a-menu-item>
          <a-menu-item key="SystemMenu"><template #icon><icon-menu /></template>菜单管理</a-menu-item>
        </a-sub-menu>
      </a-menu>

      <!-- 收起状态：Mini Dock 图标导航 -->
      <div v-else class="mini-menu">
        <div
          class="mini-item"
          :class="{ active: selectedKeys.some(k => ['SystemOrg','SystemDept','SystemPost','SystemUser','SystemRole','SystemMenu'].includes(k)) }"
          @click="onMenuClick('SystemOrg')"
          title="组织与权限"
        >
          <icon-settings />
        </div>
      </div>

      <!-- 自定义悬浮伸缩按钮 -->
      <div class="collapse-btn" @click="collapsed = !collapsed">
        <component :is="collapsed ? IconRight : IconLeft" />
      </div>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部栏：透明 + Glass Dock -->
      <a-layout-header>
        <!-- 左侧：纯净标题区 -->
        <div class="header-left">
          <div class="page-title-area">
            <div class="title-decoration"></div>
            <a-breadcrumb class="modern-breadcrumb">
              <!-- 若路由定义了 parentTitle（一级菜单名），显示为面包屑根节点 -->
              <a-breadcrumb-item v-if="parentTitle">
                <span class="breadcrumb-root">{{ parentTitle }}</span>
              </a-breadcrumb-item>
              <a-breadcrumb-item v-if="pageTitle">
                <span :class="parentTitle ? 'breadcrumb-active' : 'breadcrumb-active-solo'">{{ pageTitle }}</span>
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
            <div class="dock-item" @click="toggleFullscreen" title="全屏切换">
              <component :is="isFullscreen ? IconFullscreenExit : IconFullscreen" />
            </div>

            <div class="dock-divider"></div>

            <!-- 用户 -->
            <a-dropdown trigger="hover">
              <div class="dock-item user-item">
                <a-avatar :size="28" class="user-avatar">
                  <icon-user />
                </a-avatar>
                <span v-if="userInfo" class="user-name">{{ userInfo.nickname || userInfo.username }}</span>
                <icon-down class="user-arrow" />
              </div>
              <template #content>
                <a-doption @click="handleLogout" style="color: #f53f3f;">
                  <template #icon><icon-export /></template>退出登录
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content>
        <!-- 标签页导航栏（与中台一致） -->
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
    <!-- 主题设置抽屉（与中台管理共用同一组件） -->
    <ThemeSettings />
  </a-layout>
</template>

<script lang="ts" setup>
/**
 * BML 业务系统主布局
 * <p>
 * 视觉风格与中台管理系统保持一致（Vision Pro 毛玻璃设计语言）：
 *   - 毛玻璃侧边栏 + 渐变品牌色菜单
 *   - 透明 Header + Glass Dock 胶囊控制栏
 *   - 柔光纹理全局背景
 *   - 收起状态 Mini Dock 图标导航
 *   - TagsView 标签页导航 + keep-alive 页面缓存
 * </p>
 */
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import {
  IconSettings, IconUser, IconSafe, IconMenu, IconBranch,
  IconLeft, IconRight, IconDown, IconExport,
  IconFullscreen, IconFullscreenExit, IconPalette,
  IconApps, IconIdcard
} from '@arco-design/web-vue/es/icon';
import request from '../utils/request';
import { clearAuthTokens, getAccessToken } from '../utils/auth';
import { fetchLoginConfig } from '../api/auth';
import TagsView from '../components/TagsView.vue';
import ThemeSettings from '../components/ThemeSettings.vue';
import { useTagsViewStore } from '../store/tagsView';
import { useAppStore } from '../store/app';
import { useIdleTimeout } from '../composables/useIdleTimeout';

const router = useRouter();
const route = useRoute();
const tagsViewStore = useTagsViewStore();
const appStore = useAppStore();

/** keep-alive 缓存的视图列表 */
const cachedViews = computed(() => tagsViewStore.cachedViews);

/** 侧边栏折叠状态 */
const collapsed = ref(false);

/** 全屏状态 */
const isFullscreen = ref(false);

/** 侧边栏 Logo 图片 URL（从系统配置动态加载） */
const sidebarLogoUrl = ref('');

/** 当前登录用户信息 */
interface SimpleUserInfo { id: number; username: string; nickname: string; }
const userInfo = ref<SimpleUserInfo | null>(null);

/** 菜单选中 / 展开状态（自动跟随路由） */
const selectedKeys = computed(() => { const name = route.name as string; return name ? [name] : []; });
const openKeys = ref(['system']);

/** 当前页面标题（来自路由 meta） */
const pageTitle = computed(() => { const t = route.meta?.title; return typeof t === 'string' ? t : ''; });

/** 一级菜单名称（来自路由 meta.parentTitle，用于面包屑根节点） */
const parentTitle = computed(() => { const t = (route.meta as any)?.parentTitle; return typeof t === 'string' ? t : ''; });

/** 菜单点击导航 */
const onMenuClick = (key: string) => { router.push({ name: key }); };

/** 点击 Logo 返回业务系统工作台首页 */
const goHome = () => { router.push('/dashboard'); };

/** 全屏切换 */
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
    isFullscreen.value = true;
  } else if (document.exitFullscreen) {
    document.exitFullscreen();
    isFullscreen.value = false;
  }
};

/** 加载当前登录用户信息 */
const loadUserInfo = async () => {
  if (!getAccessToken()) return;
  try {
    const res = await request.get('/auth/info') as any;
    if (res.data?.user) {
      userInfo.value = { id: res.data.user.id, username: res.data.user.username, nickname: res.data.user.nickname };
    }
  } catch { /* ignore */ }
};

/** 退出登录 */
const handleLogout = async () => {
  try { await request.post('/auth/logout'); } catch { /* ignore */ }
  finally {
    clearAuthTokens();
    tagsViewStore.delAllViews();
    Message.success('已退出登录');
    router.push('/login');
  }
};

/** 加载侧边栏 Logo 配置 */
const loadSidebarLogo = async () => {
  try {
    const res = await fetchLoginConfig() as any;
    const config = res.data || {};
    sidebarLogoUrl.value = config['sys.sidebar.logo'] || '';
  } catch { /* ignore */ }
};

// ═══════════════════════════════════════════════════════
// 空闲超时自动登出
// 从后端配置读取空闲时长，超时后自动执行登出操作
// ═══════════════════════════════════════════════════════

/** 空闲超时时长（分钟），默认 30 分钟，由 loadIdleConfig 动态更新 */
const idleTimeoutMinutes = ref(30);

/** 初始化空闲检测实例（回调为登出操作） */
const { start: startIdleWatch, stop: stopIdleWatch } = useIdleTimeout({
  get timeoutMinutes() { return idleTimeoutMinutes.value; },
  onIdle: () => {
    Message.warning('您已长时间未操作，系统已自动退出登录');
    handleLogout();
  }
});

/** 加载空闲超时配置并启动检测 */
const loadIdleConfig = async () => {
  try {
    const res = await fetchLoginConfig() as any;
    const config = res.data || {};
    const minutes = parseInt(config['sys.login.idleTimeout'] || '30', 10);
    idleTimeoutMinutes.value = isNaN(minutes) ? 30 : minutes;
  } catch {
    idleTimeoutMinutes.value = 30;
  }
  // 启动空闲检测（内部会判断 timeoutMinutes <= 0 时不启动）
  startIdleWatch();
};

onMounted(() => {
  appStore.initTheme();
  loadUserInfo();
  loadSidebarLogo();
  loadIdleConfig();
});

onUnmounted(() => {
  stopIdleWatch();
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════════
   根布局 — 柔光纹理背景（与中台一致）
   ═══════════════════════════════════════════════════ */
.biz-layout {
  height: 100vh;
  width: 100vw;
  background-color: #f7f8fa;
  display: flex;
  overflow: hidden;
  background-image:
    radial-gradient(circle at 10% 20%, rgba(226, 240, 255, 0.4) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(240, 230, 255, 0.4) 0%, transparent 40%);
}

/* ═══════════════════════════════════════════════════
   侧边栏 — Vision Pro 毛玻璃风格
   ═══════════════════════════════════════════════════ */
.biz-layout :deep(.arco-layout-sider) {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-right: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 20px 0 40px rgba(0, 0, 0, 0.02);
  z-index: 100;
  height: 100%;
  position: relative;
}

/* 隐藏默认滚动条 */
.biz-layout :deep(.arco-layout-sider-children) {
  overflow-y: auto !important;
  overflow-x: hidden !important;
  padding-bottom: 60px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.biz-layout :deep(.arco-layout-sider-children)::-webkit-scrollbar { display: none; width: 0 !important; }
.biz-layout :deep(.arco-layout-sider)::-webkit-scrollbar { display: none; width: 0 !important; }

/* 隐藏默认 Trigger + Mask */
.biz-layout :deep(.arco-layout-sider-trigger) { display: none; }
.biz-layout :deep(.arco-layout-sider-mask) { display: none !important; }

/* ═══════════════════════════════════════════════════
   Logo — 渐变文字 + hover 缩放
   ═══════════════════════════════════════════════════ */
.biz-layout :deep(.arco-layout-sider) .logo {
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
.biz-layout :deep(.arco-layout-sider) .logo:hover {
  transform: scale(1.05);
}
/* Logo 图片（展开状态） */
.logo-img {
  max-height: 48px;
  max-width: 160px;
  object-fit: contain;
}
/* Logo 图片（收起状态） */
.logo-img-mini {
  max-height: 36px;
  max-width: 48px;
  object-fit: contain;
}
/* Logo 文字回退（无图片时） */
.logo-text {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 2px;
  background: var(--bml-gradient-alt, linear-gradient(135deg, #165dff 0%, #722ed1 100%));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
/* 收起状态：Logo 容器尺寸调整 */
.biz-layout :deep(.arco-layout-sider-collapsed) .logo {
  font-size: 0 !important;
  width: 100% !important;
  height: 64px !important;
  padding: 0 !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  position: relative !important;
}
/* 收起 + 无图片时：显示首字母 "B" 回退 */
.biz-layout :deep(.arco-layout-sider-collapsed) .logo:not(:has(img))::after {
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

/* ═══════════════════════════════════════════════════
   菜单 — 渐变选中 + hover 位移动画
   ═══════════════════════════════════════════════════ */
.biz-layout :deep(.arco-menu) {
  background: transparent;
  padding: 12px;
}
.biz-layout :deep(.arco-menu-inner) {
  padding: 0;
}
.biz-layout :deep(.arco-menu-item) {
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
  padding: 0 12px !important;
}
.biz-layout :deep(.arco-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.6);
  color: #1d2129;
  transform: translateX(4px);
}
.biz-layout :deep(.arco-menu-item.arco-menu-selected) {
  background: var(--bml-gradient, linear-gradient(135deg, #165dff 0%, #3c7eff 100%));
  color: #fff;
  box-shadow: 0 4px 12px var(--bml-shadow, rgba(22, 93, 255, 0.3));
  font-weight: 600;
  transform: translateX(0);
}
.biz-layout :deep(.arco-menu-item.arco-menu-selected:hover) {
  background: var(--bml-gradient, linear-gradient(135deg, #165dff 0%, #3c7eff 100%));
  box-shadow: 0 6px 16px var(--bml-shadow, rgba(22, 93, 255, 0.4));
  transform: translateY(-1px);
}
.biz-layout :deep(.arco-menu-item .arco-icon) {
  font-size: 18px;
  margin-right: 4px;
  flex-shrink: 0;
  transition: all 0.3s;
}
.biz-layout :deep(.arco-menu-item.arco-menu-selected .arco-icon) {
  fill: #fff;
  transform: scale(1.1);
}

/* ── 一级父菜单（子菜单头） ── */
.biz-layout :deep(.arco-menu-inline-header) {
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
.biz-layout :deep(.arco-menu-inline-header.arco-menu-selected) {
  background-color: var(--bml-primary-lighter, rgba(22, 93, 255, 0.08)) !important;
  color: var(--bml-primary, #165dff) !important;
  font-weight: 700 !important;
  border: 1px solid rgba(var(--bml-primary-rgb, 22, 93, 255), 0.1) !important;
  box-shadow: 0 4px 10px rgba(var(--bml-primary-rgb, 22, 93, 255), 0.05);
}
.biz-layout :deep(.arco-menu-inline-header.arco-menu-selected)::before {
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
.biz-layout :deep(.arco-menu-inline-header.arco-menu-selected .arco-icon) {
  color: var(--bml-primary, #165dff) !important;
  fill: var(--bml-primary, #165dff) !important;
  transform: scale(1.1);
}
.biz-layout :deep(.arco-menu-inline-header.arco-menu-selected .arco-menu-icon-suffix) {
  color: var(--bml-primary, #165dff) !important;
}
.biz-layout :deep(.arco-menu-inline-header:hover) {
  background-color: rgba(255, 255, 255, 0.6);
  color: #1d2129;
  transform: translateX(4px);
}

/* 覆盖子级菜单左侧间距 */
.biz-layout :deep(.arco-menu-inline-content .arco-menu-item) {
  padding-left: 28px !important;
}

/* 彻底移除 Arco 各种横向占位符 */
.biz-layout :deep(.arco-menu-indent),
.biz-layout :deep(.arco-menu-icon-empty),
.biz-layout :deep(.arco-menu-indent-list) {
  display: none !important;
  width: 0 !important;
}

/* ═══════════════════════════════════════════════════
   自定义悬浮收缩按钮 — 毛玻璃效果
   ═══════════════════════════════════════════════════ */
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
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08), inset 0 0 0 1px rgba(255, 255, 255, 0.5);
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
  box-shadow: 0 12px 32px var(--bml-shadow, rgba(22, 93, 255, 0.25));
}

/* ═══════════════════════════════════════════════════
   Mini Dock — 收起状态图标导航
   ═══════════════════════════════════════════════════ */
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
  background: rgba(0, 0, 0, 0.05);
  transform: scale(1.1);
}
.mini-item.active {
  background: var(--bml-gradient, linear-gradient(135deg, #165dff 0%, #3c7eff 100%));
  color: #fff;
  box-shadow: 0 4px 12px var(--bml-shadow, rgba(22, 93, 255, 0.3));
}

/* ═══════════════════════════════════════════════════
   Header — 透明 + Glass Dock（与中台一致）
   ═══════════════════════════════════════════════════ */
.biz-layout :deep(.arco-layout-header) {
  height: 48px;
  background: transparent;
  border-bottom: none;
  display: flex;
  align-items: flex-start;
  padding: 4px 24px 0 32px;
  justify-content: space-between;
  z-index: 99;
}

/* 左侧标题区 */
.page-title-area {
  display: flex;
  align-items: center;
  position: relative;
  padding-left: 12px;
  margin-top: 4px;
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
  font-size: 18px;
  color: #1d2129;
  font-weight: 600;
  letter-spacing: 0.2px;
}
/* 无父级菜单时的独立标题样式（如工作台） */
.modern-breadcrumb .breadcrumb-active-solo {
  font-size: 18px;
  color: #1d2129;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.modern-breadcrumb :deep(.arco-breadcrumb-item-separator) {
  color: #c9cdd4;
  margin: 0 8px;
}

/* 右侧 Glass Dock */
.glass-dock {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 8px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  box-shadow:
    0 4px 12px -2px rgba(0, 0, 0, 0.03),
    0 12px 32px rgba(0, 0, 0, 0.05),
    inset 0 0 0 1px rgba(255, 255, 255, 0.6);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
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

/* Dock 按钮 */
.dock-item {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 4px;
  color: #4e5969;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.dock-item:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1d2129;
  transform: scale(1.05);
}

/* 用户按钮（胶囊形） */
.user-item {
  width: auto;
  padding: 0 8px 0 0;
  border-radius: 36px;
  margin-right: 0;
  gap: 4px;
}
.user-item:hover {
  background: rgba(0, 0, 0, 0.04);
}
.user-avatar {
  margin-right: 0;
  transition: transform 0.3s;
  background-color: var(--bml-primary, #165dff) !important;
}
.user-item:hover .user-avatar {
  transform: rotate(10deg);
}
.user-name {
  font-size: 14px;
  color: #1d2129;
  font-weight: 500;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-arrow {
  font-size: 10px;
  margin-left: 2px;
  color: #86909c;
}

/* Dock 分割线 */
.dock-divider {
  width: 1px;
  height: 16px;
  background: #e5e6eb;
  margin: 0 8px;
}

/* ═══════════════════════════════════════════════════
   内容区
   ═══════════════════════════════════════════════════ */
.biz-layout :deep(.arco-layout-content) {
  background: transparent;
  padding: 0;
  height: calc(100vh - 48px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 标签页容器（与中台一致） */
.tags-view-wrapper {
  flex-shrink: 0;
  height: 44px;
  margin-top: -6px;
  z-index: 98;
}

.page-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0;
}

/* ═══════════════════════════════════════════════════
   路由切换动画
   ═══════════════════════════════════════════════════ */
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
</style>
