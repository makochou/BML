<template>
  <!--
    用户个人功能授权弹窗
    ════════════════════════════════════
    复用角色权限分配的三面板联动设计：
      左面板 — 模块菜单（M 目录 + C 菜单），树形勾选
      中面板 — 功能按钮（B），按所属菜单分组
      右面板 — 表单字段（F），按所属菜单分组

    与角色权限分配的区别：
      - 数据来源：sys_user_menu 表（而非 sys_role_menu）
      - 保存接口：POST /system/user/{userId}/assignMenus
      - 加载接口：GET /system/user/{userId}/menuIds
      - 权限面板数据复用：GET /system/menu/permissionData
  -->
  <BmlModal
    :visible="visible"
    :title="`功能授权 — ${userName}`"
    :width="1200"
    :height="720"
    :min-width="900"
    :min-height="560"
    :mask-closable="false"
    @close="handleClose"
  >
    <template #header-actions>
      <a-button @click="handleClose" :disabled="saving">取消</a-button>
      <a-button type="primary" :loading="saving" @click="handleSave">
        <template #icon><IconSave /></template>
        保存授权
      </a-button>
    </template>

    <!-- 三面板主体 -->
    <div class="perm-container">
      <!-- ═══ 左面板：模块菜单树 ═══ -->
      <div class="perm-panel perm-panel--modules">
        <div class="perm-panel__header">
          <div class="perm-panel__header-title">
            <span class="perm-panel__icon perm-panel__icon--modules"><IconApps /></span>
            <span class="perm-panel__title">模块菜单</span>
          </div>
          <div class="perm-panel__header-actions">
            <a-checkbox v-model="expandAll" @change="onExpandChange" class="perm-action-check">
              <span class="perm-action-text">展开</span>
            </a-checkbox>
            <a-checkbox v-model="checkAllModules" @change="onCheckAllModules" class="perm-action-check">
              <span class="perm-action-text">全选</span>
            </a-checkbox>
          </div>
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
              <span
                class="perm-tree-node"
                :class="{ 'perm-tree-node--focused': focusedMenuId === nodeData.id }"
                @click.stop="onNodeTitleClick(nodeData.id)"
              >{{ nodeData.menuName }}</span>
            </template>
          </a-tree>
        </div>
        <div class="perm-panel__footer">
          <span class="perm-stat">已选 <b>{{ checkedModuleCount }}</b> / {{ totalModuleCount }} 个模块</span>
        </div>
      </div>

      <!-- ═══ 中面板：功能按钮 ═══ -->
      <div class="perm-panel perm-panel--buttons">
        <div class="perm-panel__header">
          <div class="perm-panel__header-title">
            <span class="perm-panel__icon perm-panel__icon--buttons"><IconCommand /></span>
            <span class="perm-panel__title">功能按钮</span>
          </div>
          <div class="perm-panel__header-actions">
            <a-checkbox v-model="checkAllButtons" @change="onCheckAllButtons" class="perm-action-check">
              <span class="perm-action-text">全选</span>
            </a-checkbox>
          </div>
        </div>
        <div class="perm-panel__search">
          <a-input v-model="buttonSearch" placeholder="搜索按钮权限…" allow-clear size="small" />
        </div>
        <div class="perm-panel__body">
          <template v-if="filteredButtonGroups.length > 0">
            <div v-for="group in filteredButtonGroups" :key="group.menuId" class="perm-group">
              <div class="perm-group__header">
                <span class="perm-group__name">{{ group.menuName }}</span>
                <a-checkbox
                  :model-value="isGroupAllChecked('button', group.menuId)"
                  :indeterminate="isGroupIndeterminate('button', group.menuId)"
                  @change="(val: boolean | (string | number | boolean)[]) => onGroupCheckAll('button', group.menuId, val)"
                  class="perm-action-check"
                >
                  <span class="perm-group__check-label">全选</span>
                </a-checkbox>
              </div>
              <div class="perm-group__body">
                <a-checkbox-group v-model="checkedButtonIds" class="perm-group__grid">
                  <a-checkbox v-for="btn in group.items" :key="btn.id" :value="btn.id" class="perm-check-item">
                    <span class="perm-check-label">{{ btn.menuName }}</span>
                  </a-checkbox>
                </a-checkbox-group>
              </div>
            </div>
          </template>
          <div v-else class="perm-empty">
            <IconLeft class="perm-empty__icon" />
            <span class="perm-empty__text">{{ buttonSearch.trim() ? '无匹配的按钮权限' : '请先在左侧勾选模块菜单' }}</span>
          </div>
        </div>
        <div class="perm-panel__footer">
          <span class="perm-stat">已选 <b>{{ checkedButtonIds.length }}</b> 个按钮</span>
        </div>
      </div>

      <!-- ═══ 右面板：表单字段 ═══ -->
      <div class="perm-panel perm-panel--fields">
        <div class="perm-panel__header">
          <div class="perm-panel__header-title">
            <span class="perm-panel__icon perm-panel__icon--fields"><IconList /></span>
            <span class="perm-panel__title">表单字段</span>
          </div>
          <div class="perm-panel__header-actions">
            <a-checkbox v-model="checkAllFields" @change="onCheckAllFields" class="perm-action-check">
              <span class="perm-action-text">全选</span>
            </a-checkbox>
          </div>
        </div>
        <div class="perm-panel__search">
          <a-input v-model="fieldSearch" placeholder="搜索字段权限…" allow-clear size="small" />
        </div>
        <div class="perm-panel__body">
          <template v-if="filteredFieldGroups.length > 0">
            <div v-for="group in filteredFieldGroups" :key="group.menuId" class="perm-group">
              <div class="perm-group__header">
                <span class="perm-group__name">{{ group.menuName }}</span>
                <a-checkbox
                  :model-value="isGroupAllChecked('field', group.menuId)"
                  :indeterminate="isGroupIndeterminate('field', group.menuId)"
                  @change="(val: boolean | (string | number | boolean)[]) => onGroupCheckAll('field', group.menuId, val)"
                  class="perm-action-check"
                >
                  <span class="perm-group__check-label">全选</span>
                </a-checkbox>
              </div>
              <div class="perm-group__body">
                <a-checkbox-group v-model="checkedFieldIds" class="perm-group__grid perm-group__grid--fields">
                  <a-checkbox v-for="field in group.items" :key="field.id" :value="field.id" class="perm-check-item">
                    <span class="perm-check-label">{{ field.menuName }}</span>
                  </a-checkbox>
                </a-checkbox-group>
              </div>
            </div>
          </template>
          <div v-else class="perm-empty">
            <IconLeft class="perm-empty__icon" />
            <span class="perm-empty__text">{{ fieldSearch.trim() ? '无匹配的字段权限' : '请先在左侧勾选模块菜单' }}</span>
          </div>
        </div>
        <div class="perm-panel__footer">
          <span class="perm-stat">已选 <b>{{ checkedFieldIds.length }}</b> 个字段</span>
        </div>
      </div>
    </div>
  </BmlModal>
