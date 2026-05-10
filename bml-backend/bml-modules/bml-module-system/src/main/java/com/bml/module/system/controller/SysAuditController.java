package com.bml.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.result.Result;
import com.bml.core.framework.operlog.BusinessType;
import com.bml.core.framework.operlog.OperationLog;
import com.bml.module.system.converter.AlertConverter;
import com.bml.module.system.dto.SysOperationLogQuery;
import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.entity.SysLoginLog;
import com.bml.module.system.service.ISysAlertService;
import com.bml.module.system.service.SysConfigService;
import com.bml.module.system.service.SysLoginLogService;
import com.bml.module.system.service.SysOperationLogService;
import com.bml.module.system.util.AuditCsvExportUtils;
import com.bml.module.system.vo.AuditArchiveSettingVO;
import com.bml.module.system.vo.AuditOverviewVO;
import com.bml.module.system.vo.SysAlertVO;
import com.bml.module.system.vo.SysOperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 日志审计中心控制器。
 * <p>
 * 该控制器负责提供跨日志类型的聚合视图与治理配置，包括审计首页概览、风险告警列表和归档策略维护。
 * 登录日志、操作日志仍保留独立控制器，便于权限隔离和后续扩展独立表结构。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "日志审计中心", description = "审计概览、风险告警与归档策略")
@RestController
@RequestMapping("/system/audit")
@RequiredArgsConstructor
public class SysAuditController {

    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final String RETENTION_DAYS_KEY = "sys.audit.onlineRetentionDays";

    private static final String ARCHIVE_ENABLED_KEY = "sys.audit.archiveEnabled";

    private static final String ARCHIVE_STORAGE_KEY = "sys.audit.archiveStorage";

    private static final String AUTO_CLEAN_ENABLED_KEY = "sys.audit.autoCleanEnabled";

    private final SysOperationLogService operationLogService;

    private final SysLoginLogService loginLogService;

    private final ISysAlertService alertService;

    private final SysConfigService configService;

    /**
     * 查询审计中心概览指标。
     *
     * @return 审计概览
     */
    @Operation(summary = "查询日志审计中心概览")
    @PreAuthorize("@ss.hasPermi('system:audit:list')")
    @GetMapping("/overview")
    public Result<AuditOverviewVO> overview() {
        AuditArchiveSettingVO setting = getArchiveSettingInternal();
        AuditOverviewVO overview = new AuditOverviewVO();
        overview.setOperationTotal(operationLogService.countOperationLogs(null));
        overview.setOperationErrorTotal(operationLogService.countOperationLogs(1));
        overview.setLoginTotal(loginLogService.count());
        overview.setLoginFailureTotal(loginLogService.count(
                new LambdaQueryWrapper<SysLoginLog>().eq(SysLoginLog::getStatus, 0)));
        overview.setAlertTotal(alertService.count(new LambdaQueryWrapper<SysAlert>().eq(SysAlert::getDeleted, 0)));
        overview.setUnreadAlertTotal(alertService.getUnreadCount());
        overview.setOnlineRetentionDays(setting.getOnlineRetentionDays());
        overview.setArchiveEnabled(setting.getArchiveEnabled());
        return Result.ok(overview);
    }

    /**
     * 查询风险告警列表。
     *
     * @param limit 返回条数
     * @return 风险告警列表
     */
    @Operation(summary = "查询风险告警列表")
    @PreAuthorize("@ss.hasPermi('system:securityalert:list')")
    @GetMapping("/security-alerts")
    public Result<List<SysAlertVO>> securityAlerts(@RequestParam(name = "limit", defaultValue = "50") Integer limit) {
        return Result.ok(AlertConverter.INSTANCE.toVOList(alertService.getRecentAlerts(limit)));
    }

    /**
     * 将指定风险告警标记为已读。
     * <p>
     * 该接口归属于日志审计中心的风险告警权限体系，避免审计页面直接依赖监控中心的告警处理权限。
     * </p>
     *
     * @param id 风险告警ID
     * @return 是否标记成功
     */
    @Operation(summary = "将指定风险告警标记为已读")
    @PreAuthorize("@ss.hasPermi('system:securityalert:edit')")
    @PutMapping("/security-alerts/{id}/read")
    public Result<Boolean> markSecurityAlertAsRead(@PathVariable("id") Long id) {
        return Result.ok(alertService.markAsRead(id));
    }

