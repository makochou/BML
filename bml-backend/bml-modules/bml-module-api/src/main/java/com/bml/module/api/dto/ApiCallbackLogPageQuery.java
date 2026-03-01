package com.bml.module.api.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 回调日志分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "API 回调日志分页查询参数")
public class ApiCallbackLogPageQuery extends PageQuery {

    @Schema(description = "回调状态：0待执行 1重试中 2成功 3失败")
    private Integer callbackStatus;
}
