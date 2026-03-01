package com.bml.core.common.enums;

import com.bml.core.common.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码定义。
 * <p>
 * 约定说明：
 * </p>
 * <ul>
 *     <li>200-599：与 HTTP 语义强相关的通用业务码</li>
 *     <li>1000-1999：系统级错误</li>
 *     <li>2000-2099：认证与登录态错误</li>
 *     <li>2100-2199：OpenAPI 调用错误</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录态已失效"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后重试"),

    // 系统级错误 1000-1999
    SYSTEM_ERROR(1000, "系统内部错误"),
    LICENSE_INVALID(1001, "License 无效或已过期"),
    LICENSE_EXPIRED(1002, "License 已过期"),
    LICENSE_MACHINE_ERROR(1003, "机器码不匹配"),

    // 认证与登录态错误 2000-2099
    TOKEN_EXPIRED(2001, "登录已过期，请重新登录"),
    TOKEN_INVALID(2002, "无效的 Token"),
    TOKEN_REFRESH_EXPIRED(2003, "刷新令牌已过期，请重新登录"),
    ACCOUNT_DISABLED(2004, "账号已被禁用"),
    ACCOUNT_LOCKED(2005, "账号已被锁定"),

    // OpenAPI 错误 2100-2199
    OPEN_API_HEADER_MISSING(2101, "缺少必要的签名请求头"),
    OPEN_API_TIMESTAMP_INVALID(2102, "时间戳格式错误"),
    OPEN_API_REQUEST_EXPIRED(2103, "请求已过期"),
    OPEN_API_APP_INVALID(2104, "无效的应用凭证"),
    OPEN_API_FORBIDDEN(2105, "当前应用无权访问该接口"),
    OPEN_API_REPLAY_REQUEST(2106, "重复请求已被拒绝"),
    OPEN_API_SIGNATURE_INVALID(2107, "签名校验失败"),
    OPEN_API_SIGN_VERSION_INVALID(2108, "签名算法版本不匹配"),
    OPEN_API_IP_FORBIDDEN(2109, "当前来源 IP 不在白名单内");

    private final int code;
    private final String message;
}
