ALTER TABLE sys_api_account
    ADD COLUMN ip_whitelist TEXT DEFAULT NULL COMMENT 'IP 白名单，支持单个 IP 或 CIDR，使用英文逗号分隔' AFTER access_environment,
    ADD COLUMN sign_version VARCHAR(16) NOT NULL DEFAULT 'v1' COMMENT '签名算法版本' AFTER ip_whitelist,
    ADD COLUMN callback_url VARCHAR(255) DEFAULT NULL COMMENT '业务回调地址' AFTER sign_version;
