package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;

/**
 * 角色信息 VO
 *
 * @author BML Team
 */
@Data
@Schema(description = "角色信息视图对象")
public class SysRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "数据范围")
    private Integer dataScope;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "菜单ID列表（完全勾选的菜单/按钮/字段 ID）")
    private List<Long> menuIds;

    @Schema(description = "半选菜单 ID 列表（仅部分子权限授予的父节点，用于前端权限树回显）")
    private List<Long> halfCheckMenuIds;

    @Schema(description = "自定义数据权限时的机构ID列表")
    private List<Long> customOrgIds;

    @Schema(description = "自定义数据权限时的部门ID列表")
    private List<Long> customDeptIds;

    @Schema(description = "创建人ID")
    private Long createBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "更新人ID")
    private Long updateBy;

}
