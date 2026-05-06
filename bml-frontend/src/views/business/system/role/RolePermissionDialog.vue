<template>
  <!--
    角色权限分配弹窗
    ==================
    三面板联动设计：
      左面板 — 模块菜单（M 目录 + C 菜单），树形勾选，仅显示菜单名称，不展示类型标签
      中面板 — 功能按钮（B），按所属菜单分组，仅显示按钮名称（隐藏权限标识），每组带独立全选
      右面板 — 表单字段（F），按所属菜单分组，仅显示字段名称（隐藏权限标识），每组带独立全选

    联动规则：
      1. 勾选 / 取消左面板的 C 菜单时，中面板与右面板自动刷新只显示已勾选模块下的 B / F 项
      2. 取消某个 C 菜单时，其下所有 B / F 的勾选状态也同步清除
      3. 保存时将全部已勾选节点的 ID 按 halfCheck 状态分别传给后端

    设计原则：
      - 组件完全解耦，通过 props / emits 与父页面通信
      - 内部不直接调用保存 API，仅通过 @save 事件抛出数据，由调用方决定如何保存
  -->
  <BmlModal
    :visible="visible"
    :title="`权限分配 — ${roleName}`"
    :width="960"
    :mask-closable="false"
    :esc-to-close="!saving"
    @cancel="handleClose"
  >
    <!-- 弹窗底部操作栏 -->
    <template #footer>
      <a-space>
        <a-button @click="handleClose" :disabled="saving">取消</a-button>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><IconSave /></template>
          保存授权
        </a-button>
      </a-space>
    </template>

    <!-- 三面板主体 -->
    <div class="perm-panels">
      <!-- ═══ 左面板：模块菜单树 ═══ -->
      <div class="perm-panel perm-panel--left">
        <div class="perm-panel__header">
          <span class="perm-panel__title">模块菜单</span>
          <a-space :size="12">
            <a-checkbox v-model="expandAll" @change="onExpandChange">
              <span class="perm-panel__action-text">展开</span>
            </a-checkbox>
            <a-checkbox v-model="checkAllModules" @change="onCheckAllModules">
              <span class="perm-panel__action-text">全选</span>
            </a-checkbox>
          </a-space>
        </div>
        <div class="perm-panel__body">
          <a-tree
            ref="moduleTreeRef"
            :data="moduleTreeData"
            :field-names="{ key: 'id', title: 'menuName', children: 'children' }"
            checkable
            :checked-keys="checkedModuleIds"
            @check="onModuleCheck"
            block-node
          >
            <template #title="nodeData">
              <span class="perm-node">{{ nodeData.menuName }}</span>
            </template>
          </a-tree>
        </div>
      </div>

      <!-- ═══ 中面板：功能按钮 ═══ -->
      <div class="perm-panel perm-panel--center">
        <div class="perm-panel__header">
          <span class="perm-panel__title">功能按钮</span>
          <a-checkbox v-model="checkAllButtons" @change="onCheckAllButtons">
            <span class="perm-panel__action-text">全选</span>
          </a-checkbox>
        </div>
        <div class="perm-panel__body">
          <template v-if="visibleButtonGroups.length > 0">
            <div
              v-for="group in visibleButtonGroups"
              :key="group.menuId"
              class="perm-group"
            >
              <div class="perm-group__title">
                <span>{{ group.menuName }}</span>
                <a-checkbox
                  :model-value="isGroupAllChecked('button', group.menuId)"
                  :indeterminate="isGroupIndeterminate('button', group.menuId)"
                  @change="(val: boolean | (string | number | boolean)[]) => onGroupCheckAll('button', group.menuId, val)"
                >
                  <span class="perm-group__check-text">全选</span>
                </a-checkbox>
              </div>
              <a-checkbox-group v-model="checkedButtonIds" class="perm-group__items">
                <a-checkbox v-for="btn in group.items" :key="btn.id" :value="btn.id">
                  <span class="perm-item">
                    <span class="perm-item__name">{{ btn.menuName }}</span>
                  </span>
                </a-checkbox>
              </a-checkbox-group>
            </div>
          </template>
          <a-empty v-else description="请先在左侧勾选模块菜单" />
        </div>
      </div>

      <!-- ═══ 右面板：表单字段 ═══ -->
      <div class="perm-panel perm-panel--right">
        <div class="perm-panel__header">
          <span class="perm-panel__title">表单字段</span>
          <a-checkbox v-model="checkAllFields" @change="onCheckAllFields">
            <span class="perm-panel__action-text">全选</span>
          </a-checkbox>
        </div>
        <div class="perm-panel__body">
          <template v-if="visibleFieldGroups.length > 0">
            <div
              v-for="group in visibleFieldGroups"
              :key="group.menuId"
              class="perm-group"
            >
              <div class="perm-group__title">
                <span>{{ group.menuName }}</span>
                <a-checkbox
                  :model-value="isGroupAllChecked('field', group.menuId)"
                  :indeterminate="isGroupIndeterminate('field', group.menuId)"
                  @change="(val: boolean | (string | number | boolean)[]) => onGroupCheckAll('field', group.menuId, val)"
                >
                  <span class="perm-group__check-text">全选</span>
                </a-checkbox>
              </div>
              <a-checkbox-group v-model="checkedFieldIds" class="perm-group__items">
                <a-checkbox v-for="field in group.items" :key="field.id" :value="field.id">
                  <span class="perm-item">
                    <span class="perm-item__name">{{ field.menuName }}</span>
                  </span>
                </a-checkbox>
              </a-checkbox-group>
            </div>
          </template>
          <a-empty v-else description="请先在左侧勾选模块菜单" />
        </div>
      </div>
    </div>
  </BmlModal>
