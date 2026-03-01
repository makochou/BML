/*
 * V1.6.0 API账号管理菜单重构
 * ---------------------------------------
 * 1. 新增“API账号管理”统一入口与按钮权限
 * 2. 隐藏旧的 API管理 / 在线调试 / 应用管理 菜单
 * 3. 为超级管理员补齐新菜单与按钮授权
 */

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status, visible)
SELECT 234, 'API账号管理', 'api:account:list', 0, 'C', 'api/account', 'api/ApiAccountManage', 'apps', 3, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 234);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2341, 'API账号查询', 'api:account:query', 234, 'B', 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2341);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2342, 'API账号新增', 'api:account:add', 234, 'B', 2, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2342);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2343, 'API账号编辑', 'api:account:edit', 234, 'B', 3, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2343);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2344, 'API账号删除', 'api:account:remove', 234, 'B', 4, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2344);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2345, '重置API密钥', 'api:account:reset', 234, 'B', 5, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2345);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2346, 'API账号授权', 'api:account:authorize', 234, 'B', 6, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2346);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status, visible)
SELECT 2347, '同步开放接口目录', 'api:account:sync', 234, 'B', 7, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2347);

UPDATE sys_menu
SET visible = 0,
    status = 0
WHERE id IN (231, 232, 233);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.id IN (234, 2341, 2342, 2343, 2344, 2345, 2346, 2347)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_menu rm
      WHERE rm.role_id = 1
        AND rm.menu_id = m.id
  );
