-- =============================================================================
-- BML-Backend 系统库初始化脚本
-- 版本: V1.0.0
-- 数据库: bml_system
-- 日期: 2026-02-09
-- =============================================================================

-- 使用数据库
-- CREATE DATABASE IF NOT EXISTS bml_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE bml_system;

-- =============================================================================
-- Part 1: 组织与部门
-- =============================================================================

-- 组织表
CREATE TABLE IF NOT EXISTS sys_org (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    org_name        VARCHAR(100)    NOT NULL COMMENT '组织名称',
    org_code        VARCHAR(50)     NOT NULL COMMENT '组织编码',
    parent_id       BIGINT          DEFAULT 0 COMMENT '上级组织ID',
    ancestors       VARCHAR(500)    DEFAULT '' COMMENT '祖级列表(逗号分隔)',
    sort            INT             DEFAULT 0 COMMENT '显示顺序',
    leader          VARCHAR(50)     DEFAULT NULL COMMENT '负责人',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    version         INT             DEFAULT 1 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_org_code (org_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB COMMENT='组织表';

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    dept_name       VARCHAR(100)    NOT NULL COMMENT '部门名称',
    dept_code       VARCHAR(50)     NOT NULL COMMENT '部门编码',
    org_id          BIGINT          NOT NULL COMMENT '所属组织ID',
    parent_id       BIGINT          DEFAULT 0 COMMENT '上级部门ID',
    ancestors       VARCHAR(500)    DEFAULT '' COMMENT '祖级列表(逗号分隔)',
    sort            INT             DEFAULT 0 COMMENT '显示顺序',
    leader          VARCHAR(50)     DEFAULT NULL COMMENT '负责人',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '联系电话',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    version         INT             DEFAULT 1 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_org_id (org_id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB COMMENT='部门表';

-- =============================================================================
-- Part 2: 用户与角色
-- =============================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    password        VARCHAR(100)    NOT NULL COMMENT '密码(BCrypt)',
    real_name       VARCHAR(50)     DEFAULT NULL COMMENT '姓名',
    nickname        VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    avatar          VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    gender          TINYINT         DEFAULT 0 COMMENT '性别(0未知 1男 2女)',
    org_id          BIGINT          DEFAULT NULL COMMENT '所属组织ID',
    dept_id         BIGINT          DEFAULT NULL COMMENT '所属部门ID',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0禁用 2锁定)',
    login_ip        VARCHAR(50)     DEFAULT NULL COMMENT '最后登录IP',
    login_date      DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    pwd_update_time DATETIME        DEFAULT NULL COMMENT '密码修改时间',
    pwd_error_count INT             DEFAULT 0 COMMENT '密码错误次数',
    lock_time       DATETIME        DEFAULT NULL COMMENT '锁定时间',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    version         INT             DEFAULT 1 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_org_id (org_id),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status)
) ENGINE=InnoDB COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    role_name       VARCHAR(50)     NOT NULL COMMENT '角色名称',
    role_code       VARCHAR(50)     NOT NULL COMMENT '角色编码',
    role_type       TINYINT         DEFAULT 1 COMMENT '角色类型(1普通 2系统)',
    data_scope      TINYINT         DEFAULT 1 COMMENT '数据权限范围(1全部 2本组织及下级 3仅本组织 4本部门及下级 5仅本部门 6仅本人 7自定义)',
    sort            INT             DEFAULT 0 COMMENT '显示顺序',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    version         INT             DEFAULT 1 COMMENT '乐观锁版本',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- =============================================================================
