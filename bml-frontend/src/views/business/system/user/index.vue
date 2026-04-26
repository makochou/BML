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
            <a-option :value="2">锁定</a-option>
          </a-select>
        </a-form-item>

        <!-- 次要字段（展开时显示） -->
        <transition name="query-expand">
          <div v-if="queryExpanded" class="biz-query-form-extra">
            <a-form-item field="orgId" label="所属机构">
              <a-tree-select v-model="queryParams.orgId" :data="orgTreeData"
                :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                placeholder="全部机构" allow-clear style="width: 180px;" @change="handleSearch" />
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd">
          <template #icon><icon-plus /></template>
          新增用户
        </a-button>
        <a-popover trigger="click" position="br" :content-style="{ padding: 0 }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting :items="columnSettingItems" :drag-state="dragState"
              @toggle-visible="toggleColumnVisible" @move="moveColumn" @toggle-fixed="toggleColumnFixed"
              @drag-start="handleDragStart" @drag-over="handleDragOver" @drop="handleDrop"
              @drag-end="handleDragEnd" @reset="resetColumns" />
          </template>
        </a-popover>
      </template>
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false"
        row-key="id" stripe size="small" :scroll="{ y: '100%' }" :scrollbar="true"
        sticky-header :columns="visibleColumns" column-resizable @column-resize="handleColumnResize">
        <template #employeeNo="{ record }">
          {{ record.employeeNo || '—' }}
        </template>
        <template #status="{ record }">
          <a-tag :color="USER_STATUS_MAP[record.status]?.color" size="small">{{ USER_STATUS_MAP[record.status]?.label }}</a-tag>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary" @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-dropdown trigger="click" position="br">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="openResetPwd(record)">重置密码</a-doption>
                <a-doption class="is-danger" @click="confirmDelete(record.id)">删除用户</a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
      <div style="display: flex; justify-content: flex-end; padding: 8px 4px 2px;">
        <a-pagination v-model:current="pagination.current" v-model:page-size="pagination.pageSize"
          :total="pagination.total" show-total show-page-size
          @change="handlePageChange" @page-size-change="handlePageSizeChange" />
      </div>
    </GovernanceListStage>

    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="740" :height="620" :min-width="560" :min-height="440">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
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
            <a-row v-if="!formData.id" :gutter="16">
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
          <a-tab-pane key="org" title="组织与岗位">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="orgId" label="所属机构">
                  <a-tree-select v-model="formData.orgId" :data="orgTreeData"
                    :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                    placeholder="请选择所属机构" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="deptId" label="所属部门">
                  <a-tree-select v-model="formData.deptId" :data="deptTreeData"
                    :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                    placeholder="请选择所属部门" allow-clear />
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
        <a-button v-if="formData.id" status="warning" @click="resetPwdVisible = true">重置密码</a-button>
        <a-button @click="dialogVisible = false">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="handleSubmit">确定</a-button>
      </template>
    </BmlModal>

    <a-modal v-model:visible="resetPwdVisible" title="重置密码" :width="400"
      @ok="handleResetPassword" @cancel="resetPwdVisible = false">
      <a-form layout="vertical">
        <a-form-item label="新密码">
          <a-input-password v-model="newPassword" placeholder="请输入新密码" allow-clear />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'SystemUser' });

import { ref, reactive, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconMore, IconSettings, IconUp, IconDown } from '@arco-design/web-vue/es/icon';
import {
  fetchUserPage, fetchUserDetail, createUser, updateUser, deleteUser, resetUserPassword,
  fetchRoleList, fetchOrgList, fetchDeptList, fetchPostList,
  type UserVO, type UserForm, type RoleVO, type OrgVO, type DeptVO, type PostVO
} from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';

const USER_STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常', color: 'green' },
  0: { label: '停用', color: 'red' },
  2: { label: '锁定', color: 'orange' },
};

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

