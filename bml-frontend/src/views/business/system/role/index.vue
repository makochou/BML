<template>
  <div class="page-wrapper">
    <GovernanceCompactQueryPanel density="ultra" theme="aurora">
      <template #note>
        <a-button class="query-panel-toggle-btn" @click="queryExpanded = !queryExpanded">
          <template #icon>
            <component :is="queryExpanded ? IconUp : IconDown" />
          </template>
          {{ queryExpanded ? '收起条件' : '更多条件' }}
        </a-button>
      </template>
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
        <!-- 主要字段（默认显示 3 列） -->
        <div class="biz-query-form-primary">
          <a-form-item field="roleName" label="角色名称">
            <a-input v-model="queryParams.roleName" placeholder="请输入角色名称" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="roleCode" label="角色编码">
            <a-input v-model="queryParams.roleCode" placeholder="请输入角色编码" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="status" label="状态">
            <a-select v-model="queryParams.status" placeholder="全部" allow-clear @change="handleSearch">
              <a-option :value="1">正常</a-option>
              <a-option :value="0">停用</a-option>
            </a-select>
          </a-form-item>
        </div>

        <!-- 次要字段（展开时显示，4 列网格） -->
        <transition name="query-expand">
          <div v-if="queryExpanded" class="biz-query-form-extra">
            <a-form-item field="dataScope" label="数据权限">
              <a-select v-model="queryParams.dataScope" placeholder="全部" allow-clear @change="handleSearch">
                <a-option v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value" :value="ds.value">{{ ds.label }}</a-option>
              </a-select>
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button v-if="hasPermission('system:role:add')" type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增角色
        </a-button>
        <a-button v-if="hasPermission('system:role:assignUser')" class="toolbar-btn--binduser" :disabled="!selectedRole" @click="handleUserAssignFromToolbar">
          <template #icon><icon-user-group /></template>
          绑定用户
        </a-button>
        <a-popover trigger="click" position="br"
          :content-style="{ padding: '0', background: 'transparent', boxShadow: 'none', border: 'none' }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting :items="columnSettingItems" :drag-state="dragState" @toggle-visible="toggleColumnVisible" @move="moveColumn" @toggle-fixed="toggleColumnFixed" @drag-start="handleDragStart" @drag-over="handleDragOver" @drop="handleDrop" @drag-end="handleDragEnd" @reset="resetColumns" />
          </template>
        </a-popover>
      </template>
      <a-table :key="tableResetKey" :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" stripe size="small" :scroll="{ x: scrollX, y: '100%' }" :scrollbar="false" sticky-header :columns="visibleColumns" column-resizable :row-class="getRowClass" @row-click="handleRowClick" @column-resize="handleColumnResize" @row-dblclick="handleRowDblClick">
        <!-- 自定义列头：每列标题旁加放大镜搜索图标（与授权治理一致） -->
        <template #th-roleName><TableColumnSearch title="角色名称" v-model="columnFilters['roleName']" /></template>
        <template #th-roleCode><TableColumnSearch title="角色编码" v-model="columnFilters['roleCode']" /></template>
        <template #th-dataScope><TableColumnSearch title="数据权限" v-model="columnFilters['dataScope']" /></template>
        <template #th-sort><TableColumnSearch title="排序" v-model="columnFilters['sort']" /></template>
        <template #th-status><TableColumnSearch title="状态" v-model="columnFilters['status']" /></template>
        <template #th-createTime><TableColumnSearch title="创建时间" v-model="columnFilters['createTime']" /></template>
        <template #th-remark><TableColumnSearch title="备注" v-model="columnFilters['remark']" /></template>

        <template #dataScope="{ record }">
          <a-tag size="small" :color="dataScopeColor(record.dataScope)">{{ dataScopeLabel(record.dataScope) }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button v-if="hasPermission('system:role:edit')" type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-button v-if="hasPermission('system:role:assign')" size="mini" class="table-action-btn table-action-btn--warning" @click="handlePermAssign(record)">
              <template #icon><icon-safe /></template>
              授权
            </a-button>
            <a-dropdown v-if="hasPermission('system:role:remove')" trigger="click" position="br">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="confirmDelete(record.id)">
                  <template #icon><icon-delete /></template>
                  删除角色
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
      <!-- 底部统计栏：左侧统计信息 + 右侧分页 -->
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ pagination.total }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">正常 <b>{{ activeCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">停用 <b>{{ disabledCount }}</b></span>
        </div>
        <div class="biz-table-footer__actions">
          <a-pagination v-model:current="pagination.current" v-model:page-size="pagination.pageSize"
            :total="pagination.total" show-total show-page-size size="small"
            @change="handlePageChange" @page-size-change="handlePageSizeChange" />
        </div>
      </div>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="700" :height="620" :min-width="540" :min-height="440">
      <a-form :model="formData" ref="formRef" :rules="formReadonly ? undefined : formRules" layout="vertical" :disabled="formReadonly">
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
            <a-form-item v-if="hasPermission('system:role:field:remark')" field="remark" label="备注">
              <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
            </a-form-item>
          </a-tab-pane>
          <a-tab-pane v-if="hasPermission('system:role:field:dataScope')" key="dataScope" title="数据权限">
            <a-form-item field="dataScope" label="数据范围">
              <a-select v-model="formData.dataScope" placeholder="请选择数据范围">
                <a-option v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value" :value="ds.value">{{ ds.label }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item v-if="formData.dataScope === 7 && hasPermission('system:role:field:customOrgIds')" label="自定义机构范围">
              <a-tree-select v-model="formData.customOrgIds" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" multiple tree-checkable placeholder="请选择可访问的机构范围" />
            </a-form-item>
            <a-form-item v-if="formData.dataScope === 7 && hasPermission('system:role:field:customDeptIds')" label="自定义部门范围">
              <a-tree-select v-model="formData.customDeptIds" :data="deptTreeData" :field-names="{ key: 'id', title: 'deptName', children: 'children' }" multiple tree-checkable placeholder="请选择可访问的部门范围" />
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
      <template #header-actions>
        <a-button @click="dialogVisible = false">{{ formReadonly ? '关闭' : '取消' }}</a-button>
        <a-button v-if="!formReadonly" type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>

    <!-- 绑定用户弹窗（独立组件） -->
    <RoleUserDialog
      v-model:visible="userDialogVisible"
      :role-id="userRoleId"
      :role-name="userRoleName"
      @saved="loadData"
    />

    <!-- 权限分配弹窗（独立组件） -->
    <RolePermissionDialog
      v-model:visible="permDialogVisible"
      :role-id="permRoleId"
      :role-name="permRoleName"
      @saved="loadData"
    />
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemRole' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconSettings, IconUp, IconDown, IconDelete, IconSafe, IconMore, IconUserGroup } from '@arco-design/web-vue/es/icon';
import { fetchRolePage, fetchRoleDetail, createRole, updateRole, deleteRole, fetchOrgList, fetchDeptList, type RoleVO, type RoleForm, type OrgVO, type DeptVO } from '../../../../api/system';
import RolePermissionDialog from './RolePermissionDialog.vue';
import RoleUserDialog from './RoleUserDialog.vue';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据', desc: '不受任何限制，可查看系统所有数据' },
  { value: 2, label: '所在机构及下级', desc: '可查看本机构及下级数据（受机构数据隔离模式影响，完全隔离的下级不可见）' },
  { value: 3, label: '仅本机构', desc: '只能查看本机构数据（若本机构设为同级互通，可看兄弟机构数据）' },
  { value: 4, label: '所在部门及下级', desc: '可查看本部门及所有下级部门数据' },
  { value: 5, label: '仅本部门', desc: '只能查看当前所属部门的数据' },
  { value: 6, label: '仅本人', desc: '只能查看自己创建的数据' },
  { value: 7, label: '自定义', desc: '管理员手动指定可访问的机构/部门范围' },
  { value: 8, label: '本人及下属', desc: '可查看自己及汇报链所有下属员工创建的数据' }
];
const DS_MAP: Record<number, string> = Object.fromEntries(DATA_SCOPE_OPTIONS.map(d => [d.value, d.label]));
const DS_COLOR: Record<number, string> = { 1: 'red', 2: 'purple', 3: 'arcoblue', 4: 'cyan', 5: 'green', 6: 'orangered', 7: 'gold', 8: 'magenta' };
const dataScopeLabel = (v: number) => DS_MAP[v] || '未设置';
const dataScopeColor = (v: number) => DS_COLOR[v] || 'gray';

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

