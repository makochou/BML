package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户个人数据权限配置表
 * <p>
 * 允许对特定用户设置独立的数据权限，优先级高于角色的 data_scope。
 * </p>
 * <p>
 * <b>适用场景：</b>
 * <ul>
 *   <li>某员工临时借调到其他部门，需要额外查看借调部门的数据</li>
 *   <li>总经理助理需要跨部门查看汇总数据</li>
 *   <li>外部审计人员需要临时查看特定范围数据</li>
 * </ul>
 * </p>
 * <p>
 * <b>优先级规则：</b>
 * <ol>
 *   <li>若用户有 sys_user_data_scope 记录且 status=1 且未过期，以此为准</li>
 *   <li>否则以用户所有角色中最宽的 data_scope 为准（取并集）</li>
 * </ol>
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_data_scope")
@Schema(description = "用户个人数据权限配置表")
public class SysUserDataScope extends BaseEntity {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "数据范围 (1:全部 2:本机构及下级 3:仅本机构 4:本部门及下级 5:仅本部门 6:仅本人 7:自定义 8:本人及下属)")
    private Integer dataScope;

    @Schema(description = "自定义可访问机构ID列表（逗号分隔，data_scope=7时生效）")
    private String customOrgIds;

    @Schema(description = "自定义可访问部门ID列表（逗号分隔，data_scope=7时生效）")
    private String customDeptIds;

    @Schema(description = "状态 (1:生效 0:停用)")
    private Integer status;

    @Schema(description = "过期时间（NULL表示永不过期，用于临时授权场景）")
    private LocalDateTime expireTime;

    @Schema(description = "备注（记录授权原因，如：临时借调至XX部门）")
    private String remark;
}
