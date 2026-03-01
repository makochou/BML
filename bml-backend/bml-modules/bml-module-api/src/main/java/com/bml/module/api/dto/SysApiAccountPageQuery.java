package com.bml.module.api.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 账号分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "API 账号分页查询参数")
public class SysApiAccountPageQuery extends PageQuery {

    @Schema(description = "账号名称，支持模糊匹配")
    private String accountName;

    @Schema(description = "账号类型：1-内部账号，2-外部账号")
    private Integer accountType;

    @Schema(description = "调用客户端类型代码，例如 web、app、mini_program")
    private String clientType;

    @Schema(description = "业务系统关键字，支持匹配系统名称或系统编码")
    private String systemKeyword;

    @Schema(description = "接入环境代码：test/staging/production")
    private String accessEnvironment;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
