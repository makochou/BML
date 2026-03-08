import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { Message } from '@arco-design/web-vue';
import { clearAuthTokens, getAccessToken, getRefreshToken, redirectToLogin, setAuthTokens } from './auth';

/**
 * 后端统一响应结构。
 * <p>
 * 前端请求层只认这一套协议，避免不同接口返回不同字段名称。
 * </p>
 */
type ApiResponse<T = unknown> = {
    code: number;
    message: string;
    data: T;
    timestamp: number;
    traceId?: string;
};

type RetryableRequestConfig = InternalAxiosRequestConfig & {
    _retry?: boolean;
    _skipAuthRefresh?: boolean;
    _skipErrorMessage?: boolean;
};

/**
 * 接口请求根路径，与后端 server.servlet.context-path 一致。
 * 用于 API 调试面板等需要自行拼接 URL 的场景。
 */
export const apiBaseURL = import.meta.env.VITE_API_BASE_URL || '/api';

const service = axios.create({
    baseURL: apiBaseURL,
    timeout: 10000
});

let refreshTokenPromise: Promise<string | null> | null = null;

/**
 * 在控制台打印后端返回的 traceId，便于联调和日志排查。
 */
const logTraceId = (payload?: Pick<ApiResponse, 'traceId' | 'code' | 'message'>) => {
    if (!payload?.traceId) {
        return;
    }
    console.warn(`[API Trace] code=${payload.code} traceId=${payload.traceId} message=${payload.message}`);
};

const logoutAndRedirect = (message?: string) => {
    clearAuthTokens();
    if (message) {
        Message.error(message);
    }
    redirectToLogin();
};

const refreshAccessToken = async (): Promise<string | null> => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        return null;
    }
    if (!refreshTokenPromise) {
        refreshTokenPromise = axios.post<ApiResponse<{ accessToken: string; refreshToken?: string }>>(
            `${apiBaseURL}/auth/refresh`,
            { refreshToken },
            { timeout: 10000 }
        ).then(response => {
            const payload = response.data;
            if (payload.code !== 200 || !payload.data?.accessToken) {
                logTraceId(payload);
                throw new Error(payload.message || '刷新登录状态失败');
            }
            setAuthTokens({
                accessToken: payload.data.accessToken,
                refreshToken: payload.data.refreshToken || refreshToken
            });
            return payload.data.accessToken;
        }).catch(error => {
            logoutAndRedirect('登录已失效，请重新登录');
            throw error;
        }).finally(() => {
            refreshTokenPromise = null;
        });
    }
    return refreshTokenPromise;
};

const replayWithFreshToken = async (config?: RetryableRequestConfig) => {
    if (!config || config._retry || config._skipAuthRefresh) {
        throw new Error('Unauthorized');
    }
    config._retry = true;
    const newAccessToken = await refreshAccessToken();
    if (!newAccessToken) {
        logoutAndRedirect('登录已失效，请重新登录');
        throw new Error('Unauthorized');
    }
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${newAccessToken}`;
    return service(config);
};

service.interceptors.request.use(
    config => {
        const token = getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

(service.interceptors.response.use as any)(
    async (response: any) => {
        const payload = response.data as ApiResponse;
        const config = response.config as RetryableRequestConfig;

        if (payload.code === 200) {
            return payload;
        }
        if (payload.code === 401) {
            return replayWithFreshToken(config);
        }

        logTraceId(payload);
        const message = payload.message || 'Request Error';
        if (!config?._skipErrorMessage) {
            Message.error(message);
        }
        return Promise.reject(new Error(message));
    },
    async (error: AxiosError<ApiResponse>) => {
        const config = error.config as RetryableRequestConfig | undefined;
        if (error.response?.status === 401) {
            try {
                return await replayWithFreshToken(config);
            } catch (refreshError) {
                return Promise.reject(refreshError);
            }
        }

        logTraceId(error.response?.data);
        const message = error.response?.data?.message || error.message || 'Request Error';
        if (!config?._skipErrorMessage) {
            Message.error(message);
        }
        return Promise.reject(error);
    }
);

/**
 * 获取用于 API 调试的 HTTP 客户端。
 * 该实例仅注入 Authorization 头，不做响应体统一解析与 401 自动刷新，
 * 便于调试面板展示原始 HTTP 状态码、响应头与响应体。
 *
 * @returns 带 baseURL 与 Token 注入的 axios 实例，调用方将得到完整 AxiosResponse
 */
export function getDebugHttpClient() {
    const client = axios.create({
        baseURL: apiBaseURL,
        timeout: 30000
    });
    client.interceptors.request.use(config => {
        const token = getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    }, error => Promise.reject(error));
    return client;
}

export default service;
