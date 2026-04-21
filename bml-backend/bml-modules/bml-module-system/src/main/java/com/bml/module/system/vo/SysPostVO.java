package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位信息 VO
 *
 * @author BML Team
 */
@Data
@Schema(description = "岗位信息视图对象")
public class SysPostVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "岗位ID")
    private Long id;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "所属机构名称")
    private String orgName;

    @Schema(description = "岗位类别")
    private String postCategory;

    @Schema(description = "岗位级别")
    private String postLevel;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
