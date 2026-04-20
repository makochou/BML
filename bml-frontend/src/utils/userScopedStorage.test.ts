import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setAuthTokens } from './auth';
import { buildUserScopedStorageKey, normalizeUserScopedIdentity } from './userScopedStorage';

describe('userScopedStorage utils', () => {
    beforeEach(() => {
        const store = new Map<string, string>();
        vi.stubGlobal('localStorage', {
            getItem: (key: string) => store.get(key) ?? null,
            setItem: (key: string, value: string) => {
                store.set(key, value);
            },
            removeItem: (key: string) => {
                store.delete(key);
            }
        });

        // 模拟 window.location.pathname（双入口认证隔离需要）
        vi.stubGlobal('window', {
            location: { pathname: '/admin/dashboard' }
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('normalizes scoped identity with lower-case fallback', () => {
        expect(normalizeUserScopedIdentity('  AdminUser  ')).toBe('adminuser');
        expect(normalizeUserScopedIdentity('   ', ' Guest ')).toBe('guest');
    });

    it('builds key with explicit identity', () => {
        const key = buildUserScopedStorageKey('demo.layout', { identity: 'User-A' });
        expect(key).toBe('demo.layout:user-a');
    });

    it('builds key by current auth identity when identity option is not provided', () => {
        setAuthTokens({
            accessToken: 'access-1',
            refreshToken: 'refresh-1',
            userIdentity: 'CurrentLogin'
        });

        const key = buildUserScopedStorageKey('demo.layout');
        expect(key).toBe('demo.layout:currentlogin');
    });
});
