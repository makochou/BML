<template>
  <div class="page-wrapper exception-log-page">
    <GovernanceCompactQueryPanel
      eyebrow="EXCEPTION AUDIT"
      title="系统异常日志"
      description="聚合系统运行异常、接口错误和业务执行失败记录，帮助研发与运维快速定位故障。"
      density="ultra"
      theme="aurora"
      :meta-items="metaItems"
    >
      <template #actions>
        <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <a-form :model="queryParams" layout="inline" class="biz-query-form exception-log-query">
        <div class="biz-query-form-primary biz-query-form-grid-4">
          <a-form-item field="title" label="模块"><a-input v-model="queryParams.title" placeholder="请输入模块" allow-clear @press-enter="handleSearch" /></a-form-item>
          <a-form-item field="operName" label="操作人"><a-input v-model="queryParams.operName" placeholder="请输入账号" allow-clear @press-enter="handleSearch" /></a-form-item>
          <a-form-item field="operIp" label="IP地址"><a-input v-model="queryParams.operIp" placeholder="请输入 IP" allow-clear @press-enter="handleSearch" /></a-form-item>
          <a-form-item field="requestMethod" label="请求方式">
            <a-select v-model="queryParams.requestMethod" placeholder="全部方式" allow-clear @change="handleSearch">
              <a-option value="GET">GET</a-option>
              <a-option value="POST">POST</a-option>
              <a-option value="PUT">PUT</a-option>
              <a-option value="DELETE">DELETE</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="timeRange" label="时间" class="biz-query-form-span-2">
            <a-range-picker v-model="timeRange" show-time value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" allow-clear @change="handleTimeRangeChange" />
          </a-form-item>
        </div>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill :meta-items="tableMetaItems">
      <template #actions>
        <a-button type="primary" status="success" @click="handleExport"><template #icon><icon-download /></template>导出 CSV</a-button>
      </template>
      <a-table
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        class="exception-log-table"
        row-key="id"
        size="small"
        sticky-header
        :scroll="{ x: 1280, y: '100%' }"
        :scrollbar="false"
        @row-click="handleView"
      >
        <template #columns>
          <a-table-column title="模块" data-index="title" :width="150" />
          <a-table-column title="请求方式" data-index="requestMethod" :width="110" />
          <a-table-column title="操作人" data-index="operName" :width="130" />
          <a-table-column title="IP地址" data-index="operIp" :width="150" />
          <a-table-column title="请求地址" data-index="operUrl" :width="260" ellipsis tooltip />
          <a-table-column title="异常信息" data-index="errorMsg" :width="360" ellipsis tooltip />
          <a-table-column title="耗时" data-index="costTime" :width="100"><template #cell="{ record }">{{ record.costTime }} ms</template></a-table-column>
          <a-table-column title="时间" data-index="operTime" :width="180" />
        </template>
      </a-table>
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ pagination.total }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">异常 <b>{{ exceptionCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-locked">高耗时 <b>{{ slowCount }}</b></span>
          <a-divider direction="vertical" />
          <span>平均耗时 <b>{{ averageCost }}</b></span>
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

    <a-drawer :visible="detailVisible" width="680px" title="异常日志详情" unmount-on-close @cancel="detailVisible = false">
      <a-descriptions v-if="currentRecord" :column="1" bordered>
        <a-descriptions-item label="模块">{{ currentRecord.title }}</a-descriptions-item>
        <a-descriptions-item label="方法">{{ currentRecord.method }}</a-descriptions-item>
        <a-descriptions-item label="请求地址">{{ currentRecord.operUrl }}</a-descriptions-item>
        <a-descriptions-item label="请求参数"><a-typography-paragraph copyable class="json-block">{{ currentRecord.operParam || '-' }}</a-typography-paragraph></a-descriptions-item>
        <a-descriptions-item label="错误信息"><a-typography-paragraph copyable class="json-block error-text">{{ currentRecord.errorMsg || '-' }}</a-typography-paragraph></a-descriptions-item>
        <a-descriptions-item label="发生时间">{{ currentRecord.operTime }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconDownload, IconRefresh, IconSearch } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import { exportExceptionLogs, fetchExceptionLogPage, type OperationLogQuery, type OperationLogVO } from '../../../../api/system';
import { normalizePageResult } from '../../../../utils/pageResult';

const loading = ref(false);
const tableData = ref<OperationLogVO[]>([]);
const timeRange = ref<string[]>([]);
const detailVisible = ref(false);
const currentRecord = ref<OperationLogVO | null>(null);
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });
const queryParams = reactive<OperationLogQuery>({ pageNum: 1, pageSize: 20, status: 1 });
const exceptionCount = computed(() => tableData.value.length);
const slowCount = computed(() => tableData.value.filter(item => (item.costTime || 0) > 1000).length);

const metaItems = computed(() => [
  { label: '异常总数', value: pagination.total, tone: 'gold' as const },
  { label: '当前页', value: tableData.value.length, tone: 'blue' as const },
  { label: '高耗时', value: slowCount.value, tone: 'violet' as const },
]);

const tableMetaItems = computed(() => [
  { label: '总记录', value: String(pagination.total), tone: 'blue' as const },
  { label: '当前页', value: `${pagination.current}/${Math.max(1, Math.ceil(pagination.total / pagination.pageSize))}`, tone: 'teal' as const },
]);

const averageCost = computed(() => {
  if (!tableData.value.length) return '0ms';
  const totalCost = tableData.value.reduce((sum, item) => sum + (item.costTime || 0), 0);
  return `${Math.round(totalCost / tableData.value.length)}ms`;
});

const loadData = async () => {
  loading.value = true;
  try {
    const params: OperationLogQuery = {
      ...queryParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: 1,
      title: queryParams.title || undefined,
      operName: queryParams.operName || undefined,
      operIp: queryParams.operIp || undefined,
      requestMethod: queryParams.requestMethod || undefined,
    };
    const res = await fetchExceptionLogPage(params) as any;
    const page = normalizePageResult<OperationLogVO>(res.data, pagination.current, pagination.pageSize);
    tableData.value = page.records;
    pagination.total = page.total;
    pagination.current = page.pageNum;
    pagination.pageSize = page.pageSize;
    queryParams.pageNum = page.pageNum;
    queryParams.pageSize = page.pageSize;
  } catch {
    tableData.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pagination.current = 1; queryParams.pageNum = 1; queryParams.status = 1; loadData(); };
const handleReset = () => {
  Object.assign(queryParams, { pageNum: 1, pageSize: pagination.pageSize, title: undefined, operName: undefined, operIp: undefined, requestMethod: undefined, status: 1, beginTime: undefined, endTime: undefined });
  timeRange.value = [];
  pagination.current = 1;
  loadData();
};
const handleTimeRangeChange = (value?: string[]) => { queryParams.beginTime = value?.[0]; queryParams.endTime = value?.[1]; handleSearch(); };
const handlePageChange = (page: number) => { pagination.current = page; queryParams.pageNum = page; loadData(); };
const handlePageSizeChange = (pageSize: number) => { pagination.pageSize = pageSize; pagination.current = 1; queryParams.pageSize = pageSize; queryParams.pageNum = 1; loadData(); };
const handleView = (record: OperationLogVO) => { currentRecord.value = record; detailVisible.value = true; };
const handleExport = async () => { await exportExceptionLogs(queryParams); Message.success('异常日志导出已开始'); };

onMounted(loadData);
</script>

<style scoped>
.exception-log-page { min-height: 100%; }
.json-block { max-height: 220px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
.error-text { color: rgb(var(--red-6)); }
</style>
