<template>
  <a-layout class="business-layout">
    <a-layout-sider collapsible breakpoint="xl" :collapsed="collapsed" @collapse="onCollapse" :width="240" :collapsed-width="64">
      <div class="sidebar-logo" @click="goHome">
        <span v-if="!collapsed" class="logo-text">BML 业务系统</span>
        <span v-else class="logo-text-mini">B</span>
      </div>
      <a-menu :selected-keys="selectedKeys" :default-open-keys="openKeys" :auto-open-selected="true" @menu-item-click="onMenuClick">
        <a-sub-menu key="system">
          <template #icon><icon-settings /></template>
          <template #title>系统管理</template>
          <a-menu-item key="SystemUser"><template #icon><icon-user /></template>用户管理</a-menu-item>
          <a-menu-item key="SystemRole"><template #icon><icon-safe /></template>角色管理</a-menu-item>
          <a-menu-item key="SystemMenu"><template #icon><icon-menu /></template>菜单管理</a-menu-item>
          <a-menu-item key="SystemDept"><template #icon><icon-branch /></template>部门管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
      <div class="collapse-toggle" @click="collapsed = !collapsed">
        <component :is="collapsed ? IconRight : IconLeft" />
      </div>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="business-header">
        <div class="header-left">
          <div class="title-bar">
            <div class="title-indicator"></div>
            <a-breadcrumb class="header-breadcrumb">
              <a-breadcrumb-item><span class="bc-root">业务系统</span></a-breadcrumb-item>
              <a-breadcrumb-item v-if="pageTitle"><span class="bc-current">{{ pageTitle }}</span></a-breadcrumb-item>
            </a-breadcrumb>
          </div>
        </div>
        <div class="header-right">
          <div class="header-dock">
            <a-dropdown trigger="hover">
              <div class="dock-user">
                <a-avatar :size="30" class="dock-avatar"><icon-user /></a-avatar>
                <span v-if="userInfo" class="dock-username">{{ userInfo.nickname || userInfo.username }}</span>
                <icon-down :size="12" class="dock-arrow" />
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
      <a-layout-content class="business-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="route.fullPath" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Message } from '@arco-design/web-vue';
import { IconSettings, IconUser, IconSafe, IconMenu, IconBranch, IconLeft, IconRight, IconDown, IconExport } from '@arco-design/web-vue/es/icon';
import request from '../utils/request';
import { clearAuthTokens, getAccessToken } from '../utils/auth';

const router = useRouter();
const route = useRoute();
const collapsed = ref(false);

interface SimpleUserInfo { id: number; username: string; nickname: string; }
const userInfo = ref<SimpleUserInfo | null>(null);

const selectedKeys = computed(() => { const name = route.name as string; return name ? [name] : []; });
const openKeys = ref(['system']);
const pageTitle = computed(() => { const t = route.meta?.title; return typeof t === 'string' ? t : ''; });

const onCollapse = (val: boolean) => { collapsed.value = val; };
const onMenuClick = (key: string) => { router.push({ name: key }); };
const goHome = () => { router.push('/dashboard'); };

const loadUserInfo = async () => {
  if (!getAccessToken()) return;
  try {
    const res = await request.get('/auth/info') as any;
    if (res.data?.user) {
      userInfo.value = { id: res.data.user.id, username: res.data.user.username, nickname: res.data.user.nickname };
    }
  } catch { /* ignore */ }
};

const handleLogout = async () => {
  try { await request.post('/auth/logout'); } catch { /* ignore */ }
  finally {
    clearAuthTokens();
    Message.success('已退出登录');
    router.push('/login');
  }
};

onMounted(() => { loadUserInfo(); });
</script>

