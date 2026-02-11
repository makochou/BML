package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API应用管理表
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bml_api_app")
@Schema(description = "API应用管理表")
public class ApiApp extends BaseEntity {

    @Schema(description = "应用ID(AppKey)")
    private String appId;

    @Schema(description = "应用密钥(AppSecret)")
    private String appSecret;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "状态(1正常 0停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
