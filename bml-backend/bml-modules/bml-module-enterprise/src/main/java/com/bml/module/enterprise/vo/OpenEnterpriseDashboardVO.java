package com.bml.module.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业管理业务概览对象。
 */
@Data
@Schema(description = "企业管理业务概览对象")
public class OpenEnterpriseDashboardVO {

    @Schema(description = "企业总数")
    private Integer companyCount;

    @Schema(description = "启用企业数")
    private Integer enabledCompanyCount;

    @Schema(description = "系统账号总数")
    private Integer systemAccountCount;

    @Schema(description = "启用系统账号数")
    private Integer enabledSystemAccountCount;
}
