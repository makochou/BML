import request from '../utils/request';
import type {
    ApiAccountDetail,
    ApiAccountItem,
    ApiCallbackLogItem,
    ApiAccountPageQuery,
    ApiCallbackLogPagePayload,
    ApiCallbackLogPageQuery,
    ApiAuthorizationSnapshot,
    ApiCredentialPayload,
    OpenApiGroupNode,
    OpenApiRegistrySyncResult,
    OpenApiRegistryTreeQuery,
    PageResult,
    SaveApiAccountPayload
} from '../types/apiAccount';

type ApiPromise<T> = Promise<{ data: T }>;

export const fetchApiAccountPage = (params: ApiAccountPageQuery): ApiPromise<PageResult<ApiAccountItem>> => {
    return request.get('/account/page', { params }) as ApiPromise<PageResult<ApiAccountItem>>;
};

export const fetchApiAccountList = (): ApiPromise<ApiAccountItem[]> => {
    return request.get('/account/list') as ApiPromise<ApiAccountItem[]>;
};

export const fetchApiAccountDetail = (id: number): ApiPromise<ApiAccountDetail> => {
    return request.get(`/account/${id}`) as ApiPromise<ApiAccountDetail>;
};

export const fetchApiAccountCopy = (id: number): ApiPromise<ApiAccountDetail> => {
    return request.get(`/account/${id}/copy`) as ApiPromise<ApiAccountDetail>;
};

export const createApiAccount = (payload: SaveApiAccountPayload): ApiPromise<ApiCredentialPayload> => {
    return request.post('/account', payload) as ApiPromise<ApiCredentialPayload>;
};

export const updateApiAccount = (id: number, payload: SaveApiAccountPayload): ApiPromise<void> => {
    return request.put(`/account/${id}`, payload) as ApiPromise<void>;
};

export const updateApiAccountStatus = (id: number, status: number): ApiPromise<void> => {
    return request.put(`/account/${id}/status`, { status }) as ApiPromise<void>;
};

export const deleteApiAccount = (id: number): ApiPromise<void> => {
    return request.delete(`/account/${id}`) as ApiPromise<void>;
};

export const resetApiAccountSecret = (id: number): ApiPromise<ApiCredentialPayload> => {
    return request.put(`/account/${id}/secret/reset`) as ApiPromise<ApiCredentialPayload>;
};

export const triggerApiAccountTestCallback = (id: number): ApiPromise<ApiCallbackLogItem> => {
    return request.post(`/account/${id}/callback/test`) as ApiPromise<ApiCallbackLogItem>;
};

export const fetchApiCallbackLogs = (
    id: number,
    params: ApiCallbackLogPageQuery
): ApiPromise<ApiCallbackLogPagePayload> => {
    return request.get(`/account/${id}/callback-log/page`, { params }) as ApiPromise<ApiCallbackLogPagePayload>;
};

export const retryApiCallbackLog = (logId: number): ApiPromise<ApiCallbackLogItem> => {
    return request.post(`/account/callback-log/${logId}/retry`) as ApiPromise<ApiCallbackLogItem>;
};

export const fetchAuthorizationSnapshot = (id: number): ApiPromise<ApiAuthorizationSnapshot> => {
    return request.get(`/account/${id}/authorization`) as ApiPromise<ApiAuthorizationSnapshot>;
};

export const saveAuthorization = (id: number, apiIds: number[]): ApiPromise<void> => {
    return request.put(`/account/${id}/authorization`, { apiIds }) as ApiPromise<void>;
};

export const fetchOpenApiRegistryTree = (
    params?: OpenApiRegistryTreeQuery
): ApiPromise<OpenApiGroupNode[]> => {
    return request.get('/openapi/registry/tree', { params }) as ApiPromise<OpenApiGroupNode[]>;
};

export const syncOpenApiRegistry = (): ApiPromise<OpenApiRegistrySyncResult> => {
    return request.post('/openapi/registry/sync') as ApiPromise<OpenApiRegistrySyncResult>;
};
