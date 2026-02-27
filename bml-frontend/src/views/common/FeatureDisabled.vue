<template>
  <div class="feature-disabled">
    <a-result status="warning" :title="`${featureName} 暂不开放`" subtitle="当前版本已下线未闭环的 API 平台能力，避免暴露错误入口。">
      <template #extra>
        <a-space>
          <a-button type="primary" @click="router.push({ name: 'AppList' })">前往应用管理</a-button>
          <a-button @click="router.push({ name: 'Dashboard' })">返回仪表盘</a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const featureName = computed(() => (route.meta.featureName as string) || '该功能');
</script>

<style scoped>
.feature-disabled {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    min-height: 420px;
    padding: 24px;
}
</style>
