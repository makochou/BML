package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "工作台聚合汇总数据流")
public class DashboardSummaryVO {

    @Schema(description = "授权状态")
    private LicenseStats license;

    @Schema(description = "主机监控状态")
    private MonitorStats monitor;

    @Schema(description = "告警统计")
    private AlertStats alert;

    @Schema(description = "API账号统计")
    private ApiAccountStats apiAccount;

    @Schema(description = "API接口统计")
    private ApiRegistryStats apiRegistry;

    @Schema(description = "近期活动记录")
    private List<RecentActivity> recentActivities;

    @Data
    public static class LicenseStats {
        @Schema(description = "授权状态（true: 有效, false: 无效/过期）")
        private Boolean valid;

        @Schema(description = "客户名称")
        private String clientName;

        @Schema(description = "到期日期")
        private String expireDate;

        @Schema(description = "剩余天数")
        private Long daysLeft;
    }

    @Data
    public static class MonitorStats {
        @Schema(description = "CPU使用率（百分比）")
        private Double cpuPercent;

        @Schema(description = "内存使用率（百分比）")
        private Double memPercent;

        @Schema(description = "磁盘使用率（百分比）")
        private Double diskPercent;
        
        @Schema(description = "操作系统信息")
        private String os;
    }

    @Data
    public static class AlertStats {
        @Schema(description = "今日告警总数")
        private Long todayTotal;

        @Schema(description = "待处理告警总数")
        private Long unresolved;
    }

    @Data
    public static class ApiAccountStats {
        @Schema(description = "全部账号数")
        private Long total;

        @Schema(description = "启用的账号数")
        private Long enabled;
    }

    @Data
    public static class ApiRegistryStats {
        @Schema(description = "纳管通过的接口总数")
        private Long total;
    }

    @Data
    public static class RecentActivity {
        @Schema(description = "活动内容描述")
        private String text;

        @Schema(description = "相差时间语义（如：5分钟前）")
        private String time;

        @Schema(description = "颜色主题指示（根据活动级别，如：#165dff）")
        private String color;
    }
}
