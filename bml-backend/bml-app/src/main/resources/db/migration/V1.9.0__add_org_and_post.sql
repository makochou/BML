-- ═══════════════════════════════════════════════════════════════════════════════
-- V1.9.0 组织与权限模块 — 行业标准字段增强 & 新增岗位表
-- ═══════════════════════════════════════════════════════════════════════════════
-- 说明：
--   V1.0.0 已创建 sys_org / sys_dept / sys_user / sys_role 等基础表。
--   本脚本在原有结构上做增量变更：
--     1) ALTER sys_org  — 补充企业工商信息、地址、数据隔离策略等字段
--     2) ALTER sys_dept — 补充部门类型、职能分类
--     3) CREATE sys_post — 新建岗位表（含岗位类别、岗位级别）
--     4) ALTER sys_user — 补充岗位ID、工号、入职日期
--     5) 初始化默认数据
-- ═══════════════════════════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────
-- 1. 增强 sys_org（机构表）— 补充企业级标准字段
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_org`
    ADD COLUMN `org_type`            TINYINT       NOT NULL DEFAULT 2    COMMENT '机构类型 (1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部)' AFTER `org_code`,
    ADD COLUMN `credit_code`         VARCHAR(18)            DEFAULT NULL COMMENT '统一社会信用代码（18位）' AFTER `org_type`,
    ADD COLUMN `legal_person`        VARCHAR(50)            DEFAULT NULL COMMENT '法定代表人' AFTER `credit_code`,
    ADD COLUMN `registered_capital`  DECIMAL(18,2)          DEFAULT NULL COMMENT '注册资本（万元）' AFTER `legal_person`,
    ADD COLUMN `establish_date`      DATE                   DEFAULT NULL COMMENT '成立日期' AFTER `registered_capital`,
    ADD COLUMN `email`               VARCHAR(100)           DEFAULT NULL COMMENT '邮箱' AFTER `phone`,
    ADD COLUMN `province`            VARCHAR(50)            DEFAULT NULL COMMENT '省份' AFTER `email`,
    ADD COLUMN `city`                VARCHAR(50)            DEFAULT NULL COMMENT '城市' AFTER `province`,
    ADD COLUMN `district`            VARCHAR(50)            DEFAULT NULL COMMENT '区县' AFTER `city`,
    ADD COLUMN `address`             VARCHAR(200)           DEFAULT NULL COMMENT '详细地址' AFTER `district`,
    ADD COLUMN `business_scope`      TEXT                   DEFAULT NULL COMMENT '经营范围' AFTER `address`,
    ADD COLUMN `remark`              VARCHAR(500)           DEFAULT NULL COMMENT '备注' AFTER `business_scope`,
    ADD COLUMN `data_isolation`      TINYINT       NOT NULL DEFAULT 0    COMMENT '数据隔离模式 (0:共享-上级可查看下级数据 1:隔离-各机构数据独立)' AFTER `remark`;

-- ──────────────────────────────────────────────────────────
-- 2. 增强 sys_dept（部门表）— 补充部门分类
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_dept`
    ADD COLUMN `dept_type`  TINYINT     NOT NULL DEFAULT 3  COMMENT '部门类型 (1:事业部 2:中心 3:部门 4:小组)' AFTER `dept_code`,
    ADD COLUMN `func_type`  VARCHAR(20)          DEFAULT NULL COMMENT '职能分类 (管理/研发/销售/财务/人事/行政/生产/采购/仓储)' AFTER `dept_type`;

-- ──────────────────────────────────────────────────────────
-- 3. 新建 sys_post（岗位表）
-- ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `sys_post` (
    `id`            BIGINT        NOT NULL                                     COMMENT '岗位ID（雪花算法）',
    `post_code`     VARCHAR(64)   NOT NULL                                     COMMENT '岗位编码（全局唯一）',
    `post_name`     VARCHAR(50)   NOT NULL                                     COMMENT '岗位名称',
    `org_id`        BIGINT                 DEFAULT NULL                        COMMENT '所属机构ID（NULL 表示全局岗位）',
    `post_category` VARCHAR(20)            DEFAULT NULL                        COMMENT '岗位类别 (管理类/技术类/行政类/财务类/销售类/生产类)',
    `post_level`    VARCHAR(20)            DEFAULT NULL                        COMMENT '岗位级别 (如 P1~P10 技术序列, M1~M5 管理序列)',
    `sort`          INT           NOT NULL DEFAULT 0                           COMMENT '显示顺序',
    `status`        TINYINT       NOT NULL DEFAULT 1                           COMMENT '状态 (1:正常 0:停用)',
    `remark`        VARCHAR(500)           DEFAULT NULL                        COMMENT '备注',
    `create_by`     BIGINT                 DEFAULT NULL                        COMMENT '创建人ID',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_by`     BIGINT                 DEFAULT NULL                        COMMENT '更新人ID',
    `update_time`   DATETIME               DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT       NOT NULL DEFAULT 0                           COMMENT '逻辑删除 (0:存在 1:删除)',
    `version`       INT           NOT NULL DEFAULT 1                           COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_code` (`post_code`),
    KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';

-- ──────────────────────────────────────────────────────────
-- 4. 增强 sys_user（用户表）— 补充岗位、工号、入职日期
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_user`
    ADD COLUMN `post_id`      BIGINT       DEFAULT NULL COMMENT '岗位ID' AFTER `dept_id`,
    ADD COLUMN `employee_no`  VARCHAR(30)  DEFAULT NULL COMMENT '工号' AFTER `post_id`,
    ADD COLUMN `entry_date`   DATE         DEFAULT NULL COMMENT '入职日期' AFTER `employee_no`;

-- ──────────────────────────────────────────────────────────
-- 5. 初始化默认数据：机构
-- ──────────────────────────────────────────────────────────
INSERT INTO `sys_org` (`id`, `parent_id`, `ancestors`, `org_name`, `org_code`, `org_type`, `sort`, `leader`, `status`, `data_isolation`, `create_by`, `create_time`) VALUES
(100, 0, '0', 'BML科技集团', 'GROUP_BML', 1, 1, '管理员', 1, 0, 1, NOW()),
(101, 100, '0,100', '深圳总公司', 'CO_SZ',  2, 1, NULL, 1, 0, 1, NOW()),
(102, 100, '0,100', '北京分公司', 'BR_BJ',  3, 2, NULL, 1, 0, 1, NOW());

-- ──────────────────────────────────────────────────────────
-- 6. 初始化默认数据：岗位
-- ──────────────────────────────────────────────────────────
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `post_category`, `post_level`, `sort`, `status`, `create_by`, `create_time`) VALUES
(1, 'CEO',  '董事长',       '管理类', 'M5', 1, 1, 1, NOW()),
(2, 'GM',   '总经理',       '管理类', 'M4', 2, 1, 1, NOW()),
(3, 'PM',   '项目经理',     '管理类', 'M2', 3, 1, 1, NOW()),
(4, 'SE',   '高级工程师',   '技术类', 'P7', 4, 1, 1, NOW()),
(5, 'DEV',  '开发工程师',   '技术类', 'P5', 5, 1, 1, NOW()),
(6, 'FIN',  '财务主管',     '财务类', 'M2', 6, 1, 1, NOW()),
(7, 'HR',   '人事专员',     '行政类', 'P4', 7, 1, 1, NOW()),
(8, 'SALE', '销售代表',     '销售类', 'P4', 8, 1, 1, NOW());
