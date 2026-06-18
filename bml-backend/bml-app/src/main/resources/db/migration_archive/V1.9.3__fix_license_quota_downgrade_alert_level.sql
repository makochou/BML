-- =============================================================================
-- BML 企业中台管理系统 — 数据修复脚本
-- 版本: V1.9.3
-- 说明: 修复历史数据中 LICENSE_QUOTA_DOWNGRADE 类型告警的级别分类问题。
--
-- 背景：
--   许可证配额降级告警（alert_type = 'LICENSE_QUOTA_DOWNGRADE'）属于系统自动
--   执行的配额调整结果通知，性质上是「系统提示」，而非真正的资源异常告警。
--   历史数据中该类型告警的 alert_level 被错误地设置为 'warning'（警告），
--   导致在告警中心被归类到「警告」Tab，与 CPU 过载、磁盘满等真实告警混在一起，
--   增加了运维噪音，也不符合业务语义。
--
-- 修复内容：
--   将 sys_alert 表中所有 alert_type = 'LICENSE_QUOTA_DOWNGRADE' 且
--   alert_level = 'warning' 的历史记录，统一修正为 alert_level = 'info'（提示），
--   使其在告警中心归类到「提示」Tab 下展示。
--
-- 影响范围：
--   仅修改 alert_type = 'LICENSE_QUOTA_DOWNGRADE' 的记录，
--   不影响其他类型（CPU_HIGH / MEMORY_HIGH / DISK_FULL / JVM_HIGH）的告警数据。
--
-- 幂等性：
--   脚本使用 WHERE 条件精确过滤，重复执行不会产生副作用。
-- =============================================================================

UPDATE sys_alert
SET alert_level = 'info'
WHERE alert_type  = 'LICENSE_QUOTA_DOWNGRADE'
  AND alert_level = 'warning'
  AND deleted     = 0;
