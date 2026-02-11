package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API接口详情表
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bml_api_info")
@Schema(description = "API接口详情表")
public class ApiInfo extends BaseEntity {

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "所属控制器类名")
    private String controller;

    @Schema(description = "接口名称")
    private String name;

    @Schema(description = "权限标识")
    private String permFlag;

    @Schema(description = "请求路径")
    private String path;

    @Schema(description = "请求方法(GET, POST, PUT, DELETE)")
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
