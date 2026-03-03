<template>
  <div
    class="governance-form-sections"
    :class="`label-layout-${labelLayout}`"
    :style="sectionRootStyle"
  >
    <section
      v-for="section in sections"
      :key="section.key"
      class="governance-form-panel"
      :class="`variant-${variant}`"
    >
      <div v-if="!section.hideHeader" class="governance-form-panel__heading">
        <div class="governance-form-panel__heading-main">
          <span
            v-if="headingLevelMark"
            class="governance-form-panel__heading-level"
            :title="headingLevelText || undefined"
          >
            <span class="governance-form-panel__heading-level-index">{{ headingLevelMark }}</span>
            <small v-if="headingLevelText" class="governance-form-panel__heading-level-text">{{ headingLevelText }}</small>
          </span>
          <h3>{{ section.title }}</h3>
          <p>{{ section.description }}</p>
        </div>
      </div>

      <div
        class="governance-form-panel__fields"
        :class="resolveFieldGridClass(section)"
      >
        <a-form-item
          v-for="field in section.fields"
          :key="field.key"
          class="governance-form-panel__item"
          :class="{
            'span-2': section.layout !== 'single' && field.colSpan === 2
          }"
          :field="field.field"
          :label="field.label"
          :required="field.required"
        >
          <component
            :is="resolveFieldComponent(field.kind)"
            :model-value="getFieldValue(field.field)"
            v-bind="field.componentProps"
            @update:model-value="(value: unknown) => setFieldValue(field.field, value)"
          />
          <div v-if="field.helper" class="governance-form-panel__helper">{{ field.helper }}</div>
        </a-form-item>
      </div>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type {
  GovernanceFieldKind,
  GovernanceFormSectionSchema
} from '../../types/governance';

const props = withDefaults(defineProps<{
  model: Record<string, unknown>;
  sections: GovernanceFormSectionSchema[];
  variant?: 'card' | 'embedded';
  labelLayout?: 'stacked' | 'inline';
  inlineLabelWidth?: string;
  headingLevelMark?: string;
  headingLevelText?: string;
}>(), {
  variant: 'card',
  labelLayout: 'stacked',
  inlineLabelWidth: '108px',
  headingLevelMark: '',
  headingLevelText: ''
});

/**
 * 通过 CSS 变量暴露行内标签宽度，业务页可按场景细调，
 * 同时保持组件内部布局规则不变，减少重复样式代码。
 */
const sectionRootStyle = computed(() => {
  if (props.labelLayout !== 'inline') {
    return undefined;
  }

  return {
    '--governance-inline-label-width': props.inlineLabelWidth
  };
});

/**
 * 通用治理表单分区渲染器。
 * 通过字段 schema 统一渲染输入框、选择器、数字框、日期框和文本域，
 * 让业务页面在新增字段时优先维护配置，而不是反复修改模板结构。
 */
function resolveFieldComponent(kind: GovernanceFieldKind) {
  return {
    input: 'a-input',
    select: 'a-select',
    'input-number': 'a-input-number',
    'date-picker': 'a-date-picker',
    textarea: 'a-textarea'
  }[kind];
}

function getFieldValue(field: string) {
  return props.model[field];
}

function setFieldValue(field: string, value: unknown) {
  props.model[field] = value;
}

/**
 * 按分区配置返回字段网格类名。
 * 查询区等轻量表单可以通过 columns 控制 3 到 6 列布局，
 * 其他页面继续使用默认 2 列布局即可。
 */
function resolveFieldGridClass(section: GovernanceFormSectionSchema) {
  if (section.layout === 'single') {
    return 'layout-single';
  }

  return ['layout-grid', `columns-${section.columns || 2}`];
}

const headingLevelMark = computed(() => props.headingLevelMark.trim());
const headingLevelText = computed(() => props.headingLevelText.trim());
</script>

<style scoped>
.governance-form-sections {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.governance-form-sections.label-layout-inline {
  --governance-inline-label-width: 108px;
  --governance-inline-label-gap: 10px;
}

.governance-form-panel {
  padding: 22px;
  border: 1px solid #e5edf6;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
}

.governance-form-panel.variant-embedded {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.governance-form-panel__heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.governance-form-panel__heading-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.governance-form-panel__heading-level {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  max-width: 100%;
  padding: 2px 10px 2px 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.12), rgba(18, 184, 166, 0.16));
  color: #0f5de2;
}

.governance-form-panel__heading-level-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  min-width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #ffffff;
  color: #0f5de2;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.governance-form-panel__heading-level-text {
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  color: #0f5de2;
  white-space: nowrap;
}

