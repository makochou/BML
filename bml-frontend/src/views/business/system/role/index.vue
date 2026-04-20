<template>
  <div class="page-wrapper">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-space wrap>
        <a-input v-model="queryParams.roleName" placeholder="角色名称" allow-clear style="width: 180px;" @press-enter="handleSearch" />
        <a-input v-model="queryParams.roleCode" placeholder="角色编码" allow-clear style="width: 180px;" @press-enter="handleSearch" />
        <a-select v-model="queryParams.status" placeholder="状态" allow-clear style="width: 120px;" @change="handleSearch">
          <a-option :value="1">正常</a-option>
          <a-option :value="0">停用</a-option>
        </a-select>
        <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>搜索</a-button>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
      </a-space>
      <a-button type="primary" status="success" @click="handleAdd"><template #icon><icon-plus /></template>新增角色</a-button>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" stripe>
        <template #columns>
          <a-table-column title="角色名称" data-index="roleName" :width="160" />
          <a-table-column title="角色编码" data-index="roleCode" :width="160" />
          <a-table-column title="排序" data-index="sort" :width="80" align="center" />
          <a-table-column title="状态" data-index="status" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="创建时间" data-index="createTime" :width="180" />
          <a-table-column title="备注" data-index="remark" :width="200" ellipsis />
          <a-table-column title="操作" :width="200" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space>
                <a-button type="text" size="small" @click="handleEdit(record)"><template #icon><icon-edit /></template>编辑</a-button>
                <a-popconfirm content="确认删除该角色吗？" @ok="handleDelete(record.id)">
                  <a-button type="text" size="small" status="danger"><template #icon><icon-delete /></template>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗（BmlModal：支持拖拽、缩放、全屏） -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="640" :height="580" :min-width="480" :min-height="360">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="roleName" label="角色名称">
              <a-input v-model="formData.roleName" placeholder="请输入角色名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="roleCode" label="角色编码">
              <a-input v-model="formData.roleCode" placeholder="请输入角色编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="sort" label="排序">
              <a-input-number v-model="formData.sort" :min="0" placeholder="显示顺序" style="width: 100%;" />
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
        <a-form-item field="menuIds" label="菜单权限">
          <a-tree-select
            v-model="formData.menuIds"
            :data="menuTreeData"
            :field-names="{ key: 'id', title: 'menuName', children: 'children' }"
            placeholder="请选择菜单权限"
            multiple
            allow-clear
            :max-tag-count="3"
            tree-checkable
            tree-check-strictly
          />
        </a-form-item>
        <a-form-item field="remark" label="备注">
          <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
        </a-form-item>
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
import { fetchRoleList, createRole, updateRole, deleteRole, fetchMenuList, type RoleVO, type RoleForm, type MenuVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';

const loading = ref(false);
const tableData = ref<RoleVO[]>([]);
const menuTreeData = ref<MenuVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const formRef = ref();

const queryParams = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined });

const defaultForm = (): RoleForm => ({ id: undefined, roleName: '', roleCode: '', sort: 0, status: 1, menuIds: [], remark: '' });
const formData = reactive<RoleForm>(defaultForm());

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [{ required: true, message: '请输入角色编码' }]
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchRoleList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const loadMenuTree = async () => {
  try {
    const res = await fetchMenuList() as any;
    menuTreeData.value = res.data || [];
  } catch { menuTreeData.value = []; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => { queryParams.roleName = ''; queryParams.roleCode = ''; queryParams.status = undefined; loadData(); };

const handleAdd = () => {
  dialogTitle.value = '新增角色';
  Object.assign(formData, defaultForm());
  dialogVisible.value = true;
};

const handleEdit = (row: RoleVO) => {
  dialogTitle.value = '编辑角色';
  Object.assign(formData, { id: row.id, roleName: row.roleName, roleCode: row.roleCode, sort: row.sort, status: row.status, menuIds: [], remark: row.remark });
  dialogVisible.value = true;
};

const submitting = ref(false);
const handleSubmit = async () => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) return;
    submitting.value = true;
    if (formData.id) {
      await updateRole(formData);
      Message.success('修改成功');
    } else {
      await createRole(formData);
      Message.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch { /* 保持弹窗打开 */ }
  finally { submitting.value = false; }
};

const handleDelete = async (id: number) => {
  try {
    await deleteRole(id);
    Message.success('删除成功');
    loadData();
  } catch { /* ignore */ }
};

onMounted(() => { loadData(); loadMenuTree(); });
</script>

<style scoped>
.page-wrapper { padding: 20px; height: 100%; display: flex; flex-direction: column; gap: 16px; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; padding: 20px; background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.table-card { flex: 1; background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); padding: 16px; overflow: auto; }
</style>
