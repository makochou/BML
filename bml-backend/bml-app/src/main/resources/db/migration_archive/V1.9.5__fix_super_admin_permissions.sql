-- =============================================================================
-- BML 企业中台管理系统 — 修复超级管理员权限
-- 版本: V1.9.5
-- 说明: 修复超级管理员（bml 用户）无权访问系统管理页面的问题。
--
-- 问题根因：
--   1. V1.0.0 初始化时，sys_role_menu 通过 SELECT 语句将所有菜单关联到超级管理员角色。
--      但后续 V1.1.x ~ V1.9.x 迁移脚本新增的菜单（如机构管理、部门管理、岗位管理等）
--      未同步插入 sys_role_menu，导致超级管理员缺少这些菜单的权限标识。
--   2. 后端 PermissionService 原本通过硬编码 userId=1 来绕过权限检查，
--      但 bml 用户的 id=2，导致绕过逻辑失效。
--      （此问题已在 Java 代码层面修复：改为通过 *:*:* 权限标识识别超级管理员）
--
-- 修复内容：
--   1. 确保超级管理员角色（role_id=1）关联了数据库中所有菜单。
--   2. 确保 bml 用户（id=2）绑定了超级管理员角色（role_id=1）。
--   3. 确保超级管理员角色的 role_key 为 'admin'（Java 代码依赖此字段识别超级管理员）。
--
-- 幂等性：
--   所有 INSERT 语句均使用 ON DUPLICATE KEY UPDATE，重复执行不会产生副作用。
-- =============================================================================

-- ── 1. 确保超级管理员角色的 role_key 为 'admin' ──
-- Java 代码通过 role_key='admin' 来识别超级管理员并赋予 *:*:* 全量权限
UPDATE sys_role
SET role_key = 'admin'
WHERE id = 1
  AND (role_key IS NULL OR role_key != 'admin');

-- ── 2. 将所有现有菜单关联到超级管理员角色 ──
-- 包含 V1.0.0 之后新增的所有菜单（机构管理、部门管理、岗位管理等）
-- ON DUPLICATE KEY UPDATE 确保幂等性
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id
FROM sys_menu
WHERE deleted = 0
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ── 3. 确保 bml 用户绑定了超级管理员角色 ──
-- 防止因数据异常导致 bml 用户失去超级管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT 2, 1
FROM DUAL
WHERE EXISTS (SELECT 1 FROM sys_user WHERE id = 2 AND deleted = 0)
  AND EXISTS (SELECT 1 FROM sys_role WHERE id = 1 AND deleted = 0)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- ── 4. 确保 bml 用户状态正常 ──
UPDATE sys_user
SET status = 1
WHERE id = 2
  AND status != 1;