const defaultColumns: BusinessTableColumn[] = [
  { key: 'username', title: '用户名', dataIndex: 'username', width: 120, visible: true },
  { key: 'nickname', title: '昵称', dataIndex: 'nickname', width: 110, visible: true },
  { key: 'employeeNo', title: '工号', slotName: 'employeeNo', width: 100, visible: true },
  { key: 'orgName', title: '所属机构', dataIndex: 'orgName', width: 140, visible: true },
  { key: 'deptName', title: '部门', dataIndex: 'deptName', width: 120, visible: true },
  { key: 'postName', title: '岗位', dataIndex: 'postName', width: 110, visible: true },
  { key: 'phone', title: '手机号', dataIndex: 'phone', width: 130, visible: true },
  { key: 'status', title: '状态', slotName: 'status', width: 80, visible: true, align: 'center' },
  { key: 'entryDate', title: '入职日期', dataIndex: 'entryDate', width: 110, visible: true },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 170, visible: true },
  { key: 'actions', title: '操作', slotName: 'actions', width: 140, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const {
  visibleColumns, columnSettingItems, dragState,
  handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed,
  handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns,
} = useBusinessTableColumns('system-user', defaultColumns);

const loading = ref(false);
const tableData = ref<UserVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const orgTreeData = ref<OrgVO[]>([]);
const deptTreeData = ref<DeptVO[]>([]);
const postOptions = ref<PostVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增用户');
const formRef = ref();
const pagination = reactive({ current: 1, pageSize: 20, total: 0 });
const queryParams = reactive({ username: '', phone: '', status: undefined as number | undefined, orgId: undefined as number | undefined });
const defaultForm = (): UserForm => ({
  id: undefined, username: '', nickname: '', password: '', phone: '', email: '',
  gender: 0, status: 1, orgId: undefined, deptId: undefined, postId: undefined,
  employeeNo: '', entryDate: '', roleIds: [], remark: ''
});
const formData = reactive<UserForm>(defaultForm());
const formRules = {
  username: [{ required: true, message: '请输入用户名' }],
  nickname: [{ required: true, message: '请输入昵称' }],
  password: [{ required: true, message: '请输入密码' }],
};
const resetPwdVisible = ref(false);
const newPassword = ref('');
const currentResetUserId = ref<number | undefined>(undefined);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchUserPage({ ...queryParams, pageNum: pagination.current, pageSize: pagination.pageSize }) as any;
    tableData.value = res.data?.records || [];
    pagination.total = res.data?.total || 0;
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};
const loadRoles = async () => { try { const r = await fetchRoleList() as any; roleOptions.value = r.data || []; } catch { roleOptions.value = []; } };
const loadOrgTree = async () => { try { const r = await fetchOrgList({}) as any; orgTreeData.value = r.data || []; } catch { orgTreeData.value = []; } };
const loadDeptTree = async () => { try { const r = await fetchDeptList({}) as any; deptTreeData.value = r.data || []; } catch { deptTreeData.value = []; } };
const loadPosts = async () => { try { const r = await fetchPostList({}) as any; postOptions.value = r.data || []; } catch { postOptions.value = []; } };

const handleSearch = () => { pagination.current = 1; loadData(); };
const handleReset = () => { queryParams.username = ''; queryParams.phone = ''; queryParams.status = undefined; queryParams.orgId = undefined; pagination.current = 1; loadData(); };
const handlePageChange = (page: number) => { pagination.current = page; loadData(); };
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; loadData(); };

const handleAdd = () => { dialogTitle.value = '新增用户'; Object.assign(formData, defaultForm()); dialogVisible.value = true; };

const handleEdit = async (row: UserVO) => {
  dialogTitle.value = '编辑用户';
  Object.assign(formData, { id: row.id, username: row.username, nickname: row.nickname, password: '', phone: row.phone, email: row.email, gender: row.gender, status: row.status, orgId: row.orgId || undefined, deptId: row.deptId || undefined, postId: row.postId || undefined, employeeNo: row.employeeNo || '', entryDate: row.entryDate || '', roleIds: row.roleIds || [], remark: row.remark });
  try { const r = await fetchUserDetail(row.id) as any; formData.roleIds = r.data?.roleIds || []; } catch { /* keep */ }
  dialogVisible.value = true;
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) { await updateUser(formData); Message.success('修改成功'); }
    else { await createUser(formData); Message.success('新增成功'); }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => { try { await deleteUser(id); Message.success('删除成功'); loadData(); } catch { /* ignore */ } };
const confirmDelete = (id: number) => { Modal.confirm({ title: '确认删除', content: '确认删除该用户吗？', okButtonProps: { status: 'danger' }, onOk: () => handleDelete(id) }); };

const openResetPwd = (record: UserVO) => { currentResetUserId.value = record.id; newPassword.value = ''; resetPwdVisible.value = true; };
const handleResetPassword = async () => {
  const uid = currentResetUserId.value || formData.id;
  if (!uid || !newPassword.value) return;
  try { await resetUserPassword(uid, newPassword.value); Message.success('密码重置成功'); resetPwdVisible.value = false; newPassword.value = ''; } catch { /* ignore */ }
};

onMounted(() => { loadData(); loadRoles(); loadOrgTree(); loadDeptTree(); loadPosts(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) { padding-top: 12px; }
</style>
