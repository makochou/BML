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

/**
 * 空状态页面按钮样式优化
 * 确保按钮在任何主题下都清晰可见且美观
 */
.feature-disabled :deep(.arco-btn-primary) {
  background: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
  color: #fff !important;
  border-radius: 10px;
  font-weight: 500;
  padding: 0 24px;
  height: 40px;
  font-size: 15px;
  box-shadow: 0 4px 12px var(--bml-shadow, rgba(22, 93, 255, 0.25));
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.feature-disabled :deep(.arco-btn-primary:hover) {
  background: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
  color: #fff !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px var(--bml-shadow, rgba(22, 93, 255, 0.35));
}

.feature-disabled :deep(.arco-btn-primary:active) {
  background: var(--color-primary-dark-1, #0e42d2) !important;
  border-color: var(--color-primary-dark-1, #0e42d2) !important;
  color: #fff !important;
  transform: translateY(0);
}

/**
 * 次要按钮样式 - 使用主题色轮廓样式
 * 让次要按钮也能清晰可见，与主题色保持一致
 */
.feature-disabled :deep(.arco-btn:not(.arco-btn-primary)) {
  background: rgba(255, 255, 255, 0.95) !important;
  border: 2px solid var(--color-primary, #165dff) !important;
  color: var(--color-primary, #165dff) !important;
  border-radius: 10px;
  font-weight: 500;
  padding: 0 24px;
  height: 40px;
  font-size: 15px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.feature-disabled :deep(.arco-btn:not(.arco-btn-primary):hover) {
  background: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
  color: #fff !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--bml-shadow, rgba(22, 93, 255, 0.25));
}

.feature-disabled :deep(.arco-btn:not(.arco-btn-primary):active) {
  background: var(--color-primary-dark-1, #0e42d2) !important;
  border-color: var(--color-primary-dark-1, #0e42d2) !important;
  color: #fff !important;
  transform: translateY(0);
}

/**
 * 按钮图标颜色
 */
.feature-disabled :deep(.arco-btn .arco-icon) {
  color: inherit !important;
}

/**
 * 按钮间距优化
 */
.feature-disabled :deep(.arco-space) {
  gap: 16px !important;
}

/**
 * Result组件标题和副标题样式优化
 */
.feature-disabled :deep(.arco-result-title) {
  font-size: 24px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
}

.feature-disabled :deep(.arco-result-subtitle) {
  font-size: 15px;
  color: #4e5969;
  line-height: 1.6;
  max-width: 560px;
  margin: 0 auto;
}

/**
 * Result图标样式优化
 */
.feature-disabled :deep(.arco-result-icon) {
  margin-bottom: 24px;
}

.feature-disabled :deep(.arco-result-icon svg) {
  width: 80px;
  height: 80px;
}
</style>
