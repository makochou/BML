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

    @Schema(description = "数据范围 (1:全部 2:本部门 3:本部门及以下 4:仅本人 5:自定义)")
    private Integer dataScope;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单组")
    private List<Long> menuIds;
}
