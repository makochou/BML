<template>
  <div class="page-wrapper">
    <!-- ════════════════════════════════════════════════
         查询面板
         ════════════════════════════════════════════════ -->
    <GovernanceCompactQueryPanel density="ultra" theme="aurora">
      <template #footerActions>
        <a-button @click="handleReset">重置条件</a-button>
        <a-button type="primary" @click="handleSearch">查询</a-button>
      </template>
      <a-form :model="queryParams" layout="inline" class="query-form">
        <a-form-item field="postName" label="岗位名称">
          <a-input v-model="queryParams.postName" placeholder="请输入岗位名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="postCode" label="岗位编码">
          <a-input v-model="queryParams.postCode" placeholder="请输入岗位编码" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="postCategory" label="岗位类别">
          <a-select v-model="queryParams.postCategory" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
            <a-option v-for="cat in POST_CATEGORIES" :key="cat" :value="cat">{{ cat }}</a-option>
          </a-select>
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select v-model="queryParams.status" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>
      </a-form>
    </GovernanceCompactQueryPanel>

    <!-- ════════════════════════════════════════════════
         列表舞台
         ════════════════════════════════════════════════ -->
    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增岗位
        </a-button>
      </template>
      <a-table
        :data="tableData"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ y: '100%' }"
        :scrollbar="true"
        sticky-header
      >
        <template #columns>
          <a-table-column title="岗位编码" data-index="postCode" :width="120" />
          <a-table-column title="岗位名称" data-index="postName" :width="160" />
          <a-table-column title="所属机构" data-index="orgName" :width="160" />
          <a-table-column title="岗位类别" data-index="postCategory" :width="110" align="center">
            <template #cell="{ record }">
              <a-tag v-if="record.postCategory" size="small" :color="categoryColor(record.postCategory)">{{ record.postCategory }}</a-tag>
              <span v-else class="text-gray">—</span>
            </template>
          </a-table-column>
          <a-table-column title="岗位级别" data-index="postLevel" :width="100" align="center">
            <template #cell="{ record }">
              <span>{{ record.postLevel || '—' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="排序" data-index="sort" :width="70" align="center" />
          <a-table-column title="状态" data-index="status" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="备注" data-index="remark" ellipsis />
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="200" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space>
                <a-button type="text" size="small" @click="handleEdit(record)"><template #icon><icon-edit /></template>编辑</a-button>
                <a-popconfirm content="确认删除该岗位吗？" @ok="handleDelete(record.id)">
                  <a-button type="text" size="small" status="danger"><template #icon><icon-delete /></template>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </GovernanceListStage>

    <!-- 新增/编辑弹窗 -->
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
              <a-tree-select
                v-model="formData.orgId"
                :data="orgTreeData"
                :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                placeholder="全局岗位（不限机构）"
                allow-clear
              />
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
import { ref, reactive, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchPostList, createPost, updatePost, deletePost, fetchOrgList, type PostVO, type PostForm, type PostQuery, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';

/* ════════════════════════════════════════════════════════════
   常量
   ════════════════════════════════════════════════════════════ */
const POST_CATEGORIES = ['管理类', '技术类', '行政类', '财务类', '销售类', '生产类'];
const CATEGORY_COLOR: Record<string, string> = {
  '管理类': 'purple', '技术类': 'arcoblue', '行政类': 'green',
  '财务类': 'orangered', '销售类': 'cyan', '生产类': 'gold'
};
const categoryColor = (c: string) => CATEGORY_COLOR[c] || 'gray';

/* ════════════════════════════════════════════════════════════
   响应式状态
   ════════════════════════════════════════════════════════════ */
const loading = ref(false);
const tableData = ref<PostVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增岗位');
const formRef = ref();

/** 查询参数 */
const queryParams = reactive<PostQuery>({ postName: '', postCode: '', postCategory: undefined, status: undefined });

/** 表单默认值 */
const defaultForm = (): PostForm => ({
  id: undefined, postCode: '', postName: '', orgId: undefined,
  postCategory: undefined, postLevel: '', sort: 0, status: 1, remark: ''
});
const formData = reactive<PostForm>(defaultForm());

/** 表单校验规则 */
const formRules = {
  postCode: [{ required: true, message: '请输入岗位编码' }],
  postName: [{ required: true, message: '请输入岗位名称' }]
};

/* ════════════════════════════════════════════════════════════
   数据加载与操作
   ════════════════════════════════════════════════════════════ */

/** 加载机构树（用于下拉选择） */
const loadOrgTree = async () => {
  try {
    const res = await fetchOrgList({}) as any;
    orgTreeData.value = res.data || [];
  } catch { orgTreeData.value = []; }
};

/** 加载岗位列表 */
const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchPostList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

/** 查询 */
const handleSearch = () => { loadData(); };

/** 重置查询条件 */
const handleReset = () => {
  queryParams.postName = '';
  queryParams.postCode = '';
  queryParams.postCategory = undefined;
  queryParams.status = undefined;
  loadData();
};

/** 新增 */
const handleAdd = () => {
  dialogTitle.value = '新增岗位';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

/** 编辑 */
const handleEdit = (row: PostVO) => {
  dialogTitle.value = '编辑岗位';
  Object.assign(formData, {
    id: row.id, postCode: row.postCode, postName: row.postName,
    orgId: row.orgId || undefined, postCategory: row.postCategory || undefined,
    postLevel: row.postLevel || '', sort: row.sort, status: row.status, remark: row.remark
  });
  dialogVisible.value = true;
};

/** 提交表单 */
const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) {
      await updatePost(formData);
      Message.success('修改成功');
    } else {
      await createPost(formData);
      Message.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await deletePost(id);
    Message.success('删除成功');
    loadData();
  } catch { /* ignore */ }
};

onMounted(() => { loadOrgTree(); loadData(); });
</script>

<style scoped>
.page-wrapper {
  padding: 16px 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
}
.query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}
.query-form :deep(.arco-form-item) {
  margin-bottom: 4px;
}
.query-form :deep(.arco-form-item-label-col > label) {
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
}
.page-wrapper :deep(.governance-list-stage) {
  flex: 1;
  min-height: 0;
  margin-top: 10px;
}
.text-gray { color: #c0c0c0; }
</style>
