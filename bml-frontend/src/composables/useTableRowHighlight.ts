/**
 * BML 表格行点击选中高亮 —— 通用 Composable
 * ═══════════════════════════════════════════════════════
 *
 * 功能说明：
 *   为 Arco Design <a-table> 提供"单击行选中高亮"能力。
 *   点击某行后自动标记为选中状态，再次点击取消选中（toggle 行为）。
 *   选中行会附加统一的 CSS 类名 `bml-row-active`，
 *   其高亮底色通过全局 CSS 变量 `--bml-primary-rgb` 跟随主题色。
 *
 * 使用方式：
 *   ```vue
 *   <script setup lang="ts">
 *   import { useTableRowHighlight } from '@/composables/useTableRowHighlight';
 *   // idField 为数据行的唯一标识字段名，默认 'id'
 *   const { activeRowId, handleRowClick, getRowClass, clearSelection } = useTableRowHighlight();
 *   </script>
 *
 *   <template>
 *     <a-table
 *       row-key="id"
 *       :row-class="getRowClass"
 *       @row-click="handleRowClick"
 *       ...
 *     />
 *   </template>
 *   ```
 *
 * CSS 配套：
 *   全局样式定义在 `src/assets/business-system.css` 中的 `.bml-row-active` 规则，
 *   底色使用 `rgba(var(--bml-primary-rgb), 0.06)` 自动跟随主题。
 *
 * 设计原则：
 *   - 零外部依赖，仅依赖 Vue Composition API
 *   - 泛型支持，适用于任意数据类型
 *   - 不与具体业务耦合，纯 UI 交互逻辑
 *   - 支持自定义主键字段名（默认 'id'）
 *
 * @module useTableRowHighlight
 */

import { ref, type Ref } from 'vue';

/* ═══════════════════════════════════════════════════════
   统一选中行 CSS 类名常量
   ═══════════════════════════════════════════════════════ */

/**
 * 选中行附加的 CSS 类名。
 * 全局样式在 `business-system.css` 中定义，所有页面共享同一套高亮规则。
 */
export const ROW_ACTIVE_CLASS = 'bml-row-active';

/* ═══════════════════════════════════════════════════════
   Composable 选项类型
   ═══════════════════════════════════════════════════════ */

/** useTableRowHighlight 可选配置 */
export interface UseTableRowHighlightOptions {
  /**
   * 数据行中用作唯一标识的字段名。
   * 默认 'id'，适用于大多数 CRUD 场景。
   *
   * 示例：若数据行结构为 { userId: 1, name: '张三' }，
   *        则传入 { idField: 'userId' }
   */
  idField?: string;

  /**
   * 是否支持 toggle（再次点击取消选中）。
   * 默认 true。设为 false 时，点击始终选中，不会取消。
   */
  toggleable?: boolean;
}

/* ═══════════════════════════════════════════════════════
   Composable 返回类型
   ═══════════════════════════════════════════════════════ */

/** useTableRowHighlight 返回值 */
export interface UseTableRowHighlightReturn {
  /** 当前选中行的主键值（null 表示无选中） */
  activeRowId: Ref<string | number | null>;

  /**
   * 行点击事件处理函数。
   * 直接绑定到 <a-table @row-click="handleRowClick">。
   * @param record - 被点击行的数据对象
   */
  handleRowClick: (record: Record<string, any>) => void;

  /**
   * 行 CSS 类名生成函数。
   * 直接绑定到 <a-table :row-class="getRowClass">。
   * @param record - 行数据对象
   * @returns 选中行返回 'bml-row-active'，否则返回 ''
   */
  getRowClass: (record: Record<string, any>) => string;

  /**
   * 清空当前选中状态。
   * 适用于：刷新数据后重置、删除记录后重置等场景。
   */
  clearSelection: () => void;

  /**
   * 编程式选中指定行。
   * @param id - 目标行的主键值
   */
  selectRow: (id: string | number) => void;
}

/* ═══════════════════════════════════════════════════════
   Composable 实现
   ═══════════════════════════════════════════════════════ */

/**
 * 表格行点击选中高亮 Composable
 *
 * @param options - 可选配置项
 * @returns 行选中相关的响应式状态与方法
 *
 * @example
 * ```ts
 * // 基础用法（默认以 'id' 作为主键）
 * const { activeRowId, handleRowClick, getRowClass } = useTableRowHighlight();
 *
 * // 自定义主键字段
 * const { activeRowId, handleRowClick, getRowClass } = useTableRowHighlight({ idField: 'userId' });
 *
 * // 禁用 toggle（点击始终选中）
 * const { activeRowId, handleRowClick, getRowClass } = useTableRowHighlight({ toggleable: false });
 * ```
 */
export function useTableRowHighlight(
  options: UseTableRowHighlightOptions = {}
): UseTableRowHighlightReturn {
  const { idField = 'id', toggleable = true } = options;

  /** 当前选中行主键 */
  const activeRowId = ref<string | number | null>(null);

  /**
   * 行点击处理：
   * - 点击未选中的行 → 选中
   * - 再次点击同一行 → 取消选中（toggleable=true 时）
   */
  const handleRowClick = (record: Record<string, any>) => {
    const rowId = record[idField];
    if (rowId == null) return;

    if (toggleable && activeRowId.value === rowId) {
      activeRowId.value = null;
    } else {
      activeRowId.value = rowId;
    }
  };

  /**
   * 返回行 CSS 类名。
   * Arco Table 的 :row-class prop 接收此函数，
   * 对于选中行返回统一的 'bml-row-active' 类名。
   */
  const getRowClass = (record: Record<string, any>): string => {
    return record[idField] === activeRowId.value ? ROW_ACTIVE_CLASS : '';
  };

  /** 清空选中状态 */
  const clearSelection = () => {
    activeRowId.value = null;
  };

  /** 编程式选中指定行 */
  const selectRow = (id: string | number) => {
    activeRowId.value = id;
  };

  return {
    activeRowId,
    handleRowClick,
    getRowClass,
    clearSelection,
    selectRow,
  };
}
