-- =============================================================================
-- V1.8.0 — 新增业务系统空闲超时配置
-- =============================================================================
-- 在 sys_config 表中插入空闲超时时长配置项：
--   sys.login.idleTimeout — 业务系统用户空闲自动登出时长（分钟），0 表示不限制
-- =============================================================================

INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, remark)
VALUES
    (1005, '空闲超时时长', 'sys.login.idleTimeout', '30', 1, '业务系统用户空闲自动登出时长（分钟），0 表示不限制，默认 30 分钟')
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name);
