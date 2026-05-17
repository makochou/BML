-- ═══════════════════════════════════════════════════════════════════════════════
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.22.0
-- 说明: 统一所有 F 类型字段权限的 remark 格式，确保包含标签页分组信息
--
-- 变更背景：
--   功能授权面板的"表单字段"面板升级为按标签页二级分组展示。
--   前端通过解析 remark 字段中的标签页名称来确定字段所属的标签页。
--   remark 格式约定：
--     "模块名 — 标签名 — 字段名字段"
--   早期版本（V2.3.0）插入的字段 remark 不符合此格式，需要统一更新。
--
-- 影响范围：
--   仅更新 remark 字段（展示用），不影响权限标识和功能逻辑。
-- ═══════════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、机构管理（parent_id=45）— 工商信息标签页字段
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `remark` = '机构管理 — 工商信息 — 信用代码字段' WHERE `id` = 455 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 工商信息 — 法定代表人字段' WHERE `id` = 456 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 工商信息 — 注册资本字段' WHERE `id` = 457 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 联系与地址 — 联系电话字段' WHERE `id` = 458 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 联系与地址 — 邮箱地址字段' WHERE `id` = 459 AND `menu_type` = 'F';
-- V2.5.0 中的字段（已有正确格式，但去除"标签页"后缀以统一）
UPDATE `sys_menu` SET `remark` = '机构管理 — 工商信息 — 成立日期字段' WHERE `id` = 5001 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 工商信息 — 经营范围字段' WHERE `id` = 5002 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 联系与地址 — 省份字段' WHERE `id` = 5003 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 联系与地址 — 城市字段' WHERE `id` = 5004 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 联系与地址 — 区县字段' WHERE `id` = 5005 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '机构管理 — 联系与地址 — 详细地址字段' WHERE `id` = 5006 AND `menu_type` = 'F';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、部门管理（parent_id=44）— 联系信息标签页字段
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `remark` = '部门管理 — 联系信息 — 负责人字段' WHERE `id` = 445 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '部门管理 — 联系信息 — 联系电话字段' WHERE `id` = 446 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '部门管理 — 联系信息 — 邮箱字段' WHERE `id` = 447 AND `menu_type` = 'F';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、岗位管理（parent_id=46）— 其他信息标签页字段
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `remark` = '岗位管理 — 基本信息 — 岗位类别字段' WHERE `id` = 465 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '岗位管理 — 基本信息 — 岗位级别字段' WHERE `id` = 466 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '岗位管理 — 其他信息 — 备注字段' WHERE `id` = 467 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '岗位管理 — 基本信息 — 所属机构字段' WHERE `id` = 5021 AND `menu_type` = 'F';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、用户管理（parent_id=41）— 标签页字段
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `remark` = '用户管理 — 账号信息 — 手机号字段' WHERE `id` = 416 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '用户管理 — 账号信息 — 邮箱字段' WHERE `id` = 417 AND `menu_type` = 'F';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、角色与权限（parent_id=42）— 标签页字段
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `remark` = '角色与权限 — 数据权限 — 数据范围字段' WHERE `id` = 425 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '角色与权限 — 基本信息 — 备注字段' WHERE `id` = 426 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '角色与权限 — 数据权限 — 自定义机构范围字段' WHERE `id` = 5041 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '角色与权限 — 数据权限 — 自定义部门范围字段' WHERE `id` = 5042 AND `menu_type` = 'F';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 六、菜单管理（parent_id=43）— 标签页字段
-- ═══════════════════════════════════════════════════════════════════════════════
UPDATE `sys_menu` SET `remark` = '菜单管理 — 基本信息 — 权限标识字段' WHERE `id` = 435 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '菜单管理 — 基本信息 — 组件路径字段' WHERE `id` = 436 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '菜单管理 — 基本信息 — 菜单图标字段' WHERE `id` = 437 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '菜单管理 — 基本信息 — 路由地址字段' WHERE `id` = 5051 AND `menu_type` = 'F';
UPDATE `sys_menu` SET `remark` = '菜单管理 — 基本信息 — 是否外链字段' WHERE `id` = 5052 AND `menu_type` = 'F';
