<template>
  <section
    class="api-environment-whitelist-editor"
    :class="{
      'is-compact': !showHeading && !showCallout,
      'surface-flat': surfaceMode === 'flat'
    }"
  >
    <div v-if="showHeading" class="api-environment-whitelist-editor__heading">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <a-tag :color="getEnvironmentTagColor(accessEnvironment)">
        当前生效：{{ getAccessEnvironmentLabel(accessEnvironment) }}
      </a-tag>
    </div>

    <div v-if="showCallout" class="api-environment-whitelist-editor__callout">
      <div class="api-environment-whitelist-editor__callout-main">
        <strong>环境独立治理</strong>
        <p>推荐按环境分开维护来源 IP，避免测试流量误入生产白名单，同时减少联调切换时的重复配置。</p>
      </div>
      <div class="api-environment-whitelist-editor__callout-meta">
        <span>当前生效条目</span>
        <strong>{{ getEnvironmentWhitelistCountLabel(modelValue[accessEnvironment]) }}</strong>
      </div>
    </div>

    <div class="api-environment-whitelist-editor__grid">
      <div
        v-for="environment in environmentOptions"
        :key="environment.value"
        class="api-environment-whitelist-editor__card"
        :class="{ active: accessEnvironment === environment.value }"
      >
        <div class="api-environment-whitelist-editor__card-head">
          <div class="api-environment-whitelist-editor__card-title">
            <a-tag :color="getEnvironmentTagColor(environment.value)">{{ environment.label }}</a-tag>
            <strong>{{ accessEnvironment === environment.value ? '当前生效环境' : '待命环境' }}</strong>
          </div>
          <span>{{ getEnvironmentWhitelistCountLabel(modelValue[environment.value]) }}</span>
        </div>

        <a-textarea
          :model-value="modelValue[environment.value]"
          :max-length="1000"
          :auto-size="{ minRows: 5, maxRows: 8 }"
          placeholder="每行一个 IP 或 CIDR，例如 203.0.113.10 或 10.0.0.0/24"
          @update:model-value="(value: string | number | undefined) => handleValueChange(environment.value, value)"
        />
        <small class="api-environment-whitelist-editor__card-helper">
          支持换行、英文逗号、中文逗号、分号混合分隔，保存时自动标准化和去重。
        </small>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
import type { AccessEnvironment, EnvironmentIpWhitelistTextMap } from '../../types/apiAccount';
import { getAccessEnvironmentLabel, getEnvironmentTagColor } from '../../utils/api-account-governance';

type EnvironmentOption = {
  label: string;
  value: AccessEnvironment;
};

const props = withDefaults(defineProps<{
  modelValue: EnvironmentIpWhitelistTextMap;
  accessEnvironment: AccessEnvironment;
  environmentOptions: EnvironmentOption[];
  title?: string;
  description?: string;
  showHeading?: boolean;
  showCallout?: boolean;
  surfaceMode?: 'card' | 'flat';
}>(), {
  title: '按环境维护 IP 白名单',
  description: '测试、预发、生产独立治理，系统会根据当前接入环境自动选择生效清单。',
  showHeading: true,
  showCallout: true,
  surfaceMode: 'card'
});

const emit = defineEmits<{
  'update:modelValue': [value: EnvironmentIpWhitelistTextMap];
}>();

/**
 * 环境白名单编辑器独立收口白名单治理区的卡片布局和输入交互。
 * 后续如需复用到其他账号、应用或租户治理页，可直接复用本组件。
 */
function handleValueChange(environment: AccessEnvironment, value: string | number | undefined) {
  emit('update:modelValue', {
    ...props.modelValue,
    [environment]: String(value ?? '')
  });
}

function getEnvironmentWhitelistCountLabel(value: string) {
  const count = Array.from(new Set((value || '').split(/[\n,，;；]+/).map(item => item.trim()).filter(Boolean))).length;
  return count > 0 ? `${count} 条来源` : '未限制';
}
</script>

<style scoped>
.api-environment-whitelist-editor {
  padding: 22px;
  border: 1px solid #e5edf6;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
}

/**
 * 无卡片表面模式：
 * 由业务页按需开启，直接在宿主区域平铺展示字段，不额外绘制外层卡片壳。
 */
