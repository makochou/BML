package com.bml.module.system.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统操作日志视图对象。
 * <p>
 * 面向前端审计页面返回标准展示字段，保留请求参数、响应结果和异常信息，便于问题追踪和安全审计。
 * </p>
 *
 * @author BML Team
 */
@Data
@Schema(description = "系统操作日志视图对象")
public class SysOperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "业务类型")
    private Integer businessType;

    @Schema(description = "Java 方法名称")
    private String method;

    @Schema(description = "HTTP 请求方式")
    private String requestMethod;

    @Schema(description = "操作类别")
    private Integer operatorType;

    @Schema(description = "操作人员账号")
    private String operName;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "请求URL")
    private String operUrl;

    @Schema(description = "操作IP")
    private String operIp;

    @Schema(description = "请求参数")
    private String operParam;

    @Schema(description = "返回结果")
    private String jsonResult;

    @Schema(description = "操作状态：0正常 1异常")
    private Integer status;

    @Schema(description = "错误消息")
    private String errorMsg;

    @Schema(description = "操作时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime operTime;

    @Schema(description = "消耗时间，单位毫秒")
    private Long costTime;
}
