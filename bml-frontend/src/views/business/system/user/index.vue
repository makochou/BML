<template>
  <div class="page-wrapper">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-space wrap>
        <a-input v-model="queryParams.username" placeholder="用户名" allow-clear style="width: 180px;" @press-enter="handleSearch" />
        <a-input v-model="queryParams.phone" placeholder="手机号" allow-clear style="width: 180px;" @press-enter="handleSearch" />
        <a-select v-model="queryParams.status" placeholder="状态" allow-clear style="width: 120px;" @change="handleSearch">
          <a-option :value="1">正常</a-option>
          <a-option :value="0">停用</a-option>
        </a-select>
        <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>搜索</a-button>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
      </a-space>
      <a-button type="primary" status="success" @click="handleAdd"><template #icon><icon-plus /></template>新增用户</a-button>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" stripe>
        <template #columns>
          <a-table-column title="用户名" data-index="username" :width="140" />
          <a-table-column title="昵称" data-index="nickname" :width="140" />
          <a-table-column title="手机号" data-index="phone" :width="140" />
          <a-table-column title="邮箱" data-index="email" :width="200" />
          <a-table-column title="状态" data-index="status" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="创建时间" data-index="createTime" :width="180" />
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
    </div>

    <!-- 新增/编辑弹窗（BmlModal：支持拖拽、缩放、全屏） -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="640" :height="560" :min-width="480" :min-height="360">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
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
import { IconSearch, IconRefresh, IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchUserList, createUser, updateUser, deleteUser, type UserVO, type UserForm } from '../../../../api/system';
import { fetchRoleList, type RoleVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';

const loading = ref(false);
const tableData = ref<UserVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增用户');
const formRef = ref();

const queryParams = reactive({ username: '', phone: '', status: undefined as number | undefined });

const defaultForm = (): UserForm => ({ id: undefined, username: '', nickname: '', password: '', phone: '', email: '', gender: 0, status: 1, roleIds: [], remark: '' });
const formData = reactive<UserForm>(defaultForm());

const formRules = {
  username: [{ required: true, message: '请输入用户名' }],
  nickname: [{ required: true, message: '请输入昵称' }],
  password: [{ required: true, message: '请输入密码' }]
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchUserList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const loadRoles = async () => {
  try {
    const res = await fetchRoleList() as any;
    roleOptions.value = res.data || [];
  } catch { roleOptions.value = []; }
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
  Object.assign(formData, { id: row.id, username: row.username, nickname: row.nickname, password: '', phone: row.phone, email: row.email, gender: row.gender, status: row.status, roleIds: row.roleIds || [], remark: row.remark });
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

onMounted(() => { loadData(); loadRoles(); });
</script>

<style scoped>
.page-wrapper { padding: 20px; height: 100%; display: flex; flex-direction: column; gap: 16px; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; padding: 20px; background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.table-card { flex: 1; background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); padding: 16px; overflow: auto; }
</style>
