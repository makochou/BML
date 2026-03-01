ALTER TABLE sys_api_account
    ADD COLUMN client_types VARCHAR(200) DEFAULT NULL COMMENT '调用客户端类型，使用英文代码并以逗号分隔' AFTER account_type;
