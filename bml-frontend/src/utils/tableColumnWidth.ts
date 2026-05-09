/**
 * 表格列宽计算工具。
 *
 * 设计目标：
 * 1. 统一计算“表头标题完整展示”所需的最小列宽，避免各页面手工估算；
 * 2. 同时考虑 Arco Table 单元格内边距、列头搜索图标、排序图标、拖拽手柄与安全留白；
 * 3. 后续新增菜单只需要声明标题、是否有搜索列头、是否可排序，即可得到稳定的默认宽度。
 */

/**
 * 列宽方案版本。
 * 当表头宽度计算规则发生变化时递增此版本，调用方可将其纳入本地缓存指纹，
 * 自动让旧的 localStorage 列宽缓存失效，保证新默认方案立即生效。
 */
export const TABLE_COLUMN_WIDTH_SCHEME_VERSION = 2;

/** 表头最小宽度计算参数。 */
export interface TableColumnHeaderWidthOptions {
  /** 表头标题文本。 */
  title: string;
  /** 是否使用自定义列头插槽，例如 TableColumnSearch。 */
  hasTitleSlot?: boolean;
  /** 是否启用排序图标。 */
  sortable?: boolean;
  /** 是否启用列宽拖拽手柄。 */
  resizable?: boolean;
}

/** 默认列宽归一化参数。 */
export interface ResolveTableColumnWidthOptions extends TableColumnHeaderWidthOptions {
  /** 开发者声明的设计宽度。 */
  width: number;
}

const CJK_CHAR_WIDTH = 14;
const ASCII_CHAR_WIDTH = 8;
const TABLE_CELL_HORIZONTAL_PADDING = 32;
const TITLE_SLOT_SEARCH_ICON_WIDTH = 26;
const SORTER_WIDTH = 30;
const RESIZE_HANDLE_WIDTH = 10;
const SAFE_GUTTER_WIDTH = 14;
const MIN_INTERACTIVE_HEADER_WIDTH = 118;

/** 判断字符是否按全角宽度估算。 */
function isWideChar(char: string): boolean {
  return /[\u4e00-\u9fff\u3400-\u4dbf\u3000-\u303f\uff00-\uffef]/.test(char);
}

/** 计算标题文字本身的渲染宽度。 */
export function calcTableHeaderTextWidth(title: string): number {
  return Array.from(title).reduce((total, char) => {
    return total + (isWideChar(char) ? CJK_CHAR_WIDTH : ASCII_CHAR_WIDTH);
  }, 0);
}

/**
 * 计算表头完整展示所需的最小列宽。
 *
 * 宽度组成：单元格左右内边距 + 搜索图标占位 + 标题文本 + 排序图标占位 + 拖拽手柄占位 + 安全留白。
 */
export function calcTableHeaderMinWidth(options: TableColumnHeaderWidthOptions): number {
  const textWidth = calcTableHeaderTextWidth(options.title);
  const slotWidth = options.hasTitleSlot ? TITLE_SLOT_SEARCH_ICON_WIDTH : 0;
  const sorterWidth = options.sortable ? SORTER_WIDTH : 0;
  const resizeWidth = options.resizable ? RESIZE_HANDLE_WIDTH : 0;
  const rawWidth = TABLE_CELL_HORIZONTAL_PADDING + slotWidth + textWidth + sorterWidth + resizeWidth + SAFE_GUTTER_WIDTH;

  if (options.hasTitleSlot || options.sortable) {
    return Math.ceil(Math.max(rawWidth, MIN_INTERACTIVE_HEADER_WIDTH));
  }
  return Math.ceil(rawWidth);
}

/** 使用表头最小宽度归一化开发者声明的默认列宽。 */
export function resolveTableColumnDefaultWidth(options: ResolveTableColumnWidthOptions): number {
  return Math.max(
    Math.round(options.width),
    calcTableHeaderMinWidth(options)
  );
}
