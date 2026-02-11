package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新 Token 请求对象
 * <p>
 * 当 AccessToken 过期时，前端使用此 DTO 携带 RefreshToken 请求获取新的 AccessToken。
 * </p>
 *
 * <h3>使用场景：</h3>
 * 
 * <pre>
 * POST /auth/refresh
 * Content-Type: application/json
 *
 * {
 *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
 * }
 * </pre>
 *
 * @author BML Team
 * @see com.bml.module.system.vo.TokenVO 响应对象
 */
@Data
@Schema(description = "刷新Token请求对象")
public class RefreshTokenDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 刷新令牌
     * <p>
     * 登录时返回的 RefreshToken，有效期较长（默认7天）。
     * 用于在 AccessToken 过期后获取新的 AccessToken。
     * </p>
     */
    @NotBlank(message = "refreshToken不能为空")
    @Schema(description = "刷新令牌（RefreshToken）", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
