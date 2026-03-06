/**
 * API 接口列表相关接口请求。
 * 与后端 SysApiListController、统一响应 Result 配合使用。
 */
import request from '../utils/request';
import type { ApiCatalogTreeNode, OpenApiRegistryTreeQuery } from '../types/apiList';

type ApiPromise<T> = Promise<{ data: T }>;

/**
 * 查询 API 接口目录树。
 * 用于「API 接口列表」页，返回模块 → 业务资源 → 具体接口 三层树形数据。
 *
 * @param params 可选筛选：keyword、method、status、moduleName
 * @returns 树节点列表
 */
export const fetchApiListTree = (
  params?: OpenApiRegistryTreeQuery
): ApiPromise<ApiCatalogTreeNode[]> => {
  return request.get('/api-list/tree', { params }) as ApiPromise<ApiCatalogTreeNode[]>;
};
