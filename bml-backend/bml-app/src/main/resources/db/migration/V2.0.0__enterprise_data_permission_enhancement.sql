-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.0.0
-- 说明: 企业管理系统数据权限增强，新增以下表结构：
--
--   1. sys_org_data_share      — 机构数据共享规则表
--   2. sys_role_dept           — 角色自定义数据权限-部门范围关联表
--   3. sys_user_data_scope     — 用户个人数据权限配置表
--
-- 同时增强现有表结构：
--   4. sys_role 新增 data_scope 枚举值 8（本人及所有下属员工）
--   5. sys_user 新增 superior_id 字段（直属上级用户ID）
--
-- 背景与设计目标：
--   • 支持多机构、多层级组织架构下的灵活数据共享配置
--   • 角色数据权限支持「本人及下属」维度，覆盖汇报链场景
--   • 用户可拥有独立的数据权限覆盖配置（优先级高于角色）
--   • 机构间可配置定向数据共享规则，支持按模块粒度控制
--
-- 影响范围：
--   • 新增 3 张表，修改 sys_user 表结构
--   • 不修改已有数据，向下兼容
--
-- 幂等性：
--   使用 CREATE TABLE IF NOT EXISTS，列使用条件判断避免重复添加。
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 1. 机构数据共享规则表（sys_org_data_share）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 说明：
--   当机构 data_isolation 设为「完全隔离」或其他模式时，仍然可以通过此表
--   配置定向数据共享规则，将本机构的数据有选择地开放给目标机构查看。
--
-- 使用场景：
--   • 子公司A 将财务报表共享给集团总部查看（只读）
--   • 分公司B 将客户资源共享给兄弟分公司C（按模块共享）
--   • 事业部D 临时将项目数据共享给外部审计机构（有截止时间）
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `sys_org_data_share` (
    `id`              BIGINT        NOT NULL                                     COMMENT '主键ID（雪花算法）',
    `source_org_id`   BIGINT        NOT NULL                                     COMMENT '源机构ID（数据所属机构）',
    `target_org_id`   BIGINT        NOT NULL                                     COMMENT '目标机构ID（被共享的机构）',
    `share_type`      TINYINT       NOT NULL DEFAULT 1                           COMMENT '共享类型 (1:全模块共享 2:指定模块共享)',
    `module_code`     VARCHAR(100)           DEFAULT NULL                        COMMENT '模块编码（share_type=2时有效，多个用逗号分隔，如 finance,inventory）',
    `permission`      TINYINT       NOT NULL DEFAULT 1                           COMMENT '权限级别 (1:只读 2:读写)',
    `status`          TINYINT       NOT NULL DEFAULT 1                           COMMENT '状态 (1:生效 0:停用)',
    `expire_time`     DATETIME               DEFAULT NULL                        COMMENT '过期时间（NULL表示永不过期）',
    `remark`          VARCHAR(500)           DEFAULT NULL                        COMMENT '备注',
    `create_by`       BIGINT                 DEFAULT NULL                        COMMENT '创建人ID',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_by`       BIGINT                 DEFAULT NULL                        COMMENT '更新人ID',
    `update_time`     DATETIME               DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0                           COMMENT '逻辑删除 (0:存在 1:删除)',
    PRIMARY KEY (`id`),
    KEY `idx_source_org` (`source_org_id`),
    KEY `idx_target_org` (`target_org_id`),
    UNIQUE KEY `uk_source_target` (`source_org_id`, `target_org_id`, `module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='机构数据共享规则表';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 2. 角色自定义数据权限-部门范围关联表（sys_role_dept）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 说明：
--   当 sys_role.data_scope = 7（自定义）时，除了可以指定可访问的机构范围
--   （sys_role_org），还可以精细到部门粒度。本表存储角色与可访问部门的关联。
--
-- 与 sys_role_org 的关系：
--   • sys_role_org 定义可访问的机构范围（粗粒度）
--   • sys_role_dept 定义可访问的部门范围（细粒度）
--   • 查询时取二者并集，即机构范围内所有数据 + 指定部门的数据
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
    `role_id`  BIGINT NOT NULL COMMENT '角色ID',
    `dept_id`  BIGINT NOT NULL COMMENT '部门ID',
    PRIMARY KEY (`role_id`, `dept_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='角色自定义数据权限 — 角色部门关联表';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 3. 用户个人数据权限配置表（sys_user_data_scope）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 说明：
--   允许对特定用户设置独立的数据权限，优先级高于角色的 data_scope。
--   适用场景：
--     • 某员工临时借调到其他部门，需要额外查看借调部门的数据
--     • 总经理助理需要跨部门查看汇总数据
--     • 外部审计人员需要临时查看特定范围数据
--
-- 优先级规则：
--   1. 若用户有 sys_user_data_scope 记录且 status=1，以此为准
--   2. 否则以用户所有角色中最宽的 data_scope 为准（取并集）
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `sys_user_data_scope` (
    `id`              BIGINT        NOT NULL                                     COMMENT '主键ID（雪花算法）',
    `user_id`         BIGINT        NOT NULL                                     COMMENT '用户ID',
    `data_scope`      TINYINT       NOT NULL DEFAULT 6                           COMMENT '数据范围 (1:全部 2:本机构及下级 3:仅本机构 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义 8:本人及下属)',
    `custom_org_ids`  TEXT                   DEFAULT NULL                        COMMENT '自定义可访问机构ID列表（逗号分隔，data_scope=7时生效）',
    `custom_dept_ids` TEXT                   DEFAULT NULL                        COMMENT '自定义可访问部门ID列表（逗号分隔，data_scope=7时生效）',
    `status`          TINYINT       NOT NULL DEFAULT 1                           COMMENT '状态 (1:生效 0:停用)',
    `expire_time`     DATETIME               DEFAULT NULL                        COMMENT '过期时间（NULL表示永不过期，用于临时授权场景）',
    `remark`          VARCHAR(500)           DEFAULT NULL                        COMMENT '备注（记录授权原因，如"临时借调至XX部门"）',
    `create_by`       BIGINT                 DEFAULT NULL                        COMMENT '创建人ID',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP           COMMENT '创建时间',
    `update_by`       BIGINT                 DEFAULT NULL                        COMMENT '更新人ID',
    `update_time`     DATETIME               DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       NOT NULL DEFAULT 0                           COMMENT '逻辑删除 (0:存在 1:删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户个人数据权限配置表';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 4. 增强 sys_user — 新增直属上级字段（支持汇报链查询）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 说明：
--   data_scope = 8（本人及所有下属员工）需要通过 superior_id 构建汇报链。
--   通过递归查询 superior_id 可得到某用户的所有下属员工 ID 列表。
-- ═══════════════════════════════════════════════════════════════════════════════
SET @column_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'superior_id'
);

SET @alter_sql = IF(@column_exists = 0,
    'ALTER TABLE `sys_user` ADD COLUMN `superior_id` BIGINT DEFAULT NULL COMMENT ''直属上级用户ID（构建汇报链，NULL表示无上级）'' AFTER `post_id`',
    'SELECT 1'
);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 superior_id 添加索引（如不存在）
SET @index_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND INDEX_NAME = 'idx_superior_id'
);

SET @index_sql = IF(@index_exists = 0,
    'ALTER TABLE `sys_user` ADD KEY `idx_superior_id` (`superior_id`)',
    'SELECT 1'
);

PREPARE stmt2 FROM @index_sql;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 5. 数据权限说明文档（注释形式保留在 SQL 中方便参考）
-- ═══════════════════════════════════════════════════════════════════════════════
-- 
-- ┌────────────────────────────────────────────────────────────────────────────┐
-- │ 数据权限类型（sys_role.data_scope / sys_user_data_scope.data_scope）        │
-- ├────────┬──────────────────────┬────────────────────────────────────────────┤
-- │ 编码   │ 名称                 │ 说明                                       │
-- ├────────┼──────────────────────┼────────────────────────────────────────────┤
-- │   1    │ 全部数据             │ 不受任何限制，可查看系统所有数据              │
-- │   2    │ 本机构及所有下级机构  │ 可查看本机构及所有下级机构数据（受隔离影响）  │
-- │   3    │ 仅本机构             │ 只能查看当前所属机构的数据                   │
-- │   4    │ 本部门及所有下级部门  │ 可查看本部门及所有下级部门数据               │
-- │   5    │ 仅本部门             │ 只能查看当前所属部门的数据                   │
-- │   6    │ 仅本人               │ 只能查看自己创建的数据                       │
-- │   7    │ 自定义               │ 管理员手动指定可访问的机构/部门范围           │
-- │   8    │ 本人及所有下属员工    │ 可查看自己及所有汇报链下属员工创建的数据      │
-- └────────┴──────────────────────┴────────────────────────────────────────────┘
--
-- 优先级规则：
--   1. 超级管理员（admin）：不受任何数据权限限制
--   2. 用户个人数据权限（sys_user_data_scope，status=1 且未过期）：优先级最高
--   3. 角色数据权限（sys_role.data_scope）：取所有角色的并集
--   4. 机构数据隔离（sys_org.data_isolation）：作为基础约束，隔离机构不可穿透
--   5. 机构数据共享（sys_org_data_share）：定向共享规则，在隔离基础上开口子
--
-- ═══════════════════════════════════════════════════════════════════════════════
