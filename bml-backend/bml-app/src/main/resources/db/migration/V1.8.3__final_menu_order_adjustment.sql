/*
 * V1.8.3 最终菜单顺序对齐
 * -----------------------------------------------
 * 1. 工作台 (id=230) -> 1
 * 2. 全源资产目录 (id=236) -> 2
 * 3. 授权治理中心 (id=234) -> 3
 * 4. 系统管理 (id=1) -> 4
 * 5. 系统监控 (id=2) -> 5
 */

-- 1. 工作台
UPDATE sys_menu SET menu_name = '工作台', sort = 1 WHERE id = 230;

-- 2. API 相关核心资产与治理
UPDATE sys_menu SET menu_name = '全源资产目录', sort = 2 WHERE id = 236;
UPDATE sys_menu SET menu_name = '授权治理中心', sort = 3 WHERE id = 234;

-- 3. 系统基础管理组件
UPDATE sys_menu SET menu_name = '系统管理', sort = 4 WHERE id = 1 AND parent_id = 0;
UPDATE sys_menu SET sort = 5 WHERE id = 2; -- 系统监控

-- 4. 将其他顶层菜单顺延排在第 10 位以后，防止遮挡
UPDATE sys_menu SET sort = 10 WHERE parent_id = 0 AND id NOT IN (230, 236, 234, 1, 2);
