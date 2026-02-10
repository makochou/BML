package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "用户信息传输对象")
public class SysUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID (更新时必填)")
    private Long id;

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 30, message = "用户名长度必须在4到30个字符之间")
    private String username;

    @Schema(description = "用户昵称")
    @NotBlank(message = "用户昵称不能为空")
    @Size(min = 2, max = 30, message = "用户昵称长度必须在2到30个字符之间")
    private String nickname;

    @Schema(description = "用户邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    @Schema(description = "手机号码")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    @Schema(description = "用户性别（0男 1女 2未知）")
    private Integer gender;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "密码 (创建时必填)")
    private String password;

    @Schema(description = "帐号状态（0正常 1停用）")
    private Integer status;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "岗位ID列表")
    private List<Long> postIds;

    @Schema(description = "备注")
    private String remark;
}
