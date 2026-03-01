<template>
  <section
    class="governance-compact-query-panel"
    :class="[`align-${align}`, { 'is-minimal': !hasHeader, 'has-footer': hasFooter }]"
    :style="{ '--query-panel-max-width': maxWidth || '100%' }"
  >
    <header v-if="hasHeader" class="governance-compact-query-panel__header">
      <div class="governance-compact-query-panel__heading">
        <p v-if="eyebrow" class="governance-compact-query-panel__eyebrow">{{ eyebrow }}</p>
        <div class="governance-compact-query-panel__title-row">
          <h2 v-if="title">{{ title }}</h2>
          <div v-if="metaItems.length" class="governance-compact-query-panel__meta">
            <span
              v-for="item in metaItems"
              :key="`${item.label}-${item.value}`"
              class="governance-compact-query-panel__meta-pill"
              :class="`tone-${item.tone || 'blue'}`"
            >
              {{ item.label }} · {{ item.value }}
            </span>
          </div>
        </div>
        <p v-if="description" class="governance-compact-query-panel__description">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="governance-compact-query-panel__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="governance-compact-query-panel__body" :class="{ 'no-header': !hasHeader }">
      <slot />
    </div>

    <footer
      v-if="hasFooter"
      class="governance-compact-query-panel__footer"
      :class="{ 'has-note': noteTitle || noteText || $slots.note }"
    >
      <div v-if="noteTitle || noteText || $slots.note" class="governance-compact-query-panel__note-wrap">
        <slot name="note">
          <div class="governance-compact-query-panel__note">
            <strong v-if="noteTitle">{{ noteTitle }}</strong>
            <span v-if="noteText">{{ noteText }}</span>
          </div>
        </slot>
      </div>

      <div v-if="$slots.footerActions" class="governance-compact-query-panel__footer-actions">
        <slot name="footerActions" />
      </div>
    </footer>
  </section>
</template>

<script lang="ts" setup>
import { computed, useSlots } from 'vue';
import type { GovernanceCompactMetaItem } from '../../types/governance';

/**
 * 通用紧凑筛选面板。
 * 统一承载查询标题、轻量摘要、操作按钮、嵌入式表单区和底部动作区，
 * 适用于列表页或治理页首屏需要“紧凑筛选条 + 字段区”的场景。
 */
const props = withDefaults(
  defineProps<{
    eyebrow?: string;
    title?: string;
    description?: string;
    noteTitle?: string;
    noteText?: string;
    metaItems?: GovernanceCompactMetaItem[];
    maxWidth?: string;
    align?: 'start' | 'center';
  }>(),
  {
    eyebrow: '',
    title: '',
    description: '',
    noteTitle: '',
    noteText: '',
    metaItems: () => [],
    maxWidth: '',
    align: 'center'
  }
);

const slots = useSlots();

/**
 * 头部和底部都按需渲染。
 * 页面如果只想保留字段区和右下角按钮，可以直接不传标题、说明和提示文案。
 */
const hasHeader = computed(() => Boolean(props.eyebrow || props.title || props.description || props.metaItems.length || slots.actions));
const hasFooter = computed(() => Boolean(props.noteTitle || props.noteText || slots.note || slots.footerActions));
</script>

<style scoped>
.governance-compact-query-panel {
  position: relative;
  overflow: hidden;
  width: min(100%, var(--query-panel-max-width));
  padding: 12px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 30px;
  background:
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.1), transparent 28%),
    radial-gradient(circle at bottom right, rgba(45, 212, 191, 0.1), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(245, 249, 255, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 20px 48px rgba(15, 23, 42, 0.08);
}

.governance-compact-query-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 18px;
  width: 180px;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1769ff, #11c5b7);
  opacity: 0.88;
}

.governance-compact-query-panel::after {
  content: '';
  position: absolute;
  top: -72px;
  right: -54px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.14), transparent 72%);
  pointer-events: none;
}

.governance-compact-query-panel.align-start {
  margin-right: auto;
}

.governance-compact-query-panel.align-center {
  margin-inline: auto;
}

.governance-compact-query-panel__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.governance-compact-query-panel__heading {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.governance-compact-query-panel__eyebrow {
  margin: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #7c8aa5;
}

.governance-compact-query-panel__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.governance-compact-query-panel__title-row h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.14;
  color: #0f172a;
}

.governance-compact-query-panel__meta,
.governance-compact-query-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.governance-compact-query-panel__meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.88);
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  font-size: 11px;
  font-weight: 600;
}

.governance-compact-query-panel__meta-pill.tone-blue {
  border-color: rgba(96, 165, 250, 0.36);
}

.governance-compact-query-panel__meta-pill.tone-green {
  border-color: rgba(52, 211, 153, 0.32);
}

.governance-compact-query-panel__meta-pill.tone-teal {
  border-color: rgba(45, 212, 191, 0.3);
}

.governance-compact-query-panel__meta-pill.tone-gold {
  border-color: rgba(251, 191, 36, 0.34);
}

.governance-compact-query-panel__meta-pill.tone-violet {
  border-color: rgba(167, 139, 250, 0.32);
}

.governance-compact-query-panel__description {
  margin: 0;
  max-width: 560px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.62;
}

