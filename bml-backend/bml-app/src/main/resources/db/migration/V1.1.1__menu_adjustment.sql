-- =============================================================================
-- BML 企业中台管理系统 — 菜单层级与顺序调整脚本
-- 版本: V1.1.0
-- 说明: 将「服务器监控」与「告警中心」提拔至一级菜单，并调整全局排序。
-- =============================================================================

-- 1. 提拔「服务器监控」 (ID: 51)
UPDATE sys_menu 
SET parent_id = 0, 
    sort = 2, 
    visible = 1
WHERE id = 51;

-- 2. 调整「全源资产目录」 (ID: 2) 到位置 3
UPDATE sys_menu 
SET sort = 3 
WHERE id = 2;

-- 3. 提拔「告警中心」 (ID: 52)
UPDATE sys_menu 
SET parent_id = 0, 
    sort = 4, 
    visible = 1
WHERE id = 52;

-- 4. 调整「授权治理中心」 (ID: 3) 到位置 5
UPDATE sys_menu 
SET sort = 5 
WHERE id = 3;

-- 5. 调整「系统管理」 (ID: 4) 到位置 6
UPDATE sys_menu 
SET sort = 6 
WHERE id = 4;

-- 6. 隐藏原「系统监控」目录 (ID: 5) 并移至末尾
UPDATE sys_menu 
SET sort = 7, 
    visible = 0 
WHERE id = 5;
