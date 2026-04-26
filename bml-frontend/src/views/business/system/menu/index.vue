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
        <a-form-item field="menuName" label="菜单名称">
          <a-input v-model="queryParams.menuName" placeholder="请输入菜单名称" allow-clear @press-enter="handleSearch" />
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
          新增菜单
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
        <template #icon="{ record }">
          <span v-if="record.icon" class="icon-text">{{ record.icon }}</span>
          <span v-else style="color: #c9cdd4;">-</span>
        </template>
        <template #menuType="{ record }">
          <a-tag v-if="record.menuType === 'M'" color="blue" size="small">目录</a-tag>
          <a-tag v-else-if="record.menuType === 'C'" color="green" size="small">菜单</a-tag>
          <a-tag v-else-if="record.menuType === 'F'" color="orange" size="small">按钮</a-tag>
          <span v-else>-</span>
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
                <a-doption v-if="record.menuType !== 'F'" @click="handleAdd(record.id)">新增子菜单</a-doption>
                <a-doption class="is-danger" @click="confirmDelete(record.id)">删除菜单</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="700" :height="580" :min-width="500" :min-height="380">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="parentId" label="上级菜单">
              <a-tree-select v-model="formData.parentId" :data="menuTreeOptions" :field-names="{ key: 'id', title: 'menuName', children: 'children' }" placeholder="请选择上级菜单" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="menuType" label="菜单类型">
              <a-radio-group v-model="formData.menuType">
                <a-radio value="M">目录</a-radio>
                <a-radio value="C">菜单</a-radio>
                <a-radio value="F">按钮</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="menuName" label="菜单名称">
              <a-input v-model="formData.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="sort" label="显示排序">
              <a-input-number v-model="formData.sort" :min="0" placeholder="排序" style="width: 100%;" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16" v-if="formData.menuType !== 'F'">
          <a-col :span="12">
            <a-form-item field="path" label="路由地址">
              <a-input v-model="formData.path" placeholder="请输入路由地址" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.menuType === 'C'">
            <a-form-item field="component" label="组件路径">
              <a-input v-model="formData.component" placeholder="请输入组件路径" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12" v-if="formData.menuType !== 'M'">
            <a-form-item field="perms" label="权限标识" extra="格式：模块:资源:操作，如 system:user:list">
              <a-input v-model="formData.perms" placeholder="如：system:user:list" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.menuType !== 'F'">
            <a-form-item field="icon" label="菜单图标">
              <a-input v-model="formData.icon" placeholder="请输入图标名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item v-if="formData.menuType !== 'F'" field="isFrame" label="是否外链">
          <a-radio-group v-model="formData.isFrame">
            <a-radio :value="0">内嵌框架</a-radio>
            <a-radio :value="1">外部链接</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12" v-if="formData.menuType !== 'F'">
            <a-form-item field="visible" label="显示状态">
              <a-select v-model="formData.visible" placeholder="请选择">
                <a-option :value="1">显示</a-option>
                <a-option :value="0">隐藏</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" label="菜单状态">
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
defineOptions({ name: 'SystemMenu' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings } from '@arco-design/web-vue/es/icon';
import { fetchMenuList, createMenu, updateMenu, deleteMenu, type MenuVO, type MenuForm } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');

const defaultColumns: BusinessTableColumn[] = [
  { key: 'menuName', title: '菜单名称', dataIndex: 'menuName', width: 220, visible: true },
  { key: 'icon', title: '图标', slotName: 'icon', width: 80, visible: true, align: 'center' },
  { key: 'sort', title: '排序', dataIndex: 'sort', width: 80, visible: true, align: 'center' },
  { key: 'menuType', title: '类型', slotName: 'menuType', width: 100, visible: true, align: 'center' },
  { key: 'perms', title: '权限标识', dataIndex: 'perms', width: 200, visible: true, ellipsis: true },
  { key: 'component', title: '组件路径', dataIndex: 'component', width: 200, visible: true, ellipsis: true },
  { key: 'status', title: '状态', slotName: 'status', width: 100, visible: true, align: 'center' },
  { key: 'actions', title: '操作', slotName: 'actions', width: 140, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-menu', defaultColumns);

const loading = ref(false);
const tableData = ref<MenuVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增菜单');
const formRef = ref();

const queryParams = reactive({ menuName: '', status: undefined as number | undefined });

const defaultForm = (): MenuForm => ({
  id: undefined, parentId: 0, menuName: '', menuType: 'M', path: '', component: '', perms: '',
  icon: '', sort: 0, visible: 1, status: 1, isFrame: 0
});
const formData = reactive<MenuForm>(defaultForm());

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称' }],
  menuType: [{ required: true, message: '请选择菜单类型' }]
};

const menuTreeOptions = computed(() => {
  const root: MenuVO = { id: 0, parentId: -1, menuName: '根目录', menuType: 'M', path: '', component: '', perms: '', icon: '', sort: 0, visible: 1, status: 1, isFrame: 0, remark: '', createTime: '', children: tableData.value };
  return [root];
});

const loadData = async () => {
  loading.value = true;
  try { const res = await fetchMenuList(queryParams) as any; tableData.value = res.data || []; }
  catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => { queryParams.menuName = ''; queryParams.status = undefined; loadData(); };

const handleAdd = (parentId?: number) => {
  dialogTitle.value = '新增菜单';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

const handleEdit = (row: MenuVO) => {
  dialogTitle.value = '编辑菜单';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, menuName: row.menuName, menuType: row.menuType,
    path: row.path, component: row.component, perms: row.perms, icon: row.icon,
    sort: row.sort, visible: row.visible, status: row.status, isFrame: row.isFrame
  });
  dialogVisible.value = true;
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) { await updateMenu(formData); Message.success('修改成功'); }
    else { await createMenu(formData); Message.success('新增成功'); }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try { await deleteMenu(id); Message.success('删除成功'); loadData(); }
  catch { /* ignore */ }
};

const confirmDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确认删除该菜单吗？',
    onOk: () => handleDelete(id),
  });
};

onMounted(() => { loadData(); });
</script>

<style scoped>
.icon-text {
  color: var(--bml-primary, #165dff);
  font-weight: 600;
}
</style>
