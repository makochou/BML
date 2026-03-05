package com.bml.module.api.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 账号分页查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "API 账号分页查询参数")
public class SysApiAccountPageQuery extends PageQuery {

    @Schema(description = "账号名称，支持模糊匹配")
    private String accountName;

    @Schema(description = "账号用途描述，支持模糊匹配")
    private String description;

    @Schema(description = "账号ID")
    private Long accountId;

    @Schema(description = "AccessKey")
    private String accessKey;

    @Schema(description = "负责人")
    private String ownerName;

    @Schema(description = "负责人联系方式")
    private String ownerContact;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "系统编码")
    private String systemCode;

    @Schema(description = "账号类型：1-内部账号，2-外部账号")
    private Integer accountType;

    @Schema(description = "调用客户端类型代码，例如 web、app、mini_program")
    private String clientType;

    @Schema(description = "业务系统关键字，支持匹配系统名称或系统编码")
    private String systemKeyword;

    @Schema(description = "接入环境代码：test/staging/production")
    private String accessEnvironment;

    @Schema(description = "签名版本")
    private String signVersion;

    @Schema(description = "授权范围标签，精确匹配单个 scope")
    private String allowedScope;

    @Schema(description = "回调地址")
    private String callbackUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "白名单IP关键字")
    private String ipKeyword;

    @Schema(description = "字符匹配模式：fuzzy-模糊，exact-精准")
    private String textMatchMode;

    @Schema(description = "限流下限")
    private Integer rateLimitMin;

    @Schema(description = "限流上限")
    private Integer rateLimitMax;

    @Schema(description = "过期开始时间，格式：yyyy-MM-dd HH:mm:ss")
    private String expireTimeStart;

    @Schema(description = "过期结束时间，格式：yyyy-MM-dd HH:mm:ss")
    private String expireTimeEnd;

    @Schema(description = "创建开始时间，格式：yyyy-MM-dd HH:mm:ss")
    private String createTimeStart;

    @Schema(description = "创建结束时间，格式：yyyy-MM-dd HH:mm:ss")
    private String createTimeEnd;

    @Schema(description = "更新开始时间，格式：yyyy-MM-dd HH:mm:ss")
    private String updateTimeStart;

    @Schema(description = "更新结束时间，格式：yyyy-MM-dd HH:mm:ss")
    private String updateTimeEnd;

    @Schema(description = "状态：1-启用，0-停用")
    private Integer status;
}
