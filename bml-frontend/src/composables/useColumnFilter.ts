/**
 * 通用表格列头搜索过滤 — 组合式函数
 *
 * 为表格列头放大镜搜索提供客户端关键词过滤能力，
 * 支持平铺列表（useColumnFilter）和树形列表（useTreeColumnFilter）两种模式。
 *
 * 搜索规则：大小写不敏感的子串匹配，所有激活的过滤条件之间为 AND 关系。
 *
 * @module useColumnFilter
 * @see TableColumnSearch.vue — 配套的列头搜索 UI 组件
 */

import { computed, type Ref } from 'vue';

/**
 * 列值格式化函数类型。
 * 将原始字段值转换为可搜索的展示文本（如状态码 → "正常"）。
 *
 * @param value  字段原始值
 * @param record 当前行完整数据（可用于跨字段判断）
 * @returns 可搜索的展示文本
 *
 * @example
 * const statusFormatter: ColumnValueFormatter = (val) => val === 1 ? '正常' : '停用';
 */
export type ColumnValueFormatter = (value: any, record: any) => string;

/* ════════════════════════════════════════════════════════════════════
 *  平铺列表过滤（适用于角色、岗位、用户等服务端分页列表）
 * ════════════════════════════════════════════════════════════════════ */

/**
 * 通用列头搜索过滤（平铺列表）。
 *
 * 对当前页表格数据进行客户端列头关键词过滤，支持自定义值格式化。
 * 适用于分页由服务端完成的平铺列表（如角色、岗位、用户管理），
 * 列头搜索作为客户端二次筛选。
 *
 * @param data       原始表格数据（Ref 数组）
 * @param filters    列头搜索条件（reactive 对象，key = 列字段名，value = 搜索关键词）
 * @param formatters 可选值格式化映射，将非字符串字段（如状态码）转换为可搜索文本
 * @returns {{ filteredData: ComputedRef<T[]> }} 过滤后的数据
 *
 * @example
 * const { filteredData } = useColumnFilter(tableData, columnFilters, {
 *   status: (val) => val === 1 ? '正常' : '停用',
 * });
 * // 模板中使用: <a-table :data="filteredData" />
 */
export function useColumnFilter<T extends Record<string, any>>(
  data: Ref<T[]>,
  filters: Record<string, string>,
  formatters?: Record<string, ColumnValueFormatter>,
) {
  const filteredData = computed(() => {
    /* 收集所有有值的过滤条件，提前 trim + lowercase 避免循环内重复计算 */
    const activeFilters = Object.entries(filters)
      .filter(([, v]) => v.trim() !== '')
      .map(([key, keyword]) => [key, keyword.trim().toLowerCase()] as const);

    /* 无任何过滤条件时直接返回原数据，避免无意义遍历 */
    if (activeFilters.length === 0) return data.value;

    return data.value.filter(record =>
      activeFilters.every(([key, keyword]) => {
        const formatter = formatters?.[key];
        const displayValue = formatter
          ? formatter(record[key], record)
          : String(record[key] ?? '');
        return displayValue.toLowerCase().includes(keyword);
      }),
    );
  });

  return { filteredData };
}

/* ════════════════════════════════════════════════════════════════════
 *  树形列表过滤（适用于机构、部门等树形结构表格）
 * ════════════════════════════════════════════════════════════════════ */

/**
 * 通用列头搜索过滤（树形列表）。
 *
 * 递归过滤树形数据，保留匹配节点及其祖先路径：
 * - 节点自身匹配所有条件 → 保留该节点及其**全部**子树（不裁剪后代）
 * - 节点自身不匹配但后代中有匹配 → 保留该节点作为路径，子树仅保留匹配分支
 * - 节点及所有后代均不匹配 → 整棵子树移除
 *
 * @param data       原始树形数据（Ref 数组，节点含可选 children 属性）
 * @param filters    列头搜索条件
 * @param formatters 可选值格式化映射
 * @param childrenKey 子节点属性名，默认 'children'
 * @returns {{ filteredData: ComputedRef<T[]> }} 过滤后的树形数据
 *
 * @example
 * const { filteredData: filteredTreeData } = useTreeColumnFilter(tableData, columnFilters, {
 *   orgType: (val) => orgTypeLabel(val),
 *   status:  (val) => val === 1 ? '正常' : '停用',
 * });
 */
export function useTreeColumnFilter<T extends Record<string, any>>(
  data: Ref<T[]>,
  filters: Record<string, string>,
  formatters?: Record<string, ColumnValueFormatter>,
  childrenKey: string = 'children',
) {
  /** 判断单个节点（不含子节点）是否匹配所有过滤条件 */
  const matchNode = (node: T, activeFilters: readonly (readonly [string, string])[]) =>
    activeFilters.every(([key, keyword]) => {
      const formatter = formatters?.[key];
      const displayValue = formatter
        ? formatter(node[key], node)
        : String(node[key] ?? '');
      return displayValue.toLowerCase().includes(keyword);
    });

  /** 递归过滤树节点 */
  const filterTree = (nodes: T[], activeFilters: readonly (readonly [string, string])[]): T[] =>
    nodes.reduce<T[]>((acc, node) => {
      const children = (node as any)[childrenKey] as T[] | undefined;

      /* 先递归处理子节点 */
      const filteredChildren = children?.length
        ? filterTree(children, activeFilters)
        : [];

      const selfMatch = matchNode(node, activeFilters);

      if (selfMatch) {
        /* 节点自身匹配 → 保留原始子树（用户想看的节点，其下级也应完整展示） */
        acc.push(node);
      } else if (filteredChildren.length > 0) {
        /* 节点不匹配但后代有匹配 → 保留节点作为路径，替换子树为过滤结果 */
        acc.push({ ...node, [childrenKey]: filteredChildren } as T);
      }
      /* 节点及后代均不匹配 → 不加入结果 */

      return acc;
    }, []);

  const filteredData = computed(() => {
    const activeFilters = Object.entries(filters)
      .filter(([, v]) => v.trim() !== '')
      .map(([key, keyword]) => [key, keyword.trim().toLowerCase()] as const);

    if (activeFilters.length === 0) return data.value;
    return filterTree(data.value, activeFilters);
  });

  return { filteredData };
}

/* ════════════════════════════════════════════════════════════════════
 *  工具函数
 * ════════════════════════════════════════════════════════════════════ */

/**
 * 重置所有列头搜索条件为空字符串。
 * 通常在页面"重置条件"按钮中调用，与查询面板的重置一起清空列头过滤。
 *
 * @param filters 列头搜索条件（reactive 对象）
 *
 * @example
 * const handleReset = () => {
 *   resetColumnFilters(columnFilters);
 *   loadData();
 * };
 */
export function resetColumnFilters(filters: Record<string, string>): void {
  Object.keys(filters).forEach(key => { filters[key] = ''; });
}
