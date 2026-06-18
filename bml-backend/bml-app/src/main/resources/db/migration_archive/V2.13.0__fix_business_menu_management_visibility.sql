-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.13.0
-- 说明: 修复业务系统“菜单管理”重复展示与超级管理员授权一致性
--
-- 背景说明：
--   1. V1.0.0 初始化脚本中曾使用 id=43 作为“菜单管理”，并挂载在业务根目录 id=4 下。
--   2. 后续规范已明确不复用历史 id=43/431-434，V2.8.0 使用 id=53 与 5300+ 权限段重建菜单管理。
--   3. V2.12.0 又将 id=53 移动到“基础配置”(id=60) 下，但历史 id=43 及其按钮/字段若仍存在，
--      会导致角色授权树中出现两个“菜单管理”，也容易让用户误以为业务侧菜单缺失或挂载错误。
--
-- 处理策略：
--   1. 先将历史旧授权迁移到新菜单 id=53/5300+/5310+，尽量不丢失普通角色已有授权。
--   2. 再删除历史旧菜单 id=43 及其旧按钮/字段权限，保证授权树只保留“基础配置 -> 菜单管理”。
--   3. 最后补齐超级管理员角色授权，保证 admin 超级管理员拥有菜单管理入口与全部按钮/字段权限。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、确保新版菜单管理位于“基础配置”目录下
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu`
SET `menu_name` = '菜单管理',
    `parent_id` = 60,
    `menu_type` = 'C',
    `path` = 'menu',
    `component` = 'system/menu/index',
    `perms` = 'system:menu:list',
    `icon` = 'list',
    `sort` = 1,
    `visible` = 1,
    `status` = 1,
    `is_frame` = 0,
    `remark` = '业务系统菜单、按钮、字段权限元数据维护入口'
WHERE `id` = 53;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、将历史旧菜单授权迁移到新版菜单权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 53, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 43 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5301, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 431 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5302, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 432 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5303, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 433 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5304, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 434 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5305, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 4005 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5315, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 435 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5314, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 436 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5316, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 437 GROUP BY `role_id`;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
SELECT `role_id`, 5313, MIN(`half_check`) FROM `sys_role_menu` WHERE `menu_id` = 5051 GROUP BY `role_id`;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、清理历史旧菜单管理节点，避免授权树重复展示
-- ═══════════════════════════════════════════════════════════════════════════════
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (43, 431, 432, 433, 434, 435, 436, 437, 4005, 5051, 5052);

DELETE FROM `sys_menu`
WHERE `id` IN (431, 432, 433, 434, 435, 436, 437, 4005, 5051, 5052);

DELETE FROM `sys_menu`
WHERE `id` = 43;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、补齐超级管理员角色授权，确保 admin 具备菜单管理全部权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 4, 0),
    (1, 60, 0),
    (1, 53, 0),
    (1, 5301, 0),
    (1, 5302, 0),
    (1, 5303, 0),
    (1, 5304, 0),
    (1, 5305, 0),
    (1, 5311, 0),
    (1, 5312, 0),
    (1, 5313, 0),
    (1, 5314, 0),
    (1, 5315, 0),
    (1, 5316, 0),
    (1, 5317, 0),
    (1, 5318, 0);
