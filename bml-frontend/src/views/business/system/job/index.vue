<template>
  <div class="ops-page">
    <a-card class="query-card" :bordered="false">
      <a-form :model="queryParams" layout="inline">
        <a-form-item label="任务名称"><a-input v-model="queryParams.jobName" allow-clear placeholder="请输入任务名称" /></a-form-item>
        <a-form-item label="任务分组"><a-input v-model="queryParams.jobGroup" allow-clear placeholder="SYSTEM" /></a-form-item>
        <a-form-item label="状态"><a-select v-model="queryParams.status" allow-clear placeholder="全部" style="width: 120px"><a-option :value="1">启用</a-option><a-option :value="0">暂停</a-option></a-select></a-form-item>
        <a-form-item><a-space><a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button><a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>定时任务</template>
      <template #extra><a-button type="primary" :disabled="!hasPermission('system:job:add')" @click="openDialog('add')"><template #icon><icon-plus /></template>新增任务</a-button></template>
      <a-table row-key="id" :loading="loading" :data="tableData" :pagination="pagination" @page-change="onPageChange" @page-size-change="onPageSizeChange">
        <template #columns>
          <a-table-column title="任务名称" data-index="jobName" :width="180" />
          <a-table-column title="分组" data-index="jobGroup" :width="120" />
          <a-table-column title="调用目标" data-index="invokeTarget" :width="260" ellipsis tooltip />
          <a-table-column title="Cron" data-index="cronExpression" :width="150" />
          <a-table-column title="状态" :width="100" align="center"><template #cell="{ record }"><a-switch :model-value="record.status === 1" size="small" :disabled="!hasPermission('system:job:changeStatus')" @change="changeStatusRow(record, $event)" /></template></a-table-column>
          <a-table-column title="操作" :width="220" align="center" fixed="right"><template #cell="{ record }"><a-space size="mini"><a-button size="mini" type="text" :disabled="!hasPermission('system:job:run')" @click="runRow(record)">运行</a-button><a-button size="mini" type="text" :disabled="!hasPermission('system:job:edit')" @click="openDialog('edit', record)">编辑</a-button><a-button size="mini" type="text" status="danger" :disabled="!hasPermission('system:job:remove')" @click="removeRow(record)">删除</a-button></a-space></template></a-table-column>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="dialog.visible" :title="dialog.mode === 'add' ? '新增任务' : '编辑任务'" width="720px" :mask-closable="false" @ok="submitForm" @cancel="closeDialog">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item field="jobName" label="任务名称"><a-input v-model="form.jobName" allow-clear /></a-form-item>
        <a-form-item field="jobGroup" label="任务分组"><a-input v-model="form.jobGroup" allow-clear placeholder="DEFAULT" /></a-form-item>
        <a-form-item field="invokeTarget" label="调用目标"><a-select v-model="form.invokeTarget" allow-clear placeholder="请选择调用目标"><a-option v-for="target in targets" :key="target" :value="target">{{ target }}</a-option></a-select></a-form-item>
        <a-form-item field="cronExpression" label="Cron 表达式"><a-input v-model="form.cronExpression" allow-clear placeholder="0 * * * * ?" /></a-form-item>
        <a-form-item field="concurrent" label="是否并发"><a-radio-group v-model="form.concurrent" type="button"><a-radio :value="0">禁止</a-radio><a-radio :value="1">允许</a-radio></a-radio-group></a-form-item>
        <a-form-item field="status" label="状态"><a-radio-group v-model="form.status" type="button"><a-radio :value="0">暂停</a-radio><a-radio :value="1">启用</a-radio></a-radio-group></a-form-item>
        <a-form-item field="remark" label="备注"><a-textarea v-model="form.remark" allow-clear /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { normalizePageResult } from '../../../../utils/pageResult';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { changeJobStatus, createJob, deleteJob, fetchJobDetail, fetchJobPage, fetchJobTargets, runJob, updateJob, type JobForm, type JobQuery, type JobVO } from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<JobVO[]>([]);
const targets = ref<string[]>([]);
const formRef = ref();
const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit' });
const queryParams = reactive<JobQuery>({ pageNum: 1, pageSize: 10, jobName: '', jobGroup: '', status: undefined });
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });
const defaultForm = (): JobForm => ({ jobName: '', jobGroup: 'DEFAULT', invokeTarget: '', cronExpression: '0 * * * * ?', misfirePolicy: 1, concurrent: 0, status: 0, remark: '' });
const form = reactive<JobForm>(defaultForm());
const rules = { jobName: [{ required: true, message: '请输入任务名称' }], invokeTarget: [{ required: true, message: '请选择调用目标' }], cronExpression: [{ required: true, message: '请输入 Cron 表达式' }] };

async function loadData() { loading.value = true; try { const res = await fetchJobPage({ ...queryParams, jobName: queryParams.jobName || undefined, jobGroup: queryParams.jobGroup || undefined }) as any; const page = normalizePageResult<JobVO>(res.data, pagination.current, pagination.pageSize); tableData.value = page.records; pagination.total = page.total; pagination.current = page.pageNum; pagination.pageSize = page.pageSize; } finally { loading.value = false; } }
async function loadTargets() { const res = await fetchJobTargets() as any; targets.value = res.data || []; }
function handleSearch() { pagination.current = 1; queryParams.pageNum = 1; loadData(); }
function handleReset() { queryParams.jobName = ''; queryParams.jobGroup = ''; queryParams.status = undefined; handleSearch(); }
function onPageChange(page: number) { pagination.current = page; queryParams.pageNum = page; loadData(); }
function onPageSizeChange(pageSize: number) { pagination.pageSize = pageSize; queryParams.pageSize = pageSize; queryParams.pageNum = 1; loadData(); }
function assignForm(data: JobForm) { Object.assign(form, defaultForm(), data); }
async function openDialog(mode: 'add' | 'edit', record?: JobVO) { dialog.mode = mode; if (mode === 'edit' && record) { const res = await fetchJobDetail(record.id) as any; assignForm(res.data || record); } else assignForm(defaultForm()); dialog.visible = true; }
function closeDialog() { dialog.visible = false; formRef.value?.resetFields?.(); }
async function submitForm() { const err = await formRef.value?.validate?.(); if (err) return false; if (dialog.mode === 'add') { await createJob(form); Message.success('新增成功'); } else { await updateJob(form); Message.success('修改成功'); } closeDialog(); await loadData(); return true; }
async function changeStatusRow(record: JobVO, checked: boolean | string | number) { await changeJobStatus(record.id, checked ? 1 : 0); Message.success(checked ? '任务已启用' : '任务已暂停'); await loadData(); }
async function runRow(record: JobVO) { await runJob(record.id); Message.success('任务已触发'); }
function removeRow(record: JobVO) { Modal.warning({ title: '确认删除任务', content: `确定删除“${record.jobName}”吗？`, hideCancel: false, async onOk() { await deleteJob(record.id); Message.success('删除成功'); await loadData(); } }); }
onMounted(() => { loadTargets(); loadData(); });
</script>

<style scoped>
.ops-page { padding: 18px; min-height: 100%; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.query-card { margin-bottom: 16px; border-radius: 16px; }
.table-card { border-radius: 16px; }
:deep(.arco-table-th) { background: #f5f8ff; font-weight: 700; }
</style>