</template>

<script lang="ts" setup>
/**
 * 角色权限分配弹窗组件
 *
 * 功能说明：
 *   1. 从后端 /system/menu/permissionData 获取扁平菜单列表
 *   2. 前端按 menuType 分为三类：M/C（模块菜单）、B（功能按钮）、F（表单字段）
 *   3. 模块菜单以树形展示，B/F 按所属父菜单分组展示
 *   4. 三面板联动：取消左侧模块菜单后，中/右面板自动移除对应权限
 *
 * 使用方式：
 *   <RolePermissionDialog
 *     v-model:visible="permDialogVisible"
 *     :role-id="currentRoleId"
 *     :role-name="currentRoleName"
 *     @save="handlePermSave"
 *   />
 */

import { ref, computed, watch, nextTick } from 'vue';
import { IconSave } from '@arco-design/web-vue/es/icon';
import { fetchPermissionData, fetchRoleDetail, updateRole, type RoleForm } from '../../../../api/system';
import { Message } from '@arco-design/web-vue';
import BmlModal from '../../../../components/BmlModal.vue';

/** 菜单项接口（对应后端 SysMenuVO 的扁平结构） */
interface MenuItem {
  id: number;
  parentId: number;
  menuName: string;
  menuType: string; // M=目录, C=菜单, B=按钮, F=字段
  perms?: string;
  icon?: string;
  sort?: number;
  children?: MenuItem[];
}

/** 按钮/字段分组结构 */
interface PermGroup {
  menuId: number;
  menuName: string;
  items: MenuItem[];
}

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

/* ──────────────────────────── 响应式状态 ──────────────────────────── */

/** 全部菜单原始扁平列表 */
const allMenus = ref<MenuItem[]>([]);
/** 模块树数据（M + C，组装成树形） */
const moduleTreeData = ref<MenuItem[]>([]);
/** 全部 B 类型按钮（按 parentId 分组） */
const allButtonGroups = ref<PermGroup[]>([]);
/** 全部 F 类型字段（按 parentId 分组） */
const allFieldGroups = ref<PermGroup[]>([]);

/** 左面板：已勾选的模块菜单 ID（M + C） */
const checkedModuleIds = ref<number[]>([]);
/** 中面板：已勾选的按钮 ID */
const checkedButtonIds = ref<number[]>([]);
/** 右面板：已勾选的字段 ID */
const checkedFieldIds = ref<number[]>([]);

/** 树组件引用 */
const moduleTreeRef = ref();
/** 展开全部 */
const expandAll = ref(false);
/** 全选模块 */
const checkAllModules = ref(false);
/** 全选按钮 */
const checkAllButtons = ref(false);
/** 全选字段 */
const checkAllFields = ref(false);
/** 保存中状态 */
const saving = ref(false);

/* ──────────────────────────── 计算属性 ──────────────────────────── */

/** 当前勾选的叶子 C 菜单 ID 集合（仅 C 类型，用于过滤 B/F） */
const checkedCMenuIds = computed(() => {
  const cMenuIds = new Set(allMenus.value.filter(m => m.menuType === 'C').map(m => m.id));
  return new Set(checkedModuleIds.value.filter(id => cMenuIds.has(id)));
});

