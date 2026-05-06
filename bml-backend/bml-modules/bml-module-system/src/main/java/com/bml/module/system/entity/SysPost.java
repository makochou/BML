package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位表
 * <p>
 * 岗位是组织架构中的职务标识，用于标记用户在机构/部门中所担任的角色，
 * 如"总经理""开发工程师""产品经理"等。岗位为扁平列表结构（非树形）。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
@Schema(description = "岗位表")
public class SysPost extends BaseEntity {

    @Schema(description = "岗位编码（全局唯一）")
    private String postCode;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "所属机构ID（NULL 表示全局岗位）")
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long orgId;

    @Schema(description = "岗位类别 (管理类/技术类/行政类/财务类/销售类/生产类)")
    private String postCategory;

    @Schema(description = "岗位级别 (如 P1~P10 技术序列, M1~M5 管理序列)")
    private String postLevel;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
