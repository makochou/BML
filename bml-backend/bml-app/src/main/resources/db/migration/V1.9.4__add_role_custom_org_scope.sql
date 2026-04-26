-- =============================================================================
-- BML 企业中台管理系统 — 数据库迁移脚本
-- 版本: V1.9.4
-- 说明: 新增角色自定义数据权限关联表 sys_role_org。
--
-- 背景：
--   角色数据权限支持多种模式（sys_role.data_scope 字段），其中 data_scope = 7
--   表示「自定义」模式，允许管理员手动指定该角色可访问的机构范围。
--   本脚本创建 sys_role_org 多对多关联表，用于存储自定义模式下角色与机构的
--   对应关系，供 DataScopeAspect 在生成 SQL 数据范围条件时查询使用。
--
-- 影响范围：
--   新增表 sys_role_org，不修改任何已有表结构或数据。
--
-- 幂等性：
--   使用 CREATE TABLE IF NOT EXISTS，重复执行不会产生副作用。
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_role_org (
    role_id BIGINT NOT NULL COMMENT '角色 ID',
    org_id  BIGINT NOT NULL COMMENT '机构 ID',
    PRIMARY KEY (role_id, org_id),
    KEY idx_org_id (org_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='角色自定义数据权限 — 角色机构关联表';
