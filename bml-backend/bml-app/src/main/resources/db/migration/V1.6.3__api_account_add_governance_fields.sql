ALTER TABLE sys_api_account
    ADD COLUMN owner_name VARCHAR(50) DEFAULT NULL COMMENT '接入方负责人' AFTER client_types,
    ADD COLUMN owner_contact VARCHAR(100) DEFAULT NULL COMMENT '负责人联系方式' AFTER owner_name,
    ADD COLUMN system_name VARCHAR(100) DEFAULT NULL COMMENT '业务系统名称' AFTER owner_contact,
    ADD COLUMN system_code VARCHAR(64) DEFAULT NULL COMMENT '业务系统编码' AFTER system_name,
    ADD COLUMN access_environment VARCHAR(32) DEFAULT NULL COMMENT '接入环境代码：test/staging/production' AFTER system_code;
