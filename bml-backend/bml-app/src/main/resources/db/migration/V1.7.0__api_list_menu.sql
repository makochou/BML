/*
 * V1.7.0 API 接口列表菜单
 * ---------------------------------------
 * 1. 新增「API 接口列表」功能菜单，用于树形展示全量纳管 API（模块 > 业务资源 > 具体接口）
 * 2. 权限码：system:apiList:list
 * 3. 默认赋权给超级管理员（role_id=1）
 */

-- 菜单：API 接口列表（与 API 账号管理并列，一级菜单）
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status, visible)
SELECT 236, 'API接口列表', 'system:apiList:list', 0, 'C', 'api/list', 'api/ApiList', 'code', 4, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 236);

-- 超级管理员赋权（幂等）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 236
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 236);