/**
 * 角色列默认配置（与授权治理列管理模式一致）：
 * - 角色名称：默认固定在左侧（fixed: 'left'）
 * - 常用字段默认显示，扩展字段默认隐藏
 */
/** 列头搜索筛选条件 */
const columnFilters = reactive<Record<string, string>>({
  roleName: '', roleCode: '', dataScope: '', sort: '', status: '', createTime: '', remark: '',
});

const defaultColumns: BusinessTableColumn[] = [
  /* ── 核心标识（默认显示） ── */
  { key: 'roleName',   title: '角色名称', dataIndex: 'roleName',   width: 200, visible: true, fixed: 'left', sortable: true, titleSlotName: 'th-roleName' },
  { key: 'roleCode',   title: '角色编码', dataIndex: 'roleCode',   width: 200, visible: true, sortable: true, titleSlotName: 'th-roleCode' },
  { key: 'dataScope',  title: '数据权限', slotName: 'dataScope',   width: 160, visible: true, align: 'center', sortable: true, titleSlotName: 'th-dataScope', permission: 'system:role:field:dataScope' },
  { key: 'sort',       title: '排序',     dataIndex: 'sort',       width: 100, visible: true, align: 'center', sortable: true, titleSlotName: 'th-sort' },
  { key: 'status',     title: '状态',     slotName: 'status',      width: 100, visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 200, visible: true, sortable: true, titleSlotName: 'th-createTime' },
  /* ── 扩展字段（默认隐藏） ── */
  { key: 'remark', title: '备注', dataIndex: 'remark', width: 240, visible: true, ellipsis: true, sortable: true, titleSlotName: 'th-remark', permission: 'system:role:field:remark' },
  /* ── 操作列（锁定） ── */
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, tableResetKey, scrollX, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-role', defaultColumns);

/** 表格列宽 CSS 绑定值（含单位，供 v-bind 使用） */
const tableScrollWidth = computed(() => scrollX.value + 'px');

const loading = ref(false);
const tableData = ref<RoleVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const deptTreeData = ref<DeptVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const formRef = ref();

/** 表单只读模式 */
const formReadonly = ref(false);
const { hasPermission } = useButtonPermission();
const canEditRole = computed(() => hasPermission('system:role:edit'));

const queryParams = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined, dataScope: undefined as number | undefined });
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });

