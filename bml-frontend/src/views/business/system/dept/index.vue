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
        <a-form-item field="deptName" label="部门名称">
          <a-input v-model="queryParams.deptName" placeholder="请输入部门名称" allow-clear @press-enter="handleSearch" />
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
        <a-button type="primary" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增部门
        </a-button>
      </template>
      <a-table
        :data="tableData"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="true"
        size="small"
        :scroll="{ y: '100%' }"
        :scrollbar="true"
        sticky-header
      >
        <template #columns>
          <a-table-column title="部门名称" data-index="deptName" :width="200" />
          <a-table-column title="部门编码" data-index="deptCode" :width="120" />
          <a-table-column title="所属机构" data-index="orgName" :width="150" />
          <a-table-column title="部门类型" data-index="deptType" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag size="small" :color="deptTypeColor(record.deptType)">{{ deptTypeLabel(record.deptType) }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="职能分类" data-index="funcType" :width="100" align="center">
            <template #cell="{ record }">
              <span>{{ record.funcType || '—' }}</span>
            </template>
          </a-table-column>
          <a-table-column title="负责人" data-index="leader" :width="100" />
          <a-table-column title="联系电话" data-index="phone" :width="130" />
          <a-table-column title="排序" data-index="sort" :width="70" align="center" />
          <a-table-column title="状态" data-index="status" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="240" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space>
                <a-button type="text" size="small" @click="handleAdd(record.id)">
                  <template #icon><icon-plus /></template>新增
                </a-button>
                <a-button type="text" size="small" @click="handleEdit(record)"><template #icon><icon-edit /></template>编辑</a-button>
                <a-popconfirm content="确认删除该部门吗？" @ok="handleDelete(record.id)">
                  <a-button type="text" size="small" status="danger"><template #icon><icon-delete /></template>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </GovernanceListStage>

    <!-- 新增/编辑弹窗 -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="680" :height="580" :min-width="520" :min-height="420">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select
                v-model="formData.orgId"
                :data="orgTreeData"
                :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                placeholder="请选择所属机构"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="parentId" label="上级部门">
              <a-tree-select
                v-model="formData.parentId"
                :data="deptTreeOptions"
                :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                placeholder="请选择上级部门"
                allow-clear
              />
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
/**
 * 部门管理页面
 *
 * 重要说明：
 *   defineOptions({ name: 'SystemDept' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'SystemDept' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchDeptList, createDept, updateDept, deleteDept, fetchOrgList, type DeptVO, type DeptForm, type OrgVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';

/* ════════════════════════════════════════════════════════════
   常量
   ════════════════════════════════════════════════════════════ */
const DEPT_TYPE_MAP: Record<number, string> = { 1: '事业部', 2: '中心', 3: '部门', 4: '小组' };
const DEPT_TYPE_COLOR: Record<number, string> = { 1: 'purple', 2: 'arcoblue', 3: 'green', 4: 'cyan' };
const deptTypeLabel = (t: number) => DEPT_TYPE_MAP[t] || '部门';
const deptTypeColor = (t: number) => DEPT_TYPE_COLOR[t] || 'gray';
const FUNC_TYPES = ['管理', '研发', '销售', '财务', '人事', '行政', '生产', '采购', '仓储'];

/* ════════════════════════════════════════════════════════════
   响应式状态
   ════════════════════════════════════════════════════════════ */
const loading = ref(false);
const tableData = ref<DeptVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增部门');
const formRef = ref();

const queryParams = reactive({ deptName: '', status: undefined as number | undefined });

const defaultForm = (): DeptForm => ({
  id: undefined, parentId: 0, orgId: undefined, deptName: '', deptCode: '',
  deptType: 3, funcType: undefined, sort: 0, leader: '', phone: '', email: '', status: 1
});
const formData = reactive<DeptForm>(defaultForm());

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称' }]
};

const deptTreeOptions = computed(() => {
  const root = { id: 0, parentId: -1, deptName: '根部门', deptCode: '', orgId: 0, orgName: '',
    deptType: 3, funcType: '', sort: 0, leader: '', phone: '', email: '', status: 1,
    createTime: '', children: tableData.value } as DeptVO;
  return [root];
});

/* ════════════════════════════════════════════════════════════
   数据加载与操作
   ════════════════════════════════════════════════════════════ */

/** 加载机构树 */
const loadOrgTree = async () => {
  try {
    const res = await fetchOrgList({}) as any;
    orgTreeData.value = res.data || [];
  } catch { orgTreeData.value = []; }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchDeptList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => { queryParams.deptName = ''; queryParams.status = undefined; loadData(); };

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
    if (formData.id) {
      await updateDept(formData);
      Message.success('修改成功');
    } else {
      await createDept(formData);
      Message.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try {
    await deleteDept(id);
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
</style>
