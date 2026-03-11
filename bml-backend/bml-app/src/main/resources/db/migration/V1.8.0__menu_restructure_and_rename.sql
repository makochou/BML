/*
 * V1.8.0 菜单结构重组与专业化命名优化
 * -----------------------------------------------
 * 1. 调序：将「全源资产目录」置于第二位，「授信治理中心」置于第三位
 * 2. 重命名：应用中台范、科技感的专业术语，提升产品调性
 */

-- 1. 更新「API接口列表」 -> 「全源资产目录」并调序到 2
UPDATE sys_menu 
SET menu_name = '全源资产目录',
    sort = 2,
    icon = 'layers'
WHERE id = 236;

-- 2. 更新「API账号管理」 -> 「授信治理中心」并调序到 3
UPDATE sys_menu 
SET menu_name = '授信治理中心',
    sort = 3,
    icon = 'safe'
WHERE id = 234;

-- 3. 更新「授信治理中心」下属功能按钮名称
UPDATE sys_menu SET menu_name = '凭证画像查询' WHERE id = 2341;
UPDATE sys_menu SET menu_name = '凭证签发新增' WHERE id = 2342;
UPDATE sys_menu SET menu_name = '凭证主体编辑' WHERE id = 2343;
UPDATE sys_menu SET menu_name = '凭借注销移除' WHERE id = 2344;
UPDATE sys_menu SET menu_name = '密钥滚动重置' WHERE id = 2345;
UPDATE sys_menu SET menu_name = '凭证策略配置' WHERE id = 2346;
UPDATE sys_menu SET menu_name = '资产全量发现' WHERE id = 2347;

-- 4. 优化其他相关菜单（可选，根据整体调性一致性）
UPDATE sys_menu SET menu_name = '效能仪表盘' WHERE id = 1 AND parent_id = 0;
