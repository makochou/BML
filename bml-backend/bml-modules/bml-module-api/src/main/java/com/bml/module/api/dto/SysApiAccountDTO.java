package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API 账号传输对象。
 * <p>
 * 该对象主要用于兼容旧接口，字段需要与新接口治理字段保持一致，
 * 这样旧路径和新路径都能共享同一套账号模型。
 * </p>
 */
@Data
@Schema(description = "API 账号传输对象")
public class SysApiAccountDTO {

    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "账号类型：1-内部账号，2-外部账号")
    private Integer accountType;

    @Schema(description = "调用客户端类型代码集合")
    private List<String> clientTypes;

    @Schema(description = "查询时使用的单个客户端类型代码")
    private String clientType;

    @Schema(description = "接入方负责人")
    private String ownerName;

    @Schema(description = "负责人联系方式")
    private String ownerContact;

    @Schema(description = "业务系统名称")
    private String systemName;

    @Schema(description = "业务系统编码")
    private String systemCode;

    @Schema(description = "业务系统关键字，用于兼容旧接口查询系统名称或系统编码")
    private String systemKeyword;

    @Schema(description = "接入环境代码：test/staging/production")
    private String accessEnvironment;

    @Schema(description = "IP 白名单，支持单个 IP 或 CIDR")
    private List<String> ipWhitelist;

    @Schema(description = "按环境独立维护的白名单映射，键值固定为 test/staging/production")
    private Map<String, List<String>> environmentIpWhitelist;

    @Schema(description = "签名算法版本，例如 v1")
    private String signVersion;

    @Schema(description = "业务回调地址")
    private String callbackUrl;

    @Schema(description = "每分钟限流阈值")
    private Integer rateLimit;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
