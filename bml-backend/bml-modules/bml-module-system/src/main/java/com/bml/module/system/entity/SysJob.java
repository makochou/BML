package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
@Schema(description = "定时任务实体")
public class SysJob extends BaseEntity {

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "Cron 表达式")
    private String cronExpression;

    @Schema(description = "错过策略")
    private Integer misfirePolicy;

    @Schema(description = "是否并发")
    private Integer concurrent;

    @Schema(description = "任务状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