/** 中面板：仅显示已勾选模块下的按钮分组 */
const visibleButtonGroups = computed(() =>
  allButtonGroups.value.filter(g => checkedCMenuIds.value.has(g.menuId))
);

/** 右面板：仅显示已勾选模块下的字段分组 */
const visibleFieldGroups = computed(() =>
  allFieldGroups.value.filter(g => checkedCMenuIds.value.has(g.menuId))
);

/* ──────────────────────────── 数据加载 ──────────────────────────── */

/**
 * 加载权限面板数据
 * 1. 从后端获取扁平菜单列表
 * 2. 按 menuType 分类为模块树 / 按钮组 / 字段组
 * 3. 加载角色已有权限并回填勾选状态
 */
const loadData = async () => {
  try {
    // 1) 获取全量菜单扁平列表
    const menuRes = await fetchPermissionData() as any;
    const menus: MenuItem[] = menuRes.data || [];
    allMenus.value = menus;

    // 2) 分类
    const mcMenus = menus.filter(m => m.menuType === 'M' || m.menuType === 'C');
    const bMenus = menus.filter(m => m.menuType === 'B');
    const fMenus = menus.filter(m => m.menuType === 'F');

    // 3) 构建模块菜单树（M + C）
    moduleTreeData.value = buildTree(mcMenus, 0);

    // 4) 按钮分组（按 parentId 分组，parentId 对应某个 C 菜单）
    allButtonGroups.value = buildGroups(bMenus, mcMenus);

    // 5) 字段分组
    allFieldGroups.value = buildGroups(fMenus, mcMenus);

    // 6) 加载角色已有权限
    if (props.roleId) {
      const detailRes = await fetchRoleDetail(props.roleId) as any;
      const detail = detailRes.data || {};
      const savedMenuIds: number[] = detail.menuIds || [];
      const savedHalfCheckIds: number[] = detail.halfCheckMenuIds || [];
      const allSavedIds = new Set([...savedMenuIds, ...savedHalfCheckIds]);

      // 回填模块勾选（M + C 类型中被保存的 ID）
      const mcIdSet = new Set(mcMenus.map(m => m.id));
      checkedModuleIds.value = savedMenuIds.filter(id => mcIdSet.has(id));

      // 回填按钮勾选
      const bIdSet = new Set(bMenus.map(m => m.id));
      checkedButtonIds.value = [...allSavedIds].filter(id => bIdSet.has(id));

      // 回填字段勾选
      const fIdSet = new Set(fMenus.map(m => m.id));
      checkedFieldIds.value = [...allSavedIds].filter(id => fIdSet.has(id));
    }
  } catch {
    allMenus.value = [];
    moduleTreeData.value = [];
    allButtonGroups.value = [];
    allFieldGroups.value = [];
  }
};

/** 构建树形结构（从扁平列表） */
const buildTree = (items: MenuItem[], _rootParentId: number = 0): MenuItem[] => {
  const map = new Map<number, MenuItem>();
  const roots: MenuItem[] = [];

  // 深拷贝，避免修改原始数据
  items.forEach(item => map.set(item.id, { ...item, children: [] }));

  map.forEach(item => {
    const parent = map.get(item.parentId);
    if (parent) {
      parent.children!.push(item);
    } else {
      roots.push(item);
    }
  });

  return roots;
};

/** 按 parentId 分组，生成 PermGroup[] */
const buildGroups = (items: MenuItem[], parentMenus: MenuItem[]): PermGroup[] => {
  const parentMap = new Map<number, string>();
  parentMenus.forEach(m => parentMap.set(m.id, m.menuName));

  const groupMap = new Map<number, PermGroup>();
  items.forEach(item => {
    if (!groupMap.has(item.parentId)) {
      groupMap.set(item.parentId, {
        menuId: item.parentId,
        menuName: parentMap.get(item.parentId) || `菜单#${item.parentId}`,
        items: [],
      });
    }
    groupMap.get(item.parentId)!.items.push(item);
  });

  return Array.from(groupMap.values());
};

/* ──────────────────────────── 面板联动 ──────────────────────────── */

/**
 * 模块树勾选变更
 * 当取消某个 C 菜单的勾选时，自动清除其下所有 B/F 的勾选
 */
