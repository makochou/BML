package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "定时任务传输对象")
public class SysJobDTO {

    private Long id;

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    private String jobGroup;

    @NotBlank(message = "调用目标不能为空")
    private String invokeTarget;

    @NotBlank(message = "Cron 表达式不能为空")
    private String cronExpression;

    private Integer misfirePolicy;

    private Integer concurrent;

    @NotNull(message = "任务状态不能为空")
    private Integer status;

    private String remark;
}
