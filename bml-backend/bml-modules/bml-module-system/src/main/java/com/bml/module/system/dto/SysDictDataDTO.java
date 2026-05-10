package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典数据传输对象。
 *
 * @author BML Team
 */
@Data
@Schema(description = "字典数据传输对象")
public class SysDictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典数据ID")
    private Long id;

    @Schema(description = "字典类型编码")
    @NotBlank(message = "字典类型编码不能为空")
    @Size(max = 100, message = "字典类型编码长度不能超过100个字符")
    private String dictType;

    @Schema(description = "字典标签")
    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过100个字符")
    private String dictLabel;

    @Schema(description = "字典键值")
    @NotBlank(message = "字典键值不能为空")
    @Size(max = 100, message = "字典键值长度不能超过100个字符")
    private String dictValue;

    @Schema(description = "CSS样式类名")
    @Size(max = 100, message = "CSS样式类名长度不能超过100个字符")
    private String cssClass;

    @Schema(description = "显示顺序")
    private Integer sort;

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
