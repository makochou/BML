package com.bml.module.api.vo;

import com.bml.core.common.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * API 回调日志分页与汇总响应对象。
 */
@Data
@Builder
@Schema(description = "API 回调日志分页与汇总响应对象")
public class ApiCallbackLogPageVO {

    @Schema(description = "分页数据")
    private PageResult<ApiCallbackLogVO> page;

    @Schema(description = "统计汇总")
    private ApiCallbackLogSummaryVO summary;
}
