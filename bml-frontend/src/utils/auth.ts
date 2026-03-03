const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const LEGACY_TOKEN_KEY = 'token';
const USER_IDENTITY_KEY = 'authUserIdentity';

type JwtPayload = Record<string, unknown>;
const USER_IDENTITY_CLAIM_KEYS = ['userId', 'user_id', 'uid', 'sub', 'username', 'userName', 'loginName', 'name'] as const;

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY);

export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

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
    const storedIdentity = localStorage.getItem(USER_IDENTITY_KEY)?.trim();
    if (storedIdentity) {
        return storedIdentity;
    }

    const token = getAccessToken();
    if (!token) {
        return '';
    }

    const identityFromToken = resolveUserIdentityFromJwt(token);
    if (identityFromToken) {
        localStorage.setItem(USER_IDENTITY_KEY, identityFromToken);
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
    const explicitIdentity = payload.userIdentity?.trim();
    if (explicitIdentity) {
        localStorage.setItem(USER_IDENTITY_KEY, explicitIdentity);
        return;
    }

    const existingIdentity = localStorage.getItem(USER_IDENTITY_KEY)?.trim();
    if (existingIdentity) {
        return;
    }

    const identityFromToken = resolveUserIdentityFromJwt(payload.accessToken);
    if (identityFromToken) {
        localStorage.setItem(USER_IDENTITY_KEY, identityFromToken);
    }
};

export const setAuthTokens = (payload: { accessToken: string; refreshToken?: string; userIdentity?: string }) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    if (payload.refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
    }

    /**
     * 用户标识用于列表布局等“按账号隔离”的前端本地配置。
     * - 登录时优先落入传入的 userIdentity（通常为用户名）；
     * - 刷新 token 时若未传入，优先沿用当前标识；仅在缺失时才从新 token 提取。
     */
    persistUserIdentity(payload);
};

export const clearAuthTokens = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    localStorage.removeItem(USER_IDENTITY_KEY);
};

export const redirectToLogin = () => {
    if (window.location.pathname !== '/admin/login') {
        window.location.href = '/admin/login';
    }
};
