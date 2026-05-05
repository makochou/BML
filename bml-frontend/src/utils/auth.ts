/**
 * ═══════════════════════════════════════════════════════════
 * 双入口认证隔离方案
 * ───────────────────────────────────────────────────────────
 * 项目有两个独立入口：
 *   1. 中台管理平台：ip:port/admin（scope = 'admin'）
 *   2. 前台业务系统：ip:port/     （scope = 'biz'）
 *
 * 两个入口的 token 使用不同的 localStorage 键前缀，
 * 退出其中一个入口不会影响另一个入口的登录状态。
 *
 * 存储键命名规则：
 *   admin 入口 → admin_accessToken / admin_refreshToken / admin_authUserIdentity
 *   业务 入口 → biz_accessToken   / biz_refreshToken   / biz_authUserIdentity
 * ═══════════════════════════════════════════════════════════
 */

/** 认证作用域类型 */
type AuthScope = 'admin' | 'biz';

/**
 * 根据当前页面路径自动判断所属认证作用域。
 * /admin 开头 → 中台管理平台；其他 → 前台业务系统。
 */
export const getAuthScope = (): AuthScope =>
    window.location.pathname.startsWith('/admin') ? 'admin' : 'biz';

/** 基础键名（不含前缀） */
const BASE_ACCESS_TOKEN_KEY = 'accessToken';
const BASE_REFRESH_TOKEN_KEY = 'refreshToken';
const BASE_USER_IDENTITY_KEY = 'authUserIdentity';

/** 旧版单键名（用于兼容迁移，读取后自动清除） */
const LEGACY_TOKEN_KEY = 'token';
const LEGACY_ACCESS_TOKEN_KEY = 'accessToken';
const LEGACY_REFRESH_TOKEN_KEY = 'refreshToken';
const LEGACY_USER_IDENTITY_KEY = 'authUserIdentity';

/** 根据作用域构造实际存储键 */
const scopedKey = (scope: AuthScope, base: string): string => `${scope}_${base}`;

type JwtPayload = Record<string, unknown>;
const USER_IDENTITY_CLAIM_KEYS = ['userId', 'user_id', 'uid', 'sub', 'username', 'userName', 'loginName', 'name'] as const;

/**
 * 获取当前作用域的 accessToken。
 * 优先读取新的带前缀键，兼容旧的无前缀键（首次迁移场景）。
 */
export const getAccessToken = (): string | null => {
    const scope = getAuthScope();
    const token = localStorage.getItem(scopedKey(scope, BASE_ACCESS_TOKEN_KEY));
    if (token) {
        return token;
    }
    // 兼容旧版：读取无前缀键并自动迁移到当前作用域
    const legacy = localStorage.getItem(LEGACY_ACCESS_TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY);
    if (legacy) {
        localStorage.setItem(scopedKey(scope, BASE_ACCESS_TOKEN_KEY), legacy);
        localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
        localStorage.removeItem(LEGACY_TOKEN_KEY);
        return legacy;
    }
    return null;
};

/**
 * 获取当前作用域的 refreshToken。
 */
export const getRefreshToken = (): string | null => {
    const scope = getAuthScope();
    const token = localStorage.getItem(scopedKey(scope, BASE_REFRESH_TOKEN_KEY));
    if (token) {
        return token;
    }
    // 兼容旧版
    const legacy = localStorage.getItem(LEGACY_REFRESH_TOKEN_KEY);
    if (legacy) {
        localStorage.setItem(scopedKey(scope, BASE_REFRESH_TOKEN_KEY), legacy);
        localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY);
        return legacy;
    }
    return null;
};

/**
 * 解码 JWT payload。
 * 若 token 非 JWT 或解码失败，返回 null，调用侧按匿名/兜底流程处理。
 */