/** 当前页正常状态角色数量 */
const activeCount = computed(() => tableData.value.filter(r => r.status === 1).length);
/** 当前页停用状态角色数量 */
const disabledCount = computed(() => tableData.value.filter(r => r.status === 0).length);

const defaultForm = (): RoleForm => ({ id: undefined, roleName: '', roleCode: '', sort: 0, dataScope: 6, status: 1, menuIds: [], halfCheckMenuIds: [], customOrgIds: [], customDeptIds: [], remark: '' });

/** 权限分配弹窗状态 */
const permDialogVisible = ref(false);
const permRoleId = ref<number>();
const permRoleName = ref<string>();

/** 角色行单选（点击行即选中，无需单独的 radio 列） */
const selectedRoleId = ref<number | undefined>(undefined);
const selectedRole = computed(() => tableData.value.find(r => r.id === selectedRoleId.value));

/**
 * 行点击选中处理：单击某行即标记为当前选中角色，
 * 再次点击同一行则取消选中（toggle 行为）。
 */
const handleRowClick = (record: RoleVO) => {
  selectedRoleId.value = selectedRoleId.value === record.id ? undefined : record.id;
};

/**
 * 根据当前选中的角色ID返回行 CSS 类名，
 * 用于给选中行添加高亮背景色以区分。
 */
const getRowClass = (record: RoleVO) => {
  return record.id === selectedRoleId.value ? 'role-row--selected' : '';
};

/** 绑定用户弹窗状态 */
const userDialogVisible = ref(false);
const userRoleId = ref<number>();
const userRoleName = ref<string>();

/** 从工具栏打开绑定用户弹窗（依赖行选中） */
const handleUserAssignFromToolbar = () => {
  if (!selectedRole.value) {
    Message.warning('请先选择一个角色');
    return;
  }
  userRoleId.value = selectedRole.value.id;
  userRoleName.value = selectedRole.value.roleName;
  userDialogVisible.value = true;
};

/** 打开权限分配弹窗 */
const handlePermAssign = (row: RoleVO) => {
  permRoleId.value = row.id;
  permRoleName.value = row.roleName;
  permDialogVisible.value = true;
};
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

const loadOrgTree = async () => {
  try { const res = await fetchOrgList() as any; orgTreeData.value = res.data || []; }
  catch { orgTreeData.value = []; }
};

const loadDeptTree = async () => {
  try { const res = await fetchDeptList() as any; deptTreeData.value = res.data || []; }
  catch { deptTreeData.value = []; }
};

const handleSearch = () => { pagination.current = 1; loadData(); };
const handleReset = () => { queryParams.roleName = ''; queryParams.roleCode = ''; queryParams.status = undefined; queryParams.dataScope = undefined; pagination.current = 1; loadData(); };
const handlePageChange = (page: number) => { pagination.current = page; loadData(); };
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; loadData(); };

