<template>
  <div class="system-menu-page">
    <GovernanceCompactQueryPanel
      eyebrow="SYSTEM PERMISSION"
      title="菜单管理"
      description="统一维护业务系统菜单、按钮权限与字段权限，支撑角色授权、前端路由与后端接口鉴权。"
      :meta-items="summaryMeta"
      density="compact"
      theme="aurora"
    >
      <a-form :model="queryParams" layout="inline" class="menu-query-form">
        <a-form-item label="菜单名称">
          <a-input v-model="queryParams.menuName" allow-clear placeholder="请输入菜单名称" />
        </a-form-item>
        <a-form-item label="菜单类型">
          <a-select v-model="queryParams.menuType" allow-clear placeholder="全部类型" style="width: 150px">
            <a-option value="M">目录</a-option>
            <a-option value="C">菜单</a-option>
            <a-option value="B">按钮</a-option>
            <a-option value="F">字段</a-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model="queryParams.status" allow-clear placeholder="全部状态" style="width: 140px">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadData">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage
      eyebrow="RBAC METADATA"
      title="权限元数据树"
      description="目录与菜单用于页面访问，按钮与字段用于细粒度操作控制。"
      :meta-items="listMeta"
      density="compact"
      body-fill
    >
      <template #actions>
        <a-space>
          <a-button type="primary" v-if="hasPermission('system:menu:add')" @click="openAddDialog()">
            <template #icon><icon-plus /></template>
            新增菜单
          </a-button>
          <a-button @click="loadData">
            <template #icon><icon-refresh /></template>
            刷新
          </a-button>
        </a-space>
      </template>

      <a-table
        row-key="id"
        :loading="loading"
        :data="tableData"
        :columns="columns"
        :pagination="false"
        :default-expand-all-rows="true"
        :scroll="{ x: 1320 }"
        :scrollbar="false"
        class="menu-tree-table"
      >
        <template #menuName="{ record }">
          <div class="menu-name-cell">
            <span class="menu-icon"><component :is="iconByType(record.menuType)" /></span>
            <span class="menu-title">{{ record.menuName }}</span>
          </div>
        </template>
        <template #menuType="{ record }">
          <a-tag :color="typeColor(record.menuType)">{{ typeLabel(record.menuType) }}</a-tag>
        </template>
        <template #perms="{ record }">
          <a-typography-text v-if="record.perms" code>{{ record.perms }}</a-typography-text>
          <span v-else class="empty-text">未配置</span>
        </template>
        <template #visible="{ record }">
          <a-tag :color="record.visible === 1 ? 'green' : 'gray'">{{ record.visible === 1 ? '显示' : '隐藏' }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-badge :status="record.status === 1 ? 'success' : 'warning'" :text="record.status === 1 ? '正常' : '停用'" />
        </template>
        <template #actions="{ record }">
          <a-space size="mini">
            <a-button size="mini" type="text" v-if="hasPermission('system:menu:addChild')" @click="openAddDialog(record)">新增下级</a-button>
            <a-button size="mini" type="text" v-if="hasPermission('system:menu:edit')" @click="openEditDialog(record)">编辑</a-button>
            <a-button size="mini" type="text" status="danger" v-if="hasPermission('system:menu:remove')" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
    </GovernanceListStage>

    <a-modal
      v-model:visible="dialogVisible"
      :title="dialogTitle"
      width="760px"
      :mask-closable="false"
      :esc-to-close="false"
      @ok="submitForm"
      @cancel="closeDialog"
    >
      <AuditInfoFooter :data="form" style="margin-bottom: 12px;" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="menu-form">
        <a-grid :cols="2" :col-gap="18" :row-gap="6">
          <a-grid-item v-if="hasPermission('system:menu:field:parentId')">
            <a-form-item field="parentId" label="上级菜单">
              <a-select v-model="form.parentId" placeholder="请选择上级菜单" allow-search>
                <a-option :value="0">根目录</a-option>
                <a-option v-for="item in parentOptions" :key="item.id" :value="item.id">
                  {{ item.label }}
                </a-option>
              </a-select>
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:menuType')">
            <a-form-item field="menuType" label="菜单类型">
              <a-radio-group v-model="form.menuType" type="button">
                <a-radio value="M">目录</a-radio>
                <a-radio value="C">菜单</a-radio>
                <a-radio value="B">按钮</a-radio>
                <a-radio value="F">字段</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:menuName')">
            <a-form-item field="menuName" label="菜单名称">
              <a-input v-model="form.menuName" allow-clear placeholder="请输入菜单名称" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:sort')">
            <a-form-item field="sort" label="显示排序">
              <a-input-number v-model="form.sort" :min="0" :max="9999" style="width: 100%" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:path')">
            <a-form-item field="path" label="路由地址">
              <a-input v-model="form.path" allow-clear placeholder="如 system/menu 或 menu" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:component')">
            <a-form-item field="component" label="组件路径">
              <a-input v-model="form.component" allow-clear placeholder="如 system/menu/index" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:perms')">
            <a-form-item field="perms" label="权限标识">
              <a-input v-model="form.perms" allow-clear placeholder="如 system:menu:list" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:icon')">
            <a-form-item field="icon" label="图标标识">
              <a-input v-model="form.icon" allow-clear placeholder="如 list、apps、settings" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:visible')">
            <a-form-item field="visible" label="是否显示">
              <a-switch v-model="form.visible" :checked-value="1" :unchecked-value="0" checked-text="显示" unchecked-text="隐藏" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:status')">
            <a-form-item field="status" label="状态">
              <a-switch v-model="form.status" :checked-value="1" :unchecked-value="0" checked-text="正常" unchecked-text="停用" />
            </a-form-item>
          </a-grid-item>
          <a-grid-item v-if="hasPermission('system:menu:field:remark')" :span="2">
            <a-form-item field="remark" label="备注">
              <a-textarea v-model="form.remark" allow-clear placeholder="请输入备注" :max-length="500" show-word-limit />
            </a-form-item>
          </a-grid-item>
        </a-grid>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { Message, Modal, type TableColumnData } from '@arco-design/web-vue';
import { IconApps, IconCode, IconFile, IconList, IconPlus, IconRefresh } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import { createMenu, deleteMenu, fetchMenuDetail, fetchMenuList, updateMenu, type MenuForm, type MenuQuery, type MenuVO } from '../../../../api/system';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import AuditInfoFooter from '../../../../components/common/AuditInfoFooter.vue';

const { hasPermission } = useButtonPermission();
const loading = ref(false);
const tableData = ref<MenuVO[]>([]);
const dialogVisible = ref(false);
const dialogMode = ref<'add' | 'edit'>('add');
const formRef = ref();

const queryParams = reactive<MenuQuery>({
  menuName: '',
  menuType: undefined,
  status: undefined,
});

const defaultForm = (): MenuForm => ({
  parentId: 0,
  menuName: '',
  menuType: 'C',
  path: '',
  component: '',
  perms: '',
  icon: 'list',
  sort: 0,
  visible: 1,
  status: 1,
  isFrame: 0,
  remark: '',
});

const form = reactive<MenuForm>(defaultForm());

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称' }],
  menuType: [{ required: true, message: '请选择菜单类型' }],
};

