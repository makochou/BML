<template>
  <div class="page-wrapper">
    <!-- ════════════════════════════════════════════════
         查询面板
         ════════════════════════════════════════════════ -->
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
        <a-form-item field="orgName" label="机构名称">
          <a-input v-model="queryParams.orgName" placeholder="请输入机构名称" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="orgCode" label="机构编码">
          <a-input v-model="queryParams.orgCode" placeholder="请输入机构编码" allow-clear @press-enter="handleSearch" />
        </a-form-item>
        <a-form-item field="status" label="状态">
          <a-select v-model="queryParams.status" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
            <a-option :value="1">正常</a-option>
            <a-option :value="0">停用</a-option>
          </a-select>
        </a-form-item>

        <!-- 次要字段（展开时显示） -->
        <transition name="query-expand">
          <div v-if="queryExpanded" class="biz-query-form-extra">
            <a-form-item field="orgType" label="机构类型">
              <a-select v-model="queryParams.orgType" placeholder="全部" allow-clear style="width: 120px;" @change="handleSearch">
                <a-option v-for="opt in ORG_TYPE_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</a-option>
              </a-select>
            </a-form-item>
          </div>
        </transition>
      </a-form>
    </GovernanceCompactQueryPanel>

    <!-- ════════════════════════════════════════════════
         列表舞台
         ════════════════════════════════════════════════ -->
    <GovernanceListStage density="ultra" body-fill>
      <template #actions>
        <a-button type="primary" @click="handleAdd()">
          <template #icon><icon-plus /></template>
          新增机构
        </a-button>
        <a-popover trigger="click" position="br" :content-style="{ padding: 0 }">
          <a-button class="table-column-setting-btn">
            <template #icon><icon-settings /></template>
            列设置
          </a-button>
          <template #content>
            <BusinessTableColumnSetting
              :items="columnSettingItems"
              :drag-state="dragState"
              @toggle-visible="toggleColumnVisible"
              @move="moveColumn"
              @toggle-fixed="toggleColumnFixed"
              @drag-start="handleDragStart"
              @drag-over="handleDragOver"
              @drop="handleDrop"
              @drag-end="handleDragEnd"
              @reset="resetColumns"
            />
          </template>
        </a-popover>
      </template>

      <a-table
        :data="tableData"
        :loading="loading"
        :bordered="false"
        :pagination="false"
        row-key="id"
        :default-expand-all-rows="true"
        size="small"
        :columns="visibleColumns"
        :scroll="{ y: '100%' }"
        :scrollbar="true"
        sticky-header
        column-resizable
        @column-resize="handleColumnResize"
      >
        <template #orgType="{ record }">
          <a-tag size="small" :color="orgTypeColor(record.orgType)">{{ orgTypeLabel(record.orgType) }}</a-tag>
        </template>
        <template #dataIsolation="{ record }">
          <a-tag size="small" :color="isolationColor(record.dataIsolation)">{{ isolationLabel(record.dataIsolation) }}</a-tag>
        </template>
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'" size="small">{{ record.status === 1 ? '正常' : '停用' }}</a-tag>
        </template>
        <template #actions="{ record }">
          <div class="table-row-actions" @click.stop @dblclick.stop>
            <a-button type="primary" size="mini" class="table-action-btn table-action-btn--primary"
              @click="handleEdit(record)">
              <template #icon><icon-edit /></template>
              编辑
            </a-button>
            <a-dropdown trigger="click" position="br">
              <a-button size="mini" class="table-action-btn table-action-btn--more">
                <template #icon><icon-more /></template>
              </a-button>
              <template #content>
                <a-doption @click="handleAdd(record.id)">
                  <template #icon><icon-plus /></template>
                  新增子机构
                </a-doption>
                <a-doption class="is-danger" @click="confirmDelete(record)">
                  <template #icon><icon-delete /></template>
                  删除机构
                </a-doption>
              </template>
            </a-dropdown>
          </div>
        </template>
      </a-table>
    </GovernanceListStage>

    <!-- 新增/编辑弹窗 -->
    <BmlModal v-model:visible="dialogVisible" :title="dialogTitle" :width="820" :height="640" :min-width="640" :min-height="480">
      <a-form :model="formData" ref="formRef" :rules="formRules" layout="vertical">
        <a-tabs default-active-key="basic" size="small" class="form-tabs">
          <a-tab-pane key="basic" title="基本信息">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="parentId" label="上级机构">
                  <a-tree-select v-model="formData.parentId" :data="orgTreeOptions"
                    :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
                    placeholder="请选择上级机构" allow-clear />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="orgName" label="机构名称" required>
                  <a-input v-model="formData.orgName" placeholder="请输入机构名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="orgCode" label="机构编码" required>
                  <a-input v-model="formData.orgCode" placeholder="请输入机构编码" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="orgType" label="机构类型" required>
                  <a-select v-model="formData.orgType" placeholder="请选择机构类型">
                    <a-option v-for="opt in ORG_TYPE_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item field="dataIsolation" label="数据隔离模式" required>
                  <a-select v-model="formData.dataIsolation" placeholder="请选择">
                    <a-option v-for="iso in ISOLATION_OPTIONS" :key="iso.value" :value="iso.value">
                      <span style="font-weight: 500;">{{ iso.label }}</span>
                      <span style="color: var(--color-text-3); font-size: 12px; margin-left: 6px;">{{ iso.desc }}</span>
                    </a-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item field="leader" label="负责人">
                  <a-input v-model="formData.leader" placeholder="请输入负责人" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item field="sort" label="排序">
                  <a-input-number v-model="formData.sort" :min="0" placeholder="排序" style="width: 100%;" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
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
              <a-col :span="24">
                <a-form-item field="remark" label="备注">
                  <a-textarea v-model="formData.remark" placeholder="请输入备注" :auto-size="{ minRows: 2, maxRows: 4 }" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>

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
 * defineOptions({ name: 'SystemOrg' }) 是 keep-alive 缓存的关键。
 */
