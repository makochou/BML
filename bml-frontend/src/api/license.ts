import request from '../utils/request';

/**
 * 许可证状态响应类型。
 */
export interface LicenseStatus {
    /** 许可证校验是否启用 */
    enabled: boolean;
    /** 是否已激活 */
    activated: boolean;
    /** 是否已过期 */
    expired: boolean;
    /** 许可证 ID */
    licenseId?: string;
    /** 客户名称 */
    customerName?: string;
    /** 客户编码 */
    customerCode?: string;
    /** 授权产品版本 */
    productVersion?: string;
    /** 授权功能模块 */
    features?: string[];
    /** 最大 API 账号数 */
    maxApiAccounts?: number;
    /** 单账号最大用户数（API 来源用户上限） */
    maxUsersPerAccount?: number;
    /** 全局最大用户数（前台业务系统用户上限） */
    maxTotalUsers?: number;

    // ── 配额使用量（含所有状态，用于前端配额进度展示） ──

    /** 当前已创建的 API 账号数量（含所有状态） */
    currentApiAccounts?: number;
    /** 当前处于启用状态的 API 账号数量 */
    activeApiAccounts?: number;
    /** 当前已创建的前台业务用户数量（含所有状态） */
    currentTotalUsers?: number;
    /** 当前处于启用状态的前台业务用户数量 */
    activeTotalUsers?: number;
    /** 所有 API 账号累计创建的用户总数（含所有状态） */
    currentApiUsers?: number;
    /** 所有 API 账号累计创建的、且处于启用状态的用户数 */
    activeApiUsers?: number;

    // ── 配额降级冻结统计（许可证更新时返回） ──

    /** 本次更新后被自动冻结（停用）的超额业务用户数量 */
    frozenUserCount?: number;
    /** 本次更新后被自动冻结（停用）的超额 API 账号数量 */
    frozenApiAccountCount?: number;
    /** 本次更新后被自动冻结的超额 API 创建的用户数量 */
    frozenApiUserCount?: number;

    /** 签发日期 */
    issueDate?: string;
    /** 到期日期 */
    expireDate?: string;
    /** 附加说明 */
    remark?: string;
    /** 许可证文件绝对路径 */
    filePath?: string;
    /** 错误信息 */
    errorMessage?: string;
}

/**
 * 查询许可证状态。
 * 无需登录即可调用，用于路由守卫和许可证页面判断系统激活状态。
 */
export function fetchLicenseStatus() {
    return request.get<LicenseStatus>('/system/license/status', {
        _skipAuthRefresh: true,
        _skipErrorMessage: true
    } as any);
}

/**
 * 预览许可证文件（仅验证解析，不保存）。
 * 用于更新许可证前的对比预览。
 *
 * @param file 许可证 .lic 文件
 */
export function previewLicense(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post<LicenseStatus>('/system/license/preview', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        _skipAuthRefresh: true
    } as any);
}

/**
 * 上传许可证文件。
 * 无需登录即可调用。
 *
 * @param file 许可证 .lic 文件
 */
export function uploadLicense(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post<LicenseStatus>('/system/license/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        _skipAuthRefresh: true
    } as any);
}

/**
 * 更新许可证文件（授权升级）。
 * 适用于客户购买更多用户数、更多功能模块后替换旧许可证的场景。
 * 后端会自动备份旧许可证文件，无需担心数据丢失。
 *
 * @param file 新许可证 .lic 文件
 */
export function updateLicense(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post<LicenseStatus>('/system/license/update', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        _skipAuthRefresh: true
    } as any);
}

/**
 * 删除许可证文件并重置状态。
 * 仅用于开发测试环境。
 */
export function resetLicense() {
    return request.delete<LicenseStatus>('/system/license/reset', {
        _skipAuthRefresh: true
    } as any);
}
