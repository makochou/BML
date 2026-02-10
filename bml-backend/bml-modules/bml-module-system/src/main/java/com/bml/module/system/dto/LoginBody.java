package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求对象
 *
 * @author BML Team
 */
@Data
@Schema(description = "登录请求对象")
public class LoginBody {

    @Schema(description = "用户名", required = true)
    private String username;

    @Schema(description = "密码", required = true)
    private String password;
}
