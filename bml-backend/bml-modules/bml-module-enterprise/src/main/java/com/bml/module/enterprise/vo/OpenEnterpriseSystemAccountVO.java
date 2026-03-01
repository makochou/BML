package com.bml.module.enterprise.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业系统账号开放接口视图对象。
 */
@Data
@Schema(description = "企业系统账号开放接口对象")
public class OpenEnterpriseSystemAccountVO {

    @Schema(description = "系统账号ID")
    private Long accountId;

    @Schema(description = "所属企业ID")
    private Long companyId;

    @Schema(description = "所属企业名称")
    private String companyName;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "账号类型")
    private String accountType;

    @Schema(description = "账号状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "角色摘要")
    private String roleSummary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最近登录时间")
    private LocalDateTime lastLoginTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
