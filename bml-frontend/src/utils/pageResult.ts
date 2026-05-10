/**
 * BML 前端统一分页结果适配器。
 *
 * 后端标准分页协议为 { records, total, pageNum, pageSize }，但部分历史接口或第三方组件
 * 可能返回 { list, rows, current, size } 等字段。统一在这里做兼容，避免每个页面重复解析分页结构。
 */
export interface NormalizedPageResult<T> {
  /** 当前页数据列表。 */
  records: T[];
  /** 总记录数。 */
  total: number;
  /** 当前页码，从 1 开始。 */
  pageNum: number;
  /** 每页条数。 */
  pageSize: number;
}

/**
 * 将任意分页响应归一化为前端稳定可用的分页结构。
 *
 * @param payload 接口返回的分页 data 节点
 * @param fallbackPageNum 请求时使用的页码，用于接口未返回页码时兜底
 * @param fallbackPageSize 请求时使用的每页条数，用于接口未返回页大小时兜底
 * @returns 归一化分页结果
 */
export function normalizePageResult<T>(
  payload: unknown,
  fallbackPageNum = 1,
  fallbackPageSize = 10
): NormalizedPageResult<T> {
  const data = (payload || {}) as Record<string, unknown>;
  const recordsCandidate = data.records ?? data.list ?? data.rows ?? [];
  const records = Array.isArray(recordsCandidate) ? recordsCandidate as T[] : [];
  const totalCandidate = data.total ?? data.totalCount ?? data.count;
  const parsedTotal = Number(totalCandidate);
  const total = Number.isFinite(parsedTotal) && parsedTotal > 0 ? parsedTotal : records.length;
  const parsedPageNum = Number(data.pageNum ?? data.current ?? fallbackPageNum);
  const parsedPageSize = Number(data.pageSize ?? data.size ?? fallbackPageSize);

  return {
    records,
    total,
    pageNum: Number.isFinite(parsedPageNum) && parsedPageNum > 0 ? parsedPageNum : fallbackPageNum,
    pageSize: Number.isFinite(parsedPageSize) && parsedPageSize > 0 ? parsedPageSize : fallbackPageSize,
  };
}
