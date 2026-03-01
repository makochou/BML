<template>
  <section class="governance-panel">
    <div v-if="$slots.header || title" class="governance-panel__header">
      <slot name="header">
        <div class="section-mini-title">{{ title }}</div>
      </slot>
    </div>
    <div class="governance-panel__body" :class="bodyClass">
      <slot />
    </div>
  </section>
</template>

<script lang="ts" setup>
/**
 * 通用治理面板容器。
 * 用于统一承载账号画像、筛选器、联调建议、摘要区等侧栏或主区内容，
 * 后续页面只需传入标题和内容，无需重复拼装面板壳层。
 */
defineProps<{
  title?: string;
  bodyClass?: string;
}>();
</script>

<style scoped>
.governance-panel {
  position: relative;
  overflow: hidden;
}

.governance-panel__header {
  margin-bottom: 12px;
}

.governance-panel__body {
  min-width: 0;
}
</style>
