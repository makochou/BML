-- =============================================================================
-- 版本: V1.9.6
-- 描述: 优化告警去重机制和增量轮询性能
-- 作者: BML Team
-- 日期: 2026-04-30
-- =============================================================================

-- 为 sys_alert 表添加复合索引，优化告警去重查询性能
-- 该索引用于快速查找相同类型和标题的未读告警，支持智能去重功能
-- 索引列顺序：alert_type, alert_title, read_status, deleted
-- 这样可以高效支持 WHERE alert_type = ? AND alert_title = ? AND read_status = 0 AND deleted = 0 的查询
CREATE INDEX idx_alert_dedup ON sys_alert (alert_type, alert_title, read_status, deleted);

-- 为 sys_alert 表添加 update_time 索引，优化增量轮询查询性能
-- 该索引用于前端增量轮询接口，快速查找指定时间之后更新的告警
-- 这样可以高效支持 WHERE update_time > ? AND deleted = 0 的查询
CREATE INDEX idx_update_time ON sys_alert (update_time, deleted);

-- 说明：
-- 1. idx_alert_dedup 索引不是唯一索引，因为允许存在多条已读的相同告警（历史记录）
-- 2. 去重逻辑在应用层实现（SysAlertServiceImpl.saveOrUpdateAlert），确保灵活性
-- 3. 索引包含 deleted 字段，因为 MyBatis-Plus 的逻辑删除会自动添加 deleted = 0 条件
-- 4. idx_update_time 索引用于增量轮询，确保告警更新时前端能及时感知

