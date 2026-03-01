package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * API 账号状态修改命令对象。
 */
@Data
@Schema(description = "API账号状态修改命令对象")
public class UpdateSysApiAccountStatusCommand {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    @Schema(description = "状态(1正常 0禁用)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
