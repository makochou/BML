<template>
  <!--
    角色权限分配弹窗（V2.5.0 重新设计）
    ════════════════════════════════════
    三面板联动设计：
      左面板 — 模块菜单（M 目录 + C 菜单），树形勾选，按侧边栏顺序显示
      中面板 — 功能按钮（B），按所属菜单分组，按钮名称对齐实际页面文案
      右面板 — 表单字段（F），按所属菜单分组，覆盖所有表单标签页的字段

    联动规则：
      1. 勾选 / 取消左面板的 C 菜单时，中面板与右面板自动刷新只显示已勾选模块下的 B / F 项
      2. 取消某个 C 菜单时，其下所有 B / F 的勾选状态也同步清除
      3. 保存时将全部已勾选节点的 ID 按 halfCheck 状态分别传给后端

    设计原则：
      - 组件完全解耦，通过 props / emits 与父页面通信
      - 内部不直接调用保存 API，仅通过 @save 事件抛出数据，由调用方决定如何保存
      - 弹窗尺寸加大至 1200x720，确保三面板内容充分展示
  -->
  <BmlModal
    :visible="visible"
    :title="`权限分配 — ${roleName}`"
    :width="1200"
    :height="720"
    :min-width="900"
    :min-height="560"
    :mask-closable="false"
    @close="handleClose"
  >
    <!-- 标题栏操作按钮 -->
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
            <span class="perm-panel__icon perm-panel__icon--modules">
              <IconApps />
            </span>
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
              <span class="perm-tree-node">{{ nodeData.menuName }}</span>
            </template>
          </a-tree>
        </div>
        <!-- 底部统计 -->
        <div class="perm-panel__footer">
          <span class="perm-stat">已选 <b>{{ checkedModuleCount }}</b> / {{ totalModuleCount }} 个模块</span>
        </div>
      </div>

      <!-- ═══ 中面板：功能按钮 ═══ -->
      <div class="perm-panel perm-panel--buttons">
        <div class="perm-panel__header">
          <div class="perm-panel__header-title">
            <span class="perm-panel__icon perm-panel__icon--buttons">
              <IconCommand />
            </span>
            <span class="perm-panel__title">功能按钮</span>
          </div>
          <div class="perm-panel__header-actions">
            <a-checkbox v-model="checkAllButtons" @change="onCheckAllButtons" class="perm-action-check">
              <span class="perm-action-text">全选</span>
            </a-checkbox>
          </div>
        </div>
        <!-- 搜索栏：输入关键词可实时过滤全部模块下的按钮权限 -->
        <div class="perm-panel__search">
          <a-input
            v-model="buttonSearch"
            placeholder="搜索按钮权限…"
            allow-clear
            size="small"
          >
            <template #prefix><IconSearch /></template>
          </a-input>
        </div>
        <div class="perm-panel__body">
          <template v-if="filteredButtonGroups.length > 0">
            <div
              v-for="group in filteredButtonGroups"
              :key="group.menuId"
              class="perm-group"
            >
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
            <IconSearch v-if="buttonSearch.trim()" class="perm-empty__icon" />
            <IconLeft v-else class="perm-empty__icon" />
            <span class="perm-empty__text">{{ buttonSearch.trim() ? '无匹配的按钮权限' : '请先在左侧勾选模块菜单' }}</span>
          </div>
        </div>
        <!-- 底部统计 -->
        <div class="perm-panel__footer">
          <span class="perm-stat">已选 <b>{{ checkedButtonIds.length }}</b> / {{ totalVisibleButtonCount }} 个按钮</span>
        </div>
      </div>

      <!-- ═══ 右面板：表单字段 ═══ -->
      <div class="perm-panel perm-panel--fields">
        <div class="perm-panel__header">
          <div class="perm-panel__header-title">
            <span class="perm-panel__icon perm-panel__icon--fields">
              <IconList />
            </span>
            <span class="perm-panel__title">表单字段</span>
          </div>
          <div class="perm-panel__header-actions">
            <a-checkbox v-model="checkAllFields" @change="onCheckAllFields" class="perm-action-check">
              <span class="perm-action-text">全选</span>
            </a-checkbox>
          </div>
        </div>
        <!-- 搜索栏：输入关键词可实时过滤全部模块下的字段权限 -->
        <div class="perm-panel__search">
          <a-input
            v-model="fieldSearch"
            placeholder="搜索字段权限…"
            allow-clear
            size="small"
          >
            <template #prefix><IconSearch /></template>
          </a-input>
        </div>
        <div class="perm-panel__body">
          <template v-if="filteredFieldGroups.length > 0">
            <div
              v-for="group in filteredFieldGroups"
              :key="group.menuId"
              class="perm-group"
            >
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
            <IconSearch v-if="fieldSearch.trim()" class="perm-empty__icon" />
            <IconLeft v-else class="perm-empty__icon" />
            <span class="perm-empty__text">{{ fieldSearch.trim() ? '无匹配的字段权限' : '请先在左侧勾选模块菜单' }}</span>
          </div>
        </div>
        <!-- 底部统计 -->
        <div class="perm-panel__footer">
          <span class="perm-stat">已选 <b>{{ checkedFieldIds.length }}</b> / {{ totalVisibleFieldCount }} 个字段</span>
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
import { IconSave, IconApps, IconCommand, IconList, IconLeft, IconSearch } from '@arco-design/web-vue/es/icon';
import { fetchPermissionData, fetchRoleDetail, updateRole, type RoleForm } from '../../../../api/system';
import { Message } from '@arco-design/web-vue';
import BmlModal from '../../../../components/BmlModal.vue';

