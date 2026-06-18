-- =============================================================================
-- V1.7.0 — 登录页配置初始化
-- =============================================================================
-- 在 sys_config 表中插入登录页所需的默认配置项：
--   1. sys.login.captchaEnabled — 图形验证码开关（默认关闭）
--   2. sys.login.bgImage       — 登录页全屏背景图 URL（空=使用前端内置默认渐变）
--   3. sys.login.cardBgImage   — 登录框背景图 URL（空=使用前端内置默认）
--   4. sys.login.favicon       — 浏览器标签图标 URL（空=使用前端内置默认）
-- =============================================================================

INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, remark)
VALUES
    (1001, '验证码开关', 'sys.login.captchaEnabled', 'false', 1, '是否开启登录页图形验证码（true/false）'),
    (1002, '登录页背景图', 'sys.login.bgImage', '', 1, '登录页全屏背景图片 URL，为空时使用前端内置默认渐变背景'),
    (1003, '登录框背景图', 'sys.login.cardBgImage', '', 1, '登录框背景图片 URL，为空时使用前端内置默认'),
    (1004, '浏览器标签图标', 'sys.login.favicon', '', 1, '浏览器标签 favicon URL，为空时使用前端内置默认')
ON DUPLICATE KEY UPDATE config_name = VALUES(config_name);
