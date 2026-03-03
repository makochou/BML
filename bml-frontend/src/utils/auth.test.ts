import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearAuthTokens, getAccessToken, getCurrentUserIdentity, getRefreshToken, setAuthTokens } from './auth';

const createJwt = (payload: Record<string, unknown>) => {
    const toBase64Url = (input: string) =>
        Buffer.from(input, 'utf-8')
            .toString('base64')
            .replace(/=/g, '')
            .replace(/\+/g, '-')
            .replace(/\//g, '_');

    const header = toBase64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = toBase64Url(JSON.stringify(payload));
    return `${header}.${body}.signature`;
};

describe('auth utils', () => {
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

        if (typeof globalThis.atob !== 'function') {
            vi.stubGlobal('atob', (value: string) => Buffer.from(value, 'base64').toString('binary'));
        }
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('stores both access token and refresh token', () => {
        setAuthTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

        expect(getAccessToken()).toBe('access-1');
        expect(getRefreshToken()).toBe('refresh-1');
    });

    it('keeps explicit user identity after token refresh', () => {
        const refreshedAccessToken = createJwt({ userId: 10086, username: 'token-user' });

        setAuthTokens({ accessToken: 'access-1', refreshToken: 'refresh-1', userIdentity: 'Alice' });
        setAuthTokens({ accessToken: refreshedAccessToken, refreshToken: 'refresh-2' });

        expect(getCurrentUserIdentity()).toBe('Alice');
    });

    it('derives user identity from jwt when local identity is absent', () => {
        const accessToken = createJwt({ username: 'alice-from-jwt' });

        setAuthTokens({ accessToken, refreshToken: 'refresh-1' });

        expect(getCurrentUserIdentity()).toBe('alice-from-jwt');
    });

    it('clears all auth related tokens', () => {
        setAuthTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

        clearAuthTokens();

        expect(getAccessToken()).toBeNull();
        expect(getRefreshToken()).toBeNull();
    });
});
