package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * API分组 VO
 *
 * @author BML Team
 */
@Data
@Schema(description = "API分组视图对象")
public class ApiGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
