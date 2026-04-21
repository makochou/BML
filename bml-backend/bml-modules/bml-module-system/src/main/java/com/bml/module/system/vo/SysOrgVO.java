package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 机构信息 VO
 * <p>
 * 包含完整的企业工商信息，用于前端机构管理界面展示。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "机构信息视图对象")
public class SysOrgVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "机构ID")
    private Long id;

    @Schema(description = "父机构ID")
    private Long parentId;

    @Schema(description = "机构名称")
    private String orgName;

    @Schema(description = "机构编码")
    private String orgCode;

    @Schema(description = "机构类型 (1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部)")
    private Integer orgType;

    @Schema(description = "统一社会信用代码")
    private String creditCode;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "注册资本（万元）")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期")
    private LocalDate establishDate;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经营范围")
    private String businessScope;

    @Schema(description = "状态 (1:正常 0:停用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "数据隔离模式 (0:共享 1:完全隔离 2:汇总共享 3:同级互通 4:按模块隔离)")
    private Integer dataIsolation;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子机构")
    private List<SysOrgVO> children = new ArrayList<>();
}
