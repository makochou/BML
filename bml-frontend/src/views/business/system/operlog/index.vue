<template>
  <div class="page-wrapper operlog-page">
    <GovernanceCompactQueryPanel
      eyebrow="AUDIT TRAIL"
      title="系统操作日志"
      description="集中审计后台用户与 API 账号的关键操作，支持按模块、操作人、状态、IP 与时间范围快速追踪。"
      density="ultra"
      theme="aurora"
      :meta-items="queryMetaItems"
    >
      <template #actions>
        <a-button type="primary" @click="handleSearch">
          <template #icon><icon-search /></template>
          查询日志
        </a-button>
        <a-button @click="handleReset">
          <template #icon><icon-refresh /></template>
          重置
        </a-button>
      </template>

      <a-form :model="queryParams" layout="inline" class="biz-query-form operlog-query-form">
        <div class="biz-query-form-primary operlog-query-grid">
          <a-form-item field="title" label="模块名称">
            <a-input v-model="queryParams.title" placeholder="如：用户管理" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="operName" label="操作人员">
            <a-input v-model="queryParams.operName" placeholder="请输入账号" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="businessType" label="业务类型">
            <a-select v-model="queryParams.businessType" placeholder="全部类型" allow-clear @change="handleSearch">
              <a-option v-for="item in businessTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="status" label="操作状态">
            <a-select v-model="queryParams.status" placeholder="全部状态" allow-clear @change="handleSearch">
              <a-option :value="0">成功</a-option>
              <a-option :value="1">异常</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="requestMethod" label="请求方式">
            <a-select v-model="queryParams.requestMethod" placeholder="全部方式" allow-clear @change="handleSearch">
              <a-option v-for="item in requestMethodOptions" :key="item" :value="item">{{ item }}</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="operIp" label="来源 IP">
            <a-input v-model="queryParams.operIp" placeholder="请输入 IP" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="timeRange" label="操作时间" class="operlog-time-range">
            <a-range-picker
              v-model="timeRange"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm:ss"
              allow-clear
              @change="handleTimeRangeChange"
            />
          </a-form-item>
        </div>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage
      density="ultra"
      body-fill
      :meta-items="tableMetaItems"
    >
      <template #actions>
        <a-button v-if="hasPermission('system:operlog:export')" type="primary" status="success" @click="handleExport">
          <template #icon><icon-download /></template>
          导出 CSV
        </a-button>
        <a-button v-if="hasPermission('system:operlog:remove')" status="danger" :disabled="!selectedRowKeys.length" @click="confirmBatchDelete">
          <template #icon><icon-delete /></template>
          批量删除
        </a-button>
        <a-button v-if="hasPermission('system:operlog:clean')" status="danger" @click="confirmClean">
          <template #icon><icon-delete /></template>
          清空日志
        </a-button>
        <a-popover trigger="click" position="br" :content-style="{ padding: '0', background: 'transparent', boxShadow: 'none', border: 'none' }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting
              :items="columnSettingItems"
              :drag-state="dragState"
              @toggle-visible="toggleColumnVisible"
              @move="moveColumn"
              @toggle-fixed="toggleColumnFixed"
              @drag-start="handleDragStart"
              @drag-over="handleDragOver"
              @drop="handleDrop"
              @drag-end="handleDragEnd"
              @reset="resetColumns"
            />
          </template>
        </a-popover>
      </template>

      <a-table
        :key="tableResetKey"
        ref="tableRef"
        v-model:selected-keys="selectedRowKeys"
        :row-selection="rowSelection"
        :data="filteredData"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: scrollX, y: '100%' }"
        :scrollbar="false"
        sticky-header
        :columns="visibleColumns"
        column-resizable
        :style="tableStyle"
        @column-resize="handleColumnResize"
        @row-click="handleRowClick"
        @row-dblclick="handleView"
      >
        <template #th-title><TableColumnSearch title="模块" v-model="columnFilters.title" /></template>
        <template #th-businessType><TableColumnSearch title="业务类型" v-model="columnFilters.businessType" /></template>
        <template #th-requestMethod><TableColumnSearch title="请求方式" v-model="columnFilters.requestMethod" /></template>
        <template #th-operName><TableColumnSearch title="操作人" v-model="columnFilters.operName" /></template>
        <template #th-operIp><TableColumnSearch title="来源 IP" v-model="columnFilters.operIp" /></template>
        <template #th-status><TableColumnSearch title="状态" v-model="columnFilters.status" /></template>
        <template #th-costTime><TableColumnSearch title="耗时" v-model="columnFilters.costTime" /></template>
        <template #th-operTime><TableColumnSearch title="操作时间" v-model="columnFilters.operTime" /></template>
        <template #th-operUrl><TableColumnSearch title="请求地址" v-model="columnFilters.operUrl" /></template>

        <template #businessType="{ record }">
          <a-tag size="small" :color="businessTypeColor(record.businessType)">{{ businessTypeLabel(record.businessType) }}</a-tag>
        </template>
        <template #requestMethod="{ record }">
          <a-tag size="small" :color="requestMethodColor(record.requestMethod)">{{ record.requestMethod || '未知' }}</a-tag>
        </template>
        <template #operatorType="{ record }">
          <span>{{ operatorTypeLabel(record.operatorType) }}</span>
        </template>
        <template #status="{ record }">
          <a-tag size="small" :color="record.status === 0 ? 'green' : 'red'">{{ record.status === 0 ? '成功' : '异常' }}</a-tag>
        </template>
        <template #costTime="{ record }">
          <span :class="costTimeClass(record.costTime)">{{ record.costTime ?? 0 }} ms</span>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button v-if="hasPermission('system:operlog:query')" type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleView(record)">
              <template #icon><icon-eye /></template>
              详情
            </a-button>
            <a-button v-if="hasPermission('system:operlog:remove')" size="mini" class="table-action-btn table-action-btn--danger" @click="confirmDelete(record)">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
          </div>
        </template>
      </a-table>

      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ pagination.total }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">成功 <b>{{ successCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">异常 <b>{{ errorCount }}</b></span>
          <a-divider direction="vertical" />
          <span>已选 <b>{{ selectedRowKeys.length }}</b> 条</span>
        </div>
        <div class="biz-table-footer__actions">
          <a-pagination
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            show-total
            show-page-size
            size="small"
            @change="handlePageChange"
            @page-size-change="handlePageSizeChange"
          />
        </div>
      </div>
    </GovernanceListStage>

    <a-drawer v-model:visible="detailVisible" :width="720" unmount-on-close class="operlog-detail-drawer">
      <template #title>
        <span>操作日志详情</span>
      </template>
      <a-descriptions v-if="currentDetail" :column="2" bordered size="small" class="operlog-descriptions">
        <a-descriptions-item label="模块名称">{{ currentDetail.title || '-' }}</a-descriptions-item>
        <a-descriptions-item label="业务类型">{{ businessTypeLabel(currentDetail.businessType) }}</a-descriptions-item>
        <a-descriptions-item label="操作人员">{{ currentDetail.operName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="部门名称">{{ currentDetail.deptName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求方式">{{ currentDetail.requestMethod || '-' }}</a-descriptions-item>
        <a-descriptions-item label="来源 IP">{{ currentDetail.operIp || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ currentDetail.status === 0 ? '成功' : '异常' }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ currentDetail.costTime ?? 0 }} ms</a-descriptions-item>
        <a-descriptions-item label="请求地址" :span="2">{{ currentDetail.operUrl || '-' }}</a-descriptions-item>
        <a-descriptions-item label="方法名称" :span="2">{{ currentDetail.method || '-' }}</a-descriptions-item>
        <a-descriptions-item label="操作时间" :span="2">{{ currentDetail.operTime || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div v-if="currentDetail" class="operlog-json-section">
        <h4>请求参数</h4>
        <pre>{{ formatJsonText(currentDetail.operParam) }}</pre>
        <h4>响应结果</h4>
        <pre>{{ formatJsonText(currentDetail.jsonResult) }}</pre>
        <h4 v-if="currentDetail.errorMsg">异常信息</h4>
        <pre v-if="currentDetail.errorMsg" class="is-error">{{ currentDetail.errorMsg }}</pre>
      </div>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemOperationLog' });

import { computed, onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconDelete, IconDownload, IconEye, IconRefresh, IconSearch, IconSettings } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import { cleanOperationLogs, deleteOperationLogs, exportOperationLogs, fetchOperationLogDetail, fetchOperationLogPage, type OperationLogQuery, type OperationLogVO } from '../../../../api/system';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { resetColumnFilters, useColumnFilter } from '../../../../composables/useColumnFilter';

const businessTypeOptions = [
  { value: 0, label: '其他', color: 'gray' },
  { value: 1, label: '新增', color: 'green' },
  { value: 2, label: '修改', color: 'arcoblue' },
  { value: 3, label: '删除', color: 'red' },
  { value: 4, label: '查询', color: 'cyan' },
  { value: 5, label: '授权', color: 'purple' },
  { value: 6, label: '重置', color: 'orangered' },
  { value: 7, label: '清空', color: 'magenta' },
  { value: 8, label: '同步', color: 'gold' },
  { value: 9, label: '状态变更', color: 'lime' },
];
const requestMethodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'];
const operatorTypeMap: Record<number, string> = { 0: '其他', 1: '后台用户', 2: '移动端用户', 3: 'API账号' };

const loading = ref(false);
const tableData = ref<OperationLogVO[]>([]);
const selectedRowKeys = ref<number[]>([]);
const detailVisible = ref(false);
const currentDetail = ref<OperationLogVO | null>(null);
const timeRange = ref<string[]>([]);
const { hasPermission } = useButtonPermission();

const pagination = reactive({ current: 1, pageSize: 20, total: 0 });
const queryParams = reactive<OperationLogQuery>({
  title: '',
  businessType: undefined,
  requestMethod: undefined,
  operName: '',
  operIp: '',
  status: undefined,
  beginTime: undefined,
  endTime: undefined,
});
const columnFilters = reactive<Record<string, string>>({
  title: '', businessType: '', requestMethod: '', operName: '', operIp: '', status: '', costTime: '', operTime: '', operUrl: '',
});

const defaultColumns: BusinessTableColumn[] = [
  { key: 'title', title: '模块', dataIndex: 'title', width: 130, visible: true, fixed: 'left', sortable: true, titleSlotName: 'th-title' },
  { key: 'businessType', title: '业务类型', slotName: 'businessType', width: 120, visible: true, align: 'center', sortable: true, titleSlotName: 'th-businessType' },
  { key: 'requestMethod', title: '请求方式', slotName: 'requestMethod', width: 110, visible: true, align: 'center', sortable: true, titleSlotName: 'th-requestMethod' },
  { key: 'operName', title: '操作人', dataIndex: 'operName', width: 130, visible: true, sortable: true, titleSlotName: 'th-operName' },
  { key: 'operIp', title: '来源 IP', dataIndex: 'operIp', width: 150, visible: true, sortable: true, titleSlotName: 'th-operIp' },
  { key: 'status', title: '状态', slotName: 'status', width: 90, visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  { key: 'costTime', title: '耗时', slotName: 'costTime', width: 100, visible: true, align: 'right', sortable: true, titleSlotName: 'th-costTime' },
  { key: 'operTime', title: '操作时间', dataIndex: 'operTime', width: 180, visible: true, sortable: true, titleSlotName: 'th-operTime' },
  { key: 'operUrl', title: '请求地址', dataIndex: 'operUrl', width: 260, visible: true, ellipsis: true, sortable: true, titleSlotName: 'th-operUrl' },
  { key: 'operatorType', title: '操作类别', slotName: 'operatorType', width: 120, visible: false, align: 'center', sortable: true },
  { key: 'deptName', title: '部门名称', dataIndex: 'deptName', width: 140, visible: false, sortable: true },
  { key: 'method', title: '方法名称', dataIndex: 'method', width: 320, visible: false, ellipsis: true, sortable: true },
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const {
  visibleColumns,
  columnSettingItems,
  dragState,
  tableResetKey,
  scrollX,
  tableStyle,
  tableRef,
  handleColumnResize,
  toggleColumnVisible,
  moveColumn,
  toggleColumnFixed,
  handleDragStart,
  handleDragOver,
  handleDrop,
  handleDragEnd,
  resetColumns,
} = useBusinessTableColumns('system-operlog', defaultColumns);

const { filteredData } = useColumnFilter(tableData, columnFilters, {
  businessType: value => businessTypeLabel(Number(value)),
  status: value => Number(value) === 0 ? '成功' : '异常',
  costTime: value => `${value ?? 0} ms`,
});

const rowSelection = computed(() => ({
  type: 'checkbox' as const,
  showCheckedAll: true,
  onlyCurrent: true,
}));
const successCount = computed(() => filteredData.value.filter(item => item.status === 0).length);
const errorCount = computed(() => filteredData.value.filter(item => item.status === 1).length);
const queryMetaItems = computed(() => [
  { label: '筛选维度', value: '模块 / 操作人 / IP / 时间', tone: 'blue' as const },
  { label: '当前页成功', value: String(successCount.value), tone: 'green' as const },
  { label: '当前页异常', value: String(errorCount.value), tone: 'gold' as const },
]);
const tableMetaItems = computed(() => [
  { label: '总记录', value: String(pagination.total), tone: 'blue' as const },
  { label: '当前页', value: `${pagination.current}/${Math.max(1, Math.ceil(pagination.total / pagination.pageSize))}`, tone: 'teal' as const },
]);

const loadData = async () => {
  loading.value = true;
  try {
    const params: OperationLogQuery = {
      ...queryParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      requestMethod: queryParams.requestMethod || undefined,
      title: queryParams.title || undefined,
      operName: queryParams.operName || undefined,
      operIp: queryParams.operIp || undefined,
    };
    const res = await fetchOperationLogPage(params) as any;
    tableData.value = res.data?.records || [];
    pagination.total = Number(res.data?.total || 0);
    selectedRowKeys.value = [];
  } catch {
    tableData.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.current = 1;
  loadData();
};
const handleReset = () => {
  queryParams.title = '';
  queryParams.businessType = undefined;
  queryParams.requestMethod = undefined;
  queryParams.operName = '';
  queryParams.operIp = '';
  queryParams.status = undefined;
  queryParams.beginTime = undefined;
  queryParams.endTime = undefined;
  timeRange.value = [];
  resetColumnFilters(columnFilters);
  pagination.current = 1;
  loadData();
};
const handleTimeRangeChange = (value: string[] | undefined) => {
  queryParams.beginTime = value?.[0];
  queryParams.endTime = value?.[1];
  handleSearch();
};
const handlePageChange = (page: number) => {
  pagination.current = page;
  loadData();
};
const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size;
  pagination.current = 1;
  loadData();
};
const handleRowClick = (record: OperationLogVO) => {
  const id = Number(record.id);
  selectedRowKeys.value = selectedRowKeys.value.includes(id)
    ? selectedRowKeys.value.filter(item => item !== id)
    : [...selectedRowKeys.value, id];
};
const handleView = async (record: OperationLogVO) => {
  try {
    const res = await fetchOperationLogDetail(Number(record.id)) as any;
    currentDetail.value = res.data || record;
    detailVisible.value = true;
  } catch {
    currentDetail.value = record;
    detailVisible.value = true;
  }
};
const confirmDelete = (record: OperationLogVO) => {
  Modal.warning({
    title: '确认删除操作日志？',
    content: `即将删除模块“${record.title || '-'}”在 ${record.operTime || '-'} 产生的审计记录。`,
    hideCancel: false,
    onOk: async () => {
      await deleteOperationLogs([Number(record.id)]);
      Message.success('删除成功');
      loadData();
    },
  });
};

const handleExport = async () => {
  await exportOperationLogs(queryParams);
  Message.success('操作日志导出已开始');
};

const confirmBatchDelete = () => {
  if (!selectedRowKeys.value.length) {
    Message.warning('请先选择要删除的日志');
    return;
  }
  Modal.warning({
    title: '确认批量删除操作日志？',
    content: `即将删除选中的 ${selectedRowKeys.value.length} 条审计记录，此操作不可撤销。`,
    hideCancel: false,
    onOk: async () => {
      await deleteOperationLogs(selectedRowKeys.value.map(Number));
      Message.success('批量删除成功');
      loadData();
    },
  });
};
const confirmClean = () => {
  Modal.error({
    title: '确认清空全部操作日志？',
    content: '清空后将无法从系统页面恢复历史审计记录，请确认已完成必要备份。',
    hideCancel: false,
    onOk: async () => {
      await cleanOperationLogs();
      Message.success('操作日志已清空');
      loadData();
    },
  });
};

const businessTypeLabel = (value?: number) => businessTypeOptions.find(item => item.value === value)?.label || '其他';
const businessTypeColor = (value?: number) => businessTypeOptions.find(item => item.value === value)?.color || 'gray';
const requestMethodColor = (method?: string) => ({ GET: 'cyan', POST: 'green', PUT: 'arcoblue', DELETE: 'red', PATCH: 'orange' } as Record<string, string>)[method || ''] || 'gray';
const operatorTypeLabel = (value?: number) => operatorTypeMap[value ?? 0] || '其他';
const costTimeClass = (value?: number) => value && value >= 1000 ? 'cost-time is-slow' : value && value >= 300 ? 'cost-time is-warn' : 'cost-time';
const formatJsonText = (text?: string) => {
  if (!text) return '-';
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
};

onMounted(() => {
  void tableRef.value;
  loadData();
});
</script>

<style scoped>
.operlog-page {
  min-height: 100%;
}

.operlog-page :deep(.governance-compact-query-panel__description) {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.operlog-query-form :deep(.arco-form-item) {
  min-width: 220px;
}

.operlog-query-grid {
  grid-template-columns: repeat(4, minmax(180px, 1fr));
}

.operlog-time-range {
  grid-column: span 2;
}

.operlog-time-range :deep(.arco-picker) {
  width: 100%;
}

.cost-time {
  color: #2563eb;
  font-weight: 600;
}

.cost-time.is-warn {
  color: #d97706;
}

.cost-time.is-slow {
  color: #dc2626;
}

.operlog-detail-drawer :deep(.arco-drawer-body) {
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
}

.operlog-descriptions {
  margin-bottom: 16px;
}

.operlog-json-section {
  display: grid;
  gap: 10px;
}

.operlog-json-section h4 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 14px;
}

.operlog-json-section pre {
  max-height: 240px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid rgba(203, 213, 225, 0.9);
  border-radius: 12px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.operlog-json-section pre.is-error {
  background: #2b1111;
  color: #fecaca;
}

@media (max-width: 1360px) {
  .operlog-query-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 760px) {
  .operlog-query-grid {
    grid-template-columns: 1fr;
  }

  .operlog-time-range {
    grid-column: span 1;
  }
}
</style>
