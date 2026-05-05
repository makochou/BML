package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门表
 * <p>
 * 部门归属于机构，支持多级树形结构。
 * </p>
 * <p>
 * <b>部门类型说明：</b>
 * <ul>
 *   <li>1 — 事业部：大的业务板块</li>
 *   <li>2 — 中心：职能管理中心（如研发中心、运营中心）</li>
 *   <li>3 — 部门：标准部门（默认）</li>
 *   <li>4 — 小组：最小组织单元</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
@Schema(description = "部门表")
public class SysDept extends BaseEntity {

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编码（全局唯一）")
    private String deptCode;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "所属机构名称（非持久化字段，由服务层根据 orgId 批量填充）")
    @TableField(exist = false)
    private String orgName;

    @Schema(description = "父部门ID（0 表示顶级部门）")
    private Long parentId;

    @Schema(description = "祖级列表（逗号分隔）")
    private String ancestors;

    @Schema(description = "部门类型 (1:事业部 2:中心 3:部门 4:小组)")
    private Integer deptType;

    @Schema(description = "职能分类 (管理/研发/销售/财务/人事/行政/生产/采购/仓储)")
    private String funcType;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "子部门")
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
