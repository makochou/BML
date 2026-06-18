-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.12.0
-- 说明: 按业务侧“系统管理”新规划重组模块菜单目录
--
-- 目录规划：
--   1. 组织与权限：机构、部门、岗位、用户、角色
--   2. 基础配置：菜单、字典、参数、系统设置、文件
--   3. 消息中心：通知公告
--   4. 安全审计：审计总览、登录日志、操作日志、异常日志、风险告警、在线用户、归档策略
--   5. 运维管理：系统监控、缓存管理、定时任务、任务日志
--
-- 兼容说明：
--   1. 业务系统前端路由仍由 bml-frontend/src/router/index.ts 静态维护，sys_menu 负责授权和后端鉴权。
--   2. 不复用历史已删除菜单管理 id=43/431-434。
--   3. 历史 id=51 已在 V2.7.0 中作为“风险告警”使用，本次业务侧系统监控使用新菜单 id=73。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、系统管理下的四个新目录
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES
    (60, '基础配置', 4, 'M', 'base-config', NULL, NULL, 'settings', 2, 1, 1, 0, '业务系统基础配置、权限元数据和文件资源目录', 1, NOW()),
    (70, '消息中心', 4, 'M', 'message-center', NULL, NULL, 'notification', 3, 1, 1, 0, '业务系统通知公告与消息触达目录', 1, NOW()),
    (71, '安全审计', 4, 'M', 'security-audit', NULL, NULL, 'history', 4, 1, 1, 0, '业务系统安全审计、日志、告警和在线会话目录', 1, NOW()),
    (72, '运维管理', 4, 'M', 'ops-management', NULL, NULL, 'storage', 5, 1, 1, 0, '业务系统运行监控、缓存和任务调度目录', 1, NOW())
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
-- 二、组织与权限目录排序，仅保留组织权限类菜单
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `menu_name` = '系统管理', `sort` = 6, `visible` = 1, `status` = 1 WHERE `id` = 4;
UPDATE `sys_menu` SET `parent_id` = 4, `sort` = 1 WHERE `id` = 45;
UPDATE `sys_menu` SET `parent_id` = 4, `sort` = 2 WHERE `id` = 44;
UPDATE `sys_menu` SET `parent_id` = 4, `sort` = 3 WHERE `id` = 46;
UPDATE `sys_menu` SET `parent_id` = 4, `sort` = 4 WHERE `id` = 41;
UPDATE `sys_menu` SET `parent_id` = 4, `sort` = 5 WHERE `id` = 42;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、基础配置目录：菜单、字典、参数、系统设置、文件
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `parent_id` = 60, `sort` = 1 WHERE `id` = 53;
UPDATE `sys_menu` SET `parent_id` = 60, `sort` = 2 WHERE `id` = 61;
UPDATE `sys_menu` SET `parent_id` = 60, `sort` = 3 WHERE `id` = 62;
UPDATE `sys_menu` SET `menu_name` = '系统设置', `parent_id` = 60, `sort` = 4 WHERE `id` = 63;
UPDATE `sys_menu` SET `parent_id` = 60, `sort` = 5 WHERE `id` = 64;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、消息中心目录：通知公告
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `parent_id` = 70, `sort` = 1 WHERE `id` = 65;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、安全审计目录：审计、日志、告警、在线用户、归档策略
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 1 WHERE `id` = 48;
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 2 WHERE `id` = 49;
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 3 WHERE `id` = 47;
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 4 WHERE `id` = 50;
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 5 WHERE `id` = 51;
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 6 WHERE `id` = 66;
UPDATE `sys_menu` SET `parent_id` = 71, `sort` = 7 WHERE `id` = 52;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 六、运维管理目录：系统监控、缓存、任务、任务日志
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES (73, '系统监控', 72, 'C', 'monitor', 'monitor/server/index', 'monitor:server:list', 'desktop', 1, 1, 1, 0, '业务侧服务器 CPU、内存、磁盘、网络和 JVM 运行监控', 1, NOW())
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

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (511, '查询系统监控', 73, 'B', 'monitor:server:list', 1, 1, 1, '系统监控 — 查询服务器运行指标', 1, NOW()),
    (512, '触发内存回收', 73, 'B', 'monitor:server:gc', 2, 1, 1, '系统监控 — 手动触发 JVM GC', 1, NOW())
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `perms` = VALUES(`perms`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

UPDATE `sys_menu` SET `parent_id` = 72, `sort` = 2 WHERE `id` = 67;
UPDATE `sys_menu` SET `parent_id` = 72, `sort` = 3 WHERE `id` = 68;
UPDATE `sys_menu` SET `parent_id` = 72, `sort` = 4 WHERE `id` = 69;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 七、超级管理员补充授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 4, 0),
    (1, 60, 0),
    (1, 70, 0),
    (1, 71, 0),
    (1, 72, 0),
    (1, 73, 0),
    (1, 511, 0),
    (1, 512, 0);