-- Part 3: 菜单与权限
-- =============================================================================

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    menu_name       VARCHAR(50)     NOT NULL COMMENT '菜单名称',
    perms           VARCHAR(100)    DEFAULT NULL COMMENT '权限标识',
    parent_id       BIGINT          DEFAULT 0 COMMENT '上级菜单ID',
    ancestors       VARCHAR(500)    DEFAULT '' COMMENT '祖级列表',
    menu_type       CHAR(1)         NOT NULL COMMENT '菜单类型(M目录 C菜单 B按钮)',
    path            VARCHAR(255)    DEFAULT NULL COMMENT '路由地址',
    component       VARCHAR(255)    DEFAULT NULL COMMENT '组件路径',
    query_params    VARCHAR(255)    DEFAULT NULL COMMENT '路由参数',
    icon            VARCHAR(100)    DEFAULT NULL COMMENT '菜单图标',
    sort            INT             DEFAULT 0 COMMENT '显示顺序',
    is_frame        TINYINT         DEFAULT 0 COMMENT '是否外链(0否 1是)',
    is_cache        TINYINT         DEFAULT 1 COMMENT '是否缓存(1是 0否)',
    visible         TINYINT         DEFAULT 1 COMMENT '是否显示(1是 0否)',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB COMMENT='菜单表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    menu_id         BIGINT          NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB COMMENT='角色菜单关联表';

-- 字段权限表
CREATE TABLE IF NOT EXISTS sys_field_permission (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    menu_code       VARCHAR(100)    NOT NULL COMMENT '菜单编码',
    field_name      VARCHAR(50)     NOT NULL COMMENT '字段名',
    visible         TINYINT         DEFAULT 1 COMMENT '是否可见(1是 0否)',
    editable        TINYINT         DEFAULT 1 COMMENT '是否可编辑(1是 0否)',
    required        TINYINT         DEFAULT 0 COMMENT '是否必填(1是 0否)',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu_field (role_id, menu_code, field_name)
) ENGINE=InnoDB COMMENT='字段权限表';

-- 数据权限规则表
CREATE TABLE IF NOT EXISTS sys_data_rule (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    menu_code       VARCHAR(100)    NOT NULL COMMENT '菜单编码',
    rule_name       VARCHAR(100)    NOT NULL COMMENT '规则名称',
    rule_column     VARCHAR(50)     NOT NULL COMMENT '规则字段',
    rule_operator   VARCHAR(20)     NOT NULL COMMENT '操作符(EQ/NE/IN/LIKE等)',
    rule_value      VARCHAR(500)    NOT NULL COMMENT '规则值(支持表达式)',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_role_menu (role_id, menu_code)
) ENGINE=InnoDB COMMENT='数据权限规则表';

