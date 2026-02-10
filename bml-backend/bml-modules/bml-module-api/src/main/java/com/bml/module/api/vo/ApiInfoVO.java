package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * API接口详情 VO
 *
 * @author BML Team
 */
@Data
@Schema(description = "API接口详情视图对象")
public class ApiInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "接口ID")
    private Long id;

    @Schema(description = "所属分组ID")
    private Long groupId;

    @Schema(description = "接口名称")
    private String name;

    @Schema(description = "请求路径")
    private String path;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "认证方式")
    private String authType;

    @Schema(description = "请求参数定义(JSON)")
    private String requestParams;

    @Schema(description = "响应参数定义(JSON)")
    private String responseParams;

    @Schema(description = "请求示例")
    private String example;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
