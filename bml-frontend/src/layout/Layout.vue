<template>
  <a-layout class="layout-demo">
    <a-layout-sider collapsible breakpoint="xl" :collapsed="collapsed" @collapse="onCollapse">
      <div class="logo">BML API Platform</div>
      <a-menu
        :default-selected-keys="['1']"
        :style="{ width: '100%' }"
        @menu-item-click="onClickMenuItem"
      >
        <a-menu-item key="ApiList">
          <icon-list />
          API 管理
        </a-menu-item>
        <a-menu-item key="ApiDebug">
          <icon-bug />
          在线调试
        </a-menu-item>
        <a-menu-item key="AppList">
          <icon-apps />
          应用管理
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="padding-left: 20px;">
        <a-button shape="circle" @click="toggleTheme">
            <template #icon>
                <icon-moon v-if="theme === 'light'" />
                <icon-sun v-else />
            </template>
        </a-button>
      </a-layout-header>
      <a-layout-content style="padding: 0 24px 24px;">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { IconList, IconBug, IconApps, IconMoon, IconSun } from '@arco-design/web-vue/es/icon';

const router = useRouter();
const collapsed = ref(false);
const theme = ref('light');

const onCollapse = (val: boolean) => {
  collapsed.value = val;
};

const onClickMenuItem = (key: string) => {
  router.push({ name: key });
};

const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light';
    if (theme.value === 'dark') {
        document.body.setAttribute('arco-theme', 'dark');
    } else {
        document.body.removeAttribute('arco-theme');
    }
}
</script>

<style scoped>
.layout-demo {
  height: 100vh;
}
.layout-demo :deep(.arco-layout-sider) .logo {
  height: 32px;
  margin: 12px 8px;
  background: rgba(255, 255, 255, 0.2);
  color: var(--color-text-1);
  text-align: center;
  line-height: 32px;
  font-weight: bold;
}
.layout-demo :deep(.arco-layout-header)  {
  height: 64px;
  line-height: 64px;
  background: var(--color-bg-2);
}
.layout-demo :deep(.arco-layout-footer) {
  height: 48px;
  color: var(--color-text-2);
  text-align: center;
  line-height: 48px;
}
.layout-demo :deep(.arco-layout-content) {
  color: var(--color-text-2);
  font-weight: 400;
  font-size: 14px;
  background: var(--color-fill-2);
}
.layout-demo :deep(.arco-layout-sider-light) .logo{
  background: var(--color-fill-2);
}
</style>
