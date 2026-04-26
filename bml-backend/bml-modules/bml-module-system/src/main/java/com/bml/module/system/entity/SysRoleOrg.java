package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色自定义数据权限 — 角色机构关联表（中间表）
 * <p>
 * 中间表仅存储关联关系的外键，不继承 {@code BaseEntity}，
 * 无需 id、create_by、update_by、deleted 等审计字段。
 * 删除关联关系时应物理删除而非逻辑删除。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_role_org")
@Schema(description = "角色自定义数据权限 — 角色机构关联表")
public class SysRoleOrg implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "机构ID")
    private Long orgId;
}
