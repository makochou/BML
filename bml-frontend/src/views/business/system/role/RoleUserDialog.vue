<template>
  <!--
    角色绑定用户弹窗（V2.5.0）
    ────────────────────────────
    布局结构（上下分层）：
    ┌─────────────────────────────────────────────────┐
    │  顶部：面板标题行（待选用户 ← → 已绑定用户）          │
    ├────────────┬───┬────────────────────────────────┤
    │ 部门树侧栏 │搜 │ 待选用户表格   │分隔│ 已绑定用户表格 │
    │            │索 │               │线  │               │
    │            │栏 │               │    │               │
    ├────────────┴───┴───────────────┴────┴───────────────┤
    │  底部操作栏：左=待选分页+绑定  │  右=已绑定分页+解绑   │
    └─────────────────────────────────────────────────────┘
  -->
  <BmlModal
    v-model:visible="innerVisible"
    title="绑定用户"
    :width="1200"
    :height="700"
    :min-width="900"
    :min-height="520"
  >
    <!-- 标题栏副标题 -->
    <template #header-extra>
      <a-tag v-if="roleName" color="arcoblue" style="margin-left: 8px;" size="small">{{ roleName }}</a-tag>
    </template>

    <div ref="dialogRef" class="role-user-dialog">
      <!-- ═══════════════════════════════════════════════════
           主内容区域（flex: 1, 占满剩余高度）
           ═══════════════════════════════════════════════════ -->
      <div class="role-user-main">
        <!-- ── 左侧面板：待选用户（含部门树） ── -->
        <div class="role-user-panel role-user-panel--left">
          <div class="role-user-panel__header">
            <span class="role-user-panel__title">待选用户</span>
            <a-tag color="green" size="small">{{ unassignedPagination.total }}</a-tag>
          </div>

          <div class="role-user-panel__body">
            <!-- 部门树侧栏 -->
            <div class="dept-tree-sidebar">
              <a-input v-model="deptSearchKey" placeholder="搜索部门" allow-clear size="small" class="dept-tree-search">
                <template #prefix><icon-search /></template>
              </a-input>
              <div class="dept-tree-wrapper">
                <div
                  class="dept-tree-all-node"
                  :class="{ 'is-active': !selectedDeptId }"
                  @click="handleDeptSelect([])"
                >
                  <icon-apps style="margin-right: 4px;" />
                  全部部门
                </div>
                <a-tree
                  :data="filteredDeptTree"
                  :field-names="{ key: 'id', title: 'deptName', children: 'children' }"
                  :selected-keys="selectedDeptId ? [selectedDeptId] : []"
                  block-node
                  size="small"
                  @select="handleDeptSelect"
                />
              </div>
            </div>

            <!-- 用户列表区域 -->
            <div class="user-list-area">
              <!-- 搜索栏 -->
              <div class="role-user-panel__search">
                <a-input v-model="unassignedQuery.username" placeholder="账号/用户名" allow-clear size="small"
                  @press-enter="searchUnassigned" @clear="searchUnassigned" style="flex: 1; min-width: 80px;" />
                <a-input v-model="unassignedQuery.phone" placeholder="手机号" allow-clear size="small"
                  @press-enter="searchUnassigned" @clear="searchUnassigned" style="flex: 1; min-width: 80px;" />
                <a-button type="primary" size="small" @click="searchUnassigned">
                  <template #icon><icon-search /></template>
                </a-button>
              </div>

              <!-- 用户表格（flex:1 自动撑满） -->
              <a-table
                class="panel-table"
                :data="unassignedData"
                :loading="unassignedLoading"
                :pagination="false"
                :bordered="false"
                row-key="id"
                size="mini"
                stripe
                :row-selection="{ type: 'checkbox', showCheckedAll: true }"
                v-model:selectedKeys="unassignedSelectedKeys"
                :scroll="{ y: '100%' }"
              >
                <template #columns>
                  <a-table-column title="账号" data-index="username" :width="110" ellipsis />
                  <a-table-column title="用户名" data-index="nickname" :width="100" ellipsis />
                  <a-table-column title="手机号" data-index="phone" :width="120" ellipsis />
                  <a-table-column title="部门" data-index="deptName" :width="110" ellipsis />
                </template>
              </a-table>
            </div>
          </div>
        </div>

        <!-- ── 中间分隔线 ── -->
        <div class="role-user-divider">
          <icon-swap style="font-size: 20px; color: var(--color-text-3);" />
        </div>

        <!-- ── 右侧面板：已绑定用户 ── -->
        <div class="role-user-panel role-user-panel--right">
          <div class="role-user-panel__header">
            <span class="role-user-panel__title">已绑定用户</span>
            <a-tag color="arcoblue" size="small">{{ assignedPagination.total }}</a-tag>
          </div>

          <!-- 搜索栏 -->
          <div class="role-user-panel__search">
            <a-input v-model="assignedQuery.username" placeholder="账号/用户名" allow-clear size="small"
              @press-enter="searchAssigned" @clear="searchAssigned" style="flex: 1; min-width: 80px;" />
            <a-input v-model="assignedQuery.phone" placeholder="手机号" allow-clear size="small"
              @press-enter="searchAssigned" @clear="searchAssigned" style="flex: 1; min-width: 80px;" />
            <a-button type="primary" size="small" @click="searchAssigned">
              <template #icon><icon-search /></template>
            </a-button>
          </div>

          <!-- 用户表格（flex:1 自动撑满） -->
          <a-table
            class="panel-table"
            :data="assignedData"
            :loading="assignedLoading"
            :pagination="false"
            :bordered="false"
            row-key="id"
            size="mini"
            stripe
            :row-selection="{ type: 'checkbox', showCheckedAll: true }"
            v-model:selectedKeys="assignedSelectedKeys"
            :scroll="{ y: '100%' }"
          >
            <template #columns>
              <a-table-column title="账号" data-index="username" :width="110" ellipsis />
              <a-table-column title="用户名" data-index="nickname" :width="100" ellipsis />
              <a-table-column title="手机号" data-index="phone" :width="120" ellipsis />
              <a-table-column title="部门" data-index="deptName" :width="110" ellipsis />
            </template>
          </a-table>
        </div>
      </div>

      <!-- ═══════════════════════════════════════════════════
           底部操作栏（固定在弹窗底部，左右分区对应两面板）
           ═══════════════════════════════════════════════════ -->
      <div class="role-user-footer">
        <!-- 左侧：待选用户分页 + 绑定按钮 -->
        <div class="role-user-footer__section role-user-footer__section--left">
          <a-pagination
            v-model:current="unassignedPagination.current"
            v-model:page-size="unassignedPagination.pageSize"
            :total="unassignedPagination.total"
            size="mini"
            :page-size-options="[10, 20, 50]"
            show-total
            show-page-size
            @change="loadUnassignedUsers"
            @page-size-change="(size: number) => { unassignedPagination.pageSize = size; unassignedPagination.current = 1; loadUnassignedUsers(); }"
          />
          <a-button
            class="footer-btn footer-btn--bind"
            :disabled="unassignedSelectedKeys.length === 0"
            :loading="assigning"
            @click="handleAssign"
          >
            <template #icon><icon-right /></template>
            绑定选中 ({{ unassignedSelectedKeys.length }})
          </a-button>
        </div>

        <!-- 右侧：已绑定用户分页 + 解绑按钮 -->
        <div class="role-user-footer__section role-user-footer__section--right">
          <a-pagination
            v-model:current="assignedPagination.current"
            v-model:page-size="assignedPagination.pageSize"
            :total="assignedPagination.total"
            size="mini"
            :page-size-options="[10, 20, 50]"
            show-total
            show-page-size
            @change="loadAssignedUsers"
            @page-size-change="(size: number) => { assignedPagination.pageSize = size; assignedPagination.current = 1; loadAssignedUsers(); }"
          />
          <a-button
            class="footer-btn footer-btn--unbind"
            :disabled="assignedSelectedKeys.length === 0"
            :loading="unassigning"
            @click="handleUnassign"
          >
            <template #icon><icon-left /></template>
            解绑选中 ({{ assignedSelectedKeys.length }})
          </a-button>
        </div>
      </div>
    </div>
  </BmlModal>