/** 菜单项接口（对应后端 SysMenuVO 的扁平结构） */
interface MenuItem {
  id: number;
  parentId: number;
  menuName: string;
  menuType: string; // M=目录, C=菜单, B=按钮, F=字段
  path?: string;
  perms?: string;
  icon?: string;
  sort?: number;
  visible?: number;
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
/** 角色完整数据缓存（保存时需携带 roleName 等必填字段，避免后端校验失败） */
const roleDetailCache = ref<Record<string, any>>({});
/** 功能按钮面板搜索关键词 */
const buttonSearch = ref('');
/** 表单字段面板搜索关键词 */
const fieldSearch = ref('');

/** 业务系统在后端 sys_menu 中的隐藏根目录 path，授权展示时需要展开其子节点作为顶层模块。 */
const BUSINESS_SYSTEM_ROOT_PATH = 'system';
/** 业务系统根目录在授权弹窗中的展示名称，需要与业务侧侧边栏保持一致。 */
const BUSINESS_ORG_PERMISSION_TITLE = '组织与权限';

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

/** 已勾选的 C 菜单数量（左面板统计） */
const checkedModuleCount = computed(() => checkedCMenuIds.value.size);

/** 全部 C 菜单总数（左面板统计分母） */
const totalModuleCount = computed(() =>
  allMenus.value.filter(m => m.menuType === 'C').length
);

/**
 * 按搜索关键词过滤后的按钮分组（中面板实际渲染用）
 * 规则：关键词为空时返回全量可见分组；非空时按 menuName 模糊匹配，隐藏空分组
 */
const filteredButtonGroups = computed(() => {
  const kw = buttonSearch.value.trim();
  if (!kw) return visibleButtonGroups.value;
  return visibleButtonGroups.value
    .map(g => ({ ...g, items: g.items.filter(i => i.menuName.includes(kw)) }))
    .filter(g => g.items.length > 0);
});

/**
 * 按搜索关键词过滤后的字段分组（右面板实际渲染用）
 * 规则同上
 */
const filteredFieldGroups = computed(() => {
  const kw = fieldSearch.value.trim();
  if (!kw) return visibleFieldGroups.value;
  return visibleFieldGroups.value
    .map(g => ({ ...g, items: g.items.filter(i => i.menuName.includes(kw)) }))
    .filter(g => g.items.length > 0);
});

/** 当前可见按钮总数（中面板统计分母，跟随搜索过滤） */
const totalVisibleButtonCount = computed(() =>
  filteredButtonGroups.value.reduce((sum, g) => sum + g.items.length, 0)
);

/** 当前可见字段总数（右面板统计分母，跟随搜索过滤） */
const totalVisibleFieldCount = computed(() =>
  filteredFieldGroups.value.reduce((sum, g) => sum + g.items.length, 0)
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

    // 3) 构建模块菜单树（M + C），展示时隐藏业务根目录“系统管理”，与业务系统侧边栏保持一致。
    moduleTreeData.value = buildModuleTree(mcMenus);

    // 4) 按钮分组（按 parentId 分组，parentId 对应某个 C 菜单）
    allButtonGroups.value = buildGroups(bMenus, mcMenus);

    // 5) 字段分组
    allFieldGroups.value = buildGroups(fMenus, mcMenus);

    // 6) 加载角色已有权限
    if (props.roleId) {
      const detailRes = await fetchRoleDetail(props.roleId) as any;
      const detail = detailRes.data || {};
      roleDetailCache.value = detail;
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

/** 构建授权模块树：后端保留业务根目录用于权限关系，前端展示时按业务侧侧边栏分组重排。 */
const buildModuleTree = (items: MenuItem[]): MenuItem[] => {
  const roots = buildTree(items, 0);
  return flattenBusinessSystemRoot(roots);
};

/** 递归剥离业务系统隐藏根目录，避免授权树任何层级继续展示“系统管理”包裹节点。 */
const flattenBusinessSystemRoot = (nodes: MenuItem[]): MenuItem[] =>
  nodes.flatMap(node => {
    if (isBusinessSystemRoot(node)) {
      return buildBusinessTopModules(node);
    }
    return [{
      ...node,
      children: node.children?.length ? flattenBusinessSystemRoot(node.children) : node.children,
    }];
  });

/** 将隐藏业务根目录拆成“组织与权限 + 其他业务域目录”，确保授权弹窗顶层与登录侧边栏一致。 */
const buildBusinessTopModules = (root: MenuItem): MenuItem[] => {
  const children = root.children || [];
  const directMenus = children.filter(child => child.menuType === 'C');
  const domainDirectories = children.filter(child => child.menuType !== 'C');
  const topModules: MenuItem[] = [];

  if (directMenus.length > 0) {
    topModules.push({
      ...root,
      menuName: BUSINESS_ORG_PERMISSION_TITLE,
      children: directMenus,
    });
  }

  topModules.push(...domainDirectories);
  return topModules.map(node => ({
    ...node,
    children: node.children?.length ? flattenBusinessSystemRoot(node.children) : node.children,
  }));
};

/** 构建树形结构（从扁平列表），按 sort 字段排序 */
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

  // 递归排序子节点（按 sort 字段升序）
  const sortChildren = (nodes: MenuItem[]) => {
    nodes.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    nodes.forEach(n => { if (n.children?.length) sortChildren(n.children); });
  };
  sortChildren(roots);

  return roots;
};

/** 判断是否为业务系统隐藏根目录“系统管理”：只识别业务根身份，不依赖 parentId 或 children，兼容扁平数据和树节点。 */
const isBusinessSystemRoot = (menu?: MenuItem): boolean => {
  if (!menu || menu.menuType !== 'M') {
    return false;
  }
  const normalizedPath = (menu.path || '').replace(/^\/+/, '').toLowerCase();
  const normalizedName = (menu.menuName || '').trim();
  return normalizedPath === BUSINESS_SYSTEM_ROOT_PATH || normalizedName === '系统管理';
};

/** 从当前授权数据中定位业务系统隐藏根目录。 */
const findBusinessSystemRoot = (): MenuItem | undefined =>
  allMenus.value.find(menu => isBusinessSystemRoot(menu));

/** 保存授权前补齐隐藏业务根目录，避免普通角色登录时因缺少根节点导致业务菜单无法构建。 */
const ensureBusinessSystemRootForSave = (allCheckedIds: number[], halfCheckIds: Set<number>) => {
  const root = findBusinessSystemRoot();
  if (!root) {
    return;
  }

  const checkedSet = new Set(allCheckedIds);
  const selectedBusinessNodeCount = allMenus.value
    .filter(menu => menu.id !== root.id)
    .filter(menu => checkedSet.has(menu.id))
    .length;

  if (selectedBusinessNodeCount === 0) {
    const rootIndex = allCheckedIds.indexOf(root.id);
    if (rootIndex > -1) {
      allCheckedIds.splice(rootIndex, 1);
    }
    halfCheckIds.delete(root.id);
    return;
  }

  const businessModuleIds = allMenus.value
    .filter(menu => menu.id !== root.id && (menu.menuType === 'M' || menu.menuType === 'C'))
    .map(menu => menu.id);
  const allBusinessModulesChecked = businessModuleIds.length > 0
    && businessModuleIds.every(id => checkedSet.has(id));

  if (!allBusinessModulesChecked) {
    const rootIndex = allCheckedIds.indexOf(root.id);
    if (rootIndex > -1) {
      allCheckedIds.splice(rootIndex, 1);
    }
    halfCheckIds.add(root.id);
    return;
  }

  if (!checkedSet.has(root.id)) {
    halfCheckIds.add(root.id);
  }
};

/** 按 parentId 分组，生成 PermGroup[]，按父菜单 sort 排序 */
const buildGroups = (items: MenuItem[], parentMenus: MenuItem[]): PermGroup[] => {
  const parentMap = new Map<number, MenuItem>();
  parentMenus.forEach(m => parentMap.set(m.id, m));

  const groupMap = new Map<number, PermGroup>();
  items.forEach(item => {
    if (!groupMap.has(item.parentId)) {
      const parent = parentMap.get(item.parentId);
      groupMap.set(item.parentId, {
        menuId: item.parentId,
        menuName: parent?.menuName || `菜单#${item.parentId}`,
        items: [],
      });
    }
    groupMap.get(item.parentId)!.items.push(item);
  });

  // 按父菜单的 sort 字段排序分组
  const groups = Array.from(groupMap.values());
  groups.sort((a, b) => {
    const sortA = parentMap.get(a.menuId)?.sort ?? 999;
    const sortB = parentMap.get(b.menuId)?.sort ?? 999;
    return sortA - sortB;
  });

  // 每组内的 items 也按 sort 排序
  groups.forEach(g => g.items.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)));