const columns: TableColumnData[] = [
  { title: '菜单名称', dataIndex: 'menuName', slotName: 'menuName', width: 260, fixed: 'left' },
  { title: '类型', dataIndex: 'menuType', slotName: 'menuType', width: 90, align: 'center' },
  { title: '路由地址', dataIndex: 'path', width: 160, ellipsis: true, tooltip: true },
  { title: '组件路径', dataIndex: 'component', width: 220, ellipsis: true, tooltip: true },
  { title: '权限标识', dataIndex: 'perms', slotName: 'perms', width: 220, ellipsis: true, tooltip: true },
  { title: '排序', dataIndex: 'sort', width: 80, align: 'center' },
  { title: '显示', dataIndex: 'visible', slotName: 'visible', width: 90, align: 'center' },
  { title: '状态', dataIndex: 'status', slotName: 'status', width: 100, align: 'center' },
  { title: '操作', slotName: 'actions', width: 190, align: 'center', fixed: 'right' },
];

const flatMenus = computed(() => flattenMenus(tableData.value));
const parentOptions = computed(() => flatMenus.value.map(item => ({ id: item.id, label: `${'　'.repeat(item.level)}${item.menuName}` })));
const dialogTitle = computed(() => dialogMode.value === 'add' ? '新增菜单权限' : '编辑菜单权限');
const summaryMeta = computed(() => [
  { label: '目录', value: String(countByType('M')), tone: 'blue' as const },
  { label: '菜单', value: String(countByType('C')), tone: 'green' as const },
  { label: '按钮', value: String(countByType('B')), tone: 'gold' as const },
  { label: '字段', value: String(countByType('F')), tone: 'violet' as const },
]);
const listMeta = computed(() => [
  { label: '节点总数', value: String(flatMenus.value.length), tone: 'blue' as const },
  { label: '正常节点', value: String(flatMenus.value.filter(item => item.status === 1).length), tone: 'green' as const },
]);