</template>

<script lang="ts" setup>
/**
 * 角色绑定用户弹窗组件（V2.5.0）
 *
 * 功能说明：
 * 1. 左侧「待选用户」面板：部门树 + 用户列表，点击部门节点可过滤该部门下的用户
 * 2. 右侧「已绑定用户」面板：展示已绑定角色的用户，含部门/机构列
 * 3. 支持按账号/用户名/手机号搜索过滤
 * 4. 支持分页浏览用户
 * 5. 支持批量绑定和批量解绑操作
 *
 * @prop {boolean} visible  - 控制弹窗的显隐
 * @prop {number}  roleId   - 角色ID
 * @prop {string}  roleName - 角色名称（显示用）
 * @emit update:visible - 弹窗关闭时触发
 * @emit saved - 绑定/解绑操作成功后触发，通知父组件刷新数据
 */
import { ref, reactive, watch, computed, nextTick, onBeforeUnmount } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconSearch, IconLeft, IconRight, IconSwap, IconApps } from '@arco-design/web-vue/es/icon';
import {
  fetchAssignedUsers,
  fetchUnassignedUsers,
  assignUsersToRole,
  unassignUsersFromRole,
  fetchDeptList,
  type UserVO,
  type DeptVO,
} from '../../../../api/system';
import BmlModal from '../../../../components/BmlModal.vue';

