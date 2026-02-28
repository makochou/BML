/*
 * V1.5.0 动态路由菜单初始化
 * ---------------------------------------
 * 1. 补齐前端动态路由可直接展示的核心菜单（工作台/API管理/在线调试/应用管理）
 * 2. 幂等写入角色菜单关系，默认赋予超级管理员
 */

-- 工作台
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status)
SELECT 230, '工作台', 'monitor:server:list', 0, 'C', 'dashboard', 'dashboard/Workplace', 'dashboard', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 230);

-- API 管理
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status)
SELECT 231, 'API管理', 'api:account:list', 0, 'C', 'api/list', 'api/ApiList', 'list', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 231);

-- 在线调试
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status)
SELECT 232, '在线调试', 'api:account:list', 0, 'C', 'api/debug', 'api/ApiDebug', 'bug', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 232);

-- 应用管理
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status)
SELECT 233, '应用管理', 'api:account:list', 0, 'C', 'app', 'app/AppList', 'apps', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 233);

-- 默认赋权给超级管理员（幂等）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.id IN (230, 231, 232, 233)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );
