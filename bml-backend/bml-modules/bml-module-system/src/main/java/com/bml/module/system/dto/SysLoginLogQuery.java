package com.bml.module.system.dto;

import com.bml.core.base.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 系统登录日志分页查询参数。
 * <p>
 * 覆盖账号、IP、状态与时间范围等登录审计高频过滤维度，适用于登录成功、登录失败、登出和修改密码等认证事件追踪。
 * </p>
 *
 * @author BML Team
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统登录日志分页查询参数")
public class SysLoginLogQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "登录IP地址")
    private String ipaddr;

    @Schema(description = "登录状态：1成功 0失败")
    private Integer status;

    @Schema(description = "认证事件关键字，如 登录、登出、修改密码")
    private String msg;

    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