const parseJwtPayload = (token: string): JwtPayload | null => {
    const segments = token.split('.');
    if (segments.length < 2) {
        return null;
    }
    const payloadSegment = segments[1];
    if (!payloadSegment) {
        return null;
    }

    const base64 = payloadSegment.replace(/-/g, '+').replace(/_/g, '/');
    const paddingLength = (4 - (base64.length % 4)) % 4;
    const paddedBase64 = `${base64}${'='.repeat(paddingLength)}`;
    const atobFn = typeof globalThis.atob === 'function' ? globalThis.atob.bind(globalThis) : null;
    if (!atobFn) {
        return null;
    }

    try {
        const decoded = atobFn(paddedBase64);
        const utf8Decoded = decodeURIComponent(
            Array.from(decoded)
                .map(character => `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`)
                .join('')
        );
        return JSON.parse(utf8Decoded) as JwtPayload;
    } catch {
        try {
            const fallbackDecoded = atobFn(paddedBase64);
            return JSON.parse(fallbackDecoded) as JwtPayload;
        } catch {
            return null;
        }
    }
};

/**
 * 从 JWT 常见用户字段中提取稳定的用户标识。
 * 优先返回字符串或数字类型，便于做“按用户隔离”的本地存储键。
 */
const resolveUserIdentityFromJwt = (token: string): string => {
    const payload = parseJwtPayload(token);
    if (!payload) {
        return '';
    }
    for (const claimKey of USER_IDENTITY_CLAIM_KEYS) {
        const hit = payload[claimKey];
        if (typeof hit === 'string' && hit.trim()) {
            return hit.trim();
        }
        if (typeof hit === 'number' && Number.isFinite(hit)) {
            return String(hit);
        }
    }
    return '';
};

/**
 * 返回当前登录用户标识：
 * 1) 优先使用登录时缓存的用户名/账号；
 * 2) 其次尝试从 accessToken 的 JWT payload 提取；
 * 3) 都失败则返回空字符串，由业务侧走匿名兜底键。
 */
export const getCurrentUserIdentity = () => {
    const scope = getAuthScope();
    const identityKey = scopedKey(scope, BASE_USER_IDENTITY_KEY);
    const storedIdentity = localStorage.getItem(identityKey)?.trim();
    if (storedIdentity) {
        return storedIdentity;
    }

    // 兼容旧版无前缀键
    const legacyIdentity = localStorage.getItem(LEGACY_USER_IDENTITY_KEY)?.trim();
    if (legacyIdentity) {
        localStorage.setItem(identityKey, legacyIdentity);
        localStorage.removeItem(LEGACY_USER_IDENTITY_KEY);
        return legacyIdentity;
    }

    const token = getAccessToken();
    if (!token) {
        return '';
    }

    const identityFromToken = resolveUserIdentityFromJwt(token);
    if (identityFromToken) {
        localStorage.setItem(identityKey, identityFromToken);
    }
    return identityFromToken;
};

/**
 * 统一处理用户标识写入策略：
 * 1) 登录时若显式传入 userIdentity，直接覆盖为登录账号；
 * 2) 刷新 token 等未显式传入时，优先保留已有标识，避免列表布局等按账号配置被覆盖；
 * 3) 本地尚无标识时，再从 token 尝试提取并持久化。
 */
const persistUserIdentity = (payload: { accessToken: string; userIdentity?: string }) => {
    const scope = getAuthScope();
    const identityKey = scopedKey(scope, BASE_USER_IDENTITY_KEY);
    const explicitIdentity = payload.userIdentity?.trim();
    if (explicitIdentity) {
        localStorage.setItem(identityKey, explicitIdentity);
        return;
    }

    const existingIdentity = localStorage.getItem(identityKey)?.trim();
    if (existingIdentity) {
        return;
    }

    const identityFromToken = resolveUserIdentityFromJwt(payload.accessToken);
    if (identityFromToken) {
        localStorage.setItem(identityKey, identityFromToken);
    }
};

export const setAuthTokens = (payload: { accessToken: string; refreshToken?: string; userIdentity?: string }) => {
    const scope = getAuthScope();
    localStorage.setItem(scopedKey(scope, BASE_ACCESS_TOKEN_KEY), payload.accessToken);
    // 清理旧版无前缀键（迁移完成后不再使用）
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
    if (payload.refreshToken) {
        localStorage.setItem(scopedKey(scope, BASE_REFRESH_TOKEN_KEY), payload.refreshToken);
        localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY);
    }

    /**
     * 用户标识用于列表布局等"按账号隔离"的前端本地配置。
     * - 登录时优先落入传入的 userIdentity（通常为用户名）；
     * - 刷新 token 时若未传入，优先沿用当前标识；仅在缺失时才从新 token 提取。
     */
    persistUserIdentity(payload);
};

