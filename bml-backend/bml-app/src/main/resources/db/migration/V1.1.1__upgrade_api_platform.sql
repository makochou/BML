/*
 * V1.1.0 API 开放平台数据库变更脚本
 * ---------------------------------------
 * 1. 新增 bml_api_app 应用表 (存储 AppKey/Secret)
 * 2. 新增 bml_api_access 权限表 (App与API的关联)
 * 3. 修改 bml_api_info 表 (增加权限标识等辅助字段)
 */

-- ----------------------------
-- 1. 应用管理表
-- ----------------------------
create table if not exists bml_api_app (
    id bigint not null auto_increment comment '主键ID',
    app_id varchar(32) not null comment '应用ID(AppKey)',
    app_secret varchar(64) not null comment '应用密钥(AppSecret)',
    name varchar(50) not null comment '应用名称',
    status tinyint default 1 comment '状态(1正常 0停用)',
    remark varchar(500) default '' comment '备注',
    deleted tinyint default 0 comment '逻辑删除(1已删 0未删)',
    create_by varchar(64) default '' comment '创建者',
    create_time datetime default null comment '创建时间',
    update_by varchar(64) default '' comment '更新者',
    update_time datetime default null comment '更新时间',
    primary key (id),
    unique key uk_app_id (app_id)
) engine=innodb comment='API应用管理表';

-- ----------------------------
-- 2. 应用权限关联表
-- ----------------------------
create table if not exists bml_api_access (
    id bigint not null auto_increment comment '主键ID',
    app_id bigint not null comment '应用ID(外键)',
    api_id bigint not null comment 'API ID(外键)',
    create_time datetime default null comment '创建时间',
    primary key (id),
    unique key uk_app_api (app_id, api_id)
) engine=innodb comment='API应用权限表';


-- ----------------------------
-- 3. API信息表扩展 (如果字段不存在则添加)
-- ----------------------------
-- 检查是否需要增加字段 (Flyway 会自动处理脚本，但为了幂等性，可以使用存储过程或直接依靠Flyway的版本控制特性。
-- 这里假设是全新追加，如果 V3.0.0 脚本里已经创建了表，我们这里只 ALTER)

-- 增加 `perm_flag` 权限标识 (用于关联菜单权限，例如 "system:user:list")
set @exist := (select count(*) from information_schema.columns where table_schema=database() and table_name='bml_api_info' and column_name='perm_flag');
set @sql := if(@exist=0, 'alter table bml_api_info add column perm_flag varchar(100) default null comment "权限标识" after auth_type', 'select "Column perm_flag exists"');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

-- 增加 `controller_class` (用于同步识别，例如 "UserControler")
set @exist := (select count(*) from information_schema.columns where table_schema=database() and table_name='bml_api_info' and column_name='controller');
set @sql := if(@exist=0, 'alter table bml_api_info add column controller varchar(255) default null comment "所属控制器类名" after group_id', 'select "Column controller exists"');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