const onModuleCheck = (checkedKeys: number[]) => {
  // 找出被取消的 C 菜单
  const prevCIds = checkedCMenuIds.value;
  const newCIds = new Set(
    checkedKeys.filter(id =>
      allMenus.value.find(m => m.id === id && m.menuType === 'C')
    )
  );

  // 被移除的 C 菜单 ID
  const removedCIds = new Set([...prevCIds].filter(id => !newCIds.has(id)));

  // 清除被移除 C 菜单下的 B 勾选
  if (removedCIds.size > 0) {
    const removedBIds = new Set(
      allButtonGroups.value
        .filter(g => removedCIds.has(g.menuId))
        .flatMap(g => g.items.map(i => i.id))
    );
    checkedButtonIds.value = checkedButtonIds.value.filter(id => !removedBIds.has(id));

    // 清除被移除 C 菜单下的 F 勾选
    const removedFIds = new Set(
      allFieldGroups.value
        .filter(g => removedCIds.has(g.menuId))
        .flatMap(g => g.items.map(i => i.id))
    );
    checkedFieldIds.value = checkedFieldIds.value.filter(id => !removedFIds.has(id));
  }

  checkedModuleIds.value = checkedKeys;
};

/** 展开/折叠切换 */
const onExpandChange = (val: boolean | (string | number | boolean)[]) => {
  const expand = val === true || (Array.isArray(val) && val.includes(true));
  moduleTreeRef.value?.expandAll(expand);
};

/** 模块全选/全不选 */
const onCheckAllModules = (val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  if (check) {
    const allMcIds = allMenus.value
      .filter(m => m.menuType === 'M' || m.menuType === 'C')
      .map(m => m.id);
    checkedModuleIds.value = allMcIds;
  } else {
    checkedModuleIds.value = [];
    checkedButtonIds.value = [];
    checkedFieldIds.value = [];
  }
};

/** 按钮全选/全不选 */
const onCheckAllButtons = (val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  if (check) {
    checkedButtonIds.value = visibleButtonGroups.value.flatMap(g => g.items.map(i => i.id));
  } else {
    checkedButtonIds.value = [];
  }
};

/** 字段全选/全不选 */
const onCheckAllFields = (val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  if (check) {
    checkedFieldIds.value = visibleFieldGroups.value.flatMap(g => g.items.map(i => i.id));
  } else {
    checkedFieldIds.value = [];
  }
};

/* ──────────────────────── 分组全选辅助 ──────────────────────── */

/**
 * 判断指定分组下的项是否全部勾选
 * @param type - 'button' | 'field'，对应中面板 / 右面板
 * @param menuId - 分组所属的父菜单 ID
 */
const isGroupAllChecked = (type: 'button' | 'field', menuId: number): boolean => {
  const groups = type === 'button' ? allButtonGroups.value : allFieldGroups.value;
  const checkedIds = type === 'button' ? checkedButtonIds.value : checkedFieldIds.value;
  const group = groups.find(g => g.menuId === menuId);
  if (!group || group.items.length === 0) return false;
  const checkedSet = new Set(checkedIds);
  return group.items.every(item => checkedSet.has(item.id));
};

/**
 * 判断指定分组是否处于半选（部分勾选）状态
 * @param type - 'button' | 'field'
 * @param menuId - 分组所属的父菜单 ID
 */
const isGroupIndeterminate = (type: 'button' | 'field', menuId: number): boolean => {
  const groups = type === 'button' ? allButtonGroups.value : allFieldGroups.value;
  const checkedIds = type === 'button' ? checkedButtonIds.value : checkedFieldIds.value;
  const group = groups.find(g => g.menuId === menuId);
  if (!group || group.items.length === 0) return false;
  const checkedSet = new Set(checkedIds);
  const checkedCount = group.items.filter(item => checkedSet.has(item.id)).length;
  return checkedCount > 0 && checkedCount < group.items.length;
};

/**
 * 分组全选 / 全不选切换
 * @param type - 'button' | 'field'
 * @param menuId - 分组所属的父菜单 ID
 * @param val - checkbox 变更值
 */