.governance-compact-query-panel__actions {
  justify-content: flex-end;
}

.governance-compact-query-panel__actions :deep(.arco-btn) {
  min-width: 110px;
  height: 38px;
  padding: 0 15px;
  border-radius: 999px;
  font-weight: 700;
}

.governance-compact-query-panel__actions :deep(.arco-btn-primary) {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 12px 24px rgba(23, 105, 255, 0.18);
}

.governance-compact-query-panel__body {
  position: relative;
  margin-top: 10px;
  padding: 14px 16px 4px;
  border-radius: 24px;
  border: 1px solid rgba(218, 227, 240, 0.96);
  background: linear-gradient(180deg, rgba(251, 253, 255, 0.98), rgba(245, 249, 255, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 10px 24px rgba(15, 23, 42, 0.04);
}

.governance-compact-query-panel__body.no-header {
  margin-top: 0;
}

.governance-compact-query-panel.is-minimal .governance-compact-query-panel__body {
  padding-top: 16px;
}

.governance-compact-query-panel__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-top: 10px;
  padding: 6px 6px 2px;
}

.governance-compact-query-panel__footer:not(.has-note) {
  justify-content: flex-end;
}

.governance-compact-query-panel__note-wrap,
.governance-compact-query-panel__footer-actions {
  display: flex;
  align-items: center;
}

.governance-compact-query-panel__note {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.65;
}

.governance-compact-query-panel__note strong {
  color: #0f172a;
}

.governance-compact-query-panel__footer-actions {
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
}

.governance-compact-query-panel__footer-actions :deep(.arco-btn) {
  min-width: 116px;
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  font-weight: 700;
}

.governance-compact-query-panel__footer-actions :deep(.arco-btn-primary) {
  border: 0;
  background: linear-gradient(135deg, #1769ff, #12b8a6);
  box-shadow: 0 14px 28px rgba(23, 105, 255, 0.18);
}

.governance-compact-query-panel__footer-actions :deep(.arco-btn:not(.arco-btn-primary)) {
  border-color: rgba(217, 226, 237, 0.96);
  background: rgba(255, 255, 255, 0.92);
  color: #475569;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.governance-compact-query-panel__body :deep(.governance-form-sections) {
  gap: 0;
}

.governance-compact-query-panel__body :deep(.governance-form-panel__fields.layout-grid) {
  gap: 0 10px;
}

.governance-compact-query-panel__body :deep(.governance-form-panel.variant-embedded .governance-form-panel__heading) {
  margin-bottom: 4px;
}

.governance-compact-query-panel__body :deep(.governance-form-panel.variant-embedded .governance-form-panel__heading h3) {
  font-size: 15px;
}

.governance-compact-query-panel__body :deep(.governance-form-panel.variant-embedded .governance-form-panel__heading p) {
  margin-top: 2px;
  font-size: 12px;
}

.governance-compact-query-panel__body :deep(.arco-form-item) {
  margin-bottom: 6px;
}

.governance-compact-query-panel__body :deep(.arco-form-item-label-col) {
  padding-bottom: 3px;
}

.governance-compact-query-panel__body :deep(.arco-form-item-label-col > label) {
  color: #1e293b;
  font-weight: 700;
  font-size: 11px;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.governance-compact-query-panel__body :deep(.arco-input-wrapper),
.governance-compact-query-panel__body :deep(.arco-select-view) {
  min-height: 36px;
  border-radius: 14px;
  border-color: #d7e2ef;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.94);
  transition: all 0.2s ease;
}

.governance-compact-query-panel__body :deep(.arco-input-wrapper:hover),
.governance-compact-query-panel__body :deep(.arco-select-view:hover) {
  border-color: rgba(23, 105, 255, 0.28);
  background: #ffffff;
}

.governance-compact-query-panel__body :deep(.arco-input-wrapper.arco-input-focus),
.governance-compact-query-panel__body :deep(.arco-select-view.arco-select-view-focus) {
  border-color: rgba(23, 105, 255, 0.44);
  box-shadow: 0 0 0 4px rgba(23, 105, 255, 0.08);
}

.governance-compact-query-panel__body :deep(.arco-input),
.governance-compact-query-panel__body :deep(.arco-select-view-value),
.governance-compact-query-panel__body :deep(.arco-select-view-placeholder) {
  font-size: 12px;
}

.governance-compact-query-panel__body :deep(.arco-input::placeholder) {
  color: #94a3b8;
}

@media (max-width: 1280px) {
  .governance-compact-query-panel__header {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .governance-compact-query-panel__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .governance-compact-query-panel {
    padding: 14px;
    border-radius: 24px;
  }

  .governance-compact-query-panel__title-row h2 {
    font-size: 20px;
  }

  .governance-compact-query-panel__body {
    padding: 12px 12px 2px;
    border-radius: 20px;
  }

  .governance-compact-query-panel__actions :deep(.arco-btn) {
    width: 100%;
  }

  .governance-compact-query-panel__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .governance-compact-query-panel__footer-actions {
    margin-left: 0;
  }

  .governance-compact-query-panel__footer-actions :deep(.arco-btn) {
    width: 100%;
  }
}
</style>
