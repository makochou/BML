<template>
  <div class="api-callback-log-table">
    <a-table
      class="api-callback-log-table__table"
      row-key="id"
      :data="data"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      @page-change="$emit('pageChange', $event)"
      @page-size-change="$emit('pageSizeChange', $event)"
    >
      <template #columns>
        <a-table-column
          v-for="column in activeColumns"
          :key="column.key"
          :title="column.title"
          :data-index="column.dataIndex"
          :width="column.width"
          :fixed="column.fixed"
          :ellipsis="column.ellipsis"
          :tooltip="column.tooltip"
        >
          <template #cell="{ record }">
            <a-typography-text v-if="column.kind === 'logId'" copyable>#{{ record.id }}</a-typography-text>
            <div v-else-if="column.kind === 'event'" class="stack">
              <strong>{{ record.eventType }}</strong>
              <small>{{ record.businessType }}</small>
            </div>
            <a-tag v-else-if="column.kind === 'status'" :color="getCallbackStatusColor(record.callbackStatus)">{{ getCallbackStatusLabel(record.callbackStatus) }}</a-tag>
            <span v-else-if="column.kind === 'retryCount'">{{ record.retryCount }} / {{ record.maxRetryCount }}</span>
            <span v-else-if="column.kind === 'callbackUrl'" class="ellipsis" :title="record.callbackUrl">{{ record.callbackUrl }}</span>
            <span v-else-if="column.kind === 'nextRetryTime'">{{ formatDisplayDateTime(record.nextRetryTime) }}</span>
            <span v-else-if="column.kind === 'lastCallbackTime' || column.kind === 'time'">{{ formatDisplayDateTime(record.lastCallbackTime || record.createTime) }}</span>
            <span v-else-if="column.kind === 'lastError'" class="ellipsis" :title="record.lastErrorMessage || '无'">{{ record.lastErrorMessage || '无' }}</span>
            <span v-else-if="column.kind === 'createTime'">{{ formatDisplayDateTime(record.createTime) }}</span>
            <div v-else-if="column.kind === 'actions'" class="table-action-row">
              <a-button
                v-if="isCallbackRetryable(record.callbackStatus)"
                class="table-action-btn"
                type="text"
                size="small"
                :loading="retryingId === record.id"
                @click="$emit('retry', record)"
              >
                立即重试
              </a-button>
              <span v-else class="muted">无需重试</span>
            </div>
            <span v-else>{{ getPlainText(record, column.dataIndex, '-') }}</span>
          </template>
        </a-table-column>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { defineTableColumns, useTableColumns } from '../../composables/useTableColumns';
import type { ApiCallbackLogItem } from '../../types/apiAccount';
import { formatDisplayDateTime, getCallbackStatusColor, getCallbackStatusLabel, isCallbackRetryable } from '../../utils/api-account-governance';

type CallbackTableMode = 'full' | 'preview';
type CallbackTableColumnKind = 'logId' | 'event' | 'status' | 'retryCount' | 'plain' | 'callbackUrl' | 'nextRetryTime' | 'lastCallbackTime' | 'lastError' | 'createTime' | 'actions' | 'time';

const props = withDefaults(defineProps<{
  data: ApiCallbackLogItem[];
  loading?: boolean;
  pagination?: false | Record<string, unknown>;
  scrollX?: number;
  retryingId?: number | null;
  mode?: CallbackTableMode;
}>(), {
  loading: false,
  pagination: false,
  scrollX: 1900,
  retryingId: null,
  mode: 'full'
});

defineEmits<{
  retry: [record: ApiCallbackLogItem];
  pageChange: [page: number];
  pageSizeChange: [pageSize: number];
}>();

/**
 * 回调日志表统一服务于“完整工作台”和“详情预览”两种场景。
 * 通过 `mode` 控制列集，避免维护两套相近但长期容易漂移的日志表模板。
 */
const fullColumnModel = defineTableColumns<CallbackTableColumnKind>([
  { key: 'id', title: '日志ID', kind: 'logId', width: 120 },
  { key: 'event', title: '事件', kind: 'event', width: 220 },
  { key: 'callbackStatus', title: '状态', kind: 'status', width: 120 },
  { key: 'retryCount', title: '重试次数', kind: 'retryCount', width: 130 },
  { key: 'responseStatusCode', title: '响应状态码', kind: 'plain', width: 120, dataIndex: 'responseStatusCode' },
  { key: 'callbackUrl', title: '回调地址', kind: 'callbackUrl', width: 300, dataIndex: 'callbackUrl', ellipsis: true, tooltip: true },
  { key: 'nextRetryTime', title: '下次重试', kind: 'nextRetryTime', width: 180 },
  { key: 'lastCallbackTime', title: '最近回调时间', kind: 'lastCallbackTime', width: 180 },
  { key: 'lastErrorMessage', title: '最近错误', kind: 'lastError', width: 240 },
  { key: 'createTime', title: '创建时间', kind: 'createTime', width: 180 },
  { key: 'actions', title: '操作', kind: 'actions', width: 140, fixed: 'right' }
]);

const previewColumnModel = defineTableColumns<CallbackTableColumnKind>([
  { key: 'event', title: '事件', kind: 'event', width: 220 },
  { key: 'callbackStatus', title: '状态', kind: 'status', width: 120 },
  { key: 'retryCount', title: '重试次数', kind: 'retryCount', width: 120 },
  { key: 'callbackUrl', title: '回调地址', kind: 'callbackUrl', width: 360, dataIndex: 'callbackUrl', ellipsis: true, tooltip: true },
  { key: 'responseStatusCode', title: '响应状态码', kind: 'plain', width: 140, dataIndex: 'responseStatusCode' },
  { key: 'lastCallbackTime', title: '最近回调时间', kind: 'time', width: 180 }
]);

const { columns: fullColumns, getPlainText } = useTableColumns(fullColumnModel);
const { columns: previewColumns } = useTableColumns(previewColumnModel);
const activeColumns = computed(() => props.mode === 'preview' ? previewColumns.value : fullColumns.value);
</script>

<style scoped>
.api-callback-log-table__table :deep(.arco-table-container) {
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  overflow: hidden;
}

.api-callback-log-table__table :deep(.arco-table-th) {
  background: linear-gradient(180deg, rgba(239, 244, 255, 0.96), rgba(248, 250, 252, 0.96));
  color: #475569;
  font-weight: 700;
}

.api-callback-log-table__table :deep(.arco-table-td) {
  background: rgba(255, 255, 255, 0.96);
}

.table-action-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-action-btn {
  padding: 0;
  font-weight: 600;
}

.stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stack small,
.muted {
  color: #64748b;
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