/* ── Props & Emits ── */
const props = defineProps<{
  /** 弹窗显隐 */
  visible: boolean;
  /** 角色ID */
  roleId?: number;
  /** 角色名称（显示用） */
  roleName?: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'saved'): void;
}>();

/* ── 弹窗显隐双向绑定 ── */
const innerVisible = computed({
  get: () => props.visible,
  set: (v) => emit('update:visible', v),
});

/* ═══════════════════════════════════════════════════════════
   部门树
   ═══════════════════════════════════════════════════════════ */
const deptTreeData = ref<DeptVO[]>([]);
const deptSearchKey = ref('');
const selectedDeptId = ref<number | undefined>(undefined);

/** 加载部门树 */
const loadDeptTree = async () => {
  try {
    const res = await fetchDeptList() as any;
    deptTreeData.value = res.data || [];
  } catch {
    deptTreeData.value = [];
  }
};

/**
 * 递归过滤部门树：按关键词模糊匹配部门名称。
 * 若父节点匹配则保留全部子节点；若子节点匹配则保留父链路。
 */
const filterTree = (nodes: DeptVO[], keyword: string): DeptVO[] => {
  if (!keyword) return nodes;
  const lowerKey = keyword.toLowerCase();
  return nodes.reduce<DeptVO[]>((acc, node) => {
    const childMatch = node.children ? filterTree(node.children, keyword) : [];
    const selfMatch = node.deptName.toLowerCase().includes(lowerKey);
    if (selfMatch || childMatch.length > 0) {
      acc.push({ ...node, children: selfMatch ? (node.children || []) : childMatch });
    }
    return acc;
  }, []);
};

/** 过滤后的部门树数据 */
const filteredDeptTree = computed(() => filterTree(deptTreeData.value, deptSearchKey.value));

/** 部门树节点选中事件 */
const handleDeptSelect = (selectedKeys: (string | number)[]) => {
  selectedDeptId.value = selectedKeys.length > 0 ? Number(selectedKeys[0]) : undefined;
  unassignedPagination.current = 1;
  loadUnassignedUsers();
};

/* ═══════════════════════════════════════════════════════════
   待选用户（未绑定）
   ═══════════════════════════════════════════════════════════ */