  return groups;
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
    const halfCheckIds = new Set<number>();

    mMenus.forEach(mMenu => {
      // 该目录下的所有 C 菜单
      const childCMenus = allMenus.value.filter(
        m => m.parentId === mMenu.id && m.menuType === 'C'
      );
      if (childCMenus.length === 0) return;

      const checkedChildren = childCMenus.filter(c => checkedSet.has(c.id));
      if (checkedChildren.length > 0 && checkedChildren.length < childCMenus.length) {
        // 部分子菜单被选中 → M 目录为半选
        halfCheckIds.add(mMenu.id);
        // 从完全勾选中移除此 M 目录（它是半选，不应在 menuIds 中）
        const idx = allCheckedIds.indexOf(mMenu.id);
        if (idx > -1) allCheckedIds.splice(idx, 1);
      }
    });

    ensureBusinessSystemRootForSave(allCheckedIds, halfCheckIds);

    const formData: RoleForm = {
      id: props.roleId,
      roleName: roleDetailCache.value.roleName,
      roleCode: roleDetailCache.value.roleCode,
      sort: roleDetailCache.value.sort,
      dataScope: roleDetailCache.value.dataScope,
      status: roleDetailCache.value.status,
      remark: roleDetailCache.value.remark,
      customOrgIds: roleDetailCache.value.customOrgIds,
      customDeptIds: roleDetailCache.value.customDeptIds,
      menuIds: allCheckedIds,
      halfCheckMenuIds: [...halfCheckIds],
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
  buttonSearch.value = '';
  fieldSearch.value = '';
  roleDetailCache.value = {};
};

/* ──────────────────────────── 监听可见性 ──────────────────────────── */

watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => loadData());
  }
});
</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════════════════════════
 * 角色权限分配弹窗 — 三面板精美布局（V2.5.0 重新设计）
 *
 * 设计语言：
 *   - 卡片化面板，圆角 + 精细边框 + 微妙投影
 *   - 渐变色面板头部，区分三面板的视觉层次
 *   - 网格化的按钮/字段勾选项，紧凑且美观
 *   - 底部统计栏，实时反馈选择状态
 * ═══════════════════════════════════════════════════════════════════════════════ */

