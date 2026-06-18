-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.8.0
-- 说明: 新增业务系统“菜单管理”菜单、按钮权限与字段权限
--
-- 设计说明：
--   1. 业务系统前端路由在 bml-frontend/src/router/index.ts 中维护，sys_menu 负责角色授权与后端鉴权。
--   2. 历史菜单管理 id=43 及按钮 431-434 不再复用，本次使用 id=53 与 5300+ 权限 ID 段。
--   3. 菜单管理挂载到业务系统根目录 id=4（组织与权限）下，保证角色授权面板可分配。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、菜单管理页面菜单
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES (53, '菜单管理', 4, 'C', 'menu', 'system/menu/index', 'system:menu:list', 'list', 6, 1, 1, 0, '业务系统菜单、按钮、字段权限元数据维护入口', 1, NOW())
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

-- 对齐组织与权限分组的业务菜单排序。
UPDATE `sys_menu` SET `sort` = 1 WHERE `id` = 45;
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 44;
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 46;
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 41;
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 42;
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 53;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、菜单管理按钮权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (5301, '查询菜单', 53, 'B', 'system:menu:query', 1, 1, 1, '菜单管理 — 查询菜单详情', 1, NOW()),
    (5302, '新增菜单', 53, 'B', 'system:menu:add', 2, 1, 1, '菜单管理 — 新增目录、菜单、按钮或字段权限', 1, NOW()),
    (5303, '编辑菜单', 53, 'B', 'system:menu:edit', 3, 1, 1, '菜单管理 — 编辑目录、菜单、按钮或字段权限', 1, NOW()),
    (5304, '删除菜单', 53, 'B', 'system:menu:remove', 4, 1, 1, '菜单管理 — 删除目录、菜单、按钮或字段权限', 1, NOW()),
    (5305, '新增下级', 53, 'B', 'system:menu:addChild', 5, 1, 1, '菜单管理 — 在指定节点下新增子权限项', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、菜单管理字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (5311, '父级菜单', 53, 'F', 'system:menu:field:parentId', 11, 1, 1, '菜单管理 — 父级菜单字段', 1, NOW()),
    (5312, '菜单类型', 53, 'F', 'system:menu:field:menuType', 12, 1, 1, '菜单管理 — 菜单类型字段', 1, NOW()),
    (5313, '路由地址', 53, 'F', 'system:menu:field:path', 13, 1, 1, '菜单管理 — 路由地址字段', 1, NOW()),
    (5314, '组件路径', 53, 'F', 'system:menu:field:component', 14, 1, 1, '菜单管理 — 组件路径字段', 1, NOW()),
    (5315, '权限标识', 53, 'F', 'system:menu:field:perms', 15, 1, 1, '菜单管理 — 权限标识字段', 1, NOW()),
    (5316, '菜单图标', 53, 'F', 'system:menu:field:icon', 16, 1, 1, '菜单管理 — 菜单图标字段', 1, NOW()),
    (5317, '显示状态', 53, 'F', 'system:menu:field:visible', 17, 1, 1, '菜单管理 — 显示状态字段', 1, NOW()),
    (5318, '启用状态', 53, 'F', 'system:menu:field:status', 18, 1, 1, '菜单管理 — 启用状态字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、超级管理员授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 4, 0),
    (1, 53, 0),
    (1, 5301, 0), (1, 5302, 0), (1, 5303, 0), (1, 5304, 0), (1, 5305, 0),
    (1, 5311, 0), (1, 5312, 0), (1, 5313, 0), (1, 5314, 0),
    (1, 5315, 0), (1, 5316, 0), (1, 5317, 0), (1, 5318, 0);
