-- =============================================================================
-- BML 企业管理系统 — 数据库迁移脚本
-- 版本: V2.15.0
-- 说明: 补充所有业务菜单的 F 类型字段权限，确保权限分配面板右侧"表单字段"
--       面板能完整展示每个模块的字段级权限控制项。
--
-- 设计规范：
--   1. F 类型菜单 ID 规则：父菜单ID * 100 + 序号（如 65 → 6511, 6512...）
--   2. 权限标识格式：{模块}:{资源}:field:{字段名}
--   3. 每个 F 节点挂在对应的 C 菜单下，与 B 类型按钮同级
-- =============================================================================

-- ═══════════════════════════════════════════════════════════════════════════════
-- 一、系统设置（id=63）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6311, '系统名称', 63, 'F', 'system:setting:field:systemName', 11, 1, 1, '系统设置 — 系统名称字段', 1, NOW()),
    (6312, '登录页标题', 63, 'F', 'system:setting:field:loginTitle', 12, 1, 1, '系统设置 — 登录页标题字段', 1, NOW()),
    (6313, '侧边栏Logo', 63, 'F', 'system:setting:field:sidebarLogo', 13, 1, 1, '系统设置 — 侧边栏Logo字段', 1, NOW()),
    (6314, '登录页背景', 63, 'F', 'system:setting:field:loginBg', 14, 1, 1, '系统设置 — 登录页背景字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 二、文件管理（id=64）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6411, '文件名称', 64, 'F', 'system:file:field:originalName', 11, 1, 1, '文件管理 — 文件名称字段', 1, NOW()),
    (6412, '文件类型', 64, 'F', 'system:file:field:fileExt', 12, 1, 1, '文件管理 — 文件类型字段', 1, NOW()),
    (6413, '文件大小', 64, 'F', 'system:file:field:fileSize', 13, 1, 1, '文件管理 — 文件大小字段', 1, NOW()),
    (6414, '存储类型', 64, 'F', 'system:file:field:storageType', 14, 1, 1, '文件管理 — 存储类型字段', 1, NOW()),
    (6415, '备注信息', 64, 'F', 'system:file:field:remark', 15, 1, 1, '文件管理 — 备注信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 三、通知公告（id=65）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6511, '公告标题', 65, 'F', 'system:notice:field:noticeTitle', 11, 1, 1, '通知公告 — 公告标题字段', 1, NOW()),
    (6512, '公告类型', 65, 'F', 'system:notice:field:noticeType', 12, 1, 1, '通知公告 — 公告类型字段', 1, NOW()),
    (6513, '公告内容', 65, 'F', 'system:notice:field:noticeContent', 13, 1, 1, '通知公告 — 公告内容字段', 1, NOW()),
    (6514, '发布状态', 65, 'F', 'system:notice:field:status', 14, 1, 1, '通知公告 — 发布状态字段', 1, NOW()),
    (6515, '备注信息', 65, 'F', 'system:notice:field:remark', 15, 1, 1, '通知公告 — 备注信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 四、在线用户（id=66）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6611, '用户账号', 66, 'F', 'system:online:field:username', 11, 1, 1, '在线用户 — 用户账号字段', 1, NOW()),
    (6612, '登录时间', 66, 'F', 'system:online:field:loginTime', 12, 1, 1, '在线用户 — 登录时间字段', 1, NOW()),
    (6613, '过期时间', 66, 'F', 'system:online:field:expireTime', 13, 1, 1, '在线用户 — 过期时间字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 五、缓存管理（id=67）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6711, 'Redis版本', 67, 'F', 'system:cache:field:redisVersion', 11, 1, 1, '缓存管理 — Redis版本字段', 1, NOW()),
    (6712, '已用内存', 67, 'F', 'system:cache:field:usedMemory', 12, 1, 1, '缓存管理 — 已用内存字段', 1, NOW()),
    (6713, '连接数', 67, 'F', 'system:cache:field:connectedClients', 13, 1, 1, '缓存管理 — 连接数字段', 1, NOW()),
    (6714, '运行天数', 67, 'F', 'system:cache:field:uptimeInDays', 14, 1, 1, '缓存管理 — 运行天数字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 六、定时任务（id=68）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6811, '任务名称', 68, 'F', 'system:job:field:jobName', 11, 1, 1, '定时任务 — 任务名称字段', 1, NOW()),
    (6812, '任务分组', 68, 'F', 'system:job:field:jobGroup', 12, 1, 1, '定时任务 — 任务分组字段', 1, NOW()),
    (6813, '调用目标', 68, 'F', 'system:job:field:invokeTarget', 13, 1, 1, '定时任务 — 调用目标字段', 1, NOW()),
    (6814, 'Cron表达式', 68, 'F', 'system:job:field:cronExpression', 14, 1, 1, '定时任务 — Cron表达式字段', 1, NOW()),
    (6815, '执行策略', 68, 'F', 'system:job:field:misfirePolicy', 15, 1, 1, '定时任务 — 执行策略字段', 1, NOW()),
    (6816, '是否并发', 68, 'F', 'system:job:field:concurrent', 16, 1, 1, '定时任务 — 是否并发字段', 1, NOW()),
    (6817, '任务状态', 68, 'F', 'system:job:field:status', 17, 1, 1, '定时任务 — 任务状态字段', 1, NOW()),
    (6818, '备注信息', 68, 'F', 'system:job:field:remark', 18, 1, 1, '定时任务 — 备注信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 七、任务日志（id=69）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (6911, '任务名称', 69, 'F', 'system:joblog:field:jobName', 11, 1, 1, '任务日志 — 任务名称字段', 1, NOW()),
    (6912, '任务分组', 69, 'F', 'system:joblog:field:jobGroup', 12, 1, 1, '任务日志 — 任务分组字段', 1, NOW()),
    (6913, '调用目标', 69, 'F', 'system:joblog:field:invokeTarget', 13, 1, 1, '任务日志 — 调用目标字段', 1, NOW()),
    (6914, '执行状态', 69, 'F', 'system:joblog:field:status', 14, 1, 1, '任务日志 — 执行状态字段', 1, NOW()),
    (6915, '执行耗时', 69, 'F', 'system:joblog:field:costTime', 15, 1, 1, '任务日志 — 执行耗时字段', 1, NOW()),
    (6916, '异常信息', 69, 'F', 'system:joblog:field:exceptionInfo', 16, 1, 1, '任务日志 — 异常信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 八、系统监控（id=73）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (7311, 'CPU信息', 73, 'F', 'monitor:server:field:cpu', 11, 1, 1, '系统监控 — CPU信息字段', 1, NOW()),
    (7312, '内存信息', 73, 'F', 'monitor:server:field:mem', 12, 1, 1, '系统监控 — 内存信息字段', 1, NOW()),
    (7313, 'JVM信息', 73, 'F', 'monitor:server:field:jvm', 13, 1, 1, '系统监控 — JVM信息字段', 1, NOW()),
    (7314, '磁盘信息', 73, 'F', 'monitor:server:field:disk', 14, 1, 1, '系统监控 — 磁盘信息字段', 1, NOW()),
    (7315, '网络信息', 73, 'F', 'monitor:server:field:net', 15, 1, 1, '系统监控 — 网络信息字段', 1, NOW()),
    (7316, '系统环境', 73, 'F', 'monitor:server:field:sys', 16, 1, 1, '系统监控 — 系统环境字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 九、审计总览（id=48）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (4811, '操作总数', 48, 'F', 'system:audit:field:operationTotal', 11, 1, 1, '审计总览 — 操作总数字段', 1, NOW()),
    (4812, '异常总数', 48, 'F', 'system:audit:field:operationErrorTotal', 12, 1, 1, '审计总览 — 异常总数字段', 1, NOW()),
    (4813, '登录总数', 48, 'F', 'system:audit:field:loginTotal', 13, 1, 1, '审计总览 — 登录总数字段', 1, NOW()),
    (4814, '告警总数', 48, 'F', 'system:audit:field:alertTotal', 14, 1, 1, '审计总览 — 告警总数字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十、登录日志（id=49）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (4911, '用户账号', 49, 'F', 'system:loginlog:field:username', 11, 1, 1, '登录日志 — 用户账号字段', 1, NOW()),
    (4912, '登录IP', 49, 'F', 'system:loginlog:field:ipaddr', 12, 1, 1, '登录日志 — 登录IP字段', 1, NOW()),
    (4913, '登录地点', 49, 'F', 'system:loginlog:field:loginLocation', 13, 1, 1, '登录日志 — 登录地点字段', 1, NOW()),
    (4914, '浏览器', 49, 'F', 'system:loginlog:field:browser', 14, 1, 1, '登录日志 — 浏览器字段', 1, NOW()),
    (4915, '操作系统', 49, 'F', 'system:loginlog:field:os', 15, 1, 1, '登录日志 — 操作系统字段', 1, NOW()),
    (4916, '登录状态', 49, 'F', 'system:loginlog:field:status', 16, 1, 1, '登录日志 — 登录状态字段', 1, NOW()),
    (4917, '登录信息', 49, 'F', 'system:loginlog:field:msg', 17, 1, 1, '登录日志 — 登录信息字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十一、操作日志（id=47）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (4711, '操作模块', 47, 'F', 'system:operlog:field:title', 11, 1, 1, '操作日志 — 操作模块字段', 1, NOW()),
    (4712, '业务类型', 47, 'F', 'system:operlog:field:businessType', 12, 1, 1, '操作日志 — 业务类型字段', 1, NOW()),
    (4713, '操作人员', 47, 'F', 'system:operlog:field:operName', 13, 1, 1, '操作日志 — 操作人员字段', 1, NOW()),
    (4714, '请求方式', 47, 'F', 'system:operlog:field:requestMethod', 14, 1, 1, '操作日志 — 请求方式字段', 1, NOW()),
    (4715, '操作IP', 47, 'F', 'system:operlog:field:operIp', 15, 1, 1, '操作日志 — 操作IP字段', 1, NOW()),
    (4716, '操作状态', 47, 'F', 'system:operlog:field:status', 16, 1, 1, '操作日志 — 操作状态字段', 1, NOW()),
    (4717, '操作耗时', 47, 'F', 'system:operlog:field:costTime', 17, 1, 1, '操作日志 — 操作耗时字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十二、异常日志（id=50）字段权限
-- 注意：原 ID 5011-5015 与 V2.5.0（部门管理字段）和 V2.7.0（异常日志按钮）冲突，
--       INSERT IGNORE 会静默跳过。实际字段权限由 V2.15.1 使用新 ID 5061-5065 插入。
-- ═══════════════════════════════════════════════════════════════════════════════
-- (已移至 V2.15.1 修复脚本，使用无冲突的 ID 5061-5065)

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十三、风险告警（id=51）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (5111, '告警类型', 51, 'F', 'system:securityalert:field:alertType', 11, 1, 1, '风险告警 — 告警类型字段', 1, NOW()),
    (5112, '告警级别', 51, 'F', 'system:securityalert:field:alertLevel', 12, 1, 1, '风险告警 — 告警级别字段', 1, NOW()),
    (5113, '告警标题', 51, 'F', 'system:securityalert:field:alertTitle', 13, 1, 1, '风险告警 — 告警标题字段', 1, NOW()),
    (5114, '告警内容', 51, 'F', 'system:securityalert:field:alertContent', 14, 1, 1, '风险告警 — 告警内容字段', 1, NOW()),
    (5115, '处理状态', 51, 'F', 'system:securityalert:field:readStatus', 15, 1, 1, '风险告警 — 处理状态字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十四、归档策略（id=52）字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (5211, '在线保留天数', 52, 'F', 'system:auditsetting:field:onlineRetentionDays', 11, 1, 1, '归档策略 — 在线保留天数字段', 1, NOW()),
    (5212, '归档存储', 52, 'F', 'system:auditsetting:field:archiveStorage', 12, 1, 1, '归档策略 — 归档存储字段', 1, NOW()),
    (5213, '自动清理', 52, 'F', 'system:auditsetting:field:autoCleanEnabled', 13, 1, 1, '归档策略 — 自动清理字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十五、角色与权限（id=42）补充字段权限
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `sort`, `visible`, `status`, `remark`, `create_by`, `create_time`)
VALUES
    (5043, '角色名称', 42, 'F', 'system:role:field:roleName', 13, 1, 1, '角色与权限 — 角色名称字段', 1, NOW()),
    (5044, '角色编码', 42, 'F', 'system:role:field:roleCode', 14, 1, 1, '角色与权限 — 角色编码字段', 1, NOW()),
    (5045, '显示排序', 42, 'F', 'system:role:field:sort', 15, 1, 1, '角色与权限 — 显示排序字段', 1, NOW()),
    (5046, '角色状态', 42, 'F', 'system:role:field:status', 16, 1, 1, '角色与权限 — 角色状态字段', 1, NOW());

-- ═══════════════════════════════════════════════════════════════════════════════
-- 十六、超级管理员授权（补充所有新增 F 类型菜单）
-- ═══════════════════════════════════════════════════════════════════════════════
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `half_check`)
VALUES
    -- 系统设置
    (1, 6311, 0), (1, 6312, 0), (1, 6313, 0), (1, 6314, 0),
    -- 文件管理
    (1, 6411, 0), (1, 6412, 0), (1, 6413, 0), (1, 6414, 0), (1, 6415, 0),
    -- 通知公告
    (1, 6511, 0), (1, 6512, 0), (1, 6513, 0), (1, 6514, 0), (1, 6515, 0),
    -- 在线用户
    (1, 6611, 0), (1, 6612, 0), (1, 6613, 0),
    -- 缓存管理
    (1, 6711, 0), (1, 6712, 0), (1, 6713, 0), (1, 6714, 0),
    -- 定时任务
    (1, 6811, 0), (1, 6812, 0), (1, 6813, 0), (1, 6814, 0), (1, 6815, 0), (1, 6816, 0), (1, 6817, 0), (1, 6818, 0),
    -- 任务日志
    (1, 6911, 0), (1, 6912, 0), (1, 6913, 0), (1, 6914, 0), (1, 6915, 0), (1, 6916, 0),
    -- 系统监控
    (1, 7311, 0), (1, 7312, 0), (1, 7313, 0), (1, 7314, 0), (1, 7315, 0), (1, 7316, 0),
    -- 审计总览
    (1, 4811, 0), (1, 4812, 0), (1, 4813, 0), (1, 4814, 0),
    -- 登录日志
    (1, 4911, 0), (1, 4912, 0), (1, 4913, 0), (1, 4914, 0), (1, 4915, 0), (1, 4916, 0), (1, 4917, 0),
    -- 操作日志
    (1, 4711, 0), (1, 4712, 0), (1, 4713, 0), (1, 4714, 0), (1, 4715, 0), (1, 4716, 0), (1, 4717, 0),
    -- 异常日志（已移至 V2.15.1，使用新 ID 5061-5065）
    -- 风险告警
    (1, 5111, 0), (1, 5112, 0), (1, 5113, 0), (1, 5114, 0), (1, 5115, 0),
    -- 归档策略
    (1, 5211, 0), (1, 5212, 0), (1, 5213, 0),
    -- 角色与权限补充
    (1, 5043, 0), (1, 5044, 0), (1, 5045, 0), (1, 5046, 0);
