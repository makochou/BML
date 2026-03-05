package com.bml.module.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 新建 API 账号命令对象。
 */
@Data
@Schema(description = "新建 API 账号命令对象")
public class CreateSysApiAccountCommand {

    @NotBlank(message = "账号名称不能为空")
    @Size(max = 100, message = "账号名称长度不能超过100个字符")
    @Schema(description = "账号名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountName;

    @Size(max = 255, message = "账号用途描述长度不能超过255个字符")
    @Schema(description = "账号用途描述，用于说明该账号的业务场景和使用目的")
    private String description;

    @Min(value = 1, message = "账号类型不合法")
    @Max(value = 2, message = "账号类型不合法")
    @Schema(description = "账号类型：1-内部账号，2-外部账号")
    private Integer accountType;

    @Size(max = 8, message = "调用客户端数量不能超过8个")
    @Schema(description = "调用客户端类型代码集合，例如 web、app、mini_program")
    private List<String> clientTypes;

    @NotBlank(message = "接入方负责人不能为空")
    @Size(max = 50, message = "接入方负责人长度不能超过50个字符")
    @Schema(description = "接入方负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerName;

    @NotBlank(message = "联系方式不能为空")
    @Size(max = 100, message = "联系方式长度不能超过100个字符")
    @Schema(description = "负责人联系方式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ownerContact;

    @NotBlank(message = "业务系统名称不能为空")
    @Size(max = 100, message = "业务系统名称长度不能超过100个字符")
    @Schema(description = "业务系统名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String systemName;

    @NotBlank(message = "业务系统编码不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$", message = "业务系统编码仅支持2到64位字母、数字、下划线和中划线")
    @Schema(description = "业务系统编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String systemCode;

    @NotBlank(message = "接入环境不能为空")
    @Schema(description = "接入环境代码：test/staging/production", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accessEnvironment;

    @Schema(description = "IP 白名单，支持单个 IP 或 CIDR")
    private List<String> ipWhitelist;

    @Schema(description = "按环境独立维护的白名单映射，键值固定为 test/staging/production")
    private Map<String, List<String>> environmentIpWhitelist;

    @NotBlank(message = "签名算法版本不能为空")
    @Schema(description = "签名算法版本，例如 v1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signVersion;

    @Size(max = 16, message = "授权范围数量不能超过16个")
    @Schema(description = "授权范围标签集合，例如 read、write、admin")
    private List<String> allowedScopes;

    @Size(max = 255, message = "回调地址长度不能超过255个字符")
    @Schema(description = "业务回调地址")
    private String callbackUrl;

    @Min(value = 1, message = "每分钟限流阈值必须大于0")
    @Schema(description = "每分钟限流阈值")
    private Integer rateLimit;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String remark;
}
