package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 系统告警通知实体类
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_alert")
@Schema(description = "系统告警通知实体")
public class SysAlert extends BaseEntity {

    @Schema(description = "告警类型 (例如: CPU_HIGH, MEMORY_HIGH, DISK_FULL, JVM_HIGH)")
    private String alertType;

    @Schema(description = "告警级别 (例如: info, warning, error, critical)")
    private String alertLevel;

    @Schema(description = "告警标题")
    private String alertTitle;

    @Schema(description = "告警详情内容")
    private String alertContent;

    @Schema(description = "阅读状态 (0:未读, 1:已读)")
    private Integer readStatus;

}
