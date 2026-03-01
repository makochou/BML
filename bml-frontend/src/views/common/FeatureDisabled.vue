<template>
  <div class="feature-disabled">
    <a-result
      status="warning"
      :title="`${featureName} 暂未开放`"
      subtitle="当前能力已经统一收口到正式页面；为避免继续进入旧入口，请通过推荐入口完成后续操作。"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" @click="router.push({ name: primaryActionName })">
            {{ primaryActionLabel }}
          </a-button>
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

/**
 * 占位页是通用兜底页，优先允许路由通过 meta 指定推荐入口；
 * 若未显式指定，则对 API 能力统一回收到 API账号管理页，其他场景回到仪表盘。
 */
const primaryActionName = computed(() => {
  const redirectName = route.meta.redirectName as string | undefined;
  if (redirectName) {
    return redirectName;
  }
  if ((route.path.startsWith('/admin/api') || route.path.startsWith('/admin/app')) && router.hasRoute('ApiAccountManage')) {
    return 'ApiAccountManage';
  }
  return 'Dashboard';
});

const primaryActionLabel = computed(() => {
  if (primaryActionName.value === 'ApiAccountManage') {
    return '前往 API账号管理';
  }
  return '返回仪表盘';
});
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