const onGroupCheckAll = (
  type: 'button' | 'field',
  menuId: number,
  val: boolean | (string | number | boolean)[]
) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  const groups = type === 'button' ? allButtonGroups.value : allFieldGroups.value;
  const group = groups.find(g => g.menuId === menuId);
  if (!group) return;

  const groupItemIds = new Set(group.items.map(i => i.id));

  if (type === 'button') {
    if (check) {
      // 合并：保留已有 + 加入当前分组全部
      const existing = checkedButtonIds.value.filter(id => !groupItemIds.has(id));
      checkedButtonIds.value = [...existing, ...groupItemIds];
    } else {
      // 移除当前分组的全部
      checkedButtonIds.value = checkedButtonIds.value.filter(id => !groupItemIds.has(id));
    }
  } else {
    if (check) {
      const existing = checkedFieldIds.value.filter(id => !groupItemIds.has(id));
      checkedFieldIds.value = [...existing, ...groupItemIds];
    } else {
      checkedFieldIds.value = checkedFieldIds.value.filter(id => !groupItemIds.has(id));
    }
  }
};

/* ──────────────────────────── 保存 ──────────────────────────── */

/**
 * 收集全部已勾选 ID 并调用更新接口
 *
 * 规则：
 *   menuIds = 全部完全勾选的节点（模块 + 按钮 + 字段）
 *   halfCheckMenuIds = 树组件中的半选节点（由 a-tree 自动计算，此处手动计算：
 *     M 目录如果部分子 C 被勾选则为半选）
 */
const handleSave = async () => {
  if (!props.roleId) return;
  saving.value = true;
  try {
    // 所有完全勾选的叶子 ID（C + B + F）
    const allCheckedIds = [
      ...checkedModuleIds.value,
      ...checkedButtonIds.value,
      ...checkedFieldIds.value,
    ];

    // 计算 M 类型目录的半选状态
    const mMenus = allMenus.value.filter(m => m.menuType === 'M');
    const checkedSet = new Set(allCheckedIds);
    const halfCheckIds: number[] = [];

    mMenus.forEach(mMenu => {
      // 该目录下的所有 C 菜单
      const childCMenus = allMenus.value.filter(
        m => m.parentId === mMenu.id && m.menuType === 'C'
      );
      if (childCMenus.length === 0) return;

      const checkedChildren = childCMenus.filter(c => checkedSet.has(c.id));
      if (checkedChildren.length > 0 && checkedChildren.length < childCMenus.length) {
        // 部分子菜单被选中 → M 目录为半选
        halfCheckIds.push(mMenu.id);
        // 从完全勾选中移除此 M 目录（它是半选，不应在 menuIds 中）
        const idx = allCheckedIds.indexOf(mMenu.id);
        if (idx > -1) allCheckedIds.splice(idx, 1);
      }
    });

    const formData: RoleForm = {
      id: props.roleId,
      menuIds: allCheckedIds,
      halfCheckMenuIds: halfCheckIds,
    };

    await updateRole(formData);
    Message.success('权限分配成功');
    emit('saved');
    handleClose();
  } catch {
    Message.error('权限分配失败');
  } finally {
    saving.value = false;
  }
};

/* ──────────────────────────── 关闭 ──────────────────────────── */

const handleClose = () => {
  emit('update:visible', false);
  // 重置状态
  checkedModuleIds.value = [];
  checkedButtonIds.value = [];
  checkedFieldIds.value = [];
  expandAll.value = false;
  checkAllModules.value = false;
  checkAllButtons.value = false;
  checkAllFields.value = false;
};

/* ──────────────────────────── 监听可见性 ──────────────────────────── */

watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => loadData());
  }
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════════
   角色权限分配弹窗 — 三面板布局
   ═══════════════════════════════════════════════════════════════ */

/* 三面板容器：左中右等分布局 */
.perm-panels {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  min-height: 420px;
}

/* 单个面板通用样式 */
.perm-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  overflow: hidden;
  background: var(--color-bg-1);
}

/* 面板头部 */
.perm-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--color-fill-1);
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
}
.perm-panel__title {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-1);
}
.perm-panel__action-text {
  font-size: 12px;
}

/* 面板内容区（可滚动） */
.perm-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  min-height: 0;
}

/* 分组标题 */
.perm-group {
  margin-bottom: 12px;
}
.perm-group:last-child {
  margin-bottom: 0;
}
.perm-group__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-2);
  padding: 4px 8px;
  margin-bottom: 4px;
  background: var(--color-fill-1);
  border-radius: 4px;
}
.perm-group__check-text {
  font-size: 11px;
  font-weight: 400;
}
.perm-group__items {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-left: 4px;
}

/* 树节点样式 */
.perm-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

/* 单项权限样式 */
.perm-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.perm-item__name {
  font-size: 13px;
  color: var(--color-text-1);
}
</style>
