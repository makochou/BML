package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 日志审计中心概览视图对象。
 * <p>
 * 用于审计中心首页展示登录、操作、异常、风险告警和归档策略的关键指标，帮助管理员快速判断系统风险态势。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "日志审计中心概览视图对象")
public class AuditOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "操作日志总数")
    private Long operationTotal;

    @Schema(description = "异常操作日志总数")
    private Long operationErrorTotal;

    @Schema(description = "登录日志总数")
    private Long loginTotal;

    @Schema(description = "登录失败总数")
    private Long loginFailureTotal;

    @Schema(description = "安全告警总数")
    private Long alertTotal;

    @Schema(description = "未读安全告警总数")
    private Long unreadAlertTotal;

    @Schema(description = "在线保留天数")
    private Integer onlineRetentionDays;

    @Schema(description = "是否启用自动归档")
    private Boolean archiveEnabled;
}
