package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * API 回调日志汇总对象。
 */
@Data
@Builder
@Schema(description = "API 回调日志汇总对象")
public class ApiCallbackLogSummaryVO {

    @Schema(description = "日志总数")
    private long totalCount;

    @Schema(description = "待执行数量")
    private long pendingCount;

    @Schema(description = "重试中数量")
    private long retryingCount;

    @Schema(description = "成功数量")
    private long successCount;

    @Schema(description = "失败数量")
    private long failedCount;
}