-- ABAC访问规则表
CREATE TABLE IF NOT EXISTS sys_access_rule (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    rule_name       VARCHAR(100)    NOT NULL COMMENT '规则名称',
    rule_type       VARCHAR(20)     NOT NULL COMMENT '规则类型(TIME/IP/DEVICE/CUSTOM)',
    rule_expression TEXT            NOT NULL COMMENT 'SpEL表达式',
    enabled         TINYINT         DEFAULT 1 COMMENT '是否启用(1是 0否)',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '描述',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='访问规则表(ABAC)';

-- 角色访问规则关联表
CREATE TABLE IF NOT EXISTS sys_role_access_rule (
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    rule_id         BIGINT          NOT NULL COMMENT '规则ID',
    menu_code       VARCHAR(100)    DEFAULT NULL COMMENT '菜单编码(为空表示全局)',
    PRIMARY KEY (role_id, rule_id),
    KEY idx_rule_id (rule_id)
) ENGINE=InnoDB COMMENT='角色访问规则关联表';

-- =============================================================================
-- Part 4: 字典与配置
-- =============================================================================

-- 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    dict_name       VARCHAR(100)    NOT NULL COMMENT '字典名称',
    dict_type       VARCHAR(100)    NOT NULL COMMENT '字典类型',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB COMMENT='字典类型表';

-- 字典数据表
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    dict_type       VARCHAR(100)    NOT NULL COMMENT '字典类型',
    dict_label      VARCHAR(100)    NOT NULL COMMENT '字典标签',
    dict_value      VARCHAR(100)    NOT NULL COMMENT '字典值',
    order_num       INT             DEFAULT 0 COMMENT '显示顺序',
    css_class       VARCHAR(100)    DEFAULT NULL COMMENT '样式类名',
    list_class      VARCHAR(100)    DEFAULT NULL COMMENT '表格样式',
    is_default      TINYINT         DEFAULT 0 COMMENT '是否默认(1是 0否)',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB COMMENT='字典数据表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    config_name     VARCHAR(100)    NOT NULL COMMENT '配置名称',
    config_key      VARCHAR(100)    NOT NULL COMMENT '配置键',
    config_value    TEXT            COMMENT '配置值',
    config_type     TINYINT         DEFAULT 1 COMMENT '配置类型(1系统 2业务)',
    is_frontend     TINYINT         DEFAULT 0 COMMENT '是否前端(1是 0否)',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB COMMENT='系统配置表';

-- =============================================================================
-- Part 5: License授权
-- =============================================================================

CREATE TABLE IF NOT EXISTS sys_license (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    license_key     TEXT            NOT NULL COMMENT '授权密钥(RSA加密)',
    machine_code    VARCHAR(255)    NOT NULL COMMENT '机器码',
    customer_name   VARCHAR(100)    NOT NULL COMMENT '客户名称',
    product_name    VARCHAR(100)    DEFAULT 'BML-Backend' COMMENT '产品名称',
    product_version VARCHAR(20)     DEFAULT '1.0.0' COMMENT '产品版本',
    license_type    VARCHAR(20)     DEFAULT 'STANDARD' COMMENT '授权类型(TRIAL/STANDARD/ENTERPRISE)',
    max_users       INT             DEFAULT 100 COMMENT '最大用户数',
    enabled_modules VARCHAR(500)    DEFAULT '*' COMMENT '启用模块(逗号分隔,*表示全部)',
    issue_date      DATE            NOT NULL COMMENT '颁发日期',
    expire_date     DATE            NOT NULL COMMENT '过期日期',
    signature       TEXT            NOT NULL COMMENT '数字签名',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1有效 0无效)',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='系统授权表';

-- =============================================================================
-- Part 6: API管理
-- =============================================================================

-- API账号表
CREATE TABLE IF NOT EXISTS sys_api_account (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    account_name    VARCHAR(100)    NOT NULL COMMENT '账号名称',
    access_key      VARCHAR(50)     NOT NULL COMMENT 'AccessKey',
    secret_key      VARCHAR(100)    NOT NULL COMMENT 'SecretKey(加密存储)',
    account_type    TINYINT         DEFAULT 1 COMMENT '账号类型(1内部 2外部)',
    rate_limit      INT             DEFAULT 1000 COMMENT '请求限流(次/分钟)',
    expire_time     DATETIME        DEFAULT NULL COMMENT '过期时间(NULL为永久)',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0禁用)',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '删除标志(0未删除 1已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_access_key (access_key)
) ENGINE=InnoDB COMMENT='API账号表';

-- API注册表
CREATE TABLE IF NOT EXISTS sys_api_registry (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    api_name        VARCHAR(100)    NOT NULL COMMENT 'API名称',
    api_url         VARCHAR(255)    NOT NULL COMMENT 'API路径',
    http_method     VARCHAR(10)     NOT NULL COMMENT 'HTTP方法',
    module_name     VARCHAR(50)     NOT NULL COMMENT '模块名称',
    controller_name VARCHAR(100)    NOT NULL COMMENT '控制器名称',
    method_name     VARCHAR(100)    NOT NULL COMMENT '方法名称',
    description     VARCHAR(500)    DEFAULT NULL COMMENT 'API描述',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1正常 0停用)',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_url_method (api_url, http_method)
) ENGINE=InnoDB COMMENT='API注册表';

-- API权限表
CREATE TABLE IF NOT EXISTS sys_api_permission (
    account_id      BIGINT          NOT NULL COMMENT '账号ID',
    api_id          BIGINT          NOT NULL COMMENT 'API ID',
    PRIMARY KEY (account_id, api_id),
    KEY idx_api_id (api_id)
) ENGINE=InnoDB COMMENT='API权限表';

-- =============================================================================
-- Part 7: 日志表(支持分表)
-- =============================================================================