/* ── 三面板容器 ── */
.perm-container {
  display: grid;
  grid-template-columns: 260px 1fr 1fr;
  gap: 14px;
  height: 100%;
  min-height: 480px;
}

/* ── 单面板通用样式 ── */
.perm-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.2s ease;
}
.perm-panel:hover {
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.07);
}

/* ── 面板头部（渐变背景区分视觉层次） ── */
.perm-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
  flex-shrink: 0;
}
.perm-panel__header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.perm-panel__header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 面板图标圆点 */
.perm-panel__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
  font-size: 14px;
  color: #ffffff;
}
.perm-panel__icon--modules {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
.perm-panel__icon--buttons {
  background: linear-gradient(135deg, #2f6df6, #1f8bff);
}
.perm-panel__icon--fields {
  background: linear-gradient(135deg, #10b981, #14b8a6);
}

/* 面板标题文字 */
.perm-panel__title {
  font-weight: 700;
  font-size: 14px;
  color: #0f172a;
}

/* 头部操作 checkbox */
.perm-action-check {
  font-size: 12px;
}
.perm-action-text {
  font-size: 12px;
  color: #64748b;
}

/* ── 面板头部渐变色条（识别性装饰线） ── */
.perm-panel--modules .perm-panel__header {
  background: linear-gradient(180deg, rgba(99, 102, 241, 0.04), transparent);
}
.perm-panel--buttons .perm-panel__header {
  background: linear-gradient(180deg, rgba(47, 109, 246, 0.04), transparent);
}
.perm-panel--fields .perm-panel__header {
  background: linear-gradient(180deg, rgba(16, 185, 129, 0.04), transparent);
}

/* ── 面板搜索栏 ── */
.perm-panel__search {
  padding: 8px 12px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(248, 250, 252, 0.5);
  flex-shrink: 0;
}
.perm-panel__search :deep(.arco-input-wrapper) {
  border-radius: 8px;
  background: #ffffff;
  border-color: rgba(226, 232, 240, 0.8);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.perm-panel__search :deep(.arco-input-wrapper:hover) {
  border-color: rgba(47, 109, 246, 0.4);
}
.perm-panel__search :deep(.arco-input-wrapper.arco-input-focus) {
  border-color: #2f6df6;
  box-shadow: 0 0 0 2px rgba(47, 109, 246, 0.1);
}
.perm-panel__search :deep(.arco-input-prefix) {
  color: #94a3b8;
}

/* ── 面板内容区 ── */
.perm-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 10px 12px;
  min-height: 0;
}

/* 面板内容区滚动条 */
.perm-panel__body::-webkit-scrollbar {
  width: 6px;
}
.perm-panel__body::-webkit-scrollbar-track {
  background: transparent;
}
.perm-panel__body::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.4);
}
.perm-panel__body::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.6);
}

