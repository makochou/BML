/**
 * API 接口列表相关类型定义。
 * 与后端 ApiCatalogTreeNodeVO、OpenApiRegistryTreeQuery 保持一致，便于前后端协作。
 */

/** 树节点类型：模块（一级）、业务资源（二级）、具体接口（叶子） */
export type ApiCatalogNodeType = 'MODULE' | 'RESOURCE' | 'API';

/**
 * API 接口目录树节点。
 * 用于「API 接口列表」页的树形展示。
 */
export interface ApiCatalogTreeNode {
  /** 节点唯一标识 */
  id: string;
  /** 展示名称（中文） */
  label: string;
  /** 节点类型 */
  type: ApiCatalogNodeType;
  /** 子节点（仅 MODULE、RESOURCE 存在） */
  children?: ApiCatalogTreeNode[];
  /** 接口主键（仅 API 类型） */
  apiId?: number;
  /** HTTP 方法（仅 API 类型） */
  httpMethod?: string;
  /** 接口路径（仅 API 类型） */
  apiUrl?: string;
  /** 接口描述（仅 API 类型） */
  description?: string;
  /** 状态：1 启用，0 停用（仅 API 类型） */
  status?: number;
}

/**
 * 开放接口目录树查询参数（与后端 OpenApiRegistryTreeQuery 一致）。
 */
export interface OpenApiRegistryTreeQuery {
  /** 关键词，匹配接口名称、路径、描述 */
  keyword?: string;
  /** HTTP 方法 */
  method?: string;
  /** 状态(1正常 0停用) */
  status?: number;
  /** 模块名称 */
  moduleName?: string;
}
