package com.bml.core.common.result;

import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.enums.GlobalErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一接口响应模型。
 * <p>
 * 该模型是后端所有 HTTP 接口、异常处理器、安全处理器以及拦截器的唯一响应协议。
 * 字段约定如下：
 * </p>
 * <ul>
 *     <li>{@code code}：业务状态码，而非 HTTP 状态码</li>
 *     <li>{@code message}：给调用方展示或记录的可读消息</li>
 *     <li>{@code data}：成功响应的数据载荷，失败时通常为空</li>
 *     <li>{@code timestamp}：响应构造时间戳</li>
 *     <li>{@code traceId}：链路追踪标识，便于定位日志</li>
 * </ul>
 *
 * @param <T> 数据载荷类型
 * @author BML Team
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码。
     */
    private int code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 响应生成时间戳。
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * 当前请求链路追踪 ID。
     */
    private String traceId = MDC.get(GlobalConstants.TRACE_ID);

    /**
     * 构造不带数据的成功响应。
     *
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 构造成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> ok(T data) {
        return build(GlobalErrorCode.SUCCESS.getCode(), GlobalErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 使用错误码对象构造失败响应。
     *
     * @param errorCode 错误码定义
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 使用指定业务码与消息构造失败响应。
     *
     * @param code    业务状态码
     * @param message 业务消息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(int code, String message) {
        return build(code, message, null);
    }

    /**
     * 构造请求参数或业务规则不满足的失败响应。
     *
     * @param message 失败消息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> badRequest(String message) {
        return fail(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 构造系统内部错误响应。
     *
     * @param message 失败消息
     * @param <T>     数据类型
     * @return 失败响应
     */
    public static <T> Result<T> error(String message) {
        return fail(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
