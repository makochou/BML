# 当前数据库设计

## 1. 权威来源

数据库结构以以下内容为准：

1. `bml-app/src/main/resources/db/migration/*.sql`
2. 实体类与 Mapper
3. 运行时空库验收脚本

## 2. 当前核心表

### 2.1 系统管理

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_dept`
- `sys_user_role`
- `sys_role_menu`
- `sys_alert`

### 2.2 OpenAPI

- `sys_api_account`
- `sys_api_registry`
- `sys_api_permission`

### 2.3 Flyway

- `flyway_schema_history`

## 3. 设计规则

- 业务表统一使用 `utf8mb4`
- 逻辑删除字段统一使用 `deleted`
- 时间字段统一使用 `create_time`、`update_time`
- 文档 SQL 只做说明，真实执行以 `bml-app` 迁移脚本为准

## 4. 当前收口

- 历史 `bml_api_*` 表已在后续迁移中清理
- 当前 API 模型以 `sys_api_*` 为唯一有效模型
