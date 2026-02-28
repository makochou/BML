package com.bml.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警信息 VO。
 * <p>
 * 对外接口统一输出 VO，不直接暴露数据库实体，避免实体结构变更影响前端契约。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "告警信息视图对象")
public class SysAlertVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "告警ID")
    private Long id;

    @Schema(description = "告警类型")
    private String alertType;

    @Schema(description = "告警级别")
    private String alertLevel;

    @Schema(description = "告警标题")
    private String alertTitle;

    @Schema(description = "告警详情内容")
    private String alertContent;

    @Schema(description = "阅读状态 (0:未读, 1:已读)")
    private Integer readStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
