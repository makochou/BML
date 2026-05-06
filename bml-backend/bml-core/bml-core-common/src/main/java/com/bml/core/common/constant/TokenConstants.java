package com.bml.core.common.constant;

/**
 * Token 相关常量定义
 * <p>
 * 集中管理所有与 JWT Token、Redis缓存 相关的常量，
 * 避免硬编码在各个业务类中，方便统一维护和修改。
 * </p>
 *
 * <h3>使用说明：</h3>
 * <ul>
 *   <li>Redis Key 前缀：使用 {@link #LOGIN_TOKEN_KEY} 拼接用户唯一标识（UUID）</li>
 *   <li>Token 类型：通过 {@link #TOKEN_TYPE_ACCESS} 和 {@link #TOKEN_TYPE_REFRESH} 区分双令牌</li>
 *   <li>自动续期：当 Token 剩余有效期小于 {@link #MILLIS_MINUTE_TWENTY} 时触发自动续期</li>
 * </ul>
 *
 * @author BML Team
 */
public interface TokenConstants {

    // ======================== Redis Key 前缀 ========================

    /**
     * 登录用户 Redis Key 前缀
     * <p>
     * 完整的 Redis Key 格式为: {@code login_tokens:{uuid}}
     * 其中 uuid 是登录时生成的唯一标识
     * </p>
     */
    String LOGIN_TOKEN_KEY = "login_tokens:";

    // ======================== Token 过期时间（毫秒） ========================

    /**
     * AccessToken 默认过期时间：2小时（毫秒）
     * <p>
     * 可通过配置项 {@code bml.jwt.access-token-expiration} 覆盖
     * </p>
     */
    long ACCESS_TOKEN_EXPIRATION = 2 * 60 * 60 * 1000L;

    /**
     * RefreshToken 默认过期时间：7天（毫秒）
     * <p>
     * 可通过配置项 {@code bml.jwt.refresh-token-expiration} 覆盖
     * </p>
     */
    long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    // ======================== Token 请求头 ========================

    /**
     * Token 请求头前缀
     * <p>
     * 标准 Bearer Token 格式: {@code Authorization: Bearer {token}}
     * </p>
     */
    String TOKEN_PREFIX = "Bearer ";

    /**
     * Token 请求头名称
     */
    String TOKEN_HEADER = "Authorization";

    // ======================== JWT Claims 字段名 ========================

    /**
     * Claims 字段：用户唯一标识（UUID）
     * <p>
     * 对应 Redis 中的 Key 后缀，用于从 Redis 恢复完整的 LoginUser 对象
     * </p>
     */
    String CLAIMS_KEY_USER_KEY = "user_key";

    /**
     * Claims 字段：用户ID
     */
    String CLAIMS_KEY_USER_ID = "user_id";

    /**
     * Claims 字段：账号
     */
    String CLAIMS_KEY_USERNAME = "username";

    /**
     * Claims 字段：Token 类型
     * <p>
     * 取值为 {@link #TOKEN_TYPE_ACCESS} 或 {@link #TOKEN_TYPE_REFRESH}
     * </p>
     */
    String CLAIMS_KEY_TOKEN_TYPE = "token_type";

    // ======================== Token 类型标识 ========================

    /**
     * Token 类型：访问令牌
     */
    String TOKEN_TYPE_ACCESS = "access";

    /**
     * Token 类型：刷新令牌
     */
    String TOKEN_TYPE_REFRESH = "refresh";

    // ======================== 自动续期阈值 ========================

    /**
     * 自动续期阈值：20分钟（毫秒）
     * <p>
     * 当用户的 AccessToken 剩余有效期小于此阈值时，
     * 系统会自动延长 Redis 中用户缓存的过期时间，实现无感续期。
     * </p>
     */
    long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;
}
