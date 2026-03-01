<template>
  <section class="governance-list-stage" :class="{ 'governance-list-stage--headless': !hasHeadContent }" :style="stageStyle">
    <div class="governance-list-stage__glow" aria-hidden="true" />

    <header v-if="hasHeadContent" class="governance-list-stage__head">
      <div class="governance-list-stage__intro">
        <p v-if="eyebrow" class="governance-list-stage__eyebrow">{{ eyebrow }}</p>

        <div v-if="title || $slots.titleSuffix" class="governance-list-stage__title-row">
          <h3 v-if="title">{{ title }}</h3>
          <slot name="titleSuffix" />
        </div>

        <p v-if="description" class="governance-list-stage__description">{{ description }}</p>

        <div v-if="metaItems.length" class="governance-list-stage__meta">
          <span
            v-for="item in metaItems"
            :key="`${item.label}-${item.value}`"
            class="governance-list-stage__meta-item"
            :class="`tone-${item.tone || 'blue'}`"
          >
            {{ item.label }} · {{ item.value }}
          </span>
        </div>
      </div>

      <div v-if="$slots.actions" class="governance-list-stage__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="governance-list-stage__body">
      <slot />
    </div>
  </section>
</template>

<script lang="ts" setup>
import { computed, useSlots } from 'vue';
import type { GovernanceCompactMetaItem } from '../../types/governance';

/**
 * 通用列表舞台组件。
 * 用于统一承载列表区标题、轻量摘要指标、主操作按钮与表格主体，
 * 让治理类页面的“列表工作台”具备统一的视觉层次与复用入口。
 */
const props = withDefaults(
  defineProps<{
    eyebrow?: string;
    title?: string;
    description?: string;
    metaItems?: GovernanceCompactMetaItem[];
    maxWidth?: string;
  }>(),
  {
    eyebrow: '',
    description: '',
    metaItems: () => [],
    maxWidth: '1260px'
  }
);

const slots = useSlots();

const stageStyle = computed(() => ({
  '--governance-list-stage-max-width': props.maxWidth
}));

// 列表工作台允许按需隐藏头部，便于页面在只需要“列表容器”时复用同一套外壳。
const hasHeadContent = computed(() => Boolean(
  props.eyebrow
  || props.title
  || props.description
  || props.metaItems.length
  || slots.actions
  || slots.titleSuffix
));
</script>

<style scoped>
.governance-list-stage {
  position: relative;
  width: min(100%, var(--governance-list-stage-max-width));
  margin: 20px auto 0;
  padding: 24px;
  border-radius: 30px;
  border: 1px solid rgba(220, 230, 241, 0.98);
  background:
    linear-gradient(135deg, rgba(237, 245, 255, 0.96), rgba(255, 255, 255, 0.98) 34%, rgba(241, 252, 251, 0.96)),
    #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 22px 48px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.governance-list-stage__glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(20, 184, 166, 0.12), transparent 30%);
  pointer-events: none;
}

.governance-list-stage__head,
.governance-list-stage__body {
  position: relative;
  z-index: 1;
}

.governance-list-stage__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: end;
  margin-bottom: 20px;
}

.governance-list-stage__intro {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.governance-list-stage__eyebrow {
  margin: 0;
  color: #7c8aa5;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.governance-list-stage__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.governance-list-stage__title-row h3 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.08;
}

.governance-list-stage__description {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.75;
}

.governance-list-stage__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.governance-list-stage__meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.94);
  background: rgba(255, 255, 255, 0.84);
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.94);
}

.governance-list-stage__meta-item.tone-blue {
  border-color: rgba(96, 165, 250, 0.28);
}

.governance-list-stage__meta-item.tone-green {
  border-color: rgba(74, 222, 128, 0.32);
}

.governance-list-stage__meta-item.tone-teal {
  border-color: rgba(45, 212, 191, 0.3);
}

.governance-list-stage__meta-item.tone-gold {
  border-color: rgba(250, 204, 21, 0.34);
}

.governance-list-stage__meta-item.tone-violet {
  border-color: rgba(167, 139, 250, 0.3);
}

.governance-list-stage__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.governance-list-stage__actions :deep(.arco-btn) {
  min-width: 118px;
  height: 42px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 700;
}

.governance-list-stage__actions :deep(.arco-btn-primary) {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.16);
}

.governance-list-stage__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  border-color: rgba(217, 226, 237, 0.98);
  background: rgba(255, 255, 255, 0.94);
  color: #475569;
}

.governance-list-stage__body {
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(223, 232, 243, 0.92);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 255, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.governance-list-stage--headless .governance-list-stage__body {
  border-radius: 26px;
}

@media (max-width: 1280px) {
  .governance-list-stage__head {
    grid-template-columns: 1fr;
  }

  .governance-list-stage__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .governance-list-stage {
    padding: 18px;
    border-radius: 24px;
  }

  .governance-list-stage__title-row h3 {
    font-size: 24px;
  }

  .governance-list-stage__body {
    padding: 14px;
    border-radius: 20px;
  }

  .governance-list-stage__actions {
    width: 100%;
  }

  .governance-list-stage__actions :deep(.arco-btn) {
    min-width: 0;
    flex: 1;
  }
}
</style>
