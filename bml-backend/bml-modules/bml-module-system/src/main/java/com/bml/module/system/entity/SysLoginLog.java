package com.bml.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统登录日志实体。
 * <p>
 * 对应 {@code sys_login_log} 表，专门记录用户登录、登出、修改密码、登录失败等认证安全事件。
 * 登录日志与操作日志分表存储，可以让安全审计人员快速定位账号级行为，同时避免业务操作审计列表被登录事件淹没。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_login_log")
@Schema(description = "系统登录日志实体")
public class SysLoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
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
