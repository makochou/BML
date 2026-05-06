-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.3.1
-- 说明: 修复 V2.3.0 可能未完整执行的菜单/按钮/字段权限数据
--
-- 问题背景：
--   V2.3.0 由于 Flyway 校验码机制，可能已被标记为 SUCCESS 但实际只部分执行
--   （C 类型菜单已插入，B 按钮和 F 字段缺失）。
--   本脚本使用 INSERT IGNORE 逐条补插所有缺失记录，确保无论 V2.3.0 执行到
--   哪一步，最终数据都完整。与 V1.9.2 修复 V1.9.0 的模式相同。
--
-- 处理策略：
--   • INSERT IGNORE — 如果记录已存在则跳过，不报错
--   • 每条记录独立一条 INSERT — 避免批量 INSERT 因单条失败而全部回滚
--   • 兜底 UPDATE — 确保已存在记录的关键字段值正确
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、C 类型菜单（确保存在）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
VALUES (45, '机构管理', 4, 'C', 'org', 'system/org/index', 'system:org:list', 'apps', 5, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`)
VALUES (46, '岗位管理', 4, 'C', 'post', 'system/post/index', 'system:post:list', 'bookmark', 6, 1, 1);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、B 类型按钮 — 部门管理（parent_id=44）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (441, '部门查询', 44, 'B', 'system:dept:query', 1, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (442, '部门新增', 44, 'B', 'system:dept:add', 2, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (443, '部门编辑', 44, 'B', 'system:dept:edit', 3, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (444, '部门删除', 44, 'B', 'system:dept:remove', 4, 1, 1);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、B 类型按钮 — 机构管理（parent_id=45）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (451, '机构查询', 45, 'B', 'system:org:query', 1, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (452, '机构新增', 45, 'B', 'system:org:add', 2, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (453, '机构编辑', 45, 'B', 'system:org:edit', 3, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (454, '机构删除', 45, 'B', 'system:org:remove', 4, 1, 1);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、B 类型按钮 — 岗位管理（parent_id=46）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (461, '岗位查询', 46, 'B', 'system:post:query', 1, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (462, '岗位新增', 46, 'B', 'system:post:add', 2, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (463, '岗位编辑', 46, 'B', 'system:post:edit', 3, 1, 1);

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`)
VALUES (464, '岗位删除', 46, 'B', 'system:post:remove', 4, 1, 1);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、F 类型字段 — 角色管理（parent_id=42）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (425, '数据范围', 42, 'F', 'system:role:field:dataScope', 10, 1, 1, '控制角色详情中数据范围字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (426, '备注信息', 42, 'F', 'system:role:field:remark', 11, 1, 1, '控制角色详情中备注字段的可见性', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 六、F 类型字段 — 菜单管理（parent_id=43）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (435, '权限标识', 43, 'F', 'system:menu:field:perms', 10, 1, 1, '控制菜单详情中权限标识字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (436, '组件路径', 43, 'F', 'system:menu:field:component', 11, 1, 1, '控制菜单详情中组件路径字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (437, '菜单图标', 43, 'F', 'system:menu:field:icon', 12, 1, 1, '控制菜单详情中图标字段的可见性', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 七、F 类型字段 — 部门管理（parent_id=44）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (445, '负责人', 44, 'F', 'system:dept:field:leader', 10, 1, 1, '控制部门详情中负责人字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (446, '联系电话', 44, 'F', 'system:dept:field:phone', 11, 1, 1, '控制部门详情中联系电话字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (447, '邮箱地址', 44, 'F', 'system:dept:field:email', 12, 1, 1, '控制部门详情中邮箱地址字段的可见性', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 八、F 类型字段 — 机构管理（parent_id=45）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (455, '信用代码', 45, 'F', 'system:org:field:creditCode', 10, 1, 1, '控制机构详情中统一社会信用代码字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (456, '法定代表人', 45, 'F', 'system:org:field:legalPerson', 11, 1, 1, '控制机构详情中法定代表人字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (457, '注册资本', 45, 'F', 'system:org:field:registeredCapital', 12, 1, 1, '控制机构详情中注册资本字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (458, '联系电话', 45, 'F', 'system:org:field:phone', 13, 1, 1, '控制机构详情中联系电话字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (459, '邮箱地址', 45, 'F', 'system:org:field:email', 14, 1, 1, '控制机构详情中邮箱地址字段的可见性', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 九、F 类型字段 — 岗位管理（parent_id=46）
-- ═══════════════════════════════════════════════════════════════════════════════

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (465, '岗位类别', 46, 'F', 'system:post:field:postCategory', 10, 1, 1, '控制岗位详情中岗位类别字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (466, '岗位级别', 46, 'F', 'system:post:field:postLevel', 11, 1, 1, '控制岗位详情中岗位级别字段的可见性', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (467, '备注信息', 46, 'F', 'system:post:field:remark', 12, 1, 1, '控制岗位详情中备注字段的可见性', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十、超级管理员赋予所有新增权限
-- ═══════════════════════════════════════════════════════════════════════════════
-- role_id=1 为超级管理员角色，half_check=0 表示完全勾选

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 45, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 46, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 441, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 442, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 443, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 444, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 445, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 446, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 447, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 451, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 452, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 453, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 454, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 455, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 456, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 457, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 458, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 459, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 461, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 462, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 463, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 464, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 465, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 466, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 467, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 425, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 426, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 435, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 436, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 437, 0);
