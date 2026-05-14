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
          <a-form-item field="deptName" label="部门名称">
            <a-input v-model="queryParams.deptName" placeholder="请输入部门名称" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="orgId" label="所属机构">
            <a-tree-select v-model="queryParams.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="全部机构" allow-clear @change="handleSearch" />
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
            <a-form-item field="deptCode" label="部门编码">
              <a-input v-model="queryParams.deptCode" placeholder="请输入部门编码" allow-clear @press-enter="handleSearch" />
            </a-form-item>
            <a-form-item field="deptType" label="部门类型">
              <a-select v-model="queryParams.deptType" placeholder="全部" allow-clear @change="handleSearch">
                <a-option :value="1">事业部</a-option>
                <a-option :value="2">中心</a-option>
                <a-option :value="3">部门</a-option>
                <a-option :value="4">小组</a-option>
              </a-select>
            </a-form-item>
            <a-form-item field="funcType" label="职能分类">
              <a-select v-model="queryParams.funcType" placeholder="全部" allow-clear @change="handleSearch">
                <a-option v-for="f in FUNC_TYPES" :key="f" :value="f">{{ f }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item field="leader" label="负责人">
              <a-input v-model="queryParams.leader" placeholder="请输入负责人" allow-clear @press-enter="handleSearch" />
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" :disabled="permDisabled('system:dept:add')" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增部门
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
      <a-table :key="tableResetKey" :data="pagedTableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" :expanded-keys="expandedKeys" size="small" :scroll="{ x: scrollX, y: '100%' }" :scrollbar="false" sticky-header :columns="visibleColumns" column-resizable ref="tableRef" :style="tableStyle" :row-class="getRowClass" @row-click="handleRowClick" @column-resize="handleColumnResize" @row-dblclick="handleRowDblClick" @expand="handleExpandChange">
        <!-- 自定义列头：每列标题旁加放大镜搜索图标（与授权治理一致） -->
        <template #th-deptName><TableColumnSearch title="部门名称" v-model="columnFilters['deptName']" /></template>
        <template #th-deptCode><TableColumnSearch title="部门编码" v-model="columnFilters['deptCode']" /></template>
        <template #th-orgName><TableColumnSearch title="所属机构" v-model="columnFilters['orgName']" /></template>
        <template #th-deptType><TableColumnSearch title="部门类型" v-model="columnFilters['deptType']" /></template>
        <template #th-funcType><TableColumnSearch title="职能分类" v-model="columnFilters['funcType']" /></template>
        <template #th-leader><TableColumnSearch title="负责人" v-model="columnFilters['leader']" /></template>
        <template #th-sort><TableColumnSearch title="排序" v-model="columnFilters['sort']" /></template>
        <template #th-status><TableColumnSearch title="状态" v-model="columnFilters['status']" /></template>
        <template #th-createTime><TableColumnSearch title="创建时间" v-model="columnFilters['createTime']" /></template>
        <template #th-phone><TableColumnSearch title="联系电话" v-model="columnFilters['phone']" /></template>
        <template #th-email><TableColumnSearch title="邮箱" v-model="columnFilters['email']" /></template>

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
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary" :disabled="permDisabled('system:dept:edit')" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-button size="mini" class="table-action-btn table-action-btn--danger" :disabled="permDisabled('system:dept:remove')" @click="confirmDelete(record.id)">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
            <a-dropdown v-if="hasPermission('system:dept:addChild')" trigger="click">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="handleAdd(record.id)">
                  <template #icon><icon-plus /></template>
                  新增子部门
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>

      <!-- 底部统计栏：左侧统计信息 + 右侧树控制 & 分页 -->
      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ totalDeptCount }}</b> 条</span>
          <a-divider direction="vertical" />
          <span>顶级 <b>{{ topLevelCount }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-normal">正常 <b>{{ activeCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">停用 <b>{{ disabledCount }}</b></span>
        </div>
        <div class="biz-table-footer__actions">
          <!-- 全部展开 / 全部收缩 -->
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
          <!-- 树形分页：按顶级节点分页，每页显示 pageSize 棵子树 -->
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

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="680" :height="580" :min-width="520" :min-height="420">
      <a-form :model="formData" ref="formRef" :rules="formReadonly ? undefined : formRules" layout="vertical" :disabled="formReadonly">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="基本信息">
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:dept:field:orgId')" :span="12">
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
              <a-col v-if="hasPermission('system:dept:field:deptType')" :span="12">
                <a-form-item field="deptType" label="部门类型">
                  <a-select v-model="formData.deptType" placeholder="请选择部门类型">
                    <a-option :value="1">事业部</a-option>
                    <a-option :value="2">中心</a-option>
                    <a-option :value="3">部门</a-option>
                    <a-option :value="4">小组</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:dept:field:funcType')" :span="12">
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
                <a-form-item field="status" label="部门状态">
                  <a-select v-model="formData.status" placeholder="请选择">
                    <a-option :value="1">正常</a-option>
                    <a-option :value="0">停用</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="contact" title="联系信息">
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:dept:field:leader')" :span="12">
                <a-form-item field="leader" label="负责人">
                  <a-input v-model="formData.leader" placeholder="请输入负责人" />
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:dept:field:phone')" :span="12">
                <a-form-item field="phone" label="联系电话">
                  <a-input v-model="formData.phone" placeholder="请输入联系电话" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:dept:field:email')" :span="12">
                <a-form-item field="email" label="邮箱">
                  <a-input v-model="formData.email" placeholder="请输入邮箱" />
                </a-form-item>
              </a-col>
            </a-row>
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
defineOptions({ name: 'SystemDept' });

import { ref, reactive, computed, onMounted, watch } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings, IconUp, IconDown, IconDelete, IconExpand, IconShrink } from '@arco-design/web-vue/es/icon';
import { fetchDeptList, createDept, updateDept, deleteDept, fetchOrgList, type DeptVO, type DeptForm, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';
import { useTreeColumnFilter, resetColumnFilters } from '../../../../composables/useColumnFilter';
import { useTableRowHighlight } from '../../../../composables/useTableRowHighlight';

const DEPT_TYPE_MAP: Record<number, string> = { 1: '事业部', 2: '中心', 3: '部门', 4: '小组' };
const DEPT_TYPE_COLOR: Record<number, string> = { 1: 'purple', 2: 'arcoblue', 3: 'green', 4: 'cyan' };
const deptTypeLabel = (t: number) => DEPT_TYPE_MAP[t] || '部门';
const deptTypeColor = (t: number) => DEPT_TYPE_COLOR[t] || 'gray';
const FUNC_TYPES = ['管理', '研发', '销售', '财务', '人事', '行政', '生产', '采购', '仓储'];

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

/**
 * 行点击选中高亮（使用通用 composable）
 * ──────────────────────────────────
 * handleRowClick + getRowClass 由 useTableRowHighlight 提供，
 * 选中行自动附加 .bml-row-active 类名，底色跟随主题色。
 * 全局样式定义在 business-system.css 第 24 节。
 */
const { handleRowClick, getRowClass } = useTableRowHighlight();

/**
 * 部门列默认配置（与授权治理列管理模式一致）：
 * - 部门名称：固定在左侧（fixed: 'left'），操作列固定在右侧（fixed: 'right'）
 * - 树形表格必须使用 :scrollbar="false" + table-layout: fixed + 精确 scroll.x 来保证列对齐
 * - 常用字段默认显示，扩展字段默认隐藏
 */
/** 列头搜索筛选条件 */
const columnFilters = reactive<Record<string, string>>({
  deptName: '', deptCode: '', orgName: '', deptType: '', funcType: '',
  leader: '', sort: '', status: '', createTime: '', phone: '', email: '',
});

const defaultColumns: BusinessTableColumn[] = [
  /* ── 核心标识（默认显示） ── */
  { key: 'deptName', title: '部门名称', dataIndex: 'deptName', width: 240, visible: true, fixed: 'left', ellipsis: true, sortable: true, titleSlotName: 'th-deptName' },
  { key: 'deptCode', title: '部门编码', dataIndex: 'deptCode', width: 140, visible: true, sortable: true, titleSlotName: 'th-deptCode' },
  { key: 'orgName',  title: '所属机构', dataIndex: 'orgName',  width: 150, visible: true, sortable: true, titleSlotName: 'th-orgName', permission: 'system:dept:field:orgId' },
  { key: 'deptType', title: '部门类型', slotName: 'deptType',  width: 120, visible: true, align: 'center', sortable: true, titleSlotName: 'th-deptType', permission: 'system:dept:field:deptType' },
  { key: 'funcType', title: '职能分类', slotName: 'funcType',  width: 120, visible: true, align: 'center', sortable: true, titleSlotName: 'th-funcType', permission: 'system:dept:field:funcType' },
  { key: 'leader',   title: '负责人',   dataIndex: 'leader',   width: 110, visible: true, sortable: true, titleSlotName: 'th-leader', permission: 'system:dept:field:leader' },
  { key: 'sort',     title: '排序',     dataIndex: 'sort',     width: 80,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-sort' },
  { key: 'status',   title: '状态',     slotName: 'status',    width: 90,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 180, visible: true, sortable: true, titleSlotName: 'th-createTime' },
  /* ── 扩展字段（默认隐藏） ── */
  { key: 'phone',  title: '联系电话', dataIndex: 'phone',  width: 130, visible: false, sortable: true, titleSlotName: 'th-phone', permission: 'system:dept:field:phone' },
  { key: 'email',  title: '邮箱',     dataIndex: 'email',  width: 180, visible: false, sortable: true, titleSlotName: 'th-email', permission: 'system:dept:field:email' },
  /* ── 操作列（锁定） ── */
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const {
  visibleColumns, columnSettingItems, dragState, tableResetKey, scrollX, tableStyle, tableRef,
  handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed,
  handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns,
} = useBusinessTableColumns('system-dept', defaultColumns);

const loading = ref(false);
const tableData = ref<DeptVO[]>([]);

/** 列头搜索值格式化器：将非文本字段转换为可搜索的展示文本 */
const columnFilterFormatters: Record<string, (val: any) => string> = {
  deptType: (val) => deptTypeLabel(val),
  status: (val) => val === 1 ? '正常' : '停用',
};
/** 列头搜索过滤后的树形数据 */
const { filteredData: filteredTreeData } = useTreeColumnFilter(tableData, columnFilters, columnFilterFormatters);

/** 递归收集树所有节点 ID，用于默认展开全部行 */
const collectAllKeys = (nodes: DeptVO[]): number[] =>
  nodes.reduce<number[]>((keys, n) => {
    keys.push(n.id);
    if (n.children?.length) keys.push(...collectAllKeys(n.children));
    return keys;
  }, []);
const expandedKeys = ref<number[]>([]);

/** 递归统计树形数据总条数 */
const countTreeNodes = (nodes: DeptVO[]): number =>
  nodes.reduce((sum, n) => sum + 1 + (n.children ? countTreeNodes(n.children) : 0), 0);
const totalDeptCount = computed(() => countTreeNodes(filteredTreeData.value));

/** 递归统计树形数据中指定状态的节点数量 */
const countByStatus = (nodes: DeptVO[], status: number): number =>
  nodes.reduce((sum, n) => sum + (n.status === status ? 1 : 0) + (n.children ? countByStatus(n.children, status) : 0), 0);

/** 顶级部门数量（树的根节点数） */
const topLevelCount = computed(() => filteredTreeData.value.length);
/** 正常状态部门数量（列头筛选后） */
const activeCount = computed(() => countByStatus(filteredTreeData.value, 1));
/** 停用状态部门数量（列头筛选后） */
const disabledCount = computed(() => countByStatus(filteredTreeData.value, 0));

/* ══════ 树形分页（按顶级节点分页，保持每棵子树完整） ══════ */
const pageSize = ref(10);
const currentPage = ref(1);

/**
 * 分页后的表格数据
 * <p>
 * 树形表格不能使用传统行级分页（会破坏父子层级），
 * 因此按顶级节点切片，每页显示 pageSize 棵完整子树。
 * pageSize 为 0 时表示显示全部。
 * </p>
 */
const pagedTableData = computed(() => {
  if (pageSize.value === 0) return filteredTreeData.value;
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredTreeData.value.slice(start, start + pageSize.value);
});

/** 全部展开 */
const expandAll = () => {
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};
/** 全部收缩 */
const collapseAll = () => {
  expandedKeys.value = [];
};

/** 切换每页条数时重置到第一页 */
const handlePageSizeChange = () => {
  currentPage.value = 1;
  // 重新展开当前页数据
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};
/** 翻页后自动展开当前页所有节点 */
const handlePageChange = () => {
  expandedKeys.value = collectAllKeys(pagedTableData.value);
};

/** 列头筛选条件变化时重置到第一页并展开所有节点 */
watch(columnFilters, () => {
  currentPage.value = 1;
  expandedKeys.value = collectAllKeys(pagedTableData.value);
}, { deep: true });

const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增部门');
const formRef = ref();

/** 表单只读模式：双击行无编辑权限时以查看模式打开 */
const formReadonly = ref(false);
const { hasPermission, permDisabled } = useButtonPermission();
const canEditDept = computed(() => hasPermission('system:dept:edit'));

const queryParams = reactive({
  deptName: '', deptCode: '', status: undefined as number | undefined,
  orgId: undefined as number | undefined, deptType: undefined as number | undefined,
  funcType: undefined as string | undefined, leader: ''
});

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
  try {
    const res = await fetchDeptList(queryParams) as any;
    tableData.value = res.data || [];
    // 重新加载数据后重置到第一页
    currentPage.value = 1;
    // 数据加载完成后默认展开当前页所有行
    expandedKeys.value = collectAllKeys(pagedTableData.value);
  }
  catch { tableData.value = []; }
  finally { loading.value = false; }
};

/**
 * 树形表格展开/收缩事件处理
 * <p>
 * 使用受控 :expanded-keys 时必须手动维护展开状态，
 * 否则用户点击展开/收缩按钮后不会生效。
 * </p>
 *
 * @param rowKey 当前操作行的 key（即部门 id）
 */
const handleExpandChange = (rowKey: number) => {
  const idx = expandedKeys.value.indexOf(rowKey);
  if (idx >= 0) {
    expandedKeys.value.splice(idx, 1);
  } else {
    expandedKeys.value.push(rowKey);
  }
};

const handleSearch = () => { loadData(); };
const handleReset = () => {
  queryParams.deptName = ''; queryParams.deptCode = ''; queryParams.status = undefined;
  queryParams.orgId = undefined; queryParams.deptType = undefined;
  queryParams.funcType = undefined; queryParams.leader = '';
  resetColumnFilters(columnFilters);
  loadData();
};

const handleOrgChange = async (orgId: number | undefined) => {
  try { const res = await fetchDeptList({ orgId }) as any; deptTreeOptionsData.value = res.data || []; }
  catch { deptTreeOptionsData.value = []; }
  formData.parentId = undefined;
};

const handleAdd = (parentId?: number) => {
  formReadonly.value = false;
  dialogTitle.value = '新增部门';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

const handleEdit = (row: DeptVO) => {
  formReadonly.value = false;
  dialogTitle.value = '编辑部门';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, orgId: row.orgId || undefined,
    deptName: row.deptName, deptCode: row.deptCode || '',
    deptType: row.deptType || 3, funcType: row.funcType || undefined,
    sort: row.sort, leader: row.leader, phone: row.phone, email: row.email, status: row.status
  });
  dialogVisible.value = true;
};

/** 双击行：有编辑权限则编辑，否则查看 */
const handleRowDblClick = (record: DeptVO) => {
  if (canEditDept.value) {
    handleEdit(record);
  } else {
    formReadonly.value = true;
    dialogTitle.value = '查看部门';
    Object.assign(formData, {
      id: record.id, parentId: record.parentId, orgId: record.orgId || undefined,
      deptName: record.deptName, deptCode: record.deptCode || '',
      deptType: record.deptType || 3, funcType: record.funcType || undefined,
      sort: record.sort, leader: record.leader, phone: record.phone, email: record.email, status: record.status
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
.form-tabs :deep(.arco-tabs-content) { padding-top: 12px; }

/**
 * 树形缩进内容溢出裁剪
 * table-layout: fixed 下列宽严格固定，树形展开图标 + 缩进 + 文本可能超出列宽，
 * 统一在单元格内容层裁剪并显示省略号，保持视觉整洁。
 */
:deep(.arco-table-td .arco-table-cell) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
