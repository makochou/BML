-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.10.0
-- 说明: 新增业务系统“参数配置”菜单、按钮权限与基础配置项
--
-- 设计说明：
--   1. 参数配置挂载到 V2.9.0 新增的“系统配置”目录 id=60 下。
--   2. 参数配置使用 id=62，按钮权限使用 6201-6204，字段权限使用 6211-6215。
--   3. 系统内置参数 config_type=1，不允许在标准 CRUD 页面删除。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、参数配置菜单
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES (62, '参数配置', 60, 'C', 'config', 'system/config/index', 'system:config:list', 'tool', 2, 1, 1, 0, '维护运行时参数、登录策略和业务开关', 1, NOW())
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
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、参数配置按钮权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6201, '查询参数', 62, 'B', 'system:config:query', 1, 1, 1, '参数配置 — 查询参数详情', 1, NOW()),
    (6202, '新增参数', 62, 'B', 'system:config:add', 2, 1, 1, '参数配置 — 新增参数配置', 1, NOW()),
    (6203, '编辑参数', 62, 'B', 'system:config:edit', 3, 1, 1, '参数配置 — 编辑参数配置', 1, NOW()),
    (6204, '删除参数', 62, 'B', 'system:config:remove', 4, 1, 1, '参数配置 — 删除非内置参数配置', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、参数配置字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6211, '参数名称', 62, 'F', 'system:config:field:configName', 11, 1, 1, '参数配置 — 参数名称字段', 1, NOW()),
    (6212, '参数键名', 62, 'F', 'system:config:field:configKey', 12, 1, 1, '参数配置 — 参数键名字段', 1, NOW()),
    (6213, '参数键值', 62, 'F', 'system:config:field:configValue', 13, 1, 1, '参数配置 — 参数键值字段', 1, NOW()),
    (6214, '参数类型', 62, 'F', 'system:config:field:configType', 14, 1, 1, '参数配置 — 参数类型字段', 1, NOW()),
    (6215, '备注信息', 62, 'F', 'system:config:field:remark', 15, 1, 1, '参数配置 — 备注信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、基础参数配置
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`)
VALUES
    (1001, '登录验证码开关', 'sys.login.captchaEnabled', 'true', 1, '控制登录页是否启用验证码', 1, NOW()),
    (1005, '空闲超时分钟数', 'sys.login.idleTimeout', '30', 1, '业务系统用户空闲超时分钟数', 1, NOW()),
    (1006, '登录页品牌标题', 'sys.login.brandTitle', 'BML 企业管理系统', 1, '登录页品牌主标题', 1, NOW()),
    (1007, '侧边栏 Logo', 'sys.sidebar.logo', '', 1, '业务系统侧边栏 Logo 图片地址', 1, NOW())
ON DUPLICATE KEY UPDATE
    `config_name` = VALUES(`config_name`),
    `config_type` = VALUES(`config_type`),
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、超级管理员授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 60, 0), (1, 62, 0),
    (1, 6201, 0), (1, 6202, 0), (1, 6203, 0), (1, 6204, 0),
    (1, 6211, 0), (1, 6212, 0), (1, 6213, 0), (1, 6214, 0), (1, 6215, 0);
