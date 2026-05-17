<template>
  <div class="system-config-page">
    <GovernanceCompactQueryPanel
      eyebrow="RUNTIME PARAMETERS"
      title="参数配置"
      description="集中维护运行时参数、登录策略与业务开关，支持系统内置参数保护。"
      :meta-items="queryMetaItems"
      density="compact"
      theme="aurora"
    >
      <a-form :model="queryParams" layout="inline" class="config-query-form">
        <a-form-item label="参数名称">
          <a-input v-model="queryParams.configName" allow-clear placeholder="请输入参数名称" />
        </a-form-item>
        <a-form-item label="参数键名">
          <a-input v-model="queryParams.configKey" allow-clear placeholder="如 sys.login.captchaEnabled" />
        </a-form-item>
        <a-form-item label="参数类型">
          <a-select v-model="queryParams.configType" allow-clear placeholder="全部类型" style="width: 150px">
            <a-option :value="1">系统内置</a-option>
            <a-option :value="0">业务配置</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button>
            <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage
      eyebrow="CONFIG REGISTRY"
      title="参数列表"
      description="configKey 是程序读取配置的稳定键名，修改前请确认影响范围。"
      :meta-items="tableMetaItems"
      density="compact"
      body-fill
    >
      <template #actions>
        <a-space>
          <a-button type="primary" v-if="hasPermission('system:config:add')" @click="openDialog('add')">
            <template #icon><icon-plus /></template>
            新增参数
          </a-button>
          <a-button @click="loadData"><template #icon><icon-refresh /></template>刷新</a-button>
        </a-space>
      </template>

      <a-table
        row-key="id"
        :loading="loading"
        :data="tableData"
        :pagination="pagination"
        :bordered="false"
        class="config-table"
        @page-change="onPageChange"
        @page-size-change="onPageSizeChange"
      >
        <template #columns>
          <a-table-column title="参数名称" data-index="configName" :width="180">
            <template #cell="{ record }"><span class="config-name">{{ record.configName }}</span></template>
          </a-table-column>
          <a-table-column title="参数键名" data-index="configKey" :width="260">
            <template #cell="{ record }"><a-typography-text code>{{ record.configKey }}</a-typography-text></template>
          </a-table-column>
          <a-table-column title="参数键值" data-index="configValue" :width="260" ellipsis tooltip>
            <template #cell="{ record }"><span>{{ record.configValue }}</span></template>
          </a-table-column>
          <a-table-column title="类型" data-index="configType" :width="110" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.configType === 1 ? 'gold' : 'arcoblue'">{{ record.configType === 1 ? '系统内置' : '业务配置' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="更新时间" data-index="updateTime" :width="170" />
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="150" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space size="mini">
                <a-button size="mini" type="text" v-if="hasPermission('system:config:edit')" @click="openDialog('edit', record)">编辑</a-button>
                <a-button size="mini" type="text" status="danger" :disabled="record.configType === 1 || !hasPermission('system:config:remove')" @click="removeRow(record)">删除</a-button>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </GovernanceListStage>

    <a-modal
      v-model:visible="dialog.visible"
      :title="dialog.mode === 'add' ? '新增参数配置' : '编辑参数配置'"
      width="680px"
      :mask-closable="false"
      @ok="submitForm"
      @cancel="closeDialog"
    >
      <AuditInfoFooter :data="form" style="margin-bottom: 12px;" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item field="configName" label="参数名称"><a-input v-model="form.configName" allow-clear placeholder="请输入参数名称" /></a-form-item>
        <a-form-item field="configKey" label="参数键名"><a-input v-model="form.configKey" allow-clear placeholder="如 sys.login.captchaEnabled" /></a-form-item>
        <a-form-item field="configValue" label="参数键值"><a-textarea v-model="form.configValue" allow-clear :auto-size="{ minRows: 3, maxRows: 6 }" placeholder="请输入参数键值" /></a-form-item>
        <a-form-item field="configType" label="参数类型">
          <a-radio-group v-model="form.configType" type="button">
            <a-radio :value="0">业务配置</a-radio>
            <a-radio :value="1">系统内置</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item field="remark" label="备注"><a-textarea v-model="form.remark" allow-clear :max-length="500" show-word-limit /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import { normalizePageResult } from '../../../../utils/pageResult';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import AuditInfoFooter from '../../../../components/common/AuditInfoFooter.vue';
