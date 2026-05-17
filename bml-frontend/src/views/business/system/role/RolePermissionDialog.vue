<template>
  <!--
    角色功能授权弹窗（V2.18.0 优化）
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
      - 入口从表格操作列移至工具栏，需先选中角色行再点击"功能授权"按钮
  -->
  <BmlModal
    :visible="visible"
    :title="`功能授权 — ${roleName}`"
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
              <span
                class="perm-tree-node"
                :class="{ 'perm-tree-node--focused': focusedMenuId === nodeData.id }"
                @click.stop="onNodeTitleClick(nodeData.id)"
              >{{ nodeData.menuName }}</span>
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
            <a-checkbox v-model="expandAllButtons" @change="onExpandAllButtons" class="perm-action-check">
              <span class="perm-action-text">展开</span>
            </a-checkbox>
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
          <template v-if="groupedButtonsByDirectory.length > 0">
            <div
              v-for="dir in groupedButtonsByDirectory"
              :key="dir.directoryId"
              class="perm-directory"
            >
              <!-- M 目录级标题（可展开/收起） -->
              <div class="perm-directory__header" @click="toggleDirectory('button', dir.directoryId)">
                <span class="perm-directory__arrow" :class="{ 'perm-directory__arrow--open': isDirectoryExpanded('button', dir.directoryId) }"></span>
                <span class="perm-directory__name">{{ dir.directoryName }}</span>
              </div>
              <!-- C 菜单级分组 -->
              <div v-show="isDirectoryExpanded('button', dir.directoryId)" class="perm-directory__body">
                <div
                  v-for="group in dir.children"
                  :key="group.menuId"
                  class="perm-group"
                >
                  <div class="perm-group__header perm-group__header--collapsible" @click="toggleGroup('button', group.menuId)">
                    <span class="perm-group__arrow" :class="{ 'perm-group__arrow--open': isGroupExpanded('button', group.menuId) }"></span>
                    <span class="perm-group__name">{{ group.menuName }}</span>
                    <a-checkbox
                      :model-value="isGroupAllChecked('button', group.menuId)"
                      :indeterminate="isGroupIndeterminate('button', group.menuId)"
                      @change="(val: boolean | (string | number | boolean)[]) => onGroupCheckAll('button', group.menuId, val)"
                      class="perm-action-check"
                      @click.stop
                    >
                      <span class="perm-group__check-label">全选</span>
                    </a-checkbox>
                  </div>
                  <div v-show="isGroupExpanded('button', group.menuId)" class="perm-group__body">
                    <a-checkbox-group v-model="checkedButtonIds" class="perm-group__grid">
                      <a-checkbox v-for="btn in group.items" :key="btn.id" :value="btn.id" class="perm-check-item">
                        <span class="perm-check-label">{{ btn.menuName }}</span>
                      </a-checkbox>
                    </a-checkbox-group>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-if="groupedButtonsByDirectory.length === 0" class="perm-empty">
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
            <a-checkbox v-model="expandAllFields" @change="onExpandAllFields" class="perm-action-check">
              <span class="perm-action-text">展开</span>
            </a-checkbox>
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
          <template v-if="groupedFieldsByDirectory.length > 0">
            <div
              v-for="dir in groupedFieldsByDirectory"
              :key="dir.directoryId"
              class="perm-directory"
            >
              <!-- M 目录级标题（可展开/收起） -->
              <div class="perm-directory__header" @click="toggleDirectory('field', dir.directoryId)">
                <span class="perm-directory__arrow" :class="{ 'perm-directory__arrow--open': isDirectoryExpanded('field', dir.directoryId) }"></span>
                <span class="perm-directory__name">{{ dir.directoryName }}</span>
              </div>
              <!-- C 菜单级分组 -->
              <div v-show="isDirectoryExpanded('field', dir.directoryId)" class="perm-directory__body">
                <div
                  v-for="group in dir.children"
                  :key="group.menuId"
                  class="perm-group"
                >
                  <div class="perm-group__header perm-group__header--collapsible" @click="toggleGroup('field', group.menuId)">
                    <span class="perm-group__arrow" :class="{ 'perm-group__arrow--open': isGroupExpanded('field', group.menuId) }"></span>
                    <span class="perm-group__name">{{ group.menuName }}</span>
                    <a-checkbox
                      :model-value="isGroupAllChecked('field', group.menuId)"
                      :indeterminate="isGroupIndeterminate('field', group.menuId)"
                      @change="(val: boolean | (string | number | boolean)[]) => onGroupCheckAll('field', group.menuId, val)"
                      class="perm-action-check"
                      @click.stop
                    >
                      <span class="perm-group__check-label">全选</span>
                    </a-checkbox>
                  </div>
                  <div v-show="isGroupExpanded('field', group.menuId)" class="perm-group__body">
                    <!-- 按标签页二级分组展示字段（可展开/收起） -->
                    <div v-for="tab in group.tabs" :key="tab.tabName" class="perm-tab-node">
                      <div class="perm-tab-node__header" @click="toggleFieldTab(group.menuId, tab.tabName)">
                        <span class="perm-tab-node__arrow" :class="{ 'perm-tab-node__arrow--open': isFieldTabExpanded(group.menuId, tab.tabName) }"></span>
                        <span class="perm-tab-node__name">{{ tab.tabName }}</span>
                        <a-checkbox
                          :model-value="isTabAllChecked(tab.items)"
                          :indeterminate="isTabIndeterminate(tab.items)"
                          @change="(val: boolean | (string | number | boolean)[]) => onTabCheckAll(tab.items, val)"
                          class="perm-action-check perm-action-check--tab"
                          @click.stop
                        >
                          <span class="perm-group__check-label">全选</span>
                        </a-checkbox>
                      </div>
                      <div v-show="isFieldTabExpanded(group.menuId, tab.tabName)" class="perm-tab-node__body">
                        <a-checkbox-group v-model="checkedFieldIds" class="perm-group__grid perm-group__grid--fields">
                          <a-checkbox v-for="field in tab.items" :key="field.id" :value="field.id" class="perm-check-item">
                            <span class="perm-check-label">{{ field.menuName }}</span>
                          </a-checkbox>
                        </a-checkbox-group>
                      </div>
                    </div>
                  </div>
                </div>
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
 * 角色功能授权弹窗组件
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
 *     @saved="handlePermSaved"
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
  remark?: string;
  children?: MenuItem[];
}

