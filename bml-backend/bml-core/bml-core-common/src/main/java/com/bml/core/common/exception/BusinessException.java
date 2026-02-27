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
 *
 * @author BML Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

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
    }

    /**
     * 使用统一错误码对象构造异常。
     *
     * @param errorCode 错误码对象
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
