package com.bml.module.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统登录日志视图对象。
 * <p>
 * 仅向前端暴露审计展示所需字段，不直接返回实体对象，保证后续表结构扩展时接口契约稳定。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "系统登录日志视图对象")
public class SysLoginLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户账号")
    private String username;

    @Schema(description = "登录IP地址")
    private String ipaddr;

    @Schema(description = "登录地点")
    private String loginLocation;

    @Schema(description = "浏览器类型")
    private String browser;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录状态：1成功 0失败")
    private Integer status;

    @Schema(description = "提示消息")
    private String msg;

    @Schema(description = "登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime loginTime;
}