/** 按钮/字段分组结构 */
interface PermGroup {
  menuId: number;
  menuName: string;
  items: MenuItem[];
}

/**
 * 字段分组结构（支持标签页二级分组）
 * 每个模块下的字段按标签页（如"基本信息"、"工商信息"）进行二级分组展示，
 * 标签信息从数据库 remark 字段中解析（格式：模块名 — 标签名 — 字段名）。
 */
interface FieldTabGroup {
  tabName: string;
  items: MenuItem[];
}

interface FieldModuleGroup {
  menuId: number;
  menuName: string;
  tabs: FieldTabGroup[];
  /** 所有字段的扁平列表（用于全选/统计） */
  allItems: MenuItem[];
}

/**
 * 模块目录分组结构（用于中面板/右面板按 M 目录分组展示）
 * 与左面板的模块菜单树结构对应：
 *   组织与权限
 *   ├── 机构管理
 *   ├── 部门管理
 *   └── ...
 */
interface ModuleDirectoryGroup<T> {
  /** M 目录的 ID（如"组织与权限"的 ID） */
  directoryId: number;
  /** M 目录的显示名称 */
  directoryName: string;
  /** 该目录下的 C 菜单分组列表 */
  children: T[];
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
/** 全部 B 类型按钮（按 parentId 分组，不含查询按钮） */
const allButtonGroups = ref<PermGroup[]>([]);
/** 全部 F 类型字段（按 parentId 分组，含标签页二级分组） */
const allFieldGroups = ref<FieldModuleGroup[]>([]);

/** 左面板：已勾选的模块菜单 ID（M + C） */
const checkedModuleIds = ref<number[]>([]);
/** 中面板：已勾选的按钮 ID */
const checkedButtonIds = ref<number[]>([]);
/** 右面板：已勾选的字段 ID */
const checkedFieldIds = ref<number[]>([]);

/**
 * 查询按钮缓存（perms 以 ':query' 结尾的 B 类型按钮）
 * 这些按钮不在功能按钮面板中展示，保存时根据模块勾选状态自动补入。
 * 设计原则：勾选了模块菜单即隐含拥有该模块的查询权限。
 */
const queryButtonsCache = ref<MenuItem[]>([]);

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
/** 展开全部按钮分组 */
const expandAllButtons = ref(false);
/** 展开全部字段分组 */
const expandAllFields = ref(false);
/** 保存中状态 */
const saving = ref(false);
/** 角色完整数据缓存（保存时需携带 roleName 等必填字段，避免后端校验失败） */
const roleDetailCache = ref<Record<string, any>>({});
/** 功能按钮面板搜索关键词 */
const buttonSearch = ref('');
/** 表单字段面板搜索关键词 */
const fieldSearch = ref('');
/**
 * 字段面板标签页展开状态
 * key 格式为 "menuId:tabName"，存在于 Set 中表示已展开
 * 默认收起，点击标签页头部可展开/收起
 */
const collapsedFieldTabs = ref<Set<string>>(new Set());

/**
 * 功能按钮/表单字段面板一级分组（模块级）的收起状态
 * key 格式为 "button:menuId" 或 "field:menuId"
 * 默认收起（即 Set 中不存储任何 key，isGroupExpanded 返回 false）
 * 展开后将 key 加入 expandedGroups
 */
const expandedGroups = ref<Set<string>>(new Set());
/**
 * 当前聚焦（点击选中）的树节点 ID。
 * 中面板和右面板仅显示该节点（或其子节点）对应的按钮/字段，
 * 实现左中右三面板的精确对齐联动。
 * 为 null 时显示所有已勾选模块的按钮/字段（兼容初始状态）。
 */
const focusedMenuId = ref<number | null>(null);
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

/**
 * 当前聚焦节点对应的 C 菜单 ID 集合。
 * - 若聚焦的是 C 菜单：仅包含该菜单自身
 * - 若聚焦的是 M 目录：包含其下所有 C 菜单子节点
 * - 若未聚焦任何节点：回退为所有已勾选的 C 菜单（兼容初始状态）
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

/** 中面板：仅显示当前聚焦模块下的按钮分组（已勾选的才显示） */
const visibleButtonGroups = computed(() =>
  allButtonGroups.value.filter(g => focusedCMenuIds.value.has(g.menuId) && checkedCMenuIds.value.has(g.menuId))
);

/** 右面板：仅显示当前聚焦模块下的字段分组（已勾选的才显示） */
const visibleFieldGroups = computed(() =>
  allFieldGroups.value.filter(g => focusedCMenuIds.value.has(g.menuId) && checkedCMenuIds.value.has(g.menuId))
);

/** 右面板：用于兼容旧的 PermGroup 接口（全选/统计等辅助函数使用） */
const visibleFieldGroupsFlat = computed((): PermGroup[] =>
  visibleFieldGroups.value.map(g => ({ menuId: g.menuId, menuName: g.menuName, items: g.allItems }))
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
 * 支持标签页二级分组结构，搜索时过滤每个标签页内的字段
 */
const filteredFieldGroups = computed((): FieldModuleGroup[] => {
  const kw = fieldSearch.value.trim();
  if (!kw) return visibleFieldGroups.value;
  return visibleFieldGroups.value
    .map(g => {
      const filteredTabs = g.tabs
        .map(tab => ({ ...tab, items: tab.items.filter(i => i.menuName.includes(kw)) }))
        .filter(tab => tab.items.length > 0);
      const allItems = filteredTabs.flatMap(tab => tab.items);
      return { ...g, tabs: filteredTabs, allItems };
    })
    .filter(g => g.allItems.length > 0);
});

/**
 * 中面板：按 M 目录分组的按钮列表（与左面板模块树结构对应）
 * 展示层级：M 目录 → C 菜单 → 按钮列表
 */
const groupedButtonsByDirectory = computed((): ModuleDirectoryGroup<PermGroup>[] => {
  return buildDirectoryGroups(filteredButtonGroups.value);
});

/**
 * 右面板：按 M 目录分组的字段列表（与左面板模块树结构对应）
 * 展示层级：M 目录 → C 菜单 → 标签页 → 字段列表
 */
const groupedFieldsByDirectory = computed((): ModuleDirectoryGroup<FieldModuleGroup>[] => {
  return buildDirectoryGroups(filteredFieldGroups.value);
});

/**
 * 通用：将 C 菜单级分组按其所属 M 目录进行归类
 * 利用 moduleTreeData（已经过 flattenBusinessSystemRoot 重组）的结构，
 * 确保分组顺序与左面板模块树完全一致。
 */
const buildDirectoryGroups = <T extends { menuId: number }>(groups: T[]): ModuleDirectoryGroup<T>[] => {
  if (groups.length === 0) return [];

  const groupMenuIds = new Set(groups.map(g => g.menuId));
  const result: ModuleDirectoryGroup<T>[] = [];

  // 遍历 moduleTreeData 顶层节点（即 M 目录），按树顺序收集
  for (const dirNode of moduleTreeData.value) {
    const childCIds = (dirNode.children || [])
      .filter(c => c.menuType === 'C')
      .map(c => c.id);
    const matchedGroups = groups.filter(g => childCIds.includes(g.menuId));
    if (matchedGroups.length > 0) {
      result.push({
        directoryId: dirNode.id,
        directoryName: dirNode.menuName,
        children: matchedGroups,
      });
    }
  }

  // 处理未归入任何 M 目录的分组（兜底）
  const assignedIds = new Set(result.flatMap(d => d.children.map(c => c.menuId)));
  const unassigned = groups.filter(g => !assignedIds.has(g.menuId));
  if (unassigned.length > 0) {
    result.push({
      directoryId: 0,
      directoryName: '其他',
      children: unassigned,
    });
  }

  return result;
};

/** 当前可见按钮总数（中面板统计分母，跟随搜索过滤） */
const totalVisibleButtonCount = computed(() =>
  filteredButtonGroups.value.reduce((sum, g) => sum + g.items.length, 0)
);

/** 当前可见字段总数（右面板统计分母，跟随搜索过滤） */
const totalVisibleFieldCount = computed(() =>
  filteredFieldGroups.value.reduce((sum, g) => sum + g.allItems.length, 0)
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

    /**
     * 过滤掉"数据权限字段"：
     * 数据权限字段（dataScope、customOrgIds、customDeptIds）已拆分为独立的"数据授权"弹窗管理，
     * 不需要在功能授权面板的表单字段中显示。
     */
    const DATA_SCOPE_FIELD_SUFFIXES = [':field:dataScope', ':field:customOrgIds', ':field:customDeptIds'];
    const nonInherentFields = fMenus.filter(m =>
      !DATA_SCOPE_FIELD_SUFFIXES.some(suffix => m.perms?.endsWith(suffix))
    );

    /**
     * 3) 分离"查询"按钮：
     *    perms 以 ':query' 结尾的 B 类型按钮视为"隐含查询权限"，
     *    不在功能按钮面板中展示，保存时根据模块勾选状态自动补入。
     *    设计原则：勾选了模块菜单即默认拥有该模块的查询权限，无需手动勾选。
     */
    const queryButtons = bMenus.filter(m => m.perms?.endsWith(':query'));
    const nonQueryButtons = bMenus.filter(m => !m.perms?.endsWith(':query'));
    queryButtonsCache.value = queryButtons;

    // 3) 构建模块菜单树（M + C），展示时隐藏业务根目录“系统管理”，与业务系统侧边栏保持一致。
    moduleTreeData.value = buildModuleTree(mcMenus);

    // 4) 按钮分组（按 parentId 分组，parentId 对应某个 C 菜单）
    allButtonGroups.value = buildGroups(nonQueryButtons, mcMenus);

    // 5) 字段分组（排除固有字段，按标签页二级分组）
    allFieldGroups.value = buildFieldGroupsWithTabs(nonInherentFields, mcMenus);

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

      // 回填按钮勾选（排除查询按钮，查询按钮由模块勾选自动隐含）
      const nonQueryBIdSet = new Set(nonQueryButtons.map(m => m.id));
      checkedButtonIds.value = [...allSavedIds].filter(id => nonQueryBIdSet.has(id));

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

/** 按 parentId 分组，生成 PermGroup[]，按左侧树中 C 菜单的实际显示顺序排序 */
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
  groups.sort((a, b) => {
    const orderA = treeOrderMap.get(a.menuId) ?? 999;
    const orderB = treeOrderMap.get(b.menuId) ?? 999;
    return orderA - orderB;
  });

  // 每组内的 items 也按 sort 排序
  groups.forEach(g => g.items.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)));

