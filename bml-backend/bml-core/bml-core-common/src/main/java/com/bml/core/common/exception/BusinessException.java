package com.bml.core.common.exception;

import com.bml.core.common.enums.GlobalErrorCode;
import com.bml.core.common.result.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 用于表示可预期、可直接反馈给调用方的业务错误，
 * 例如参数校验失败、状态不允许、资源不存在等。
 * </p>
 * <p>
 * 该异常可携带附加的 {@code data} 载荷（如字段级错误列表），
 * 由 {@code GlobalExceptionHandler} 在序列化到 {@code Result.data}
 * 字段时一并下发给调用方，便于前端按字段定位非法输入。
 * </p>
 *
 * @author BML Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码。 */
    private final int code;

    /**
     * 附加错误载荷（可空）。
     * <p>
     * 例如 {@code List<FieldError>}，用于在请求参数全字段校验场景下
     * 一次性返回所有非法字段的明细。
     * </p>
     */
    private final transient Object data;

    /**
     * 使用默认业务错误码构造异常。
     * <p>
     * 未显式指定错误码时，默认按请求不合法处理。
     * </p>
     *
     * @param message 业务消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = GlobalErrorCode.BAD_REQUEST.getCode();
        this.data = null;
    }

    /**
     * 使用指定业务错误码构造异常。
     *
     * @param code    业务错误码
     * @param message 业务消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.data = null;
    }

    /**
     * 使用统一错误码对象构造异常。
     *
     * @param errorCode 错误码对象
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.data = null;
    }

    /**
     * 使用统一错误码对象与附加载荷构造异常。
     *
     * @param errorCode 错误码对象
     * @param data      附加载荷（如字段级错误列表）
     */
    public BusinessException(ErrorCode errorCode, Object data) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.data = data;
    }

    /**
     * 使用自定义错误码、消息与附加载荷构造异常。
     *
     * @param code    业务错误码
     * @param message 业务消息
     * @param data    附加载荷
     */
    public BusinessException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
