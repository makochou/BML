package com.bml.core.common.enums;

import com.bml.core.common.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码定义。
 *
 * <p>错误码分段约定：</p>
 * <ul>
 *     <li>{@code 200}：操作成功</li>
 *     <li>{@code 400-499}：客户端请求错误（与 HTTP 语义对齐）</li>
 *     <li>{@code 500}：服务端内部错误</li>
 *     <li>{@code 1000-1999}：系统级错误</li>
 *     <li>{@code 2000-2099}：认证与登录态错误</li>
 *     <li>{@code 2100-2199}：OpenAPI 签名调用错误</li>
 * </ul>
 *
 * <p>使用规范：</p>
 * <ul>
 *     <li>业务模块自定义错误码请在各模块内部定义，不要在此处堆砌；</li>
 *     <li>此处只维护跨模块通用的基础错误码。</li>
 * </ul>
 *
 * @author BML Team
 */
@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    // ── 通用成功 ──
    SUCCESS(200, "操作成功"),

    // ── 客户端错误（4xx） ──
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录态已失效"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),

    // ── 服务端错误（5xx） ──
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后重试"),

    // ── 系统级错误（1000-1999） ──
    SYSTEM_ERROR(1000, "系统内部错误"),

    // ── 认证与登录态错误（2000-2099） ──
    /** AccessToken 已过期，需使用 RefreshToken 换取新 Token */
    TOKEN_EXPIRED(2001, "登录已过期，请重新登录"),
    /** Token 格式非法或签名验证失败 */
    TOKEN_INVALID(2002, "无效的 Token"),
    /** RefreshToken 已过期，需重新登录 */
    TOKEN_REFRESH_EXPIRED(2003, "刷新令牌已过期，请重新登录"),
    /** 账号状态为停用（status=0） */
    ACCOUNT_DISABLED(2004, "账号已被禁用"),
    /** 账号状态为锁定（status=2） */
    ACCOUNT_LOCKED(2005, "账号已被锁定"),

    // ── OpenAPI 签名调用错误（2100-2199） ──
    /** 请求头缺少 X-Bml-App-Key / X-Bml-Timestamp / X-Bml-Nonce / X-Bml-Sign 中的任意一个 */
    OPEN_API_HEADER_MISSING(2101, "缺少必要的签名请求头"),
    /** X-Bml-Timestamp 不是合法的毫秒时间戳数字 */
    OPEN_API_TIMESTAMP_INVALID(2102, "时间戳格式错误"),
    /** 请求时间戳与服务器时间偏差超过允许范围（默认 5 分钟） */
    OPEN_API_REQUEST_EXPIRED(2103, "请求已过期"),
    /** AccessKey 不存在、账号已停用或已过期 */
    OPEN_API_APP_INVALID(2104, "无效的应用凭证"),
    /** 账号未被授权访问当前接口 */
    OPEN_API_FORBIDDEN(2105, "当前应用无权访问该接口"),
    /** Nonce 已在 Redis 中存在，判定为重放攻击 */
    OPEN_API_REPLAY_REQUEST(2106, "重复请求已被拒绝"),
    /** HmacSHA256 签名值与服务端计算结果不一致 */
    OPEN_API_SIGNATURE_INVALID(2107, "签名校验失败"),
    /** 请求头中的签名版本与账号配置的签名版本不匹配 */
    OPEN_API_SIGN_VERSION_INVALID(2108, "签名算法版本不匹配"),
    /** 请求来源 IP 不在账号配置的白名单范围内 */
    OPEN_API_IP_FORBIDDEN(2109, "当前来源 IP 不在白名单内");

    /** 业务状态码 */
    private final int code;
    /** 可读错误消息 */
    private final String message;
}