const unassignedLoading = ref(false);
const unassignedData = ref<UserVO[]>([]);
const unassignedSelectedKeys = ref<number[]>([]);
const unassignedQuery = reactive({ username: '', phone: '' });
const unassignedPagination = reactive({ current: 1, pageSize: 10, total: 0 });

/** 加载未绑定用户列表 */
const loadUnassignedUsers = async () => {
  if (!props.roleId) return;
  unassignedLoading.value = true;
  try {
    const res = await fetchUnassignedUsers(props.roleId, {
      username: unassignedQuery.username || undefined,
      phone: unassignedQuery.phone || undefined,
      deptId: selectedDeptId.value || undefined,
      pageNum: unassignedPagination.current,
      pageSize: unassignedPagination.pageSize,
    }) as any;
    const data = res.data || {};
    unassignedData.value = data.records || [];
    unassignedPagination.total = data.total || 0;
  } catch {
    unassignedData.value = [];
    unassignedPagination.total = 0;
  } finally {
    unassignedLoading.value = false;
  }
};

/** 搜索未绑定用户 */
const searchUnassigned = () => {
  unassignedPagination.current = 1;
  loadUnassignedUsers();
};

/* ═══════════════════════════════════════════════════════════
   已绑定用户
   ═══════════════════════════════════════════════════════════ */
const assignedLoading = ref(false);
const assignedData = ref<UserVO[]>([]);
const assignedSelectedKeys = ref<number[]>([]);
const assignedQuery = reactive({ username: '', phone: '' });
const assignedPagination = reactive({ current: 1, pageSize: 10, total: 0 });

/** 加载已绑定用户列表 */
const loadAssignedUsers = async () => {
  if (!props.roleId) return;
  assignedLoading.value = true;
  try {
    const res = await fetchAssignedUsers(props.roleId, {
      username: assignedQuery.username || undefined,
      phone: assignedQuery.phone || undefined,
      pageNum: assignedPagination.current,
      pageSize: assignedPagination.pageSize,
    }) as any;
    const data = res.data || {};
    assignedData.value = data.records || [];
    assignedPagination.total = data.total || 0;
  } catch {
    assignedData.value = [];
    assignedPagination.total = 0;
  } finally {
    assignedLoading.value = false;
  }
};

/** 搜索已绑定用户 */
const searchAssigned = () => {
  assignedPagination.current = 1;
  loadAssignedUsers();
};

/* ═══════════════════════════════════════════════════════════
   绑定 / 解绑操作
   ═══════════════════════════════════════════════════════════ */
const assigning = ref(false);
const unassigning = ref(false);

/** 批量绑定选中的用户到当前角色 */
const handleAssign = async () => {
  if (!props.roleId || unassignedSelectedKeys.value.length === 0) return;
  assigning.value = true;
  try {
    await assignUsersToRole(props.roleId, unassignedSelectedKeys.value);
    Message.success(`成功绑定 ${unassignedSelectedKeys.value.length} 个用户`);
    unassignedSelectedKeys.value = [];
    assignedSelectedKeys.value = [];
    loadAssignedUsers();
    loadUnassignedUsers();
    emit('saved');
  } catch { /* 保持状态 */ }
  finally { assigning.value = false; }
};

/** 批量解绑选中的用户 */
const handleUnassign = async () => {
  if (!props.roleId || assignedSelectedKeys.value.length === 0) return;
  unassigning.value = true;
  try {
    await unassignUsersFromRole(props.roleId, assignedSelectedKeys.value);
    Message.success(`成功解绑 ${assignedSelectedKeys.value.length} 个用户`);
    assignedSelectedKeys.value = [];
    unassignedSelectedKeys.value = [];
    loadAssignedUsers();
    loadUnassignedUsers();
    emit('saved');
  } catch { /* 保持状态 */ }
  finally { unassigning.value = false; }
};

