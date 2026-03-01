ALTER TABLE sys_api_account
    ADD COLUMN test_ip_whitelist TEXT DEFAULT NULL COMMENT '测试环境 IP 白名单，支持单个 IP 或 CIDR，使用英文逗号分隔' AFTER ip_whitelist,
    ADD COLUMN staging_ip_whitelist TEXT DEFAULT NULL COMMENT '预发环境 IP 白名单，支持单个 IP 或 CIDR，使用英文逗号分隔' AFTER test_ip_whitelist,
    ADD COLUMN production_ip_whitelist TEXT DEFAULT NULL COMMENT '生产环境 IP 白名单，支持单个 IP 或 CIDR，使用英文逗号分隔' AFTER staging_ip_whitelist;

UPDATE sys_api_account
SET test_ip_whitelist = ip_whitelist
WHERE access_environment = 'test'
  AND ip_whitelist IS NOT NULL
  AND test_ip_whitelist IS NULL;

UPDATE sys_api_account
SET staging_ip_whitelist = ip_whitelist
WHERE access_environment = 'staging'
  AND ip_whitelist IS NOT NULL
  AND staging_ip_whitelist IS NULL;

UPDATE sys_api_account
SET production_ip_whitelist = ip_whitelist
WHERE access_environment = 'production'
  AND ip_whitelist IS NOT NULL
  AND production_ip_whitelist IS NULL;

CREATE TABLE IF NOT EXISTS sys_api_callback_log (
    id BIGINT NOT NULL COMMENT '主键 ID',
    account_id BIGINT NOT NULL COMMENT 'API 账号 ID',
    account_name VARCHAR(100) NOT NULL COMMENT '账号名称快照',
    system_name VARCHAR(100) DEFAULT NULL COMMENT '业务系统名称快照',
    system_code VARCHAR(64) DEFAULT NULL COMMENT '业务系统编码快照',
    business_type VARCHAR(100) NOT NULL COMMENT '业务类型',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    callback_url VARCHAR(255) NOT NULL COMMENT '回调地址',
    http_method VARCHAR(10) NOT NULL DEFAULT 'POST' COMMENT 'HTTP 方法',
    request_headers TEXT DEFAULT NULL COMMENT '请求头 JSON',
    request_body LONGTEXT DEFAULT NULL COMMENT '请求体',
    response_status_code INT DEFAULT NULL COMMENT '响应状态码',
    response_body LONGTEXT DEFAULT NULL COMMENT '响应体',
    callback_status TINYINT NOT NULL DEFAULT 0 COMMENT '回调状态：0待执行 1重试中 2成功 3失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    last_callback_time DATETIME DEFAULT NULL COMMENT '最近一次回调时间',
    success_time DATETIME DEFAULT NULL COMMENT '成功时间',
    last_error_message VARCHAR(500) DEFAULT NULL COMMENT '最近一次错误信息',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    PRIMARY KEY (id),
    KEY idx_callback_account_id (account_id),
    KEY idx_callback_status_retry_time (callback_status, next_retry_time),
    KEY idx_callback_create_time (create_time)
) ENGINE=InnoDB COMMENT='API 回调日志表';
