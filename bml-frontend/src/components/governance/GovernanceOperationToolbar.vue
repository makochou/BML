<template>
  <section class="governance-operation-toolbar" :class="`variant-${variant}`">
    <div class="governance-operation-toolbar__main">
      <div class="governance-operation-toolbar__heading">
        <p v-if="eyebrow" class="governance-operation-toolbar__eyebrow">{{ eyebrow }}</p>
        <div class="governance-operation-toolbar__title-row">
          <h3>{{ title }}</h3>
          <slot name="titleSuffix" />
        </div>
        <p v-if="description" class="governance-operation-toolbar__description">{{ description }}</p>
      </div>

      <div v-if="badges.length" class="governance-operation-toolbar__badges">
        <span
          v-for="item in badges"
          :key="`${item.label}-${item.value}`"
          class="governance-operation-toolbar__badge"
          :class="`tone-${item.tone || 'blue'}`"
        >
          {{ item.label }} · {{ item.value }}
        </span>
      </div>
    </div>

    <div class="governance-operation-toolbar__action-zone">
      <div v-if="$slots.leading" class="governance-operation-toolbar__leading">
        <slot name="leading" />
      </div>

      <div v-if="$slots.actions" class="governance-operation-toolbar__actions">
        <slot name="actions" />
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
import type { GovernanceCompactMetaItem } from '../../types/governance';

/**
 * 通用运营工具带。
 * 用于承载列表标题、结果摘要、当前视图状态以及一组核心操作按钮，
 * 适合放在查询区和表格区之间，作为统一的工作流入口。
 */
withDefaults(
  defineProps<{
    eyebrow?: string;
    title: string;
    description?: string;
    badges?: GovernanceCompactMetaItem[];
    variant?: 'card' | 'embedded';
  }>(),
  {
    eyebrow: '',
    description: '',
    badges: () => [],
    variant: 'card'
  }
);
</script>

<style scoped>
.governance-operation-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}

.governance-operation-toolbar.variant-card {
  padding: 18px 20px;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.12), transparent 28%),
    radial-gradient(circle at bottom left, rgba(20, 184, 166, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 250, 255, 0.97));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 18px 38px rgba(15, 23, 42, 0.06);
}

.governance-operation-toolbar.variant-embedded {
  position: relative;
  padding: 18px 0 4px;
}

.governance-operation-toolbar.variant-embedded::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.14), rgba(148, 163, 184, 0.42), rgba(20, 184, 166, 0.14));
}

.governance-operation-toolbar__main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.governance-operation-toolbar__heading {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.governance-operation-toolbar__eyebrow {
  margin: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #7c8aa5;
}

.governance-operation-toolbar__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.governance-operation-toolbar__title-row h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.12;
  color: #0f172a;
}

.governance-operation-toolbar.variant-embedded .governance-operation-toolbar__title-row h3 {
  font-size: 21px;
}

.governance-operation-toolbar__description {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.governance-operation-toolbar.variant-embedded .governance-operation-toolbar__description {
  max-width: 680px;
  font-size: 12px;
}

.governance-operation-toolbar__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.governance-operation-toolbar__badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.9);
  background: rgba(255, 255, 255, 0.94);
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.governance-operation-toolbar.variant-embedded .governance-operation-toolbar__badge {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(246, 250, 255, 0.92));
}

.governance-operation-toolbar__badge.tone-blue {
  border-color: rgba(96, 165, 250, 0.3);
}

.governance-operation-toolbar__badge.tone-green {
  border-color: rgba(74, 222, 128, 0.3);
}

.governance-operation-toolbar__badge.tone-teal {
  border-color: rgba(45, 212, 191, 0.3);
}

.governance-operation-toolbar__badge.tone-gold {
  border-color: rgba(250, 204, 21, 0.34);
}

.governance-operation-toolbar__badge.tone-violet {
  border-color: rgba(167, 139, 250, 0.3);
}

.governance-operation-toolbar__action-zone {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.governance-operation-toolbar__leading,
.governance-operation-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.governance-operation-toolbar__leading :deep(.arco-btn),
.governance-operation-toolbar__actions :deep(.arco-btn) {
  min-width: 112px;
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 700;
}

.governance-operation-toolbar__actions :deep(.arco-btn-primary) {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.18);
}

.governance-operation-toolbar.variant-embedded .governance-operation-toolbar__actions :deep(.arco-btn-primary) {
  box-shadow: 0 12px 24px rgba(23, 105, 255, 0.16);
}

.governance-operation-toolbar__leading :deep(.arco-btn),
.governance-operation-toolbar__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  border-color: rgba(217, 226, 237, 0.98);
  background: rgba(255, 255, 255, 0.94);
  color: #475569;
}

@media (max-width: 1280px) {
  .governance-operation-toolbar {
    grid-template-columns: 1fr;
  }

  .governance-operation-toolbar__action-zone {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .governance-operation-toolbar {
    padding: 16px;
    border-radius: 24px;
  }

  .governance-operation-toolbar__title-row h3 {
    font-size: 22px;
  }

  .governance-operation-toolbar__leading,
  .governance-operation-toolbar__actions {
    width: 100%;
  }

  .governance-operation-toolbar__leading :deep(.arco-btn),
  .governance-operation-toolbar__actions :deep(.arco-btn) {
    width: 100%;
  }
}
</style>
