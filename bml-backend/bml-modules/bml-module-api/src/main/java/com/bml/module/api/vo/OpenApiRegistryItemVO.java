package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 可授权开放接口叶子节点。
 */
@Data
@Schema(description = "可授权开放接口叶子节点")
public class OpenApiRegistryItemVO {

    @Schema(description = "接口ID")
    private Long id;

    @Schema(description = "接口名称")
    private String apiName;

    @Schema(description = "接口路径")
    private String apiUrl;

    @Schema(description = "HTTP方法")
    private String httpMethod;

    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "状态(1正常 0停用)")
    private Integer status;
}