</template>

<script lang="ts" setup>
/**
 * 用户个人功能授权弹窗组件
 *
 * 功能说明：
 *   1. 从后端 /system/menu/permissionData 获取扁平菜单列表（复用角色授权数据源）
 *   2. 从后端 /system/user/{userId}/menuIds 获取该用户已有的个人权限
 *   3. 前端按 menuType 分为三类：M/C（模块菜单）、B（功能按钮）、F（表单字段）
 *   4. 三面板联动：取消左侧模块菜单后，中/右面板自动移除对应权限
 *   5. 保存时调用 POST /system/user/{userId}/assignMenus
 *
 * 使用方式：
 *   <UserPermissionDialog
 *     v-model:visible="userPermDialogVisible"
 *     :user-id="currentUserId"
 *     :user-name="currentUserName"
 *   />
 */

import { ref, computed, watch, nextTick } from 'vue';
import { IconSave, IconApps, IconCommand, IconList, IconLeft } from '@arco-design/web-vue/es/icon';
import { fetchPermissionData, fetchUserMenuIds, saveUserMenuIds } from '../../../../api/system';
import { Message } from '@arco-design/web-vue';
import BmlModal from '../../../../components/BmlModal.vue';

/** 菜单项接口 */
interface MenuItem {
  id: number;
  parentId: number;
  menuName: string;
  menuType: string;
  path?: string;
  perms?: string;
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
  userId?: number;
  userName?: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'saved'): void;
}>();

