# BML-Backend 数据库设计 V2.0

> **版本**: v2.0 | **项目**: bml-backend | **日期**: 2026-02-09

---

## 1. 数据库架构

### 1.1 分库策略

| 数据库 | 用途 | 字符集 |
|--------|------|--------|
| bml_system | 系统配置、权限、日志 | utf8mb4_unicode_ci |
| bml_business | 业务数据 | utf8mb4_unicode_ci |

### 1.2 分表策略

| 表 | 策略 | 说明 |
|----|------|------|
| sys_operation_log | 按月分表 | sys_operation_log_YYYYMM |
| sys_login_log | 按月分表 | sys_login_log_YYYYMM |

---

## 2. ER关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户权限模型                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    ┌──────────┐         ┌───────────────┐         ┌──────────┐             │
│    │ sys_org  │◀────────│   sys_dept    │◀────────│ sys_user │             │
│    │   组织   │  1:N    │      部门     │    N:1  │   用户   │             │
│    └──────────┘         └───────────────┘         └────┬─────┘             │
│         │                                               │                   │
│         │ 自关联(parent_id)                             │                   │
│         ▼                                               │                   │
│    ┌──────────┐                                   ┌─────▼──────┐           │
│    │ 下级组织 │                                   │sys_user_role│           │
│    └──────────┘                                   │ 用户角色关联 │           │
│                                                   └─────┬──────┘           │
│                                                         │                   │
│                                                   ┌─────▼──────┐           │
│                                                   │  sys_role  │           │
│                                                   │    角色    │           │
│                                                   └─────┬──────┘           │
│                                                         │                   │
│                              ┌──────────────────────────┼────────┐         │
│                              │                          │        │         │
│                        ┌─────▼──────┐            ┌──────▼─────┐  │         │
│                        │sys_role_menu│            │sys_data_rule│ │         │
│                        │ 角色菜单    │            │ 数据权限规则│ │         │
│                        └─────┬──────┘            └────────────┘  │         │
│                              │                                    │         │
│                        ┌─────▼──────┐            ┌────────────────▼───┐    │
│                        │  sys_menu  │            │sys_field_permission│    │
│                        │    菜单    │            │    字段权限        │    │
│                        └────────────┘            └────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              系统配置模型                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐      │
│    │sys_dict_type │◀────────│sys_dict_data │         │  sys_config  │      │
│    │   字典类型   │   1:N   │   字典数据   │         │  系统参数    │      │
│    └──────────────┘         └──────────────┘         └──────────────┘      │
│                                                                             │
│    ┌──────────────┐         ┌──────────────┐                               │
│    │ sys_license  │         │sys_access_rule│                               │
│    │  系统授权    │         │  访问规则    │                               │
│    └──────────────┘         └──────────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              API管理模型                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    ┌───────────────┐       ┌───────────────────┐       ┌────────────────┐  │
│    │sys_api_account│◀──────│sys_api_permission │       │sys_api_registry│  │
│    │   API账号     │  1:N  │    API权限        │       │   API注册表    │  │
│    └───────────────┘       └───────────────────┘       └────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              日志模型                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│    ┌───────────────────┐                    ┌───────────────┐              │
│    │ sys_operation_log │ (按月分表)          │ sys_login_log │ (按月分表)   │
│    │     操作日志      │                    │   登录日志    │              │
│    └───────────────────┘                    └───────────────┘              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 表结构详细设计

### 3.1 通用字段规范

所有业务表必须包含以下字段:

| 字段 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| id | BIGINT | 主键(雪花ID) | - |
| create_by | BIGINT | 创建人ID | NULL |
| create_time | DATETIME | 创建时间 | CURRENT_TIMESTAMP |
| update_by | BIGINT | 更新人ID | NULL |
| update_time | DATETIME | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | TINYINT | 逻辑删除 | 0 |
| version | INT | 乐观锁版本 | 1 |

### 3.2 核心表清单

| 序号 | 表名 | 说明 | 行数预估 |
|------|------|------|---------|
| 1 | sys_user | 用户表 | 1000+ |
| 2 | sys_role | 角色表 | 50+ |
| 3 | sys_menu | 菜单表 | 500+ |
| 4 | sys_org | 组织表 | 100+ |
| 5 | sys_dept | 部门表 | 500+ |
| 6 | sys_user_role | 用户角色关联 | 3000+ |
| 7 | sys_role_menu | 角色菜单关联 | 5000+ |
| 8 | sys_field_permission | 字段权限 | 2000+ |
| 9 | sys_data_rule | 数据权限规则 | 500+ |
| 10 | sys_dict_type | 字典类型 | 100+ |
| 11 | sys_dict_data | 字典数据 | 1000+ |
| 12 | sys_config | 系统参数 | 100+ |
| 13 | sys_access_rule | 访问规则(ABAC) | 50+ |
| 14 | sys_license | 系统授权 | 1 |
| 15 | sys_api_account | API账号 | 50+ |
| 16 | sys_api_permission | API权限 | 500+ |
| 17 | sys_api_registry | API注册表 | 500+ |
| 18 | sys_operation_log | 操作日志 | 100万+/月 |
| 19 | sys_login_log | 登录日志 | 10万+/月 |

---

## 4. 索引设计策略

### 4.1 索引命名规范

| 类型 | 前缀 | 示例 |
|------|------|------|
| 主键 | pk_ | pk_sys_user |
| 唯一索引 | uk_ | uk_username |
| 普通索引 | idx_ | idx_dept_id |
| 联合索引 | idx_ | idx_org_dept |

### 4.2 核心表索引设计

**sys_user 索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_username (username),
KEY idx_dept_id (dept_id),
KEY idx_org_id (org_id),
KEY idx_status (status),
KEY idx_create_time (create_time)
```

**sys_menu 索引**:
```sql
PRIMARY KEY (id),
UNIQUE KEY uk_menu_code (menu_code),
KEY idx_parent_id (parent_id),
KEY idx_menu_type (menu_type)
```

**sys_operation_log 索引**:
```sql
PRIMARY KEY (id),
KEY idx_operator_id (operator_id),
KEY idx_create_time (create_time),
KEY idx_module (module),
KEY idx_composite (operator_id, create_time)
```

---

## 5. 数据迁移策略

### 5.1 Flyway配置

```yaml
spring:
  flyway:
    enabled: true
    locations:
      - classpath:db/migration/system
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
```

### 5.2 脚本命名规范

```
V{版本号}__{描述}.sql

示例:
V1.0.0__init_tables.sql          # 初始化表结构
V1.0.1__add_field_permission.sql # 添加字段权限表
V1.1.0__add_api_account.sql      # 添加API账号表
```

### 5.3 多数据源迁移

```java
@Configuration
public class FlywayConfig {

    @Bean
    public Flyway systemFlyway(@Qualifier("systemDataSource") DataSource ds) {
        Flyway flyway = Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/system")
            .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    public Flyway businessFlyway(@Qualifier("businessDataSource") DataSource ds) {
        Flyway flyway = Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration/business")
            .load();
        flyway.migrate();
        return flyway;
    }
}
```
