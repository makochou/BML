package com.bml.core.common.result;

import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.enums.GlobalErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 🏷️ 统一接口响应协议 (Unified Response Protocol)
 * <p>
 * 该模型是 BML 企业中台所有 HTTP 接口、全局异常处理器、安全处理器以及拦截器的唯一响应标准。
 * 通过统一的结构化数据，确保前端及第三方调用者能够高效、一致地处理业务结果。
 * </p>
 *
 * <h3>规范说明：</h3>
 * <ul>
 *   <li><b>code:</b> 业务逻辑状态码（非 HTTP 状态码），成功通常为 200。</li>
 *   <li><b>message:</b> 对业务结果的可读性描述，可用于前端友好提示。</li>
 *   <li><b>data:</b> 业务载荷数据，采用泛型封装，失败时该字段通常为 null。</li>
 *   <li><b>timestamp:</b> 响应生成的毫秒时间戳，用于性能分析。</li>
 *   <li><b>traceId:</b> 链路追踪标识，关联后端分布式日志，便于快速排查问题。</li>
 * </ul>
 *
 * @param <T> 数据载荷的具体类型
 * @author BML Advanced Coding Team
 * @since 1.0.0
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
     * 响应消息描述。
     */
    private String message;

    /**
     * 响应核心数据载荷。
     */
    private T data;

    /**
     * 响应生成的时间戳（毫秒）。
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * 全局链路追踪 ID，从日志上下文 MDC 中获取。
     */
    private String traceId = MDC.get(GlobalConstants.TRACE_ID);

    /**
     * 快捷构造：构造一个不带数据载荷的成功响应。
     *
     * @param <T> 数据类型
     * @return 状态码为 SUCCESS 的 Result 实例
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 快捷构造：构造一个包含具体数据的成功响应。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 状态码为 SUCCESS 且包含数据的 Result 实例
     */
    public static <T> Result<T> ok(T data) {
        return build(GlobalErrorCode.SUCCESS.getCode(), GlobalErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 快捷构造：根据预定义的错误码枚举构造失败响应。
     *
     * @param errorCode 错误码常量定义
     * @param <T>       数据类型
     * @return 包含错误信息的 Result 实例
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 快捷构造：使用自定义状态码与消息构造失败响应。
     *
     * @param code    自定义业务状态码
     * @param message 错误提示消息
     * @param <T>     数据类型
     * @return 包含自定义错误信息的 Result 实例
     */
    public static <T> Result<T> fail(int code, String message) {
        return build(code, message, null);
    }

    /**
     * 快捷构造：构造一个参数校验失败或请求不合法的响应 (400)。
     *
     * @param message 失败细节描述
     * @param <T>     数据类型
     * @return 状态码为 BAD_REQUEST 的 Result 实例
     */
    public static <T> Result<T> badRequest(String message) {
        return fail(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    /**
     * 快捷构造：构造一个服务器内部运行异常的响应 (500)。
     *
     * @param message 异常描述或友好提示
     * @param <T>     数据类型
     * @return 状态码为 INTERNAL_SERVER_ERROR 的 Result 实例
     */
    public static <T> Result<T> error(String message) {
        return fail(GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(), message);
    }

    /**
     * 内部通用构造方法。
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @return 填充后的 Result 实例
     */
    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}