/* ── 面板底部统计栏 ── */
.perm-panel__footer {
  display: flex;
  align-items: center;
  padding: 8px 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(248, 250, 252, 0.8);
  flex-shrink: 0;
}
.perm-stat {
  font-size: 12px;
  color: #64748b;
}
.perm-stat b {
  color: #2f6df6;
  font-weight: 700;
}

/* ── 分组卡片 ── */
.perm-group {
  margin-bottom: 14px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 8px;
  overflow: hidden;
}
.perm-group:last-child {
  margin-bottom: 0;
}

/* 分组头部 */
.perm-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 12px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 1), rgba(241, 245, 249, 0.8));
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
}
.perm-group__name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.perm-group__check-label {
  font-size: 11px;
  color: #64748b;
}

/* 分组内容 */
.perm-group__body {
  padding: 10px 12px;
}

/* 网格布局：按钮 3 列 */
.perm-group__grid {
  display: grid !important;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px 12px;
}

/* 字段面板 2 列（字段名通常较长） */
.perm-group__grid--fields {
  grid-template-columns: repeat(2, 1fr);
}

/* 勾选项样式 */
.perm-check-item {
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s ease;
}
.perm-check-item:hover {
  background: rgba(47, 109, 246, 0.04);
}
.perm-check-label {
  font-size: 13px;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── 树节点样式 ── */
.perm-tree-node {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: #334155;
  padding: 2px 0;
}

/* ── 空状态提示 ── */
.perm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 200px;
  color: #94a3b8;
}
.perm-empty__icon {
  font-size: 28px;
  opacity: 0.5;
}
.perm-empty__text {
  font-size: 13px;
}

/* ── 左面板树组件微调 ── */
.perm-panel--modules :deep(.arco-tree-node) {
  padding: 2px 0;
}
.perm-panel--modules :deep(.arco-tree-node-title) {
  font-size: 13px;
}
.perm-panel--modules :deep(.arco-checkbox) {
  margin-right: 2px;
}

/* ── 响应式适配 ── */
@media (max-width: 1024px) {
  .perm-container {
    grid-template-columns: 220px 1fr 1fr;
    gap: 10px;
  }
  .perm-group__grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .perm-group__grid--fields {
    grid-template-columns: 1fr;
  }
}
</style>