/* ── 弹窗打开时加载数据 ── */
watch(() => props.visible, (val) => {
  if (val && props.roleId) {
    // 重置状态
    assignedQuery.username = '';
    assignedQuery.phone = '';
    unassignedQuery.username = '';
    unassignedQuery.phone = '';
    deptSearchKey.value = '';
    selectedDeptId.value = undefined;
    assignedPagination.current = 1;
    unassignedPagination.current = 1;
    assignedSelectedKeys.value = [];
    unassignedSelectedKeys.value = [];
    // 加载数据
    loadDeptTree();
    loadAssignedUsers();
    loadUnassignedUsers();
  }

  /**
   * 动态为父级 .bml-modal__body 添加/移除 fill 类。
   * 开启时：body 变为 flex column + overflow hidden，使本组件撑满全部高度，
   *        footer 固定贴底；
   * 关闭时：恢复 BmlModal 默认的 overflow-y: auto 行为。
   */
  nextTick(() => {
    const bodyEl = dialogRef.value?.parentElement;
    if (bodyEl?.classList.contains('bml-modal__body')) {
      if (val) {
        bodyEl.classList.add('bml-modal__body--fill');
      } else {
        bodyEl.classList.remove('bml-modal__body--fill');
      }
    }
  });
});

/** 组件卸载时确保清理父级 class */
onBeforeUnmount(() => {
  const bodyEl = dialogRef.value?.parentElement;
  if (bodyEl) bodyEl.classList.remove('bml-modal__body--fill');
});

/** 弹窗根元素引用（用于定位父级 .bml-modal__body） */
const dialogRef = ref<HTMLElement | null>(null);
</script>

<style scoped>
/* ══════════════════════════════════════════════════════════
   整体对话框容器（纵向 flex：主内容 + 底部操作栏）
   ══════════════════════════════════════════════════════════ */
/**
 * 弹窗根容器：纵向 flex 布局（main 区 + footer 区）。
 * flex: 1 确保在 BmlModal body（已覆盖为 flex column）中
 * 撑满全部可用高度，footer 始终贴底。
 */
.role-user-dialog {
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

/* ── 主内容区（两面板 + 分隔线并排，占满剩余高度） ── */
.role-user-main {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ── 单面板 ── */
.role-user-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.role-user-panel--left {
  flex: 6;
}
.role-user-panel--right {
  flex: 4;
}

/* ── 面板标题 ── */
.role-user-panel__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px 8px;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-1);
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
}

/* ── 左侧面板主体（部门树 + 用户列表并排） ── */
.role-user-panel__body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ══════════════════════════════════════════════════════════
   部门树侧栏
   ══════════════════════════════════════════════════════════ */
.dept-tree-sidebar {
  width: 160px;
  min-width: 120px;
  max-width: 160px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border-2);
  padding: 8px 0 0 0;
  overflow: hidden;
}
.dept-tree-search {
  margin: 0 6px 6px 6px;
  /* 严格限制在侧栏内，不溢出 */
  max-width: calc(100% - 12px);
  box-sizing: border-box;
}
.dept-tree-wrapper {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 2px;
}

/* 「全部部门」节点 */
.dept-tree-all-node {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  margin: 0 4px 2px 4px;
  border-radius: 4px;
  font-size: 13px;
  color: var(--color-text-2);
  cursor: pointer;
  transition: all 0.2s;
}
.dept-tree-all-node:hover {
  background: var(--color-fill-2);
  color: var(--color-text-1);
}
.dept-tree-all-node.is-active {
  background: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
  font-weight: 600;
}

/* 树节点紧凑 */
:deep(.dept-tree-sidebar .arco-tree-node) {
  padding: 0 4px;
}
:deep(.dept-tree-sidebar .arco-tree-node-title) {
  font-size: 12px;
}

/* ── 用户列表区域（待选面板右侧） ── */
.user-list-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── 搜索栏 ── */
.role-user-panel__search {
  display: flex;
  gap: 6px;
  padding: 8px 6px;
  align-items: center;
  flex-shrink: 0;
}

/* ── 表格自动撑满 ── */
.panel-table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
/* 确保 Arco 表格内部容器也撑满 */
:deep(.panel-table .arco-table-container) {
  height: 100%;
}
:deep(.panel-table .arco-table-body) {
  flex: 1;
  min-height: 0;
}

