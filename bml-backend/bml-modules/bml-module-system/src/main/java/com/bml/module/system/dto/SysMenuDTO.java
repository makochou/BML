package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单信息 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "菜单信息传输对象")
public class SysMenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 1, max = 50, message = "菜单名称长度必须在1到50个字符之间")
    private String menuName;

    @Schema(description = "菜单类型 (M:目录 C:菜单 B:按钮)")
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "是否显示 (1:显示 0:隐藏)")
    private Integer visible;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "是否外链 (1:是 0:否)")
    private Integer isFrame;

    @Schema(description = "备注")
    private String remark;
}
