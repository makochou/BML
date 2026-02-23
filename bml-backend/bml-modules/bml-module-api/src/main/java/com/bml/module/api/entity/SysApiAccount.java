package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * API账号实体
 * 对应表: sys_api_account
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api_account")
@Schema(description = "API账号表")
public class SysApiAccount extends BaseEntity {

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "SecretKey(加密存储)")
    private String secretKey;

    @Schema(description = "账号类型(1内部 2外部)")
    private Integer accountType;

    @Schema(description = "请求限流(次/分钟)")
    private Integer rateLimit;

    @Schema(description = "过期时间(NULL为永久)")
    private LocalDateTime expireTime;

    @Schema(description = "状态(1正常 0禁用)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
