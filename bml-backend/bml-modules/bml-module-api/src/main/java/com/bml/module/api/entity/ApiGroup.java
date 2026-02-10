package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API分组表
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bml_api_group")
@Schema(description = "API分组表")
public class ApiGroup extends BaseEntity {

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;
}