  return groups;
};

/**
 * 构建字段分组（含标签页二级分组）
 *
 * 解析规则：
 *   从字段的 remark 中提取标签页名称，格式为 "模块名 — 标签名 — 字段名"
 *   如果 remark 不符合格式或为空，则归入默认分组"其他字段"
 *
 * @param items - F 类型字段列表
 * @param parentMenus - 父菜单列表（M + C）
 * @returns FieldModuleGroup[] - 按模块 → 标签页 → 字段的三级结构
 */
const buildFieldGroupsWithTabs = (items: MenuItem[], parentMenus: MenuItem[]): FieldModuleGroup[] => {
  const parentMap = new Map<number, MenuItem>();
  parentMenus.forEach(m => parentMap.set(m.id, m));

  // 先按 parentId 分组（模块级）
  const moduleMap = new Map<number, { menuId: number; menuName: string; items: MenuItem[] }>();
  items.forEach(item => {
    if (!moduleMap.has(item.parentId)) {
      const parent = parentMap.get(item.parentId);
      moduleMap.set(item.parentId, {
        menuId: item.parentId,
        menuName: parent?.menuName || `菜单#${item.parentId}`,
        items: [],
      });
    }
    moduleMap.get(item.parentId)!.items.push(item);
  });

  // 按左侧模块树顺序排序
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

  const modules = Array.from(moduleMap.values());
  modules.sort((a, b) => {
    const orderA = treeOrderMap.get(a.menuId) ?? 999;
    const orderB = treeOrderMap.get(b.menuId) ?? 999;
    return orderA - orderB;
  });

  // 每个模块内按标签页分组
  return modules.map(mod => {
    // 按 sort 排序
    mod.items.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

    // 从 remark 中提取标签页名称
    const tabMap = new Map<string, MenuItem[]>();
    mod.items.forEach(item => {
      const tabName = extractTabName(item.remark);
      if (!tabMap.has(tabName)) {
        tabMap.set(tabName, []);
      }
      tabMap.get(tabName)!.push(item);
    });

    const tabs: FieldTabGroup[] = Array.from(tabMap.entries()).map(([tabName, tabItems]) => ({
      tabName,
      items: tabItems,
    }));

    return {
      menuId: mod.menuId,
      menuName: mod.menuName,
      tabs,
      allItems: mod.items,
    };
  });
};

