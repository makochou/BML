package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户和角色关联表（中间表）
 * <p>
 * 中间表仅存储关联关系的外键，不继承 {@code BaseEntity}，
 * 无需 id、create_by、update_by、deleted 等审计字段。
 * 删除关联关系时应物理删除而非逻辑删除。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_user_role")
@Schema(description = "用户和角色关联表")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID")
    private Long roleId;
}
