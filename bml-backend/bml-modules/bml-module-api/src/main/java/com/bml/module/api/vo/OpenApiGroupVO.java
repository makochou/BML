package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 开放接口模块分组。
 */
@Data
@Schema(description = "开放接口模块分组")
public class OpenApiGroupVO {

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "模块下的控制器分组")
    private List<OpenApiControllerGroupVO> controllers = new ArrayList<>();
}