import {
  createConfig,
  deleteConfig,
  fetchConfigDetail,
  fetchConfigPage,
  updateConfig,
  type ConfigForm,
  type ConfigQuery,
  type ConfigVO,
} from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<ConfigVO[]>([]);
const formRef = ref();
const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit' });
const queryParams = reactive<ConfigQuery>({ pageNum: 1, pageSize: 10, configName: '', configKey: '', configType: undefined });
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });
const defaultForm = (): ConfigForm => ({ configName: '', configKey: '', configValue: '', configType: 0, remark: '' });
const form = reactive<ConfigForm>(defaultForm());

const rules = {
  configName: [{ required: true, message: '请输入参数名称' }],
  configKey: [{ required: true, message: '请输入参数键名' }],
  configValue: [{ required: true, message: '请输入参数键值' }],
};

const queryMetaItems = computed(() => [
  { label: '参数总数', value: pagination.total, tone: 'blue' as const },
  { label: '系统内置', value: tableData.value.filter(item => item.configType === 1).length, tone: 'gold' as const },
  { label: '业务配置', value: tableData.value.filter(item => item.configType === 0).length, tone: 'teal' as const },
]);
const tableMetaItems = computed(() => [
  { label: '当前页', value: tableData.value.length, tone: 'blue' as const },
  { label: '可删除', value: tableData.value.filter(item => item.configType !== 1).length, tone: 'green' as const },
]);

async function loadData() {
  loading.value = true;
  try {
    const res = await fetchConfigPage({
      ...queryParams,
      configName: queryParams.configName || undefined,
      configKey: queryParams.configKey || undefined,
    }) as any;
    const page = normalizePageResult<ConfigVO>(res.data, pagination.current, pagination.pageSize);
    tableData.value = page.records;
    pagination.total = page.total;
    pagination.current = page.pageNum;
    pagination.pageSize = page.pageSize;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  queryParams.pageNum = 1;
  loadData();
}

function handleReset() {
  queryParams.configName = '';
  queryParams.configKey = '';
  queryParams.configType = undefined;
  handleSearch();
}

function onPageChange(page: number) {
  pagination.current = page;
  queryParams.pageNum = page;
  loadData();
}

function onPageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize;
  queryParams.pageSize = pageSize;
  queryParams.pageNum = 1;
  loadData();
}

function assignForm(data: ConfigForm) {
  Object.assign(form, defaultForm(), data);
}

async function openDialog(mode: 'add' | 'edit', record?: ConfigVO) {
  dialog.mode = mode;
  if (mode === 'edit' && record) {
    const res = await fetchConfigDetail(record.id) as any;
    assignForm(res.data || record);
  } else {
    assignForm(defaultForm());
  }
  dialog.visible = true;
}

function closeDialog() {
  dialog.visible = false;
  formRef.value?.resetFields?.();
}

async function submitForm() {
  const err = await formRef.value?.validate?.();
  if (err) return false;
  if (dialog.mode === 'add') {
    await createConfig(form);
    Message.success('新增参数成功');
  } else {
    await updateConfig(form);
    Message.success('修改参数成功');
  }
  closeDialog();
  await loadData();
  return true;
}

function removeRow(record: ConfigVO) {
  Modal.warning({
    title: '确认删除参数',
    content: `确定删除“${record.configName}”吗？系统内置参数不允许删除。`,
    hideCancel: false,
    okText: '确认删除',
    async onOk() {
      await deleteConfig(record.id);
      Message.success('删除成功');
      await loadData();
    },
  });
}

onMounted(loadData);
</script>

<style scoped>
.system-config-page {
  min-height: 100%;
  padding: 18px;
  background:
    radial-gradient(circle at 12% 8%, rgba(20, 184, 166, 0.08), transparent 30%),
    radial-gradient(circle at 88% 18%, rgba(37, 99, 235, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%);
}

.config-query-form {
  width: 100%;
}

.config-table :deep(.arco-table-th) {
  background: linear-gradient(180deg, #f8fbff, #eef6ff);
  color: #26364d;
  font-weight: 700;
}

.config-table :deep(.arco-table-tr:hover .arco-table-td) {
  background: rgba(37, 99, 235, 0.045);
}

.config-name {
  font-weight: 650;
  color: #1f2d3d;
}
</style>
