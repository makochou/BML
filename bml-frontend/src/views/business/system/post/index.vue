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
          <a-form-item field="postName" label="岗位名称">
            <a-input v-model="queryParams.postName" placeholder="请输入岗位名称" allow-clear @press-enter="handleSearch" />
          </a-form-item>
          <a-form-item field="postCode" label="岗位编码">
            <a-input v-model="queryParams.postCode" placeholder="请输入岗位编码" allow-clear @press-enter="handleSearch" />
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
            <a-form-item field="postCategory" label="岗位类别">
              <a-select v-model="queryParams.postCategory" placeholder="全部" allow-clear @change="handleSearch">
                <a-option v-for="cat in POST_CATEGORIES" :key="cat" :value="cat">{{ cat }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select v-model="queryParams.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="全部机构" allow-clear @change="handleSearch" />
            </a-form-item>
            <a-form-item field="postLevel" label="岗位级别">
              <a-input v-model="queryParams.postLevel" placeholder="如 P5、M3" allow-clear @press-enter="handleSearch" />
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button v-if="hasPermission('system:post:add')" type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增岗位
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
      <a-table :key="tableResetKey" :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" size="small" :scroll="{ x: scrollX, y: '100%' }" :scrollbar="false" sticky-header :columns="visibleColumns" column-resizable @column-resize="handleColumnResize" @row-dblclick="handleRowDblClick">
        <!-- 自定义列头：每列标题旁加放大镜搜索图标（与授权治理一致） -->
        <template #th-postName><TableColumnSearch title="岗位名称" v-model="columnFilters['postName']" /></template>
        <template #th-postCode><TableColumnSearch title="岗位编码" v-model="columnFilters['postCode']" /></template>
        <template #th-orgName><TableColumnSearch title="所属机构" v-model="columnFilters['orgName']" /></template>
        <template #th-postCategory><TableColumnSearch title="岗位类别" v-model="columnFilters['postCategory']" /></template>
        <template #th-postLevel><TableColumnSearch title="岗位级别" v-model="columnFilters['postLevel']" /></template>
        <template #th-sort><TableColumnSearch title="排序" v-model="columnFilters['sort']" /></template>
        <template #th-status><TableColumnSearch title="状态" v-model="columnFilters['status']" /></template>
        <template #th-createTime><TableColumnSearch title="创建时间" v-model="columnFilters['createTime']" /></template>
        <template #th-remark><TableColumnSearch title="备注" v-model="columnFilters['remark']" /></template>

        <template #postCategory="{ record }">
          <a-tag v-if="record.postCategory" size="small" :color="categoryColor(record.postCategory)">{{ record.postCategory }}</a-tag>
          <span v-else class="text-gray">无</span>
        </template>
        <template #postLevel="{ record }">
          <span>{{ record.postLevel || '无' }}</span>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button v-if="hasPermission('system:post:edit')" type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-button v-if="hasPermission('system:post:remove')" size="mini" class="table-action-btn table-action-btn--danger" @click="confirmDelete(record.id)">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
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

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="600" :height="520" :min-width="480" :min-height="380">
      <a-form :model="formData" ref="formRef" :rules="formReadonly ? undefined : formRules" layout="vertical" :disabled="formReadonly">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="基本信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="postCode" label="岗位编码">
                  <a-input v-model="formData.postCode" placeholder="请输入岗位编码" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="postName" label="岗位名称">
                  <a-input v-model="formData.postName" placeholder="请输入岗位名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:post:field:orgId')" :span="12">
                <a-form-item field="orgId" label="所属机构">
                  <a-tree-select v-model="formData.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="全局岗位（不限机构）" allow-clear />
                </a-form-item>
              </a-col>
              <a-col v-if="hasPermission('system:post:field:postCategory')" :span="12">
                <a-form-item field="postCategory" label="岗位类别">
                  <a-select v-model="formData.postCategory" placeholder="请选择岗位类别" allow-clear>
                    <a-option v-for="cat in POST_CATEGORIES" :key="cat" :value="cat">{{ cat }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:post:field:postLevel')" :span="12">
                <a-form-item field="postLevel" label="岗位级别">
                  <a-input v-model="formData.postLevel" placeholder="如 P5、M3" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="sort" label="显示排序">
                  <a-input-number v-model="formData.sort" :min="0" placeholder="排序" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="status" label="岗位状态">
                  <a-select v-model="formData.status" placeholder="请选择">
                    <a-option :value="1">正常</a-option>
                    <a-option :value="0">停用</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="extra" title="其他信息">
            <a-row :gutter="16">
              <a-col v-if="hasPermission('system:post:field:remark')" :span="24">
                <a-form-item field="remark" label="备注">
                  <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
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
defineOptions({ name: 'SystemPost' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconSettings, IconUp, IconDown, IconDelete } from '@arco-design/web-vue/es/icon';
import { createPost, updatePost, deletePost, fetchOrgList, fetchPostPage, type PostVO, type PostForm, type PostQuery, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import TableColumnSearch from '../../../../components/common/TableColumnSearch.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';
import { useButtonPermission } from '../../../../composables/useButtonPermission';

const POST_CATEGORIES = ['管理类', '技术类', '行政类', '财务类', '销售类', '生产类'];
const CATEGORY_COLOR: Record<string, string> = {
  '管理类': 'purple', '技术类': 'arcoblue', '行政类': 'green',
  '财务类': 'orangered', '销售类': 'cyan', '生产类': 'gold'
};
const categoryColor = (c: string) => CATEGORY_COLOR[c] || 'gray';

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

/**
 * 岗位列默认配置（与授权治理列管理模式一致）：
 * - 岗位名称：默认固定在左侧（fixed: 'left'）
 * - 常用字段默认显示，扩展字段默认隐藏
 */
/** 列头搜索筛选条件 */
const columnFilters = reactive<Record<string, string>>({
  postName: '', postCode: '', orgName: '', postCategory: '', postLevel: '',
  sort: '', status: '', createTime: '', remark: '',
});

const defaultColumns: BusinessTableColumn[] = [
  /* ── 核心标识（默认显示） ── */
  { key: 'postName',     title: '岗位名称', dataIndex: 'postName',     width: 160, visible: true, fixed: 'left', sortable: true, titleSlotName: 'th-postName' },
  { key: 'postCode',     title: '岗位编码', dataIndex: 'postCode',     width: 120, visible: true, sortable: true, titleSlotName: 'th-postCode' },
  { key: 'orgName',      title: '所属机构', dataIndex: 'orgName',      width: 160, visible: true, sortable: true, titleSlotName: 'th-orgName', permission: 'system:post:field:orgId' },
  { key: 'postCategory', title: '岗位类别', slotName: 'postCategory',  width: 110, visible: true, align: 'center', sortable: true, titleSlotName: 'th-postCategory', permission: 'system:post:field:postCategory' },
  { key: 'postLevel',    title: '岗位级别', slotName: 'postLevel',     width: 100, visible: true, align: 'center', sortable: true, titleSlotName: 'th-postLevel', permission: 'system:post:field:postLevel' },
  { key: 'sort',         title: '排序',     dataIndex: 'sort',         width: 70,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-sort' },
  { key: 'status',       title: '状态',     slotName: 'status',        width: 80,  visible: true, align: 'center', sortable: true, titleSlotName: 'th-status' },
  { key: 'createTime',   title: '创建时间', dataIndex: 'createTime',   width: 170, visible: true, sortable: true, titleSlotName: 'th-createTime' },
  /* ── 扩展字段（默认隐藏） ── */
  { key: 'remark', title: '备注', dataIndex: 'remark', width: 200, visible: true, ellipsis: true, sortable: true, titleSlotName: 'th-remark', permission: 'system:post:field:remark' },
  /* ── 操作列（锁定） ── */
  { key: 'actions', title: '操作', slotName: 'actions', width: 170, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, tableResetKey, scrollX, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-post', defaultColumns);

/** 表格列宽 CSS 绑定值（含单位，供 v-bind 使用） */
const tableScrollWidth = computed(() => scrollX.value + 'px');

const loading = ref(false);
const tableData = ref<PostVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增岗位');
const formRef = ref();

/** 表单只读模式 */
const formReadonly = ref(false);
const { hasPermission } = useButtonPermission();
const canEditPost = computed(() => hasPermission('system:post:edit'));

const queryParams = reactive<PostQuery & { postLevel?: string }>({ postName: '', postCode: '', postCategory: undefined, status: undefined, orgId: undefined, postLevel: '' });
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });

/** 当前页正常状态岗位数量 */
const activeCount = computed(() => tableData.value.filter(r => r.status === 1).length);
/** 当前页停用状态岗位数量 */
const disabledCount = computed(() => tableData.value.filter(r => r.status === 0).length);

const defaultForm = (): PostForm => ({
  id: undefined, postCode: '', postName: '', orgId: undefined,
  postCategory: undefined, postLevel: '', sort: 0, status: 1, remark: ''
});
const formData = reactive<PostForm>(defaultForm());

const formRules = {
  postCode: [{ required: true, message: '请输入岗位编码' }],
  postName: [{ required: true, message: '请输入岗位名称' }]
};

const loadOrgTree = async () => {
  try { const res = await fetchOrgList({}) as any; orgTreeData.value = res.data || []; }
  catch { orgTreeData.value = []; }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchPostPage({ ...queryParams, pageNum: pagination.current, pageSize: pagination.pageSize }) as any;
    pagination.total = res.data?.total ?? 0;
    tableData.value = res.data?.records ?? [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { pagination.current = 1; loadData(); };
const handleReset = () => {
  queryParams.postName = ''; queryParams.postCode = ''; queryParams.postCategory = undefined;
  queryParams.status = undefined; queryParams.orgId = undefined; queryParams.postLevel = '';
  pagination.current = 1; loadData();
};
const handlePageChange = (page: number) => { pagination.current = page; loadData(); };
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; loadData(); };

const handleAdd = () => {
  formReadonly.value = false;
  dialogTitle.value = '新增岗位';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

const handleEdit = (row: PostVO) => {
  formReadonly.value = false;
  dialogTitle.value = '编辑岗位';
  Object.assign(formData, {
    id: row.id, postCode: row.postCode, postName: row.postName,
    orgId: row.orgId || undefined, postCategory: row.postCategory || undefined,
    postLevel: row.postLevel || '', sort: row.sort, status: row.status, remark: row.remark
  });
  dialogVisible.value = true;
};

/** 双击行：有编辑权限则编辑，否则查看 */
const handleRowDblClick = (record: PostVO) => {
  if (canEditPost.value) {
    handleEdit(record);
  } else {
    formReadonly.value = true;
    dialogTitle.value = '查看岗位';
    Object.assign(formData, {
      id: record.id, postCode: record.postCode, postName: record.postName,
      orgId: record.orgId || undefined, postCategory: record.postCategory || undefined,
      postLevel: record.postLevel || '', sort: record.sort, status: record.status, remark: record.remark
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
    if (formData.id) { await updatePost(formData); Message.success('修改成功'); }
    else { await createPost(formData); Message.success('新增成功'); }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try { await deletePost(id); Message.success('删除成功'); loadData(); }
  catch { /* ignore */ }
};

const confirmDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确认删除该岗位吗？',
    onOk: () => handleDelete(id),
  });
};

onMounted(() => { loadOrgTree(); loadData(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) { padding-top: 12px; }
.text-gray { color: #c0c0c0; }

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
</style>
