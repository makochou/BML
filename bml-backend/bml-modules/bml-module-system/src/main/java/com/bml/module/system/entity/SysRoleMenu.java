package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色和菜单关联表（中间表）
 * <p>
 * 存储角色与菜单/按钮/字段权限的关联关系。
 * 中间表仅存储关联关系的外键，不继承 {@code BaseEntity}，
 * 无需 id、create_by、update_by、deleted 等审计字段。
 * 删除关联关系时应物理删除而非逻辑删除。
 * </p>
 *
 * <h3>半选状态（half_check）说明：</h3>
 * <p>
 * 前端权限树使用 Arco Design {@code a-tree} 的 checkable 模式，
 * 父节点的勾选状态由子节点自动推导。保存时需要区分：
 * <ul>
 *   <li>{@code halfCheck=0}：完全勾选 — 该权限已授予</li>
 *   <li>{@code halfCheck=1}：半选（indeterminate）— 仅部分子权限授予</li>
 * </ul>
 * 加载时只把 {@code halfCheck=0} 的节点设为 checkedKeys，
 * 树组件自动计算半选态，保证前端回显的正确性。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_role_menu")
@Schema(description = "角色和菜单关联表")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "是否半选状态：0 完全勾选（权限已授予），1 半选（仅部分子权限授予）")
    private Integer halfCheck;
}
