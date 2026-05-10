<template>
  <div class="ops-page">
    <a-card class="query-card" :bordered="false">
      <a-form :model="queryParams" layout="inline">
        <a-form-item label="标题"><a-input v-model="queryParams.noticeTitle" allow-clear placeholder="请输入公告标题" /></a-form-item>
        <a-form-item label="类型">
          <a-select v-model="queryParams.noticeType" allow-clear placeholder="全部" style="width: 120px"><a-option :value="1">通知</a-option><a-option :value="2">公告</a-option><a-option :value="3">维护</a-option></a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model="queryParams.status" allow-clear placeholder="全部" style="width: 120px"><a-option :value="1">已发布</a-option><a-option :value="0">草稿</a-option></a-select>
        </a-form-item>
        <a-form-item><a-space><a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button><a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button></a-space></a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>通知公告</template>
      <template #extra><a-button type="primary" :disabled="!hasPermission('system:notice:add')" @click="openDialog('add')"><template #icon><icon-plus /></template>新增公告</a-button></template>
      <a-table row-key="id" :loading="loading" :data="tableData" :pagination="pagination" @page-change="onPageChange" @page-size-change="onPageSizeChange">
        <template #columns>
          <a-table-column title="标题" data-index="noticeTitle" :width="260" ellipsis tooltip />
          <a-table-column title="类型" :width="110" align="center"><template #cell="{ record }"><a-tag :color="noticeTypeColor(record.noticeType)">{{ noticeTypeText(record.noticeType) }}</a-tag></template></a-table-column>
          <a-table-column title="状态" :width="110" align="center"><template #cell="{ record }"><a-tag :color="record.status === 1 ? 'green' : 'gray'">{{ record.status === 1 ? '已发布' : '草稿' }}</a-tag></template></a-table-column>
          <a-table-column title="发布时间" data-index="publishTime" :width="180" />
          <a-table-column title="操作" :width="230" align="center" fixed="right">
            <template #cell="{ record }"><a-space size="mini"><a-button size="mini" type="text" :disabled="!hasPermission('system:notice:edit')" @click="openDialog('edit', record)">编辑</a-button><a-button size="mini" type="text" :disabled="!hasPermission('system:notice:publish')" @click="togglePublish(record)">{{ record.status === 1 ? '撤回' : '发布' }}</a-button><a-button size="mini" type="text" status="danger" :disabled="!hasPermission('system:notice:remove')" @click="removeRow(record)">删除</a-button></a-space></template>
          </a-table-column>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:visible="dialog.visible" :title="dialog.mode === 'add' ? '新增公告' : '编辑公告'" width="760px" :mask-closable="false" @ok="submitForm" @cancel="closeDialog">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item field="noticeTitle" label="公告标题"><a-input v-model="form.noticeTitle" allow-clear /></a-form-item>
        <a-form-item field="noticeType" label="公告类型"><a-radio-group v-model="form.noticeType" type="button"><a-radio :value="1">通知</a-radio><a-radio :value="2">公告</a-radio><a-radio :value="3">维护</a-radio></a-radio-group></a-form-item>
        <a-form-item field="noticeContent" label="公告内容"><a-textarea v-model="form.noticeContent" :auto-size="{ minRows: 8, maxRows: 12 }" allow-clear /></a-form-item>
        <a-form-item field="status" label="状态"><a-radio-group v-model="form.status" type="button"><a-radio :value="0">草稿</a-radio><a-radio :value="1">发布</a-radio></a-radio-group></a-form-item>
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
import { createNotice, deleteNotice, fetchNoticeDetail, fetchNoticePage, publishNotice, revokeNotice, updateNotice, type NoticeForm, type NoticeQuery, type NoticeVO } from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<NoticeVO[]>([]);
const formRef = ref();
const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit' });
const queryParams = reactive<NoticeQuery>({ pageNum: 1, pageSize: 10, noticeTitle: '', noticeType: undefined, status: undefined });
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });
const defaultForm = (): NoticeForm => ({ noticeTitle: '', noticeType: 1, noticeContent: '', status: 0, remark: '' });
const form = reactive<NoticeForm>(defaultForm());
const rules = { noticeTitle: [{ required: true, message: '请输入公告标题' }], noticeType: [{ required: true, message: '请选择公告类型' }], noticeContent: [{ required: true, message: '请输入公告内容' }] };

async function loadData() { loading.value = true; try { const res = await fetchNoticePage({ ...queryParams, noticeTitle: queryParams.noticeTitle || undefined }) as any; const page = normalizePageResult<NoticeVO>(res.data, pagination.current, pagination.pageSize); tableData.value = page.records; pagination.total = page.total; pagination.current = page.pageNum; pagination.pageSize = page.pageSize; } finally { loading.value = false; } }
function handleSearch() { pagination.current = 1; queryParams.pageNum = 1; loadData(); }
function handleReset() { queryParams.noticeTitle = ''; queryParams.noticeType = undefined; queryParams.status = undefined; handleSearch(); }
function onPageChange(page: number) { pagination.current = page; queryParams.pageNum = page; loadData(); }
function onPageSizeChange(pageSize: number) { pagination.pageSize = pageSize; queryParams.pageSize = pageSize; queryParams.pageNum = 1; loadData(); }
function assignForm(data: NoticeForm) { Object.assign(form, defaultForm(), data); }
async function openDialog(mode: 'add' | 'edit', record?: NoticeVO) { dialog.mode = mode; if (mode === 'edit' && record) { const res = await fetchNoticeDetail(record.id) as any; assignForm(res.data || record); } else assignForm(defaultForm()); dialog.visible = true; }
function closeDialog() { dialog.visible = false; formRef.value?.resetFields?.(); }
async function submitForm() { const err = await formRef.value?.validate?.(); if (err) return false; if (dialog.mode === 'add') { await createNotice(form); Message.success('新增成功'); } else { await updateNotice(form); Message.success('修改成功'); } closeDialog(); await loadData(); return true; }
async function togglePublish(record: NoticeVO) { if (record.status === 1) { await revokeNotice(record.id); Message.success('已撤回'); } else { await publishNotice(record.id); Message.success('已发布'); } await loadData(); }
function removeRow(record: NoticeVO) { Modal.warning({ title: '确认删除公告', content: `确定删除“${record.noticeTitle}”吗？`, hideCancel: false, async onOk() { await deleteNotice(record.id); Message.success('删除成功'); await loadData(); } }); }
function noticeTypeText(type: number) { return type === 2 ? '公告' : type === 3 ? '维护' : '通知'; }
function noticeTypeColor(type: number) { return type === 2 ? 'arcoblue' : type === 3 ? 'orange' : 'green'; }
onMounted(loadData);
</script>

<style scoped>
.ops-page { padding: 18px; min-height: 100%; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.query-card { margin-bottom: 16px; border-radius: 16px; }
.table-card { border-radius: 16px; }
:deep(.arco-table-th) { background: #f5f8ff; font-weight: 700; }
</style>
