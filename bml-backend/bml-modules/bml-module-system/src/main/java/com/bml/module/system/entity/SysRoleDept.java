package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色自定义数据权限 — 角色部门关联表（中间表）
 * <p>
 * 当 sys_role.data_scope = 7（自定义）时，除了可以指定可访问的机构范围
 * （sys_role_org），还可以精细到部门粒度。本表存储角色与可访问部门的关联。
 * </p>
 * <p>
 * <b>与 sys_role_org 的关系：</b>
 * <ul>
 *   <li>sys_role_org 定义可访问的机构范围（粗粒度）</li>
 *   <li>sys_role_dept 定义可访问的部门范围（细粒度）</li>
 *   <li>查询时取二者并集，即机构范围内所有数据 + 指定部门的数据</li>
 * </ul>
 * </p>
 * <p>
 * 中间表仅存储关联关系的外键，不继承 {@code BaseEntity}，
 * 无需 id、create_by、update_by、deleted 等审计字段。
 * 删除关联关系时应物理删除而非逻辑删除。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_role_dept")
@Schema(description = "角色自定义数据权限 — 角色部门关联表")
public class SysRoleDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "部门ID")
    private Long deptId;
}
