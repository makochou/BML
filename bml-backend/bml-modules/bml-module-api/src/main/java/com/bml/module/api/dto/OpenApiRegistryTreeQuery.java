package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 开放接口目录树查询参数。
 */
@Data
@Schema(description = "开放接口目录树查询参数")
public class OpenApiRegistryTreeQuery {

    @Schema(description = "关键词，匹配接口名称、路径、描述")
    private String keyword;

    @Schema(description = "HTTP方法")
    private String method;

    @Schema(description = "状态(1正常 0停用)")
    private Integer status;

    @Schema(description = "模块名称")
    private String moduleName;
}
