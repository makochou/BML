-- =============================================================================
-- BML 企业中台管理系统 — Flyway 合并基线脚本（V1.0.0 候选）
-- 
-- 用途   : 作为 Flyway 的【新基线】，替代原 V1.0.0~V2.24.0 共 44 个增量脚本。
--          内容 = 44 个迁移执行后的最终态（全部表结构 + 基础数据）。
-- 区别   : 不含 CREATE DATABASE / CREATE USER（Flyway 连接到已存在的库后执行）。
-- 适用   : 全新仓库 / 全新环境，从合并基线起步，后续变更继续追加 V2.25.0+。
-- 采用方式: 详见同目录《数据库初始化与合并教程.md》第 5 节。
-- =============================================================================

SET NAMES utf8mb4;
SET @OLD_SQL_MODE = @@SQL_MODE;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 第一部分：表结构（DDL） —— 共 29 张表
-- =============================================================================

-- =============================================================================
-- 组织架构（组织 / 部门 / 岗位）
-- =============================================================================

-- 【表】sys_org：组织表
CREATE TABLE IF NOT EXISTS `sys_org` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `org_name` varchar(100) NOT NULL COMMENT '组织名称',
  `org_code` varchar(50) NOT NULL COMMENT '组织编码（全局唯一）',
  `org_type` tinyint(4) NOT NULL DEFAULT 2 COMMENT '机构类型 (1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部)',
  `credit_code` varchar(18) DEFAULT NULL COMMENT '统一社会信用代码（18位）',
  `legal_person` varchar(50) DEFAULT NULL COMMENT '法定代表人',
  `registered_capital` decimal(18,2) DEFAULT NULL COMMENT '注册资本（万元）',
  `establish_date` date DEFAULT NULL COMMENT '成立日期',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '上级组织 ID，顶级为 0',
  `ancestors` varchar(500) NOT NULL DEFAULT '' COMMENT '祖级 ID 列表，逗号分隔，用于快速查询子树',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '显示顺序，数字越小越靠前',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `district` varchar(50) DEFAULT NULL COMMENT '区县',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `business_scope` text DEFAULT NULL COMMENT '经营范围',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `data_isolation` tinyint(4) NOT NULL DEFAULT 0 COMMENT '数据隔离模式 (0:共享 1:完全隔离 2:汇总共享 3:同级互通 4:按模块隔离)',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_code` (`org_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织表';;

-- 【表】sys_dept：部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `dept_name` varchar(100) NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) NOT NULL COMMENT '部门编码（全局唯一）',
  `dept_type` tinyint(4) NOT NULL DEFAULT 3 COMMENT '部门类型 (1:事业部 2:中心 3:部门 4:小组)',
  `func_type` varchar(20) DEFAULT NULL COMMENT '职能分类 (管理/研发/销售/财务/人事/行政/生产/采购/仓储)',
  `org_id` bigint(20) NOT NULL COMMENT '所属组织 ID',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '上级部门 ID，顶级为 0',
  `ancestors` varchar(500) NOT NULL DEFAULT '' COMMENT '祖级 ID 列表，逗号分隔',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';;

