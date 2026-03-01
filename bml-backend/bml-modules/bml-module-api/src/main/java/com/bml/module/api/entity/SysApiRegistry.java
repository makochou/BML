package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开放接口目录实体。
 * <p>
 * `sys_api_registry` 设计为轻量目录表，仅保留接口识别与展示所需字段，
 * 不包含 `create_by`、`update_by`、`deleted` 等审计/逻辑删除列。
 * 因此这里不能继承通用 `BaseEntity`，否则 MyBatis-Plus 会自动拼出不存在的列。
 */
@Data
@TableName("sys_api_registry")
@Schema(description = "开放接口目录表")
public class SysApiRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "接口名称")
    private String apiName;

    @Schema(description = "接口路径")
    private String apiUrl;

    @Schema(description = "HTTP方法")
    private String httpMethod;

    @Schema(description = "模块名称")
    private String moduleName;

    @Schema(description = "控制器名称")
    private String controllerName;

    @Schema(description = "方法名称")
    private String methodName;

    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "状态：1启用，0停用")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
