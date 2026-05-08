<template>
  <div class="dashboard-wrapper">
    <div class="welcome-card">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎使用 BML 业务管理系统</h1>
        <p class="welcome-desc">
          统一身份认证与权限管理平台，提供用户管理、角色管理、部门管理、机构管理等核心功能。
        </p>
      </div>
      <div class="welcome-illustration">
        <icon-apps :size="80" style="color: rgba(79, 70, 229, 0.15);" />
      </div>
    </div>

    <div class="stat-cards">
      <div class="stat-card" v-for="item in visibleStatItems" :key="item.label" @click="$router.push(item.route)">
        <div class="stat-icon" :style="{ background: item.iconBg }">
          <component :is="item.icon" :size="22" style="color: #fff;" />
        </div>
        <div class="stat-info">
          <span class="stat-label">{{ item.label }}</span>
          <span class="stat-desc">{{ item.desc }}</span>
        </div>
        <icon-right :size="16" style="color: #c9cdd4;" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * 业务系统工作台页面
 *
 * 重要说明：
 *   defineOptions({ name: 'BusinessDashboard' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'BusinessDashboard' });

import { computed } from 'vue';
import { IconApps, IconUser, IconSafe, IconBranch, IconRight } from '@arco-design/web-vue/es/icon';
import { usePermissionStore } from '../../../store/permission';

const permissionStore = usePermissionStore();

/**
 * 工作台快捷入口配置
 * permission 字段对应菜单的 list 权限，无权限则不显示该卡片
 */
const statItems = [
  { label: '用户管理', desc: '管理系统用户账号', icon: IconUser, iconBg: 'linear-gradient(135deg, #4f46e5, #7c3aed)', route: { name: 'SystemUser' }, permission: 'system:user:list' },
  { label: '角色管理', desc: '配置角色与权限', icon: IconSafe, iconBg: 'linear-gradient(135deg, #059669, #10b981)', route: { name: 'SystemRole' }, permission: 'system:role:list' },
  { label: '部门管理', desc: '维护组织架构', icon: IconBranch, iconBg: 'linear-gradient(135deg, #dc2626, #ef4444)', route: { name: 'SystemDept' }, permission: 'system:dept:list' }
];

/** 根据当前用户权限过滤后的可见快捷入口 */
const visibleStatItems = computed(() =>
  statItems.filter(item => permissionStore.hasPermission(item.permission))
);
</script>

<style scoped>
.dashboard-wrapper { padding: 24px; height: 100%; }

.welcome-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 40px 48px; border-radius: 16px;
  background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #a855f7 100%);
  color: #fff; margin-bottom: 24px;
}
.welcome-title { font-size: 26px; font-weight: 800; margin: 0 0 12px; letter-spacing: 1px; }
.welcome-desc { font-size: 15px; opacity: 0.85; margin: 0; line-height: 1.8; max-width: 480px; }

.stat-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.stat-card {
  display: flex; align-items: center; gap: 16px;
  padding: 24px; background: #fff; border-radius: 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); cursor: pointer;
  transition: all 0.3s ease; border: 1px solid transparent;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(79, 70, 229, 0.12);
  border-color: rgba(79, 70, 229, 0.15);
}
.stat-icon {
  width: 48px; height: 48px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: 16px; font-weight: 600; color: #1d2129; }
.stat-desc { font-size: 13px; color: #86909c; }
</style>