    /**
     * 分页查询异常日志。
     * <p>
     * 当前异常日志复用操作日志表中 status=1 的异常记录，保持审计查询入口独立、底层数据源统一。
     * 后续若建设独立异常表，只需要替换该方法的数据源，前端接口契约无需变化。
     * </p>
     *
     * @param query 查询条件
     * @return 异常日志分页数据
     */
    @Operation(summary = "分页查询异常日志")
    @PreAuthorize("@ss.hasPermi('system:exceptionlog:list')")
    @GetMapping("/exception-logs/page")
    public Result<PageResult<SysOperationLogVO>> exceptionLogPage(SysOperationLogQuery query) {
        SysOperationLogQuery safeQuery = query == null ? new SysOperationLogQuery() : query;
        safeQuery.setStatus(1);
        return Result.ok(operationLogService.selectOperationLogPage(safeQuery));
    }

    /**
     * 导出异常日志 CSV。
     *
     * @param query    查询条件
     * @param response HTTP响应
     * @throws IOException 写出失败时由全局异常处理器统一处理
     */
    @Operation(summary = "导出异常日志")
    @OperationLog(title = "异常日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:exceptionlog:export')")
    @GetMapping("/exception-logs/export")
    public void exportExceptionLogs(SysOperationLogQuery query, HttpServletResponse response) throws IOException {
        SysOperationLogQuery exportQuery = query == null ? new SysOperationLogQuery() : query;
        exportQuery.setStatus(1);
        exportQuery.setPageNum(1);
        exportQuery.setPageSize(10000);
        List<SysOperationLogVO> records = operationLogService.selectOperationLogPage(exportQuery).getRecords();
        List<List<?>> rows = new ArrayList<>();
        for (SysOperationLogVO item : records) {
            rows.add(Arrays.asList(
                    item.getId(),
                    item.getTitle(),
                    item.getRequestMethod(),
                    item.getOperName(),
                    item.getOperIp(),
                    item.getOperUrl(),
                    item.getErrorMsg(),
                    item.getCostTime(),
                    item.getOperTime() == null ? "" : item.getOperTime()));
        }
        AuditCsvExportUtils.writeCsv(response,
                "exception-log-" + LocalDateTime.now().format(EXPORT_TIME_FORMATTER) + ".csv",
                List.of("日志ID", "模块", "请求方式", "操作人", "IP地址", "请求地址", "异常信息", "耗时ms", "发生时间"),
                rows);
    }

    /**
     * 查询审计归档策略。
     *
     * @return 归档策略
     */
    @Operation(summary = "查询审计归档策略")
    @PreAuthorize("@ss.hasPermi('system:auditsetting:list')")
    @GetMapping("/archive-setting")
    public Result<AuditArchiveSettingVO> archiveSetting() {
        return Result.ok(getArchiveSettingInternal());
    }

    /**
     * 保存审计归档策略。
     *
     * @param setting 归档策略
     * @return 操作结果
     */
    @Operation(summary = "保存审计归档策略")
    @OperationLog(title = "日志归档策略", businessType = BusinessType.UPDATE)
    @PreAuthorize("@ss.hasPermi('system:auditsetting:edit')")
    @PutMapping("/archive-setting")
    public Result<Void> saveArchiveSetting(@RequestBody AuditArchiveSettingVO setting) {
        AuditArchiveSettingVO safeSetting = setting == null ? new AuditArchiveSettingVO() : setting;
        int retentionDays = safeSetting.getOnlineRetentionDays() == null || safeSetting.getOnlineRetentionDays() < 1
                ? 180
                : Math.min(safeSetting.getOnlineRetentionDays(), 3650);
        configService.upsertConfig(RETENTION_DAYS_KEY, String.valueOf(retentionDays), "审计日志在线保留天数");
        configService.upsertConfig(ARCHIVE_ENABLED_KEY, String.valueOf(Boolean.TRUE.equals(safeSetting.getArchiveEnabled())), "审计日志自动归档开关");
        configService.upsertConfig(ARCHIVE_STORAGE_KEY,
                safeSetting.getArchiveStorage() == null ? "local://data/audit-archive" : safeSetting.getArchiveStorage(),
                "审计日志归档存储位置");
        configService.upsertConfig(AUTO_CLEAN_ENABLED_KEY, String.valueOf(Boolean.TRUE.equals(safeSetting.getAutoCleanEnabled())), "审计日志自动清理开关");
        return Result.ok();
    }

    private AuditArchiveSettingVO getArchiveSettingInternal() {
        AuditArchiveSettingVO setting = new AuditArchiveSettingVO();
        setting.setOnlineRetentionDays(parseInt(configService.getConfigValue(RETENTION_DAYS_KEY, "180"), 180));
        setting.setArchiveEnabled(Boolean.parseBoolean(configService.getConfigValue(ARCHIVE_ENABLED_KEY, "false")));
        setting.setArchiveStorage(configService.getConfigValue(ARCHIVE_STORAGE_KEY, "local://data/audit-archive"));
        setting.setAutoCleanEnabled(Boolean.parseBoolean(configService.getConfigValue(AUTO_CLEAN_ENABLED_KEY, "false")));
        return setting;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
