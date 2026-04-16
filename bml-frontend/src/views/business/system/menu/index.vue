<template>
  <div class="page-wrapper">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <a-space wrap>
        <a-input v-model="queryParams.menuName" placeholder="菜单名称" allow-clear style="width: 180px;" @press-enter="handleSearch" />
        <a-select v-model="queryParams.status" placeholder="状态" allow-clear style="width: 120px;" @change="handleSearch">
          <a-option :value="1">正常</a-option>
          <a-option :value="0">停用</a-option>
        </a-select>
        <a-button type="primary" @click="handleSearch"><template #icon><icon-search /></template>搜索</a-button>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置</a-button>
      </a-space>
      <a-button type="primary" status="success" @click="handleAdd()"><template #icon><icon-plus /></template>新增菜单</a-button>
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
          <a-table-column title="菜单名称" data-index="menuName" :width="220" />
          <a-table-column title="图标" data-index="icon" :width="80" align="center">
            <template #cell="{ record }">
              <span v-if="record.icon" style="color: #4f46e5;">{{ record.icon }}</span>
              <span v-else style="color: #c9cdd4;">-</span>
            </template>
          </a-table-column>
          <a-table-column title="排序" data-index="sort" :width="80" align="center" />
          <a-table-column title="类型" data-index="menuType" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag v-if="record.menuType === 'M'" color="blue" size="small">目录</a-tag>
              <a-tag v-else-if="record.menuType === 'C'" color="green" size="small">菜单</a-tag>
              <a-tag v-else-if="record.menuType === 'F'" color="orange" size="small">按钮</a-tag>
              <span v-else>-</span>
            </template>
          </a-table-column>
          <a-table-column title="权限标识" data-index="perms" :width="200" ellipsis />
          <a-table-column title="组件路径" data-index="component" :width="200" ellipsis />
          <a-table-column title="状态" data-index="status" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="240" align="center" fixed="right">
            <template #cell="{ record }">
              <a-space>
                <a-button v-if="record.menuType !== 'F'" type="text" size="small" @click="handleAdd(record.id)">
                  <template #icon><icon-plus /></template>新增
                </a-button>
                <a-button type="text" size="small" @click="handleEdit(record)"><template #icon><icon-edit /></template>编辑</a-button>
                <a-popconfirm content="确认删除该菜单吗？" @ok="handleDelete(record.id)">
                  <a-button type="text" size="small" status="danger"><template #icon><icon-delete /></template>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal v-model:visible="dialogVisible" :title="dialogTitle" :width="640" :mask-closable="false" @before-ok="handleSubmit">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="parentId" label="上级菜单">
              <a-tree-select
                v-model="formData.parentId"
                :data="menuTreeOptions"
                :field-names="{ key: 'id', title: 'menuName', children: 'children' }"
                placeholder="请选择上级菜单"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="menuType" label="菜单类型">
              <a-radio-group v-model="formData.menuType">
                <a-radio value="M">目录</a-radio>
                <a-radio value="C">菜单</a-radio>
                <a-radio value="F">按钮</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item field="menuName" label="菜单名称">
              <a-input v-model="formData.menuName" placeholder="请输入菜单名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="sort" label="显示排序">
              <a-input-number v-model="formData.sort" :min="0" placeholder="排序" style="width: 100%;" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16" v-if="formData.menuType !== 'F'">
          <a-col :span="12">
            <a-form-item field="path" label="路由地址">
              <a-input v-model="formData.path" placeholder="请输入路由地址" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.menuType === 'C'">
            <a-form-item field="component" label="组件路径">
              <a-input v-model="formData.component" placeholder="请输入组件路径" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12" v-if="formData.menuType !== 'M'">
            <a-form-item field="perms" label="权限标识">
              <a-input v-model="formData.perms" placeholder="如：system:user:list" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="formData.menuType !== 'F'">
            <a-form-item field="icon" label="菜单图标">
              <a-input v-model="formData.icon" placeholder="请输入图标名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12" v-if="formData.menuType !== 'F'">
            <a-form-item field="visible" label="显示状态">
              <a-select v-model="formData.visible" placeholder="请选择">
                <a-option :value="1">显示</a-option>
                <a-option :value="0">隐藏</a-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item field="status" label="菜单状态">
              <a-select v-model="formData.status" placeholder="请选择">
                <a-option :value="1">正常</a-option>
                <a-option :value="0">停用</a-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconSearch, IconRefresh, IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchMenuList, createMenu, updateMenu, deleteMenu, type MenuVO, type MenuForm } from '../../../../api/system';

const loading = ref(false);
const tableData = ref<MenuVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增菜单');
const formRef = ref();

const queryParams = reactive({ menuName: '', status: undefined as number | undefined });

const defaultForm = (): MenuForm => ({
  id: undefined, parentId: 0, menuName: '', menuType: 'M', path: '', component: '', perms: '',
  icon: '', sort: 0, visible: 1, status: 1, isFrame: 1
});
const formData = reactive<MenuForm>(defaultForm());

const formRules = {
  menuName: [{ required: true, message: '请输入菜单名称' }],
  menuType: [{ required: true, message: '请选择菜单类型' }]
};

const menuTreeOptions = computed(() => {
  const root: MenuVO = { id: 0, parentId: -1, menuName: '根目录', menuType: 'M', path: '', component: '', perms: '', icon: '', sort: 0, visible: 1, status: 1, isFrame: 0, remark: '', createTime: '', children: tableData.value };
  return [root];
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchMenuList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => { queryParams.menuName = ''; queryParams.status = undefined; loadData(); };

const handleAdd = (parentId?: number) => {
  dialogTitle.value = '新增菜单';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

const handleEdit = (row: MenuVO) => {
  dialogTitle.value = '编辑菜单';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, menuName: row.menuName, menuType: row.menuType,
    path: row.path, component: row.component, perms: row.perms, icon: row.icon,
    sort: row.sort, visible: row.visible, status: row.status, isFrame: row.isFrame
  });
  dialogVisible.value = true;
};

const handleSubmit = async (done: (closed: boolean) => void) => {
  try {
    const errors = await formRef.value?.validate();
    if (errors) { done(false); return; }
    if (formData.id) {
      await updateMenu(formData);
      Message.success('修改成功');
    } else {
      await createMenu(formData);
      Message.success('新增成功');
    }
    done(true);
    loadData();
  } catch { done(false); }
};

const handleDelete = async (id: number) => {
  try {
    await deleteMenu(id);
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
