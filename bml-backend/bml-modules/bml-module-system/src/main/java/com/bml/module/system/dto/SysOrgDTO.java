package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 机构信息 DTO
 * <p>
 * 用于机构新增/编辑请求的数据传输对象，包含完整的企业工商信息字段。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "机构信息传输对象")
public class SysOrgDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "机构ID（更新时必填）")
    private Long id;

    @Schema(description = "父机构ID（0 表示顶级机构）")
    private Long parentId;

    @Schema(description = "机构名称")
    @NotBlank(message = "机构名称不能为空")
    @Size(min = 1, max = 100, message = "机构名称长度必须在1到100个字符之间")
    private String orgName;

    @Schema(description = "机构编码")
    @Size(max = 50, message = "机构编码长度不能超过50个字符")
    private String orgCode;

    @Schema(description = "机构类型 (1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部)")
    private Integer orgType;

    @Schema(description = "统一社会信用代码（18位）")
    @Size(max = 18, message = "统一社会信用代码长度不能超过18个字符")
    private String creditCode;

    @Schema(description = "法定代表人")
    @Size(max = 50, message = "法定代表人长度不能超过50个字符")
    private String legalPerson;

    @Schema(description = "注册资本（万元）")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期")
    private LocalDate establishDate;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "负责人")
    @Size(max = 50, message = "负责人长度不能超过50个字符")
    private String leader;

    @Schema(description = "联系电话")
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Schema(description = "邮箱")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "详细地址")
    @Size(max = 200, message = "详细地址长度不能超过200个字符")
    private String address;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    @Schema(description = "数据隔离模式 (0:共享 1:完全隔离 2:汇总共享 3:同级互通 4:按模块隔离)")
    private Integer dataIsolation;
}
