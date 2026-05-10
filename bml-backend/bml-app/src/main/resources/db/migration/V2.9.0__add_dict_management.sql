-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.9.0
-- 说明: 新增业务系统“系统配置 / 字典管理”菜单、按钮权限与基础字典数据
--
-- 设计说明：
--   1. 新增“系统配置”业务目录 id=60，挂在业务系统根目录 id=4 下，用于承载字典、参数、系统设置等配置类模块。
--   2. 字典管理使用 id=61，按钮权限使用 6101-6105，字段权限使用 6111-6118。
--   3. 不复用历史已删除菜单管理 id=43/431-434。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、系统配置目录与字典管理菜单
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES (60, '系统配置', 4, 'M', 'system-config', NULL, NULL, 'settings', 13, 1, 1, 0, '业务系统配置类模块目录', 1, NOW())
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `path` = VALUES(`path`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES (61, '字典管理', 60, 'C', 'dict', 'system/dict/index', 'system:dict:list', 'book', 1, 1, 1, 0, '维护系统基础枚举、状态选项与业务下拉数据', 1, NOW())
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
-- 二、字典管理按钮权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6101, '查询字典', 61, 'B', 'system:dict:query', 1, 1, 1, '字典管理 — 查询字典详情', 1, NOW()),
    (6102, '新增字典', 61, 'B', 'system:dict:add', 2, 1, 1, '字典管理 — 新增字典类型或字典数据', 1, NOW()),
    (6103, '编辑字典', 61, 'B', 'system:dict:edit', 3, 1, 1, '字典管理 — 编辑字典类型或字典数据', 1, NOW()),
    (6104, '删除字典', 61, 'B', 'system:dict:remove', 4, 1, 1, '字典管理 — 删除字典类型或字典数据', 1, NOW()),
    (6105, '刷新字典', 61, 'B', 'system:dict:refresh', 5, 1, 1, '字典管理 — 刷新前端字典缓存', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、字典管理字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6111, '字典名称', 61, 'F', 'system:dict:field:dictName', 11, 1, 1, '字典管理 — 字典名称字段', 1, NOW()),
    (6112, '字典编码', 61, 'F', 'system:dict:field:dictType', 12, 1, 1, '字典管理 — 字典编码字段', 1, NOW()),
    (6113, '字典标签', 61, 'F', 'system:dict:field:dictLabel', 13, 1, 1, '字典管理 — 字典标签字段', 1, NOW()),
    (6114, '字典键值', 61, 'F', 'system:dict:field:dictValue', 14, 1, 1, '字典管理 — 字典键值字段', 1, NOW()),
    (6115, '样式类名', 61, 'F', 'system:dict:field:cssClass', 15, 1, 1, '字典管理 — 标签样式字段', 1, NOW()),
    (6116, '显示排序', 61, 'F', 'system:dict:field:sort', 16, 1, 1, '字典管理 — 显示排序字段', 1, NOW()),
    (6117, '启用状态', 61, 'F', 'system:dict:field:status', 17, 1, 1, '字典管理 — 启用状态字段', 1, NOW()),
    (6118, '备注信息', 61, 'F', 'system:dict:field:remark', 18, 1, 1, '字典管理 — 备注信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、基础字典数据
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (1201, '系统状态', 'sys_common_status', 1, '通用启用/停用状态', 1, NOW()),
    (1202, '用户性别', 'sys_user_gender', 1, '用户基础性别枚举', 1, NOW()),
    (1203, '菜单类型', 'sys_menu_type', 1, '菜单目录、菜单、按钮、字段类型', 1, NOW())
ON DUPLICATE KEY UPDATE
    `dict_name` = VALUES(`dict_name`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

INSERT IGNORE INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (120101, 'sys_common_status', '正常', '1', 'green', 1, 1, '通用正常状态', 1, NOW()),
    (120102, 'sys_common_status', '停用', '0', 'orange', 2, 1, '通用停用状态', 1, NOW()),
    (120201, 'sys_user_gender', '未知', '0', 'gray', 1, 1, '未知性别', 1, NOW()),
    (120202, 'sys_user_gender', '男', '1', 'arcoblue', 2, 1, '男性', 1, NOW()),
    (120203, 'sys_user_gender', '女', '2', 'magenta', 3, 1, '女性', 1, NOW()),
    (120301, 'sys_menu_type', '目录', 'M', 'arcoblue', 1, 1, '菜单目录', 1, NOW()),
    (120302, 'sys_menu_type', '菜单', 'C', 'green', 2, 1, '业务菜单', 1, NOW()),
    (120303, 'sys_menu_type', '按钮', 'B', 'orange', 3, 1, '按钮权限', 1, NOW()),
    (120304, 'sys_menu_type', '字段', 'F', 'purple', 4, 1, '字段权限', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、超级管理员授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 60, 0), (1, 61, 0),
    (1, 6101, 0), (1, 6102, 0), (1, 6103, 0), (1, 6104, 0), (1, 6105, 0),
    (1, 6111, 0), (1, 6112, 0), (1, 6113, 0), (1, 6114, 0),
    (1, 6115, 0), (1, 6116, 0), (1, 6117, 0), (1, 6118, 0);
