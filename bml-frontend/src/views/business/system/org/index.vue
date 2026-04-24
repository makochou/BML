<template>
  <div class="page-wrapper">
    <!-- ════════════════════════════════════════════════
         查询面板（与中台授权治理风格一致）
         ════════════════════════════════════════════════ -->
    <GovernanceCompactQueryPanel density="ultra" theme="aurora">
      <template #footerActions>
        <a-button @click="handleReset">重置条件</a-button>
        <a-button type="primary" @click="handleSearch">查询</a-button>
      </template>
      <a-form :model="queryParams" layout="inline" class="query-form">
        <a-form-item field="orgName" label="机构名称">
          <a-input v-model="queryParams.orgName" placeholder="请输入机构名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="orgCode" label="机构编码">
          <a-input v-model="queryParams.orgCode" placeholder="请输入机构编码" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="orgType" label="机构类型">
          <a-select v-model="queryParams.orgType" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
            <a-option :value="1">集团</a-option>
            <a-option :value="2">公司</a-option>
            <a-option :value="3">分公司</a-option>
            <a-option :value="4">子公司</a-option>
            <a-option :value="5">办事处</a-option>
            <a-option :value="6">事业部</a-option>
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
         列表舞台（与中台授权治理风格一致）
         ════════════════════════════════════════════════ -->
    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增机构
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
          <a-table-column title="机构名称" data-index="orgName" :width="220" />
          <a-table-column title="机构编码" data-index="orgCode" :width="130" />
          <a-table-column title="机构类型" data-index="orgType" :width="100" align="center">
            <template #cell="{ record }">
              <a-tag size="small" :color="orgTypeColor(record.orgType)">{{ orgTypeLabel(record.orgType) }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column title="负责人" data-index="leader" :width="100" />
          <a-table-column title="联系电话" data-index="phone" :width="130" />
          <a-table-column title="数据隔离" data-index="dataIsolation" :width="110" align="center">
            <template #cell="{ record }">
              <a-tag size="small" :color="isolationColor(record.dataIsolation)">{{ isolationLabel(record.dataIsolation) }}</a-tag>
            </template>
          </a-table-column>
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
                <a-popconfirm content="确认删除该机构吗？" @ok="handleDelete(record.id)">
                  <a-button type="text" size="small" status="danger"><template #icon><icon-delete /></template>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-table-column>
        </template>
      </a-table>
    </GovernanceListStage>

    <!-- 新增/编辑弹窗（BmlModal：支持拖拽、缩放、全屏） -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="780" :height="620" :min-width="600" :min-height="480">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <!-- ── 基本信息 ── -->
          <a-tab-pane key="basic" title="基本信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="parentId" label="上级机构">
                  <a-tree-select
                    v-model="formData.parentId"
                    :data="orgTreeOptions"
                    :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                    placeholder="请选择上级机构"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="orgName" label="机构名称">
                  <a-input v-model="formData.orgName" placeholder="请输入机构名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="orgCode" label="机构编码">
                  <a-input v-model="formData.orgCode" placeholder="请输入机构编码" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="orgType" label="机构类型">
                  <a-select v-model="formData.orgType" placeholder="请选择机构类型">
                    <a-option :value="1">集团</a-option>
                    <a-option :value="2">公司</a-option>
                    <a-option :value="3">分公司</a-option>
                    <a-option :value="4">子公司</a-option>
                    <a-option :value="5">办事处</a-option>
                    <a-option :value="6">事业部</a-option>
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
                <a-form-item field="status" label="机构状态">
                  <a-select v-model="formData.status" placeholder="请选择">
                    <a-option :value="1">正常</a-option>
                    <a-option :value="0">停用</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="dataIsolation" label="数据隔离模式">
                  <a-select v-model="formData.dataIsolation" placeholder="请选择">
                    <a-option v-for="iso in ISOLATION_OPTIONS" :key="iso.value" :value="iso.value">{{ iso.label }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="leader" label="负责人">
                  <a-input v-model="formData.leader" placeholder="请输入负责人" />
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
          </a-tab-pane>

          <!-- ── 工商信息 ── -->
          <a-tab-pane key="business" title="工商信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="creditCode" label="统一社会信用代码">
                  <a-input v-model="formData.creditCode" placeholder="18位信用代码" :max-length="18" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="legalPerson" label="法定代表人">
                  <a-input v-model="formData.legalPerson" placeholder="请输入法定代表人" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="registeredCapital" label="注册资本（万元）">
                  <a-input-number v-model="formData.registeredCapital" :min="0" :precision="2" placeholder="注册资本" style="width: 100%;" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="establishDate" label="成立日期">
                  <a-date-picker v-model="formData.establishDate" placeholder="请选择成立日期" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="businessScope" label="经营范围">
                  <a-textarea v-model="formData.businessScope" placeholder="请输入经营范围" :auto-size="{ minRows: 3, maxRows: 6 }" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>

          <!-- ── 联系与地址 ── -->
          <a-tab-pane key="contact" title="联系与地址">
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
              <a-col :span="8">
                <a-form-item field="province" label="省份">
                  <a-input v-model="formData.province" placeholder="省份" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item field="city" label="城市">
                  <a-input v-model="formData.city" placeholder="城市" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item field="district" label="区县">
                  <a-input v-model="formData.district" placeholder="区县" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="address" label="详细地址">
                  <a-input v-model="formData.address" placeholder="请输入详细地址" />
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
 * 机构管理页面
 *
 * 重要说明：
 *   defineOptions({ name: 'SystemOrg' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'SystemOrg' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete } from '@arco-design/web-vue/es/icon';
import { fetchOrgList, createOrg, updateOrg, deleteOrg, type OrgVO, type OrgForm, type OrgQuery } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';

/* ════════════════════════════════════════════════════════════
   机构类型映射
   ════════════════════════════════════════════════════════════ */
const ORG_TYPE_MAP: Record<number, string> = { 1: '集团', 2: '公司', 3: '分公司', 4: '子公司', 5: '办事处', 6: '事业部' };
const ORG_TYPE_COLOR: Record<number, string> = { 1: 'purple', 2: 'arcoblue', 3: 'cyan', 4: 'blue', 5: 'orangered', 6: 'green' };
const orgTypeLabel = (t: number) => ORG_TYPE_MAP[t] || '未知';
const orgTypeColor = (t: number) => ORG_TYPE_COLOR[t] || 'gray';

/* ════════════════════════════════════════════════════════════
   数据隔离模式（对应后端 DataIsolationType 枚举）
   ════════════════════════════════════════════════════════════ */
const ISOLATION_OPTIONS = [
  { value: 0, label: '共享 — 上级可查看下级数据', short: '共享' },
  { value: 1, label: '完全隔离 — 各机构数据独立', short: '完全隔离' },
  { value: 2, label: '汇总共享 — 上级仅看汇总', short: '汇总共享' },
  { value: 3, label: '同级互通 — 兄弟机构互查', short: '同级互通' },
  { value: 4, label: '按模块隔离 — 分模块控制', short: '按模块隔离' }
];
const ISOLATION_MAP: Record<number, string> = Object.fromEntries(ISOLATION_OPTIONS.map(i => [i.value, i.short]));
const ISOLATION_COLOR: Record<number, string> = { 0: 'arcoblue', 1: 'orangered', 2: 'gold', 3: 'green', 4: 'purple' };
const isolationLabel = (v: number) => ISOLATION_MAP[v] || '未知';
const isolationColor = (v: number) => ISOLATION_COLOR[v] || 'gray';

/* ════════════════════════════════════════════════════════════
   响应式状态
   ════════════════════════════════════════════════════════════ */
const loading = ref(false);
const tableData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增机构');
const formRef = ref();

/** 查询参数 */
const queryParams = reactive<OrgQuery>({ orgName: '', orgCode: '', orgType: undefined, status: undefined });

/** 表单默认值 */
const defaultForm = (): OrgForm => ({
  id: undefined, parentId: 0, orgName: '', orgCode: '', orgType: 2,
  creditCode: '', legalPerson: '', registeredCapital: undefined,
  establishDate: '', sort: 0, leader: '', phone: '', email: '',
  province: '', city: '', district: '', address: '',
  businessScope: '', status: 1, remark: '', dataIsolation: 0
});
const formData = reactive<OrgForm>(defaultForm());

/** 表单校验规则 */
const formRules = {
  orgName: [{ required: true, message: '请输入机构名称' }],
  orgType: [{ required: true, message: '请选择机构类型' }]
};

/** 机构树选项（用于上级机构下拉） */
const orgTreeOptions = computed(() => {
  const root = {
    id: 0, parentId: -1, orgName: '顶级机构', orgCode: '', orgType: 0,
    creditCode: '', legalPerson: '', registeredCapital: 0, establishDate: '',
    sort: 0, leader: '', phone: '', email: '', province: '', city: '', district: '',
    address: '', businessScope: '', status: 1, remark: '', dataIsolation: 0,
    createTime: '', children: tableData.value
  } as OrgVO;
  return [root];
});

/* ════════════════════════════════════════════════════════════
   数据加载与操作
   ════════════════════════════════════════════════════════════ */

/** 加载机构树列表 */
const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchOrgList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

/** 查询 */
const handleSearch = () => { loadData(); };

/** 重置查询条件 */
const handleReset = () => {
  queryParams.orgName = '';
  queryParams.orgCode = '';
  queryParams.orgType = undefined;
  queryParams.status = undefined;
  loadData();
};

/** 新增（可指定父机构ID） */
const handleAdd = (parentId?: number) => {
  dialogTitle.value = '新增机构';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

/** 编辑 */
const handleEdit = (row: OrgVO) => {
  dialogTitle.value = '编辑机构';
  Object.assign(formData, {
    id: row.id, parentId: row.parentId, orgName: row.orgName, orgCode: row.orgCode,
    orgType: row.orgType, creditCode: row.creditCode, legalPerson: row.legalPerson,
    registeredCapital: row.registeredCapital, establishDate: row.establishDate,
    sort: row.sort, leader: row.leader, phone: row.phone, email: row.email,
    province: row.province, city: row.city, district: row.district,
    address: row.address, businessScope: row.businessScope,
    status: row.status, remark: row.remark, dataIsolation: row.dataIsolation
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
      await updateOrg(formData);
      Message.success('修改成功');
    } else {
      await createOrg(formData);
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
    await deleteOrg(id);
    Message.success('删除成功');
    loadData();
  } catch { /* ignore */ }
};

onMounted(() => { loadData(); });
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
