/**
 * 系统配置 API 接口层
 * <p>
 * 用于中台管理平台的系统配置管理，包括验证码开关、品牌图片上传替换等。
 * 所有接口均需要管理员 JWT 认证。
 * </p>
 */
import request from '../utils/request';

/**
 * 查询登录相关配置
 */
export function fetchLoginConfig() {
    return request.get<Record<string, string>>('/system/config/login');
}

/**
 * 批量更新配置项
 * @param configs key-value 配置对象
 */
export function batchUpdateConfig(configs: Record<string, string>) {
    return request.put('/system/config/batch', configs);
}

/**
 * 上传品牌图片
 * @param type 图片类型：loginBg / loginCardBg / favicon
 * @param file 图片文件
 */
export function uploadBrandingImage(type: string, file: File) {
    const formData = new FormData();
    formData.append('type', type);
    formData.append('file', file);
    return request.post<{ url: string }>('/system/config/branding/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
}

/**
 * 删除品牌图片（恢复默认）
 * @param type 图片类型
 */
export function deleteBrandingImage(type: string) {
    return request.delete(`/system/config/branding/${type}`);
}
