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
 * 系统操作日志实体。
 * <p>
 * 对应 sys_operation_log 表，用于记录后台用户或 API 账号在系统中的关键操作行为，包含请求方法、
 * 请求地址、请求参数、响应结果、异常信息和执行耗时等审计字段。
 * </p>
 *
 * @author BML Team
 */
@Data
@TableName("sys_operation_log")
@Schema(description = "系统操作日志实体")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "模块标题")
    private String title;

    @Schema(description = "业务类型：0其他 1新增 2修改 3删除 4查询 5授权 6重置 7清空 8同步 9状态变更")
    private Integer businessType;

    @Schema(description = "Java 方法名称")
    private String method;

    @Schema(description = "HTTP 请求方式")
    private String requestMethod;

    @Schema(description = "操作类别：0其他 1后台用户 2移动端用户 3API账号")
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
