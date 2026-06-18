-- =============================================================================
-- BML 企业中台管理系统 — 数据库初始化脚本
-- 版本: V1.0.0
-- 数据库: bml_system（MariaDB / MySQL 8.0+）
-- 字符集: utf8mb4 / utf8mb4_unicode_ci
-- 说明: 本脚本为全量初始化脚本，包含所有表结构与基础数据。
--       后续变更请新建 V1.x.x__xxx.sql 增量脚本，不要修改本文件。
-- =============================================================================

-- =============================================================================
-- Part 1: 组织与部门
-- =============================================================================

-- 组织表（支持多级树形结构，ancestors 存储祖级 ID 链）
CREATE TABLE IF NOT EXISTS sys_org (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID（雪花算法）',
    org_name        VARCHAR(100)    NOT NULL                                    COMMENT '组织名称',
    org_code        VARCHAR(50)     NOT NULL                                    COMMENT '组织编码（全局唯一）',
    parent_id       BIGINT          NOT NULL DEFAULT 0                          COMMENT '上级组织 ID，顶级为 0',
    ancestors       VARCHAR(500)    NOT NULL DEFAULT ''                         COMMENT '祖级 ID 列表，逗号分隔，用于快速查询子树',
    sort            INT             NOT NULL DEFAULT 0                          COMMENT '显示顺序，数字越小越靠前',
    leader          VARCHAR(50)              DEFAULT NULL                       COMMENT '负责人姓名',
    phone           VARCHAR(20)              DEFAULT NULL                       COMMENT '联系电话',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    version         INT             NOT NULL DEFAULT 1                          COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '组织表';

-- 部门表（归属于组织，支持多级树形结构）
CREATE TABLE IF NOT EXISTS sys_dept (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID（雪花算法）',
    dept_name       VARCHAR(100)    NOT NULL                                    COMMENT '部门名称',
    dept_code       VARCHAR(50)     NOT NULL                                    COMMENT '部门编码（全局唯一）',
    org_id          BIGINT          NOT NULL                                    COMMENT '所属组织 ID',
    parent_id       BIGINT          NOT NULL DEFAULT 0                          COMMENT '上级部门 ID，顶级为 0',
    ancestors       VARCHAR(500)    NOT NULL DEFAULT ''                         COMMENT '祖级 ID 列表，逗号分隔',
    sort            INT             NOT NULL DEFAULT 0                          COMMENT '显示顺序',
    leader          VARCHAR(50)              DEFAULT NULL                       COMMENT '负责人姓名',
    phone           VARCHAR(20)              DEFAULT NULL                       COMMENT '联系电话',
    email           VARCHAR(100)             DEFAULT NULL                       COMMENT '邮箱',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    version         INT             NOT NULL DEFAULT 1                          COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_org_id (org_id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '部门表';

-- =============================================================================
-- Part 2: 用户与角色
-- =============================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID（雪花算法）',
    username        VARCHAR(50)     NOT NULL                                    COMMENT '登录用户名（全局唯一）',
    password        VARCHAR(100)    NOT NULL                                    COMMENT '密码（BCrypt 加密存储）',
    real_name       VARCHAR(50)              DEFAULT NULL                       COMMENT '真实姓名',
    nickname        VARCHAR(50)              DEFAULT NULL                       COMMENT '昵称',
    email           VARCHAR(100)             DEFAULT NULL                       COMMENT '邮箱',
    phone           VARCHAR(20)              DEFAULT NULL                       COMMENT '手机号',
    avatar          VARCHAR(255)             DEFAULT NULL                       COMMENT '头像 URL',
    gender          TINYINT         NOT NULL DEFAULT 0                          COMMENT '性别：0 未知，1 男，2 女',
    org_id          BIGINT                   DEFAULT NULL                       COMMENT '所属组织 ID',
    dept_id         BIGINT                   DEFAULT NULL                       COMMENT '所属部门 ID',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用，2 锁定',
    login_ip        VARCHAR(50)              DEFAULT NULL                       COMMENT '最后登录 IP',
    login_date      DATETIME                 DEFAULT NULL                       COMMENT '最后登录时间',
    pwd_update_time DATETIME                 DEFAULT NULL                       COMMENT '密码最后修改时间',
    pwd_error_count INT             NOT NULL DEFAULT 0                          COMMENT '连续密码错误次数',
    lock_time       DATETIME                 DEFAULT NULL                       COMMENT '账号锁定时间',
    remark          VARCHAR(500)             DEFAULT NULL                       COMMENT '备注',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    version         INT             NOT NULL DEFAULT 1                          COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_org_id (org_id),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID（雪花算法）',
    role_name       VARCHAR(30)     NOT NULL                                    COMMENT '角色名称',
    role_key        VARCHAR(100)    NOT NULL                                    COMMENT '角色权限字符串（如 admin、common）',
    data_scope      TINYINT         NOT NULL DEFAULT 1                          COMMENT '数据范围：1 全部，2 本组织及下级，3 仅本组织，4 本部门及下级，5 仅本部门，6 仅本人，7 自定义',
    sort            INT             NOT NULL DEFAULT 0                          COMMENT '显示顺序',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用',
    remark          VARCHAR(500)             DEFAULT NULL                       COMMENT '备注',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    version         INT             NOT NULL DEFAULT 1                          COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表';

-- 用户角色关联表（多对多）
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT          NOT NULL                                    COMMENT '用户 ID',
    role_id         BIGINT          NOT NULL                                    COMMENT '角色 ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表';

-- =============================================================================
-- Part 3: 菜单与权限
-- =============================================================================

-- 菜单权限表（支持目录 M / 菜单 C / 按钮 B 三种类型）
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    menu_name       VARCHAR(50)     NOT NULL                                    COMMENT '菜单名称',
    parent_id       BIGINT          NOT NULL DEFAULT 0                          COMMENT '父菜单 ID，顶级为 0',
    menu_type       CHAR(1)         NOT NULL                                    COMMENT '菜单类型：M 目录，C 菜单，B 按钮',
    path            VARCHAR(200)             DEFAULT NULL                       COMMENT '路由路径（前端 Vue Router path）',
    component       VARCHAR(255)             DEFAULT NULL                       COMMENT '前端组件路径（相对 views/ 目录）',
    perms           VARCHAR(100)             DEFAULT NULL                       COMMENT '权限标识（如 system:user:list）',
    icon            VARCHAR(100)             DEFAULT NULL                       COMMENT '菜单图标（Arco Design 图标名）',
    sort            INT             NOT NULL DEFAULT 0                          COMMENT '显示顺序，数字越小越靠前',
    visible         TINYINT         NOT NULL DEFAULT 1                          COMMENT '是否显示：1 显示，0 隐藏',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用',
    is_frame        TINYINT         NOT NULL DEFAULT 0                          COMMENT '是否外链：1 是，0 否',
    remark          VARCHAR(500)             DEFAULT NULL                       COMMENT '备注',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜单权限表';

-- 角色菜单关联表（多对多）
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id         BIGINT          NOT NULL                                    COMMENT '角色 ID',
    menu_id         BIGINT          NOT NULL                                    COMMENT '菜单 ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色菜单关联表';

-- 字段权限表（控制特定角色对特定实体字段的可见性）
CREATE TABLE IF NOT EXISTS sys_field_permission (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    role_id         BIGINT          NOT NULL                                    COMMENT '角色 ID',
    entity_name     VARCHAR(100)    NOT NULL                                    COMMENT '实体类名（如 SysUser）',
    field_name      VARCHAR(100)    NOT NULL                                    COMMENT '字段名（如 phone）',
    visible         TINYINT         NOT NULL DEFAULT 1                          COMMENT '是否可见：1 可见，0 隐藏',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_entity_field (role_id, entity_name, field_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字段权限表';

-- 数据权限规则表（自定义数据范围规则）
CREATE TABLE IF NOT EXISTS sys_data_rule (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    rule_name       VARCHAR(100)    NOT NULL                                    COMMENT '规则名称',
    entity_name     VARCHAR(100)    NOT NULL                                    COMMENT '实体类名',
    column_name     VARCHAR(100)    NOT NULL                                    COMMENT '过滤字段名',
    condition_type  VARCHAR(20)     NOT NULL                                    COMMENT '条件类型：EQ 等于，LIKE 模糊，IN 包含',
    condition_value VARCHAR(500)    NOT NULL                                    COMMENT '条件值（支持 SpEL 表达式）',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据权限规则表';

-- =============================================================================
-- Part 4: 字典与系统配置
-- =============================================================================

-- 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    dict_name       VARCHAR(100)    NOT NULL                                    COMMENT '字典名称',
    dict_type       VARCHAR(100)    NOT NULL                                    COMMENT '字典类型（全局唯一，如 sys_user_sex）',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用',
    remark          VARCHAR(500)             DEFAULT NULL                       COMMENT '备注',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典类型表';

-- 字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    dict_type       VARCHAR(100)    NOT NULL                                    COMMENT '字典类型',
    dict_label      VARCHAR(100)    NOT NULL                                    COMMENT '字典标签（显示文本）',
    dict_value      VARCHAR(100)    NOT NULL                                    COMMENT '字典键值（存储值）',
    css_class       VARCHAR(100)             DEFAULT NULL                       COMMENT 'CSS 样式类名（用于前端标签颜色）',
    sort            INT             NOT NULL DEFAULT 0                          COMMENT '显示顺序',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 正常，0 停用',
    remark          VARCHAR(500)             DEFAULT NULL                       COMMENT '备注',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典数据表';

-- 系统配置表（键值对形式存储系统参数）
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    config_name     VARCHAR(100)    NOT NULL                                    COMMENT '配置名称',
    config_key      VARCHAR(100)    NOT NULL                                    COMMENT '配置键名（全局唯一）',
    config_value    VARCHAR(500)    NOT NULL                                    COMMENT '配置键值',
    config_type     TINYINT         NOT NULL DEFAULT 0                          COMMENT '是否系统内置：1 是，0 否（内置配置不允许删除）',
    remark          VARCHAR(500)             DEFAULT NULL                       COMMENT '备注',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置表';

-- =============================================================================
-- Part 5: 日志与监控
-- =============================================================================

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    title           VARCHAR(50)              DEFAULT NULL                       COMMENT '模块标题',
    business_type   INT             NOT NULL DEFAULT 0                          COMMENT '业务类型：0 其他，1 新增，2 修改，3 删除，4 查询',
    method          VARCHAR(200)             DEFAULT NULL                       COMMENT '方法名称',
    request_method  VARCHAR(10)              DEFAULT NULL                       COMMENT 'HTTP 请求方式',
    operator_type   INT             NOT NULL DEFAULT 0                          COMMENT '操作类别：0 其他，1 后台用户，2 手机端用户',
    oper_name       VARCHAR(50)              DEFAULT NULL                       COMMENT '操作人员',
    dept_name       VARCHAR(50)              DEFAULT NULL                       COMMENT '部门名称',
    oper_url        VARCHAR(255)             DEFAULT NULL                       COMMENT '请求 URL',
    oper_ip         VARCHAR(128)             DEFAULT NULL                       COMMENT '主机地址',
    oper_param      VARCHAR(2000)            DEFAULT NULL                       COMMENT '请求参数',
    json_result     VARCHAR(2000)            DEFAULT NULL                       COMMENT '返回参数',
    status          INT             NOT NULL DEFAULT 0                          COMMENT '操作状态：0 正常，1 异常',
    error_msg       VARCHAR(2000)            DEFAULT NULL                       COMMENT '错误消息',
    oper_time       DATETIME                 DEFAULT NULL                       COMMENT '操作时间',
    cost_time       BIGINT                   DEFAULT 0                          COMMENT '消耗时间（毫秒）',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作日志表';

-- 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID',
    username        VARCHAR(50)              DEFAULT NULL                       COMMENT '用户账号',
    ipaddr          VARCHAR(128)             DEFAULT NULL                       COMMENT '登录 IP 地址',
    login_location  VARCHAR(255)             DEFAULT NULL                       COMMENT '登录地点',
    browser         VARCHAR(50)              DEFAULT NULL                       COMMENT '浏览器类型',
    os              VARCHAR(50)              DEFAULT NULL                       COMMENT '操作系统',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '登录状态：1 成功，0 失败',
    msg             VARCHAR(255)             DEFAULT NULL                       COMMENT '提示消息',
    login_time      DATETIME                 DEFAULT NULL                       COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_login_time (login_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '登录日志表';

-- 系统告警表（服务器资源告警通知）
CREATE TABLE IF NOT EXISTS sys_alert (
    id              BIGINT          NOT NULL AUTO_INCREMENT                     COMMENT '主键 ID',
    alert_type      VARCHAR(50)     NOT NULL                                    COMMENT '告警类型：CPU_HIGH / MEMORY_HIGH / DISK_FULL / JVM_HIGH',
    alert_level     VARCHAR(20)     NOT NULL                                    COMMENT '告警级别：info / warning / error / critical',
    alert_title     VARCHAR(100)    NOT NULL                                    COMMENT '告警标题',
    alert_content   TEXT            NOT NULL                                    COMMENT '告警详情内容',
    read_status     TINYINT         NOT NULL DEFAULT 0                          COMMENT '阅读状态：0 未读，1 已读',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_alert_type (alert_type),
    KEY idx_read_status (read_status),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统告警表';

-- =============================================================================
-- Part 6: API 开放平台
-- =============================================================================

-- API 账号表（外部系统接入凭证）
CREATE TABLE IF NOT EXISTS sys_api_account (
    id                      BIGINT          NOT NULL                            COMMENT '主键 ID（雪花算法）',
    account_name            VARCHAR(100)    NOT NULL                            COMMENT '账号名称（全局唯一）',
    description             VARCHAR(255)             DEFAULT NULL               COMMENT '账号用途描述，说明业务场景（如：生产数据同步、沙箱调试）',
    access_key              VARCHAR(64)     NOT NULL                            COMMENT 'AccessKey（公钥，全局唯一）',
    secret_key              VARCHAR(255)    NOT NULL                            COMMENT 'SecretKey（AES 加密后存储，明文仅在创建/重置时返回一次）',
    account_type            TINYINT         NOT NULL DEFAULT 1                  COMMENT '账号类型：1 内部账号，2 外部账号',
    client_types            VARCHAR(200)             DEFAULT NULL               COMMENT '允许的客户端类型，英文逗号分隔（如 web,mobile,server）',
    owner_name              VARCHAR(50)              DEFAULT NULL               COMMENT '接入方负责人姓名',
    owner_contact           VARCHAR(100)             DEFAULT NULL               COMMENT '负责人联系方式（手机/邮箱）',
    system_name             VARCHAR(100)             DEFAULT NULL               COMMENT '接入业务系统名称',
    system_code             VARCHAR(64)              DEFAULT NULL               COMMENT '接入业务系统编码',
    access_environment      VARCHAR(32)              DEFAULT NULL               COMMENT '接入环境：test / staging / production',
    ip_whitelist            TEXT                     DEFAULT NULL               COMMENT 'IP 白名单（兼容字段，存储当前环境生效的白名单，逗号分隔，支持 CIDR）',
    test_ip_whitelist       TEXT                     DEFAULT NULL               COMMENT '测试环境 IP 白名单（逗号分隔，支持 CIDR）',
    staging_ip_whitelist    TEXT                     DEFAULT NULL               COMMENT '预发环境 IP 白名单（逗号分隔，支持 CIDR）',
    production_ip_whitelist TEXT                     DEFAULT NULL               COMMENT '生产环境 IP 白名单（逗号分隔，支持 CIDR）',
    sign_version            VARCHAR(16)     NOT NULL DEFAULT 'v1'               COMMENT '签名算法版本（当前支持 v1：HmacSHA256）',
    allowed_scopes          VARCHAR(500)             DEFAULT NULL               COMMENT '授权范围标签，逗号分隔（如 read,write,admin），对标 OAuth2 scope',
    callback_url            VARCHAR(255)             DEFAULT NULL               COMMENT '业务回调地址（接收业务事件推送）',
    rate_limit              INT             NOT NULL DEFAULT 1000               COMMENT '每分钟限流阈值（次/分钟）',
    expire_time             DATETIME                 DEFAULT NULL               COMMENT '账号过期时间，NULL 表示永不过期',
    status                  TINYINT         NOT NULL DEFAULT 1                  COMMENT '状态：1 启用，0 停用',
    remark                  VARCHAR(500)             DEFAULT NULL               COMMENT '备注',
    create_by               BIGINT                   DEFAULT NULL               COMMENT '创建人 ID',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    update_by               BIGINT                   DEFAULT NULL               COMMENT '更新人 ID',
    update_time             DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 TINYINT         NOT NULL DEFAULT 0                  COMMENT '逻辑删除：0 未删除，1 已删除',
    version                 INT             NOT NULL DEFAULT 1                  COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_name (account_name),
    UNIQUE KEY uk_access_key (access_key),
    KEY idx_status (status),
    KEY idx_access_environment (access_environment)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'API 账号表';

-- API 注册表（自动扫描并纳管的接口目录）
CREATE TABLE IF NOT EXISTS sys_api_registry (
    id              BIGINT          NOT NULL                                    COMMENT '主键 ID（雪花算法）',
    api_name        VARCHAR(200)             DEFAULT NULL                       COMMENT '接口名称（来自 @Operation(summary) 注解）',
    api_url         VARCHAR(255)    NOT NULL                                    COMMENT '接口路径（去除 context-path 前缀）',
    http_method     VARCHAR(10)     NOT NULL                                    COMMENT 'HTTP 方法：GET / POST / PUT / DELETE / PATCH 等',
    module_name     VARCHAR(100)             DEFAULT NULL                       COMMENT '所属模块名（来自包名，如 system / api）',
    controller_name VARCHAR(200)             DEFAULT NULL                       COMMENT '所属控制器类名（如 SysUserController）',
    method_name     VARCHAR(200)             DEFAULT NULL                       COMMENT '控制器方法名（如 listUsers）',
    description     VARCHAR(500)             DEFAULT NULL                       COMMENT '接口描述（来自 @Operation(description) 注解）',
    status          TINYINT         NOT NULL DEFAULT 1                          COMMENT '状态：1 启用（接口存在），0 停用（接口已删除）',
    create_by       BIGINT                   DEFAULT NULL                       COMMENT '创建人 ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP          COMMENT '创建时间',
    update_by       BIGINT                   DEFAULT NULL                       COMMENT '更新人 ID',
    update_time     DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0                          COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_api_url_method (api_url, http_method),
    KEY idx_module_name (module_name),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'API 注册表（接口目录）';

-- API 权限表（账号与接口的授权关系）
CREATE TABLE IF NOT EXISTS sys_api_permission (
    account_id      BIGINT          NOT NULL                                    COMMENT 'API 账号 ID',
    api_id          BIGINT          NOT NULL                                    COMMENT 'API 注册表 ID',
    PRIMARY KEY (account_id, api_id),
    KEY idx_api_id (api_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'API 权限表（账号接口授权关系）';

-- API 回调日志表（记录业务事件回调的执行状态与重试历史）
CREATE TABLE IF NOT EXISTS sys_api_callback_log (
    id                  BIGINT          NOT NULL                                COMMENT '主键 ID（雪花算法）',
    account_id          BIGINT          NOT NULL                                COMMENT 'API 账号 ID',
    account_name        VARCHAR(100)    NOT NULL                                COMMENT '账号名称快照（冗余，防止账号删除后丢失上下文）',
    system_name         VARCHAR(100)             DEFAULT NULL                   COMMENT '业务系统名称快照',
    system_code         VARCHAR(64)              DEFAULT NULL                   COMMENT '业务系统编码快照',
    business_type       VARCHAR(100)    NOT NULL                                COMMENT '业务类型（如 ORDER_CREATED）',
    event_type          VARCHAR(100)    NOT NULL                                COMMENT '事件类型（如 PAYMENT_SUCCESS）',
    callback_url        VARCHAR(255)    NOT NULL                                COMMENT '回调地址',
    http_method         VARCHAR(10)     NOT NULL DEFAULT 'POST'                 COMMENT 'HTTP 方法',
    request_headers     TEXT                     DEFAULT NULL                   COMMENT '请求头 JSON',
    request_body        LONGTEXT                 DEFAULT NULL                   COMMENT '请求体',
    response_status_code INT                     DEFAULT NULL                   COMMENT '响应 HTTP 状态码',
    response_body       LONGTEXT                 DEFAULT NULL                   COMMENT '响应体',
    callback_status     TINYINT         NOT NULL DEFAULT 0                      COMMENT '回调状态：0 待执行，1 重试中，2 成功，3 失败',
    retry_count         INT             NOT NULL DEFAULT 0                      COMMENT '已重试次数',
    max_retry_count     INT             NOT NULL DEFAULT 3                      COMMENT '最大重试次数',
    next_retry_time     DATETIME                 DEFAULT NULL                   COMMENT '下次重试时间',
    last_callback_time  DATETIME                 DEFAULT NULL                   COMMENT '最近一次回调时间',
    success_time        DATETIME                 DEFAULT NULL                   COMMENT '回调成功时间',
    last_error_message  VARCHAR(500)             DEFAULT NULL                   COMMENT '最近一次错误信息',
    create_by           BIGINT                   DEFAULT NULL                   COMMENT '创建人 ID',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    update_by           BIGINT                   DEFAULT NULL                   COMMENT '更新人 ID',
    update_time         DATETIME                 DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0                      COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (id),
    KEY idx_account_id (account_id),
    KEY idx_callback_status_retry (callback_status, next_retry_time),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'API 回调日志表';

-- =============================================================================
-- Part 7: 基础数据初始化
-- =============================================================================

-- ── 超级管理员角色 ──
INSERT INTO sys_role (id, role_name, role_key, data_scope, sort, status, remark, create_time)
VALUES (1, '超级管理员', 'admin', 1, 1, 1, '超级管理员拥有所有权限，不受数据范围限制', NOW())
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- ── 超级管理员账号（密码：admin123，BCrypt 加密） ──
-- 注意：生产环境请在首次登录后立即修改密码！
INSERT INTO sys_user (id, username, password, real_name, status, create_time)
VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 1, NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- ── 超级管理员角色绑定 ──
INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- ── 菜单结构初始化 ──
-- 顶级菜单（parent_id = 0）
-- 1. 仪表看板
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (1, '仪表看板', 0, 'C', 'dashboard', 'dashboard/Workplace', 'monitor:server:list', 'dashboard', 1, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort = VALUES(sort);

-- 2. 硬件监测（由子级提拔至顶级，排在授权管理之后、资产目录之前）
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (51, '硬件监测', 0, 'C', 'server', 'monitor/server/index', 'monitor:server:list', 'desktop', 3, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort = VALUES(sort);

-- 5. 资源目录（API 接口列表）
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (2, '资源目录', 0, 'C', 'api/list', 'api/ApiList', 'system:apiList:list', 'layers', 5, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort = VALUES(sort);

-- 4. 告警中心（由子级提拔至顶级，排在硬件监测之后）
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (52, '告警中心', 0, 'C', 'alert', 'monitor/alert/index',  'monitor:alert:list',  'notification', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort = VALUES(sort);

-- 6. 治理中心（API 账号管理）
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES (3, '治理中心', 0, 'C', 'api/account', 'api/ApiAccountManage', 'api:account:list', 'safe', 6, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), sort = VALUES(sort);

-- 授权治理中心 — 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (31, '凭证画像查询', 3, 'B', 'api:account:query',     1, 1, 1),
    (32, '凭证签发新增', 3, 'B', 'api:account:add',       2, 1, 1),
    (33, '凭证主体编辑', 3, 'B', 'api:account:edit',      3, 1, 1),
    (34, '凭证注销移除', 3, 'B', 'api:account:remove',    4, 1, 1),
    (35, '密钥滚动重置', 3, 'B', 'api:account:reset',     5, 1, 1),
    (36, '凭证策略配置', 3, 'B', 'api:account:authorize', 6, 1, 1),
    (37, '资产全量发现', 3, 'B', 'api:account:sync',      7, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 6. 系统管理（目录）
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, icon, sort, visible, status)
VALUES (4, '系统管理', 0, 'M', 'system', 'settings', 6, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 系统管理 — 子菜单
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, perms, icon, sort, visible, status)
VALUES
    (41, '用户管理', 4, 'C', 'user',   'system/user/index',   'system:user:list',   'user',   1, 1, 1),
    (42, '角色管理', 4, 'C', 'role',   'system/role/index',   'system:role:list',   'idcard', 2, 1, 1),
    (43, '菜单管理', 4, 'C', 'menu',   'system/menu/index',   'system:menu:list',   'menu',   3, 1, 1),
    (44, '部门管理', 4, 'C', 'dept',   'system/dept/index',   'system:dept:list',   'branch', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 用户管理 — 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (411, '用户查询', 41, 'B', 'system:user:query',  1, 1, 1),
    (412, '用户新增', 41, 'B', 'system:user:add',    2, 1, 1),
    (413, '用户编辑', 41, 'B', 'system:user:edit',   3, 1, 1),
    (414, '用户删除', 41, 'B', 'system:user:remove', 4, 1, 1),
    (415, '重置密码', 41, 'B', 'system:user:reset',  5, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 角色管理 — 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (421, '角色查询', 42, 'B', 'system:role:query',  1, 1, 1),
    (422, '角色新增', 42, 'B', 'system:role:add',    2, 1, 1),
    (423, '角色编辑', 42, 'B', 'system:role:edit',   3, 1, 1),
    (424, '角色删除', 42, 'B', 'system:role:remove', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 菜单管理 — 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (431, '菜单查询', 43, 'B', 'system:menu:query',  1, 1, 1),
    (432, '菜单新增', 43, 'B', 'system:menu:add',    2, 1, 1),
    (433, '菜单编辑', 43, 'B', 'system:menu:edit',   3, 1, 1),
    (434, '菜单删除', 43, 'B', 'system:menu:remove', 4, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 7. 系统监控（目录，已提拔子级故隐藏此目录）
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, icon, sort, visible, status)
VALUES (5, '系统监控', 0, 'M', 'monitor', 'dashboard', 7, 0, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 服务器监控 — 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (511, '服务器监控查询', 51, 'B', 'monitor:server:list', 1, 1, 1),
    (512, '触发 GC',       51, 'B', 'monitor:server:gc',   2, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 告警中心 — 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, perms, sort, visible, status)
VALUES
    (521, '告警查询', 52, 'B', 'monitor:alert:list',   1, 1, 1),
    (522, '告警处理', 52, 'B', 'monitor:alert:edit',   2, 1, 1),
    (523, '告警删除', 52, 'B', 'monitor:alert:remove', 3, 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- ── 超级管理员赋予所有菜单权限 ──
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
