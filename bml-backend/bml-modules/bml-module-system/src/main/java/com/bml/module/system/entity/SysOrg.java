package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 机构表
 * <p>
 * 机构为树形结构，支持无限层级嵌套。机构是部门的上级组织层级，
 * 一个机构下可以包含多个部门，如"集团总部"下设"财务部""研发部"等。
 * </p>
 * <p>
 * <b>机构类型说明：</b>
 * <ul>
 *   <li>1 — 集团：最顶层组织，可查看所有下属机构数据</li>
 *   <li>2 — 公司：独立法人实体</li>
 *   <li>3 — 分公司：非独立法人，隶属于公司</li>
 *   <li>4 — 子公司：独立法人，隶属于集团/公司</li>
 *   <li>5 — 办事处：区域办公场所</li>
 *   <li>6 — 事业部：内部业务单元</li>
 * </ul>
 * </p>
 * <p>
 * <b>数据隔离模式（data_isolation）：</b>
 * <ul>
 *   <li>0 — 共享：上级机构可查看所有下级机构数据，适用于集团统一管控</li>
 *   <li>1 — 完全隔离：各机构数据完全独立，即使存在上下级关系也不可互查</li>
 *   <li>2 — 汇总共享：上级仅可查看下级的汇总/统计数据，不可查看明细</li>
 *   <li>3 — 同级互通：同一父机构下的兄弟机构可互查数据，上级也可查看</li>
 *   <li>4 — 按模块隔离：部分业务模块隔离，部分共享（需配合模块配置表）</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
@Schema(description = "机构表")
public class SysOrg extends BaseEntity {

    @Schema(description = "父机构ID（0 表示顶级机构）")
    private Long parentId;

    @Schema(description = "祖级列表（逗号分隔，用于快速查询子树，如 0,100）")
    private String ancestors;

    @Schema(description = "机构名称")
    private String orgName;

    @Schema(description = "机构编码（全局唯一）")
    private String orgCode;

    @Schema(description = "机构类型 (1:集团 2:公司 3:分公司 4:子公司 5:办事处 6:事业部)")
    private Integer orgType;

    @Schema(description = "统一社会信用代码（18位）")
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

    @Schema(description = "子机构")
    @TableField(exist = false)
    private List<SysOrg> children = new ArrayList<>();
}