-- 操作日志表模板
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    module          VARCHAR(50)     NOT NULL COMMENT '模块名称',
    operation       VARCHAR(100)    NOT NULL COMMENT '操作名称',
    http_method     VARCHAR(10)     DEFAULT NULL COMMENT 'HTTP方法',
    request_url     VARCHAR(255)    DEFAULT NULL COMMENT '请求URL',
    request_params  TEXT            COMMENT '请求参数',
    response_result TEXT            COMMENT '响应结果',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1成功 0失败)',
    error_msg       TEXT            COMMENT '错误信息',
    operator_id     BIGINT          DEFAULT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(50)     DEFAULT NULL COMMENT '操作人姓名',
    operator_ip     VARCHAR(50)     DEFAULT NULL COMMENT '操作人IP',
    operator_ua     VARCHAR(500)    DEFAULT NULL COMMENT '操作人UA',
    cost_time       BIGINT          DEFAULT 0 COMMENT '耗时(毫秒)',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_operator_id (operator_id),
    KEY idx_create_time (create_time),
    KEY idx_module (module)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 登录日志表模板
CREATE TABLE IF NOT EXISTS sys_login_log (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    username        VARCHAR(50)     NOT NULL COMMENT '用户名',
    user_id         BIGINT          DEFAULT NULL COMMENT '用户ID',
    login_type      VARCHAR(20)     DEFAULT 'PASSWORD' COMMENT '登录方式(PASSWORD/SMS/SCAN)',
    login_ip        VARCHAR(50)     DEFAULT NULL COMMENT '登录IP',
    login_location  VARCHAR(100)    DEFAULT NULL COMMENT '登录地点',
    browser         VARCHAR(50)     DEFAULT NULL COMMENT '浏览器',
    os              VARCHAR(50)     DEFAULT NULL COMMENT '操作系统',
    status          TINYINT         DEFAULT 1 COMMENT '状态(1成功 0失败)',
    msg             VARCHAR(255)    DEFAULT NULL COMMENT '提示消息',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='登录日志表';

-- =============================================================================
-- Part 8: 初始数据
-- =============================================================================

-- 初始化组织
INSERT INTO sys_org (id, org_name, org_code, parent_id, ancestors, sort, status) VALUES
(1, 'BML集团总部', 'BML', 0, '0', 1, 1);

-- 初始化部门
INSERT INTO sys_dept (id, dept_name, dept_code, org_id, parent_id, ancestors, sort, status) VALUES
(1, '技术部', 'TECH', 1, 0, '0', 1, 1),
(2, '产品部', 'PRODUCT', 1, 0, '0', 2, 1),
(3, '运营部', 'OPERATION', 1, 0, '0', 3, 1);

-- 初始化超级管理员 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (id, username, password, nickname, email, org_id, dept_id, status) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '超级管理员', 'admin@bml.com', 1, 1, 1);

-- 初始化角色
INSERT INTO sys_role (id, role_name, role_code, role_type, data_scope, sort, status, remark) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 2, 1, 1, 1, '拥有全部权限'),
(2, '系统管理员', 'SYS_ADMIN', 2, 2, 2, 1, '系统管理权限'),
(3, '普通用户', 'USER', 1, 6, 3, 1, '普通用户权限');

-- 初始化用户角色关联
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1);

