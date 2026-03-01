package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 开放接口控制器分组。
 */
@Data
@Schema(description = "开放接口控制器分组")
public class OpenApiControllerGroupVO {

    @Schema(description = "控制器名称")
    private String controllerName;

    @Schema(description = "控制器下的开放接口列表")
    private List<OpenApiRegistryItemVO> apis = new ArrayList<>();
}
