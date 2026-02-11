package com.bml.core.framework.security.service;

import com.bml.core.common.constant.TokenConstants;
import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.model.TokenVO;
import com.bml.core.framework.security.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token 统一管理服务
 * <p>
 * 负责 Token 的完整生命周期管理：创建、解析、刷新、删除和自动续期。
 * 采用 <b>Redis + JWT</b> 组合方案：
 * </p>
 * <ul>
 * <li>JWT Token 中仅存放用户唯一标识（UUID userKey），体积小、安全</li>
 * <li>完整的 {@link LoginUser}（含权限列表）存储在 Redis 中</li>
 * <li>支持实时踢下线（删除 Redis 即可）</li>
 * <li>支持权限变更即时生效（更新 Redis 中的 LoginUser）</li>
 * </ul>
 *
 * <h3>Redis Key 格式：</h3>
 * 
 * <pre>
 * login_tokens:{uuid}  →  LoginUser JSON
 * </pre>
 *
 * <h3>Token 流程：</h3>
 * <ol>
 * <li>登录成功 → 生成 UUID → 存入 Redis → 生成 AccessToken + RefreshToken</li>
 * <li>请求到达 → 从 Token 提取 UUID → 从 Redis 恢复 LoginUser → 设置 SecurityContext</li>
 * <li>Token 快过期 → 自动延长 Redis 缓存有效期（无感续期）</li>
 * <li>刷新 Token → 验证 RefreshToken → 生成新 AccessToken</li>
 * <li>登出 → 删除 Redis 中的用户缓存</li>
 * </ol>
 *
 * @author BML Team
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final JwtUtils jwtUtils;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造函数注入依赖
     *
     * @param jwtUtils      JWT 工具类
     * @param redisTemplate Redis 操作模板
     */
    public TokenService(JwtUtils jwtUtils, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    // ======================== 创建 Token ========================

    /**
     * 创建双令牌（AccessToken + RefreshToken）
     * <p>
     * 流程：
     * <ol>
     * <li>生成 UUID 作为用户唯一标识（userKey）</li>
     * <li>记录登录时间和过期时间</li>
     * <li>将完整的 LoginUser 对象缓存到 Redis</li>
     * <li>生成 AccessToken 和 RefreshToken</li>
     * </ol>
     * </p>
     *
     * @param loginUser 登录用户信息（由 UserDetailsService 构建）
     * @return 包含 accessToken 和 refreshToken 的 Map
     */
    public TokenVO createToken(LoginUser loginUser) {
        // 1. 生成用户唯一标识
        String userKey = UUID.randomUUID().toString().replace("-", "");
        loginUser.setUserKey(userKey);

        // 2. 记录登录时间
        long currentTimeMillis = System.currentTimeMillis();
        loginUser.setLoginTime(currentTimeMillis);
        loginUser.setExpireTime(currentTimeMillis + jwtUtils.getRefreshTokenExpiration());

        // 3. 缓存到 Redis（过期时间与 RefreshToken 保持一致）
        refreshCache(loginUser);

        // 4. 构建 JWT Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.CLAIMS_KEY_USER_KEY, userKey);
        claims.put(TokenConstants.CLAIMS_KEY_USER_ID, loginUser.getUserId());
        claims.put(TokenConstants.CLAIMS_KEY_USERNAME, loginUser.getUsername());

        // 5. 生成双 Token
        // 注意：必须分别创建新Map，因为 createAccessToken/createRefreshToken 会向 claims 中追加
        // token_type
        String accessToken = jwtUtils.createAccessToken(new HashMap<>(claims));
        String refreshToken = jwtUtils.createRefreshToken(new HashMap<>(claims));

        // 6. 组装返回结果
        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtils.getAccessTokenExpiration() / 1000)
                .build();
    }

    // ======================== 获取登录用户 ========================

    /**
     * 从 HTTP 请求中获取当前登录用户
     * <p>
     * 流程：
     * <ol>
     * <li>从请求头 {@code Authorization} 中提取 Bearer Token</li>
     * <li>解析 JWT 获取 userKey</li>
     * <li>从 Redis 中恢复完整的 LoginUser 对象</li>
     * </ol>
     * </p>
     *
     * @param request HTTP 请求
     * @return LoginUser 对象，Token 无效或 Redis 中不存在时返回 null
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        // 1. 从请求头提取 Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return getLoginUserByToken(token);
    }

    /**
     * 通过 Token 字符串获取登录用户
     *
     * @param token JWT 字符串
     * @return LoginUser 对象，无效时返回 null
     */
    public LoginUser getLoginUserByToken(String token) {
        try {
            // 2. 解析 JWT
            Claims claims = jwtUtils.extractClaims(token);
            String userKey = (String) claims.get(TokenConstants.CLAIMS_KEY_USER_KEY);
            if (!StringUtils.hasText(userKey)) {
                return null;
            }

            // 3. 从 Redis 恢复 LoginUser
            String redisKey = getRedisKey(userKey);
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof LoginUser loginUser) {
                return loginUser;
            }

            return null;
        } catch (Exception e) {
            log.warn("解析Token获取用户信息失败: {}", e.getMessage());
            return null;
        }
    }

    // ======================== 刷新 Token ========================

    /**
     * 刷新 AccessToken
     * <p>
     * 验证 RefreshToken 的有效性和类型，如果合法则生成新的 AccessToken。
     * RefreshToken 本身不会更新，直到过期后用户需要重新登录。
     * </p>
     *
     * @param refreshToken 刷新令牌
     * @return 包含新 accessToken 和 expiresIn 的 Map
     * @throws com.bml.core.common.exception.BusinessException RefreshToken 无效或已过期
     */
    public TokenVO refreshAccessToken(String refreshToken) {
        // 1. 验证 RefreshToken
        Claims claims;
        try {
            claims = jwtUtils.extractClaims(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.TOKEN_REFRESH_EXPIRED);
        }

        // 2. 校验 Token 类型必须是 refresh
        String tokenType = (String) claims.get(TokenConstants.CLAIMS_KEY_TOKEN_TYPE);
        if (!TokenConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BusinessException(GlobalErrorCode.TOKEN_INVALID);
        }

        // 3. 检查 Redis 中用户是否还存在（可能已被踢下线）
        String userKey = (String) claims.get(TokenConstants.CLAIMS_KEY_USER_KEY);
        String redisKey = getRedisKey(userKey);
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached == null) {
            throw new BusinessException(GlobalErrorCode.TOKEN_EXPIRED);
        }

        // 4. 生成新的 AccessToken（复用原有的 claims 信息）
        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put(TokenConstants.CLAIMS_KEY_USER_KEY, userKey);
        newClaims.put(TokenConstants.CLAIMS_KEY_USER_ID, claims.get(TokenConstants.CLAIMS_KEY_USER_ID));
        newClaims.put(TokenConstants.CLAIMS_KEY_USERNAME, claims.get(TokenConstants.CLAIMS_KEY_USERNAME));

        String newAccessToken = jwtUtils.createAccessToken(newClaims);

        return TokenVO.builder()
                .accessToken(newAccessToken)
                .expiresIn(jwtUtils.getAccessTokenExpiration() / 1000)
                .build();
    }

    // ======================== 自动续期 ========================

    /**
     * 自动刷新 Redis 缓存过期时间
     * <p>
     * 当用户持续活跃时，如果 Redis 缓存剩余时间小于 20 分钟，
     * 自动延长缓存有效期，避免用户在使用过程中突然被踢下线。
     * 此机制对用户完全透明（无感续期）。
     * </p>
     *
     * @param loginUser 当前登录用户
     */
    public void refreshTokenExpireTime(LoginUser loginUser) {
        long currentTime = System.currentTimeMillis();
        if (loginUser.getExpireTime() != null) {
            long remainTime = loginUser.getExpireTime() - currentTime;
            // 剩余时间小于20分钟时自动续期
            if (remainTime <= TokenConstants.MILLIS_MINUTE_TWENTY) {
                loginUser.setExpireTime(currentTime + jwtUtils.getRefreshTokenExpiration());
                refreshCache(loginUser);
                log.debug("用户[{}]的Token缓存已自动续期", loginUser.getUsername());
            }
        }
    }

    // ======================== 更新用户信息 ========================

    /**
     * 更新 Redis 中的登录用户信息
     * <p>
     * 当用户权限发生变化（如管理员修改了角色权限）时，
     * 可调用此方法实时更新 Redis 中的 LoginUser，
     * 使权限变更即时生效，无需用户重新登录。
     * </p>
     *
     * @param loginUser 更新后的登录用户信息
     */
    public void setLoginUser(LoginUser loginUser) {
        if (loginUser != null && StringUtils.hasText(loginUser.getUserKey())) {
            refreshCache(loginUser);
        }
    }

    // ======================== 删除登录用户（登出） ========================

    /**
     * 删除登录用户的 Redis 缓存（登出操作）
     * <p>
     * 删除后，该用户的所有 Token（包括 AccessToken 和 RefreshToken）
     * 均无法再从 Redis 中恢复 LoginUser，从而实现即时登出。
     * </p>
     *
     * @param userKey 用户唯一标识（UUID）
     */
    public void deleteLoginUser(String userKey) {
        if (StringUtils.hasText(userKey)) {
            String redisKey = getRedisKey(userKey);
            redisTemplate.delete(redisKey);
            log.info("用户[{}]已登出，Redis缓存已清除", userKey);
        }
    }

    // ======================== 私有工具方法 ========================

    /**
     * 从请求头中提取 Token
     * <p>
     * 从 {@code Authorization} 请求头中提取 Bearer Token，
     * 去掉 "Bearer " 前缀后返回纯 Token 字符串。
     * </p>
     *
     * @param request HTTP 请求
     * @return Token 字符串，不存在时返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(TokenConstants.TOKEN_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TokenConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(TokenConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 刷新 Redis 缓存
     * <p>
     * 将 LoginUser 序列化存储到 Redis，设置与 RefreshToken 相同的过期时间。
     * </p>
     *
     * @param loginUser 登录用户信息
     */
    private void refreshCache(LoginUser loginUser) {
        String redisKey = getRedisKey(loginUser.getUserKey());
        redisTemplate.opsForValue().set(redisKey, loginUser,
                jwtUtils.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);
    }

    /**
     * 构建 Redis Key
     *
     * @param userKey 用户唯一标识（UUID）
     * @return 完整的 Redis Key，格式：{@code login_tokens:{userKey}}
     */
    private String getRedisKey(String userKey) {
        return TokenConstants.LOGIN_TOKEN_KEY + userKey;
    }
}
