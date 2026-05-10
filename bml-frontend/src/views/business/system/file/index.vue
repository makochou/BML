<template>
  <div class="ops-page">
    <a-card class="query-card" :bordered="false">
      <a-form :model="queryParams" layout="inline">
        <a-form-item label="文件名"><a-input v-model="queryParams.originalName" allow-clear placeholder="请输入文件名" /></a-form-item>
        <a-form-item label="扩展名"><a-input v-model="queryParams.fileExt" allow-clear placeholder="如 pdf、xlsx" /></a-form-item>
        <a-form-item label="状态">
          <a-select v-model="queryParams.status" allow-clear placeholder="全部" style="width: 120px">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>查询</a-button>
            <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="table-card" :bordered="false">
      <template #title>文件管理</template>
      <template #extra>
        <a-upload :auto-upload="false" :show-file-list="false" @change="handleUpload">
          <template #upload-button>
            <a-button type="primary" :disabled="!hasPermission('system:file:upload')"><template #icon><icon-upload /></template>上传文件</a-button>
          </template>
        </a-upload>
      </template>
      <a-table row-key="id" :loading="loading" :data="tableData" :pagination="pagination" @page-change="onPageChange" @page-size-change="onPageSizeChange">
        <template #columns>
          <a-table-column title="文件名" data-index="originalName" :width="260" ellipsis tooltip />
          <a-table-column title="扩展名" data-index="fileExt" :width="100" />
          <a-table-column title="大小" :width="120"><template #cell="{ record }">{{ formatSize(record.fileSize) }}</template></a-table-column>
          <a-table-column title="MIME" data-index="mimeType" :width="180" ellipsis tooltip />
          <a-table-column title="上传时间" data-index="createTime" :width="180" />
          <a-table-column title="操作" :width="160" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space size="mini">
                <a-button size="mini" type="text" :disabled="!hasPermission('system:file:download')" @click="downloadRow(record)">下载</a-button>
                <a-button size="mini" type="text" status="danger" :disabled="!hasPermission('system:file:remove')" @click="removeRow(record)">删除</a-button>
              </a-space>
            </template>
          </a-table-column>
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
import { deleteSystemFile, downloadSystemFile, fetchFilePage, uploadSystemFile, type FileQuery, type FileVO } from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<FileVO[]>([]);
const queryParams = reactive<FileQuery>({ pageNum: 1, pageSize: 10, originalName: '', fileExt: '', status: undefined });
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });

async function loadData() {
  loading.value = true;
  try {
    const res = await fetchFilePage({ ...queryParams, originalName: queryParams.originalName || undefined, fileExt: queryParams.fileExt || undefined }) as any;
    const page = normalizePageResult<FileVO>(res.data, pagination.current, pagination.pageSize);
    tableData.value = page.records;
    pagination.total = page.total;
    pagination.current = page.pageNum;
    pagination.pageSize = page.pageSize;
  } finally {
    loading.value = false;
  }
}

function handleSearch() { pagination.current = 1; queryParams.pageNum = 1; loadData(); }
function handleReset() { queryParams.originalName = ''; queryParams.fileExt = ''; queryParams.status = undefined; handleSearch(); }
function onPageChange(page: number) { pagination.current = page; queryParams.pageNum = page; loadData(); }
function onPageSizeChange(pageSize: number) { pagination.pageSize = pageSize; queryParams.pageSize = pageSize; queryParams.pageNum = 1; loadData(); }

async function handleUpload(_fileList: any, fileItem: any) {
  const file = fileItem?.file;
  if (!file) return;
  await uploadSystemFile(file);
  Message.success('上传成功');
  await loadData();
}

async function downloadRow(record: FileVO) {
  const res = await downloadSystemFile(record.id);
  const blob = new Blob([res.data]);
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = record.originalName;
  link.click();
  URL.revokeObjectURL(url);
}

function removeRow(record: FileVO) {
  Modal.warning({ title: '确认删除文件', content: `确定删除“${record.originalName}”吗？`, hideCancel: false, async onOk() { await deleteSystemFile(record.id); Message.success('删除成功'); await loadData(); } });
}

function formatSize(size?: number) {
  const value = size || 0;
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

onMounted(loadData);
</script>

<style scoped>
.ops-page { padding: 18px; min-height: 100%; background: linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%); }
.query-card { margin-bottom: 16px; border-radius: 16px; }
.table-card { border-radius: 16px; }
:deep(.arco-table-th) { background: #f5f8ff; font-weight: 700; }
</style>
