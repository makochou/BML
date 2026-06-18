-- =============================================================================
-- BML 企业中台管理系统 — 强制修复所有菜单名称与顺序
-- 版本: V1.6.0
-- 说明: 强制同步所有菜单名称规则和左侧菜单位置
-- =============================================================================

-- ==========================================================
-- 第一级菜单：除了“工作台”，其他全部为四个字
-- 位置排序：
-- 1. 工作台
-- 2. 授权管理 (前端静态注入，后端不需要排)
-- 3. 主机监控 (原服务器监控)
-- 4. 告警中心
-- 5. 资产目录 (原资源目录/全源资产目录)
-- 6. 授权治理 (原治理中心)
-- 7. 系统管理
-- ==========================================================

-- 1. 工作台 (保持3个字)
UPDATE sys_menu SET menu_name = '工作台', sort = 1 WHERE id = 1;

-- 2. 主机监控 (排在 授权管理 之后，即在动态排序中排第1个，但因为工作台是1，所以主机监控为 2)
UPDATE sys_menu SET menu_name = '主机监控', sort = 2 WHERE id = 51;

-- 3. 告警中心 (排位置4，在后端中 sort = 3，因为 授权管理 不占后端 sort)
-- 按照截图：1 工作台(sort 1) -> 2 授权管理(static) -> 3 主机监控(sort 2) -> 4 告警中心(sort 3)
UPDATE sys_menu SET menu_name = '告警中心', sort = 3 WHERE id = 52;

-- 4. 资产目录 (排位置5，后端 sort = 4)
UPDATE sys_menu SET menu_name = '资产目录', sort = 4 WHERE id = 2;

-- 5. 授权治理 (排位置6，后端 sort = 5)
UPDATE sys_menu SET menu_name = '授权治理', sort = 5 WHERE id = 3;

-- 6. 系统管理 (排最后，后端 sort = 6)
UPDATE sys_menu SET menu_name = '系统管理', sort = 6 WHERE id = 4;
UPDATE sys_menu SET menu_name = '系统监控', sort = 7 WHERE id = 5;


-- ==========================================================
-- 第二级子菜单 & 按钮：全部四个字
-- ==========================================================

-- 系统管理子菜单
UPDATE sys_menu SET menu_name = '用户管理' WHERE id = 41;
UPDATE sys_menu SET menu_name = '角色管理' WHERE id = 42;
UPDATE sys_menu SET menu_name = '菜单管理' WHERE id = 43;
UPDATE sys_menu SET menu_name = '部门管理' WHERE id = 44;

-- 二级按钮名称也强制更新为4个字
UPDATE sys_menu SET menu_name = '用户查询' WHERE id = 411;
UPDATE sys_menu SET menu_name = '用户新增' WHERE id = 412;
UPDATE sys_menu SET menu_name = '用户编辑' WHERE id = 413;
UPDATE sys_menu SET menu_name = '用户删除' WHERE id = 414;
UPDATE sys_menu SET menu_name = '用户导出' WHERE id = 415;

UPDATE sys_menu SET menu_name = '角色查询' WHERE id = 421;
UPDATE sys_menu SET menu_name = '角色新增' WHERE id = 422;
UPDATE sys_menu SET menu_name = '角色编辑' WHERE id = 423;
UPDATE sys_menu SET menu_name = '角色删除' WHERE id = 424;

UPDATE sys_menu SET menu_name = '菜单查询' WHERE id = 431;
UPDATE sys_menu SET menu_name = '菜单新增' WHERE id = 432;
UPDATE sys_menu SET menu_name = '菜单编辑' WHERE id = 433;
UPDATE sys_menu SET menu_name = '菜单删除' WHERE id = 434;

UPDATE sys_menu SET menu_name = '监控查询' WHERE id = 511;
UPDATE sys_menu SET menu_name = '内存回收' WHERE id = 512;
UPDATE sys_menu SET menu_name = '告警查询' WHERE id = 521;
UPDATE sys_menu SET menu_name = '告警处理' WHERE id = 522;
UPDATE sys_menu SET menu_name = '告警删除' WHERE id = 523;

UPDATE sys_menu SET menu_name = '凭证查询' WHERE id = 31;
UPDATE sys_menu SET menu_name = '凭证签发' WHERE id = 32;
UPDATE sys_menu SET menu_name = '凭证编辑' WHERE id = 33;
UPDATE sys_menu SET menu_name = '凭证注销' WHERE id = 34;
UPDATE sys_menu SET menu_name = '密钥重置' WHERE id = 35;
UPDATE sys_menu SET menu_name = '策略配置' WHERE id = 36;
UPDATE sys_menu SET menu_name = '资产全量' WHERE id = 37;
