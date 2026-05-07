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
          <a-form-item field="menuName" label="菜单名称">
            <a-input v-model="queryParams.menuName" placeholder="请输入菜单名称" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="menuType" label="菜单类型">
            <a-select v-model="queryParams.menuType" placeholder="全部" allow-clear @change="handleSearch">
              <a-option value="M">目录</a-option>
              <a-option value="C">菜单</a-option>
              <a-option value="F">按钮</a-option>
            </a-select>
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
            <a-form-item field="perms" label="权限标识">
              <a-input v-model="queryParams.perms" placeholder="如 system:user:list" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="visible" label="显示状态">
              <a-select v-model="queryParams.visible" placeholder="全部" allow-clear @change="handleSearch">
                <a-option :value="1">显示</a-option>
                <a-option :value="0">隐藏</a-option>
              </a-select>
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button v-if="hasPermission('system:menu:add')" type="primary" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增菜单
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
      <a-table :key="tableResetKey" :data="pagedTableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" :expanded-keys="expandedKeys" size="small" :scroll="{ x: scrollX, y: '100%' }" :scrollbar="false" sticky-header :columns="visibleColumns" column-resizable @column-resize="handleColumnResize" @row-dblclick="handleRowDblClick" @expand="handleExpandChange">
        <!-- 自定义列头：每列标题旁加放大镜搜索图标（与授权治理一致） -->
        <template #th-menuName><TableColumnSearch title="菜单名称" v-model="columnFilters['menuName']" /></template>
        <template #th-icon><TableColumnSearch title="图标" v-model="columnFilters['icon']" /></template>
        <template #th-sort><TableColumnSearch title="排序" v-model="columnFilters['sort']" /></template>
        <template #th-menuType><TableColumnSearch title="类型" v-model="columnFilters['menuType']" /></template>
        <template #th-perms><TableColumnSearch title="权限标识" v-model="columnFilters['perms']" /></template>
        <template #th-status><TableColumnSearch title="状态" v-model="columnFilters['status']" /></template>
        <template #th-component><TableColumnSearch title="组件路径" v-model="columnFilters['component']" /></template>
        <template #th-path><TableColumnSearch title="路由地址" v-model="columnFilters['path']" /></template>
        <template #th-visible><TableColumnSearch title="是否显示" v-model="columnFilters['visible']" /></template>
        <template #th-isFrame><TableColumnSearch title="是否外链" v-model="columnFilters['isFrame']" /></template>
        <template #th-remark><TableColumnSearch title="备注" v-model="columnFilters['remark']" /></template>
        <template #th-createTime><TableColumnSearch title="创建时间" v-model="columnFilters['createTime']" /></template>

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
            <a-button v-if="hasPermission('system:menu:edit')" type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-button v-if="hasPermission('system:menu:remove')" size="mini" class="table-action-btn table-action-btn--danger" @click="confirmDelete(record.id)">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
            <a-dropdown v-if="record.menuType !== 'F' && hasPermission('system:menu:addChild')" trigger="click">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="handleAdd(record.id)">
                  <template #icon><icon-plus /></template>
                  新增子菜单
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>

      <!-- 底部统计栏：左侧统计信息 + 右侧树控制 & 分页 -->
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ totalMenuCount }}</b> 条</span>
          <a-divider direction="vertical" />
          <span>顶级 <b>{{ topLevelCount }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">正常 <b>{{ activeCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">停用 <b>{{ disabledCount }}</b></span>
        </div>
        <div class="biz-table-footer__actions">
          <a-button-group size="mini">
            <a-button @click="expandAll">
              <template #icon><icon-expand /></template>
              全部展开
            </a-button>
            <a-button @click="collapseAll">
              <template #icon><icon-shrink /></template>
              全部收缩
            </a-button>
          </a-button-group>
          <a-divider direction="vertical" style="margin: 0 8px;" />
          <span class="pagination-label">每页</span>
          <a-select v-model="pageSize" size="mini" style="width: 80px;" @change="handlePageSizeChange">
            <a-option :value="10">10 条</a-option>
            <a-option :value="20">20 条</a-option>
            <a-option :value="50">50 条</a-option>
            <a-option :value="0">全部</a-option>
          </a-select>
          <a-pagination
            v-if="pageSize > 0"
            v-model:current="currentPage"
            :total="topLevelCount"
            :page-size="pageSize"
            size="mini"
            :show-total="false"
            :show-page-size="false"
            style="margin-left: 8px;"
            @change="handlePageChange"
          />
        </div>
      </div>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="700" :height="580" :min-width="500" :min-height="380">
      <a-form :model="formData" ref="formRef" :rules="formReadonly ? undefined : formRules" layout="vertical" :disabled="formReadonly">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="基本信息">
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
            <a-row :gutter="16">
              <a-col :span="12" v-if="formData.menuType !== 'M' && hasPermission('system:menu:field:perms')">
                <a-form-item field="perms" label="权限标识" extra="格式：模块:资源:操作，如 system:user:list">
                  <a-input v-model="formData.perms" placeholder="如：system:user:list" />
                </a-form-item>
              </a-col>
              <a-col :span="12" v-if="formData.menuType !== 'F' && hasPermission('system:menu:field:icon')">
                <a-form-item field="icon" label="菜单图标">
                  <a-input v-model="formData.icon" placeholder="请输入图标名称" />
                </a-form-item>
              </a-col>
            </a-row>
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
          </a-tab-pane>
          <a-tab-pane key="route" title="路由配置" :disabled="formData.menuType === 'F'">
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:menu:field:path')" :span="12">
                <a-form-item field="path" label="路由地址">
                  <a-input v-model="formData.path" placeholder="请输入路由地址" />
                </a-form-item>
              </a-col>
              <a-col :span="12" v-if="formData.menuType === 'C' && hasPermission('system:menu:field:component')">
                <a-form-item field="component" label="组件路径">
                  <a-input v-model="formData.component" placeholder="请输入组件路径" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="hasPermission('system:menu:field:isFrame')" field="isFrame" label="是否外链">
              <a-radio-group v-model="formData.isFrame">
                <a-radio :value="0">内嵌框架</a-radio>
                <a-radio :value="1">外部链接</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-tab-pane>
        </a-tabs>
      </a-form>
      <template #header-actions>
        <a-button @click="dialogVisible = false">{{ formReadonly ? '关闭' : '取消' }}</a-button>
        <a-button v-if="!formReadonly" type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemMenu' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings, IconUp, IconDown, IconDelete, IconExpand, IconShrink } from '@arco-design/web-vue/es/icon';
import { fetchMenuList, createMenu, updateMenu, deleteMenu, type MenuVO, type MenuForm } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

/**
 * 菜单列默认配置（与授权治理列管理模式一致）：
 * - 菜单名称：固定在左侧（fixed: 'left'），操作列固定在右侧（fixed: 'right'）
 * - 树形表格必须使用 :scrollbar="false" + table-layout: fixed + 精确 scroll.x 来保证列对齐
 * - 常用字段默认显示，扩展字段默认隐藏
 */
/** 列头搜索筛选条件 */
const columnFilters = reactive<Record<string, string>>({
  menuName: '', icon: '', sort: '', menuType: '', perms: '', status: '',
  component: '', path: '', visible: '', isFrame: '', remark: '', createTime: '',
});

const defaultColumns: BusinessTableColumn[] = [
  /* ── 核心标识（默认显示） ── */
  { key: 'menuName',  title: '菜单名称', dataIndex: 'menuName',  width: 240, visible: true, fixed: 'left', ellipsis: true, sortable: true, titleSlotName: 'th-menuName' },
  { key: 'icon',      title: '图标',     slotName: 'icon',       width: 80,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-icon', permission: 'system:menu:field:icon' },
  { key: 'sort',      title: '排序',     dataIndex: 'sort',      width: 80,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-sort' },
  { key: 'menuType',  title: '类型',     slotName: 'menuType',   width: 100, visible: true, align: 'center', sortable: true, titleSlotName: 'th-menuType' },
  { key: 'perms',     title: '权限标识', dataIndex: 'perms',     width: 200, visible: true, ellipsis: true, sortable: true, titleSlotName: 'th-perms', permission: 'system:menu:field:perms' },
  { key: 'status',    title: '状态',     slotName: 'status',     width: 100, visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  /* ── 扩展字段（默认隐藏） ── */
  { key: 'component', title: '组件路径',   dataIndex: 'component', width: 200, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-component', permission: 'system:menu:field:component' },
  { key: 'path',      title: '路由地址',   dataIndex: 'path',      width: 200, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-path', permission: 'system:menu:field:path' },
  { key: 'visible',   title: '是否显示',   slotName: 'visible',    width: 90,  visible: false, align: 'center', sortable: true, titleSlotName: 'th-visible' },
  { key: 'isFrame',   title: '是否外链',   slotName: 'isFrame',    width: 90,  visible: false, align: 'center', sortable: true, titleSlotName: 'th-isFrame', permission: 'system:menu:field:isFrame' },
  { key: 'remark',    title: '备注',       dataIndex: 'remark',    width: 200, visible: false, ellipsis: true, sortable: true, titleSlotName: 'th-remark' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 170, visible: false, sortable: true, titleSlotName: 'th-createTime' },
  /* ── 操作列（锁定） ── */
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, tableResetKey, scrollX, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-menu', defaultColumns);

/** 树形表格列宽 CSS 绑定值（含单位，供 v-bind 使用） */
const treeTableScrollWidth = computed(() => scrollX.value + 'px');

const loading = ref(false);
const tableData = ref<MenuVO[]>([]);

/** 递归收集树所有节点 ID，用于默认展开全部行 */
const collectAllKeys = (nodes: MenuVO[]): number[] =>
  nodes.reduce<number[]>((keys, n) => {
    keys.push(n.id);
    if (n.children?.length) keys.push(...collectAllKeys(n.children));
    return keys;
  }, []);
const expandedKeys = ref<number[]>([]);

/** 递归统计树形数据总条数 */
const countTreeNodes = (nodes: MenuVO[]): number =>
  nodes.reduce((sum, n) => sum + 1 + (n.children ? countTreeNodes(n.children) : 0), 0);
const totalMenuCount = computed(() => countTreeNodes(tableData.value));

/** 递归统计树形数据中指定状态的节点数量 */
const countByStatus = (nodes: MenuVO[], status: number): number =>
  nodes.reduce((sum, n) => sum + (n.status === status ? 1 : 0) + (n.children ? countByStatus(n.children, status) : 0), 0);

/** 顶级菜单数量（树的根节点数） */
const topLevelCount = computed(() => tableData.value.length);
/** 正常状态菜单数量 */
const activeCount = computed(() => countByStatus(tableData.value, 1));
/** 停用状态菜单数量 */
const disabledCount = computed(() => countByStatus(tableData.value, 0));

/* ══════ 树形分页（按顶级节点分页，保持每棵子树完整） ══════ */
const pageSize = ref(10);
const currentPage = ref(1);

/** 分页后的表格数据：按顶级节点切片，pageSize 为 0 时显示全部 */
const pagedTableData = computed(() => {
  if (pageSize.value === 0) return tableData.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return tableData.value.slice(start, start + pageSize.value);
});

/** 全部展开 */
const expandAll = () => { expandedKeys.value = collectAllKeys(pagedTableData.value); };
/** 全部收缩 */
const collapseAll = () => { expandedKeys.value = []; };

/** 树形表格展开/收缩事件处理，受控模式下手动维护 expandedKeys */
const handleExpandChange = (rowKey: number) => {
  const idx = expandedKeys.value.indexOf(rowKey);
  if (idx >= 0) { expandedKeys.value.splice(idx, 1); }
  else { expandedKeys.value.push(rowKey); }
};

/** 切换每页条数时重置到第一页 */
const handlePageSizeChange = () => {
  currentPage.value = 1;
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};
/** 翻页后自动展开当前页所有节点 */
const handlePageChange = () => {
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};

const dialogVisible = ref(false);
const dialogTitle = ref('新增菜单');
const formRef = ref();

/** 表单只读模式 */
const formReadonly = ref(false);
const { hasPermission } = useButtonPermission();
const canEditMenu = computed(() => hasPermission('system:menu:edit'));

const queryParams = reactive({
  menuName: '', menuType: undefined as string | undefined,
  status: undefined as number | undefined,
  perms: '', visible: undefined as number | undefined
});

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
  try {
    const res = await fetchMenuList(queryParams) as any;
    tableData.value = res.data || [];
    // 重新加载数据后重置到第一页
    currentPage.value = 1;
    // 数据加载完成后默认展开当前页所有行
    expandedKeys.value = collectAllKeys(pagedTableData.value);
  }
  catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => {
  queryParams.menuName = ''; queryParams.menuType = undefined;
  queryParams.status = undefined; queryParams.perms = ''; queryParams.visible = undefined;
  loadData();
};

const handleAdd = (parentId?: number) => {
  formReadonly.value = false;
  dialogTitle.value = '新增菜单';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

const handleEdit = (row: MenuVO) => {
  formReadonly.value = false;
  dialogTitle.value = '编辑菜单';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, menuName: row.menuName, menuType: row.menuType,
    path: row.path, component: row.component, perms: row.perms, icon: row.icon,
    sort: row.sort, visible: row.visible, status: row.status, isFrame: row.isFrame
  });
  dialogVisible.value = true;
};

/** 双击行：有编辑权限则编辑，否则查看 */
const handleRowDblClick = (record: MenuVO) => {
  if (canEditMenu.value) {
    handleEdit(record);
  } else {
    formReadonly.value = true;
    dialogTitle.value = '查看菜单';
    Object.assign(formData, {
      id: record.id, parentId: record.parentId, menuName: record.menuName, menuType: record.menuType,
      path: record.path, component: record.component, perms: record.perms, icon: record.icon,
      sort: record.sort, visible: record.visible, status: record.status, isFrame: record.isFrame
    });
    dialogVisible.value = true;
  }
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
.form-tabs :deep(.arco-tabs-content) { padding-top: 12px; }
.icon-text {
  color: var(--bml-primary, #165dff);
  font-weight: 600;
}

/** 修复树形表格表头与数据列不对齐（原理同 dept 页面） */
:deep(.arco-table-element) {
  table-layout: fixed !important;
  width: v-bind(treeTableScrollWidth) !important;
  min-width: 0 !important;
}

:deep(.arco-table-td .arco-table-cell) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
