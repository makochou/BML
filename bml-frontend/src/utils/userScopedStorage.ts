import { getCurrentUserIdentity } from './auth';

const DEFAULT_FALLBACK_IDENTITY = 'anonymous';

/**
 * 归一化按用户隔离的本地存储标识：
 * - 统一裁剪空白并转小写，避免大小写差异造成重复配置；
 * - 缺失时回退到匿名标识，确保 key 结构稳定。
 */
export const normalizeUserScopedIdentity = (
  identity?: string,
  fallbackIdentity = DEFAULT_FALLBACK_IDENTITY
) => {
  const normalizedFallback = fallbackIdentity.trim().toLowerCase() || DEFAULT_FALLBACK_IDENTITY;
  const normalizedIdentity = (identity || '').trim().toLowerCase();
  return normalizedIdentity || normalizedFallback;
};

/**
 * 构建“按当前登录账号隔离”的 localStorage key。
 * 统一由该方法拼装 key，可在其他页面直接复用同一规范。
 */
export const buildUserScopedStorageKey = (
  prefix: string,
  options?: {
    identity?: string;
    fallbackIdentity?: string;
  }
) => {
  const normalizedPrefix = prefix.trim();
  if (!normalizedPrefix) {
    throw new Error('localStorage key prefix can not be empty');
  }
  const scopedIdentity = normalizeUserScopedIdentity(
    options?.identity ?? getCurrentUserIdentity(),
    options?.fallbackIdentity
  );
  return `${normalizedPrefix}:${encodeURIComponent(scopedIdentity)}`;
};
