<template>
  <div class="system-dict-page">
    <GovernanceCompactQueryPanel
      eyebrow="BASIC CONFIGURATION"
      title="字典管理"
      description="集中维护系统基础枚举、状态选项和业务下拉数据，避免前后端硬编码。"
      :meta-items="queryMetaItems"
      density="compact"
      theme="aurora"
    >
      <a-form :model="typeQuery" layout="inline" class="dict-query-form">
        <a-form-item label="字典名称">
          <a-input v-model="typeQuery.dictName" allow-clear placeholder="请输入字典名称" />
        </a-form-item>
        <a-form-item label="字典编码">
          <a-input v-model="typeQuery.dictType" allow-clear placeholder="如 sys_user_gender" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model="typeQuery.status" allow-clear placeholder="全部状态" style="width: 140px">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadTypes">查询</a-button>
            <a-button @click="resetTypeQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </GovernanceCompactQueryPanel>

    <div class="dict-workbench">
      <GovernanceListStage
        eyebrow="DICT TYPE"
        title="字典类型"
        description="按业务主题维护字典分类编码。"
        :meta-items="typeMetaItems"
        density="compact"
      >
        <template #actions>
          <a-space>
            <a-button type="primary" v-if="hasPermission('system:dict:add')" @click="openTypeDialog('add')">
              <template #icon><icon-plus /></template>
              新增类型
            </a-button>
            <a-button @click="loadTypes">
              <template #icon><icon-refresh /></template>
              刷新
            </a-button>
          </a-space>
        </template>

        <a-table
          row-key="id"
          :loading="typeLoading"
          :data="typeRows"
          :pagination="typePagination"
          :bordered="false"
          class="dict-table"
          @page-change="onTypePageChange"
          @page-size-change="onTypePageSizeChange"
          @row-click="selectType"
        >
          <template #columns>
            <a-table-column title="字典名称" data-index="dictName" :width="150">
              <template #cell="{ record }">
                <div class="type-name" :class="{ active: selectedType?.id === record.id }">
                  <span>{{ record.dictName }}</span>
                  <a-tag v-if="selectedType?.id === record.id" color="arcoblue" size="small">当前</a-tag>
                </div>
              </template>
            </a-table-column>
            <a-table-column title="字典编码" data-index="dictType" :width="180">
              <template #cell="{ record }">
                <a-typography-text code>{{ record.dictType }}</a-typography-text>
              </template>
            </a-table-column>
            <a-table-column title="状态" data-index="status" :width="80" align="center">
              <template #cell="{ record }">
                <a-badge :status="record.status === 1 ? 'success' : 'warning'" :text="record.status === 1 ? '正常' : '停用'" />
              </template>
            </a-table-column>
            <a-table-column title="创建时间" data-index="createTime" :width="160" />
            <a-table-column title="操作" :width="130" align="center">
              <template #cell="{ record }">
                <a-space size="mini">
                  <a-button size="mini" type="text" v-if="hasPermission('system:dict:edit')" @click.stop="openTypeDialog('edit', record)">编辑</a-button>
                  <a-button size="mini" type="text" status="danger" v-if="hasPermission('system:dict:remove')" @click.stop="removeType(record)">删除</a-button>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </GovernanceListStage>

      <GovernanceListStage
        eyebrow="DICT DATA"
        :title="selectedType ? `字典数据 · ${selectedType.dictName}` : '字典数据'"
        :description="selectedType ? `当前编码：${selectedType.dictType}` : '请选择左侧字典类型后维护字典数据。'"
        :meta-items="dataMetaItems"
        density="compact"
      >
        <template #actions>
          <a-space>
            <a-input v-model="dataQuery.dictLabel" allow-clear placeholder="搜索标签" style="width: 180px" @press-enter="loadData" />
            <a-select v-model="dataQuery.status" allow-clear placeholder="状态" style="width: 120px" @change="loadData">
              <a-option :value="1">正常</a-option>
              <a-option :value="0">停用</a-option>
            </a-select>
            <a-button type="primary" :disabled="!selectedType || !hasPermission('system:dict:add')" @click="openDataDialog('add')">
              <template #icon><icon-plus /></template>
              新增数据
            </a-button>
          </a-space>
        </template>

        <a-table
          row-key="id"
          :loading="dataLoading"
          :data="dataRows"
          :pagination="dataPagination"
          :bordered="false"
          class="dict-table"
          @page-change="onDataPageChange"
          @page-size-change="onDataPageSizeChange"
        >
          <template #columns>
            <a-table-column title="标签" data-index="dictLabel" :width="170">
              <template #cell="{ record }">
                <a-tag :color="record.cssClass || 'arcoblue'">{{ record.dictLabel }}</a-tag>
              </template>
            </a-table-column>
            <a-table-column title="键值" data-index="dictValue" :width="160">
              <template #cell="{ record }">
                <a-typography-text code>{{ record.dictValue }}</a-typography-text>
              </template>
            </a-table-column>
            <a-table-column title="样式" data-index="cssClass" :width="110">
              <template #cell="{ record }">
                <span>{{ record.cssClass || '默认' }}</span>
              </template>
            </a-table-column>
            <a-table-column title="排序" data-index="sort" :width="80" align="center" />
            <a-table-column title="状态" data-index="status" :width="90" align="center">
              <template #cell="{ record }">
                <a-badge :status="record.status === 1 ? 'success' : 'warning'" :text="record.status === 1 ? '正常' : '停用'" />
              </template>
            </a-table-column>
            <a-table-column title="操作" :width="140" align="center">
              <template #cell="{ record }">
                <a-space size="mini">
                  <a-button size="mini" type="text" v-if="hasPermission('system:dict:edit')" @click="openDataDialog('edit', record)">编辑</a-button>
                  <a-button size="mini" type="text" status="danger" v-if="hasPermission('system:dict:remove')" @click="removeData(record)">删除</a-button>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>
      </GovernanceListStage>
    </div>

    <a-modal v-model:visible="typeDialog.visible" :title="typeDialog.mode === 'add' ? '新增字典类型' : '编辑字典类型'" width="560px" @ok="submitType" @cancel="closeTypeDialog">
      <AuditInfoFooter :data="typeForm" style="margin-bottom: 12px;" />
      <a-form ref="typeFormRef" :model="typeForm" :rules="typeRules" layout="vertical">
        <a-form-item field="dictName" label="字典名称"><a-input v-model="typeForm.dictName" allow-clear placeholder="请输入字典名称" /></a-form-item>
        <a-form-item field="dictType" label="字典编码"><a-input v-model="typeForm.dictType" allow-clear placeholder="如 sys_user_gender" /></a-form-item>
        <a-form-item field="status" label="状态"><a-switch v-model="typeForm.status" :checked-value="1" :unchecked-value="0" checked-text="正常" unchecked-text="停用" /></a-form-item>
        <a-form-item field="remark" label="备注"><a-textarea v-model="typeForm.remark" allow-clear :max-length="500" show-word-limit /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:visible="dataDialog.visible" :title="dataDialog.mode === 'add' ? '新增字典数据' : '编辑字典数据'" width="620px" @ok="submitData" @cancel="closeDataDialog">
      <a-form ref="dataFormRef" :model="dataForm" :rules="dataRules" layout="vertical">
        <a-form-item field="dictType" label="字典编码"><a-input v-model="dataForm.dictType" readonly /></a-form-item>
        <a-form-item field="dictLabel" label="字典标签"><a-input v-model="dataForm.dictLabel" allow-clear placeholder="请输入显示标签" /></a-form-item>
        <a-form-item field="dictValue" label="字典键值"><a-input v-model="dataForm.dictValue" allow-clear placeholder="请输入存储键值" /></a-form-item>
        <a-form-item field="cssClass" label="标签颜色"><a-input v-model="dataForm.cssClass" allow-clear placeholder="如 arcoblue / green / orange / red" /></a-form-item>
        <a-form-item field="sort" label="显示排序"><a-input-number v-model="dataForm.sort" :min="0" :max="9999" style="width: 100%" /></a-form-item>
        <a-form-item field="status" label="状态"><a-switch v-model="dataForm.status" :checked-value="1" :unchecked-value="0" checked-text="正常" unchecked-text="停用" /></a-form-item>
        <a-form-item field="remark" label="备注"><a-textarea v-model="dataForm.remark" allow-clear :max-length="500" show-word-limit /></a-form-item>
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
  createDictData,
  createDictType,
  deleteDictData,
  deleteDictType,
  fetchDictDataDetail,
  fetchDictDataPage,
  fetchDictTypeDetail,
  fetchDictTypePage,
  updateDictData,
  updateDictType,
  type DictDataForm,
  type DictDataQuery,
  type DictDataVO,
  type DictTypeForm,
  type DictTypeQuery,
  type DictTypeVO,
} from '../../../../api/system';

