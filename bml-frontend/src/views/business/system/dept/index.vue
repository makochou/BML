<template>
  <div class="page-wrapper">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-space wrap>
        <a-input v-model="queryParams.deptName" placeholder="部门名称" allow-clear style="width: 180px;" @press-enter="handleSearch" />
        <a-select v-model="queryParams.status" placeholder="状态" allow-clear style="width: 120px;" @change="handleSearch">
          <a-option :value="1">正常</a-option>
          <a-option :value="0">停用</a-option>
        </a-select>
        <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>搜索</a-button>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
      </a-space>
      <a-button type="primary" status="success" @click="handleAdd()"><template #icon><icon-plus /></template>新增部门</a-button>
    </div>

    <!-- 数据表格（树形） -->
    <div class="table-card">
      <a-table
        :data="tableData"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="true"
      >
        <template #columns>
          <a-table-column title="部门名称" data-index="deptName" :width="220" />
          <a-table-column title="负责人" data-index="leader" :width="140" />
          <a-table-column title="联系电话" data-index="phone" :width="140" />
          <a-table-column title="邮箱" data-index="email" :width="200" />
          <a-table-column title="排序" data-index="sort" :width="80" align="center" />
          <a-table-column title="状态" data-index="status" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="创建时间" data-index="createTime" :width="180" />
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
    </div>

    <!-- 新增/编辑弹窗（BmlModal：支持拖拽、缩放、全屏） -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="640" :height="520" :min-width="480" :min-height="360">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
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
          <a-col :span="12">
            <a-form-item field="deptName" label="部门名称">
              <a-input v-model="formData.deptName" placeholder="请输入部门名称" />
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
import { ref, reactive, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconSearch, IconRefresh, IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchDeptList, createDept, updateDept, deleteDept, type DeptVO, type DeptForm } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';

const loading = ref(false);
const tableData = ref<DeptVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增部门');
const formRef = ref();

const queryParams = reactive({ deptName: '', status: undefined as number | undefined });

const defaultForm = (): DeptForm => ({ id: undefined, parentId: 0, deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1 });
const formData = reactive<DeptForm>(defaultForm());

const formRules = {
  deptName: [{ required: true, message: '请输入部门名称' }]
};

const deptTreeOptions = computed(() => {
  const root: DeptVO = { id: 0, parentId: -1, deptName: '根部门', sort: 0, leader: '', phone: '', email: '', status: 1, createTime: '', children: tableData.value };
  return [root];
});

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
    id: row.id, parentId: row.parentId, deptName: row.deptName, sort: row.sort,
    leader: row.leader, phone: row.phone, email: row.email, status: row.status
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

onMounted(() => { loadData(); });
</script>

<style scoped>
.page-wrapper { padding: 20px; height: 100%; display: flex; flex-direction: column; gap: 16px; }
.search-bar { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px; padding: 20px; background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.table-card { flex: 1; background: #fff; border-radius: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); padding: 16px; overflow: auto; }
</style>
