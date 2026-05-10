package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_log")
@Schema(description = "任务日志实体")
public class SysJobLog extends BaseEntity {

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "日志信息")
    private String jobMessage;

    @Schema(description = "执行状态")
    private Integer status;

    @Schema(description = "异常信息")
    private String exceptionInfo;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "耗时毫秒")
    private Long costTime;
}
