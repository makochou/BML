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
        <a-form-item field="roleName" label="角色名称">
          <a-input v-model="queryParams.roleName" placeholder="请输入角色名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="roleCode" label="角色编码">
          <a-input v-model="queryParams.roleCode" placeholder="请输入角色编码" allow-clear @press-enter="handleSearch" />
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
          新增角色
        </a-button>
      </template>
      <a-table :data="tableData" :loading="loading" :bordered="false" :pagination="false" row-key="id" stripe size="small" :scroll="{ y: '100%' }" :scrollbar="true" sticky-header>
        <template #columns>
          <a-table-column title="角色名称" data-index="roleName" :width="140" />
          <a-table-column title="角色编码" data-index="roleCode" :width="140" />
          <a-table-column title="数据权限" data-index="dataScope" :width="140" align="center">
            <template #cell="{ record }">
              <a-tag size="small" :color="dataScopeColor(record.dataScope)">{{ dataScopeLabel(record.dataScope) }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="排序" data-index="sort" :width="70" align="center" />
          <a-table-column title="状态" data-index="status" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="创建时间" data-index="createTime" :width="170" />
          <a-table-column title="备注" data-index="remark" ellipsis />
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
    </GovernanceListStage>

    <!-- 新增/编辑弹窗 -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="700" :height="620" :min-width="540" :min-height="440">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <!-- ── 基本信息 ── -->
          <a-tab-pane key="basic" title="基本信息">
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
            <a-form-item field="remark" label="备注">
              <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
            </a-form-item>
          </a-tab-pane>

          <!-- ── 菜单权限 ── -->
          <a-tab-pane key="menu" title="菜单权限">
            <a-form-item field="menuIds" label="分配菜单">
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
          </a-tab-pane>

          <!-- ── 数据权限 ── -->
          <a-tab-pane key="dataScope" title="数据权限">
            <a-form-item field="dataScope" label="数据范围">
              <a-select v-model="formData.dataScope" placeholder="请选择数据范围">
                <a-option v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value" :value="ds.value">{{ ds.label }}</a-option>
              </a-select>
            </a-form-item>
            <a-alert type="info" class="scope-hint">
              <template #title>数据权限说明</template>
              <div class="scope-desc">
                <p v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value">
                  <a-tag size="small" :color="dataScopeColor(ds.value)">{{ ds.label }}</a-tag>
                  <span> — {{ ds.desc }}</span>
                </p>
              </div>
            </a-alert>
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
import { ref, reactive, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchRoleList, createRole, updateRole, deleteRole, fetchMenuList, type RoleVO, type RoleForm, type MenuVO } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';

/* ════════════════════════════════════════════════════════════
   数据权限范围常量（对应后端 DataScopeType 枚举）
   ════════════════════════════════════════════════════════════ */
const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据', desc: '不受任何限制，可查看系统所有数据' },
  { value: 2, label: '所在机构及下级', desc: '可查看本机构及下级数据（受机构数据隔离模式影响，完全隔离的下级不可见）' },
  { value: 3, label: '仅本机构', desc: '只能查看本机构数据（若本机构设为同级互通，可看兄弟机构数据）' },
  { value: 4, label: '所在部门及下级', desc: '可查看本部门及所有下级部门数据' },
  { value: 5, label: '仅本部门', desc: '只能查看当前所属部门的数据' },
  { value: 6, label: '仅本人', desc: '只能查看自己创建的数据' },
  { value: 7, label: '自定义', desc: '管理员手动指定可访问的机构/部门范围' }
];
const DS_MAP: Record<number, string> = Object.fromEntries(DATA_SCOPE_OPTIONS.map(d => [d.value, d.label]));
const DS_COLOR: Record<number, string> = { 1: 'red', 2: 'purple', 3: 'arcoblue', 4: 'cyan', 5: 'green', 6: 'orangered', 7: 'gold' };
const dataScopeLabel = (v: number) => DS_MAP[v] || '未设置';
const dataScopeColor = (v: number) => DS_COLOR[v] || 'gray';

/* ════════════════════════════════════════════════════════════
   响应式状态
   ════════════════════════════════════════════════════════════ */
const loading = ref(false);
const tableData = ref<RoleVO[]>([]);
const menuTreeData = ref<MenuVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const formRef = ref();

const queryParams = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined });

const defaultForm = (): RoleForm => ({ id: undefined, roleName: '', roleCode: '', sort: 0, dataScope: 6, status: 1, menuIds: [], remark: '' });
const formData = reactive<RoleForm>(defaultForm());

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称' }],
  roleCode: [{ required: true, message: '请输入角色编码' }]
};

/* ════════════════════════════════════════════════════════════
   数据加载与操作
   ════════════════════════════════════════════════════════════ */
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
  Object.assign(formData, {
    id: row.id, roleName: row.roleName, roleCode: row.roleCode,
    sort: row.sort, dataScope: row.dataScope || 6,
    status: row.status, menuIds: [], remark: row.remark
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
.scope-hint {
  margin-top: 12px;
}
.scope-desc p {
  margin: 4px 0;
  font-size: 12px;
  line-height: 1.6;
}
</style>
