import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { clearAuthTokens, getAccessToken, getRefreshToken, setAuthTokens } from './auth';

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
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('stores both access token and refresh token', () => {
        setAuthTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

        expect(getAccessToken()).toBe('access-1');
        expect(getRefreshToken()).toBe('refresh-1');
    });

    it('clears all auth related tokens', () => {
        setAuthTokens({ accessToken: 'access-1', refreshToken: 'refresh-1' });

        clearAuthTokens();

        expect(getAccessToken()).toBeNull();
        expect(getRefreshToken()).toBeNull();
    });
});
