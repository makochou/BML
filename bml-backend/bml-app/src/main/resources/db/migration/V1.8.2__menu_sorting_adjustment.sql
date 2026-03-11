/*
 * V1.8.2 强制菜单全局排序
 * -----------------------------------------------
 * 1. 确保「系统管理」排在第1位
 * 2. 强制「全源资产目录」排在第2位
 * 3. 强制「授信治理中心」排在第3位
 * 4. 将「系统监控」及其他菜单后移
 */

-- 1. 系统管理
UPDATE sys_menu SET sort = 1 WHERE id = 1 AND parent_id = 0;

-- 2. API 相关核心菜单
UPDATE sys_menu SET sort = 2 WHERE id = 236; -- 全源资产目录
UPDATE sys_menu SET sort = 3 WHERE id = 234; -- 授信治理中心

-- 3. 系统监控及其他
UPDATE sys_menu SET sort = 4 WHERE id = 2; -- 系统监控
UPDATE sys_menu SET sort = 5 WHERE parent_id = 0 AND id NOT IN (1, 2, 234, 236);
