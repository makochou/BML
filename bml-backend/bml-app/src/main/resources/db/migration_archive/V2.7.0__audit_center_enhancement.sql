-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.7.0
-- 说明: 日志审计中心增强菜单、权限与归档策略默认配置
--
-- 设计说明：
--   1. 业务系统侧边栏由前端静态路由渲染，sys_menu 负责角色授权与后端权限校验。
--   2. 菜单 ID 避开历史已删除的菜单管理 id=43 以及按钮 431-434。
--   3. 操作日志保留 V2.6.0 的 id=47，本次新增审计总览、登录日志、异常日志、风险告警、归档策略菜单。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、审计中心菜单（均挂在系统管理/组织与权限根目录 id=4 下，便于角色授权面板分配）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
VALUES
    (48, '审计总览', 4, 'C', 'audit/overview', 'system/audit/overview/index', 'system:audit:list', 'dashboard', 7, 1, 1),
    (49, '登录日志', 4, 'C', 'loginlog', 'system/loginlog/index', 'system:loginlog:list', 'safe', 8, 1, 1),
    (50, '异常日志', 4, 'C', 'exceptionlog', 'system/exceptionlog/index', 'system:exceptionlog:list', 'bug', 10, 1, 1),
    (51, '风险告警', 4, 'C', 'security-alert', 'system/security-alert/index', 'system:securityalert:list', 'notification', 11, 1, 1),
    (52, '归档策略', 4, 'C', 'audit-setting', 'system/audit-setting/index', 'system:auditsetting:list', 'settings', 12, 1, 1);

UPDATE `sys_menu` SET `menu_name` = '审计总览', `parent_id` = 4, `menu_type` = 'C', `path` = 'audit/overview', `component` = 'system/audit/overview/index', `perms` = 'system:audit:list', `icon` = 'dashboard', `sort` = 7, `visible` = 1, `status` = 1 WHERE `id` = 48;
UPDATE `sys_menu` SET `menu_name` = '登录日志', `parent_id` = 4, `menu_type` = 'C', `path` = 'loginlog', `component` = 'system/loginlog/index', `perms` = 'system:loginlog:list', `icon` = 'safe', `sort` = 8, `visible` = 1, `status` = 1 WHERE `id` = 49;
UPDATE `sys_menu` SET `menu_name` = '操作日志', `parent_id` = 4, `menu_type` = 'C', `path` = 'operlog', `component` = 'system/operlog/index', `perms` = 'system:operlog:list', `icon` = 'history', `sort` = 9, `visible` = 1, `status` = 1 WHERE `id` = 47;
UPDATE `sys_menu` SET `menu_name` = '异常日志', `parent_id` = 4, `menu_type` = 'C', `path` = 'exceptionlog', `component` = 'system/exceptionlog/index', `perms` = 'system:exceptionlog:list', `icon` = 'bug', `sort` = 10, `visible` = 1, `status` = 1 WHERE `id` = 50;
UPDATE `sys_menu` SET `menu_name` = '风险告警', `parent_id` = 4, `menu_type` = 'C', `path` = 'security-alert', `component` = 'system/security-alert/index', `perms` = 'system:securityalert:list', `icon` = 'notification', `sort` = 11, `visible` = 1, `status` = 1 WHERE `id` = 51;
UPDATE `sys_menu` SET `menu_name` = '归档策略', `parent_id` = 4, `menu_type` = 'C', `path` = 'audit-setting', `component` = 'system/audit-setting/index', `perms` = 'system:auditsetting:list', `icon` = 'settings', `sort` = 12, `visible` = 1, `status` = 1 WHERE `id` = 52;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、按钮权限（ID 使用 4800+，避免与现有三位数菜单按钮冲突）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES
    (4801, '查看审计总览', 48, 'B', 'system:audit:query', 1, 1, 1),
    (4901, '查询登录日志', 49, 'B', 'system:loginlog:query', 1, 1, 1),
    (4902, '导出登录日志', 49, 'B', 'system:loginlog:export', 2, 1, 1),
    (4903, '删除登录日志', 49, 'B', 'system:loginlog:remove', 3, 1, 1),
    (4904, '清空登录日志', 49, 'B', 'system:loginlog:clean', 4, 1, 1),
    (4704, '导出操作日志', 47, 'B', 'system:operlog:export', 4, 1, 1),
    (5001, '查询异常日志', 50, 'B', 'system:exceptionlog:query', 1, 1, 1),
    (5002, '导出异常日志', 50, 'B', 'system:exceptionlog:export', 2, 1, 1),
    (5101, '查询风险告警', 51, 'B', 'system:securityalert:query', 1, 1, 1),
    (5102, '处理风险告警', 51, 'B', 'system:securityalert:edit', 2, 1, 1),
    (5201, '编辑归档策略', 52, 'B', 'system:auditsetting:edit', 1, 1, 1);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、审计归档策略默认配置
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`)
VALUES
    (1101, '审计日志在线保留天数', 'sys.audit.onlineRetentionDays', '180', 1, '审计日志在线查询保留天数，默认180天'),
    (1102, '审计日志自动归档开关', 'sys.audit.archiveEnabled', 'false', 1, '是否启用审计日志自动归档'),
    (1103, '审计日志归档存储位置', 'sys.audit.archiveStorage', 'local://data/audit-archive', 1, '审计日志冷归档存储位置'),
    (1104, '审计日志自动清理开关', 'sys.audit.autoCleanEnabled', 'false', 1, '是否启用超过保留周期后的自动清理')
ON DUPLICATE KEY UPDATE
    `config_name` = VALUES(`config_name`),
    `config_value` = VALUES(`config_value`),
    `config_type` = VALUES(`config_type`),
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、超级管理员授予审计中心权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 4, 0),
    (1, 48, 0), (1, 4801, 0),
    (1, 49, 0), (1, 4901, 0), (1, 4902, 0), (1, 4903, 0), (1, 4904, 0),
    (1, 47, 0), (1, 4704, 0),
    (1, 50, 0), (1, 5001, 0), (1, 5002, 0),
    (1, 51, 0), (1, 5101, 0), (1, 5102, 0),
    (1, 52, 0), (1, 5201, 0);
