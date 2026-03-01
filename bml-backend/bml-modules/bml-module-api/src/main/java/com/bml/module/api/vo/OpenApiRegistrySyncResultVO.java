package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开放接口目录同步结果。
 */
@Data
@Schema(description = "开放接口目录同步结果")
public class OpenApiRegistrySyncResultVO {

    @Schema(description = "本次发现的开放接口数量")
    private long totalDiscovered;

    @Schema(description = "新增记录数")
    private long insertedCount;

    @Schema(description = "更新记录数")
    private long updatedCount;

    @Schema(description = "禁用记录数")
    private long disabledCount;

    @Schema(description = "跳过记录数")
    private long skippedCount;
}
