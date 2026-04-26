<template>
  <div class="page-wrapper">
    <GovernanceCompactQueryPanel density="ultra" theme="aurora">
      <template #footerActions>
        <div class="query-panel-mode-actions">
          <a-button type="primary" class="query-panel-mode-btn"
            :class="{ 'is-active': textMatchMode === 'fuzzy', 'is-inactive': textMatchMode !== 'fuzzy' }"
            @click="textMatchMode = 'fuzzy'; handleSearch()">模糊查找</a-button>
          <a-button type="primary" class="query-panel-mode-btn"
            :class="{ 'is-active': textMatchMode === 'exact', 'is-inactive': textMatchMode !== 'exact' }"
            @click="textMatchMode = 'exact'; handleSearch()">精确查找</a-button>
        </div>
        <a-button @click="handleReset">重置条件</a-button>
      </template>
      <a-form :model="queryParams" layout="inline" class="biz-query-form">
        <a-form-item field="deptName" label="部门名称">
          <a-input v-model="queryParams.deptName" placeholder="请输入部门名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="orgId" label="所属机构">
          <a-tree-select v-model="queryParams.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="全部机构" allow-clear @change="handleSearch" />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select v-model="queryParams.status" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增部门
        </a-button>
        <a-popover trigger="click" position="br" :content-style="{ padding: 0 }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting :items="columnSettingItems" :drag-state="dragState" @toggle-visible="toggleColumnVisible" @move="moveColumn" @toggle-fixed="toggleColumnFixed" @drag-start="handleDragStart" @drag-over="handleDragOver" @drop="handleDrop" @drag-end="handleDragEnd" @reset="resetColumns" />
          </template>
        </a-popover>
      </template>
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" :default-expand-all-rows="true" size="small" :scroll="{ y: '100%' }" :scrollbar="true" sticky-header :columns="visibleColumns" column-resizable @column-resize="handleColumnResize">
        <template #deptType="{ record }">
          <a-tag size="small" :color="deptTypeColor(record.deptType)">{{ deptTypeLabel(record.deptType) }}</a-tag>
        </template>
        <template #funcType="{ record }">
          <span>{{ record.funcType || '无' }}</span>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-dropdown trigger="click">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="handleAdd(record.id)">新增子部门</a-doption>
                <a-doption class="is-danger" @click="confirmDelete(record.id)">删除部门</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="680" :height="580" :min-width="520" :min-height="420">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select v-model="formData.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="请选择所属机构" allow-clear @change="handleOrgChange" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="parentId" label="上级部门">
              <a-tree-select v-model="formData.parentId" :data="deptTreeOptions" :field-names="{ key: 'id', title: 'deptName', children: 'children' }" placeholder="请选择上级部门" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="deptName" label="部门名称">
              <a-input v-model="formData.deptName" placeholder="请输入部门名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="deptCode" label="部门编码">
              <a-input v-model="formData.deptCode" placeholder="请输入部门编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="deptType" label="部门类型">
              <a-select v-model="formData.deptType" placeholder="请选择部门类型">
                <a-option :value="1">事业部</a-option>
                <a-option :value="2">中心</a-option>
                <a-option :value="3">部门</a-option>
                <a-option :value="4">小组</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="funcType" label="职能分类">
              <a-select v-model="formData.funcType" placeholder="请选择职能分类" allow-clear>
                <a-option v-for="f in FUNC_TYPES" :key="f" :value="f">{{ f }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="sort" label="显示排序">
              <a-input-number v-model="formData.sort" :min="0" placeholder="排序" style="width: 100%;" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="leader" label="负责人">
              <a-input v-model="formData.leader" placeholder="请输入负责人" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="phone" label="联系电话">
              <a-input v-model="formData.phone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="email" label="邮箱">
              <a-input v-model="formData.email" placeholder="请输入邮箱" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="status" label="部门状态">
              <a-select v-model="formData.status" placeholder="请选择">
                <a-option :value="1">正常</a-option>
                <a-option :value="0">停用</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemDept' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings } from '@arco-design/web-vue/es/icon';
import { fetchDeptList, createDept, updateDept, deleteDept, fetchOrgList, type DeptVO, type DeptForm, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';

const DEPT_TYPE_MAP: Record<number, string> = { 1: '事业部', 2: '中心', 3: '部门', 4: '小组' };
const DEPT_TYPE_COLOR: Record<number, string> = { 1: 'purple', 2: 'arcoblue', 3: 'green', 4: 'cyan' };
const deptTypeLabel = (t: number) => DEPT_TYPE_MAP[t] || '部门';
const deptTypeColor = (t: number) => DEPT_TYPE_COLOR[t] || 'gray';
const FUNC_TYPES = ['管理', '研发', '销售', '财务', '人事', '行政', '生产', '采购', '仓储'];

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');

const defaultColumns: BusinessTableColumn[] = [
  { key: 'deptName', title: '部门名称', dataIndex: 'deptName', width: 200, visible: true },
  { key: 'deptCode', title: '部门编码', dataIndex: 'deptCode', width: 120, visible: true },
  { key: 'orgName', title: '所属机构', dataIndex: 'orgName', width: 150, visible: true },
  { key: 'deptType', title: '部门类型', slotName: 'deptType', width: 100, visible: true, align: 'center' },
  { key: 'funcType', title: '职能分类', slotName: 'funcType', width: 100, visible: true, align: 'center' },
  { key: 'leader', title: '负责人', dataIndex: 'leader', width: 100, visible: true },
  { key: 'phone', title: '联系电话', dataIndex: 'phone', width: 130, visible: true },
  { key: 'sort', title: '排序', dataIndex: 'sort', width: 70, visible: true, align: 'center' },
  { key: 'status', title: '状态', slotName: 'status', width: 80, visible: true, align: 'center' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 170, visible: true },
  { key: 'actions', title: '操作', slotName: 'actions', width: 140, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-dept', defaultColumns);

const loading = ref(false);
const tableData = ref<DeptVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增部门');
const formRef = ref();

const queryParams = reactive({ deptName: '', status: undefined as number | undefined, orgId: undefined as number | undefined });

const defaultForm = (): DeptForm => ({
  id: undefined, parentId: 0, orgId: undefined, deptName: '', deptCode: '',
  deptType: 3, funcType: undefined, sort: 0, leader: '', phone: '', email: '', status: 1
});
const formData = reactive<DeptForm>(defaultForm());

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称' }]
};

const deptTreeOptionsData = ref<DeptVO[]>([]);

const deptTreeOptions = computed(() => {
  const children = deptTreeOptionsData.value.length ? deptTreeOptionsData.value : tableData.value;
  const root = { id: 0, parentId: -1, deptName: '根部门', deptCode: '', orgId: 0, orgName: '',
    deptType: 3, funcType: '', sort: 0, leader: '', phone: '', email: '', status: 1,
    createTime: '', children } as DeptVO;
  return [root];
});

const loadOrgTree = async () => {
  try { const res = await fetchOrgList({}) as any; orgTreeData.value = res.data || []; }
  catch { orgTreeData.value = []; }
};

const loadData = async () => {
  loading.value = true;
  try { const res = await fetchDeptList(queryParams) as any; tableData.value = res.data || []; }
  catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => { queryParams.deptName = ''; queryParams.status = undefined; queryParams.orgId = undefined; loadData(); };

const handleOrgChange = async (orgId: number | undefined) => {
  try { const res = await fetchDeptList({ orgId }) as any; deptTreeOptionsData.value = res.data || []; }
  catch { deptTreeOptionsData.value = []; }
  formData.parentId = undefined;
};

const handleAdd = (parentId?: number) => {
  dialogTitle.value = '新增部门';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

const handleEdit = (row: DeptVO) => {
  dialogTitle.value = '编辑部门';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, orgId: row.orgId || undefined,
    deptName: row.deptName, deptCode: row.deptCode || '',
    deptType: row.deptType || 3, funcType: row.funcType || undefined,
    sort: row.sort, leader: row.leader, phone: row.phone, email: row.email, status: row.status
  });
  dialogVisible.value = true;
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) { await updateDept(formData); Message.success('修改成功'); }
    else { await createDept(formData); Message.success('新增成功'); }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try { await deleteDept(id); Message.success('删除成功'); loadData(); }
  catch { /* ignore */ }
};

const confirmDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确认删除该部门吗？',
    onOk: () => handleDelete(id),
  });
};

onMounted(() => { loadOrgTree(); loadData(); });
</script>

<style scoped>
</style>
