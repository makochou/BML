const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const LEGACY_TOKEN_KEY = 'token';

export const getAccessToken = () => localStorage.getItem(ACCESS_TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY);

export const getRefreshToken = () => localStorage.getItem(REFRESH_TOKEN_KEY);

export const setAuthTokens = (payload: { accessToken: string; refreshToken?: string }) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    if (payload.refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
    }
};

export const clearAuthTokens = () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
};

export const redirectToLogin = () => {
    if (window.location.pathname !== '/admin/login') {
        window.location.href = '/admin/login';
    }
};
