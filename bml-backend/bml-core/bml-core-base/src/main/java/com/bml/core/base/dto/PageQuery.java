package com.bml.core.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询参数
 *
 * @author BML Team
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段")
    private String orderByColumn;

    @Schema(description = "排序方式 (asc/desc)", defaultValue = "asc")
    private String isAsc = "asc";
}
