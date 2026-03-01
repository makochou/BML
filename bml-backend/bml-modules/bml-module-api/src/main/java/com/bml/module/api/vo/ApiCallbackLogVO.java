package com.bml.module.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 回调日志视图对象。
 */
@Data
@Schema(description = "API 回调日志视图对象")
public class ApiCallbackLogVO {

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "API 账号 ID")
    private Long accountId;

    @Schema(description = "账号名称快照")
    private String accountName;

    @Schema(description = "业务系统名称快照")
    private String systemName;

    @Schema(description = "业务系统编码快照")
    private String systemCode;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "回调地址")
    private String callbackUrl;

    @Schema(description = "HTTP 方法")
    private String httpMethod;

    @Schema(description = "请求头 JSON")
    private String requestHeaders;

    @Schema(description = "请求体")
    private String requestBody;

    @Schema(description = "响应状态码")
    private Integer responseStatusCode;

    @Schema(description = "响应体")
    private String responseBody;

    @Schema(description = "回调状态：0待执行 1重试中 2成功 3失败")
    private Integer callbackStatus;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "最近一次回调时间")
    private LocalDateTime lastCallbackTime;

    @Schema(description = "成功时间")
    private LocalDateTime successTime;

    @Schema(description = "最近一次错误信息")
    private String lastErrorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
