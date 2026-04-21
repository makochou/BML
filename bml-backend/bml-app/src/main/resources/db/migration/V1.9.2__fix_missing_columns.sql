-- ═══════════════════════════════════════════════════════════════════════════════
-- V1.9.2 补齐 V1.9.0 因历史原因未实际执行的 ALTER 列
-- ═══════════════════════════════════════════════════════════════════════════════
-- 说明：
--   V1.9.0 在首次部署后被修改（增加了 ALTER TABLE 语句），但 Flyway 不会重跑
--   已标记为成功的脚本。本脚本使用 ADD COLUMN IF NOT EXISTS 逐一补齐，
--   确保无论 V1.9.0 是否完整执行过，数据库结构都与实体类对齐。
-- ═══════════════════════════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────
-- 1. sys_org — 补齐企业级字段
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_org`
    ADD COLUMN IF NOT EXISTS `org_type`            TINYINT       NOT NULL DEFAULT 2    COMMENT '机构类型 (1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部)' AFTER `org_code`,
    ADD COLUMN IF NOT EXISTS `credit_code`         VARCHAR(18)            DEFAULT NULL COMMENT '统一社会信用代码（18位）',
    ADD COLUMN IF NOT EXISTS `legal_person`        VARCHAR(50)            DEFAULT NULL COMMENT '法定代表人',
    ADD COLUMN IF NOT EXISTS `registered_capital`  DECIMAL(18,2)          DEFAULT NULL COMMENT '注册资本（万元）',
    ADD COLUMN IF NOT EXISTS `establish_date`      DATE                   DEFAULT NULL COMMENT '成立日期',
    ADD COLUMN IF NOT EXISTS `email`               VARCHAR(100)           DEFAULT NULL COMMENT '邮箱',
    ADD COLUMN IF NOT EXISTS `province`            VARCHAR(50)            DEFAULT NULL COMMENT '省份',
    ADD COLUMN IF NOT EXISTS `city`                VARCHAR(50)            DEFAULT NULL COMMENT '城市',
    ADD COLUMN IF NOT EXISTS `district`            VARCHAR(50)            DEFAULT NULL COMMENT '区县',
    ADD COLUMN IF NOT EXISTS `address`             VARCHAR(200)           DEFAULT NULL COMMENT '详细地址',
    ADD COLUMN IF NOT EXISTS `business_scope`      TEXT                   DEFAULT NULL COMMENT '经营范围',
    ADD COLUMN IF NOT EXISTS `remark`              VARCHAR(500)           DEFAULT NULL COMMENT '备注',
    ADD COLUMN IF NOT EXISTS `data_isolation`      TINYINT       NOT NULL DEFAULT 0    COMMENT '数据隔离模式 (0:共享 1:完全隔离 2:汇总共享 3:同级互通 4:按模块隔离)';

-- ──────────────────────────────────────────────────────────
-- 2. sys_dept — 补齐部门分类字段
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_dept`
    ADD COLUMN IF NOT EXISTS `dept_type`  TINYINT     NOT NULL DEFAULT 3  COMMENT '部门类型 (1:事业部 2:中心 3:部门 4:小组)',
    ADD COLUMN IF NOT EXISTS `func_type`  VARCHAR(20)          DEFAULT NULL COMMENT '职能分类 (管理/研发/销售/财务/人事/行政/生产/采购/仓储)';

-- ──────────────────────────────────────────────────────────
-- 3. sys_user — 补齐岗位/工号/入职日期
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `post_id`      BIGINT       DEFAULT NULL COMMENT '岗位ID',
    ADD COLUMN IF NOT EXISTS `employee_no`  VARCHAR(30)  DEFAULT NULL COMMENT '工号',
    ADD COLUMN IF NOT EXISTS `entry_date`   DATE         DEFAULT NULL COMMENT '入职日期';

-- ──────────────────────────────────────────────────────────
-- 4. sys_post — 补齐可能缺失的字段
-- ──────────────────────────────────────────────────────────
ALTER TABLE `sys_post`
    ADD COLUMN IF NOT EXISTS `org_id`        BIGINT       DEFAULT NULL COMMENT '所属机构ID（NULL 表示全局岗位）',
    ADD COLUMN IF NOT EXISTS `post_category` VARCHAR(20)  DEFAULT NULL COMMENT '岗位类别 (管理类/技术类/行政类/财务类/销售类/生产类)',
    ADD COLUMN IF NOT EXISTS `post_level`    VARCHAR(20)  DEFAULT NULL COMMENT '岗位级别 (如 P1~P10 技术序列, M1~M5 管理序列)';

-- ──────────────────────────────────────────────────────────
-- 5. 补插初始化数据（忽略已存在的记录）
-- ──────────────────────────────────────────────────────────
INSERT IGNORE INTO `sys_org` (`id`, `parent_id`, `ancestors`, `org_name`, `org_code`, `org_type`, `sort`, `leader`, `status`, `data_isolation`, `create_by`, `create_time`) VALUES
(100, 0, '0', 'BML科技集团', 'GROUP_BML', 1, 1, '管理员', 1, 0, 1, NOW()),
(101, 100, '0,100', '深圳总公司', 'CO_SZ',  2, 1, NULL, 1, 0, 1, NOW()),
(102, 100, '0,100', '北京分公司', 'BR_BJ',  3, 2, NULL, 1, 0, 1, NOW());

INSERT IGNORE INTO `sys_post` (`id`, `post_code`, `post_name`, `post_category`, `post_level`, `sort`, `status`, `create_by`, `create_time`) VALUES
(1, 'CEO',  '董事长',       '管理类', 'M5', 1, 1, 1, NOW()),
(2, 'GM',   '总经理',       '管理类', 'M4', 2, 1, 1, NOW()),
(3, 'PM',   '项目经理',     '管理类', 'M2', 3, 1, 1, NOW()),
(4, 'SE',   '高级工程师',   '技术类', 'P7', 4, 1, 1, NOW()),
(5, 'DEV',  '开发工程师',   '技术类', 'P5', 5, 1, 1, NOW()),
(6, 'FIN',  '财务主管',     '财务类', 'M2', 6, 1, 1, NOW()),
(7, 'HR',   '人事专员',     '行政类', 'P4', 7, 1, 1, NOW()),
(8, 'SALE', '销售代表',     '销售类', 'P4', 8, 1, 1, NOW());
