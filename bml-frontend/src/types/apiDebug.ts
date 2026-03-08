/**
 * API 调试面板相关类型定义。
 * 用于「API 接口列表」页右侧 Postman 风格调试面板的请求配置与响应展示。
 */

/** 键值对单行（Query 参数、请求头等） */
export interface KeyValueRow {
  /** 唯一键，用于 v-for */
  id: string;
  /** 是否启用该行（如请求头可勾选是否携带） */
  enabled: boolean;
  /** 参数名或 Header 名 */
  key: string;
  /** 参数值或 Header 值 */
  value: string;
}

/**
 * 调试请求配置。
 * 由用户在面板中编辑后发起请求。
 */
export interface ApiDebugRequestConfig {
  /** HTTP 方法 */
  method: string;
  /** 请求路径（相对 baseURL，不含 query 字符串） */
  path: string;
  /** Query 参数列表 */
  queryParams: KeyValueRow[];
  /** 请求头列表 */
  headers: KeyValueRow[];
  /** 是否自动携带当前登录 Token（Authorization: Bearer &lt;accessToken&gt;） */
  attachToken: boolean;
  /** 请求体（仅 POST/PUT/PATCH 等有 body 时使用，原始字符串） */
  body: string;
}

/**
 * 调试响应结果。
 * 请求完成后用于展示状态码、耗时、响应头、响应体。
 */
export interface ApiDebugResponseResult {
  /** HTTP 状态码 */
  status: number;
  /** 状态文案，如 "200 OK" */
  statusText: string;
  /** 请求耗时（毫秒） */
  durationMs: number;
  /** 响应头（键值对列表，便于展示） */
  responseHeaders: KeyValueRow[];
  /** 响应体（已格式化为字符串，便于 JSON 高亮展示） */
  bodyText: string;
  /** 是否为 JSON 格式（用于决定是否尝试格式化与高亮） */
  isJson: boolean;
}

/** 支持的 HTTP 方法列表（与后端及 api-account-governance 一致） */
export const DEBUG_HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'] as const;

/** 生成 KeyValueRow 的默认 id（避免重复） */
let keyValueRowIdCounter = 0;
export function createKeyValueRow(partial?: Partial<KeyValueRow>): KeyValueRow {
  keyValueRowIdCounter += 1;
  return {
    id: `kv-${keyValueRowIdCounter}-${Date.now()}`,
    enabled: true,
    key: '',
    value: '',
    ...partial
  };
}
