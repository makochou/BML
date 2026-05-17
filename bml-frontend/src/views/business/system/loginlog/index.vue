<template>
  <div class="page-wrapper audit-log-page">
    <GovernanceCompactQueryPanel
      eyebrow="LOGIN AUDIT"
      title="系统登录日志"
      description="审计登录、登出、登录失败与密码修改等认证安全事件，快速定位账号风险。"
      density="ultra"
      theme="aurora"
      :meta-items="metaItems"
    >
      <template #actions>
        <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
      </template>
      <a-form :model="queryParams" layout="inline" class="biz-query-form audit-log-query">
        <div class="biz-query-form-primary biz-query-form-grid-4">
          <a-form-item field="username" label="账号"><a-input v-model="queryParams.username" placeholder="请输入账号" allow-clear @press-enter="handleSearch" /></a-form-item>
          <a-form-item field="ipaddr" label="IP地址"><a-input v-model="queryParams.ipaddr" placeholder="请输入 IP" allow-clear @press-enter="handleSearch" /></a-form-item>
          <a-form-item field="status" label="状态">
            <a-select v-model="queryParams.status" placeholder="全部状态" allow-clear @change="handleSearch">
              <a-option :value="1">成功</a-option>
              <a-option :value="0">失败</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="msg" label="事件"><a-input v-model="queryParams.msg" placeholder="登录/登出/密码" allow-clear @press-enter="handleSearch" /></a-form-item>
          <a-form-item field="timeRange" label="时间" class="biz-query-form-span-2">
            <a-range-picker v-model="timeRange" show-time value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" allow-clear @change="handleTimeRangeChange" />
          </a-form-item>
        </div>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill :meta-items="tableMetaItems">
      <template #actions>
        <a-button type="primary" status="success" v-if="hasPermission('system:loginlog:export')" @click="handleExport"><template #icon><icon-download /></template>导出 CSV</a-button>
        <a-button status="danger" v-if="hasPermission('system:loginlog:remove')" :disabled="!selectedRowKeys.length" @click="confirmBatchDelete"><template #icon><icon-delete /></template>批量删除</a-button>
        <a-button status="danger" v-if="hasPermission('system:loginlog:clean')" @click="confirmClean"><template #icon><icon-delete /></template>清空日志</a-button>
      </template>

      <a-table
        v-model:selected-keys="selectedRowKeys"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :bordered="false"
        class="audit-log-table"
        row-key="id"
        size="small"
        sticky-header
        :scroll="{ y: '100%' }"
        :scrollbar="false"
        :row-selection="{ type: 'checkbox', showCheckedAll: true, onlyCurrent: true }"
        @row-click="handleView"
      >
        <template #columns>
          <a-table-column v-if="hasPermission('system:loginlog:field:username')" title="账号" data-index="username" :width="112" ellipsis tooltip />
          <a-table-column v-if="hasPermission('system:loginlog:field:ipaddr')" title="IP地址" data-index="ipaddr" :width="148" ellipsis tooltip />
          <a-table-column v-if="hasPermission('system:loginlog:field:loginLocation')" title="地点" data-index="loginLocation" :width="132" ellipsis tooltip />
          <a-table-column v-if="hasPermission('system:loginlog:field:browser')" title="浏览器" data-index="browser" :width="104" ellipsis tooltip />
          <a-table-column v-if="hasPermission('system:loginlog:field:os')" title="操作系统" data-index="os" :width="112" ellipsis tooltip />
          <a-table-column v-if="hasPermission('system:loginlog:field:status')" title="状态" data-index="status" :width="76">
            <template #cell="{ record }"><a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '成功' : '失败' }}</a-tag></template>
          </a-table-column>
          <a-table-column v-if="hasPermission('system:loginlog:field:msg')" title="消息" data-index="msg" ellipsis tooltip />
          <a-table-column title="时间" data-index="loginTime" :width="144" />
        </template>
      </a-table>
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ pagination.total }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">成功 <b>{{ successCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">失败 <b>{{ failureCount }}</b></span>
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

    <a-drawer :visible="detailVisible" width="520px" title="登录日志详情" unmount-on-close @cancel="detailVisible = false">
      <a-descriptions v-if="currentRecord" :column="1" bordered size="large">
        <a-descriptions-item label="账号">{{ currentRecord.username }}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="currentRecord.status === 1 ? 'green' : 'red'">{{ currentRecord.status === 1 ? '成功' : '失败' }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="IP地址">{{ currentRecord.ipaddr }}</a-descriptions-item>
        <a-descriptions-item label="登录地点">{{ currentRecord.loginLocation }}</a-descriptions-item>
        <a-descriptions-item label="浏览器">{{ currentRecord.browser }}</a-descriptions-item>
        <a-descriptions-item label="操作系统">{{ currentRecord.os }}</a-descriptions-item>
        <a-descriptions-item label="事件消息">{{ currentRecord.msg }}</a-descriptions-item>
        <a-descriptions-item label="时间">{{ currentRecord.loginTime }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconDelete, IconDownload, IconRefresh, IconSearch } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import { cleanLoginLogs, deleteLoginLogs, exportLoginLogs, fetchLoginLogPage, type LoginLogQuery, type LoginLogVO } from '../../../../api/system';
import { normalizePageResult } from '../../../../utils/pageResult';
import { useButtonPermission } from '../../../../composables/useButtonPermission';

const { hasPermission } = useButtonPermission();

const loading = ref(false);
const tableData = ref<LoginLogVO[]>([]);
const selectedRowKeys = ref<number[]>([]);
const timeRange = ref<string[]>([]);
const detailVisible = ref(false);
const currentRecord = ref<LoginLogVO | null>(null);

const pagination = reactive({ current: 1, pageSize: 20, total: 0 });
const queryParams = reactive<LoginLogQuery>({ pageNum: 1, pageSize: 20 });
const successCount = computed(() => tableData.value.filter(item => item.status === 1).length);
const failureCount = computed(() => tableData.value.filter(item => item.status === 0).length);

const metaItems = computed(() => [
  { label: '当前页', value: tableData.value.length, tone: 'blue' as const },
  { label: '失败事件', value: failureCount.value, tone: 'gold' as const },
  { label: '总记录数', value: pagination.total, tone: 'teal' as const },
]);

const tableMetaItems = computed(() => [
  { label: '总记录', value: String(pagination.total), tone: 'blue' as const },
  { label: '当前页', value: `${pagination.current}/${Math.max(1, Math.ceil(pagination.total / pagination.pageSize))}`, tone: 'teal' as const },
]);

const loadData = async () => {
  loading.value = true;
  try {
    const params: LoginLogQuery = {
      ...queryParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      username: queryParams.username || undefined,
      ipaddr: queryParams.ipaddr || undefined,
      msg: queryParams.msg || undefined,
    };
    const res = await fetchLoginLogPage(params) as any;
    const page = normalizePageResult<LoginLogVO>(res.data, pagination.current, pagination.pageSize);
    tableData.value = page.records;
    pagination.total = page.total;
    pagination.current = page.pageNum;
    pagination.pageSize = page.pageSize;
    queryParams.pageNum = page.pageNum;
    queryParams.pageSize = page.pageSize;
    selectedRowKeys.value = [];
  } catch {
    tableData.value = [];
    pagination.total = 0;
    selectedRowKeys.value = [];
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => { pagination.current = 1; queryParams.pageNum = 1; loadData(); };
const handleReset = () => {
  Object.assign(queryParams, { pageNum: 1, pageSize: pagination.pageSize, username: undefined, ipaddr: undefined, status: undefined, msg: undefined, beginTime: undefined, endTime: undefined });
  timeRange.value = [];
  selectedRowKeys.value = [];
  pagination.current = 1;
  loadData();
};
const handleTimeRangeChange = (value?: string[]) => { queryParams.beginTime = value?.[0]; queryParams.endTime = value?.[1]; handleSearch(); };
const handlePageChange = (page: number) => { pagination.current = page; queryParams.pageNum = page; loadData(); };
const handlePageSizeChange = (pageSize: number) => { pagination.pageSize = pageSize; pagination.current = 1; queryParams.pageSize = pageSize; queryParams.pageNum = 1; loadData(); };
const handleView = (record: LoginLogVO) => { currentRecord.value = record; detailVisible.value = true; };
const handleExport = async () => { await exportLoginLogs(queryParams); Message.success('登录日志导出已开始'); };
const confirmBatchDelete = () => Modal.warning({ title: '确认删除', content: `确定删除选中的 ${selectedRowKeys.value.length} 条登录日志吗？`, hideCancel: false, onOk: async () => { await deleteLoginLogs(selectedRowKeys.value); selectedRowKeys.value = []; Message.success('删除成功'); loadData(); } });
const confirmClean = () => Modal.warning({ title: '确认清空', content: '清空后无法恢复，确定清空全部登录日志吗？', hideCancel: false, onOk: async () => { await cleanLoginLogs(); selectedRowKeys.value = []; Message.success('清空成功'); loadData(); } });

onMounted(loadData);
</script>

<style scoped>
.audit-log-page { min-height: 100%; }
.audit-log-table :deep(.arco-table-th .arco-table-cell),
.audit-log-table :deep(.arco-table-td .arco-table-cell) { padding-left: 8px; padding-right: 8px; }
.audit-log-table :deep(.arco-table-th:last-child .arco-table-cell),
.audit-log-table :deep(.arco-table-td:last-child .arco-table-cell) { padding-left: 6px; padding-right: 6px; white-space: nowrap; font-variant-numeric: tabular-nums; }
</style>
