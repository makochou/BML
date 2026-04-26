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
        <a-form-item field="roleName" label="角色名称">
          <a-input v-model="queryParams.roleName" placeholder="请输入角色名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="roleCode" label="角色编码">
          <a-input v-model="queryParams.roleCode" placeholder="请输入角色编码" allow-clear @press-enter="handleSearch" />
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
        <a-button type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增角色
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
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" stripe size="small" :scroll="{ y: '100%' }" :scrollbar="true" sticky-header :columns="visibleColumns" column-resizable @column-resize="handleColumnResize">
        <template #dataScope="{ record }">
          <a-tag size="small" :color="dataScopeColor(record.dataScope)">{{ dataScopeLabel(record.dataScope) }}</a-tag>
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
                <a-doption class="is-danger" @click="confirmDelete(record.id)">删除角色</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
      <div style="display: flex; justify-content: flex-end; padding: 12px 0 4px;">
        <a-pagination v-model:current="pagination.current" v-model:page-size="pagination.pageSize" :total="pagination.total" show-total show-page-size @change="loadData" @page-size-change="() => { pagination.current = 1; loadData(); }" />
      </div>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="700" :height="620" :min-width="540" :min-height="440">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="基本信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="roleName" label="角色名称">
                  <a-input v-model="formData.roleName" placeholder="请输入角色名称" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="roleCode" label="角色编码">
                  <a-input v-model="formData.roleCode" placeholder="请输入角色编码" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="sort" label="排序">
                  <a-input-number v-model="formData.sort" :min="0" placeholder="显示顺序" style="width: 100%;" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="status" label="状态">
                  <a-select v-model="formData.status" placeholder="请选择">
                    <a-option :value="1">正常</a-option>
                    <a-option :value="0">停用</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item field="remark" label="备注">
              <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
            </a-form-item>
          </a-tab-pane>
          <a-tab-pane key="menu" title="菜单权限">
            <a-form-item field="menuIds" label="分配菜单">
              <a-tree-select v-model="formData.menuIds" :data="menuTreeData" :field-names="{ key: 'id', title: 'menuName', children: 'children' }" placeholder="请选择菜单权限" multiple allow-clear :max-tag-count="3" tree-checkable tree-check-strictly />
            </a-form-item>
          </a-tab-pane>
          <a-tab-pane key="dataScope" title="数据权限">
            <a-form-item field="dataScope" label="数据范围">
              <a-select v-model="formData.dataScope" placeholder="请选择数据范围">
                <a-option v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value" :value="ds.value">{{ ds.label }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item v-if="formData.dataScope === 7" label="自定义机构范围">
              <a-tree-select v-model="formData.customOrgIds" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" multiple tree-checkable placeholder="请选择可访问的机构范围" />
            </a-form-item>
            <a-alert type="info" class="scope-hint">
              <template #title>数据权限说明</template>
              <div class="scope-desc">
                <p v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value">
                  <a-tag size="small" :color="dataScopeColor(ds.value)">{{ ds.label }}</a-tag>
                  <span> — {{ ds.desc }}</span>
                </p>
              </div>
            </a-alert>
          </a-tab-pane>
        </a-tabs>
      </a-form>
      <template #footer>
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemRole' });

import { ref, reactive, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings } from '@arco-design/web-vue/es/icon';
import { fetchRolePage, fetchRoleDetail, createRole, updateRole, deleteRole, fetchMenuList, fetchOrgList, type RoleVO, type RoleForm, type MenuVO, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据', desc: '不受任何限制，可查看系统所有数据' },
  { value: 2, label: '所在机构及下级', desc: '可查看本机构及下级数据（受机构数据隔离模式影响，完全隔离的下级不可见）' },
  { value: 3, label: '仅本机构', desc: '只能查看本机构数据（若本机构设为同级互通，可看兄弟机构数据）' },
  { value: 4, label: '所在部门及下级', desc: '可查看本部门及所有下级部门数据' },
  { value: 5, label: '仅本部门', desc: '只能查看当前所属部门的数据' },
  { value: 6, label: '仅本人', desc: '只能查看自己创建的数据' },
  { value: 7, label: '自定义', desc: '管理员手动指定可访问的机构/部门范围' }
];
const DS_MAP: Record<number, string> = Object.fromEntries(DATA_SCOPE_OPTIONS.map(d => [d.value, d.label]));
const DS_COLOR: Record<number, string> = { 1: 'red', 2: 'purple', 3: 'arcoblue', 4: 'cyan', 5: 'green', 6: 'orangered', 7: 'gold' };
const dataScopeLabel = (v: number) => DS_MAP[v] || '未设置';
const dataScopeColor = (v: number) => DS_COLOR[v] || 'gray';

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');

