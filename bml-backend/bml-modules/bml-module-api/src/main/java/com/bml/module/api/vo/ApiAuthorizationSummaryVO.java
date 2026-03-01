package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 账号授权摘要信息。
 */
@Data
@Schema(description = "API账号授权摘要信息")
public class ApiAuthorizationSummaryVO {

    @Schema(description = "开放接口总数")
    private long totalApiCount;

    @Schema(description = "启用中的开放接口总数")
    private long enabledApiCount;

    @Schema(description = "已授权接口总数")
    private long selectedApiCount;

    @Schema(description = "已授权且启用的接口总数")
    private long selectedEnabledApiCount;
}
