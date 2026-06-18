-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.5.0
-- 说明: 权限分配面板全面优化 — 菜单重命名/重排序/按钮名称对齐/字段补全
--
-- 变更背景：
--   角色权限分配弹窗需要与业务系统前端侧边栏完全对应：
--   1. 顶层目录名称对齐：系统管理 → 组织与权限（与侧边栏一致）
--   2. C 菜单排序对齐侧边栏显示顺序
--   3. B 按钮名称对齐前端页面实际按钮文案
--   4. 新增扩展按钮（新增子机构、新增子部门、新增子菜单等）
--   5. 补全所有表单标签页中的字段权限（工商信息、联系与地址等）
--
-- 侧边栏菜单顺序（目标顺序）：
--   组织与权限
--   ├── 机构管理 (sort=1)
--   ├── 部门管理 (sort=2)
--   ├── 岗位管理 (sort=3)
--   ├── 用户管理 (sort=4)
--   ├── 角色与权限 (sort=5)
--   └── 菜单管理 (sort=6)
--
-- ID 编码规则扩展（兼容 V2.3.0 规则）：
--   ┌──────────┬───────────────────────────────────────────────────┐
--   │ 范围     │ 说明                                               │
--   ├──────────┼───────────────────────────────────────────────────┤
--   │ 4x       │ C 菜单（挂在 id=4 下）                             │
--   │ 4x1-4x4  │ B 标准 CRUD 按钮                                   │
--   │ 4x5-4x9  │ F 字段权限（V2.3.0 已用）                          │
--   │ 4001-4099 │ B 扩展按钮（本次新增，跨菜单通用扩展区）             │
--   │ 5001-5099 │ F 扩展字段权限（本次新增，补全各表单标签页字段）      │
--   └──────────┴───────────────────────────────────────────────────┘
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、重命名顶层目录与菜单（对齐侧边栏）
-- ═══════════════════════════════════════════════════════════════════════════════

-- 1) M 目录重命名：系统管理 → 组织与权限
UPDATE `sys_menu` SET `menu_name` = '组织与权限' WHERE `id` = 4 AND `menu_type` = 'M';

-- 2) C 菜单重命名：角色管理 → 角色与权限
UPDATE `sys_menu` SET `menu_name` = '角色与权限' WHERE `id` = 42 AND `menu_type` = 'C';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、重排序 C 菜单（对齐侧边栏显示顺序）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 目标顺序：机构(1) → 部门(2) → 岗位(3) → 用户(4) → 角色(5) → 菜单(6)

UPDATE `sys_menu` SET `sort` = 1 WHERE `id` = 45; -- 机构管理
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 44; -- 部门管理
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 46; -- 岗位管理
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 41; -- 用户管理
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 42; -- 角色与权限
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 43; -- 菜单管理

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、重命名 B 按钮（对齐前端页面实际按钮文案）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 命名规则：[动作][资源名]，如 "新增机构"、"编辑部门"

-- 机构管理按钮
UPDATE `sys_menu` SET `menu_name` = '新增机构' WHERE `id` = 452;
UPDATE `sys_menu` SET `menu_name` = '编辑机构' WHERE `id` = 453;
UPDATE `sys_menu` SET `menu_name` = '删除机构' WHERE `id` = 454;

-- 部门管理按钮
UPDATE `sys_menu` SET `menu_name` = '新增部门' WHERE `id` = 442;
UPDATE `sys_menu` SET `menu_name` = '编辑部门' WHERE `id` = 443;
UPDATE `sys_menu` SET `menu_name` = '删除部门' WHERE `id` = 444;

-- 岗位管理按钮
UPDATE `sys_menu` SET `menu_name` = '新增岗位' WHERE `id` = 462;
UPDATE `sys_menu` SET `menu_name` = '编辑岗位' WHERE `id` = 463;
UPDATE `sys_menu` SET `menu_name` = '删除岗位' WHERE `id` = 464;

-- 用户管理按钮
UPDATE `sys_menu` SET `menu_name` = '新增用户' WHERE `id` = 412;
UPDATE `sys_menu` SET `menu_name` = '编辑用户' WHERE `id` = 413;
UPDATE `sys_menu` SET `menu_name` = '删除用户' WHERE `id` = 414;

-- 角色管理按钮
UPDATE `sys_menu` SET `menu_name` = '新增角色' WHERE `id` = 422;
UPDATE `sys_menu` SET `menu_name` = '编辑角色' WHERE `id` = 423;
UPDATE `sys_menu` SET `menu_name` = '删除角色' WHERE `id` = 424;