-- 初始化菜单
INSERT INTO sys_menu (id, menu_name, perms, parent_id, menu_type, path, component, icon, sort, status) VALUES
-- 一级目录
(1, '系统管理', 'system', 0, 'M', '/system', NULL, 'setting', 1, 1),
(2, '系统监控', 'monitor', 0, 'M', '/monitor', NULL, 'monitor', 2, 1),
-- 系统管理-菜单
(100, '用户管理', 'system:user', 1, 'C', 'user', 'system/user/index', 'user', 1, 1),
(101, '角色管理', 'system:role', 1, 'C', 'role', 'system/role/index', 'peoples', 2, 1),
(102, '菜单管理', 'system:menu', 1, 'C', 'menu', 'system/menu/index', 'tree-table', 3, 1),
(103, '部门管理', 'system:dept', 1, 'C', 'dept', 'system/dept/index', 'tree', 4, 1),
(104, '字典管理', 'system:dict', 1, 'C', 'dict', 'system/dict/index', 'dict', 5, 1),
(105, '参数配置', 'system:config', 1, 'C', 'config', 'system/config/index', 'edit', 6, 1),
-- 用户管理-按钮
(1001, '用户查询', 'system:user:query', 100, 'B', NULL, NULL, NULL, 1, 1),
(1002, '用户新增', 'system:user:add', 100, 'B', NULL, NULL, NULL, 2, 1),
(1003, '用户修改', 'system:user:edit', 100, 'B', NULL, NULL, NULL, 3, 1),
(1004, '用户删除', 'system:user:delete', 100, 'B', NULL, NULL, NULL, 4, 1),
(1005, '用户导出', 'system:user:export', 100, 'B', NULL, NULL, NULL, 5, 1),
(1006, '重置密码', 'system:user:resetPwd', 100, 'B', NULL, NULL, NULL, 6, 1),
-- 角色管理-按钮
(1011, '角色查询', 'system:role:query', 101, 'B', NULL, NULL, NULL, 1, 1),
(1012, '角色新增', 'system:role:add', 101, 'B', NULL, NULL, NULL, 2, 1),
(1013, '角色修改', 'system:role:edit', 101, 'B', NULL, NULL, NULL, 3, 1),
(1014, '角色删除', 'system:role:delete', 101, 'B', NULL, NULL, NULL, 4, 1),
-- 监控-菜单
(200, '操作日志', 'monitor:operlog', 2, 'C', 'operlog', 'monitor/operlog/index', 'form', 1, 1),
(201, '登录日志', 'monitor:loginlog', 2, 'C', 'loginlog', 'monitor/loginlog/index', 'logininfor', 2, 1);

-- 角色菜单关联 (超级管理员拥有全部菜单)
INSERT INTO sys_role_menu (role_id, menu_id) 
SELECT 1, id FROM sys_menu;

-- 初始化字典类型
INSERT INTO sys_dict_type (id, dict_name, dict_type, status) VALUES
(1, '用户性别', 'sys_user_gender', 1),
(2, '系统状态', 'sys_status', 1),
(3, '数据范围', 'sys_data_scope', 1);

-- 初始化字典数据
INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, order_num, is_default, status) VALUES
(1, 'sys_user_gender', '未知', '0', 1, 0, 1),
(2, 'sys_user_gender', '男', '1', 2, 1, 1),
(3, 'sys_user_gender', '女', '2', 3, 0, 1),
(4, 'sys_status', '正常', '1', 1, 1, 1),
(5, 'sys_status', '停用', '0', 2, 0, 1),
(6, 'sys_data_scope', '全部数据', '1', 1, 0, 1),
(7, 'sys_data_scope', '本组织及下级', '2', 2, 0, 1),
(8, 'sys_data_scope', '仅本组织', '3', 3, 0, 1),
(9, 'sys_data_scope', '本部门及下级', '4', 4, 0, 1),
(10, 'sys_data_scope', '仅本部门', '5', 5, 0, 1),
(11, 'sys_data_scope', '仅本人', '6', 6, 1, 1),
(12, 'sys_data_scope', '自定义', '7', 7, 0, 1);

-- 初始化系统配置
INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, is_frontend, remark) VALUES
(1, '系统名称', 'sys.name', 'BML企业管理系统', 1, 1, '系统名称'),
(2, '默认密码', 'sys.user.defaultPassword', '123456', 1, 0, '新用户默认密码'),
(3, '密码最小长度', 'sys.password.minLength', '6', 1, 0, '密码最小长度'),
(4, '登录失败锁定次数', 'sys.login.maxRetryCount', '5', 1, 0, '超过次数锁定账号'),
(5, '登录失败锁定时长(分钟)', 'sys.login.lockTime', '30', 1, 0, '锁定时长'),
(6, 'Token有效期(分钟)', 'sys.token.expireTime', '120', 1, 0, 'AccessToken有效期'),
(7, 'RefreshToken有效期(天)', 'sys.token.refreshExpireTime', '7', 1, 0, 'RefreshToken有效期');