/**
 * 从 remark 中提取标签页名称
 *
 * remark 格式约定：
 *   "模块名 — 标签名 — 字段名"  → 提取"标签名"
 *   "模块名 — 字段名"           → 归入"基本信息"
 *   空或不符合格式               → 归入"基本信息"
 *
 * 示例：
 *   "机构管理 — 基本信息 — 上级机构字段" → "基本信息"
 *   "机构管理 — 工商信息标签页 — 成立日期字段" → "工商信息"
 *   "机构管理 — 联系与地址标签页 — 省份字段" → "联系与地址"
 */
const extractTabName = (remark?: string): string => {
  if (!remark) return '基本信息';
  // 按 " — " 或 " - " 分割
  const parts = remark.split(/\s*[—\-]\s*/);
  if (parts.length >= 3) {
    // 取第二段作为标签名，去除"标签页"后缀
    return parts[1].replace(/标签页$/, '').replace(/标签$/, '').trim();
  }
  return '基本信息';
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
        .flatMap(g => g.allItems.map(i => i.id))
    );
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
    checkedFieldIds.value = visibleFieldGroups.value.flatMap(g => g.allItems.map(i => i.id));
  } else {
    checkedFieldIds.value = [];
  }
};

/**
 * 功能按钮面板：展开/收起全部分组
 */