.api-environment-whitelist-editor.surface-flat {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.api-environment-whitelist-editor.surface-flat .api-environment-whitelist-editor__grid {
  gap: 16px;
  margin-top: 8px;
}

.api-environment-whitelist-editor__heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.api-environment-whitelist-editor__heading h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.api-environment-whitelist-editor__heading p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.api-environment-whitelist-editor__callout {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid rgba(37, 99, 235, 0.08);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.06), rgba(16, 185, 129, 0.08));
}

.api-environment-whitelist-editor__callout-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.api-environment-whitelist-editor__callout-main strong,
.api-environment-whitelist-editor__callout-meta strong {
  color: #0f172a;
}

.api-environment-whitelist-editor__callout-main p,
.api-environment-whitelist-editor__callout-meta span {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.api-environment-whitelist-editor__callout-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 150px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.api-environment-whitelist-editor__grid {
  display: grid;
  /**
   * 使用自适应列而非固定 3 列：
   * 在不同弹层宽度下自动决定列数，避免右侧卡片被挤出可视区。
   */
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
  margin-top: 18px;
}

/**
 * 紧凑模式：
 * 当上方标题和治理提示卡都关闭时，白名单卡片区直接贴顶展示，避免出现多余留白。
 */
.api-environment-whitelist-editor.is-compact .api-environment-whitelist-editor__grid {
  margin-top: 0;
}

.api-environment-whitelist-editor__card {
  border: 1px solid #e5edf6;
  border-radius: 24px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.api-environment-whitelist-editor.surface-flat .api-environment-whitelist-editor__card {
  border: 0;
  border-radius: 0;
  padding: 14px 0 10px;
  background: transparent;
  box-shadow: none;
}

.api-environment-whitelist-editor.surface-flat .api-environment-whitelist-editor__card + .api-environment-whitelist-editor__card {
  border-top: 1px dashed rgba(203, 213, 225, 0.9);
  padding-top: 16px;
}

.api-environment-whitelist-editor.surface-flat .api-environment-whitelist-editor__card-head {
  margin-bottom: 10px;
}

.api-environment-whitelist-editor.surface-flat .api-environment-whitelist-editor__card.active {
  border-color: transparent;
  box-shadow: none;
}

.api-environment-whitelist-editor.surface-flat .api-environment-whitelist-editor__card-head span {
  color: #0f5de2;
  font-weight: 700;
}

.api-environment-whitelist-editor.surface-flat :deep(.arco-textarea-wrapper) {
  border-color: rgba(191, 206, 228, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.95));
}

.api-environment-whitelist-editor.surface-flat :deep(.arco-textarea-wrapper:hover) {
  border-color: rgba(47, 109, 246, 0.42);
}

.api-environment-whitelist-editor.surface-flat :deep(.arco-textarea-focus) {
  border-color: rgba(47, 109, 246, 0.62);
  box-shadow: 0 0 0 3px rgba(47, 109, 246, 0.14);
}

.api-environment-whitelist-editor__card.active {
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.12);
}

.api-environment-whitelist-editor__card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.api-environment-whitelist-editor__card-title {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.api-environment-whitelist-editor__card-title strong {
  color: #0f172a;
  font-size: 13px;
}

.api-environment-whitelist-editor__card-head span,
.api-environment-whitelist-editor__card-helper {
  color: #64748b;
  line-height: 1.6;
}

.api-environment-whitelist-editor :deep(.arco-textarea-wrapper) {
  border-radius: 16px;
  border-color: #dde6f1;
  background: #f8fbff;
}

.api-environment-whitelist-editor :deep(.arco-textarea-wrapper:hover) {
  border-color: rgba(23, 105, 255, 0.34);
  background: #ffffff;
}

.api-environment-whitelist-editor :deep(.arco-textarea-focus) {
  border-color: rgba(23, 105, 255, 0.46);
  box-shadow: 0 0 0 4px rgba(23, 105, 255, 0.08);
  background: #ffffff;
}

@media (max-width: 1280px) {
  .api-environment-whitelist-editor__callout {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .api-environment-whitelist-editor {
    padding: 18px;
  }

  .api-environment-whitelist-editor__heading {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
