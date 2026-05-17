/**
 * 用户名称缓存 composable
 *
 * 提供用户 ID → 用户名（nickname）的轻量级缓存映射。
 * 用于审计信息展示等场景，避免重复请求后端。
 *
 * 设计说明：
 *   - 全局单例缓存，所有组件共享同一份数据
 *   - 使用字符串 key 避免 JavaScript 大数字精度丢失问题
 *   - 首次请求某个 ID 时调用后端接口获取，之后从缓存读取
 *   - 缓存不过期（用户名变更频率极低，页面刷新即重置）
 *
 * 使用示例：
 * ```ts
 * const { resolveUserName, cache } = useUserNameCache();
 * resolveUserName(userId); // 触发异步加载
 * // 模板中使用: cache[String(userId)]
 * ```
 */
import { reactive } from 'vue';
import request from '../utils/request';

/** 全局用户名称缓存（字符串化的 ID → nickname） */
const cache = reactive<Record<string, string>>({});

/** 正在请求中的 ID（防止并发重复请求） */
const pending = new Set<string>();

/**
 * 根据用户 ID 获取用户名称（nickname）
 * 优先从缓存读取，缓存未命中时调用后端接口。
 * 接受 number | string 类型，内部统一转为字符串处理，避免大数字精度问题。
 */
async function resolveUserName(userId: number | string | null | undefined): Promise<string> {
  if (!userId) return '';
  const key = String(userId);
  // 缓存命中
  if (cache[key]) return cache[key];
  // 正在请求中，等待
  if (pending.has(key)) {
    await new Promise(resolve => setTimeout(resolve, 100));
    return cache[key] || '';
  }
  // 发起请求（使用轻量级接口，仅需登录权限，不需要用户管理权限）
  pending.add(key);
  try {
    const res = await request.get(`/system/user/${key}/name`) as any;
    const nickname = res?.data?.nickname || '未知用户';
    cache[key] = nickname;
    return nickname;
  } catch {
    // 请求失败时显示"未知用户"
    cache[key] = '未知用户';
    return '未知用户';
  } finally {
    pending.delete(key);
  }
}

/**
 * 批量预加载用户名称
 */
async function preloadUserNames(userIds: (number | string | null | undefined)[]): Promise<void> {
  const keys = [...new Set(userIds.filter(Boolean).map(String).filter(k => !cache[k]))];
  await Promise.allSettled(keys.map(k => resolveUserName(k)));
}

export function useUserNameCache() {
  return {
    /** 异步解析用户 ID 为名称（自动缓存） */
    resolveUserName,
    /** 批量预加载用户名称 */
    preloadUserNames,
    /** 缓存对象（响应式，key 为字符串化的用户 ID） */
    cache,
  };
}
