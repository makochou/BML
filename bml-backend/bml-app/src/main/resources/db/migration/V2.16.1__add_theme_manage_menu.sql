-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.16.1
-- 说明: 新增"主题管理"菜单与 system:theme:manage 权限，
--       用于管理平台级主题预设（增/删/改），默认仅授予超级管理员。
--
-- 菜单层级：系统管理(id=4) → 基础配置(id=60) → 主题管理(id=74)
-- 权限标识：system:theme:manage（控制预设 CRUD 接口访问）
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、主题管理菜单（C 类型，挂载于"基础配置"目录下）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES (74, '主题管理', 60, 'C', 'theme', 'system/theme/index', 'system:theme:list', 'palette', 6, 1, 1, 0, '平台主题预设管理，支持新增、编辑、删除自定义预设方案', 1, NOW())
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `perms` = VALUES(`perms`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `is_frame` = VALUES(`is_frame`),
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、主题管理按钮权限（B 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (7401, '查询预设', 74, 'B', 'system:theme:list', 1, 1, 1, '主题管理 — 查询主题预设列表', 1, NOW()),
    (7402, '新增预设', 74, 'B', 'system:theme:manage', 2, 1, 1, '主题管理 — 新增/编辑/删除自定义主题预设', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、超级管理员授权（role_id=1）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 74, 0),
    (1, 7401, 0),
    (1, 7402, 0);
