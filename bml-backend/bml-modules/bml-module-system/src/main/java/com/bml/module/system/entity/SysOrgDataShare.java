package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 机构数据共享规则表
 * <p>
 * 当机构设置为「完全隔离」或其他隔离模式时，仍可通过本表配置定向数据共享规则，
 * 将源机构的数据有选择性地开放给目标机构查看。
 * </p>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>子公司A 将财务报表共享给集团总部查看（只读）</li>
 *   <li>分公司B 将客户资源共享给兄弟分公司C（按模块共享）</li>
 *   <li>事业部D 临时将项目数据共享给外部审计机构（有截止时间）</li>
 * </ul>
 * </p>
 * <p>
 * <b>共享类型说明：</b>
 * <ul>
 *   <li>1 — 全模块共享：目标机构可查看源机构所有业务模块数据</li>
 *   <li>2 — 指定模块共享：仅共享 module_code 指定的模块数据</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org_data_share")
@Schema(description = "机构数据共享规则表")
public class SysOrgDataShare extends BaseEntity {

    @Schema(description = "源机构ID（数据所属机构）")
    private Long sourceOrgId;

    @Schema(description = "目标机构ID（被共享的机构）")
    private Long targetOrgId;

    @Schema(description = "共享类型 (1:全模块共享 2:指定模块共享)")
    private Integer shareType;

    @Schema(description = "模块编码（share_type=2时有效，多个用逗号分隔，如 finance,inventory）")
    private String moduleCode;

    @Schema(description = "权限级别 (1:只读 2:读写)")
    private Integer permission;

    @Schema(description = "状态 (1:生效 0:停用)")
    private Integer status;

    @Schema(description = "过期时间（NULL表示永不过期）")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;
}
