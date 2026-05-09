import { computed, type ComputedRef } from 'vue';
import { resolveTableColumnDefaultWidth } from '../utils/tableColumnWidth';

export type ColumnFix = 'left' | 'right';

/**
 * 通用表格列配置模型。
 * `kind` 用于页面决定单元格的渲染分支，其他属性保持与 Arco Table 常用列参数一致，
 * 便于后续多个治理页统一维护列结构与交互规范。
 */
export type ConfigurableTableColumn<Kind extends string> = {
  key: string;
  title: string;
  kind: Kind;
  width: number;
  dataIndex?: string;
  slotName?: string;
  fixed?: ColumnFix;
  ellipsis?: boolean;
  tooltip?: boolean;
  /**
   * 列内容对齐方式（表头与单元格同时生效）。
   * 默认值为 'center'，统一居中显示，视觉更整洁。
   * 如需特定列左对齐（如长文本列），可显式设置为 'left'。
   */
  align?: 'left' | 'center' | 'right';
  /**
   * 是否开启排序功能。
   * 设为 true 时，表头会显示排序图标，点击可在升序/降序/默认之间切换。
   * 排序逻辑在前端完成，无需后端支持。
   * 不需要排序的列（如操作列、序号列）不设置此属性或设为 false。
   */
  sortable?: boolean;
  /**
   * 自定义表头 slot 名称。
   * 对应 Arco Table 列定义的 titleSlotName 属性。
   * 在 a-table 中通过 #[titleSlotName] 插槽自定义表头内容。
   */
  titleSlotName?: string;
};

export type ResolvedConfigurableTableColumn<Kind extends string> =
  Omit<ConfigurableTableColumn<Kind>, 'sortable'> & {
    sortable?: boolean | { sortDirections: ('ascend' | 'descend')[] };
  };

type TableColumnSource<Kind extends string> =
  | readonly ConfigurableTableColumn<Kind>[]
  | (() => readonly ConfigurableTableColumn<Kind>[]);

/**
 * 用于声明列模型，保留字面量类型推断，后续新增列时可以获得更稳定的类型提示。
 */
export function defineTableColumns<Kind extends string>(
  columns: readonly ConfigurableTableColumn<Kind>[]
) {
  return columns;
}

/**
 * 统一表格列模型与基础取值逻辑。
 * 1. 自动补齐省略与 tooltip 的默认配对规则。
 * 2. 暴露 `getPlainText`，避免各页面重复写空值兜底代码。
 * 3. 保留 `kind` 语义，方便业务页面继续定制复杂单元格渲染。
 */
export function useTableColumns<Kind extends string>(
  source: TableColumnSource<Kind>
): {
  columns: ComputedRef<ResolvedConfigurableTableColumn<Kind>[]>;
  getPlainText: <RecordType extends object>(
    record: RecordType,
    dataIndex?: string,
    fallback?: string
  ) => string;
} {
  const columns = computed(() => {
    const resolved = typeof source === 'function' ? source() : source;
    return resolved.map(column => ({
      ...column,
      // 统一兜底列宽，确保带搜索/排序能力的表头标题默认完整展示。
      width: resolveTableColumnDefaultWidth({
        title: column.title,
        hasTitleSlot: !!column.titleSlotName,
        sortable: !!column.sortable,
        resizable: true,
        width: column.width,
      }),
      // 默认居中对齐：表头与单元格内容统一居中，视觉更整洁规范。
      align: column.align ?? 'center',
      ellipsis: column.ellipsis ?? Boolean(column.tooltip),
      tooltip: column.tooltip ?? Boolean(column.ellipsis),
      // 若列声明了 sortable: true，则注入 Arco Table 所需的 sortable 配置对象。
      // sortDirections 指定支持的排序方向：升序(ascend)、降序(descend)、取消排序(默认)。
      ...(column.sortable
        ? {
          sortable: {
            sortDirections: ['ascend', 'descend'] as ('ascend' | 'descend')[],
          },
        }
        : {}),
      // titleSlotName：透传给 Arco Table，用于自定义表头内容（如列头搜索图标）。
      // Arco Table 内部会读取此属性，从父组件 slots 中找到对应名称的 slot 渲染表头。
      ...(column.titleSlotName ? { titleSlotName: column.titleSlotName } : {}),
    }));
  });

  function getPlainText<RecordType extends object>(
    record: RecordType,
    dataIndex?: string,
    fallback = '-'
  ) {
    if (!dataIndex) return fallback;
    const value = Reflect.get(record, dataIndex);
    return value == null || value === '' ? fallback : String(value);
  }

  return { columns, getPlainText };
}
