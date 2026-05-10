-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.11.0
-- 说明: 新增品牌设置、文件管理、通知公告、在线用户、缓存管理、定时任务与任务日志基础能力
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、基础业务表
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `sys_file_info` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名',
    `file_path` VARCHAR(1000) NOT NULL COMMENT '文件物理路径',
    `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件访问地址',
    `file_ext` VARCHAR(30) DEFAULT NULL COMMENT '文件扩展名',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `storage_type` TINYINT NOT NULL DEFAULT 1 COMMENT '存储类型：1 本地',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_file_original_name` (`original_name`),
    KEY `idx_file_ext` (`file_ext`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统文件表';

CREATE TABLE IF NOT EXISTS `sys_notice` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `notice_title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `notice_type` TINYINT NOT NULL DEFAULT 1 COMMENT '公告类型：1 通知，2 公告，3 维护',
    `notice_content` LONGTEXT NOT NULL COMMENT '公告内容',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0 草稿，1 已发布',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_notice_type` (`notice_type`),
    KEY `idx_notice_status` (`status`),
    KEY `idx_notice_publish_time` (`publish_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知公告表';

CREATE TABLE IF NOT EXISTS `sys_job` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `job_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `job_group` VARCHAR(100) NOT NULL DEFAULT 'DEFAULT' COMMENT '任务分组',
    `invoke_target` VARCHAR(500) NOT NULL COMMENT '调用目标',
    `cron_expression` VARCHAR(100) NOT NULL COMMENT 'Cron 表达式',
    `misfire_policy` TINYINT NOT NULL DEFAULT 1 COMMENT '错过策略：1 立即执行，2 执行一次，3 放弃执行',
    `concurrent` TINYINT NOT NULL DEFAULT 0 COMMENT '是否并发：1 允许，0 禁止',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：1 启用，0 暂停',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_job_group` (`job_group`),
    KEY `idx_job_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '定时任务表';

CREATE TABLE IF NOT EXISTS `sys_job_log` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `job_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `job_group` VARCHAR(100) NOT NULL COMMENT '任务分组',
    `invoke_target` VARCHAR(500) NOT NULL COMMENT '调用目标',
    `job_message` VARCHAR(500) DEFAULT NULL COMMENT '日志信息',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态：0 成功，1 失败',
    `exception_info` TEXT DEFAULT NULL COMMENT '异常信息',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `cost_time` BIGINT DEFAULT 0 COMMENT '耗时毫秒',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人 ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人 ID',
    `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
    PRIMARY KEY (`id`),
    KEY `idx_job_log_name` (`job_name`),
    KEY `idx_job_log_status` (`status`),
    KEY `idx_job_log_start_time` (`start_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '任务日志表';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、系统配置目录下新增菜单
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `path`, `component`, `perms`, `icon`, `sort`, `visible`, `status`, `is_frame`, `remark`, `create_by`, `create_time`)
VALUES
    (63, '品牌设置', 60, 'C', 'branding', 'system/branding/index', 'system:setting:list', 'palette', 3, 1, 1, 0, '业务侧品牌文案、登录页图片与侧边栏 Logo 配置', 1, NOW()),
    (64, '文件管理', 60, 'C', 'file', 'system/file/index', 'system:file:list', 'file', 4, 1, 1, 0, '系统文件上传、下载与归档管理', 1, NOW()),
    (65, '通知公告', 60, 'C', 'notice', 'system/notice/index', 'system:notice:list', 'notification', 5, 1, 1, 0, '业务系统通知公告发布与维护', 1, NOW()),
    (66, '在线用户', 60, 'C', 'online', 'system/online/index', 'system:online:list', 'user', 6, 1, 1, 0, '查看在线用户并支持强制下线', 1, NOW()),
    (67, '缓存管理', 60, 'C', 'cache', 'system/cache/index', 'system:cache:list', 'storage', 7, 1, 1, 0, 'Redis 缓存概览、键查询与清理', 1, NOW()),
    (68, '定时任务', 60, 'C', 'job', 'system/job/index', 'system:job:list', 'history', 8, 1, 1, 0, '定时任务配置、启停与手动执行', 1, NOW()),
    (69, '任务日志', 60, 'C', 'job-log', 'system/job-log/index', 'system:joblog:list', 'file', 9, 1, 1, 0, '定时任务执行日志查询与清理', 1, NOW())
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `menu_type` = VALUES(`menu_type`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `perms` = VALUES(`perms`),
    `icon` = VALUES(`icon`),
    `sort` = VALUES(`sort`),
    `visible` = VALUES(`visible`),
    `status` = VALUES(`status`),
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、按钮权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6301, '保存品牌设置', 63, 'B', 'system:setting:edit', 1, 1, 1, '品牌设置 — 保存品牌配置', 1, NOW()),
    (6302, '上传品牌图片', 63, 'B', 'system:setting:upload', 2, 1, 1, '品牌设置 — 上传品牌图片', 1, NOW()),
    (6303, '删除品牌图片', 63, 'B', 'system:setting:remove', 3, 1, 1, '品牌设置 — 删除品牌图片', 1, NOW()),
    (6401, '查询文件', 64, 'B', 'system:file:query', 1, 1, 1, '文件管理 — 查询文件详情', 1, NOW()),
    (6402, '上传文件', 64, 'B', 'system:file:upload', 2, 1, 1, '文件管理 — 上传文件', 1, NOW()),
    (6403, '下载文件', 64, 'B', 'system:file:download', 3, 1, 1, '文件管理 — 下载文件', 1, NOW()),
    (6404, '删除文件', 64, 'B', 'system:file:remove', 4, 1, 1, '文件管理 — 删除文件', 1, NOW()),
    (6501, '查询公告', 65, 'B', 'system:notice:query', 1, 1, 1, '通知公告 — 查询公告详情', 1, NOW()),
    (6502, '新增公告', 65, 'B', 'system:notice:add', 2, 1, 1, '通知公告 — 新增公告', 1, NOW()),
    (6503, '编辑公告', 65, 'B', 'system:notice:edit', 3, 1, 1, '通知公告 — 编辑公告', 1, NOW()),
    (6504, '删除公告', 65, 'B', 'system:notice:remove', 4, 1, 1, '通知公告 — 删除公告', 1, NOW()),
    (6505, '发布公告', 65, 'B', 'system:notice:publish', 5, 1, 1, '通知公告 — 发布或撤回公告', 1, NOW()),
    (6601, '查询在线用户', 66, 'B', 'system:online:query', 1, 1, 1, '在线用户 — 查询用户会话', 1, NOW()),
    (6602, '强制下线', 66, 'B', 'system:online:forceLogout', 2, 1, 1, '在线用户 — 强制用户下线', 1, NOW()),
    (6701, '查询缓存', 67, 'B', 'system:cache:query', 1, 1, 1, '缓存管理 — 查询缓存详情', 1, NOW()),
    (6702, '删除缓存', 67, 'B', 'system:cache:remove', 2, 1, 1, '缓存管理 — 删除缓存键', 1, NOW()),
    (6703, '清理缓存', 67, 'B', 'system:cache:clear', 3, 1, 1, '缓存管理 — 按前缀清理缓存', 1, NOW()),
    (6801, '查询任务', 68, 'B', 'system:job:query', 1, 1, 1, '定时任务 — 查询任务详情', 1, NOW()),
    (6802, '新增任务', 68, 'B', 'system:job:add', 2, 1, 1, '定时任务 — 新增任务', 1, NOW()),
    (6803, '编辑任务', 68, 'B', 'system:job:edit', 3, 1, 1, '定时任务 — 编辑任务', 1, NOW()),
    (6804, '删除任务', 68, 'B', 'system:job:remove', 4, 1, 1, '定时任务 — 删除任务', 1, NOW()),
    (6805, '运行任务', 68, 'B', 'system:job:run', 5, 1, 1, '定时任务 — 立即运行任务', 1, NOW()),
    (6806, '启停任务', 68, 'B', 'system:job:changeStatus', 6, 1, 1, '定时任务 — 启用或暂停任务', 1, NOW()),
    (6901, '查询任务日志', 69, 'B', 'system:joblog:query', 1, 1, 1, '任务日志 — 查询任务日志详情', 1, NOW()),
    (6902, '清空任务日志', 69, 'B', 'system:joblog:clean', 2, 1, 1, '任务日志 — 清空任务日志', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、基础任务数据
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT INTO `sys_job` (`id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `remark`, `create_by`, `create_time`)
VALUES (2001, '服务器资源巡检', 'SYSTEM', 'serverAlertJob.checkServerMetrics', '0 * * * * ?', 1, 0, 0, '系统内置巡检任务，默认暂停；当前仍保留代码级 @Scheduled 巡检。', 1, NOW())
ON DUPLICATE KEY UPDATE
    `job_name` = VALUES(`job_name`),
    `job_group` = VALUES(`job_group`),
    `invoke_target` = VALUES(`invoke_target`),
    `cron_expression` = VALUES(`cron_expression`),
    `misfire_policy` = VALUES(`misfire_policy`),
    `concurrent` = VALUES(`concurrent`),
    `remark` = VALUES(`remark`);

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、超级管理员授权
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    (1, 63, 0), (1, 64, 0), (1, 65, 0), (1, 66, 0), (1, 67, 0), (1, 68, 0), (1, 69, 0),
    (1, 6301, 0), (1, 6302, 0), (1, 6303, 0),
    (1, 6401, 0), (1, 6402, 0), (1, 6403, 0), (1, 6404, 0),
    (1, 6501, 0), (1, 6502, 0), (1, 6503, 0), (1, 6504, 0), (1, 6505, 0),
    (1, 6601, 0), (1, 6602, 0),
    (1, 6701, 0), (1, 6702, 0), (1, 6703, 0),
    (1, 6801, 0), (1, 6802, 0), (1, 6803, 0), (1, 6804, 0), (1, 6805, 0), (1, 6806, 0),
    (1, 6901, 0), (1, 6902, 0);
