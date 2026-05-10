package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 审计日志归档策略视图对象。
 * <p>
 * 当前版本先通过系统配置表维护在线保留周期、冷归档开关和归档路径，后续如接入对象存储或离线任务，可保持该接口契约不变。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "审计日志归档策略视图对象")
public class AuditArchiveSettingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "在线保留天数，默认180天")
    private Integer onlineRetentionDays;

    @Schema(description = "是否启用自动归档")
    private Boolean archiveEnabled;

    @Schema(description = "冷归档存储位置")
    private String archiveStorage;

    @Schema(description = "是否启用自动清理")
    private Boolean autoCleanEnabled;
}
