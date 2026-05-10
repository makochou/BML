package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典数据视图对象。
 *
 * @author BML Team
 */
@Data
@Schema(description = "字典数据视图对象")
public class SysDictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字典数据ID")
    private Long id;

    @Schema(description = "字典类型编码")
    private String dictType;

    @Schema(description = "字典标签")
    private String dictLabel;

    @Schema(description = "字典键值")
    private String dictValue;

    @Schema(description = "CSS样式类名")
    private String cssClass;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态：1正常 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