/* ──────────────────────────── 响应式状态 ──────────────────────────── */

const allMenus = ref<MenuItem[]>([]);
const moduleTreeData = ref<MenuItem[]>([]);
const allButtonGroups = ref<PermGroup[]>([]);
const allFieldGroups = ref<PermGroup[]>([]);

const checkedModuleIds = ref<number[]>([]);
const checkedButtonIds = ref<number[]>([]);
const checkedFieldIds = ref<number[]>([]);

const moduleTreeRef = ref();
const expandAll = ref(false);
const checkAllModules = ref(false);
const checkAllButtons = ref(false);
const checkAllFields = ref(false);
const saving = ref(false);
const buttonSearch = ref('');
const fieldSearch = ref('');
/**
 * 当前聚焦（点击选中）的树节点 ID。
 * 中面板和右面板仅显示该节点（或其子节点）对应的按钮/字段，
 * 实现左中右三面板的精确对齐联动。
 * 为 null 时显示所有已勾选模块的按钮/字段（兼容初始状态）。
 */
const focusedMenuId = ref<number | null>(null);

const BUSINESS_SYSTEM_ROOT_PATH = 'system';
const BUSINESS_ORG_PERMISSION_TITLE = '组织与权限';

/* ──────────────────────────── 计算属性 ──────────────────────────── */

const checkedCMenuIds = computed(() => {
  const cMenuIds = new Set(allMenus.value.filter(m => m.menuType === 'C').map(m => m.id));
  return new Set(checkedModuleIds.value.filter(id => cMenuIds.has(id)));
});

/**
 * 当前聚焦节点对应的 C 菜单 ID 集合。
 * - 若聚焦的是 C 菜单：仅包含该菜单自身
 * - 若聚焦的是 M 目录：包含其下所有 C 菜单子节点
 * - 若未聚焦任何节点：回退为所有已勾选的 C 菜单
 */
const focusedCMenuIds = computed((): Set<number> => {
  if (focusedMenuId.value == null) {
    return checkedCMenuIds.value;
  }
  const focused = allMenus.value.find(m => m.id === focusedMenuId.value);
  if (!focused) return checkedCMenuIds.value;
  if (focused.menuType === 'C') {
    return new Set([focused.id]);
  }
  // M 目录：收集其下所有 C 菜单
  const childCIds = allMenus.value
    .filter(m => m.parentId === focused.id && m.menuType === 'C')
    .map(m => m.id);
  return childCIds.length > 0 ? new Set(childCIds) : checkedCMenuIds.value;
});

const visibleButtonGroups = computed(() =>
  allButtonGroups.value.filter(g => focusedCMenuIds.value.has(g.menuId) && checkedCMenuIds.value.has(g.menuId))
);

const visibleFieldGroups = computed(() =>
  allFieldGroups.value.filter(g => focusedCMenuIds.value.has(g.menuId) && checkedCMenuIds.value.has(g.menuId))
);

const checkedModuleCount = computed(() => checkedCMenuIds.value.size);
const totalModuleCount = computed(() => allMenus.value.filter(m => m.menuType === 'C').length);

