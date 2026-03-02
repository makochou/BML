export interface PageResult<T> {
    records: T[];
    total: number;
    pageNum: number;
    pageSize: number;
}

export type AccessEnvironment = 'test' | 'staging' | 'production';

export interface EnvironmentIpWhitelist {
    test: string[];
    staging: string[];
    production: string[];
}

export interface ApiAccountGovernanceFields {
    ownerName: string;
    ownerContact: string;
    systemName: string;
    systemCode: string;
    accessEnvironment: string;
    ipWhitelist: string[];
    environmentIpWhitelist: EnvironmentIpWhitelist;
    signVersion: string;
    callbackUrl?: string | null;
}

export interface ApiAccountItem extends ApiAccountGovernanceFields {
    id: number;
    accountName: string;
    accessKey: string;
    accountType: number;
    clientTypes: string[];
    rateLimit: number;
    expireTime?: string | null;
    status: number;
    remark?: string | null;
    authorizedApiCount: number;
    enabledAuthorizedApiCount: number;
    createTime?: string;
    updateTime?: string;
}

export interface ApiAccountDetail extends ApiAccountItem {}

export interface ApiCredentialPayload extends ApiAccountGovernanceFields {
    id: number;
    accountName: string;
    accessKey: string;
    clientTypes: string[];
    secretKey: string;
}

export interface ApiAccountPageQuery {
    pageNum: number;
    pageSize: number;
    accountId?: number;
    accountName?: string;
    accessKey?: string;
    ownerName?: string;
    ownerContact?: string;
    systemName?: string;
    systemCode?: string;
    systemKeyword?: string;
    signVersion?: string;
    callbackUrl?: string;
    remark?: string;
    ipKeyword?: string;
    textMatchMode?: 'fuzzy' | 'exact';
    status?: number;
    accountType?: number;
    clientType?: string;
    accessEnvironment?: string;
    rateLimitMin?: number;
    rateLimitMax?: number;
    expireTimeStart?: string;
    expireTimeEnd?: string;
    createTimeStart?: string;
    createTimeEnd?: string;
    updateTimeStart?: string;
    updateTimeEnd?: string;
}

export interface SaveApiAccountPayload extends ApiAccountGovernanceFields {
    accountName: string;
    accountType: number;
    clientTypes: string[];
    rateLimit: number;
    expireTime?: string | null;
    status: number;
    remark?: string | null;
}

export interface ApiAccountFormModel {
    accountName: string;
    ownerName: string;
    ownerContact: string;
    systemName: string;
    systemCode: string;
    accountType: number;
    clientTypes: string[];
    accessEnvironment: AccessEnvironment;
    signVersion: string;
    environmentIpWhitelistText: EnvironmentIpWhitelistTextMap;
    callbackUrl: string;
    rateLimit: number;
    expireTime?: string | null;
    status: number;
    remark: string;
}

export interface ApiCallbackLogFilterModel {
    callbackStatus?: number;
}

export interface ApiAuthorizationSummary {
    totalApiCount: number;
    enabledApiCount: number;
    selectedApiCount: number;
    selectedEnabledApiCount: number;
}

export interface OpenApiRegistryItem {
    id: number;
    apiName: string;
    apiUrl: string;
    httpMethod: string;
    description?: string | null;
    status: number;
}

export interface OpenApiControllerGroup {
    controllerName: string;
    apis: OpenApiRegistryItem[];
}

export interface OpenApiGroupNode {
    moduleName: string;
    controllers: OpenApiControllerGroup[];
}

export interface ApiAuthorizationSnapshot {
    account: ApiAccountItem;
    selectedApiIds: number[];
    groups: OpenApiGroupNode[];
    summary: ApiAuthorizationSummary;
}

export interface OpenApiRegistryTreeQuery {
    keyword?: string;
    method?: string;
    status?: number;
    moduleName?: string;
}

export interface OpenApiRegistrySyncResult {
    totalDiscovered: number;
    insertedCount: number;
    updatedCount: number;
    disabledCount: number;
    skippedCount: number;
}

export interface ApiCallbackLogItem {
    id: number;
    accountId: number;
    accountName: string;
    systemName?: string | null;
    systemCode?: string | null;
    businessType: string;
    eventType: string;
    callbackUrl: string;
    httpMethod: string;
    requestHeaders?: string | null;
    requestBody?: string | null;
    responseStatusCode?: number | null;
    responseBody?: string | null;
    callbackStatus: number;
    retryCount: number;
    maxRetryCount: number;
    nextRetryTime?: string | null;
    lastCallbackTime?: string | null;
    successTime?: string | null;
    lastErrorMessage?: string | null;
    createTime?: string | null;
    updateTime?: string | null;
}

export interface ApiCallbackLogSummary {
    totalCount: number;
    pendingCount: number;
    retryingCount: number;
    successCount: number;
    failedCount: number;
}

export interface ApiCallbackLogPagePayload {
    page: PageResult<ApiCallbackLogItem>;
    summary: ApiCallbackLogSummary;
}

export interface ApiCallbackLogPageQuery {
    pageNum: number;
    pageSize: number;
    callbackStatus?: number;
}

export interface EnvironmentIpWhitelistTextMap {
    test: string;
    staging: string;
    production: string;
}
