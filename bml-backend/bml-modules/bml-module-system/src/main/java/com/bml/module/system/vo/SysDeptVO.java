package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门信息 VO
 *
 * @author BML Team
 */
@Data
@Schema(description = "部门信息视图对象")
public class SysDeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "所属机构名称")
    private String orgName;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "部门编码")
    private String deptCode;

    @Schema(description = "部门类型 (1:事业部 2:中心 3:部门 4:小组)")
    private Integer deptType;

    @Schema(description = "职能分类")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子部门")
    private List<SysDeptVO> children = new ArrayList<>();
}
