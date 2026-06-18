-- ============================================================================
-- V2.4.0 — 重置业务系统默认管理员账号
-- ============================================================================
--
-- 变更说明：
--   清除 sys_user 表中所有历史用户及关联数据，
--   重新创建一个拥有超级管理员角色的默认业务用户。
--
-- 默认业务系统账号：
--   用户名：admin
--   密码：  manager（BCrypt 加密存储）
--   角色：  超级管理员（role_id=1，拥有全部权限）
--
-- 注意事项：
--   1. 此脚本会删除所有现有用户，仅保留一个默认管理员账号。
--   2. 生产环境请在首次登录后立即修改密码！
--   3. 中台管理平台（/admin）账号不受影响，仍由 application.yml 配置。
--   4. 此脚本为 Flyway 版本化迁移，仅执行一次，不可重复运行。
--
-- 执行时机：
--   系统启动时由 Flyway 自动检测并执行（Spring Boot 集成 Flyway 自动迁移）。
-- ============================================================================

-- ── 1. 清理所有现有用户关联数据 ──
-- 先删除用户-角色绑定，再删除用户记录，避免外键约束冲突
DELETE FROM sys_user_role;
DELETE FROM sys_user;

-- ── 2. 重置自增 ID（从 1 开始，保持主键整洁） ──
ALTER TABLE sys_user AUTO_INCREMENT = 1;

-- ── 3. 创建默认超级管理员账号 ──
-- 密码 "manager" 的 BCrypt 加密值（cost=10）
-- 生成方式：org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("manager")
INSERT INTO sys_user (id, username, password, nickname, real_name, status, create_time)
VALUES (
    1,
    'admin',
    '$2a$10$nL/PGqiEg.ntH.eG4Xf9n.uUl0/5Rhsp.f7MZPE0Euuum2AaD9MMK',
    '超级管理员',
    '超级管理员',
    1,
    NOW()
);

-- ── 4. 绑定超级管理员角色（role_id=1，拥有全部权限） ──
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1);
