<template>
  <a-drawer
    :visible="visible"
    width="1180px"
    :header="false"
    :footer="false"
    render-to-body
    unmount-on-close
    @update:visible="$emit('update:visible', $event)"
  >
    <GovernanceWorkbenchShell
      class="drawer callback-workbench"
      eyebrow="Callback Delivery Operations Center"
      title="回调日志与重试工作台"
      description="统一查看指定账号的回调投递结果、重试状态、响应结果与错误信息，便于联调、问题排查和失败补投。"
      :tags="heroTags"
      :stats="heroStats"
      theme="teal"
      sticky-hero
    >
      <template #heroActions>
        <a-button
          type="primary"
          :disabled="!account"
          :loading="testing"
          @click="$emit('triggerTest')"
        >
          发送测试回调
        </a-button>
        <a-button @click="$emit('update:visible', false)">
          <template #icon><icon-close /></template>
          关闭工作台
        </a-button>
      </template>

      <template #aside>
        <GovernancePanel class="callback-panel callback-panel--identity" title="账号画像">
          <GovernanceFactGrid
            class="callback-fact-grid"
            card-class="callback-fact-card"
            copy-class="callback-fact-card__copy"
            :items="accountFacts"
          />
        </GovernancePanel>

        <GovernancePanel class="callback-panel" title="日志筛选">
          <a-form :model="filters" layout="vertical" class="callback-filter-form">
            <GovernanceFormSections
              class="callback-filter-schema"
              :model="filterModel"
              :sections="callbackFilterSections"
              variant="embedded"
              label-layout="inline"
            />
          </a-form>
          <div class="callback-filter-actions">
            <a-button type="primary" @click="$emit('search')">查询日志</a-button>
            <a-button @click="$emit('reset')">重置条件</a-button>
          </div>
        </GovernancePanel>

        <GovernancePanel class="callback-panel callback-panel--guide" title="联调建议">
          <ul class="callback-guide-list">
            <li v-for="item in guideItems" :key="item">{{ item }}</li>
          </ul>
        </GovernancePanel>
      </template>

      <GovernancePanel class="callback-panel">
        <GovernanceStatGrid
          class="callback-summary-grid"
          card-class="callback-summary-card"
          :items="summaryCards"
        />
      </GovernancePanel>

      <section class="callback-panel callback-panel--table grow">
        <div class="section-heading section-heading--row">
          <div>
            <h3>投递日志</h3>
            <p>
              记录每一次业务回调的地址、状态码、错误信息和重试轨迹，支持对异常记录进行即时补投。
            </p>
          </div>
          <div class="callback-table-actions">
            <a-button
              type="primary"
              :disabled="!account"
              :loading="testing"
              @click="$emit('triggerTest')"
            >
              发送测试回调
            </a-button>
          </div>
        </div>
        <ApiCallbackLogTable
          :data="logs"
          :loading="loading"
          :pagination="paginationConfig"
          :retrying-id="retryingId"
          mode="full"
          @page-change="page => $emit('pageChange', page)"
          @page-size-change="pageSize => $emit('pageSizeChange', pageSize)"
          @retry="record => $emit('retry', record)"
        />
      </section>
    </GovernanceWorkbenchShell>
  </a-drawer>
</template>

<script lang="ts" setup>
import { IconClose } from '@arco-design/web-vue/es/icon';
import { useApiAccountCallbackLogFilterSchema } from '../../composables/useApiAccountCallbackLogFilterSchema';
import type {
  ApiAccountItem,
  ApiCallbackLogFilterModel,
  ApiCallbackLogItem
} from '../../types/apiAccount';
import type { FactCard, WorkbenchStatCard } from '../../types/governance';
import { callbackStatusOptions } from '../../utils/api-account-governance';
import ApiCallbackLogTable from './ApiCallbackLogTable.vue';
import GovernanceFactGrid from '../governance/GovernanceFactGrid.vue';
import GovernanceFormSections from '../governance/GovernanceFormSections.vue';
import GovernancePanel from '../governance/GovernancePanel.vue';
import GovernanceStatGrid from '../governance/GovernanceStatGrid.vue';
import GovernanceWorkbenchShell from '../governance/GovernanceWorkbenchShell.vue';

const props = defineProps<{
  visible: boolean;
  account: ApiAccountItem | null;
  loading: boolean;
  testing: boolean;
  retryingId: number | null;
  logs: ApiCallbackLogItem[];
  filters: ApiCallbackLogFilterModel;
  paginationConfig: Record<string, unknown>;
  heroTags: string[];
  heroStats: WorkbenchStatCard[];
  accountFacts: FactCard[];
  guideItems: string[];
  summaryCards: WorkbenchStatCard[];
}>();

defineEmits<{
  'update:visible': [value: boolean];
  search: [];
  reset: [];
  pageChange: [page: number];
  pageSizeChange: [pageSize: number];
  triggerTest: [];
  retry: [record: ApiCallbackLogItem];
}>();

const filterModel = props.filters as unknown as Record<string, unknown>;
const { sections: callbackFilterSections } = useApiAccountCallbackLogFilterSchema({
  callbackStatusOptions
});
</script>

<style scoped>
.callback-workbench {
  gap: 20px;
}

.callback-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid #e5edf6;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
  padding: 20px;
}

.callback-panel--identity {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(238, 250, 247, 0.96));
}

.callback-fact-grid,
.callback-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.callback-fact-card,
.callback-summary-card {
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.05);
  padding: 18px;
}

.callback-fact-card span,
.callback-fact-card small,
.callback-summary-card span,
.callback-summary-card small {
  color: #64748b;
}

.callback-fact-card strong,
.callback-summary-card strong {
  color: #0f172a;
}

.callback-fact-card__copy {
  font-weight: 700;
}

.callback-filter-form :deep(.arco-form-item-label-col > label) {
  color: #334155;
  font-weight: 600;
}

.callback-filter-form :deep(.arco-select-view) {
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.96);
}

.callback-filter-schema :deep(.governance-form-panel__fields) {
  gap: 0;
}

.callback-filter-schema :deep(.arco-form-item:last-child) {
  margin-bottom: 0;
}

.callback-filter-actions,
.callback-table-actions,
.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.callback-filter-actions {
  margin-top: 6px;
}

.callback-guide-list {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  line-height: 1.8;
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.section-heading p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .callback-fact-grid,
  .callback-summary-grid {
    grid-template-columns: 1fr;
  }

  .callback-filter-actions,
  .callback-table-actions,
  .section-heading {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
