package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API 账号基础视图对象。
 */
@Data
@Schema(description = "API 账号基础视图对象")
public class SysApiAccountVO {

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "账号名称")
    private String accountName;

    @Schema(description = "账号用途描述")
    private String description;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "账号类型：1-内部账号，2-外部账号")
    private Integer accountType;

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

    @Schema(description = "每分钟限流阈值")
    private Integer rateLimit;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "已授权接口数量")
    private Long authorizedApiCount;

    @Schema(description = "已授权且启用的接口数量")
    private Long enabledAuthorizedApiCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
