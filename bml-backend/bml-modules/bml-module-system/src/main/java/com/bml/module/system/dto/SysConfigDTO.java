package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 参数配置传输对象。
 * <p>
 * 用于系统参数配置的分页查询、新增和修改。configKey 是程序读取配置的稳定键名，必须全局唯一。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "参数配置传输对象")
public class SysConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置名称")
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过100个字符")
    private String configName;

    @Schema(description = "配置键名")
    @NotBlank(message = "配置键名不能为空")
    @Size(max = 100, message = "配置键名长度不能超过100个字符")
    private String configKey;

    @Schema(description = "配置键值")
    @NotBlank(message = "配置键值不能为空")
    @Size(max = 500, message = "配置键值长度不能超过500个字符")
    private String configValue;

    @Schema(description = "是否系统内置：1是 0否")
    private Integer configType;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    @Schema(description = "页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;
}
