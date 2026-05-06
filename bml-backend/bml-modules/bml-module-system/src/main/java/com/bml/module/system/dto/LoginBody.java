package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;

/**
 * 登录请求体
 * <p>
 * 包含账号、密码以及可选的图形验证码参数。
 * 当系统开启验证码功能时，captchaKey 和 captchaCode 为必填项。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "登录请求体")
public class LoginBody {

    @Schema(description = "账号", requiredMode = RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "密码", requiredMode = RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "验证码唯一标识（由 /auth/captcha 接口返回）")
    private String captchaKey;

    @Schema(description = "用户输入的验证码")
    private String captchaCode;
}
