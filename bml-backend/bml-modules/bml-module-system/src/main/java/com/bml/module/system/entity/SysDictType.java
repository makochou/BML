package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型实体。
 * <p>
 * 字典类型用于定义一组可复用的业务枚举分类，例如用户性别、启用状态、岗位类别等。
 * dictType 作为程序读取字典数据的稳定编码，必须全局唯一。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
@Schema(description = "字典类型实体")
public class SysDictType extends BaseEntity {

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型编码")
    private String dictType;

    @Schema(description = "状态：1正常 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
