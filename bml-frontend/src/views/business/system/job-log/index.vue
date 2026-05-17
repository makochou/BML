<template>
  <div class="ops-page">
    <a-card class="query-card" :bordered="false">
      <a-form :model="queryParams" layout="inline">
        <a-form-item label="任务名称"><a-input v-model="queryParams.jobName" allow-clear placeholder="请输入任务名称" /></a-form-item>
        <a-form-item label="分组"><a-input v-model="queryParams.jobGroup" allow-clear placeholder="SYSTEM" /></a-form-item>
        <a-form-item label="状态"><a-select v-model="queryParams.status" allow-clear placeholder="全部" style="width: 120px"><a-option :value="0">成功</a-option><a-option :value="1">失败</a-option></a-select></a-form-item>
        <a-form-item><a-space><a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button><a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>
    <a-card class="table-card" :bordered="false">
      <template #title>任务日志</template>
      <template #extra><a-button status="danger" v-if="hasPermission('system:joblog:clean')" @click="cleanRows">清空日志</a-button></template>
      <a-table row-key="id" :loading="loading" :data="tableData" :pagination="pagination" @page-change="onPageChange" @page-size-change="onPageSizeChange">
        <template #columns>
          <a-table-column title="任务名称" data-index="jobName" :width="180" />
          <a-table-column title="分组" data-index="jobGroup" :width="120" />
          <a-table-column title="调用目标" data-index="invokeTarget" :width="260" ellipsis tooltip />
          <a-table-column title="状态" :width="100" align="center"><template #cell="{ record }"><a-tag :color="record.status === 0 ? 'green' : 'red'">{{ record.status === 0 ? '成功' : '失败' }}</a-tag></template></a-table-column>
          <a-table-column title="开始时间" data-index="startTime" :width="180" />
          <a-table-column title="耗时" :width="100"><template #cell="{ record }">{{ record.costTime || 0 }} ms</template></a-table-column>
          <a-table-column title="异常信息" data-index="exceptionInfo" ellipsis tooltip />
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { normalizePageResult } from '../../../../utils/pageResult';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { cleanJobLogs, fetchJobLogPage, type JobLogQuery, type JobLogVO } from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<JobLogVO[]>([]);
const queryParams = reactive<JobLogQuery>({ pageNum: 1, pageSize: 10, jobName: '', jobGroup: '', status: undefined });
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });
async function loadData() { loading.value = true; try { const res = await fetchJobLogPage({ ...queryParams, jobName: queryParams.jobName || undefined, jobGroup: queryParams.jobGroup || undefined }) as any; const page = normalizePageResult<JobLogVO>(res.data, pagination.current, pagination.pageSize); tableData.value = page.records; pagination.total = page.total; pagination.current = page.pageNum; pagination.pageSize = page.pageSize; } finally { loading.value = false; } }
function handleSearch() { pagination.current = 1; queryParams.pageNum = 1; loadData(); }
function handleReset() { queryParams.jobName = ''; queryParams.jobGroup = ''; queryParams.status = undefined; handleSearch(); }
function onPageChange(page: number) { pagination.current = page; queryParams.pageNum = page; loadData(); }
function onPageSizeChange(pageSize: number) { pagination.pageSize = pageSize; queryParams.pageSize = pageSize; queryParams.pageNum = 1; loadData(); }
function cleanRows() { Modal.warning({ title: '确认清空日志', content: '确定清空全部任务日志吗？', hideCancel: false, async onOk() { await cleanJobLogs(); Message.success('已清空'); await loadData(); } }); }
onMounted(loadData);
</script>

<style scoped>
.ops-page { padding: 18px; min-height: 100%; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.query-card { margin-bottom: 16px; border-radius: 16px; }
.table-card { border-radius: 16px; }
:deep(.arco-table-th) { background: #f5f8ff; font-weight: 700; }
</style>
