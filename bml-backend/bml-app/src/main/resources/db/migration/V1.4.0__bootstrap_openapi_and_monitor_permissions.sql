/*
 * V1.4.0 安全与可用性补齐
 * ---------------------------------------
 * 1. 补充监控与告警权限菜单，支持 @PreAuthorize 权限码收口
 * 2. 注册 OpenAPI 最小可用示例接口
 * 3. 为现有 API 账号补充最小授权记录
 */

-- ==========================
-- 1) 监控与告警权限菜单
-- ==========================

-- 告警中心菜单
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status)
SELECT 210, '告警中心', 'monitor:alert:list', 2, 'C', 'alert', 'monitor/alert/index', 'notification', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 210);

-- 告警按钮权限
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status)
SELECT 2101, '告警查询', 'monitor:alert:list', 210, 'B', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2101);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status)
SELECT 2102, '告警处理', 'monitor:alert:edit', 210, 'B', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2102);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status)
SELECT 2103, '告警删除', 'monitor:alert:remove', 210, 'B', 3, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2103);

-- 服务器监控菜单与 GC 按钮权限
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status)
SELECT 220, '服务器监控', 'monitor:server:list', 2, 'C', 'server', 'monitor/server/index', 'dashboard', 4, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 220);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status)
SELECT 2201, '服务器监控查询', 'monitor:server:list', 220, 'B', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2201);

INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, sort, status)
SELECT 2202, '服务器GC', 'monitor:server:gc', 220, 'B', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 2202);

-- 超级管理员补充关联（幂等）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.id IN (210, 2101, 2102, 2103, 220, 2201, 2202)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );

-- ==========================
-- 2) OpenAPI 最小示例注册
-- ==========================

INSERT INTO sys_api_registry (
    id, api_name, api_url, http_method, module_name, controller_name, method_name, description, status
)
SELECT 9001,
       'OpenAPI 连通性检测',
       '/open/api/ping',
       'GET',
       'api',
       'OpenApiDemoController',
       'ping',
       '用于验证 OpenAPI 签名链路是否可用',
       1
WHERE NOT EXISTS (SELECT 1 FROM sys_api_registry WHERE id = 9001);

-- ==========================
-- 3) 现有账号最小授权
-- ==========================

INSERT INTO sys_api_permission (account_id, api_id)
SELECT a.id, 9001
FROM sys_api_account a
WHERE a.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_api_permission p
      WHERE p.account_id = a.id AND p.api_id = 9001
  );
