-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.14.0
-- 说明: 新增用户菜单权限关联表，支持对单个用户进行独立的功能授权
--       （在角色权限基础上，额外为特定用户分配/限制菜单权限）
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、用户菜单权限关联表（多对多）
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `sys_user_menu` (
    `user_id`    BIGINT   NOT NULL COMMENT '用户 ID',
    `menu_id`    BIGINT   NOT NULL COMMENT '菜单 ID',
    `half_check` TINYINT  NOT NULL DEFAULT 0 COMMENT '半选标记：0=完全勾选，1=半选（父节点部分子节点被选中）',
    PRIMARY KEY (`user_id`, `menu_id`),
    KEY `idx_user_menu_menu_id` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户菜单权限关联表（个人功能授权）';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、新增按钮权限：功能授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (419, '功能授权', 41, 'B', 'system:user:assignPerms', 6, 1, 1, '用户管理 — 为单个用户分配独立的菜单/按钮/字段权限', 1, NOW())
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `perms` = VALUES(`perms`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、超级管理员授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES (1, 419, 0);
