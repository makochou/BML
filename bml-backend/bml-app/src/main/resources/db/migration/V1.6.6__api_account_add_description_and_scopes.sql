/*
 * V1.6.6 API 账号新增用途描述与授权范围字段
 * -----------------------------------------------
 * 1. description   — 账号用途描述，对标 AWS IAM / Azure APIM 标准，
 *                    区分同一系统下多个账号的业务用途（如"生产数据同步"、"沙箱调试"）。
 * 2. allowed_scopes — 授权范围标签，对标 OAuth2 scope 概念，
 *                    用于鉴权链路前置快速判断请求是否属于账号允许的业务范围。
 *                    逗号分隔存储，例如 "read,write,admin"。
 *
 * 两个字段均为可选字段，不影响已有数据。
 */

ALTER TABLE sys_api_account
    ADD COLUMN description VARCHAR(255) DEFAULT NULL COMMENT '账号用途描述，用于说明该账号的业务场景和使用目的' AFTER account_name,
    ADD COLUMN allowed_scopes VARCHAR(500) DEFAULT NULL COMMENT '授权范围标签，逗号分隔存储（例如 read,write,admin），用于鉴权链路粗粒度快速判断' AFTER sign_version;
