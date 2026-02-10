package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * API接口详情 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "API接口详情传输对象")
public class ApiInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "接口ID")
    private Long id;

    @Schema(description = "所属分组ID")
    @NotNull(message = "分组ID不能为空")
    private Long groupId;

    @Schema(description = "接口名称")
    @NotBlank(message = "接口名称不能为空")
    private String name;

    @Schema(description = "请求路径")
    @NotBlank(message = "请求路径不能为空")
    private String path;

    @Schema(description = "请求方法")
    @NotBlank(message = "请求方法不能为空")
    private String method;

    @Schema(description = "状态 (0:草稿 1:发布 2:废弃)")
    private Integer status;

    @Schema(description = "认证方式 (NONE, API_KEY, OAUTH2)")
    private String authType;

    @Schema(description = "请求参数定义(JSON)")
    private String requestParams;

    @Schema(description = "响应参数定义(JSON)")
    private String responseParams;

    @Schema(description = "请求示例")
    private String example;

    @Schema(description = "备注")
    private String remark;
}
