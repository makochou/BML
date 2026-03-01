<template>
  <div class="governance-stat-grid">
    <article
      v-for="item in items"
      :key="item.label"
      class="governance-stat-grid__card"
      :class="[cardClass, `tone-${item.tone}`]"
    >
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
      <small>{{ item.hint }}</small>
    </article>
  </div>
</template>

<script lang="ts" setup>
import type { WorkbenchStatCard } from '../../types/governance';

/**
 * 通用治理指标卡网格。
 * 统一渲染“标题 / 数值 / 提示语”三段式摘要卡，避免每个页面重复写 v-for 卡片结构。
 */
defineProps<{
  items: WorkbenchStatCard[];
  cardClass?: string;
}>();
</script>

<style scoped>
.governance-stat-grid {
  display: grid;
  gap: 14px;
}

.governance-stat-grid__card {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
