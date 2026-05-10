package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典类型传输对象。
 *
 * @author BML Team
 */
@Data
@Schema(description = "字典类型传输对象")
public class SysDictTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典类型ID")
    private Long id;

    @Schema(description = "字典名称")
    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100个字符")
    private String dictName;

    @Schema(description = "字典类型编码")
    @NotBlank(message = "字典类型编码不能为空")
    @Size(max = 100, message = "字典类型编码长度不能超过100个字符")
    private String dictType;

    @Schema(description = "状态：1正常 0停用")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    @Schema(description = "页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;
}
