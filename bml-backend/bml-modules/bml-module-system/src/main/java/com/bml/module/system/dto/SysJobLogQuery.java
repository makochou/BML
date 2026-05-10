package com.bml.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务日志查询参数")
public class SysJobLogQuery {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String jobName;

    private String jobGroup;

    private Integer status;
}
