package com.bml.core.framework.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Token 响应视图对象
 * <p>
 * 用于登录成功和刷新 Token 时的统一响应结构。
 * 前端收到该对象后，应将 {@code accessToken} 存储在内存中（推荐）或 localStorage 中，
 * 并在后续请求的 {@code Authorization} 头中携带。
 * </p>
 *
 * <h3>前端使用示例：</h3>
 * 
 * <pre>
 * // 登录成功后存储 Token
 * const { accessToken, refreshToken, expiresIn } = response.data;
 * localStorage.setItem('accessToken', accessToken);
 * localStorage.setItem('refreshToken', refreshToken);
 *
 * // 请求时携带 Token
 * axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
 *
 * // Token 过期时使用 refreshToken 刷新
 * const { accessToken: newToken } = await api.post('/auth/refresh', { refreshToken });
 * </pre>
 *
 * @author BML Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token响应对象")
public class TokenVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问令牌
     * <p>
     * 用于日常 API 访问，有效期较短（默认2小时）。
     * 需在请求头中以 {@code Bearer {accessToken}} 格式携带。
     * </p>
     */
    @Schema(description = "访问令牌（AccessToken）", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    /**
     * 刷新令牌
     * <p>
     * 用于在 AccessToken 过期后获取新的 AccessToken，有效期较长（默认7天）。
     * 仅在调用 {@code POST /auth/refresh} 时使用。
     * </p>
     */
    @Schema(description = "刷新令牌（RefreshToken）", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    /**
     * AccessToken 有效期（秒）
     * <p>
     * 前端可据此设置定时器，在 Token 过期前主动刷新。
     * 例如 7200 表示 2小时。
     * </p>
     */
    @Schema(description = "AccessToken有效期（秒）", example = "7200")
    private Long expiresIn;
}
