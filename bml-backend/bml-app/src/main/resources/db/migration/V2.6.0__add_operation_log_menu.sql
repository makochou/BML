-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.6.0
-- 说明: 新增系统操作日志菜单与权限
--
-- 设计说明：
--   1. 业务系统前端路由为硬编码，后端 sys_menu 主要用于角色授权与权限校验。
--   2. 菜单管理 id=43 已在历史版本中删除，本次使用 id=47，避免与旧菜单及按钮 431-434 冲突。
--   3. 操作日志作为审计能力挂在系统管理根目录 id=4 下，角色授权面板可直接分配 system:operlog:* 权限。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、新增操作日志页面菜单（C 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
VALUES (47, '操作日志', 4, 'C', 'operlog', 'system/operlog/index', 'system:operlog:list', 'history', 7, 1, 1);

UPDATE `sys_menu`
SET `menu_name` = '操作日志',
    `parent_id` = 4,
    `menu_type` = 'C',
    `path` = 'operlog',
    `component` = 'system/operlog/index',
    `perms` = 'system:operlog:list',
    `icon` = 'history',
    `sort` = 7,
    `visible` = 1,
    `status` = 1
WHERE `id` = 47;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、新增操作日志按钮权限（B 类型）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES
    (471, '查询操作日志', 47, 'B', 'system:operlog:query',  1, 1, 1),
    (472, '删除操作日志', 47, 'B', 'system:operlog:remove', 2, 1, 1),
    (473, '清空操作日志', 47, 'B', 'system:operlog:clean',  3, 1, 1);

UPDATE `sys_menu` SET `menu_name` = '查询操作日志', `parent_id` = 47, `menu_type` = 'B', `perms` = 'system:operlog:query',  `sort` = 1, `visible` = 1, `status` = 1 WHERE `id` = 471;
UPDATE `sys_menu` SET `menu_name` = '删除操作日志', `parent_id` = 47, `menu_type` = 'B', `perms` = 'system:operlog:remove', `sort` = 2, `visible` = 1, `status` = 1 WHERE `id` = 472;
UPDATE `sys_menu` SET `menu_name` = '清空操作日志', `parent_id` = 47, `menu_type` = 'B', `perms` = 'system:operlog:clean',  `sort` = 3, `visible` = 1, `status` = 1 WHERE `id` = 473;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、超级管理员授予操作日志权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 47, 0),
    (1, 471, 0),
    (1, 472, 0),
    (1, 473, 0);

-- 确保父级系统管理目录在超级管理员角色下保持授权，便于权限树完整回显。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4, 0);
