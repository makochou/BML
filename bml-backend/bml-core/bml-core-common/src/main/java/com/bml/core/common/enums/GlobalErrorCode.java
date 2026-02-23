package com.bml.core.common.enums;

import com.bml.core.common.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 *
 * @author BML Team
 */
@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或Token已失效"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后重试"),

    // 系统级错误 1000-1999
    SYSTEM_ERROR(1000, "系统内部错误"),
    LICENSE_INVALID(1001, "License无效或已过期"),
    LICENSE_EXPIRED(1002, "License已过期"),
    LICENSE_MACHINE_ERROR(1003, "机器码不匹配"),

    // 安全模块错误 2000-2999
    /** 登录已过期（AccessToken过期且Redis会话已失效） */
    TOKEN_EXPIRED(2001, "登录已过期，请重新登录"),
    /** 无效的Token（签名错误、格式异常等） */
    TOKEN_INVALID(2002, "无效的Token"),
    /** 刷新令牌已过期，必须重新登录 */
    TOKEN_REFRESH_EXPIRED(2003, "刷新令牌已过期，请重新登录"),
    /** 账号已被管理员禁用 */
    ACCOUNT_DISABLED(2004, "账号已被禁用"),
    /** 账号因多次密码错误已被锁定 */
    ACCOUNT_LOCKED(2005, "账号已被锁定");

    private final int code;
    private final String message;
}
