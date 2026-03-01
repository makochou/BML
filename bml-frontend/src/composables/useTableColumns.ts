import { computed, type ComputedRef } from 'vue';

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
  fixed?: ColumnFix;
  ellipsis?: boolean;
  tooltip?: boolean;
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
  columns: ComputedRef<ConfigurableTableColumn<Kind>[]>;
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
      ellipsis: column.ellipsis ?? Boolean(column.tooltip),
      tooltip: column.tooltip ?? Boolean(column.ellipsis)
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