const onExpandAllButtons = (val: boolean | (string | number | boolean)[]) => {
  const expand = val === true || (Array.isArray(val) && val.includes(true));
  if (expand) {
    // 展开所有目录和分组
    groupedButtonsByDirectory.value.forEach(dir => {
      expandedGroups.value.add(`button:dir:${dir.directoryId}`);
      dir.children.forEach(g => expandedGroups.value.add(`button:${g.menuId}`));
    });
  } else {
    // 收起所有按钮相关的展开状态
    const keysToRemove = [...expandedGroups.value].filter(k => k.startsWith('button:'));
    keysToRemove.forEach(k => expandedGroups.value.delete(k));
  }
};

/**
 * 表单字段面板：展开/收起全部分组
 */
const onExpandAllFields = (val: boolean | (string | number | boolean)[]) => {
  const expand = val === true || (Array.isArray(val) && val.includes(true));
  if (expand) {
    // 展开所有目录、分组和标签页
    groupedFieldsByDirectory.value.forEach(dir => {
      expandedGroups.value.add(`field:dir:${dir.directoryId}`);
      dir.children.forEach(g => {
        expandedGroups.value.add(`field:${g.menuId}`);
        g.tabs.forEach(tab => collapsedFieldTabs.value.add(`${g.menuId}:${tab.tabName}`));
      });
    });
  } else {
    // 收起所有字段相关的展开状态
    const keysToRemove = [...expandedGroups.value].filter(k => k.startsWith('field:'));
    keysToRemove.forEach(k => expandedGroups.value.delete(k));
    collapsedFieldTabs.value.clear();
  }
};

