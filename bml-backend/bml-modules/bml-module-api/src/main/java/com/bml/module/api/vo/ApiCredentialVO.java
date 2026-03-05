package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * API 凭证一次性展示对象。
 */
@Data
@Builder
@Schema(description = "API 凭证一次性展示对象")
public class ApiCredentialVO {

    @Schema(description = "账号 ID")
    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "账号用途描述")
    private String description;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "调用客户端类型代码集合")
    private List<String> clientTypes;

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

    @Schema(description = "IP 白名单列表")
    private List<String> ipWhitelist;

    @Schema(description = "按环境独立维护的白名单映射")
    private Map<String, List<String>> environmentIpWhitelist;

    @Schema(description = "签名算法版本，例如 v1")
    private String signVersion;

    @Schema(description = "授权范围标签集合")
    private List<String> allowedScopes;

    @Schema(description = "业务回调地址")
    private String callbackUrl;

    @Schema(description = "SecretKey，仅在创建或重置时返回一次")
    private String secretKey;
}
