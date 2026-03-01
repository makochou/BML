package com.bml.module.enterprise.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业系统账号分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "企业系统账号分页查询参数")
public class EnterpriseSystemAccountPageQuery extends PageQuery {

    @Schema(description = "关键字，支持账号、姓名、所属企业模糊匹配")
    private String keyword;

    @Schema(description = "账号状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "账号类型，例如：admin、operator、auditor")
    private String accountType;

    @Schema(description = "所属企业ID")
    private Long companyId;
}
