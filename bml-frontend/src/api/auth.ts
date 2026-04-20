/**
 * 认证相关 API
 * <p>
 * 包含验证码获取、登录配置查询等无需认证即可调用的接口。
 * </p>
 */
import request from '../utils/request';

/** 登录配置项（验证码开关、品牌图片等） */
export interface LoginConfig {
    /** 验证码开关（'true' / 'false'） */
    'sys.login.captchaEnabled'?: string;
    /** 登录页全屏背景图 URL */
    'sys.login.bgImage'?: string;
    /** 浏览器标签图标 URL */
    'sys.login.favicon'?: string;
    /** 侧边栏 Logo 图片 URL（业务系统左侧导航顶部） */
    'sys.sidebar.logo'?: string;
    /** 品牌标题（登录页左侧展示） */
    'sys.login.brandTitle'?: string;
    /** 品牌副标题 */
    'sys.login.brandSlogan'?: string;
    /** 品牌描述文字 */
    'sys.login.brandDesc'?: string;
    [key: string]: string | undefined;
}

/** 验证码响应 */
export interface CaptchaResult {
    /** 验证码唯一标识 */
    captchaKey: string;
    /** Base64 编码的 PNG 图片（含 data:image/png;base64, 前缀） */
    captchaImage: string;
}

/**
 * 获取登录页配置（验证码开关、背景图等）
 * <p>无需认证，用于登录页初始化时获取动态配置。</p>
 */
export function fetchLoginConfig() {
    return request.get<LoginConfig>('/auth/login/config', {
        _skipAuthRefresh: true,
        _skipErrorMessage: true
    } as any);
}

/**
 * 获取图形验证码
 * <p>返回 Base64 图片和唯一 key，登录时需一并提交。</p>
 */
export function fetchCaptcha() {
    return request.get<CaptchaResult>('/auth/captcha', {
        _skipAuthRefresh: true,
        _skipErrorMessage: true
    } as any);
}
