package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 VO
 *
 * @author BML Team
 */
@Data
@Schema(description = "用户信息视图对象")
public class SysUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "用户名")
    private String nickname;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "用户性别 (0:未知 1:男 2:女)")
    private Integer gender;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "所属机构名称")
    private String orgName;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "入职日期")
    private LocalDate entryDate;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "角色名称列表")
    private List<String> roleNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateBy;

}
