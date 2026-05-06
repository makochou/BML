package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码 DTO
 * <p>
 * 用于当前登录用户修改自己的密码。
 * 需要提供旧密码进行身份验证，新密码需满足长度要求。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "修改密码传输对象")
public class ChangePasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "旧密码（当前密码）")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码（6~30个字符）")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 30, message = "新密码长度必须在6到30个字符之间")
    private String newPassword;
}