defineOptions({ name: 'SystemOrg' });

import { ref, reactive, computed, onMounted } from 'vue';
import { Message, Modal } from '@arco-design/web-vue';
import { IconPlus, IconEdit, IconDelete, IconMore, IconSettings, IconUp, IconDown } from '@arco-design/web-vue/es/icon';
import { fetchOrgList, createOrg, updateOrg, deleteOrg, type OrgVO, type OrgForm, type OrgQuery } from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import BusinessTableColumnSetting from '../../../../components/business/BusinessTableColumnSetting.vue';
import { useBusinessTableColumns, type BusinessTableColumn } from '../../../../composables/useBusinessTableColumns';

const ORG_TYPE_OPTIONS = [
  { value: 1, label: '集团', color: 'red' },
  { value: 2, label: '公司', color: 'arcoblue' },
  { value: 3, label: '分公司', color: 'cyan' },
  { value: 4, label: '子公司', color: 'purple' },
  { value: 5, label: '办事处', color: 'green' },
  { value: 6, label: '事业部', color: 'gold' },
];
const orgTypeLabel = (t: number) => ORG_TYPE_OPTIONS.find(o => o.value === t)?.label || '未知';
const orgTypeColor = (t: number) => ORG_TYPE_OPTIONS.find(o => o.value === t)?.color || 'gray';

const ISOLATION_OPTIONS = [
  { value: 0, label: '共享', desc: '上级机构可查看所有下级机构数据', color: 'arcoblue' },
  { value: 1, label: '完全隔离', desc: '各机构数据完全独立，上下级均不可互查', color: 'orangered' },
  { value: 2, label: '汇总共享', desc: '上级仅可查看下级的汇总统计，不可查看明细', color: 'gold' },
  { value: 3, label: '同级互通', desc: '同一父机构下的兄弟机构可互查数据', color: 'green' },
  { value: 4, label: '按模块隔离', desc: '部分业务模块隔离，部分共享（需配合模块配置）', color: 'purple' },
];
const isolationLabel = (v: number) => ISOLATION_OPTIONS.find(o => o.value === v)?.label || '未知';
const isolationColor = (v: number) => ISOLATION_OPTIONS.find(o => o.value === v)?.color || 'gray';

