<template>
  <!--
    角色数据授权弹窗组件
    ════════════════════════
    独立的数据权限配置弹窗，从角色编辑表单中拆分出来。
    支持选择数据范围类型，自定义模式下可指定机构/部门范围。

    使用方式：
      <RoleDataScopeDialog
        v-model:visible="dataScopeDialogVisible"
        :role-id="currentRoleId"
        :role-name="currentRoleName"
        @saved="handleSaved"
      />
  -->
  <BmlModal
    :visible="visible"
    :title="`数据授权 — ${roleName}`"
    :width="600"
    :height="480"
    :min-width="480"
    :min-height="380"
    :mask-closable="false"
    @close="handleClose"
  >
    <template #header-actions>
      <a-button @click="handleClose" :disabled="saving">取消</a-button>
      <a-button type="primary" :loading="saving" @click="handleSave">
        <template #icon><IconSave /></template>
        保存
      </a-button>
    </template>

    <a-form :model="formData" layout="vertical">
      <a-form-item field="dataScope" label="数据范围">
        <a-select v-model="formData.dataScope" placeholder="请选择数据范围">
          <a-option v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value" :value="ds.value">{{ ds.label }}</a-option>
        </a-select>
      </a-form-item>

      <!-- 自定义模式：选择机构范围 -->
      <a-form-item v-if="formData.dataScope === 7" label="自定义机构范围">
        <a-tree-select
          v-model="formData.customOrgIds"
          :data="orgTreeData"
          :field-names="{ key: 'id', title: 'orgName', children: 'children' }"
          multiple
          tree-checkable
          placeholder="请选择可访问的机构范围"
        />
      </a-form-item>

      <!-- 自定义模式：选择部门范围 -->
      <a-form-item v-if="formData.dataScope === 7" label="自定义部门范围">
        <a-tree-select
          v-model="formData.customDeptIds"
          :data="deptTreeData"
          :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
          multiple
          tree-checkable
          placeholder="请选择可访问的部门范围"
        />
      </a-form-item>

      <!-- 数据权限说明 -->
      <a-alert type="info" class="scope-hint">
        <template #title>数据权限说明</template>
        <div class="scope-desc">
          <p v-for="ds in DATA_SCOPE_OPTIONS" :key="ds.value">
            <a-tag size="small" :color="DS_COLOR[ds.value] || 'gray'">{{ ds.label }}</a-tag>
            <span> — {{ ds.desc }}</span>
          </p>
        </div>
      </a-alert>
    </a-form>
  </BmlModal>
</template>

<script lang="ts" setup>
/**
 * 角色数据授权弹窗组件
 *
 * 功能说明：
 *   1. 从后端获取角色当前的数据权限配置
 *   2. 支持选择数据范围类型（全部/本机构/本部门/自定义等）
 *   3. 自定义模式下可指定机构/部门范围
 *   4. 保存时仅更新数据权限相关字段，不影响其他角色配置
 *
 * 设计原则：
 *   - 独立弹窗，与功能授权、绑定用户并列
 *   - 入口从工具栏按钮触发，需先选中角色行
 */
import { ref, reactive, watch } from 'vue';
import { IconSave } from '@arco-design/web-vue/es/icon';
import { Message } from '@arco-design/web-vue';
import BmlModal from '../../../../components/BmlModal.vue';
import { fetchRoleDetail, updateRole, fetchOrgList, fetchDeptList, type RoleForm, type OrgVO, type DeptVO } from '../../../../api/system';

/* ──────────────────────────── Props & Emits ──────────────────────────── */

const props = defineProps<{
  visible: boolean;
  roleId?: number;
  roleName?: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'saved'): void;
}>();

/* ──────────────────────────── 常量 ──────────────────────────── */

const DATA_SCOPE_OPTIONS = [
  { value: 1, label: '全部数据', desc: '不受任何限制，可查看系统所有数据' },
  { value: 2, label: '所在机构及下级', desc: '可查看本机构及下级数据（受机构数据隔离模式影响，完全隔离的下级不可见）' },
  { value: 3, label: '仅本机构', desc: '只能查看本机构数据（若本机构设为同级互通，可看兄弟机构数据）' },
  { value: 4, label: '所在部门及下级', desc: '可查看本部门及所有下级部门数据' },
  { value: 5, label: '仅本部门', desc: '只能查看当前所属部门的数据' },
  { value: 6, label: '仅本人', desc: '只能查看自己创建的数据' },
  { value: 7, label: '自定义', desc: '管理员手动指定可访问的机构/部门范围' },
  { value: 8, label: '本人及下属', desc: '可查看自己及汇报链所有下属员工创建的数据' },
];

const DS_COLOR: Record<number, string> = { 1: 'red', 2: 'purple', 3: 'arcoblue', 4: 'cyan', 5: 'green', 6: 'orangered', 7: 'gold', 8: 'magenta' };

/* ──────────────────────────── 响应式状态 ──────────────────────────── */

const saving = ref(false);
const orgTreeData = ref<OrgVO[]>([]);
const deptTreeData = ref<DeptVO[]>([]);
/** 角色完整数据缓存（保存时需携带必填字段） */
const roleCache = ref<Record<string, any>>({});

const formData = reactive({
  dataScope: 1 as number,
  customOrgIds: [] as number[],
  customDeptIds: [] as number[],
});

/* ──────────────────────────── 数据加载 ──────────────────────────── */

const loadData = async () => {
  if (!props.roleId) return;
  try {
    // 并行加载角色详情、机构树、部门树
    const [roleRes, orgRes, deptRes] = await Promise.all([
      fetchRoleDetail(props.roleId) as any,
      fetchOrgList() as any,
      fetchDeptList() as any,
    ]);
    const detail = roleRes.data || {};
    roleCache.value = detail;
    formData.dataScope = detail.dataScope ?? 1;
    formData.customOrgIds = detail.customOrgIds || [];
    formData.customDeptIds = detail.customDeptIds || [];
    orgTreeData.value = orgRes.data || [];
    deptTreeData.value = deptRes.data || [];
  } catch {
    Message.error('加载数据权限失败');
  }
};

/* ──────────────────────────── 保存 ──────────────────────────── */

const handleSave = async () => {
  if (!props.roleId) return;
  saving.value = true;
  try {
    const payload: RoleForm = {
      id: props.roleId,
      roleName: roleCache.value.roleName,
      roleCode: roleCache.value.roleCode,
      sort: roleCache.value.sort,
      status: roleCache.value.status,
      remark: roleCache.value.remark,
      dataScope: formData.dataScope,
      customOrgIds: formData.dataScope === 7 ? formData.customOrgIds : [],
      customDeptIds: formData.dataScope === 7 ? formData.customDeptIds : [],
      menuIds: roleCache.value.menuIds,
      halfCheckMenuIds: roleCache.value.halfCheckMenuIds,
    };
    await updateRole(payload);
    Message.success('数据授权保存成功');
    emit('saved');
    handleClose();
  } catch {
    Message.error('数据授权保存失败');
  } finally {
    saving.value = false;
  }
};

/* ──────────────────────────── 关闭 ──────────────────────────── */

const handleClose = () => {
  emit('update:visible', false);
};

/* ──────────────────────────── 监听可见性 ──────────────────────────── */

watch(() => props.visible, (val) => {
  if (val) loadData();
});
</script>

<style scoped>
.scope-hint {
  margin-top: 16px;
}
.scope-desc p {
  margin: 4px 0;
  font-size: 12px;
  line-height: 1.8;
}
</style>
