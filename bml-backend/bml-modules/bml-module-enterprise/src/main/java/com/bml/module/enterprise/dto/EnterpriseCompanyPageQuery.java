package com.bml.module.enterprise.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业档案分页查询参数。
 * <p>
 * 该查询对象作为企业管理前台的开放接口入参骨架，后续切换到真实数据库后仍可直接复用。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "企业档案分页查询参数")
public class EnterpriseCompanyPageQuery extends PageQuery {

    @Schema(description = "关键字，支持企业名称、企业编码、联系人模糊匹配")
    private String keyword;

    @Schema(description = "企业状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "所属行业")
    private String industry;
}
