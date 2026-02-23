package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API账号传输对象
 *
 * @author BML Team
 */
@Data
@Schema(description = "API账号传输对象")
public class SysApiAccountDTO {

    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "账号类型(1内部 2外部)")
    private Integer accountType;

    @Schema(description = "请求限流(次/分钟)")
    private Integer rateLimit;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "状态(1正常 0禁用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