.governance-form-panel.variant-embedded .governance-form-panel__heading {
  margin-bottom: 14px;
}

.governance-form-panel__heading h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.governance-form-panel__heading p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.governance-form-panel__fields.layout-grid {
  display: grid;
  gap: 0 18px;
}

.governance-form-panel__fields.layout-grid.columns-1 {
  grid-template-columns: 1fr;
}

.governance-form-panel__fields.layout-grid.columns-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.governance-form-panel__fields.layout-grid.columns-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.governance-form-panel__fields.layout-grid.columns-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.governance-form-panel__fields.layout-grid.columns-5 {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.governance-form-panel__fields.layout-grid.columns-6 {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.governance-form-panel__fields.layout-single {
  display: grid;
  grid-template-columns: 1fr;
}

.governance-form-panel__item.span-2 {
  grid-column: span 2;
}

.governance-form-panel__helper {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.7;
}

.governance-form-panel :deep(.arco-form-item) {
  margin-bottom: 18px;
}

.governance-form-panel :deep(.arco-form-item-label-col > label) {
  color: #0f172a;
  font-weight: 600;
}

.governance-form-sections.label-layout-inline :deep(.arco-form-item.arco-form-item-layout-vertical) {
  display: flex;
  align-items: center;
  gap: var(--governance-inline-label-gap);
}

.governance-form-sections.label-layout-inline :deep(.arco-form-item-layout-vertical > .arco-form-item-label-col) {
  width: var(--governance-inline-label-width);
  max-width: var(--governance-inline-label-width);
  margin-bottom: 0;
  padding: 0;
  justify-content: flex-end;
  line-height: 1.2;
  white-space: nowrap;
}

.governance-form-sections.label-layout-inline :deep(.arco-form-item-label-col > label) {
  display: inline-flex;
  justify-content: flex-end;
  width: 100%;
  white-space: nowrap;
}

.governance-form-sections.label-layout-inline :deep(.arco-form-item-wrapper-col) {
  flex: 1;
  width: auto;
  min-width: 0;
}

.governance-form-panel :deep(.arco-input-wrapper),
.governance-form-panel :deep(.arco-textarea-wrapper),
.governance-form-panel :deep(.arco-select-view),
.governance-form-panel :deep(.arco-picker),
.governance-form-panel :deep(.arco-input-number) {
  border-radius: 16px;
  border-color: #dde6f1;
  background: #f8fbff;
  transition: all 0.2s ease;
}

.governance-form-panel :deep(.arco-input-wrapper:hover),
.governance-form-panel :deep(.arco-textarea-wrapper:hover),
.governance-form-panel :deep(.arco-select-view:hover),
.governance-form-panel :deep(.arco-picker:hover),
.governance-form-panel :deep(.arco-input-number:hover) {
  border-color: rgba(23, 105, 255, 0.34);
  background: #ffffff;
}

.governance-form-panel :deep(.arco-input-wrapper.arco-input-focus),
.governance-form-panel :deep(.arco-textarea-focus),
.governance-form-panel :deep(.arco-select-view.arco-select-view-focus),
.governance-form-panel :deep(.arco-picker-focus),
.governance-form-panel :deep(.arco-input-number-focus) {
  border-color: rgba(23, 105, 255, 0.46);
  box-shadow: 0 0 0 4px rgba(23, 105, 255, 0.08);
  background: #ffffff;
}

@media (max-width: 1760px) {
  .governance-form-panel__fields.layout-grid.columns-6 {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (max-width: 1560px) {
  .governance-form-panel__fields.layout-grid.columns-5,
  .governance-form-panel__fields.layout-grid.columns-6 {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1320px) {
  .governance-form-panel__fields.layout-grid.columns-4,
  .governance-form-panel__fields.layout-grid.columns-5,
  .governance-form-panel__fields.layout-grid.columns-6 {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .governance-form-panel__fields.layout-grid.columns-3,
  .governance-form-panel__fields.layout-grid.columns-4,
  .governance-form-panel__fields.layout-grid.columns-5,
  .governance-form-panel__fields.layout-grid.columns-6 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .governance-form-panel__item.span-2 {
    grid-column: span 1;
  }
}

@media (max-width: 840px) {
  .governance-form-panel__fields.layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .governance-form-panel {
    padding: 18px;
  }

  .governance-form-panel.variant-embedded {
    padding: 0;
  }

  .governance-form-panel__heading {
    flex-direction: column;
    align-items: stretch;
  }

  .governance-form-panel__heading-level-text {
    white-space: normal;
  }

  .governance-form-sections.label-layout-inline {
    --governance-inline-label-width: 96px;
    --governance-inline-label-gap: 8px;
  }
}
</style>
