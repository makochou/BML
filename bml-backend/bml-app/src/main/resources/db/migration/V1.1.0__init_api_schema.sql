/* API 分组表 */
CREATE TABLE IF NOT EXISTS `bml_api_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  `name` varchar(50) NOT NULL COMMENT '分组名称',
  `description` varchar(200) DEFAULT NULL COMMENT '分组描述',
  `sort` int(11) DEFAULT '0' COMMENT '显示顺序',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态 (1:正常 0:停用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API分组表';

/* API 接口详情表 */
CREATE TABLE IF NOT EXISTS `bml_api_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '接口ID',
  `group_id` bigint(20) NOT NULL COMMENT '所属分组ID',
  `name` varchar(100) NOT NULL COMMENT '接口名称',
  `path` varchar(200) NOT NULL COMMENT '请求路径',
  `method` varchar(20) NOT NULL COMMENT '请求方法(GET, POST, PUT, DELETE)',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态 (0:草稿 1:发布 2:废弃)',
  `auth_type` varchar(20) DEFAULT 'NONE' COMMENT '认证方式 (NONE, API_KEY, OAUTH2)',
  `request_params` text COMMENT '请求参数定义(JSON)',
  `response_params` text COMMENT '响应参数定义(JSON)',
  `example` text COMMENT '请求示例',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API接口详情表';
