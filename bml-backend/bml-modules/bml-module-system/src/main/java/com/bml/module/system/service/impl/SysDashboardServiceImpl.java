package com.bml.module.system.service.impl;

import com.bml.core.framework.license.BmlLicense;
import com.bml.core.framework.license.BmlLicenseHolder;
import com.bml.module.system.service.ServerMonitorService;
import com.bml.module.system.service.SysDashboardService;
import com.bml.module.system.vo.DashboardSummaryVO;
import com.bml.module.system.vo.ServerInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysDashboardServiceImpl implements SysDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final BmlLicenseHolder licenseHolder;
    private final ServerMonitorService serverMonitorService;

    @Override
    public DashboardSummaryVO getDashboardSummary() {
        DashboardSummaryVO vo = new DashboardSummaryVO();
        vo.setLicense(buildLicenseStats());
        vo.setMonitor(buildMonitorStats());
        vo.setApiAccount(buildApiAccountStats());
        vo.setApiRegistry(buildApiRegistryStats());
        vo.setAlert(buildAlertStats());
        vo.setRecentActivities(buildRecentActivities());
        return vo;
    }

    private Long queryForLong(String sql, Object... params) {
        try {
            Long result = jdbcTemplate.queryForObject(sql, Long.class, params);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.warn("SQL execution error: {}, sql: {}", e.getMessage(), sql);
            return 0L;
        }
    }

    private DashboardSummaryVO.LicenseStats buildLicenseStats() {
        DashboardSummaryVO.LicenseStats stats = new DashboardSummaryVO.LicenseStats();
        boolean isEnabled = licenseHolder.isEnabled();
        if (!isEnabled) {
            stats.setValid(true);
            stats.setClientName("开发测试模式");
            stats.setExpireDate("永久有效");
            stats.setDaysLeft(9999L);
            return stats;
        }

        BmlLicense license = licenseHolder.getCurrentLicense();
        if (license != null) {
            stats.setValid(!license.isExpired());
            stats.setClientName(license.getCustomerName());
            stats.setExpireDate(license.getExpireDate().toString());
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), license.getExpireDate());
            stats.setDaysLeft(Math.max(0, daysLeft));
        } else {
            stats.setValid(false);
            stats.setClientName("未授权或授权无效");
            stats.setExpireDate("-");
            stats.setDaysLeft(0L);
        }
        return stats;
    }

    private DashboardSummaryVO.MonitorStats buildMonitorStats() {
        DashboardSummaryVO.MonitorStats stats = new DashboardSummaryVO.MonitorStats();
        try {
            ServerInfoVO serverInfo = serverMonitorService.getServerInfo();
            if (serverInfo != null) {
                stats.setCpuPercent(serverInfo.getCpu() != null ? serverInfo.getCpu().getUsed() : 0.0);
                stats.setMemPercent(serverInfo.getMem() != null ? serverInfo.getMem().getUsage() : 0.0);
                double totalDiskUsage = 0.0;
                if (serverInfo.getDisks() != null && !serverInfo.getDisks().isEmpty()) {
                    for (ServerInfoVO.DiskInfo disk : serverInfo.getDisks()) {
                        totalDiskUsage += disk.getUsage();
                    }
                    totalDiskUsage = totalDiskUsage / serverInfo.getDisks().size();
                }
                stats.setDiskPercent(totalDiskUsage);
                if (serverInfo.getSys() != null) {
                    stats.setOs(serverInfo.getSys().getOsName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect monitor metrics for dashboard: {}", e.getMessage());
            stats.setCpuPercent(0.0);
            stats.setMemPercent(0.0);
            stats.setDiskPercent(0.0);
            stats.setOs("Unknown");
        }
        return stats;
    }

    private DashboardSummaryVO.ApiAccountStats buildApiAccountStats() {
        DashboardSummaryVO.ApiAccountStats stats = new DashboardSummaryVO.ApiAccountStats();
        stats.setTotal(queryForLong("SELECT COUNT(*) FROM sys_api_account WHERE deleted = 0"));
        stats.setEnabled(queryForLong("SELECT COUNT(*) FROM sys_api_account WHERE deleted = 0 AND status = '0'"));
        return stats;
    }

    private DashboardSummaryVO.ApiRegistryStats buildApiRegistryStats() {
        DashboardSummaryVO.ApiRegistryStats stats = new DashboardSummaryVO.ApiRegistryStats();
        stats.setTotal(queryForLong("SELECT COUNT(*) FROM sys_api_registry"));
        return stats;
    }

    private DashboardSummaryVO.AlertStats buildAlertStats() {
        DashboardSummaryVO.AlertStats stats = new DashboardSummaryVO.AlertStats();
        stats.setUnresolved(queryForLong("SELECT COUNT(*) FROM sys_alert WHERE read_status = 0"));
        String todayStart = LocalDate.now().atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        stats.setTodayTotal(queryForLong("SELECT COUNT(*) FROM sys_alert WHERE create_time >= ?", todayStart));
        return stats;
    }

    private List<DashboardSummaryVO.RecentActivity> buildRecentActivities() {
        List<DashboardSummaryVO.RecentActivity> activities = new ArrayList<>();
        String sql = "SELECT alert_title as title, create_time as createTime, alert_level as level FROM sys_alert ORDER BY create_time DESC LIMIT 6";
        try {
            activities = jdbcTemplate.query(sql, (rs, rowNum) -> {
                DashboardSummaryVO.RecentActivity action = new DashboardSummaryVO.RecentActivity();
                action.setText(rs.getString("title"));
                
                Timestamp createTime = rs.getTimestamp("createTime");
                if (createTime != null) {
                    action.setTime(timeAgo(createTime.toLocalDateTime()));
                } else {
                    action.setTime("刚刚");
                }
                
                String level = rs.getString("level");
                if ("CRITICAL".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level)) {
                    action.setColor("#f53f3f"); // Red
                } else if ("WARNING".equalsIgnoreCase(level)) {
                    action.setColor("#ff7d00"); // Orange
                } else {
                    action.setColor("#165dff"); // Blue
                }
                return action;
            });
        } catch (Exception e) {
            log.warn("Failed to fetch recent activities: {}", e.getMessage());
        }
        
        if (activities.isEmpty()) {
            DashboardSummaryVO.RecentActivity noData = new DashboardSummaryVO.RecentActivity();
            noData.setText("暂无系统告警活动记录");
            noData.setTime("当前");
            noData.setColor("#c9cdd4");
            activities.add(noData);
        }
        return activities;
    }

    private String timeAgo(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(time, now);
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + " 分钟前";
        long hours = ChronoUnit.HOURS.between(time, now);
        if (hours < 24) return hours + " 小时前";
        long days = ChronoUnit.DAYS.between(time, now);
        return days + " 天前";
    }
}
