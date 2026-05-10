package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 参数配置视图对象。
 *
 * @author BML Team
 */
@Data
@Schema(description = "参数配置视图对象")
public class SysConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置键名")
    private String configKey;

    @Schema(description = "配置键值")
    private String configValue;

    @Schema(description = "是否系统内置：1是 0否")
    private Integer configType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
