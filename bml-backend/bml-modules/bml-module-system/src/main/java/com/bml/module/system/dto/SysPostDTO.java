package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 岗位信息 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "岗位信息传输对象")
public class SysPostDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "岗位ID")
    private Long id;

    @Schema(description = "岗位编码")
    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过64个字符")
    private String postCode;

    @Schema(description = "岗位名称")
    @NotBlank(message = "岗位名称不能为空")
    @Size(min = 1, max = 50, message = "岗位名称长度必须在1到50个字符之间")
    private String postName;

    @Schema(description = "所属机构ID（NULL 表示全局岗位）")
    private Long orgId;

    @Schema(description = "岗位类别 (管理类/技术类/行政类/财务类/销售类/生产类)")
    @Size(max = 20, message = "岗位类别长度不能超过20个字符")
    private String postCategory;

    @Schema(description = "岗位级别 (如 P1~P10 / M1~M5)")
    @Size(max = 20, message = "岗位级别长度不能超过20个字符")
    private String postLevel;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
