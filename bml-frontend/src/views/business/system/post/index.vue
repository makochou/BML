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
        <a-form-item field="postName" label="岗位名称">
          <a-input v-model="queryParams.postName" placeholder="请输入岗位名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="postCode" label="岗位编码">
          <a-input v-model="queryParams.postCode" placeholder="请输入岗位编码" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select v-model="queryParams.status" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>

        <!-- 次要字段（展开时显示） -->
        <transition name="query-expand">
          <div v-if="queryExpanded" class="biz-query-form-extra">
            <a-form-item field="postCategory" label="岗位类别">
              <a-select v-model="queryParams.postCategory" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
                <a-option v-for="cat in POST_CATEGORIES" :key="cat" :value="cat">{{ cat }}</a-option>
              </a-select>
            </a-form-item>
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select v-model="queryParams.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="全部机构" allow-clear @change="handleSearch" />
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增岗位
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
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" size="small" :scroll="{ y: '100%' }" :scrollbar="true" sticky-header :columns="visibleColumns" column-resizable @column-resize="handleColumnResize">
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
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-dropdown trigger="click">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption class="is-danger" @click="confirmDelete(record.id)">删除岗位</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
      <a-pagination v-model:current="pagination.current" v-model:page-size="pagination.pageSize" :total="pagination.total" show-total show-page-size @change="loadData" @page-size-change="(size) => { pagination.pageSize = size; pagination.current = 1; loadData(); }" style="margin-top: 12px; text-align: right;" />
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="600" :height="520" :min-width="480" :min-height="380">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
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
          <a-col :span="12">
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select v-model="formData.orgId" :data="orgTreeData" :field-names="{ key: 'id', title: 'orgName', children: 'children' }" placeholder="全局岗位（不限机构）" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="postCategory" label="岗位类别">
              <a-select v-model="formData.postCategory" placeholder="请选择岗位类别" allow-clear>
                <a-option v-for="cat in POST_CATEGORIES" :key="cat" :value="cat">{{ cat }}</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
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
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item field="remark" label="备注">
              <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
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
defineOptions({ name: 'SystemPost' });

import { ref, reactive, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings, IconUp, IconDown } from '@arco-design/web-vue/es/icon';
import { fetchPostList, createPost, updatePost, deletePost, fetchOrgList, fetchPostPage, type PostVO, type PostForm, type PostQuery, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';

const POST_CATEGORIES = ['管理类', '技术类', '行政类', '财务类', '销售类', '生产类'];
const CATEGORY_COLOR: Record<string, string> = {
  '管理类': 'purple', '技术类': 'arcoblue', '行政类': 'green',
  '财务类': 'orangered', '销售类': 'cyan', '生产类': 'gold'
};
const categoryColor = (c: string) => CATEGORY_COLOR[c] || 'gray';

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

const defaultColumns: BusinessTableColumn[] = [
  { key: 'postCode', title: '岗位编码', dataIndex: 'postCode', width: 120, visible: true },
  { key: 'postName', title: '岗位名称', dataIndex: 'postName', width: 160, visible: true },
  { key: 'orgName', title: '所属机构', dataIndex: 'orgName', width: 160, visible: true },
  { key: 'postCategory', title: '岗位类别', slotName: 'postCategory', width: 110, visible: true, align: 'center' },
  { key: 'postLevel', title: '岗位级别', slotName: 'postLevel', width: 100, visible: true, align: 'center' },
  { key: 'sort', title: '排序', dataIndex: 'sort', width: 70, visible: true, align: 'center' },
  { key: 'status', title: '状态', slotName: 'status', width: 80, visible: true, align: 'center' },
  { key: 'remark', title: '备注', dataIndex: 'remark', width: 150, visible: true, ellipsis: true },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 170, visible: true },
  { key: 'actions', title: '操作', slotName: 'actions', width: 140, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const { visibleColumns, columnSettingItems, dragState, handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed, handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns } = useBusinessTableColumns('system-post', defaultColumns);

const loading = ref(false);
const tableData = ref<PostVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增岗位');
const formRef = ref();

const queryParams = reactive<PostQuery>({ postName: '', postCode: '', postCategory: undefined, status: undefined, orgId: undefined });
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });

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
  queryParams.status = undefined; queryParams.orgId = undefined;
  pagination.current = 1; loadData();
};

const handleAdd = () => {
  dialogTitle.value = '新增岗位';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

const handleEdit = (row: PostVO) => {
  dialogTitle.value = '编辑岗位';
  Object.assign(formData, {
    id: row.id, postCode: row.postCode, postName: row.postName,
    orgId: row.orgId || undefined, postCategory: row.postCategory || undefined,
    postLevel: row.postLevel || '', sort: row.sort, status: row.status, remark: row.remark
  });
  dialogVisible.value = true;
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
.text-gray { color: #c0c0c0; }
</style>
