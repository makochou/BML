package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * API 账号授权快照。
 */
@Data
@Schema(description = "API账号授权快照")
public class ApiAccountAuthorizationVO {

    @Schema(description = "账号基础信息")
    private SysApiAccountVO account;

    @Schema(description = "已授权接口ID列表")
    private List<Long> selectedApiIds = new ArrayList<>();

    @Schema(description = "可授权开放接口分组树")
    private List<OpenApiGroupVO> groups = new ArrayList<>();

    @Schema(description = "授权摘要信息")
    private ApiAuthorizationSummaryVO summary;
}