-- 菜单管理按钮
UPDATE `sys_menu` SET `menu_name` = '新增菜单' WHERE `id` = 432;
UPDATE `sys_menu` SET `menu_name` = '编辑菜单' WHERE `id` = 433;
UPDATE `sys_menu` SET `menu_name` = '删除菜单' WHERE `id` = 434;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、新增扩展按钮（B 类型，前端页面中存在但权限表中缺失的操作）
-- ═══════════════════════════════════════════════════════════════════════════════
-- ID 范围: 4001-4099，避免与已有 3 位数 ID 冲突

-- 机构管理扩展按钮
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (4001, '新增子机构', 45, 'B', 'system:org:addChild', 5, 1, 1, '机构管理 — 新增子机构按钮', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (4002, '数据共享配置', 45, 'B', 'system:org:share', 6, 1, 1, '机构管理 — 数据共享配置按钮', 1, NOW());

-- 部门管理扩展按钮
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (4003, '新增子部门', 44, 'B', 'system:dept:addChild', 5, 1, 1, '部门管理 — 新增子部门按钮', 1, NOW());

-- 角色与权限扩展按钮
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (4004, '权限分配', 42, 'B', 'system:role:assign', 5, 1, 1, '角色与权限 — 权限分配/授权按钮', 1, NOW());

-- 菜单管理扩展按钮
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (4005, '新增子菜单', 43, 'B', 'system:menu:addChild', 5, 1, 1, '菜单管理 — 新增子菜单按钮', 1, NOW());

-- 用户管理扩展按钮
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (4006, '个人数据权限', 41, 'B', 'system:user:dataScope', 6, 1, 1, '用户管理 — 个人数据权限配置按钮', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、补全字段权限（F 类型）— 覆盖各菜单表单中所有标签页的字段
-- ═══════════════════════════════════════════════════════════════════════════════
-- ID 范围: 5001-5099
-- 原则：每个菜单的表单中所有标签页的所有字段均应有对应的 F 类型权限项
--       以便管理员在权限分配面板中精确控制每个角色可见的字段范围

-- ─────────────────────────────────────────────────────────────────
-- 5.1 机构管理 — 补全字段
-- ─────────────────────────────────────────────────────────────────
-- 已有: 信用代码(455), 法定代表人(456), 注册资本(457), 联系电话(458), 邮箱地址(459)
-- 补全: 工商信息标签页剩余字段 + 联系与地址标签页剩余字段

-- 工商信息标签页
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5001, '成立日期', 45, 'F', 'system:org:field:establishDate', 15, 1, 1, '机构管理 — 工商信息标签页 — 成立日期字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5002, '经营范围', 45, 'F', 'system:org:field:businessScope', 16, 1, 1, '机构管理 — 工商信息标签页 — 经营范围字段', 1, NOW());

-- 联系与地址标签页
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5003, '省份', 45, 'F', 'system:org:field:province', 17, 1, 1, '机构管理 — 联系与地址标签页 — 省份字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5004, '城市', 45, 'F', 'system:org:field:city', 18, 1, 1, '机构管理 — 联系与地址标签页 — 城市字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5005, '区县', 45, 'F', 'system:org:field:district', 19, 1, 1, '机构管理 — 联系与地址标签页 — 区县字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5006, '详细地址', 45, 'F', 'system:org:field:address', 20, 1, 1, '机构管理 — 联系与地址标签页 — 详细地址字段', 1, NOW());

-- ─────────────────────────────────────────────────────────────────
-- 5.2 部门管理 — 补全字段
-- ─────────────────────────────────────────────────────────────────
-- 已有: 负责人(445), 联系电话(446), 邮箱地址(447)
-- 补全: 基本信息标签页部分敏感字段

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5011, '所属机构', 44, 'F', 'system:dept:field:orgId', 13, 1, 1, '部门管理 — 基本信息标签页 — 所属机构字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5012, '部门类型', 44, 'F', 'system:dept:field:deptType', 14, 1, 1, '部门管理 — 基本信息标签页 — 部门类型字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5013, '职能分类', 44, 'F', 'system:dept:field:funcType', 15, 1, 1, '部门管理 — 基本信息标签页 — 职能分类字段', 1, NOW());

-- ─────────────────────────────────────────────────────────────────
-- 5.3 岗位管理 — 补全字段
-- ─────────────────────────────────────────────────────────────────
-- 已有: 岗位类别(465), 岗位级别(466), 备注信息(467)
-- 补全: 所属机构

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5021, '所属机构', 46, 'F', 'system:post:field:orgId', 13, 1, 1, '岗位管理 — 基本信息标签页 — 所属机构字段', 1, NOW());

-- ─────────────────────────────────────────────────────────────────
-- 5.4 用户管理 — 补全字段
-- ─────────────────────────────────────────────────────────────────
-- 已有: 手机号码(416), 邮箱地址(417), 真实姓名(418)
-- 补全: 组织与岗位标签页全部字段 + 账号信息标签页部分字段

