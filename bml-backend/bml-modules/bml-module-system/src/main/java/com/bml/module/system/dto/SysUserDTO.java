package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
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

    @Schema(description = "账号")
    @NotBlank(message = "账号不能为空")
    @Size(min = 5, max = 30, message = "账号长度必须在5到30个字符之间")
    private String username;

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 30, message = "用户名长度必须在2到30个字符之间")
    private String nickname;

    @Schema(description = "用户邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    @Schema(description = "手机号码")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号码格式不正确")
    private String phone;

    @Schema(description = "用户性别 (0:未知 1:男 2:女)")
    private Integer gender;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "密码 (创建时必填)")
    private String password;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "工号")
    @Size(max = 30, message = "工号长度不能超过30个字符")
    private String employeeNo;

    @Schema(description = "入职日期")
    private LocalDate entryDate;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