const handleAdd = () => {
  formReadonly.value = false;
  dialogTitle.value = '新增角色';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

const handleEdit = async (row: RoleVO) => {
  formReadonly.value = false;
  dialogTitle.value = '编辑角色';
  Object.assign(formData, {
    id: row.id, roleName: row.roleName, roleCode: row.roleCode,
    sort: row.sort, dataScope: row.dataScope || 6,
    status: row.status, menuIds: [], halfCheckMenuIds: [], customOrgIds: [], customDeptIds: [], remark: row.remark
  });
  try {
    const res = await fetchRoleDetail(row.id) as any;
    const detail = res.data || {};
    formData.menuIds = detail.menuIds || [];
    formData.halfCheckMenuIds = detail.halfCheckMenuIds || [];
    formData.customOrgIds = detail.customOrgIds || [];
    formData.customDeptIds = detail.customDeptIds || [];
  } catch { /* keep defaults */ }
  dialogVisible.value = true;
};

/** 双击行：有编辑权限则编辑，否则查看 */
const handleRowDblClick = async (record: RoleVO) => {
  if (canEditRole.value) {
    await handleEdit(record);
  } else {
    formReadonly.value = true;
    dialogTitle.value = '查看角色';
    Object.assign(formData, {
      id: record.id, roleName: record.roleName, roleCode: record.roleCode,
      sort: record.sort, dataScope: record.dataScope || 6,
      status: record.status, menuIds: [], halfCheckMenuIds: [], customOrgIds: [], customDeptIds: [], remark: record.remark
    });
    try {
      const res = await fetchRoleDetail(record.id) as any;
      const detail = res.data || {};
      formData.customOrgIds = detail.customOrgIds || [];
      formData.customDeptIds = detail.customDeptIds || [];
    } catch { /* keep defaults */ }
    dialogVisible.value = true;
  }
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

onMounted(() => { loadData(); loadOrgTree(); loadDeptTree(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) {
  padding-top: 12px;
}

/* ── 授权按钮警告色 ── */
.table-action-btn--warning {
  color: var(--color-warning-6) !important;
  border-color: var(--color-warning-3) !important;
  background: var(--color-warning-1) !important;
}
.table-action-btn--warning:hover {
  color: #fff !important;
  border-color: var(--color-warning-6) !important;
  background: var(--color-warning-6) !important;
}

/**
 * 工具栏「绑定用户」按钮
 * ──────────────────
 * 采用橙色系填充背景（与绿色"新增角色"区分），
 * 视觉上醒目且一眼可辨，禁用态保留浅橙色辨识度。
 *
 * 颜色方案（Arco Orange 色阶）：
 *   正常态 #FF7D00（Orange 6） + 白字
 *   Hover  #D25F00 加深
 *   禁用态 #FFE4BA 浅橙底 + #FFAD42 橙色文字
 */
.toolbar-btn--binduser {
  color: #fff !important;
  background: #FF7D00 !important;
  border-color: #FF7D00 !important;
  font-weight: 600 !important;
}
.toolbar-btn--binduser:hover:not(:disabled) {
  background: #D25F00 !important;
  border-color: #D25F00 !important;
}
.toolbar-btn--binduser:disabled {
  color: #FFAD42 !important;
  background: #FFE4BA !important;
  border-color: #FFCF8B !important;
  cursor: not-allowed;
}

.scope-hint {
  margin-top: 12px;
}
.scope-desc p {
  margin: 4px 0;
  font-size: 12px;
  line-height: 1.6;
}

/**
 * 修复 sticky-header 模式下表头与数据列框线不对齐
 * 原理：sticky-header 将 header 和 body 拆为两个独立 <table>，
 * 使用 table-layout: fixed 强制列宽按 <colgroup> 声明精确分配，
 * 配合 scroll.x = scrollX（可见列宽之和）保证两表总宽一致。
 */
:deep(.arco-table-element) {
  table-layout: fixed !important;
  width: v-bind(tableScrollWidth) !important;
  min-width: 0 !important;
}

:deep(.arco-table-td .arco-table-cell) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/**
 * 角色行选中高亮样式
 * ─────────────────
 * 点击某行后添加 .role-row--selected 类。
 * 所有列统一使用同一种柔和的青绿色高亮，不对某一列做特殊处理，
 * 视觉上干净、协调、一眼可辨。
 *
 * 颜色方案（柔和青绿 Teal / Cyan）：
 *   底色   #E8FFFB — 清新浅青（所有列统一）
 *   hover  #C9F5ED — 略深青
 *   文字   #0E7B6B — 深青文字，保持可读性
 *   下边线 #A8EAD9 — 与底色搭配的分割线
 */
:deep(.arco-table-tr.role-row--selected > .arco-table-td) {
  background-color: #E8FFFB !important;
  border-bottom-color: #A8EAD9 !important;
}
:deep(.arco-table-tr.role-row--selected > .arco-table-td .arco-table-cell) {
  color: #0E7B6B;
  font-weight: 600;
}
:deep(.arco-table-tr.role-row--selected:hover > .arco-table-td) {
  background-color: #C9F5ED !important;
}

/* 让数据行鼠标变为手指，暗示可点击选中 */
:deep(.arco-table-body .arco-table-tr) {
  cursor: pointer;
}
</style>
