<template>
  <section
    class="governance-list-stage"
    :class="[
      `density-${density}`,
      {
        'governance-list-stage--headless': !hasHeadContent,
        'governance-list-stage--body-fill': bodyFill,
        'governance-list-stage--body-align-end': bodyAlign === 'end'
      }
    ]"
    :style="stageStyle"
  >
    <div class="governance-list-stage__glow" aria-hidden="true" />

    <header
      v-if="hasHeadContent"
      class="governance-list-stage__head"
      :class="{ 'governance-list-stage__head--actions-only': !hasIntroContent }"
    >
      <div v-if="hasIntroContent" class="governance-list-stage__intro">
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
    density?: 'regular' | 'compact' | 'ultra';
    bodyFill?: boolean;
    bodyAlign?: 'start' | 'end';
  }>(),
  {
    eyebrow: '',
    description: '',
    metaItems: () => [],
    maxWidth: '1260px',
    // 默认常规密度，页面可按需切换 ultra 获得极致紧凑布局。
    density: 'regular',
    // 默认保持内容自适应高度；需要“列表区铺满”时页面可显式打开。
    bodyFill: false,
    // 默认顶部对齐；需要“卡片贴底”时可切换为 end。
    bodyAlign: 'start'
  }
);

const slots = useSlots();

const stageStyle = computed(() => ({
  '--governance-list-stage-max-width': props.maxWidth
}));

// 头部文案区与动作区分离判断，便于支持“仅右侧按钮”这一高频布局。
const hasIntroContent = computed(() => Boolean(
  props.eyebrow
  || props.title
  || props.description
  || props.metaItems.length
  || slots.titleSuffix
));

// 列表工作台允许按需隐藏头部，便于页面在只需要“列表容器”时复用同一套外壳。
const hasHeadContent = computed(() => Boolean(hasIntroContent.value || slots.actions));
</script>

<style scoped>
.governance-list-stage {
  --stage-margin-top: 16px;
  --stage-padding: 16px;
  --stage-radius: 24px;
  --stage-head-gap: 12px;
  --stage-head-margin-bottom: 12px;
  --stage-actions-gap: 12px;
  --stage-button-min-width: 104px;
  --stage-button-height: 36px;
  --stage-button-padding-inline: 14px;
  --stage-body-padding: 12px;
  --stage-body-radius: 18px;
  --stage-meta-height: 34px;
  position: relative;
  width: min(100%, var(--governance-list-stage-max-width));
  margin: var(--stage-margin-top) auto 0;
  padding: var(--stage-padding);
  border-radius: var(--stage-radius);
  border: 1px solid rgba(218, 228, 240, 0.98);
  background:
    linear-gradient(135deg, rgba(238, 246, 255, 0.96), rgba(255, 255, 255, 0.98) 34%, rgba(243, 252, 251, 0.96)),
    #ffffff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 12px 30px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.governance-list-stage.density-compact {
  --stage-margin-top: 12px;
  --stage-padding: 12px;
  --stage-radius: 20px;
  --stage-head-gap: 10px;
  --stage-head-margin-bottom: 10px;
  --stage-actions-gap: 10px;
  --stage-button-min-width: 96px;
  --stage-button-height: 34px;
  --stage-button-padding-inline: 12px;
  --stage-body-padding: 10px;
  --stage-body-radius: 16px;
  --stage-meta-height: 32px;
}

.governance-list-stage.density-ultra {
  --stage-margin-top: 10px;
  --stage-padding: 9px;
  --stage-radius: 16px;
  --stage-head-gap: 8px;
  --stage-head-margin-bottom: 8px;
  --stage-actions-gap: 8px;
  --stage-button-min-width: 84px;
  --stage-button-height: 30px;
  --stage-button-padding-inline: 10px;
  --stage-body-padding: 8px;
  --stage-body-radius: 12px;
  --stage-meta-height: 28px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.96),
    0 8px 18px rgba(15, 23, 42, 0.04);
}

.governance-list-stage.density-ultra .governance-list-stage__glow {
  opacity: 0.72;
}

.governance-list-stage__glow {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 26%),
    radial-gradient(circle at bottom right, rgba(20, 184, 166, 0.08), transparent 28%);
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
  gap: var(--stage-head-gap);
  align-items: end;
  margin-bottom: var(--stage-head-margin-bottom);
}

.governance-list-stage__head--actions-only {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: var(--stage-head-margin-bottom);
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
  min-height: var(--stage-meta-height);
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
  gap: var(--stage-actions-gap);
}

.governance-list-stage__actions :deep(.arco-btn) {
  min-width: var(--stage-button-min-width);
  height: var(--stage-button-height);
  padding: 0 var(--stage-button-padding-inline);
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
  padding: var(--stage-body-padding);
  border-radius: var(--stage-body-radius);
  border: 1px solid rgba(223, 232, 243, 0.92);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 255, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.governance-list-stage--headless .governance-list-stage__body {
  border-radius: var(--stage-body-radius);
}

/**
 * 列表铺满模式：
 * 统一让 stage/body 进入纵向弹性布局，使槽位内表格能够通过 flex:1 吃满可用高度，
 * 避免出现“列表卡片贴顶、下方留白”的视觉割裂问题。
 */
.governance-list-stage--body-fill {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.governance-list-stage--body-fill .governance-list-stage__head {
  flex-shrink: 0;
}

.governance-list-stage--body-fill .governance-list-stage__body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

/* 在铺满模式下支持底部对齐，用于“列表卡片贴底”场景。 */
.governance-list-stage--body-fill.governance-list-stage--body-align-end .governance-list-stage__body {
  justify-content: flex-end;
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
    --stage-padding: 9px;
    --stage-radius: 16px;
    --stage-body-padding: 8px;
    --stage-body-radius: 12px;
    --stage-button-min-width: 0;
    --stage-button-height: 30px;
  }

  .governance-list-stage__title-row h3 {
    font-size: 24px;
  }

  .governance-list-stage__body {
    padding: var(--stage-body-padding);
    border-radius: var(--stage-body-radius);
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