const { hasPermission } = useButtonPermission();
const typeLoading = ref(false);
const dataLoading = ref(false);
const typeRows = ref<DictTypeVO[]>([]);
const dataRows = ref<DictDataVO[]>([]);
const selectedType = ref<DictTypeVO | null>(null);
const typeFormRef = ref();
const dataFormRef = ref();

const typeQuery = reactive<DictTypeQuery>({ pageNum: 1, pageSize: 10, dictName: '', dictType: '', status: undefined });
const dataQuery = reactive<DictDataQuery>({ pageNum: 1, pageSize: 10, dictType: '', dictLabel: '', status: undefined });
const typePagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });
const dataPagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true, showPageSize: true });
const typeDialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit' });
const dataDialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit' });

const defaultTypeForm = (): DictTypeForm => ({ dictName: '', dictType: '', status: 1, remark: '' });
const defaultDataForm = (): DictDataForm => ({ dictType: '', dictLabel: '', dictValue: '', cssClass: 'arcoblue', sort: 0, status: 1, remark: '' });
const typeForm = reactive<DictTypeForm>(defaultTypeForm());
const dataForm = reactive<DictDataForm>(defaultDataForm());

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称' }],
  dictType: [{ required: true, message: '请输入字典编码' }],
};
const dataRules = {
  dictType: [{ required: true, message: '请选择字典类型' }],
  dictLabel: [{ required: true, message: '请输入字典标签' }],
  dictValue: [{ required: true, message: '请输入字典键值' }],
};