function flattenMenus(list: MenuVO[], level = 0): Array<MenuVO & { level: number }> {
  const result: Array<MenuVO & { level: number }> = [];
  list.forEach(item => {
    result.push({ ...item, level });
    if (item.children?.length) {
      result.push(...flattenMenus(item.children, level + 1));
    }
  });
  return result;
}

function countByType(type: string) {
  return flatMenus.value.filter(item => item.menuType === type).length;
}

function typeLabel(type?: string) {
  return ({ M: '目录', C: '菜单', B: '按钮', F: '字段' } as Record<string, string>)[type || ''] || '未知';
}

function typeColor(type?: string) {
  return ({ M: 'blue', C: 'green', B: 'orange', F: 'purple' } as Record<string, string>)[type || ''] || 'gray';
}

function iconByType(type?: string) {
  return ({ M: IconApps, C: IconList, B: IconCode, F: IconFile } as Record<string, any>)[type || ''] || IconList;
}

async function loadData() {
  loading.value = true;
  try {
    const params: MenuQuery = {
      menuName: queryParams.menuName || undefined,
      menuType: queryParams.menuType || undefined,
      status: queryParams.status,
    };
    const res = await fetchMenuList(params) as any;
    tableData.value = Array.isArray(res.data) ? res.data : [];
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  queryParams.menuName = '';
  queryParams.menuType = undefined;
  queryParams.status = undefined;
  loadData();
}

function assignForm(data: MenuForm) {
  Object.assign(form, defaultForm(), data);
}

function openAddDialog(parent?: MenuVO) {
  dialogMode.value = 'add';
  assignForm({ ...defaultForm(), parentId: parent?.id ?? 0 });
  dialogVisible.value = true;
}

async function openEditDialog(record: MenuVO) {
  dialogMode.value = 'edit';
  const res = await fetchMenuDetail(record.id) as any;
  assignForm(res.data || record);
  dialogVisible.value = true;
}

function closeDialog() {
  dialogVisible.value = false;
  formRef.value?.resetFields?.();
}

async function submitForm() {
  const validateError = await formRef.value?.validate?.();
  if (validateError) return false;
  if (dialogMode.value === 'add') {
    await createMenu(form);
    Message.success('新增菜单成功');
  } else {
    await updateMenu(form);
    Message.success('修改菜单成功');
  }
  closeDialog();
  await loadData();
  return true;
}

function handleDelete(record: MenuVO) {
  Modal.warning({
    title: '确认删除菜单',
    content: `确定删除“${record.menuName}”吗？若存在下级菜单，后端将自动拦截。`,
    hideCancel: false,
    okText: '确认删除',
    async onOk() {
      await deleteMenu(record.id);
      Message.success('删除成功');
      await loadData();
    },
  });
}

onMounted(loadData);
</script>

<style scoped>
.system-menu-page {
  min-height: 100%;
  padding: 18px;
  background:
    radial-gradient(circle at 10% 8%, rgba(37, 99, 235, 0.08), transparent 30%),
    radial-gradient(circle at 88% 18%, rgba(20, 184, 166, 0.08), transparent 28%),
    linear-gradient(180deg, #f7fbff 0%, #f3f7fb 100%);
}

.menu-query-form {
  width: 100%;
}

.menu-tree-table :deep(.arco-table-th) {
  background: linear-gradient(180deg, #f8fbff, #eef6ff);
  color: #26364d;
  font-weight: 700;
}

.menu-tree-table :deep(.arco-table-tr:hover .arco-table-td) {
  background: rgba(37, 99, 235, 0.045);
}

.menu-name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.menu-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  color: #1769ff;
  background: linear-gradient(135deg, rgba(23, 105, 255, 0.12), rgba(17, 197, 183, 0.12));
}

.menu-title {
  font-weight: 650;
  color: #1f2d3d;
}

.empty-text {
  color: #9aa8b8;
}

.menu-form :deep(.arco-form-item-label) {
  font-weight: 650;
  color: #344054;
}
</style>
