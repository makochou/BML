package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API应用权限关联表
 *
 * @author BML Team
 */
@Data
@TableName("bml_api_access")
@Schema(description = "API应用权限关联表")
public class ApiAccess {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "应用ID(外键)")
    private Long appId;

    @Schema(description = "API ID(外键)")
    private Long apiId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