const queryMetaItems = computed(() => [
  { label: '类型总数', value: typePagination.total, tone: 'blue' as const },
  { label: '当前数据项', value: dataPagination.total, tone: 'teal' as const },
  { label: '选中类型', value: selectedType.value?.dictName || '未选择', tone: 'violet' as const },
]);
const typeMetaItems = computed(() => [
  { label: '当前页', value: typeRows.value.length, tone: 'blue' as const },
  { label: '启用类型', value: typeRows.value.filter(item => item.status === 1).length, tone: 'green' as const },
]);
const dataMetaItems = computed(() => [
  { label: '数据总数', value: dataPagination.total, tone: 'blue' as const },
  { label: '启用数据', value: dataRows.value.filter(item => item.status === 1).length, tone: 'green' as const },
]);

async function loadTypes() {
  typeLoading.value = true;
  try {
    const res = await fetchDictTypePage({
      ...typeQuery,
      dictName: typeQuery.dictName || undefined,
      dictType: typeQuery.dictType || undefined,
    }) as any;
    const page = normalizePageResult<DictTypeVO>(res.data, typePagination.current, typePagination.pageSize);
    typeRows.value = page.records;
    typePagination.total = page.total;
    typePagination.current = page.pageNum;
    typePagination.pageSize = page.pageSize;
    if (!selectedType.value && typeRows.value.length) {
      selectType(typeRows.value[0]);
    }
  } finally {
    typeLoading.value = false;
  }
}

async function loadData() {
  if (!selectedType.value) {
    dataRows.value = [];
    dataPagination.total = 0;
    return;
  }
  dataLoading.value = true;
  try {
    const res = await fetchDictDataPage({
      ...dataQuery,
      dictType: selectedType.value.dictType,
      dictLabel: dataQuery.dictLabel || undefined,
    }) as any;
    const page = normalizePageResult<DictDataVO>(res.data, dataPagination.current, dataPagination.pageSize);
    dataRows.value = page.records;
    dataPagination.total = page.total;
    dataPagination.current = page.pageNum;
    dataPagination.pageSize = page.pageSize;
  } finally {
    dataLoading.value = false;
  }
}

function resetTypeQuery() {
  typeQuery.dictName = '';
  typeQuery.dictType = '';
  typeQuery.status = undefined;
  typePagination.current = 1;
  typeQuery.pageNum = 1;
  loadTypes();
}

function selectType(record: DictTypeVO) {
  selectedType.value = record;
  dataQuery.dictType = record.dictType;
  dataQuery.dictLabel = '';
  dataPagination.current = 1;
  dataQuery.pageNum = 1;
  loadData();
}

function onTypePageChange(page: number) {
  typePagination.current = page;
  typeQuery.pageNum = page;
  loadTypes();
}