export const clearAuthTokens = () => {
    const scope = getAuthScope();
    // 只清除当前作用域的 token，不影响另一个入口
    localStorage.removeItem(scopedKey(scope, BASE_ACCESS_TOKEN_KEY));
    localStorage.removeItem(scopedKey(scope, BASE_REFRESH_TOKEN_KEY));
    localStorage.removeItem(scopedKey(scope, BASE_USER_IDENTITY_KEY));
    // 清除最后活动时间，确保下次访问必须重新登录
    localStorage.removeItem(scopedKey(scope, 'lastActivityTime'));
    // 同时清理旧版无前缀键（如果还残留）
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    localStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
    localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_USER_IDENTITY_KEY);
};

export const redirectToLogin = () => {
    const currentPath = window.location.pathname;
    // 根据当前路径判断应跳转哪个登录页
    if (currentPath.startsWith('/admin')) {
        if (currentPath !== '/admin/login') {
            window.location.href = '/admin/login';
        }
    } else {
        if (currentPath !== '/login') {
            window.location.href = '/login';
        }
    }
};

// ═══════════════════════════════════════════════════════════════
// 最后活动时间持久化
// ───────────────────────────────────────────────────────────────
// 空闲超时检测需要在页面刷新 / 浏览器重启后仍然有效。
// 将最后活动时间戳存入 localStorage，启动时与配置的超时时长比较，
// 若已超时则立即触发登出，确保关闭页面后再打开也能正确过期。
// ═══════════════════════════════════════════════════════════════

const BASE_LAST_ACTIVITY_KEY = 'lastActivityTime';

/**
 * 记录当前时间为最后活动时间（按作用域隔离存储）。
 * 由空闲超时 composable 在用户交互事件中调用。
 */
export const setLastActivityTime = (): void => {
    const scope = getAuthScope();
    localStorage.setItem(scopedKey(scope, BASE_LAST_ACTIVITY_KEY), String(Date.now()));
};

/**
 * 获取上次活动时间戳（毫秒）。
 * 若不存在则返回 null，表示尚未记录过活动时间。
 */
export const getLastActivityTime = (): number | null => {
    const scope = getAuthScope();
    const raw = localStorage.getItem(scopedKey(scope, BASE_LAST_ACTIVITY_KEY));
    if (!raw) return null;
    const ts = parseInt(raw, 10);
    return isNaN(ts) ? null : ts;
};

/**
 * 清除当前作用域的最后活动时间（登出时调用）。
 */
export const clearLastActivityTime = (): void => {
    const scope = getAuthScope();
    localStorage.removeItem(scopedKey(scope, BASE_LAST_ACTIVITY_KEY));
};

/**
 * 检查会话是否因空闲超时而过期。
 * @param timeoutMinutes 配置的超时时长（分钟），<= 0 表示不限制
 * @returns true = 已超时应该登出, false = 仍在有效期内
 */
export const isSessionIdleExpired = (timeoutMinutes: number): boolean => {
    if (timeoutMinutes <= 0) return false;
    const lastActivity = getLastActivityTime();
    if (lastActivity === null) return false; // 从未记录过，视为首次启动
    const elapsed = Date.now() - lastActivity;
    return elapsed > timeoutMinutes * 60 * 1000;
};

// ═══════════════════════════════════════════════════════════════
// JWT 有效期校验
// ───────────────────────────────────────────────────────────────
// 路由守卫中使用：在放行前先检查 AccessToken 的 JWT exp 字段，
// 若已过期则尝试 refresh；若 refresh 也失败则跳转登录页。
// ═══════════════════════════════════════════════════════════════

/**
 * 检查当前 AccessToken 的 JWT 是否已过期。
 * @returns true = 已过期或无法解析, false = 仍在有效期内
 */
export const isAccessTokenExpired = (): boolean => {
    const token = getAccessToken();
    if (!token) return true;
    const payload = parseJwtPayload(token);
    if (!payload) return true;
    const exp = payload.exp;
    if (typeof exp !== 'number') return true;
    // JWT exp 是秒级时间戳，加 5 秒缓冲避免临界值竞态
    return Date.now() > (exp - 5) * 1000;
};
