package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存 API 账号授权关系命令对象。
 */
@Data
@Schema(description = "保存API账号授权关系命令对象")
public class SaveApiAccountAuthorizationCommand {

    @NotNull(message = "授权接口列表不能为空")
    @Schema(description = "授权接口ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> apiIds;
}
