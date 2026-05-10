package com.bml.module.system.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 系统操作日志分页查询参数。
 * <p>
 * 查询字段按审计场景设计，覆盖模块、操作人、业务类型、请求方式、执行状态、来源 IP 和时间范围等高频过滤维度。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统操作日志分页查询参数")
public class SysOperationLogQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "业务类型")
    private Integer businessType;

    @Schema(description = "HTTP 请求方式")
    private String requestMethod;

    @Schema(description = "操作人员账号")
    private String operName;

    @Schema(description = "操作IP")
    private String operIp;

    @Schema(description = "操作状态：0正常 1异常")
    private Integer status;

    @Schema(description = "操作开始时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "操作结束时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
