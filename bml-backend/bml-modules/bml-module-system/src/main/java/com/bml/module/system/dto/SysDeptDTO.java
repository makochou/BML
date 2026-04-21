package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门信息 DTO
 *
 * @author BML Team
 */
@Data
@Schema(description = "部门信息传输对象")
public class SysDeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID（更新时必填）")
    private Long id;

    @Schema(description = "父部门ID（0 表示顶级部门）")
    private Long parentId;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "部门名称")
    @NotBlank(message = "部门名称不能为空")
    @Size(min = 1, max = 100, message = "部门名称长度必须在1到100个字符之间")
    private String deptName;

    @Schema(description = "部门编码")
    @Size(max = 50, message = "部门编码长度不能超过50个字符")
    private String deptCode;

    @Schema(description = "部门类型 (1:事业部 2:中心 3:部门 4:小组)")
    private Integer deptType;

    @Schema(description = "职能分类 (管理/研发/销售/财务/人事/行政/生产/采购/仓储)")
    @Size(max = 20, message = "职能分类长度不能超过20个字符")
    private String funcType;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "负责人")
    @Size(max = 50, message = "负责人长度不能超过50个字符")
    private String leader;

    @Schema(description = "联系电话")
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;
}
