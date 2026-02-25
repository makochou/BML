-- ----------------------------
-- Table structure for sys_alert
-- ----------------------------
DROP TABLE IF EXISTS `sys_alert`;
CREATE TABLE `sys_alert` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `alert_type` varchar(50) NOT NULL COMMENT '告警类型 (例如: CPU_HIGH, MEMORY_HIGH, DISK_FULL, JVM_HIGH)',
  `alert_level` varchar(20) NOT NULL COMMENT '告警级别 (例如: info, warning, error, critical)',
  `alert_title` varchar(100) NOT NULL COMMENT '告警标题',
  `alert_content` text NOT NULL COMMENT '告警详情内容',
  `read_status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '阅读状态 (0:未读, 1:已读)',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统探针告警通知表';
