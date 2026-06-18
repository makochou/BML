-- =============================================================================
-- BML 企业中台管理系统 — 菜单名称重命名 + 排序调整
-- 版本: V1.3.0
-- 说明:
--   1. 所有一级菜单名称统一改为四字，风格简洁统一；
--   2. 「告警中心」移至最后（sort 最大），其余菜单顺序前移；
--   3. 系统管理子菜单名称同步四字化。
--
-- 菜单排序规划（sort 越小越靠前）：
--   sort=1  运营工台  (原: 工作台,       ID: 1)
--   sort=2  主机监控  (原: 服务器监控,   ID: 51)
--   sort=3  资产目录  (原: 全源资产目录, ID: 2)
--   sort=4  授权治理  (原: 授权治理中心, ID: 3)
--   sort=5  系统管理  (原: 系统管理,     ID: 4) — 目录，含子菜单
--   sort=6  告警中心  (原: 告警中心,     ID: 52) — 移至最后
-- =============================================================================

-- ── 一级菜单：重命名 + 重排序 ──

-- 工作台 → 运营工台
UPDATE sys_menu SET menu_name = '运营工台', sort = 1 WHERE id = 1;

-- 服务器监控 → 主机监控
UPDATE sys_menu SET menu_name = '主机监控', sort = 2 WHERE id = 51;

-- 全源资产目录 → 资产目录
UPDATE sys_menu SET menu_name = '资产目录', sort = 3 WHERE id = 2;

-- 授权治理中心 → 授权治理（sort 从 5 前移到 4）
UPDATE sys_menu SET menu_name = '授权治理', sort = 4 WHERE id = 3;

-- 系统管理（保持名称，sort 从 6 调整到 5）
UPDATE sys_menu SET sort = 5 WHERE id = 4;

-- 告警中心（保持名称，sort 从 4 移至最后 6）
UPDATE sys_menu SET sort = 6 WHERE id = 52;

-- ── 系统管理子菜单：四字化 ──

-- 用户管理（已四字，保持不变）
-- 角色管理（已四字，保持不变）
-- 菜单管理（已四字，保持不变）
-- 部门管理（已四字，保持不变）

-- ── 按钮权限名称四字化 ──

-- 服务器监控按钮
UPDATE sys_menu SET menu_name = '监控查询' WHERE id = 511;
UPDATE sys_menu SET menu_name = '内存回收' WHERE id = 512;

-- 告警中心按钮
UPDATE sys_menu SET menu_name = '告警查询' WHERE id = 521;
UPDATE sys_menu SET menu_name = '告警处理' WHERE id = 522;
UPDATE sys_menu SET menu_name = '告警删除' WHERE id = 523;
