package com.bml.module.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bml.core.base.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * API 账号实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_api_account")
@Schema(description = "API 账号表")
public class SysApiAccount extends BaseEntity {

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "SecretKey，加密后存储")
    private String secretKey;

    @Schema(description = "账号类型：1-内部账号，2-外部账号")
    private Integer accountType;

    @Schema(description = "调用客户端类型，使用英文代码并以逗号分隔存储")
    private String clientTypes;

    @Schema(description = "接入方负责人")
    private String ownerName;

    @Schema(description = "负责人联系方式")
    private String ownerContact;

    @Schema(description = "业务系统名称")
    private String systemName;

    @Schema(description = "业务系统编码")
    private String systemCode;

    @Schema(description = "接入环境代码：test/staging/production")
    private String accessEnvironment;

    @Schema(description = "IP 白名单，使用英文逗号分隔存储单个 IP 或 CIDR")
    private String ipWhitelist;

    @Schema(description = "测试环境 IP 白名单，使用英文逗号分隔存储单个 IP 或 CIDR")
    private String testIpWhitelist;

    @Schema(description = "预发环境 IP 白名单，使用英文逗号分隔存储单个 IP 或 CIDR")
    private String stagingIpWhitelist;

    @Schema(description = "生产环境 IP 白名单，使用英文逗号分隔存储单个 IP 或 CIDR")
    private String productionIpWhitelist;

    @Schema(description = "签名算法版本，例如 v1")
    private String signVersion;

    @Schema(description = "业务回调地址")
    private String callbackUrl;

    @Schema(description = "每分钟限流阈值")
    private Integer rateLimit;

    @Schema(description = "过期时间，为空表示不过期")
    private LocalDateTime expireTime;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
