package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API注册实体
 * 对应表: sys_api_registry
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api_registry")
@Schema(description = "API注册表")
public class SysApiRegistry extends BaseEntity {

    @Schema(description = "API名称")
    private String apiName;

    @Schema(description = "API路径")
    private String apiUrl;

    @Schema(description = "HTTP方法")
    private String httpMethod;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "控制器名称")
    private String controllerName;

    @Schema(description = "方法名称")
    private String methodName;

    @Schema(description = "API描述")
    private String description;

    @Schema(description = "状态(1正常 0停用)")
    private Integer status;
}
