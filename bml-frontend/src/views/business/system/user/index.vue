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
        <a-form-item field="username" label="用户名">
          <a-input v-model="queryParams.username" placeholder="请输入用户名" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="phone" label="手机号">
          <a-input v-model="queryParams.phone" placeholder="请输入手机号" allow-clear @press-enter="handleSearch" />
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
          新增用户
        </a-button>
      </template>
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" stripe size="small" :scroll="{ y: '100%' }" :scrollbar="true" sticky-header>
        <template #columns>
          <a-table-column title="用户名" data-index="username" :width="120" />
          <a-table-column title="昵称" data-index="nickname" :width="110" />
          <a-table-column title="工号" data-index="employeeNo" :width="100">
            <template #cell="{ record }">{{ record.employeeNo || '—' }}</template>
          </a-table-column>
          <a-table-column title="所属机构" data-index="orgName" :width="140" />
          <a-table-column title="部门" data-index="deptName" :width="120" />
          <a-table-column title="岗位" data-index="postName" :width="110" />
          <a-table-column title="手机号" data-index="phone" :width="130" />
          <a-table-column title="状态" data-index="status" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="入职日期" data-index="entryDate" :width="110" />
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="操作" :width="200" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space>
                <a-button type="text" size="small" @click="handleEdit(record)"><template #icon><icon-edit /></template>编辑</a-button>
                <a-popconfirm content="确认删除该用户吗？" @ok="handleDelete(record.id)">
                  <a-button type="text" size="small" status="danger"><template #icon><icon-delete /></template>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </GovernanceListStage>

    <!-- 新增/编辑弹窗 -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="740" :height="620" :min-width="560" :min-height="440">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <!-- ── 账号信息 ── -->
          <a-tab-pane key="basic" title="账号信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="username" label="用户名">
                  <a-input v-model="formData.username" placeholder="请输入用户名" :disabled="!!formData.id" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="nickname" label="昵称">
                  <a-input v-model="formData.nickname" placeholder="请输入昵称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16" v-if="!formData.id">
              <a-col :span="12">
                <a-form-item field="password" label="密码">
                  <a-input-password v-model="formData.password" placeholder="请输入密码" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="phone" label="手机号">
                  <a-input v-model="formData.phone" placeholder="请输入手机号" />
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
                <a-form-item field="gender" label="性别">
                  <a-select v-model="formData.gender" placeholder="请选择">
                    <a-option :value="0">未知</a-option>
                    <a-option :value="1">男</a-option>
                    <a-option :value="2">女</a-option>
                  </a-select>
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
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="roleIds" label="角色">
                  <a-select v-model="formData.roleIds" placeholder="请选择角色" multiple allow-clear>
                    <a-option v-for="role in roleOptions" :key="role.id" :value="role.id">{{ role.roleName }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="remark" label="备注">
                  <a-input v-model="formData.remark" placeholder="请输入备注" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>

          <!-- ── 组织信息 ── -->
          <a-tab-pane key="org" title="组织与岗位">
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
                <a-form-item field="deptId" label="所属部门">
                  <a-tree-select
                    v-model="formData.deptId"
                    :data="deptTreeData"
                    :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                    placeholder="请选择所属部门"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="postId" label="岗位">
                  <a-select v-model="formData.postId" placeholder="请选择岗位" allow-clear>
                    <a-option v-for="p in postOptions" :key="p.id" :value="p.id">{{ p.postName }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="employeeNo" label="工号">
                  <a-input v-model="formData.employeeNo" placeholder="请输入工号" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="entryDate" label="入职日期">
                  <a-date-picker v-model="formData.entryDate" placeholder="请选择入职日期" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
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
/**
 * 用户管理页面
 *
 * 重要说明：
 *   defineOptions({ name: 'SystemUser' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'SystemUser' });

import { ref, reactive, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import {
  fetchUserList, createUser, updateUser, deleteUser,
  fetchRoleList, fetchOrgList, fetchDeptList, fetchPostList,
  type UserVO, type UserForm, type RoleVO, type OrgVO, type DeptVO, type PostVO
} from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';

/* ════════════════════════════════════════════════════════════
   响应式状态
   ════════════════════════════════════════════════════════════ */
const loading = ref(false);
const tableData = ref<UserVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const deptTreeData = ref<DeptVO[]>([]);
const postOptions = ref<PostVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增用户');
const formRef = ref();

const queryParams = reactive({ username: '', phone: '', status: undefined as number | undefined });

const defaultForm = (): UserForm => ({
  id: undefined, username: '', nickname: '', password: '', phone: '', email: '',
  gender: 0, status: 1, orgId: undefined, deptId: undefined, postId: undefined,
  employeeNo: '', entryDate: '', roleIds: [], remark: ''
});
const formData = reactive<UserForm>(defaultForm());

const formRules = {
  username: [{ required: true, message: '请输入用户名' }],
  nickname: [{ required: true, message: '请输入昵称' }],
  password: [{ required: true, message: '请输入密码' }]
};

/* ════════════════════════════════════════════════════════════
   数据加载
   ════════════════════════════════════════════════════════════ */
const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchUserList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const loadRoles = async () => {
  try { const res = await fetchRoleList() as any; roleOptions.value = res.data || []; }
  catch { roleOptions.value = []; }
};

const loadOrgTree = async () => {
  try { const res = await fetchOrgList({}) as any; orgTreeData.value = res.data || []; }
  catch { orgTreeData.value = []; }
};

const loadDeptTree = async () => {
  try { const res = await fetchDeptList({}) as any; deptTreeData.value = res.data || []; }
  catch { deptTreeData.value = []; }
};

const loadPosts = async () => {
  try { const res = await fetchPostList({}) as any; postOptions.value = res.data || []; }
  catch { postOptions.value = []; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => { queryParams.username = ''; queryParams.phone = ''; queryParams.status = undefined; loadData(); };

const handleAdd = () => {
  dialogTitle.value = '新增用户';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

const handleEdit = (row: UserVO) => {
  dialogTitle.value = '编辑用户';
  Object.assign(formData, {
    id: row.id, username: row.username, nickname: row.nickname, password: '',
    phone: row.phone, email: row.email, gender: row.gender, status: row.status,
    orgId: row.orgId || undefined, deptId: row.deptId || undefined,
    postId: row.postId || undefined, employeeNo: row.employeeNo || '',
    entryDate: row.entryDate || '', roleIds: row.roleIds || [], remark: row.remark
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
      await updateUser(formData);
      Message.success('修改成功');
    } else {
      await createUser(formData);
      Message.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try {
    await deleteUser(id);
    Message.success('删除成功');
    loadData();
  } catch { /* ignore */ }
};

onMounted(() => { loadData(); loadRoles(); loadOrgTree(); loadDeptTree(); loadPosts(); });
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
.form-tabs :deep(.arco-tabs-content) {
  padding-top: 12px;
}
</style>
