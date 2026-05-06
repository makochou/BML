package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色信息表
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@Schema(description = "角色信息表")
public class SysRole extends BaseEntity {

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    @TableField("role_key")
    private String roleCode;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "数据范围 (1:全部 2:本组织及下级 3:仅本组织 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义 8:本人及下属)")
    private Integer dataScope;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单组")
    @TableField(exist = false)
    private List<Long> menuIds;

    @Schema(description = "自定义数据权限时的部门ID列表")
    @TableField(exist = false)
    private List<Long> customDeptIds;
}