function onTypePageSizeChange(pageSize: number) {
  typePagination.pageSize = pageSize;
  typeQuery.pageSize = pageSize;
  typeQuery.pageNum = 1;
  loadTypes();
}

function onDataPageChange(page: number) {
  dataPagination.current = page;
  dataQuery.pageNum = page;
  loadData();
}

function onDataPageSizeChange(pageSize: number) {
  dataPagination.pageSize = pageSize;
  dataQuery.pageSize = pageSize;
  dataQuery.pageNum = 1;
  loadData();
}

function assignTypeForm(data: DictTypeForm) {
  Object.assign(typeForm, defaultTypeForm(), data);
}

function assignDataForm(data: DictDataForm) {
  Object.assign(dataForm, defaultDataForm(), data);
}

async function openTypeDialog(mode: 'add' | 'edit', record?: DictTypeVO) {
  typeDialog.mode = mode;
  if (mode === 'edit' && record) {
    const res = await fetchDictTypeDetail(record.id) as any;
    assignTypeForm(res.data || record);
  } else {
    assignTypeForm(defaultTypeForm());
  }
  typeDialog.visible = true;
}

function closeTypeDialog() {
  typeDialog.visible = false;
  typeFormRef.value?.resetFields?.();
}

async function submitType() {
  const err = await typeFormRef.value?.validate?.();
  if (err) return false;
  if (typeDialog.mode === 'add') {
    await createDictType(typeForm);
    Message.success('新增字典类型成功');
  } else {
    await updateDictType(typeForm);
    Message.success('修改字典类型成功');
  }
  closeTypeDialog();
  await loadTypes();
  return true;
}

function removeType(record: DictTypeVO) {
  Modal.warning({
    title: '确认删除字典类型',
    content: `确定删除“${record.dictName}”吗？若该类型下存在字典数据，后端将拒绝删除。`,
    hideCancel: false,
    okText: '确认删除',
    async onOk() {
      await deleteDictType(record.id);
      if (selectedType.value?.id === record.id) selectedType.value = null;
      Message.success('删除成功');
      await loadTypes();
      await loadData();
    },
  });
}

async function openDataDialog(mode: 'add' | 'edit', record?: DictDataVO) {
  if (!selectedType.value) return;
  dataDialog.mode = mode;
  if (mode === 'edit' && record) {
    const res = await fetchDictDataDetail(record.id) as any;
    assignDataForm(res.data || record);
  } else {
    assignDataForm({ ...defaultDataForm(), dictType: selectedType.value.dictType });
  }
  dataDialog.visible = true;
}

function closeDataDialog() {
  dataDialog.visible = false;
  dataFormRef.value?.resetFields?.();
}

async function submitData() {
  const err = await dataFormRef.value?.validate?.();
  if (err) return false;
  if (dataDialog.mode === 'add') {
    await createDictData(dataForm);
    Message.success('新增字典数据成功');
  } else {
    await updateDictData(dataForm);
    Message.success('修改字典数据成功');
  }
  closeDataDialog();
  await loadData();
  return true;
}

function removeData(record: DictDataVO) {
  Modal.warning({
    title: '确认删除字典数据',
    content: `确定删除“${record.dictLabel}”吗？`,
    hideCancel: false,
    okText: '确认删除',
    async onOk() {
      await deleteDictData(record.id);
      Message.success('删除成功');
      await loadData();
    },
  });
}

onMounted(loadTypes);
</script>

<style scoped>
.system-dict-page {
  min-height: 100%;
  padding: 18px;
  background:
    radial-gradient(circle at 8% 10%, rgba(37, 99, 235, 0.08), transparent 30%),
    radial-gradient(circle at 92% 16%, rgba(124, 58, 237, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fbff 0%, #f3f7fb 100%);
}

.dict-query-form {
  width: 100%;
}

.dict-workbench {
  display: grid;
  grid-template-columns: minmax(430px, 0.9fr) minmax(560px, 1.1fr);
  gap: 16px;
  align-items: start;
}

.dict-table :deep(.arco-table-th) {
  background: linear-gradient(180deg, #f8fbff, #eef6ff);
  color: #26364d;
  font-weight: 700;
}

.dict-table :deep(.arco-table-tr:hover .arco-table-td) {
  background: rgba(37, 99, 235, 0.045);
}

.type-name {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: 650;
  color: #1f2d3d;
}

.type-name.active {
  color: #1769ff;
}

@media (max-width: 1280px) {
  .dict-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
