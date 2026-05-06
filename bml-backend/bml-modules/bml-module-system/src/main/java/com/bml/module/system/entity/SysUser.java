package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户信息表
 * <p>
 * 对应数据库表 {@code sys_user}，继承 {@link BaseEntity} 获得通用审计字段。
 * </p>
 * <p>
 * <b>状态约定：</b> status 字段值 1 表示正常，0 表示停用，2 表示锁定。
 * 参见 {@link com.bml.core.common.constant.GlobalConstants#STATUS_NORMAL}。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "用户信息表")
public class SysUser extends BaseEntity {

    @Schema(description = "账号")
    private String username;

    /**
     * 密码（BCrypt 加密存储）
     * <p>
     * 使用 {@code @JsonIgnore} 确保序列化时不暴露密码哈希到前端。
     * </p>
     */
    @Schema(description = "密码", hidden = true)
    @JsonIgnore
    private String password;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "用户名")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "性别 (0:未知 1:男 2:女)")
    private Integer gender;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "状态 (1:正常 0:停用 2:锁定)")
    private Integer status;

    @Schema(description = "所属机构ID")
    private Long orgId;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "直属上级用户ID（构建汇报链，NULL表示无上级）")
    private Long superiorId;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "入职日期")
    private LocalDate entryDate;

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginDate;

    @Schema(description = "备注")
    private String remark;

}
