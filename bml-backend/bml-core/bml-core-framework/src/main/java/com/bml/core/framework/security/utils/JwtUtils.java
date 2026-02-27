package com.bml.core.framework.security.utils;

import com.bml.core.common.constant.TokenConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * <p>
 * 封装 JWT Token 的创建、解析和验证功能。基于 JJWT(0.12.5) 实现，
 * 使用 HMAC-SHA256 对称加密签名。
 * </p>
 *
 * <h3>双令牌机制说明：</h3>
 * <ul>
 * <li><b>AccessToken</b> — 有效期短（默认2小时），用于日常API访问</li>
 * <li><b>RefreshToken</b> — 有效期长（默认7天），仅用于刷新AccessToken</li>
 * </ul>
 *
 * <h3>配置项：</h3>
 * 
 * <pre>
 * bml:
 *   jwt:
 *     secret: "your-very-long-secret-key-at-least-32-bytes-long"
 *     access-token-expiration: 7200000    # AccessToken过期时间（毫秒），默认2小时
 *     refresh-token-expiration: 604800000 # RefreshToken过期时间（毫秒），默认7天
 * </pre>
 *
 * @author BML Team
 */
@Component
public class JwtUtils {

    /**
     * JWT 签名密钥
     * <p>
     * 必须至少32字节（256位），用于 HMAC-SHA256 签名。
     * 生产环境建议使用环境变量或配置中心管理，切勿硬编码。
     * </p>
     */
    @Value("${bml.jwt.secret}")
    private String secret;

    /**
     * AccessToken 过期时间（毫秒），默认2小时
     */
    @Value("${bml.jwt.access-token-expiration:7200000}")
    private long accessTokenExpiration;

    /**
     * RefreshToken 过期时间（毫秒），默认7天
     */
    @Value("${bml.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    /**
     * 获取 HMAC-SHA 签名密钥
     *
     * @return SecretKey 对象
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ======================== 创建 Token ========================

    /**
     * 创建 AccessToken
     * <p>
     * AccessToken 中会包含 {@link TokenConstants#CLAIMS_KEY_TOKEN_TYPE} = "access"，
     * 以及调用方传入的所有自定义 claims（如 userKey、userId、username）。
     * </p>
     *
     * @param claims 自定义声明信息（包含 userKey、userId、username 等）
     * @return 签名后的 JWT 字符串
     */
    public String createAccessToken(Map<String, Object> claims) {
        claims.put(TokenConstants.CLAIMS_KEY_TOKEN_TYPE, TokenConstants.TOKEN_TYPE_ACCESS);
        return buildToken(claims, accessTokenExpiration);
    }

    /**
     * 创建 RefreshToken
     * <p>
     * RefreshToken 仅用于刷新 AccessToken，有效期更长。
     * 其中 {@link TokenConstants#CLAIMS_KEY_TOKEN_TYPE} = "refresh"。
     * </p>
     *
     * @param claims 自定义声明信息（包含 userKey、userId、username 等）
     * @return 签名后的 JWT 字符串
     */
    public String createRefreshToken(Map<String, Object> claims) {
        claims.put(TokenConstants.CLAIMS_KEY_TOKEN_TYPE, TokenConstants.TOKEN_TYPE_REFRESH);
        return buildToken(claims, refreshTokenExpiration);
    }

    /**
     * 构建 JWT Token（内部方法）
     *
     * @param claims     自定义声明
     * @param expiration 过期时间（毫秒）
     * @return JWT 字符串
     */
    private String buildToken(Map<String, Object> claims, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ======================== 解析 Token ========================

    /**
     * 解析 JWT Token 中的 Claims（声明）
     * <p>
     * 会自动验证签名和过期时间，如果 Token 无效则抛出异常。
     * </p>
     *
     * @param token JWT 字符串
     * @return Claims 对象
     * @throws io.jsonwebtoken.JwtException Token 无效时抛出（签名错误、已过期等）
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取 Subject（主题）
     *
     * @param token JWT 字符串
     * @return Subject 值
     */
    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    // ======================== 验证 Token ========================

    /**
     * 验证 Token 是否有效
     * <p>
     * 检查签名正确性和是否过期。不抛出异常，返回布尔值。
     * </p>
     *
     * @param token JWT 字符串
     * @return true=有效, false=无效
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== Getter（供外部查询配置值） ========================

    /**
     * 获取 AccessToken 过期时间（毫秒）
     *
     * @return 过期时间毫秒数
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * 获取 RefreshToken 过期时间（毫秒）
     *
     * @return 过期时间毫秒数
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