const filteredButtonGroups = computed(() => {
  const kw = buttonSearch.value.trim();
  if (!kw) return visibleButtonGroups.value;
  return visibleButtonGroups.value
    .map(g => ({ ...g, items: g.items.filter(i => i.menuName.includes(kw)) }))
    .filter(g => g.items.length > 0);
});

const filteredFieldGroups = computed(() => {
  const kw = fieldSearch.value.trim();
  if (!kw) return visibleFieldGroups.value;
  return visibleFieldGroups.value
    .map(g => ({ ...g, items: g.items.filter(i => i.menuName.includes(kw)) }))
    .filter(g => g.items.length > 0);
});

/* ──────────────────────────── 数据加载 ──────────────────────────── */

const loadData = async () => {
  try {
    const menuRes = await fetchPermissionData() as any;
    const menus: MenuItem[] = menuRes.data || [];
    allMenus.value = menus;

    const mcMenus = menus.filter(m => m.menuType === 'M' || m.menuType === 'C');
    const bMenus = menus.filter(m => m.menuType === 'B');
    const fMenus = menus.filter(m => m.menuType === 'F');

    moduleTreeData.value = buildModuleTree(mcMenus);
    allButtonGroups.value = buildGroups(bMenus, mcMenus);
    allFieldGroups.value = buildGroups(fMenus, mcMenus);

    // 加载用户已有的个人权限
    if (props.userId) {
      const res = await fetchUserMenuIds(props.userId) as any;
      const data = res.data || {};
      const savedMenuIds: number[] = data.menuIds || [];
      const savedHalfCheckIds: number[] = data.halfCheckMenuIds || [];
      const allSavedIds = new Set([...savedMenuIds, ...savedHalfCheckIds]);

      const mcIdSet = new Set(mcMenus.map(m => m.id));
      checkedModuleIds.value = savedMenuIds.filter(id => mcIdSet.has(id));

      const bIdSet = new Set(bMenus.map(m => m.id));
      checkedButtonIds.value = [...allSavedIds].filter(id => bIdSet.has(id));

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

/* ──────────────────────────── 树构建工具 ──────────────────────────── */

const buildModuleTree = (items: MenuItem[]): MenuItem[] => {
  const roots = buildTree(items);
  return flattenBusinessSystemRoot(roots);
};

const flattenBusinessSystemRoot = (nodes: MenuItem[]): MenuItem[] =>
  nodes.flatMap(node => {
    if (isBusinessSystemRoot(node)) {
      return buildBusinessTopModules(node);
    }
    return [{ ...node, children: node.children?.length ? flattenBusinessSystemRoot(node.children) : node.children }];
  });

const buildBusinessTopModules = (root: MenuItem): MenuItem[] => {
  const children = root.children || [];
  const directMenus = children.filter(child => child.menuType === 'C');
  const domainDirectories = children.filter(child => child.menuType !== 'C');
  const topModules: MenuItem[] = [];
  if (directMenus.length > 0) {
    topModules.push({ ...root, menuName: BUSINESS_ORG_PERMISSION_TITLE, children: directMenus });
  }
  topModules.push(...domainDirectories);
  return topModules.map(node => ({
    ...node,
    children: node.children?.length ? flattenBusinessSystemRoot(node.children) : node.children,
  }));
};

const buildTree = (items: MenuItem[]): MenuItem[] => {
  const map = new Map<number, MenuItem>();
  const roots: MenuItem[] = [];
  items.forEach(item => map.set(item.id, { ...item, children: [] }));
  map.forEach(item => {
    const parent = map.get(item.parentId);
    if (parent) { parent.children!.push(item); } else { roots.push(item); }
  });
  const sortChildren = (nodes: MenuItem[]) => {
    nodes.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    nodes.forEach(n => { if (n.children?.length) sortChildren(n.children); });
  };
  sortChildren(roots);
  return roots;
};

const isBusinessSystemRoot = (menu?: MenuItem): boolean => {
  if (!menu || menu.menuType !== 'M') return false;
  const normalizedPath = (menu.path || '').replace(/^\/+/, '').toLowerCase();
  const normalizedName = (menu.menuName || '').trim();
  return normalizedPath === BUSINESS_SYSTEM_ROOT_PATH || normalizedName === '系统管理';
};

const findBusinessSystemRoot = (): MenuItem | undefined =>
  allMenus.value.find(menu => isBusinessSystemRoot(menu));

/** 按 parentId 分组，生成 PermGroup[]，按左侧树中 C 菜单的实际显示顺序排序 */
const buildGroups = (items: MenuItem[], parentMenus: MenuItem[]): PermGroup[] => {
  const parentMap = new Map<number, MenuItem>();
  parentMenus.forEach(m => parentMap.set(m.id, m));
  const groupMap = new Map<number, PermGroup>();
  items.forEach(item => {
    if (!groupMap.has(item.parentId)) {
      const parent = parentMap.get(item.parentId);
      groupMap.set(item.parentId, { menuId: item.parentId, menuName: parent?.menuName || `菜单#${item.parentId}`, items: [] });
    }
    groupMap.get(item.parentId)!.items.push(item);
  });
  /*
   * 排序策略：按左侧模块树中 C 菜单的实际显示顺序排列。
   * 从 moduleTreeData（已经过 flattenBusinessSystemRoot 重组）中递归提取
   * 所有 C 菜单的 ID 顺序，确保中面板/右面板的分组顺序与左侧树严格一致。
   */
  const treeOrderMap = new Map<number, number>();
  let orderIndex = 0;
  const collectTreeOrder = (nodes: MenuItem[]) => {
    for (const node of nodes) {
      if (node.menuType === 'C') {
        treeOrderMap.set(node.id, orderIndex++);
      }
      if (node.children?.length) {
        collectTreeOrder(node.children);
      }
    }
  };
  collectTreeOrder(moduleTreeData.value);
  const groups = Array.from(groupMap.values());
  groups.sort((a, b) => (treeOrderMap.get(a.menuId) ?? 999) - (treeOrderMap.get(b.menuId) ?? 999));
  groups.forEach(g => g.items.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)));
  return groups;
};

/* ──────────────────────────── 面板联动 ──────────────────────────── */

const onModuleCheck = (checkedKeys: number[]) => {
  const prevCIds = checkedCMenuIds.value;
  const newCIds = new Set(checkedKeys.filter(id => allMenus.value.find(m => m.id === id && m.menuType === 'C')));
  const removedCIds = new Set([...prevCIds].filter(id => !newCIds.has(id)));
  if (removedCIds.size > 0) {
    const removedBIds = new Set(allButtonGroups.value.filter(g => removedCIds.has(g.menuId)).flatMap(g => g.items.map(i => i.id)));
    checkedButtonIds.value = checkedButtonIds.value.filter(id => !removedBIds.has(id));
    const removedFIds = new Set(allFieldGroups.value.filter(g => removedCIds.has(g.menuId)).flatMap(g => g.items.map(i => i.id)));
    checkedFieldIds.value = checkedFieldIds.value.filter(id => !removedFIds.has(id));
  }
  checkedModuleIds.value = checkedKeys;
};

/**
 * 模块树节点标题点击。
 * 点击某个节点的文字后，中面板和右面板仅显示该节点对应的按钮/字段，
 * 实现左中右三面板的精确对齐联动。
 * 再次点击同一节点则取消聚焦，恢复显示所有已勾选模块。
 */
const onNodeTitleClick = (nodeId: number) => {
  if (focusedMenuId.value === nodeId) {
    focusedMenuId.value = null;
  } else {
    focusedMenuId.value = nodeId;
  }
};

const onExpandChange = (val: boolean | (string | number | boolean)[]) => {
  const expand = val === true || (Array.isArray(val) && val.includes(true));
  moduleTreeRef.value?.expandAll(expand);
};

const onCheckAllModules = (val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  if (check) {
    checkedModuleIds.value = allMenus.value.filter(m => m.menuType === 'M' || m.menuType === 'C').map(m => m.id);
  } else {
    checkedModuleIds.value = [];
    checkedButtonIds.value = [];
    checkedFieldIds.value = [];
  }
};

const onCheckAllButtons = (val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  checkedButtonIds.value = check ? visibleButtonGroups.value.flatMap(g => g.items.map(i => i.id)) : [];
};

const onCheckAllFields = (val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  checkedFieldIds.value = check ? visibleFieldGroups.value.flatMap(g => g.items.map(i => i.id)) : [];
};

/* ──────────────────────── 分组全选辅助 ──────────────────────── */

const isGroupAllChecked = (type: 'button' | 'field', menuId: number): boolean => {
  const groups = type === 'button' ? allButtonGroups.value : allFieldGroups.value;
  const checkedIds = type === 'button' ? checkedButtonIds.value : checkedFieldIds.value;
  const group = groups.find(g => g.menuId === menuId);
  if (!group || group.items.length === 0) return false;
  const checkedSet = new Set(checkedIds);
  return group.items.every(item => checkedSet.has(item.id));
};

const isGroupIndeterminate = (type: 'button' | 'field', menuId: number): boolean => {
  const groups = type === 'button' ? allButtonGroups.value : allFieldGroups.value;
  const checkedIds = type === 'button' ? checkedButtonIds.value : checkedFieldIds.value;
  const group = groups.find(g => g.menuId === menuId);
  if (!group || group.items.length === 0) return false;
  const checkedSet = new Set(checkedIds);
  const checkedCount = group.items.filter(item => checkedSet.has(item.id)).length;
  return checkedCount > 0 && checkedCount < group.items.length;
};

const onGroupCheckAll = (type: 'button' | 'field', menuId: number, val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  const groups = type === 'button' ? allButtonGroups.value : allFieldGroups.value;
  const group = groups.find(g => g.menuId === menuId);
  if (!group) return;
  const groupItemIds = new Set(group.items.map(i => i.id));
  if (type === 'button') {
    checkedButtonIds.value = check
      ? [...checkedButtonIds.value.filter(id => !groupItemIds.has(id)), ...groupItemIds]
      : checkedButtonIds.value.filter(id => !groupItemIds.has(id));
  } else {
    checkedFieldIds.value = check
      ? [...checkedFieldIds.value.filter(id => !groupItemIds.has(id)), ...groupItemIds]
      : checkedFieldIds.value.filter(id => !groupItemIds.has(id));
  }
};

/* ──────────────────────────── 保存 ──────────────────────────── */

const handleSave = async () => {
  if (!props.userId) return;
  saving.value = true;
  try {
    const allCheckedIds = [...checkedModuleIds.value, ...checkedButtonIds.value, ...checkedFieldIds.value];

    // 计算 M 类型目录的半选状态
    const mMenus = allMenus.value.filter(m => m.menuType === 'M');
    const checkedSet = new Set(allCheckedIds);
    const halfCheckIds = new Set<number>();

    mMenus.forEach(mMenu => {
      const childCMenus = allMenus.value.filter(m => m.parentId === mMenu.id && m.menuType === 'C');
      if (childCMenus.length === 0) return;
      const checkedChildren = childCMenus.filter(c => checkedSet.has(c.id));
      if (checkedChildren.length > 0 && checkedChildren.length < childCMenus.length) {
        halfCheckIds.add(mMenu.id);
        const idx = allCheckedIds.indexOf(mMenu.id);
        if (idx > -1) allCheckedIds.splice(idx, 1);
      }
    });

    // 确保业务系统根目录被包含
    const root = findBusinessSystemRoot();
    if (root && allCheckedIds.length > 0) {
      if (!checkedSet.has(root.id) && !halfCheckIds.has(root.id)) {
        halfCheckIds.add(root.id);
      }
    }

    await saveUserMenuIds(props.userId, {
      menuIds: allCheckedIds,
      halfCheckMenuIds: [...halfCheckIds],
    });
    Message.success('用户功能授权保存成功');
    emit('saved');
    handleClose();
  } catch {
    Message.error('保存失败');
  } finally {
    saving.value = false;
  }
};

/* ──────────────────────────── 关闭 ──────────────────────────── */

const handleClose = () => {
  emit('update:visible', false);
  checkedModuleIds.value = [];
  checkedButtonIds.value = [];
  checkedFieldIds.value = [];
  expandAll.value = false;
  checkAllModules.value = false;
  checkAllButtons.value = false;
  checkAllFields.value = false;
  buttonSearch.value = '';
  fieldSearch.value = '';
  focusedMenuId.value = null;
};

/* ──────────────────────────── 监听可见性 ──────────────────────────── */

watch(() => props.visible, (val) => {
  if (val) { nextTick(() => loadData()); }
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════════════════════════
 * 用户功能授权弹窗 — 三面板布局（复用角色权限分配设计语言）
 * ═══════════════════════════════════════════════════════════════════════════════ */

.perm-container {
  display: grid;
  grid-template-columns: 260px 1fr 1fr;
  gap: 14px;
  height: 100%;
  min-height: 480px;
}

.perm-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}
.perm-panel:hover { box-shadow: 0 4px 16px rgba(15, 23, 42, 0.07); }

.perm-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
  flex-shrink: 0;
}
.perm-panel__header-title { display: flex; align-items: center; gap: 8px; }
.perm-panel__header-actions { display: flex; align-items: center; gap: 10px; }
.perm-panel__title { font-size: 13px; font-weight: 700; color: #1e293b; }

.perm-panel__icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 26px; height: 26px; border-radius: 7px; font-size: 14px; color: #ffffff;
}
.perm-panel__icon--modules { background: linear-gradient(135deg, #6366f1, #8b5cf6); }
.perm-panel__icon--buttons { background: linear-gradient(135deg, #f59e0b, #f97316); }
.perm-panel__icon--fields { background: linear-gradient(135deg, #10b981, #14b8a6); }

.perm-panel__search { padding: 8px 12px; border-bottom: 1px solid rgba(226, 232, 240, 0.5); flex-shrink: 0; }

.perm-panel__body { flex: 1; overflow-y: auto; padding: 10px 12px; min-height: 0; }

.perm-panel__footer {
  padding: 8px 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.7);
  flex-shrink: 0;
  background: rgba(248, 250, 252, 0.8);
}
.perm-stat { font-size: 11px; color: #64748b; font-weight: 600; }
.perm-stat b { color: #165dff; }

.perm-action-check { font-size: 12px; }
.perm-action-text { font-size: 11px; color: #64748b; font-weight: 600; }

.perm-group { margin-bottom: 12px; }
.perm-group__header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 10px; background: rgba(248, 250, 252, 0.9);
  border-radius: 8px; margin-bottom: 6px;
}
.perm-group__name { font-size: 12px; font-weight: 700; color: #334155; }
.perm-group__check-label { font-size: 10px; color: #94a3b8; }
.perm-group__body { padding: 0 4px; }
.perm-group__grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 4px 8px; }
.perm-group__grid--fields { grid-template-columns: repeat(3, 1fr); }

.perm-check-item { font-size: 12px; }
.perm-check-label { font-size: 12px; color: #475569; }

.perm-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; gap: 8px; color: #94a3b8;
}
.perm-empty__icon { font-size: 32px; opacity: 0.4; }
.perm-empty__text { font-size: 12px; font-weight: 500; }

/* ── 树节点聚焦高亮（点击文字后标记当前聚焦节点） ── */
.perm-tree-node {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.15s, color 0.15s;
}
.perm-tree-node:hover {
  background: rgba(22, 93, 255, 0.06);
}
.perm-tree-node--focused {
  background: rgba(22, 93, 255, 0.12);
  color: #165dff;
  font-weight: 600;
}
</style>
