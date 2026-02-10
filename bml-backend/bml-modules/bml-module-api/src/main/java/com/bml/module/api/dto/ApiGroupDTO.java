package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * API分组 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "API分组传输对象")
public class ApiGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分组ID")
    private Long id;

    @Schema(description = "分组名称")
    @NotBlank(message = "分组名称不能为空")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;
}