<style scoped>
.business-layout { height: 100vh; width: 100vw; overflow: hidden; background: #f2f3f5; }

.business-layout :deep(.arco-layout-sider) { background: #fff; border-right: 1px solid #e5e6eb; box-shadow: 2px 0 8px rgba(0,0,0,0.03); position: relative; z-index: 100; }
.business-layout :deep(.arco-layout-sider-trigger) { display: none; }
.business-layout :deep(.arco-layout-sider-children) { overflow-y: auto; overflow-x: hidden; padding-bottom: 60px; scrollbar-width: none; }
.business-layout :deep(.arco-layout-sider-children)::-webkit-scrollbar { display: none; }

.sidebar-logo { height: 64px; display: flex; align-items: center; justify-content: center; cursor: pointer; border-bottom: 1px solid #f2f3f5; }
.logo-text { font-size: 18px; font-weight: 800; background: linear-gradient(135deg, #4f46e5, #7c3aed); -webkit-background-clip: text; background-clip: text; color: transparent; letter-spacing: 1px; }
.logo-text-mini { font-size: 22px; font-weight: 900; color: #4f46e5; }

.business-layout :deep(.arco-menu) { background: transparent; padding: 8px; }
.business-layout :deep(.arco-menu-item) { height: 42px; margin: 2px 4px; border-radius: 10px; color: #4e5969; font-weight: 500; transition: all 0.25s; }
.business-layout :deep(.arco-menu-item:hover) { background: #f2f3f5; color: #1d2129; }
.business-layout :deep(.arco-menu-item.arco-menu-selected) { background: linear-gradient(135deg, #4f46e5, #7c3aed); color: #fff; box-shadow: 0 4px 12px rgba(79,70,229,0.25); font-weight: 600; }
.business-layout :deep(.arco-menu-item.arco-menu-selected .arco-icon) { color: #fff; }
.business-layout :deep(.arco-menu-inline-header) { height: 42px; margin: 2px 4px; border-radius: 10px; transition: all 0.25s; }
.business-layout :deep(.arco-menu-inline-header.arco-menu-selected) { background: rgba(79,70,229,0.06); color: #4f46e5; font-weight: 600; }
.business-layout :deep(.arco-menu-inline-header.arco-menu-selected .arco-icon) { color: #4f46e5; }
.business-layout :deep(.arco-menu-indent), .business-layout :deep(.arco-menu-icon-empty) { display: none !important; width: 0 !important; }
.business-layout :deep(.arco-menu-inline-content .arco-menu-item) { padding-left: 28px !important; }

.collapse-toggle { position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%); width: 36px; height: 36px; border-radius: 50%; background: #f7f8fa; border: 1px solid #e5e6eb; display: flex; align-items: center; justify-content: center; cursor: pointer; color: #4e5969; font-size: 14px; transition: all 0.3s; z-index: 101; }
.collapse-toggle:hover { background: #fff; color: #4f46e5; box-shadow: 0 4px 12px rgba(79,70,229,0.2); }

.business-header { height: 56px; background: #fff; border-bottom: 1px solid #e5e6eb; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }
.title-bar { display: flex; align-items: center; position: relative; padding-left: 14px; }
.title-indicator { position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 4px; height: 18px; background: #4f46e5; border-radius: 2px; }
.bc-root { font-size: 14px; color: #86909c; }
.bc-current { font-size: 17px; font-weight: 600; color: #1d2129; }
.header-breadcrumb :deep(.arco-breadcrumb-item-separator) { color: #c9cdd4; margin: 0 8px; }

.header-dock { display: flex; align-items: center; }
.dock-user { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 8px; border-radius: 20px; transition: background 0.2s; }
.dock-user:hover { background: #f2f3f5; }
.dock-avatar { background: #4f46e5 !important; flex-shrink: 0; }
.dock-username { font-size: 14px; color: #1d2129; font-weight: 500; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dock-arrow { color: #86909c; }

.business-content { padding: 0; height: calc(100vh - 56px); overflow-y: auto; overflow-x: hidden; background: #f2f3f5; }

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.25s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateY(12px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-12px); }
</style>
