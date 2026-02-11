package com.bml.core.common.result;

import com.bml.core.common.constant.GlobalConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serializable;

/**
 * 统一响应结构体
 *
 * @author BML Team
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private int code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间戳
     */
    private long timestamp = System.currentTimeMillis();

    /**
     * 链路追踪ID
     * <p>
     * 从 SLF4J MDC 中获取。需要配合 TraceId 过滤器/拦截器使用，
     * 在请求进入时通过 {@code MDC.put(GlobalConstants.TRACE_ID, uuid)} 设置。
     * 当前项目尚未添加该过滤器，因此 traceId 始终为 null（不会输出到响应中）。
     * </p>
     * TODO: 实现 TraceIdFilter，在请求入口处向 MDC 写入 traceId
     */
    private String traceId = MDC.get(GlobalConstants.TRACE_ID);

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