const defaultColumns: BusinessTableColumn[] = [
  { key: 'orgName', title: '机构名称', dataIndex: 'orgName', width: 220, visible: true },
  { key: 'orgCode', title: '机构编码', dataIndex: 'orgCode', width: 130, visible: true },
  { key: 'orgType', title: '机构类型', slotName: 'orgType', width: 100, visible: true, align: 'center' },
  { key: 'leader', title: '负责人', dataIndex: 'leader', width: 100, visible: true },
  { key: 'phone', title: '联系电话', dataIndex: 'phone', width: 130, visible: true },
  { key: 'dataIsolation', title: '数据隔离', slotName: 'dataIsolation', width: 110, visible: true, align: 'center' },
  { key: 'sort', title: '排序', dataIndex: 'sort', width: 70, visible: true, align: 'center' },
  { key: 'status', title: '状态', slotName: 'status', width: 80, visible: true, align: 'center' },
  { key: 'createTime', title: '创建时间', dataIndex: 'createTime', width: 170, visible: true },
  { key: 'actions', title: '操作', slotName: 'actions', width: 140, visible: true, fixed: 'right', locked: true, align: 'center' },
];

const {
  visibleColumns, columnSettingItems, dragState,
  handleColumnResize, toggleColumnVisible, moveColumn, toggleColumnFixed,
  handleDragStart, handleDragOver, handleDrop, handleDragEnd, resetColumns,
} = useBusinessTableColumns('system-org', defaultColumns);

const textMatchMode = ref<'fuzzy' | 'exact'>('fuzzy');
const queryExpanded = ref(false);

const loading = ref(false);
const tableData = ref<OrgVO[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增机构');
const formRef = ref();

const queryParams = reactive<OrgQuery>({ orgName: '', orgCode: '', orgType: undefined, status: undefined });

const defaultForm = (): OrgForm => ({
  id: undefined, parentId: 0, orgName: '', orgCode: '', orgType: 2,
  creditCode: '', legalPerson: '', registeredCapital: undefined,
  establishDate: '', sort: 0, leader: '', phone: '', email: '',
  province: '', city: '', district: '', address: '',
  businessScope: '', status: 1, remark: '', dataIsolation: 0
});
const formData = reactive<OrgForm>(defaultForm());

const formRules = {
  orgName: [{ required: true, message: '请输入机构名称' }],
  orgCode: [{ required: true, message: '请输入机构编码' }],
  orgType: [{ required: true, message: '请选择机构类型' }],
  dataIsolation: [{ required: true, message: '请选择数据隔离模式' }],
};

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

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchOrgList(queryParams) as any;
    tableData.value = res.data || [];
  } catch { tableData.value = []; }
  finally { loading.value = false; }
};

const handleSearch = () => { loadData(); };
const handleReset = () => {
  queryParams.orgName = '';
  queryParams.orgCode = '';
  queryParams.orgType = undefined;
  queryParams.status = undefined;
  loadData();
};

const handleAdd = (parentId?: number) => {
  dialogTitle.value = '新增机构';
  Object.assign(formData, defaultForm());
  if (parentId !== undefined) formData.parentId = parentId;
  dialogVisible.value = true;
};

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

const confirmDelete = (record: OrgVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确认删除机构「${record.orgName}」吗？`,
    okButtonProps: { status: 'danger' },
    onOk: () => handleDelete(record.id),
  });
};

const handleDelete = async (id: number) => {
  try {
    const res = await deleteOrg(id) as any;
    const msg: string = res?.message || res?.msg || '';
    if (msg.includes('存在子机构')) {
      Message.warning('存在子机构，不允许删除');
      return;
    }
    Message.success('删除成功');
    loadData();
  } catch (err: any) {
    const msg: string = err?.response?.data?.message || err?.response?.data?.msg || err?.message || '';
    if (msg.includes('存在子机构')) {
      Message.warning('存在子机构，不允许删除');
    }
  }
};

onMounted(() => { loadData(); });
</script>

<style scoped>
.form-tabs :deep(.arco-tabs-content) {
  padding-top: 12px;
}
</style>