-- 账号信息标签页
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5031, '性别', 41, 'F', 'system:user:field:gender', 13, 1, 1, '用户管理 — 账号信息标签页 — 性别字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5032, '角色', 41, 'F', 'system:user:field:roleIds', 14, 1, 1, '用户管理 — 账号信息标签页 — 角色字段', 1, NOW());

-- 组织与岗位标签页
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5033, '所属机构', 41, 'F', 'system:user:field:orgId', 15, 1, 1, '用户管理 — 组织与岗位标签页 — 所属机构字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5034, '所属部门', 41, 'F', 'system:user:field:deptId', 16, 1, 1, '用户管理 — 组织与岗位标签页 — 所属部门字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5035, '岗位', 41, 'F', 'system:user:field:postId', 17, 1, 1, '用户管理 — 组织与岗位标签页 — 岗位字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5036, '工号', 41, 'F', 'system:user:field:employeeNo', 18, 1, 1, '用户管理 — 组织与岗位标签页 — 工号字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5037, '直属上级', 41, 'F', 'system:user:field:superiorId', 19, 1, 1, '用户管理 — 组织与岗位标签页 — 直属上级字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5038, '入职日期', 41, 'F', 'system:user:field:entryDate', 20, 1, 1, '用户管理 — 组织与岗位标签页 — 入职日期字段', 1, NOW());

-- ─────────────────────────────────────────────────────────────────
-- 5.5 角色与权限 — 补全字段
-- ─────────────────────────────────────────────────────────────────
-- 已有: 数据范围(425), 备注信息(426)
-- 补全: 数据权限标签页的自定义范围字段

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5041, '自定义机构范围', 42, 'F', 'system:role:field:customOrgIds', 12, 1, 1, '角色与权限 — 数据权限标签页 — 自定义机构范围', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5042, '自定义部门范围', 42, 'F', 'system:role:field:customDeptIds', 13, 1, 1, '角色与权限 — 数据权限标签页 — 自定义部门范围', 1, NOW());

-- ─────────────────────────────────────────────────────────────────
-- 5.6 菜单管理 — 补全字段
-- ─────────────────────────────────────────────────────────────────
-- 已有: 权限标识(435), 组件路径(436), 菜单图标(437)
-- 补全: 路由配置标签页的字段

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5051, '路由地址', 43, 'F', 'system:menu:field:path', 13, 1, 1, '菜单管理 — 路由配置标签页 — 路由地址字段', 1, NOW());

INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES (5052, '是否外链', 43, 'F', 'system:menu:field:isFrame', 14, 1, 1, '菜单管理 — 路由配置标签页 — 是否外链字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 六、超级管理员赋予所有新增按钮和字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
-- role_id=1 为超级管理员角色，half_check=0 表示完全勾选

-- 新增扩展按钮
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4001, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4002, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4003, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4004, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4005, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 4006, 0);

-- 新增扩展字段 — 机构管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5001, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5002, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5003, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5004, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5005, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5006, 0);

-- 新增扩展字段 — 部门管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5011, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5012, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5013, 0);

-- 新增扩展字段 — 岗位管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5021, 0);

-- 新增扩展字段 — 用户管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5031, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5032, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5033, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5034, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5035, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5036, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5037, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5038, 0);

-- 新增扩展字段 — 角色与权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5041, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5042, 0);