/* ──────────────────────── 分组全选辅助 ──────────────────────── */

/**
 * 判断指定分组下的项是否全部勾选
 * @param type - 'button' | 'field'，对应中面板 / 右面板
 * @param menuId - 分组所属的父菜单 ID
 */
const isGroupAllChecked = (type: 'button' | 'field', menuId: number): boolean => {
  const checkedIds = type === 'button' ? checkedButtonIds.value : checkedFieldIds.value;
  const checkedSet = new Set(checkedIds);
  if (type === 'button') {
    const group = allButtonGroups.value.find(g => g.menuId === menuId);
    if (!group || group.items.length === 0) return false;
    return group.items.every(item => checkedSet.has(item.id));
  } else {
    const group = allFieldGroups.value.find(g => g.menuId === menuId);
    if (!group || group.allItems.length === 0) return false;
    return group.allItems.every(item => checkedSet.has(item.id));
  }
};

/**
 * 判断指定分组是否处于半选（部分勾选）状态
 * @param type - 'button' | 'field'
 * @param menuId - 分组所属的父菜单 ID
 */
const isGroupIndeterminate = (type: 'button' | 'field', menuId: number): boolean => {
  const checkedIds = type === 'button' ? checkedButtonIds.value : checkedFieldIds.value;
  const checkedSet = new Set(checkedIds);
  if (type === 'button') {
    const group = allButtonGroups.value.find(g => g.menuId === menuId);
    if (!group || group.items.length === 0) return false;
    const checkedCount = group.items.filter(item => checkedSet.has(item.id)).length;
    return checkedCount > 0 && checkedCount < group.items.length;
  } else {
    const group = allFieldGroups.value.find(g => g.menuId === menuId);
    if (!group || group.allItems.length === 0) return false;
    const checkedCount = group.allItems.filter(item => checkedSet.has(item.id)).length;
    return checkedCount > 0 && checkedCount < group.allItems.length;
  }
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
  let groupItemIds: Set<number>;
  if (type === 'button') {
    const group = allButtonGroups.value.find(g => g.menuId === menuId);
    if (!group) return;
    groupItemIds = new Set(group.items.map(i => i.id));
  } else {
    const group = allFieldGroups.value.find(g => g.menuId === menuId);
    if (!group) return;
    groupItemIds = new Set(group.allItems.map(i => i.id));
  }

  if (type === 'button') {
    if (check) {
      const existing = checkedButtonIds.value.filter(id => !groupItemIds.has(id));
      checkedButtonIds.value = [...existing, ...groupItemIds];
    } else {
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

/**
 * 判断指定标签页下的字段是否全部勾选
 */
const isTabAllChecked = (tabItems: MenuItem[]): boolean => {
  if (tabItems.length === 0) return false;
  const checkedSet = new Set(checkedFieldIds.value);
  return tabItems.every(item => checkedSet.has(item.id));
};

/**
 * 判断指定标签页是否处于半选状态
 */
const isTabIndeterminate = (tabItems: MenuItem[]): boolean => {
  if (tabItems.length === 0) return false;
  const checkedSet = new Set(checkedFieldIds.value);
  const checkedCount = tabItems.filter(item => checkedSet.has(item.id)).length;
  return checkedCount > 0 && checkedCount < tabItems.length;
};

/**
 * 标签页全选/全不选切换
 */
const onTabCheckAll = (tabItems: MenuItem[], val: boolean | (string | number | boolean)[]) => {
  const check = val === true || (Array.isArray(val) && val.includes(true));
  const tabItemIds = new Set(tabItems.map(i => i.id));
  if (check) {
    const existing = checkedFieldIds.value.filter(id => !tabItemIds.has(id));
    checkedFieldIds.value = [...existing, ...tabItemIds];
  } else {
    checkedFieldIds.value = checkedFieldIds.value.filter(id => !tabItemIds.has(id));
  }
};

/* ──────────────── 字段面板标签页展开/收起 ──────────────── */

/**
 * 切换字段面板中某个标签页的展开/收起状态
 * @param menuId - 所属模块菜单 ID
 * @param tabName - 标签页名称
 */
const toggleFieldTab = (menuId: number, tabName: string) => {
  const key = `${menuId}:${tabName}`;
  if (collapsedFieldTabs.value.has(key)) {
    collapsedFieldTabs.value.delete(key);
  } else {
    collapsedFieldTabs.value.add(key);
  }
};

/**
 * 判断字段面板中某个标签页是否处于展开状态
 * 默认收起（Set 中存在该 key 时视为展开）
 */
const isFieldTabExpanded = (menuId: number, tabName: string): boolean => {
  const key = `${menuId}:${tabName}`;
  return collapsedFieldTabs.value.has(key);
};

/* ──────────────── 一级分组（模块级）展开/收起 ──────────────── */

/**
 * 切换功能按钮/表单字段面板中分组的展开/收起状态
 * @param type - 'button' | 'field'
 * @param menuId - 模块菜单 ID 或目录 ID
 */
const toggleGroup = (type: 'button' | 'field', menuId: number) => {
  const key = `${type}:${menuId}`;
  if (expandedGroups.value.has(key)) {
    expandedGroups.value.delete(key);
  } else {
    expandedGroups.value.add(key);
  }
};

/**
 * 判断分组是否处于展开状态
 * 默认收起（expandedGroups 中不存在该 key 时视为收起）
 */
const isGroupExpanded = (type: 'button' | 'field', menuId: number): boolean => {
  const key = `${type}:${menuId}`;
  return expandedGroups.value.has(key);
};

/**
 * 切换 M 目录级别的展开/收起
 */
const toggleDirectory = (type: 'button' | 'field', directoryId: number) => {
  const key = `${type}:dir:${directoryId}`;
  if (expandedGroups.value.has(key)) {
    expandedGroups.value.delete(key);
  } else {
    expandedGroups.value.add(key);
  }
};

/**
 * 判断 M 目录是否展开
 * 默认收起
 */
const isDirectoryExpanded = (type: 'button' | 'field', directoryId: number): boolean => {
  const key = `${type}:dir:${directoryId}`;
  return expandedGroups.value.has(key);
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
    /**
     * 自动补入查询按钮权限：
     * 已勾选的 C 菜单对应的查询按钮（perms 以 ':query' 结尾）自动纳入授权范围，
     * 无需用户手动勾选。这体现了"勾选模块即拥有查询权限"的设计原则。
     *
     * 触发条件（满足任一即补入）：
     *   1. 左面板勾选了该模块菜单（checkedCMenuIds）
     *   2. 中面板勾选了该模块下的任意功能按钮
     *   3. 右面板勾选了该模块下的任意表单字段
     */
    const menuIdsWithButtonChecked = new Set(
      allButtonGroups.value
        .filter(g => g.items.some(item => checkedButtonIds.value.includes(item.id)))
        .map(g => g.menuId)
    );
    const menuIdsWithFieldChecked = new Set(
      allFieldGroups.value
        .filter(g => g.allItems.some(item => checkedFieldIds.value.includes(item.id)))
        .map(g => g.menuId)
    );
    const autoQueryMenuIds = new Set([
      ...checkedCMenuIds.value,
      ...menuIdsWithButtonChecked,
      ...menuIdsWithFieldChecked,
    ]);
    const autoQueryIds = queryButtonsCache.value
      .filter(btn => autoQueryMenuIds.has(btn.parentId))
      .map(btn => btn.id);

    // 所有完全勾选的叶子 ID（C + B + F + 自动补入的查询按钮）
    const allCheckedIds = [
      ...checkedModuleIds.value,
      ...checkedButtonIds.value,
      ...checkedFieldIds.value,
      ...autoQueryIds,
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
  expandAllButtons.value = false;
  expandAllFields.value = false;
  buttonSearch.value = '';
  fieldSearch.value = '';
  focusedMenuId.value = null;
  collapsedFieldTabs.value.clear();
  expandedGroups.value.clear();
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

/* ═══ M 目录级样式 ═══ */
.perm-directory {
  margin-bottom: 2px;
}
.perm-directory__header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  border-bottom: 1px solid rgba(226, 232, 240, 0.3);
  transition: background 0.15s ease;
}
.perm-directory__header:hover {
  background: rgba(230, 240, 255, 0.5);
}
.perm-directory__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 0;
  color: #165dff;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  margin-right: 6px;
  border-radius: 4px;
  background: rgba(22, 93, 255, 0.08);
}
.perm-directory__arrow::before {
  content: '';
  display: block;
  width: 0;
  height: 0;
  border-left: 5px solid #165dff;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  margin-left: 1px;
}
.perm-directory__arrow--open {
  transform: rotate(90deg);
  background: rgba(22, 93, 255, 0.12);
}
.perm-directory__name {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
}
.perm-directory__body {
  padding-left: 8px;
}
.perm-group__header--collapsible {
  cursor: pointer;
  user-select: none;
  transition: background 0.15s ease;
}
.perm-group__header--collapsible:hover {
  background: linear-gradient(180deg, rgba(240, 245, 255, 1), rgba(230, 240, 255, 0.8));
}
.perm-group__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  font-size: 0;
  color: #64748b;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  margin-right: 6px;
  border-radius: 3px;
  background: rgba(100, 116, 139, 0.08);
}
.perm-group__arrow::before {
  content: '';
  display: block;
  width: 0;
  height: 0;
  border-left: 4px solid #64748b;
  border-top: 3px solid transparent;
  border-bottom: 3px solid transparent;
  margin-left: 1px;
}
.perm-group__arrow--open {
  transform: rotate(90deg);
  background: rgba(22, 93, 255, 0.1);
}
.perm-group__arrow--open::before {
  border-left-color: #165dff;
}
.perm-group__name {
  flex: 1;
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

/* ═══ 标签页树节点样式（表单字段面板，可展开/收起） ═══ */
.perm-tab-node {
  margin-bottom: 2px;
}
.perm-tab-node__header {
  display: flex;
  align-items: center;
  padding: 5px 8px;
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.15s ease;
  user-select: none;
}
.perm-tab-node__header:hover {
  background: var(--color-fill-2, #f2f3f5);
}
.perm-tab-node__arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  font-size: 0;
  color: #94a3b8;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  border-radius: 3px;
}
.perm-tab-node__arrow::before {
  content: '';
  display: block;
  width: 0;
  height: 0;
  border-left: 4px solid #94a3b8;
  border-top: 3px solid transparent;
  border-bottom: 3px solid transparent;
}
.perm-tab-node__arrow--open {
  transform: rotate(90deg);
}
.perm-tab-node__arrow--open::before {
  border-left-color: #165dff;
}
.perm-tab-node__name {
  flex: 1;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-2, #4e5969);
  margin-left: 4px;
}
.perm-tab-node__name::before {
  content: '';
  display: inline-block;
  width: 3px;
  height: 10px;
  background: #165dff;
  border-radius: 2px;
  margin-right: 6px;
  vertical-align: middle;
}
.perm-action-check--tab {
  font-size: 11px;
  flex-shrink: 0;
}
.perm-tab-node__body {
  padding: 4px 8px 8px 28px;
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
  padding: 2px 6px;
  border-radius: 4px;
  cursor: pointer;
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
