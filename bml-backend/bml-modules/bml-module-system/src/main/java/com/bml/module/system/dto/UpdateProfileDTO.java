package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人信息修改 DTO
 * <p>
 * 用于当前登录用户修改自己的账号和用户名。
 * 仅允许修改有限字段，不涉及角色、机构等管理类字段。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "个人信息修改传输对象")
public class UpdateProfileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "新账号（允许5~30个字符）")
    @NotBlank(message = "账号不能为空")
    @Size(min = 5, max = 30, message = "账号长度必须在5到30个字符之间")
    private String username;

    @Schema(description = "用户名（允许2~30个字符）")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 30, message = "用户名长度必须在2到30个字符之间")
    private String nickname;
}