-- 新增扩展字段 — 菜单管理
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5051, 0);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1, 5052, 0);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 附录：完整菜单结构（截至 V2.5.0）
-- ═══════════════════════════════════════════════════════════════════════════════
--
-- 组织与权限 (id=4, M, path='system')
-- │
-- ├── 机构管理 (id=45, C, sort=1, system:org:list)
-- │   ├── [B] 机构查询     (451, system:org:query)
-- │   ├── [B] 新增机构     (452, system:org:add)
-- │   ├── [B] 编辑机构     (453, system:org:edit)
-- │   ├── [B] 删除机构     (454, system:org:remove)
-- │   ├── [B] 新增子机构   (4001, system:org:addChild)
-- │   ├── [B] 数据共享配置 (4002, system:org:share)
-- │   ├── [F] 信用代码     (455, system:org:field:creditCode)
-- │   ├── [F] 法定代表人   (456, system:org:field:legalPerson)
-- │   ├── [F] 注册资本     (457, system:org:field:registeredCapital)
-- │   ├── [F] 联系电话     (458, system:org:field:phone)
-- │   ├── [F] 邮箱地址     (459, system:org:field:email)
-- │   ├── [F] 成立日期     (5001, system:org:field:establishDate)
-- │   ├── [F] 经营范围     (5002, system:org:field:businessScope)
-- │   ├── [F] 省份         (5003, system:org:field:province)
-- │   ├── [F] 城市         (5004, system:org:field:city)
-- │   ├── [F] 区县         (5005, system:org:field:district)
-- │   └── [F] 详细地址     (5006, system:org:field:address)
-- │
-- ├── 部门管理 (id=44, C, sort=2, system:dept:list)
-- │   ├── [B] 部门查询     (441, system:dept:query)
-- │   ├── [B] 新增部门     (442, system:dept:add)
-- │   ├── [B] 编辑部门     (443, system:dept:edit)
-- │   ├── [B] 删除部门     (444, system:dept:remove)
-- │   ├── [B] 新增子部门   (4003, system:dept:addChild)
-- │   ├── [F] 负责人       (445, system:dept:field:leader)
-- │   ├── [F] 联系电话     (446, system:dept:field:phone)
-- │   ├── [F] 邮箱地址     (447, system:dept:field:email)
-- │   ├── [F] 所属机构     (5011, system:dept:field:orgId)
-- │   ├── [F] 部门类型     (5012, system:dept:field:deptType)
-- │   └── [F] 职能分类     (5013, system:dept:field:funcType)
-- │
-- ├── 岗位管理 (id=46, C, sort=3, system:post:list)
-- │   ├── [B] 岗位查询     (461, system:post:query)
-- │   ├── [B] 新增岗位     (462, system:post:add)
-- │   ├── [B] 编辑岗位     (463, system:post:edit)
-- │   ├── [B] 删除岗位     (464, system:post:remove)
-- │   ├── [F] 岗位类别     (465, system:post:field:postCategory)
-- │   ├── [F] 岗位级别     (466, system:post:field:postLevel)
-- │   ├── [F] 备注信息     (467, system:post:field:remark)
-- │   └── [F] 所属机构     (5021, system:post:field:orgId)
-- │
-- ├── 用户管理 (id=41, C, sort=4, system:user:list)
-- │   ├── [B] 用户查询     (411, system:user:query)
-- │   ├── [B] 新增用户     (412, system:user:add)
-- │   ├── [B] 编辑用户     (413, system:user:edit)
-- │   ├── [B] 删除用户     (414, system:user:remove)
-- │   ├── [B] 重置密码     (415, system:user:reset)
-- │   ├── [B] 个人数据权限 (4006, system:user:dataScope)
-- │   ├── [F] 手机号码     (416, system:user:field:phone)
-- │   ├── [F] 邮箱地址     (417, system:user:field:email)
-- │   ├── [F] 真实姓名     (418, system:user:field:realName)
-- │   ├── [F] 性别         (5031, system:user:field:gender)
-- │   ├── [F] 角色         (5032, system:user:field:roleIds)
-- │   ├── [F] 所属机构     (5033, system:user:field:orgId)
-- │   ├── [F] 所属部门     (5034, system:user:field:deptId)
-- │   ├── [F] 岗位         (5035, system:user:field:postId)
-- │   ├── [F] 工号         (5036, system:user:field:employeeNo)
-- │   ├── [F] 直属上级     (5037, system:user:field:superiorId)
-- │   └── [F] 入职日期     (5038, system:user:field:entryDate)
-- │
-- ├── 角色与权限 (id=42, C, sort=5, system:role:list)
-- │   ├── [B] 角色查询     (421, system:role:query)
-- │   ├── [B] 新增角色     (422, system:role:add)
-- │   ├── [B] 编辑角色     (423, system:role:edit)
-- │   ├── [B] 删除角色     (424, system:role:remove)
-- │   ├── [B] 权限分配     (4004, system:role:assign)
-- │   ├── [F] 数据范围     (425, system:role:field:dataScope)
-- │   ├── [F] 备注信息     (426, system:role:field:remark)
-- │   ├── [F] 自定义机构范围 (5041, system:role:field:customOrgIds)
-- │   └── [F] 自定义部门范围 (5042, system:role:field:customDeptIds)
-- │
-- └── 菜单管理 (id=43, C, sort=6, system:menu:list)
--     ├── [B] 菜单查询     (431, system:menu:query)
--     ├── [B] 新增菜单     (432, system:menu:add)
--     ├── [B] 编辑菜单     (433, system:menu:edit)
--     ├── [B] 删除菜单     (434, system:menu:remove)
--     ├── [B] 新增子菜单   (4005, system:menu:addChild)
--     ├── [F] 权限标识     (435, system:menu:field:perms)
--     ├── [F] 组件路径     (436, system:menu:field:component)
--     ├── [F] 菜单图标     (437, system:menu:field:icon)
--     ├── [F] 路由地址     (5051, system:menu:field:path)
--     └── [F] 是否外链     (5052, system:menu:field:isFrame)
--
-- ═══════════════════════════════════════════════════════════════════════════════
