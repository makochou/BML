package com.bml.module.enterprise.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业档案开放接口视图对象。
 */
@Data
@Schema(description = "企业档案开放接口对象")
public class OpenEnterpriseCompanyVO {

    @Schema(description = "企业ID")
    private Long companyId;

    @Schema(description = "企业编码")
    private String companyCode;

    @Schema(description = "企业名称")
    private String companyName;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "所在城市")
    private String city;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "企业状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "员工数")
    private Integer memberCount;

    @Schema(description = "系统账号数")
    private Integer systemAccountCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近更新时间")
    private LocalDateTime updatedTime;
}
