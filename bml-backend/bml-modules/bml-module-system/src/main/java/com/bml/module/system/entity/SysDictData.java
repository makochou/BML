package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体。
 * <p>
 * 字典数据归属于某个 dictType，用于表达该类型下的实际可选项。
 * 前端表单、筛选条件和状态标签可通过 dictType 动态加载，避免枚举值硬编码。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
@Schema(description = "字典数据实体")
public class SysDictData extends BaseEntity {

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
}
