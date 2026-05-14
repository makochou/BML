package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户菜单权限关联实体（个人功能授权）。
 * <p>
 * 用于为单个用户分配独立的菜单/按钮/字段权限，
 * 在角色权限基础上进行额外的权限补充。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_user_menu")
public class SysUserMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @TableField("user_id")
    private Long userId;

    /** 菜单 ID */
    @TableField("menu_id")
    private Long menuId;

    /** 半选标记：0=完全勾选，1=半选（父节点部分子节点被选中） */
    @TableField("half_check")
    private Integer halfCheck;
}