/* ── 中间分隔线 ── */
.role-user-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  flex-shrink: 0;
}

/* ══════════════════════════════════════════════════════════
   底部操作栏（固定在弹窗最下方）
   ────────────────────────────
   采用浅灰背景底条，与上方内容区明确分离，
   绑定/解绑按钮采用填充色确保视觉突出。
   ══════════════════════════════════════════════════════════ */
.role-user-footer {
  display: flex;
  flex-shrink: 0;
  border-top: 2px solid #E5E6EB;
  background: #F7F8FA;
  padding: 12px 10px 10px;
  gap: 44px; /* 与中间分隔线宽度对齐 */
}

/* ── 左右分区 ── */
.role-user-footer__section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.role-user-footer__section--left {
  flex: 6;
}
.role-user-footer__section--right {
  flex: 4;
}

/**
 * 底部操作按钮通用样式
 * ─────────────────
 * 按钮采用大尺寸填充式设计（高度 36px、字号 14px、粗体），
 * 禁用态保留对应色系的浅色版本（而非灰色），
 * 确保按钮在任何状态下都一眼可辨。
 */
.footer-btn {
  height: 36px !important;
  padding: 0 20px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  border-radius: 6px !important;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.2s;
  letter-spacing: 0.5px;
}

/**
 * 绑定按钮（蓝色系）
 * ──────────────────
 * 正常态：#165DFF 蓝色填充白字（Arco Blue 6）
 * 禁用态：#BEDAFF 浅蓝底 + #6AA1FF 蓝色文字（保持蓝色辨识度）
 */
.footer-btn--bind {
  color: #fff !important;
  background: #165DFF !important;
  border-color: #165DFF !important;
  box-shadow: 0 2px 6px rgba(22, 93, 255, 0.35);
}
.footer-btn--bind:hover:not(:disabled) {
  background: #0E42D2 !important;
  border-color: #0E42D2 !important;
  box-shadow: 0 4px 10px rgba(22, 93, 255, 0.45);
}
.footer-btn--bind:disabled {
  color: #6AA1FF !important;
  background: #BEDAFF !important;
  border-color: #94BFFF !important;
  box-shadow: none;
  cursor: not-allowed;
}

/**
 * 解绑按钮（红色系）
 * ──────────────────
 * 正常态：#F53F3F 红色填充白字（Arco Red 6）
 * 禁用态：#FDCDC5 浅红底 + #F76965 红色文字（保持红色辨识度）
 */
.footer-btn--unbind {
  color: #fff !important;
  background: #F53F3F !important;
  border-color: #F53F3F !important;
  box-shadow: 0 2px 6px rgba(245, 63, 63, 0.35);
}
.footer-btn--unbind:hover:not(:disabled) {
  background: #CB2634 !important;
  border-color: #CB2634 !important;
  box-shadow: 0 4px 10px rgba(245, 63, 63, 0.45);
}
.footer-btn--unbind:disabled {
  color: #F76965 !important;
  background: #FDCDC5 !important;
  border-color: #FBACA3 !important;
  box-shadow: none;
  cursor: not-allowed;
}

/* ── 覆盖 mini 表格行高 ── */
:deep(.arco-table-size-mini .arco-table-td) {
  padding: 4px 8px !important;
}
:deep(.arco-table-size-mini .arco-table-th) {
  padding: 6px 8px !important;
}
</style>

<!--
  非 scoped 样式块：BmlModal body 填充模式
  ─────────────────────────────────────────
  当 .bml-modal__body 被动态添加 --fill 类后，
  body 变为 flex column 且不再滚动，
  让内部的 .role-user-dialog 撑满全部可用高度，
  footer 固定贴在弹窗最底部。
  此方式不依赖 CSS :has()，兼容所有主流浏览器。
-->
<style>
.bml-modal__body--fill {
  overflow: hidden !important;
  display: flex !important;
  flex-direction: column !important;
}
</style>