-- 【表】sys_post：岗位表
CREATE TABLE IF NOT EXISTS `sys_post` (
  `id` bigint(20) NOT NULL COMMENT '岗位ID（雪花算法）',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码（全局唯一）',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `org_id` bigint(20) DEFAULT NULL COMMENT '所属机构ID（NULL 表示全局岗位）',
  `post_category` varchar(20) DEFAULT NULL COMMENT '岗位类别 (管理类/技术类/行政类/财务类/销售类/生产类)',
  `post_level` varchar(20) DEFAULT NULL COMMENT '岗位级别 (如 P1~P10 技术序列, M1~M5 管理序列)',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 (1:正常 0:停用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0:存在 1:删除)',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_code` (`post_code`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='岗位表';;

-- =============================================================================
-- 用户与角色（用户 / 角色 / 用户-角色关联）
-- =============================================================================

-- 【表】sys_user：用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `username` varchar(50) NOT NULL COMMENT '登录用户名（全局唯一）',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像 URL',
  `gender` tinyint(4) NOT NULL DEFAULT 0 COMMENT '性别：0 未知，1 男，2 女',
  `org_id` bigint(20) DEFAULT NULL COMMENT '所属组织 ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '所属部门 ID',
  `post_id` bigint(20) DEFAULT NULL COMMENT '岗位ID',
  `superior_id` bigint(20) DEFAULT NULL COMMENT '直属上级用户ID（构建汇报链，NULL表示无上级）',
  `employee_no` varchar(30) DEFAULT NULL COMMENT '工号',
  `entry_date` date DEFAULT NULL COMMENT '入职日期',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用，2 锁定',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录 IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `pwd_update_time` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `pwd_error_count` int(11) NOT NULL DEFAULT 0 COMMENT '连续密码错误次数',
  `lock_time` datetime DEFAULT NULL COMMENT '账号锁定时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_superior_id` (`superior_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';;

-- 【表】sys_role：角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串（如 admin、common）',
  `data_scope` tinyint(4) NOT NULL DEFAULT 1 COMMENT '数据范围：1 全部，2 本组织及下级，3 仅本组织，4 本部门及下级，5 仅本部门，6 仅本人，7 自定义',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';;

-- 【表】sys_user_role：用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';;

-- =============================================================================
-- 菜单与权限（菜单 / 角色-菜单 / 用户-菜单 / 字段权限）
-- =============================================================================

-- 【表】sys_menu：菜单权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '父菜单 ID，顶级为 0',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型：M 目录，C 菜单，B 按钮',
  `path` varchar(200) DEFAULT NULL COMMENT '路由路径（前端 Vue Router path）',
  `component` varchar(255) DEFAULT NULL COMMENT '前端组件路径（相对 views/ 目录）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识（如 system:user:list）',
  `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标（Arco Design 图标名）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '显示顺序，数字越小越靠前',
  `visible` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否显示：1 显示，0 隐藏',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `is_frame` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否外链：1 是，0 否',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';;

-- 【表】sys_role_menu：角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单 ID',
  `half_check` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否半选状态：0 完全勾选（权限已授予），1 半选（仅部分子权限授予）',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';;

-- 【表】sys_user_menu：用户菜单权限关联表（个人功能授权）
CREATE TABLE IF NOT EXISTS `sys_user_menu` (
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单 ID',
  `half_check` tinyint(4) NOT NULL DEFAULT 0 COMMENT '半选标记：0=完全勾选，1=半选（父节点部分子节点被选中）',
  PRIMARY KEY (`user_id`,`menu_id`),
  KEY `idx_user_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户菜单权限关联表（个人功能授权）';;

-- 【表】sys_field_permission：字段权限表
CREATE TABLE IF NOT EXISTS `sys_field_permission` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  `entity_name` varchar(100) NOT NULL COMMENT '实体类名（如 SysUser）',
  `field_name` varchar(100) NOT NULL COMMENT '字段名（如 phone）',
  `visible` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否可见：1 可见，0 隐藏',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_entity_field` (`role_id`,`entity_name`,`field_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段权限表';;

-- =============================================================================
-- 数据权限（数据规则 / 角色-机构 / 角色-部门 / 用户数据范围 / 机构数据共享）
-- =============================================================================

-- 【表】sys_data_rule：数据权限规则表
CREATE TABLE IF NOT EXISTS `sys_data_rule` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `entity_name` varchar(100) NOT NULL COMMENT '实体类名',
  `column_name` varchar(100) NOT NULL COMMENT '过滤字段名',
  `condition_type` varchar(20) NOT NULL COMMENT '条件类型：EQ 等于，LIKE 模糊，IN 包含',
  `condition_value` varchar(500) NOT NULL COMMENT '条件值（支持 SpEL 表达式）',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据权限规则表';;

-- 【表】sys_role_org：角色自定义数据权限 — 角色机构关联表
CREATE TABLE IF NOT EXISTS `sys_role_org` (
  `role_id` bigint(20) NOT NULL COMMENT '角色 ID',
  `org_id` bigint(20) NOT NULL COMMENT '机构 ID',
  PRIMARY KEY (`role_id`,`org_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色自定义数据权限 — 角色机构关联表';;

-- 【表】sys_role_dept：角色自定义数据权限 — 角色部门关联表
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色自定义数据权限 — 角色部门关联表';;

-- 【表】sys_user_data_scope：用户个人数据权限配置表
CREATE TABLE IF NOT EXISTS `sys_user_data_scope` (
  `id` bigint(20) NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `data_scope` tinyint(4) NOT NULL DEFAULT 6 COMMENT '数据范围 (1:全部 2:本机构及下级 3:仅本机构 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义 8:本人及下属)',
  `custom_org_ids` text DEFAULT NULL COMMENT '自定义可访问机构ID列表（逗号分隔，data_scope=7时生效）',
  `custom_dept_ids` text DEFAULT NULL COMMENT '自定义可访问部门ID列表（逗号分隔，data_scope=7时生效）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 (1:生效 0:停用)',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永不过期，用于临时授权场景）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注（记录授权原因，如"临时借调至XX部门"）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0:存在 1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户个人数据权限配置表';;

-- 【表】sys_org_data_share：机构数据共享规则表
CREATE TABLE IF NOT EXISTS `sys_org_data_share` (
  `id` bigint(20) NOT NULL COMMENT '主键ID（雪花算法）',
  `source_org_id` bigint(20) NOT NULL COMMENT '源机构ID（数据所属机构）',
  `target_org_id` bigint(20) NOT NULL COMMENT '目标机构ID（被共享的机构）',
  `share_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '共享类型 (1:全模块共享 2:指定模块共享)',
  `module_code` varchar(100) DEFAULT NULL COMMENT '模块编码（share_type=2时有效，多个用逗号分隔，如 finance,inventory）',
  `permission` tinyint(4) NOT NULL DEFAULT 1 COMMENT '权限级别 (1:只读 2:读写)',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态 (1:生效 0:停用)',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间（NULL表示永不过期）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除 (0:存在 1:删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_target` (`source_org_id`,`target_org_id`,`module_code`),
  KEY `idx_source_org` (`source_org_id`),
  KEY `idx_target_org` (`target_org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构数据共享规则表';;

-- =============================================================================
-- 字典与系统配置
-- =============================================================================

-- 【表】sys_dict_type：字典类型表
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型（全局唯一，如 sys_user_sex）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';;

-- 【表】sys_dict_data：字典数据表
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签（显示文本）',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值（存储值）',
  `css_class` varchar(100) DEFAULT NULL COMMENT 'CSS 样式类名（用于前端标签颜色）',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';;

-- 【表】sys_config：系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_key` varchar(100) NOT NULL COMMENT '配置键名（全局唯一）',
  `config_value` varchar(500) NOT NULL COMMENT '配置键值',
  `config_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否系统内置：1 是，0 否（内置配置不允许删除）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';;

-- =============================================================================
-- 日志与告警（操作日志 / 登录日志 / 告警）
-- =============================================================================

-- 【表】sys_operation_log：操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `title` varchar(50) DEFAULT NULL COMMENT '模块标题',
  `business_type` int(11) NOT NULL DEFAULT 0 COMMENT '业务类型：0 其他，1 新增，2 修改，3 删除，4 查询',
  `method` varchar(200) DEFAULT NULL COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT NULL COMMENT 'HTTP 请求方式',
  `operator_type` int(11) NOT NULL DEFAULT 0 COMMENT '操作类别：0 其他，1 后台用户，2 手机端用户',
  `oper_name` varchar(50) DEFAULT NULL COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT NULL COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT NULL COMMENT '请求 URL',
  `oper_ip` varchar(128) DEFAULT NULL COMMENT '主机地址',
  `oper_param` varchar(2000) DEFAULT NULL COMMENT '请求参数',
  `json_result` varchar(2000) DEFAULT NULL COMMENT '返回参数',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '操作状态：0 正常，1 异常',
  `error_msg` varchar(2000) DEFAULT NULL COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint(20) DEFAULT 0 COMMENT '消耗时间（毫秒）',
  PRIMARY KEY (`id`),
  KEY `idx_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';;

-- 【表】sys_login_log：登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户账号',
  `ipaddr` varchar(128) DEFAULT NULL COMMENT '登录 IP 地址',
  `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '登录状态：1 成功，0 失败',
  `msg` varchar(255) DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';;

-- 【表】sys_alert：系统告警表
CREATE TABLE IF NOT EXISTS `sys_alert` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `alert_type` varchar(50) NOT NULL COMMENT '告警类型：CPU_HIGH / MEMORY_HIGH / DISK_FULL / JVM_HIGH',
  `alert_level` varchar(20) NOT NULL COMMENT '告警级别：info / warning / error / critical',
  `alert_title` varchar(100) NOT NULL COMMENT '告警标题',
  `alert_content` text NOT NULL COMMENT '告警详情内容',
  `read_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '阅读状态：0 未读，1 已读',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_read_status` (`read_status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_alert_dedup` (`alert_type`,`alert_title`,`read_status`,`deleted`),
  KEY `idx_update_time` (`update_time`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统告警表';;

-- =============================================================================
-- 运维模块（文件 / 通知公告 / 定时任务 / 任务日志）
-- =============================================================================

-- 【表】sys_file_info：系统文件表
CREATE TABLE IF NOT EXISTS `sys_file_info` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_name` varchar(255) NOT NULL COMMENT '存储文件名',
  `file_path` varchar(1000) NOT NULL COMMENT '文件物理路径',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件访问地址',
  `file_ext` varchar(30) DEFAULT NULL COMMENT '文件扩展名',
  `file_size` bigint(20) NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `mime_type` varchar(100) DEFAULT NULL COMMENT 'MIME 类型',
  `storage_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '存储类型：1 本地',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_file_original_name` (`original_name`),
  KEY `idx_file_ext` (`file_ext`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统文件表';;

-- 【表】sys_notice：通知公告表
CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `notice_title` varchar(200) NOT NULL COMMENT '公告标题',
  `notice_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '公告类型：1 通知，2 公告，3 维护',
  `notice_content` longtext NOT NULL COMMENT '公告内容',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '状态：0 草稿，1 已发布',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_notice_type` (`notice_type`),
  KEY `idx_notice_status` (`status`),
  KEY `idx_notice_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知公告表';;

-- 【表】sys_job：定时任务表
CREATE TABLE IF NOT EXISTS `sys_job` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `job_name` varchar(100) NOT NULL COMMENT '任务名称',
  `job_group` varchar(100) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标',
  `cron_expression` varchar(100) NOT NULL COMMENT 'Cron 表达式',
  `misfire_policy` tinyint(4) NOT NULL DEFAULT 1 COMMENT '错过策略：1 立即执行，2 执行一次，3 放弃执行',
  `concurrent` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否并发：1 允许，0 禁止',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '任务状态：1 启用，0 暂停',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_job_group` (`job_group`),
  KEY `idx_job_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='定时任务表';;

-- 【表】sys_job_log：任务日志表
CREATE TABLE IF NOT EXISTS `sys_job_log` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID',
  `job_name` varchar(100) NOT NULL COMMENT '任务名称',
  `job_group` varchar(100) NOT NULL COMMENT '任务分组',
  `invoke_target` varchar(500) NOT NULL COMMENT '调用目标',
  `job_message` varchar(500) DEFAULT NULL COMMENT '日志信息',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '执行状态：0 成功，1 失败',
  `exception_info` text DEFAULT NULL COMMENT '异常信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `cost_time` bigint(20) DEFAULT 0 COMMENT '耗时毫秒',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_job_log_name` (`job_name`),
  KEY `idx_job_log_status` (`status`),
  KEY `idx_job_log_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务日志表';;

-- =============================================================================
-- 开放接口 OpenAPI（接入账号 / 接口注册 / 接口授权 / 回调日志）
-- =============================================================================

-- 【表】sys_api_account：API 账号表
CREATE TABLE IF NOT EXISTS `sys_api_account` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `account_name` varchar(100) NOT NULL COMMENT '账号名称（全局唯一）',
  `description` varchar(255) DEFAULT NULL COMMENT '账号用途描述，说明业务场景（如：生产数据同步、沙箱调试）',
  `access_key` varchar(64) NOT NULL COMMENT 'AccessKey（公钥，全局唯一）',
  `secret_key` varchar(255) NOT NULL COMMENT 'SecretKey（AES 加密后存储，明文仅在创建/重置时返回一次）',
  `account_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '账号类型：1 内部账号，2 外部账号',
  `client_types` varchar(200) DEFAULT NULL COMMENT '允许的客户端类型，英文逗号分隔（如 web,mobile,server）',
  `owner_name` varchar(50) DEFAULT NULL COMMENT '接入方负责人姓名',
  `owner_contact` varchar(100) DEFAULT NULL COMMENT '负责人联系方式（手机/邮箱）',
  `system_name` varchar(100) DEFAULT NULL COMMENT '接入业务系统名称',
  `system_code` varchar(64) DEFAULT NULL COMMENT '接入业务系统编码',
  `access_environment` varchar(32) DEFAULT NULL COMMENT '接入环境：test / staging / production',
  `ip_whitelist` text DEFAULT NULL COMMENT 'IP 白名单（兼容字段，存储当前环境生效的白名单，逗号分隔，支持 CIDR）',
  `test_ip_whitelist` text DEFAULT NULL COMMENT '测试环境 IP 白名单（逗号分隔，支持 CIDR）',
  `staging_ip_whitelist` text DEFAULT NULL COMMENT '预发环境 IP 白名单（逗号分隔，支持 CIDR）',
  `production_ip_whitelist` text DEFAULT NULL COMMENT '生产环境 IP 白名单（逗号分隔，支持 CIDR）',
  `sign_version` varchar(16) NOT NULL DEFAULT 'v1' COMMENT '签名算法版本（当前支持 v1：HmacSHA256）',
  `allowed_scopes` varchar(500) DEFAULT NULL COMMENT '授权范围标签，逗号分隔（如 read,write,admin），对标 OAuth2 scope',
  `callback_url` varchar(255) DEFAULT NULL COMMENT '业务回调地址（接收业务事件推送）',
  `rate_limit` int(11) NOT NULL DEFAULT 1000 COMMENT '每分钟限流阈值（次/分钟）',
  `expire_time` datetime DEFAULT NULL COMMENT '账号过期时间，NULL 表示永不过期',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `version` int(11) NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_name` (`account_name`),
  UNIQUE KEY `uk_access_key` (`access_key`),
  KEY `idx_status` (`status`),
  KEY `idx_access_environment` (`access_environment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 账号表';;

-- 【表】sys_api_registry：API 注册表（接口目录）
CREATE TABLE IF NOT EXISTS `sys_api_registry` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `api_name` varchar(200) DEFAULT NULL COMMENT '接口名称（来自 @Operation(summary) 注解）',
  `api_url` varchar(255) NOT NULL COMMENT '接口路径（去除 context-path 前缀）',
  `http_method` varchar(10) NOT NULL COMMENT 'HTTP 方法：GET / POST / PUT / DELETE / PATCH 等',
  `module_name` varchar(100) DEFAULT NULL COMMENT '所属模块名（来自包名，如 system / api）',
  `controller_name` varchar(200) DEFAULT NULL COMMENT '所属控制器类名（如 SysUserController）',
  `method_name` varchar(200) DEFAULT NULL COMMENT '控制器方法名（如 listUsers）',
  `description` varchar(500) DEFAULT NULL COMMENT '接口描述（来自 @Operation(description) 注解）',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态：1 启用（接口存在），0 停用（接口已删除）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_url_method` (`api_url`,`http_method`),
  KEY `idx_module_name` (`module_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 注册表（接口目录）';;

-- 【表】sys_api_permission：API 权限表（账号接口授权关系）
CREATE TABLE IF NOT EXISTS `sys_api_permission` (
  `account_id` bigint(20) NOT NULL COMMENT 'API 账号 ID',
  `api_id` bigint(20) NOT NULL COMMENT 'API 注册表 ID',
  PRIMARY KEY (`account_id`,`api_id`),
  KEY `idx_api_id` (`api_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 权限表（账号接口授权关系）';;

-- 【表】sys_api_callback_log：API 回调日志表
CREATE TABLE IF NOT EXISTS `sys_api_callback_log` (
  `id` bigint(20) NOT NULL COMMENT '主键 ID（雪花算法）',
  `account_id` bigint(20) NOT NULL COMMENT 'API 账号 ID',
  `account_name` varchar(100) NOT NULL COMMENT '账号名称快照（冗余，防止账号删除后丢失上下文）',
  `system_name` varchar(100) DEFAULT NULL COMMENT '业务系统名称快照',
  `system_code` varchar(64) DEFAULT NULL COMMENT '业务系统编码快照',
  `business_type` varchar(100) NOT NULL COMMENT '业务类型（如 ORDER_CREATED）',
  `event_type` varchar(100) NOT NULL COMMENT '事件类型（如 PAYMENT_SUCCESS）',
  `callback_url` varchar(255) NOT NULL COMMENT '回调地址',
  `http_method` varchar(10) NOT NULL DEFAULT 'POST' COMMENT 'HTTP 方法',
  `request_headers` text DEFAULT NULL COMMENT '请求头 JSON',
  `request_body` longtext DEFAULT NULL COMMENT '请求体',
  `response_status_code` int(11) DEFAULT NULL COMMENT '响应 HTTP 状态码',
  `response_body` longtext DEFAULT NULL COMMENT '响应体',
  `callback_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '回调状态：0 待执行，1 重试中，2 成功，3 失败',
  `retry_count` int(11) NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry_count` int(11) NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `next_retry_time` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_callback_time` datetime DEFAULT NULL COMMENT '最近一次回调时间',
  `success_time` datetime DEFAULT NULL COMMENT '回调成功时间',
  `last_error_message` varchar(500) DEFAULT NULL COMMENT '最近一次错误信息',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人 ID',
  `update_time` datetime DEFAULT NULL ON UPDATE current_timestamp() COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_callback_status_retry` (`callback_status`,`next_retry_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 回调日志表';;

-- =============================================================================
-- 第二部分：基础数据（DML）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 基础数据：组织 / 部门 / 岗位
-- -----------------------------------------------------------------------------

-- sys_org（3 行）
INSERT INTO `sys_org` (`id`, `org_name`, `org_code`, `org_type`, `credit_code`, `legal_person`, `registered_capital`, `establish_date`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `province`, `city`, `district`, `address`, `business_scope`, `remark`, `data_isolation`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (100,'BML科技集团','GROUP_BML',1,NULL,NULL,NULL,NULL,0,'0',1,'管理员',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_org` (`id`, `org_name`, `org_code`, `org_type`, `credit_code`, `legal_person`, `registered_capital`, `establish_date`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `province`, `city`, `district`, `address`, `business_scope`, `remark`, `data_isolation`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (101,'深圳总公司','CO_SZ',2,NULL,NULL,NULL,NULL,100,'0,100',1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_org` (`id`, `org_name`, `org_code`, `org_type`, `credit_code`, `legal_person`, `registered_capital`, `establish_date`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `province`, `city`, `district`, `address`, `business_scope`, `remark`, `data_isolation`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (102,'北京分公司','BR_BJ',3,NULL,NULL,NULL,NULL,100,'0,100',2,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);

-- sys_dept（26 行）
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1000,'总裁办公室','SZ_MGT_001',3,'管理',101,0,'0',1,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1100,'研发中心','SZ_RD_000',2,'研发',101,0,'0',2,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1101,'产品部','SZ_RD_001',3,'研发',101,1100,'0,1100',1,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1102,'前端开发部','SZ_RD_002',3,'研发',101,1100,'0,1100',2,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1103,'后端开发部','SZ_RD_003',3,'研发',101,1100,'0,1100',3,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1104,'移动开发部','SZ_RD_004',3,'研发',101,1100,'0,1100',4,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1105,'测试部','SZ_RD_005',3,'研发',101,1100,'0,1100',5,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1106,'运维部','SZ_RD_006',3,'研发',101,1100,'0,1100',6,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1200,'运营中心','SZ_OP_000',2,'销售',101,0,'0',3,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1201,'市场部','SZ_OP_001',3,'销售',101,1200,'0,1200',1,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1202,'销售部','SZ_OP_002',3,'销售',101,1200,'0,1200',2,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1203,'客户服务部','SZ_OP_003',3,'销售',101,1200,'0,1200',3,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1300,'人力行政中心','SZ_HR_000',2,'人事',101,0,'0',4,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1301,'人力资源部','SZ_HR_001',3,'人事',101,1300,'0,1300',1,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1302,'行政部','SZ_HR_002',3,'行政',101,1300,'0,1300',2,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1400,'财务部','SZ_FIN_001',3,'财务',101,0,'0',5,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1500,'法务合规部','SZ_LAW_001',3,'管理',101,0,'0',6,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1600,'战略发展部','SZ_STR_001',3,'管理',101,0,'0',7,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1700,'供应链管理部','SZ_SCM_001',3,'采购',101,0,'0',8,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2000,'综合管理部','BJ_MGT_001',3,'管理',102,0,'0',1,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2100,'研发部','BJ_RD_001',3,'研发',102,0,'0',2,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2101,'开发一组','BJ_RD_002',4,'研发',102,2100,'0,2100',1,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2102,'开发二组','BJ_RD_003',4,'研发',102,2100,'0,2100',2,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2200,'市场部','BJ_MKT_001',3,'销售',102,0,'0',3,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2300,'财务部','BJ_FIN_001',3,'财务',102,0,'0',4,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_dept` (`id`, `dept_name`, `dept_code`, `dept_type`, `func_type`, `org_id`, `parent_id`, `ancestors`, `sort`, `leader`, `phone`, `email`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2400,'人事行政部','BJ_HRA_001',3,'人事',102,0,'0',5,NULL,NULL,NULL,1,1,'2026-06-16 08:03:44',NULL,NULL,0,1);

-- sys_post（8 行）
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1,'CEO','董事长',NULL,'管理类','M5',1,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (2,'GM','总经理',NULL,'管理类','M4',2,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (3,'PM','项目经理',NULL,'管理类','M2',3,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (4,'SE','高级工程师',NULL,'技术类','P7',4,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (5,'DEV','开发工程师',NULL,'技术类','P5',5,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (6,'FIN','财务主管',NULL,'财务类','M2',6,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (7,'HR','人事专员',NULL,'行政类','P4',7,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);
INSERT INTO `sys_post` (`id`, `post_code`, `post_name`, `org_id`, `post_category`, `post_level`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (8,'SALE','销售代表',NULL,'销售类','P4',8,1,NULL,1,'2026-06-16 08:03:44',NULL,NULL,0,1);

-- -----------------------------------------------------------------------------
-- 基础数据：超级管理员账号与角色
-- -----------------------------------------------------------------------------

-- sys_user（1 行）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `nickname`, `email`, `phone`, `avatar`, `gender`, `org_id`, `dept_id`, `post_id`, `superior_id`, `employee_no`, `entry_date`, `status`, `login_ip`, `login_date`, `pwd_update_time`, `pwd_error_count`, `lock_time`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1,'admin','$2a$10$nL/PGqiEg.ntH.eG4Xf9n.uUl0/5Rhsp.f7MZPE0Euuum2AaD9MMK','超级管理员','超级管理员',NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,0,NULL,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0,1);

-- sys_role（1 行）
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `data_scope`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `version`) VALUES (1,'超级管理员','admin',1,1,1,'超级管理员拥有所有权限，不受数据范围限制',NULL,'2026-06-16 08:03:44',NULL,NULL,0,1);

-- sys_user_role（1 行）
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1,1);

-- -----------------------------------------------------------------------------
-- 基础数据：菜单与角色授权（前端菜单 / 按钮权限）
-- -----------------------------------------------------------------------------

-- sys_menu（285 行）
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1,'工作台',0,'C','dashboard','dashboard/Workplace','monitor:server:list','dashboard',1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (2,'资产目录',0,'C','api/list','api/ApiList','system:apiList:list','layers',4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (3,'授权治理',0,'C','api/account','api/ApiAccountManage','api:account:list','safe',5,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4,'系统管理',0,'M','system',NULL,NULL,'settings',6,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5,'系统监控',0,'M','monitor',NULL,NULL,'dashboard',7,0,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (31,'凭证查询',3,'B',NULL,NULL,'api:account:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (32,'凭证签发',3,'B',NULL,NULL,'api:account:add',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (33,'凭证编辑',3,'B',NULL,NULL,'api:account:edit',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (34,'凭证注销',3,'B',NULL,NULL,'api:account:remove',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (35,'密钥重置',3,'B',NULL,NULL,'api:account:reset',NULL,5,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (36,'策略配置',3,'B',NULL,NULL,'api:account:authorize',NULL,6,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (37,'资产全量',3,'B',NULL,NULL,'api:account:sync',NULL,7,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (41,'用户管理',4,'C','user','system/user/index','system:user:list','user',4,0,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (42,'角色与权限',4,'C','role','system/role/index','system:role:list','idcard',5,0,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (44,'部门管理',4,'C','dept','system/dept/index','system:dept:list','branch',2,0,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (45,'机构管理',4,'C','org','system/org/index','system:org:list','apps',1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (46,'岗位管理',4,'C','post','system/post/index','system:post:list','bookmark',3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (47,'操作日志',71,'C','operlog','system/operlog/index','system:operlog:list','history',3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (48,'审计总览',71,'C','audit/overview','system/audit/overview/index','system:audit:list','dashboard',1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (49,'登录日志',71,'C','loginlog','system/loginlog/index','system:loginlog:list','safe',2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (50,'异常日志',71,'C','exceptionlog','system/exceptionlog/index','system:exceptionlog:list','bug',4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (51,'风险告警',71,'C','security-alert','system/security-alert/index','system:securityalert:list','notification',5,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (52,'归档策略',71,'C','audit-setting','system/audit-setting/index','system:auditsetting:list','settings',7,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (53,'菜单管理',60,'C','menu','system/menu/index','system:menu:list','list',1,1,1,0,'业务系统菜单、按钮、字段权限元数据维护入口',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (60,'基础配置',4,'M','base-config',NULL,NULL,'settings',2,1,1,0,'业务系统基础配置、权限元数据和文件资源目录',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (61,'字典管理',60,'C','dict','system/dict/index','system:dict:list','book',2,1,1,0,'维护系统基础枚举、状态选项与业务下拉数据',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (62,'参数配置',60,'C','config','system/config/index','system:config:list','tool',3,1,1,0,'维护运行时参数、登录策略和业务开关',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (63,'系统设置',60,'C','branding','system/branding/index','system:setting:list','palette',4,1,1,0,'业务侧品牌文案、登录页图片与侧边栏 Logo 配置',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (64,'文件管理',60,'C','file','system/file/index','system:file:list','file',5,1,1,0,'系统文件上传、下载与归档管理',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (65,'通知公告',70,'C','notice','system/notice/index','system:notice:list','notification',1,1,1,0,'业务系统通知公告发布与维护',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (66,'在线用户',71,'C','online','system/online/index','system:online:list','user',6,1,1,0,'查看在线用户并支持强制下线',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (67,'缓存管理',72,'C','cache','system/cache/index','system:cache:list','storage',2,1,1,0,'Redis 缓存概览、键查询与清理',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (68,'定时任务',72,'C','job','system/job/index','system:job:list','history',3,1,1,0,'定时任务配置、启停与手动执行',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (69,'任务日志',72,'C','job-log','system/job-log/index','system:joblog:list','file',4,1,1,0,'定时任务执行日志查询与清理',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (70,'消息中心',4,'M','message-center',NULL,NULL,'notification',3,1,1,0,'业务系统通知公告与消息触达目录',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (71,'安全审计',4,'M','security-audit',NULL,NULL,'history',4,1,1,0,'业务系统安全审计、日志、告警和在线会话目录',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (72,'运维管理',4,'M','ops-management',NULL,NULL,'storage',5,1,1,0,'业务系统运行监控、缓存和任务调度目录',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (73,'系统监控',72,'C','monitor','monitor/server/index','monitor:server:list','desktop',1,1,1,0,'业务侧服务器 CPU、内存、磁盘、网络和 JVM 运行监控',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (411,'用户查询',41,'B',NULL,NULL,'system:user:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (412,'新增用户',41,'B',NULL,NULL,'system:user:add',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (413,'编辑用户',41,'B',NULL,NULL,'system:user:edit',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (414,'删除用户',41,'B',NULL,NULL,'system:user:remove',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (415,'用户导出',41,'B',NULL,NULL,'system:user:reset',NULL,5,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (416,'手机号码',41,'F',NULL,NULL,'system:user:field:phone',NULL,10,1,1,0,'用户管理 — 账号信息 — 手机号字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (417,'邮箱地址',41,'F',NULL,NULL,'system:user:field:email',NULL,11,1,1,0,'用户管理 — 账号信息 — 邮箱字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (419,'功能授权',41,'B',NULL,NULL,'system:user:assignPerms',NULL,6,1,1,0,'用户管理 — 为单个用户分配独立的菜单/按钮/字段权限',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (421,'角色查询',42,'B',NULL,NULL,'system:role:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (422,'新增角色',42,'B',NULL,NULL,'system:role:add',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (423,'编辑角色',42,'B',NULL,NULL,'system:role:edit',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (424,'删除角色',42,'B',NULL,NULL,'system:role:remove',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (426,'备注信息',42,'F',NULL,NULL,'system:role:field:remark',NULL,11,1,1,0,'角色与权限 — 基本信息 — 备注字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (441,'部门查询',44,'B',NULL,NULL,'system:dept:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (442,'新增部门',44,'B',NULL,NULL,'system:dept:add',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (443,'编辑部门',44,'B',NULL,NULL,'system:dept:edit',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (444,'删除部门',44,'B',NULL,NULL,'system:dept:remove',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (445,'负责人',44,'F',NULL,NULL,'system:dept:field:leader',NULL,10,1,1,0,'部门管理 — 联系信息 — 负责人字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (446,'联系电话',44,'F',NULL,NULL,'system:dept:field:phone',NULL,11,1,1,0,'部门管理 — 联系信息 — 联系电话字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (447,'邮箱地址',44,'F',NULL,NULL,'system:dept:field:email',NULL,12,1,1,0,'部门管理 — 联系信息 — 邮箱字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (451,'机构查询',45,'B',NULL,NULL,'system:org:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (452,'新增机构',45,'B',NULL,NULL,'system:org:add',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (453,'编辑机构',45,'B',NULL,NULL,'system:org:edit',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (454,'删除机构',45,'B',NULL,NULL,'system:org:remove',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (455,'信用代码',45,'F',NULL,NULL,'system:org:field:creditCode',NULL,10,1,1,0,'机构管理 — 工商信息 — 信用代码字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (456,'法定代表人',45,'F',NULL,NULL,'system:org:field:legalPerson',NULL,11,1,1,0,'机构管理 — 工商信息 — 法定代表人字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (457,'注册资本',45,'F',NULL,NULL,'system:org:field:registeredCapital',NULL,12,1,1,0,'机构管理 — 工商信息 — 注册资本字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (458,'联系电话',45,'F',NULL,NULL,'system:org:field:phone',NULL,13,1,1,0,'机构管理 — 联系与地址 — 联系电话字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (459,'邮箱地址',45,'F',NULL,NULL,'system:org:field:email',NULL,14,1,1,0,'机构管理 — 联系与地址 — 邮箱地址字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (461,'岗位查询',46,'B',NULL,NULL,'system:post:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (462,'新增岗位',46,'B',NULL,NULL,'system:post:add',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (463,'编辑岗位',46,'B',NULL,NULL,'system:post:edit',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (464,'删除岗位',46,'B',NULL,NULL,'system:post:remove',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (465,'岗位类别',46,'F',NULL,NULL,'system:post:field:postCategory',NULL,10,1,1,0,'岗位管理 — 基本信息 — 岗位类别字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (466,'岗位级别',46,'F',NULL,NULL,'system:post:field:postLevel',NULL,11,1,1,0,'岗位管理 — 基本信息 — 岗位级别字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (467,'备注信息',46,'F',NULL,NULL,'system:post:field:remark',NULL,12,1,1,0,'岗位管理 — 其他信息 — 备注字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (471,'查询操作日志',47,'B',NULL,NULL,'system:operlog:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (472,'删除操作日志',47,'B',NULL,NULL,'system:operlog:remove',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (473,'清空操作日志',47,'B',NULL,NULL,'system:operlog:clean',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (511,'查询系统监控',73,'B',NULL,NULL,'monitor:server:list',NULL,1,1,1,0,'系统监控 — 查询服务器运行指标',NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (512,'触发内存回收',73,'B',NULL,NULL,'monitor:server:gc',NULL,2,1,1,0,'系统监控 — 手动触发 JVM GC',NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (521,'告警查询',52,'B',NULL,NULL,'monitor:alert:list',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (522,'告警处理',52,'B',NULL,NULL,'monitor:alert:edit',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (523,'告警删除',52,'B',NULL,NULL,'monitor:alert:remove',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4001,'新增子机构',45,'B',NULL,NULL,'system:org:addChild',NULL,5,1,1,0,'机构管理 — 新增子机构按钮',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4002,'数据共享配置',45,'B',NULL,NULL,'system:org:share',NULL,6,1,1,0,'机构管理 — 数据共享配置按钮',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4003,'新增子部门',44,'B',NULL,NULL,'system:dept:addChild',NULL,5,1,1,0,'部门管理 — 新增子部门按钮',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4004,'功能授权',42,'B',NULL,NULL,'system:role:assign',NULL,5,1,1,0,'角色与权限 — 功能授权按钮（工具栏）',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4006,'个人数据权限',41,'B',NULL,NULL,'system:user:dataScope',NULL,6,1,1,0,'用户管理 — 个人数据权限配置按钮',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4007,'绑定用户',42,'B',NULL,NULL,'system:role:assignUser',NULL,6,1,1,0,'角色与权限 — 绑定用户按钮（工具栏）',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4704,'导出操作日志',47,'B',NULL,NULL,'system:operlog:export',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4711,'操作模块',47,'F',NULL,NULL,'system:operlog:field:title',NULL,11,1,1,0,'操作日志 — 操作模块字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4712,'业务类型',47,'F',NULL,NULL,'system:operlog:field:businessType',NULL,12,1,1,0,'操作日志 — 业务类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4713,'操作人员',47,'F',NULL,NULL,'system:operlog:field:operName',NULL,13,1,1,0,'操作日志 — 操作人员字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4714,'请求方式',47,'F',NULL,NULL,'system:operlog:field:requestMethod',NULL,14,1,1,0,'操作日志 — 请求方式字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4715,'操作IP',47,'F',NULL,NULL,'system:operlog:field:operIp',NULL,15,1,1,0,'操作日志 — 操作IP字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4716,'操作状态',47,'F',NULL,NULL,'system:operlog:field:status',NULL,16,1,1,0,'操作日志 — 操作状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4717,'操作耗时',47,'F',NULL,NULL,'system:operlog:field:costTime',NULL,17,1,1,0,'操作日志 — 操作耗时字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4801,'查看审计总览',48,'B',NULL,NULL,'system:audit:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4811,'操作总数',48,'F',NULL,NULL,'system:audit:field:operationTotal',NULL,11,1,1,0,'审计总览 — 操作总数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4812,'异常总数',48,'F',NULL,NULL,'system:audit:field:operationErrorTotal',NULL,12,1,1,0,'审计总览 — 异常总数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4813,'登录总数',48,'F',NULL,NULL,'system:audit:field:loginTotal',NULL,13,1,1,0,'审计总览 — 登录总数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4814,'告警总数',48,'F',NULL,NULL,'system:audit:field:alertTotal',NULL,14,1,1,0,'审计总览 — 告警总数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4901,'查询登录日志',49,'B',NULL,NULL,'system:loginlog:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4902,'导出登录日志',49,'B',NULL,NULL,'system:loginlog:export',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4903,'删除登录日志',49,'B',NULL,NULL,'system:loginlog:remove',NULL,3,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4904,'清空登录日志',49,'B',NULL,NULL,'system:loginlog:clean',NULL,4,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4911,'用户账号',49,'F',NULL,NULL,'system:loginlog:field:username',NULL,11,1,1,0,'登录日志 — 用户账号字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4912,'登录IP',49,'F',NULL,NULL,'system:loginlog:field:ipaddr',NULL,12,1,1,0,'登录日志 — 登录IP字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4913,'登录地点',49,'F',NULL,NULL,'system:loginlog:field:loginLocation',NULL,13,1,1,0,'登录日志 — 登录地点字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4914,'浏览器',49,'F',NULL,NULL,'system:loginlog:field:browser',NULL,14,1,1,0,'登录日志 — 浏览器字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4915,'操作系统',49,'F',NULL,NULL,'system:loginlog:field:os',NULL,15,1,1,0,'登录日志 — 操作系统字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4916,'登录状态',49,'F',NULL,NULL,'system:loginlog:field:status',NULL,16,1,1,0,'登录日志 — 登录状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (4917,'登录信息',49,'F',NULL,NULL,'system:loginlog:field:msg',NULL,17,1,1,0,'登录日志 — 登录信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5001,'成立日期',45,'F',NULL,NULL,'system:org:field:establishDate',NULL,15,1,1,0,'机构管理 — 工商信息 — 成立日期字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5002,'经营范围',45,'F',NULL,NULL,'system:org:field:businessScope',NULL,16,1,1,0,'机构管理 — 工商信息 — 经营范围字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5003,'省份',45,'F',NULL,NULL,'system:org:field:province',NULL,17,1,1,0,'机构管理 — 联系与地址 — 省份字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5004,'城市',45,'F',NULL,NULL,'system:org:field:city',NULL,18,1,1,0,'机构管理 — 联系与地址 — 城市字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5005,'区县',45,'F',NULL,NULL,'system:org:field:district',NULL,19,1,1,0,'机构管理 — 联系与地址 — 区县字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5006,'详细地址',45,'F',NULL,NULL,'system:org:field:address',NULL,20,1,1,0,'机构管理 — 联系与地址 — 详细地址字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5011,'所属机构',44,'F',NULL,NULL,'system:dept:field:orgId',NULL,13,1,1,0,'部门管理 — 基本信息标签页 — 所属机构字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5012,'部门类型',44,'F',NULL,NULL,'system:dept:field:deptType',NULL,14,1,1,0,'部门管理 — 基本信息标签页 — 部门类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5013,'职能分类',44,'F',NULL,NULL,'system:dept:field:funcType',NULL,15,1,1,0,'部门管理 — 基本信息标签页 — 职能分类字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5021,'所属机构',46,'F',NULL,NULL,'system:post:field:orgId',NULL,13,1,1,0,'岗位管理 — 基本信息 — 所属机构字段',1,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:45',0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5031,'性别',41,'F',NULL,NULL,'system:user:field:gender',NULL,13,1,1,0,'用户管理 — 账号信息标签页 — 性别字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5032,'角色',41,'F',NULL,NULL,'system:user:field:roleIds',NULL,14,1,1,0,'用户管理 — 账号信息标签页 — 角色字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5033,'所属机构',41,'F',NULL,NULL,'system:user:field:orgId',NULL,15,1,1,0,'用户管理 — 组织与岗位标签页 — 所属机构字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5034,'所属部门',41,'F',NULL,NULL,'system:user:field:deptId',NULL,16,1,1,0,'用户管理 — 组织与岗位标签页 — 所属部门字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5035,'岗位',41,'F',NULL,NULL,'system:user:field:postId',NULL,17,1,1,0,'用户管理 — 组织与岗位标签页 — 岗位字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5036,'工号',41,'F',NULL,NULL,'system:user:field:employeeNo',NULL,18,1,1,0,'用户管理 — 组织与岗位标签页 — 工号字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5037,'直属上级',41,'F',NULL,NULL,'system:user:field:superiorId',NULL,19,1,1,0,'用户管理 — 组织与岗位标签页 — 直属上级字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5038,'入职日期',41,'F',NULL,NULL,'system:user:field:entryDate',NULL,20,1,1,0,'用户管理 — 组织与岗位标签页 — 入职日期字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5053,'上级菜单',43,'F',NULL,NULL,'system:menu:field:parentId',NULL,1,1,1,0,'菜单管理 — 上级菜单字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5054,'菜单类型',43,'F',NULL,NULL,'system:menu:field:menuType',NULL,2,1,1,0,'菜单管理 — 菜单类型字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5055,'菜单名称',43,'F',NULL,NULL,'system:menu:field:menuName',NULL,3,1,1,0,'菜单管理 — 菜单名称字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5056,'显示排序',43,'F',NULL,NULL,'system:menu:field:sort',NULL,4,1,1,0,'菜单管理 — 显示排序字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5057,'是否显示',43,'F',NULL,NULL,'system:menu:field:visible',NULL,15,1,1,0,'菜单管理 — 是否显示字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5058,'状态',43,'F',NULL,NULL,'system:menu:field:status',NULL,16,1,1,0,'菜单管理 — 状态字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5059,'备注',43,'F',NULL,NULL,'system:menu:field:remark',NULL,17,1,1,0,'菜单管理 — 备注字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5061,'操作模块',50,'F',NULL,NULL,'system:exceptionlog:field:title',NULL,11,1,1,0,'异常日志 — 操作模块字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5062,'异常信息',50,'F',NULL,NULL,'system:exceptionlog:field:errorMsg',NULL,12,1,1,0,'异常日志 — 异常信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5063,'请求地址',50,'F',NULL,NULL,'system:exceptionlog:field:operUrl',NULL,13,1,1,0,'异常日志 — 请求地址字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5064,'操作人员',50,'F',NULL,NULL,'system:exceptionlog:field:operName',NULL,14,1,1,0,'异常日志 — 操作人员字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5065,'操作时间',50,'F',NULL,NULL,'system:exceptionlog:field:operTime',NULL,15,1,1,0,'异常日志 — 操作时间字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5101,'查询风险告警',51,'B',NULL,NULL,'system:securityalert:query',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5102,'处理风险告警',51,'B',NULL,NULL,'system:securityalert:edit',NULL,2,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5111,'告警类型',51,'F',NULL,NULL,'system:securityalert:field:alertType',NULL,11,1,1,0,'风险告警 — 告警类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5112,'告警级别',51,'F',NULL,NULL,'system:securityalert:field:alertLevel',NULL,12,1,1,0,'风险告警 — 告警级别字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5113,'告警标题',51,'F',NULL,NULL,'system:securityalert:field:alertTitle',NULL,13,1,1,0,'风险告警 — 告警标题字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5114,'告警内容',51,'F',NULL,NULL,'system:securityalert:field:alertContent',NULL,14,1,1,0,'风险告警 — 告警内容字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5115,'处理状态',51,'F',NULL,NULL,'system:securityalert:field:readStatus',NULL,15,1,1,0,'风险告警 — 处理状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5201,'编辑归档策略',52,'B',NULL,NULL,'system:auditsetting:edit',NULL,1,1,1,0,NULL,NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5211,'在线保留天数',52,'F',NULL,NULL,'system:auditsetting:field:onlineRetentionDays',NULL,11,1,1,0,'归档策略 — 在线保留天数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5212,'归档存储',52,'F',NULL,NULL,'system:auditsetting:field:archiveStorage',NULL,12,1,1,0,'归档策略 — 归档存储字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5213,'自动清理',52,'F',NULL,NULL,'system:auditsetting:field:autoCleanEnabled',NULL,13,1,1,0,'归档策略 — 自动清理字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5214,'自动归档',52,'F',NULL,NULL,'system:auditsetting:field:archiveEnabled',NULL,14,1,1,0,'归档策略 — 自动归档开关',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5301,'查询菜单',53,'B',NULL,NULL,'system:menu:query',NULL,1,1,1,0,'菜单管理 — 查询菜单详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5302,'新增菜单',53,'B',NULL,NULL,'system:menu:add',NULL,2,1,1,0,'菜单管理 — 新增目录、菜单、按钮或字段权限',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5303,'编辑菜单',53,'B',NULL,NULL,'system:menu:edit',NULL,3,1,1,0,'菜单管理 — 编辑目录、菜单、按钮或字段权限',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5304,'删除菜单',53,'B',NULL,NULL,'system:menu:remove',NULL,4,1,1,0,'菜单管理 — 删除目录、菜单、按钮或字段权限',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5305,'新增下级',53,'B',NULL,NULL,'system:menu:addChild',NULL,5,1,1,0,'菜单管理 — 在指定节点下新增子权限项',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5311,'父级菜单',53,'F',NULL,NULL,'system:menu:field:parentId',NULL,11,1,1,0,'菜单管理 — 父级菜单字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5312,'菜单类型',53,'F',NULL,NULL,'system:menu:field:menuType',NULL,12,1,1,0,'菜单管理 — 菜单类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5313,'路由地址',53,'F',NULL,NULL,'system:menu:field:path',NULL,13,1,1,0,'菜单管理 — 路由地址字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5314,'组件路径',53,'F',NULL,NULL,'system:menu:field:component',NULL,14,1,1,0,'菜单管理 — 组件路径字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5315,'权限标识',53,'F',NULL,NULL,'system:menu:field:perms',NULL,15,1,1,0,'菜单管理 — 权限标识字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5316,'菜单图标',53,'F',NULL,NULL,'system:menu:field:icon',NULL,16,1,1,0,'菜单管理 — 菜单图标字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5317,'显示状态',53,'F',NULL,NULL,'system:menu:field:visible',NULL,17,1,1,0,'菜单管理 — 显示状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (5318,'启用状态',53,'F',NULL,NULL,'system:menu:field:status',NULL,18,1,1,0,'菜单管理 — 启用状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6001,'上级机构',45,'F',NULL,NULL,'system:org:field:parentId',NULL,1,1,1,0,'机构管理 — 基本信息 — 上级机构字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6002,'机构名称',45,'F',NULL,NULL,'system:org:field:orgName',NULL,2,1,1,0,'机构管理 — 基本信息 — 机构名称字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6003,'机构编码',45,'F',NULL,NULL,'system:org:field:orgCode',NULL,3,1,1,0,'机构管理 — 基本信息 — 机构编码字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6004,'机构类型',45,'F',NULL,NULL,'system:org:field:orgType',NULL,4,1,1,0,'机构管理 — 基本信息 — 机构类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6005,'数据隔离模式',45,'F',NULL,NULL,'system:org:field:dataIsolation',NULL,5,1,1,0,'机构管理 — 基本信息 — 数据隔离模式字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6006,'负责人',45,'F',NULL,NULL,'system:org:field:leader',NULL,6,1,1,0,'机构管理 — 基本信息 — 负责人字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6007,'排序',45,'F',NULL,NULL,'system:org:field:sort',NULL,7,1,1,0,'机构管理 — 基本信息 — 排序字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6008,'状态',45,'F',NULL,NULL,'system:org:field:status',NULL,8,1,1,0,'机构管理 — 基本信息 — 状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6009,'备注',45,'F',NULL,NULL,'system:org:field:remark',NULL,9,1,1,0,'机构管理 — 基本信息 — 备注字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6101,'查询字典',61,'B',NULL,NULL,'system:dict:query',NULL,1,1,1,0,'字典管理 — 查询字典详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6102,'新增字典',61,'B',NULL,NULL,'system:dict:add',NULL,2,1,1,0,'字典管理 — 新增字典类型或字典数据',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6103,'编辑字典',61,'B',NULL,NULL,'system:dict:edit',NULL,3,1,1,0,'字典管理 — 编辑字典类型或字典数据',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6104,'删除字典',61,'B',NULL,NULL,'system:dict:remove',NULL,4,1,1,0,'字典管理 — 删除字典类型或字典数据',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6105,'刷新字典',61,'B',NULL,NULL,'system:dict:refresh',NULL,5,1,1,0,'字典管理 — 刷新前端字典缓存',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6111,'字典名称',61,'F',NULL,NULL,'system:dict:field:dictName',NULL,11,1,1,0,'字典管理 — 字典名称字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6112,'字典编码',61,'F',NULL,NULL,'system:dict:field:dictType',NULL,12,1,1,0,'字典管理 — 字典编码字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6113,'字典标签',61,'F',NULL,NULL,'system:dict:field:dictLabel',NULL,13,1,1,0,'字典管理 — 字典标签字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6114,'字典键值',61,'F',NULL,NULL,'system:dict:field:dictValue',NULL,14,1,1,0,'字典管理 — 字典键值字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6115,'样式类名',61,'F',NULL,NULL,'system:dict:field:cssClass',NULL,15,1,1,0,'字典管理 — 标签样式字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6116,'显示排序',61,'F',NULL,NULL,'system:dict:field:sort',NULL,16,1,1,0,'字典管理 — 显示排序字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6117,'启用状态',61,'F',NULL,NULL,'system:dict:field:status',NULL,17,1,1,0,'字典管理 — 启用状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6118,'备注信息',61,'F',NULL,NULL,'system:dict:field:remark',NULL,18,1,1,0,'字典管理 — 备注信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6201,'查询参数',62,'B',NULL,NULL,'system:config:query',NULL,1,1,1,0,'参数配置 — 查询参数详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6202,'新增参数',62,'B',NULL,NULL,'system:config:add',NULL,2,1,1,0,'参数配置 — 新增参数配置',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6203,'编辑参数',62,'B',NULL,NULL,'system:config:edit',NULL,3,1,1,0,'参数配置 — 编辑参数配置',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6204,'删除参数',62,'B',NULL,NULL,'system:config:remove',NULL,4,1,1,0,'参数配置 — 删除非内置参数配置',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6211,'参数名称',62,'F',NULL,NULL,'system:config:field:configName',NULL,11,1,1,0,'参数配置 — 参数名称字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6212,'参数键名',62,'F',NULL,NULL,'system:config:field:configKey',NULL,12,1,1,0,'参数配置 — 参数键名字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6213,'参数键值',62,'F',NULL,NULL,'system:config:field:configValue',NULL,13,1,1,0,'参数配置 — 参数键值字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6214,'参数类型',62,'F',NULL,NULL,'system:config:field:configType',NULL,14,1,1,0,'参数配置 — 参数类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6215,'备注信息',62,'F',NULL,NULL,'system:config:field:remark',NULL,15,1,1,0,'参数配置 — 备注信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6301,'保存品牌设置',63,'B',NULL,NULL,'system:setting:edit',NULL,1,1,1,0,'品牌设置 — 保存品牌配置',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6302,'上传品牌图片',63,'B',NULL,NULL,'system:setting:upload',NULL,2,1,1,0,'品牌设置 — 上传品牌图片',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6303,'删除品牌图片',63,'B',NULL,NULL,'system:setting:remove',NULL,3,1,1,0,'品牌设置 — 删除品牌图片',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6304,'备注',41,'F',NULL,NULL,'system:user:field:remark',NULL,4,1,1,0,'用户管理 — 账号信息 — 备注字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6311,'图形验证码',63,'F',NULL,NULL,'system:setting:field:captchaEnabled',NULL,11,1,1,0,'品牌设置 — 登录策略 — 图形验证码开关',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6312,'空闲自动登出',63,'F',NULL,NULL,'system:setting:field:idleTimeout',NULL,12,1,1,0,'品牌设置 — 登录策略 — 空闲自动登出分钟数',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6313,'品牌标题',63,'F',NULL,NULL,'system:setting:field:brandTitle',NULL,13,1,1,0,'品牌设置 — 品牌文案 — 品牌标题',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6314,'品牌副标题',63,'F',NULL,NULL,'system:setting:field:brandSlogan',NULL,14,1,1,0,'品牌设置 — 品牌文案 — 品牌副标题',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6315,'品牌描述',63,'F',NULL,NULL,'system:setting:field:brandDesc',NULL,15,1,1,0,'品牌设置 — 品牌文案 — 品牌描述',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6316,'登录页背景图',63,'F',NULL,NULL,'system:setting:field:loginBg',NULL,16,1,1,0,'品牌设置 — 图片 — 登录页背景图',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6317,'侧边栏Logo',63,'F',NULL,NULL,'system:setting:field:sidebarLogo',NULL,17,1,1,0,'品牌设置 — 图片 — 侧边栏Logo',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6318,'浏览器图标',63,'F',NULL,NULL,'system:setting:field:favicon',NULL,18,1,1,0,'品牌设置 — 图片 — 浏览器图标',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6401,'查询文件',64,'B',NULL,NULL,'system:file:query',NULL,1,1,1,0,'文件管理 — 查询文件详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6402,'上传文件',64,'B',NULL,NULL,'system:file:upload',NULL,2,1,1,0,'文件管理 — 上传文件',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6403,'下载文件',64,'B',NULL,NULL,'system:file:download',NULL,3,1,1,0,'文件管理 — 下载文件',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6404,'删除文件',64,'B',NULL,NULL,'system:file:remove',NULL,4,1,1,0,'文件管理 — 删除文件',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6411,'文件名称',64,'F',NULL,NULL,'system:file:field:originalName',NULL,11,1,1,0,'文件管理 — 文件名称字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6412,'文件类型',64,'F',NULL,NULL,'system:file:field:fileExt',NULL,12,1,1,0,'文件管理 — 文件类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6413,'文件大小',64,'F',NULL,NULL,'system:file:field:fileSize',NULL,13,1,1,0,'文件管理 — 文件大小字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6414,'存储类型',64,'F',NULL,NULL,'system:file:field:storageType',NULL,14,1,1,0,'文件管理 — 存储类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6415,'备注信息',64,'F',NULL,NULL,'system:file:field:remark',NULL,15,1,1,0,'文件管理 — 备注信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6501,'查询公告',65,'B',NULL,NULL,'system:notice:query',NULL,1,1,1,0,'通知公告 — 查询公告详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6502,'新增公告',65,'B',NULL,NULL,'system:notice:add',NULL,2,1,1,0,'通知公告 — 新增公告',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6503,'编辑公告',65,'B',NULL,NULL,'system:notice:edit',NULL,3,1,1,0,'通知公告 — 编辑公告',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6504,'删除公告',65,'B',NULL,NULL,'system:notice:remove',NULL,4,1,1,0,'通知公告 — 删除公告',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6505,'发布公告',65,'B',NULL,NULL,'system:notice:publish',NULL,5,1,1,0,'通知公告 — 发布或撤回公告',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6511,'公告标题',65,'F',NULL,NULL,'system:notice:field:noticeTitle',NULL,11,1,1,0,'通知公告 — 公告标题字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6512,'公告类型',65,'F',NULL,NULL,'system:notice:field:noticeType',NULL,12,1,1,0,'通知公告 — 公告类型字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6513,'公告内容',65,'F',NULL,NULL,'system:notice:field:noticeContent',NULL,13,1,1,0,'通知公告 — 公告内容字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6514,'发布状态',65,'F',NULL,NULL,'system:notice:field:status',NULL,14,1,1,0,'通知公告 — 发布状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6515,'备注信息',65,'F',NULL,NULL,'system:notice:field:remark',NULL,15,1,1,0,'通知公告 — 备注信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6601,'查询在线用户',66,'B',NULL,NULL,'system:online:query',NULL,1,1,1,0,'在线用户 — 查询用户会话',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6602,'强制下线',66,'B',NULL,NULL,'system:online:forceLogout',NULL,2,1,1,0,'在线用户 — 强制用户下线',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6611,'用户账号',66,'F',NULL,NULL,'system:online:field:username',NULL,11,1,1,0,'在线用户 — 用户账号字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6612,'登录时间',66,'F',NULL,NULL,'system:online:field:loginTime',NULL,12,1,1,0,'在线用户 — 登录时间字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6613,'过期时间',66,'F',NULL,NULL,'system:online:field:expireTime',NULL,13,1,1,0,'在线用户 — 过期时间字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6701,'查询缓存',67,'B',NULL,NULL,'system:cache:query',NULL,1,1,1,0,'缓存管理 — 查询缓存详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6702,'删除缓存',67,'B',NULL,NULL,'system:cache:remove',NULL,2,1,1,0,'缓存管理 — 删除缓存键',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6703,'清理缓存',67,'B',NULL,NULL,'system:cache:clear',NULL,3,1,1,0,'缓存管理 — 按前缀清理缓存',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6711,'Redis版本',67,'F',NULL,NULL,'system:cache:field:redisVersion',NULL,11,1,1,0,'缓存管理 — Redis版本字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6712,'已用内存',67,'F',NULL,NULL,'system:cache:field:usedMemory',NULL,12,1,1,0,'缓存管理 — 已用内存字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6713,'连接数',67,'F',NULL,NULL,'system:cache:field:connectedClients',NULL,13,1,1,0,'缓存管理 — 连接数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6714,'运行天数',67,'F',NULL,NULL,'system:cache:field:uptimeInDays',NULL,14,1,1,0,'缓存管理 — 运行天数字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6801,'查询任务',68,'B',NULL,NULL,'system:job:query',NULL,1,1,1,0,'定时任务 — 查询任务详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6802,'新增任务',68,'B',NULL,NULL,'system:job:add',NULL,2,1,1,0,'定时任务 — 新增任务',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6803,'编辑任务',68,'B',NULL,NULL,'system:job:edit',NULL,3,1,1,0,'定时任务 — 编辑任务',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6804,'删除任务',68,'B',NULL,NULL,'system:job:remove',NULL,4,1,1,0,'定时任务 — 删除任务',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6805,'运行任务',68,'B',NULL,NULL,'system:job:run',NULL,5,1,1,0,'定时任务 — 立即运行任务',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6806,'启停任务',68,'B',NULL,NULL,'system:job:changeStatus',NULL,6,1,1,0,'定时任务 — 启用或暂停任务',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6811,'任务名称',68,'F',NULL,NULL,'system:job:field:jobName',NULL,11,1,1,0,'定时任务 — 任务名称字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6812,'任务分组',68,'F',NULL,NULL,'system:job:field:jobGroup',NULL,12,1,1,0,'定时任务 — 任务分组字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6813,'调用目标',68,'F',NULL,NULL,'system:job:field:invokeTarget',NULL,13,1,1,0,'定时任务 — 调用目标字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6814,'Cron表达式',68,'F',NULL,NULL,'system:job:field:cronExpression',NULL,14,1,1,0,'定时任务 — Cron表达式字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6815,'执行策略',68,'F',NULL,NULL,'system:job:field:misfirePolicy',NULL,15,1,1,0,'定时任务 — 执行策略字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6816,'是否并发',68,'F',NULL,NULL,'system:job:field:concurrent',NULL,16,1,1,0,'定时任务 — 是否并发字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6817,'任务状态',68,'F',NULL,NULL,'system:job:field:status',NULL,17,1,1,0,'定时任务 — 任务状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6818,'备注信息',68,'F',NULL,NULL,'system:job:field:remark',NULL,18,1,1,0,'定时任务 — 备注信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6901,'查询任务日志',69,'B',NULL,NULL,'system:joblog:query',NULL,1,1,1,0,'任务日志 — 查询任务日志详情',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6902,'清空任务日志',69,'B',NULL,NULL,'system:joblog:clean',NULL,2,1,1,0,'任务日志 — 清空任务日志',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6911,'任务名称',69,'F',NULL,NULL,'system:joblog:field:jobName',NULL,11,1,1,0,'任务日志 — 任务名称字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6912,'任务分组',69,'F',NULL,NULL,'system:joblog:field:jobGroup',NULL,12,1,1,0,'任务日志 — 任务分组字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6913,'调用目标',69,'F',NULL,NULL,'system:joblog:field:invokeTarget',NULL,13,1,1,0,'任务日志 — 调用目标字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6914,'执行状态',69,'F',NULL,NULL,'system:joblog:field:status',NULL,14,1,1,0,'任务日志 — 执行状态字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6915,'执行耗时',69,'F',NULL,NULL,'system:joblog:field:costTime',NULL,15,1,1,0,'任务日志 — 执行耗时字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (6916,'异常信息',69,'F',NULL,NULL,'system:joblog:field:exceptionInfo',NULL,16,1,1,0,'任务日志 — 异常信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7001,'角色名称',42,'F',NULL,NULL,'system:role:field:roleName',NULL,1,1,1,0,'角色与权限 — 基本信息 — 角色名称字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7002,'角色编码',42,'F',NULL,NULL,'system:role:field:roleCode',NULL,2,1,1,0,'角色与权限 — 基本信息 — 角色编码字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7003,'排序',42,'F',NULL,NULL,'system:role:field:sort',NULL,3,1,1,0,'角色与权限 — 基本信息 — 排序字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7004,'状态',42,'F',NULL,NULL,'system:role:field:status',NULL,4,1,1,0,'角色与权限 — 基本信息 — 状态字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7311,'CPU信息',73,'F',NULL,NULL,'monitor:server:field:cpu',NULL,11,1,1,0,'系统监控 — CPU信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7312,'内存信息',73,'F',NULL,NULL,'monitor:server:field:mem',NULL,12,1,1,0,'系统监控 — 内存信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7313,'JVM信息',73,'F',NULL,NULL,'monitor:server:field:jvm',NULL,13,1,1,0,'系统监控 — JVM信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7314,'磁盘信息',73,'F',NULL,NULL,'monitor:server:field:disk',NULL,14,1,1,0,'系统监控 — 磁盘信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7315,'网络信息',73,'F',NULL,NULL,'monitor:server:field:net',NULL,15,1,1,0,'系统监控 — 网络信息字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (7316,'系统环境',73,'F',NULL,NULL,'monitor:server:field:sys',NULL,16,1,1,0,'系统监控 — 系统环境字段',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8001,'岗位编码',46,'F',NULL,NULL,'system:post:field:postCode',NULL,1,1,1,0,'岗位管理 — 基本信息 — 岗位编码字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8002,'岗位名称',46,'F',NULL,NULL,'system:post:field:postName',NULL,2,1,1,0,'岗位管理 — 基本信息 — 岗位名称字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8003,'显示排序',46,'F',NULL,NULL,'system:post:field:sort',NULL,3,1,1,0,'岗位管理 — 基本信息 — 显示排序字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8004,'岗位状态',46,'F',NULL,NULL,'system:post:field:status',NULL,4,1,1,0,'岗位管理 — 基本信息 — 岗位状态字段',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8011,'图形验证码',63,'F',NULL,NULL,'system:setting:field:captchaEnabled',NULL,11,1,1,0,'品牌设置 — 登录策略 — 图形验证码开关',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8012,'空闲自动登出',63,'F',NULL,NULL,'system:setting:field:idleTimeout',NULL,12,1,1,0,'品牌设置 — 登录策略 — 空闲自动登出分钟数',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8013,'品牌标题',63,'F',NULL,NULL,'system:setting:field:brandTitle',NULL,13,1,1,0,'品牌设置 — 品牌文案 — 品牌标题',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8014,'品牌副标题',63,'F',NULL,NULL,'system:setting:field:brandSlogan',NULL,14,1,1,0,'品牌设置 — 品牌文案 — 品牌副标题',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8015,'品牌描述',63,'F',NULL,NULL,'system:setting:field:brandDesc',NULL,15,1,1,0,'品牌设置 — 品牌文案 — 品牌描述',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8016,'登录页背景图',63,'F',NULL,NULL,'system:setting:field:loginBg',NULL,16,1,1,0,'品牌设置 — 图片 — 登录页背景图',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8017,'侧边栏Logo',63,'F',NULL,NULL,'system:setting:field:sidebarLogo',NULL,17,1,1,0,'品牌设置 — 图片 — 侧边栏Logo',1,'2026-06-16 08:03:45',NULL,NULL,0);
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (8018,'浏览器图标',63,'F',NULL,NULL,'system:setting:field:favicon',NULL,18,1,1,0,'品牌设置 — 图片 — 浏览器图标',1,'2026-06-16 08:03:45',NULL,NULL,0);

-- sys_role_menu（280 行）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,1,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,2,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,3,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,31,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,32,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,33,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,34,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,35,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,36,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,37,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,41,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,42,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,44,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,45,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,46,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,47,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,48,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,49,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,50,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,51,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,52,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,53,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,60,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,61,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,62,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,63,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,64,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,65,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,66,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,67,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,68,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,69,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,70,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,71,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,72,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,73,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,411,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,412,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,413,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,414,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,415,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,416,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,417,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,419,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,421,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,422,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,423,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,424,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,426,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,441,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,442,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,443,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,444,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,445,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,446,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,447,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,451,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,452,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,453,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,454,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,455,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,456,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,457,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,458,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,459,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,461,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,462,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,463,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,464,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,465,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,466,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,467,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,471,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,472,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,473,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,511,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,512,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,521,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,522,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,523,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4001,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4002,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4003,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4004,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4006,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4704,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4711,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4712,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4713,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4714,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4715,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4716,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4717,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4801,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4811,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4812,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4813,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4814,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4901,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4902,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4903,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4904,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4911,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4912,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4913,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4914,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4915,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4916,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,4917,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5001,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5002,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5003,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5004,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5005,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5006,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5011,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5012,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5013,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5021,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5031,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5032,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5033,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5034,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5035,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5036,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5037,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5038,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5053,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5054,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5055,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5056,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5057,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5058,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5059,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5061,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5062,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5063,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5064,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5065,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5101,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5102,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5111,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5112,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5113,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5114,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5115,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5201,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5211,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5212,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5213,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5214,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5301,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5302,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5303,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5304,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5305,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5311,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5312,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5313,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5314,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5315,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5316,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5317,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,5318,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6001,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6002,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6003,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6004,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6005,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6006,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6007,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6008,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6009,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6101,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6102,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6103,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6104,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6105,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6111,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6112,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6113,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6114,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6115,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6116,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6117,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6118,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6201,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6202,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6203,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6204,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6211,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6212,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6213,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6214,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6215,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6301,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6302,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6303,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6304,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6311,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6312,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6313,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6314,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6315,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6316,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6317,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6318,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6411,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6412,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6413,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6414,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6415,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6501,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6502,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6503,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6504,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6505,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6511,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6512,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6513,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6514,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6515,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6601,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6602,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6611,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6612,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6613,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6701,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6702,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6703,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6711,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6712,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6713,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6714,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6801,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6802,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6803,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6804,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6805,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6806,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6811,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6812,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6813,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6814,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6815,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6816,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6817,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6818,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6901,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6902,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6911,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6912,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6913,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6914,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6915,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,6916,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7001,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7002,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7003,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7004,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7311,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7312,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7313,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7314,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7315,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,7316,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8001,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8002,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8003,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8004,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8011,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8012,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8013,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8014,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8015,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8016,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8017,0);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`) VALUES (1,8018,0);

-- -----------------------------------------------------------------------------
-- 基础数据：数据字典
-- -----------------------------------------------------------------------------

-- sys_dict_type（3 行）
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1201,'系统状态','sys_common_status',1,'通用启用/停用状态',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1202,'用户性别','sys_user_gender',1,'用户基础性别枚举',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1203,'菜单类型','sys_menu_type',1,'菜单目录、菜单、按钮、字段类型',1,'2026-06-16 08:03:44',NULL,NULL,0);

-- sys_dict_data（9 行）
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120101,'sys_common_status','正常','1','green',1,1,'通用正常状态',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120102,'sys_common_status','停用','0','orange',2,1,'通用停用状态',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120201,'sys_user_gender','未知','0','gray',1,1,'未知性别',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120202,'sys_user_gender','男','1','arcoblue',2,1,'男性',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120203,'sys_user_gender','女','2','magenta',3,1,'女性',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120301,'sys_menu_type','目录','M','arcoblue',1,1,'菜单目录',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120302,'sys_menu_type','菜单','C','green',2,1,'业务菜单',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120303,'sys_menu_type','按钮','B','orange',3,1,'按钮权限',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `css_class`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (120304,'sys_menu_type','字段','F','purple',4,1,'字段权限',1,'2026-06-16 08:03:44',NULL,NULL,0);

-- -----------------------------------------------------------------------------
-- 基础数据：系统参数配置
-- -----------------------------------------------------------------------------

-- sys_config（11 行）
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1001,'登录验证码开关','sys.login.captchaEnabled','false',1,'控制登录页是否启用验证码',NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1002,'登录页背景图','sys.login.bgImage','',1,'登录页全屏背景图片 URL，为空时使用前端内置默认渐变背景',NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1003,'登录框背景图','sys.login.cardBgImage','',1,'登录框背景图片 URL，为空时使用前端内置默认',NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1004,'浏览器标签图标','sys.login.favicon','',1,'浏览器标签 favicon URL，为空时使用前端内置默认',NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1005,'空闲超时分钟数','sys.login.idleTimeout','30',1,'业务系统用户空闲超时分钟数',NULL,'2026-06-16 08:03:44',NULL,'2026-06-16 08:03:44',0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1006,'登录页品牌标题','sys.login.brandTitle','BML 企业管理系统',1,'登录页品牌主标题',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1007,'侧边栏 Logo','sys.sidebar.logo','',1,'业务系统侧边栏 Logo 图片地址',1,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1101,'审计日志在线保留天数','sys.audit.onlineRetentionDays','180',1,'审计日志在线查询保留天数，默认180天',NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1102,'审计日志自动归档开关','sys.audit.archiveEnabled','false',1,'是否启用审计日志自动归档',NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1103,'审计日志归档存储位置','sys.audit.archiveStorage','local://data/audit-archive',1,'审计日志冷归档存储位置',NULL,'2026-06-16 08:03:44',NULL,NULL,0);
INSERT INTO `sys_config` (`id`, `config_name`, `config_key`, `config_value`, `config_type`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1104,'审计日志自动清理开关','sys.audit.autoCleanEnabled','false',1,'是否启用超过保留周期后的自动清理',NULL,'2026-06-16 08:03:44',NULL,NULL,0);

-- -----------------------------------------------------------------------------
-- 基础数据：内置定时任务
-- -----------------------------------------------------------------------------

-- sys_job（1 行）
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (2001,'服务器资源巡检','SYSTEM','serverAlertJob.checkServerMetrics','0 * * * * ?',1,0,0,'系统内置巡检任务，默认暂停；当前仍保留代码级 @Scheduled 巡检。',1,'2026-06-16 08:03:44',NULL,NULL,0);

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_MODE = @OLD_SQL_MODE;

-- =============================================================================
-- 初始化完成。默认超级管理员账号：admin / manager（首次登录后请立即修改密码）
-- =============================================================================
