-- ============================================================================
-- V1.2.0 — 中台管理员与数据库用户表解耦 + 初始化前台业务用户
-- ============================================================================
--
-- 架构说明：
--   - 中台管理平台（ip:port/admin）有且仅有一个管理员，配置在 application.yml，
--     不使用 sys_user 数据库表，两者完全解耦。
--   - 前台业务系统（ip:port/）使用 sys_user 表进行认证登录，
--     许可证 maxTotalUsers 配额限制的就是此表的用户数量。
--
-- 变更内容：
--   1. 删除 V1.0.0 中写入的 admin 数据库记录（id=1）及其角色绑定，
--      因为中台管理员不再关联 sys_user 表。
--   2. 创建默认的前台业务管理员账号（bml / bml123），绑定超级管理员角色。
--
-- 前台业务系统默认账号：
--   用户名：bml
--   密码：  bml123（BCrypt 加密存储）
--   角色：  超级管理员（role_id=1，拥有全部权限）
--
-- 注意：生产环境请在首次登录后立即修改密码！
-- ============================================================================

-- ── 1. 清理中台管理员在 sys_user 表中的历史数据 ──
-- 中台管理员已完全配置化（application.yml），不再使用数据库用户表
DELETE FROM sys_user_role WHERE user_id = 1;
DELETE FROM sys_user WHERE id = 1;

-- ── 2. 创建前台业务管理员账号 ──
-- 密码 bml123 的 BCrypt 值
INSERT INTO sys_user (id, username, password, nickname, real_name, status, create_time)
VALUES (2, 'bml', '$2a$10$vrxvIRb7UTD1xfnRF.vime8waiVRyHc/WsSLMCuBnPeACDzO7ctfK', '业务管理员', '业务管理员', 1, NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- 绑定超级管理员角色（role_id=1，拥有全部权限）
INSERT INTO sys_user_role (user_id, role_id)
VALUES (2, 1)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);
