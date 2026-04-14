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
    /** 单账号最大用户数 */
    maxUsersPerAccount?: number;
    /** 全局最大用户数 */
    maxTotalUsers?: number;
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
 * 删除许可证文件并重置状态。
 * 仅用于开发测试环境。
 */
export function resetLicense() {
    return request.delete<LicenseStatus>('/system/license/reset', {
        _skipAuthRefresh: true
    } as any);
}
