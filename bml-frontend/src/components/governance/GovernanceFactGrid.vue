<template>
  <div class="governance-fact-grid">
    <article
      v-for="item in items"
      :key="item.label"
      class="governance-fact-grid__card"
      :class="cardClass"
    >
      <span>{{ item.label }}</span>
      <a-typography-text
        v-if="item.copyable"
        copyable
        :class="copyClass"
      >
        {{ item.value }}
      </a-typography-text>
      <strong v-else>{{ item.value }}</strong>
      <small>{{ item.hint }}</small>
    </article>
  </div>
</template>

<script lang="ts" setup>
import type { FactCard } from '../../types/governance';

/**
 * 通用账号画像卡网格。
 * 统一处理标签、值、说明以及可复制字段，适用于账号画像、详情概览、凭证画像等场景。
 */
defineProps<{
  items: FactCard[];
  cardClass?: string;
  copyClass?: string;
}>();
</script>

<style scoped>
.governance-fact-grid {
  display: grid;
  gap: 14px;
}

.governance-fact-grid__card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
</style>
