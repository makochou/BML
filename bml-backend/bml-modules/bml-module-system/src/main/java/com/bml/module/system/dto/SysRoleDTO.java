package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色信息 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "角色信息传输对象")
public class SysRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID (更新时必填)")
    private Long id;

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    @Size(min = 1, max = 30, message = "角色名称长度必须在1到30个字符之间")
    private String roleName;

    @Schema(description = "角色编码")
    @NotBlank(message = "角色编码不能为空")
    @Size(min = 1, max = 30, message = "角色编码长度必须在1到30个字符之间")
    private String roleCode;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "数据范围 (1:全部 2:本组织及下级 3:仅本组织 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义 8:本人及下属)")
    private Integer dataScope;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单组（完全勾选的菜单/按钮/字段 ID 列表）")
    private List<Long> menuIds;

    @Schema(description = "半选菜单 ID 列表（仅部分子权限授予的父节点，用于前端权限树回显）")
    private List<Long> halfCheckMenuIds;

    @Schema(description = "自定义数据权限时的机构 ID 列表")
    private List<Long> customOrgIds;

    @Schema(description = "自定义数据权限时的部门 ID 列表")
    private List<Long> customDeptIds;
}