const defaultColumns: BusinessTableColumn[] = [
  { key: 'roleName', title: '角色名称', dataIndex: 'roleName', width: 140, visible: true },
  { key: 'roleCode', title: '角色编码', dataIndex: 'roleCode', width: 140, visible: true },
  { key: 'dataScope', title: '数据权限', slotName: 'dataScope', width: 140, visible: true, align: 'center' },
  { key: 'sort', title: '排序', dataIndex: 'sort', width: 70, visible: true, align: 'center' },
  { key: 'status', title: '状态', slotName: 'status', width: 80, visible: true, align: 'center' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 170, visible: true },
  { key: 'remark', title: '备注', dataIndex: 'remark', width: 150, visible: true, ellipsis: true },
  { key: 'actions', title: '操作', slotName: 'actions', width: 140, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-role', defaultColumns);

const loading = ref(false);
const tableData = ref<RoleVO[]>([]);
const menuTreeData = ref<MenuVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const formRef = ref();

const queryParams = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined });
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });

const defaultForm = (): RoleForm => ({ id: undefined, roleName: '', roleCode: '', sort: 0, dataScope: 6, status: 1, menuIds: [], customOrgIds: [], remark: '' });
const formData = reactive<RoleForm>(defaultForm());

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [{ required: true, message: '请输入角色编码' }]
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchRolePage({ ...queryParams, pageNum: pagination.current, pageSize: pagination.pageSize }) as any;
    const data = res.data || {};
    pagination.total = data.total || 0;
    tableData.value = data.records || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const loadMenuTree = async () => {
  try { const res = await fetchMenuList() as any; menuTreeData.value = res.data || []; }
  catch { menuTreeData.value = []; }
};

const loadOrgTree = async () => {
  try { const res = await fetchOrgList() as any; orgTreeData.value = res.data || []; }
  catch { orgTreeData.value = []; }
};

const handleSearch = () => { pagination.current = 1; loadData(); };
const handleReset = () => { queryParams.roleName = ''; queryParams.roleCode = ''; queryParams.status = undefined; pagination.current = 1; loadData(); };

const handleAdd = () => {
  dialogTitle.value = '新增角色';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

const handleEdit = async (row: RoleVO) => {
  dialogTitle.value = '编辑角色';
  Object.assign(formData, {
    id: row.id, roleName: row.roleName, roleCode: row.roleCode,
    sort: row.sort, dataScope: row.dataScope || 6,
    status: row.status, menuIds: [], customOrgIds: [], remark: row.remark
  });
  try {
    const res = await fetchRoleDetail(row.id) as any;
    const detail = res.data || {};
    formData.menuIds = detail.menuIds || [];
    formData.customOrgIds = detail.customOrgIds || [];
  } catch { /* keep defaults */ }
  dialogVisible.value = true;
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) { await updateRole(formData); Message.success('修改成功'); }
    else { await createRole(formData); Message.success('新增成功'); }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try { await deleteRole(id); Message.success('删除成功'); loadData(); }
  catch { /* ignore */ }
};

const confirmDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确认删除该角色吗？',
    onOk: () => handleDelete(id),
  });
};

onMounted(() => { loadData(); loadMenuTree(); loadOrgTree(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) {
  padding-top: 12px;
}
.scope-hint {
  margin-top: 12px;
}
.scope-desc p {
  margin: 4px 0;
  font-size: 12px;
  line-height: 1.6;
}
</style>
