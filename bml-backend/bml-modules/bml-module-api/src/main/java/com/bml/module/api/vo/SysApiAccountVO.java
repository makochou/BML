package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "API账号视图对象")
public class SysApiAccountVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "AccessKey")
    private String accessKey;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
