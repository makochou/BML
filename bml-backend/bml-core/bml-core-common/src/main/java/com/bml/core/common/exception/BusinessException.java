package com.bml.core.common.exception;

import com.bml.core.common.result.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author BML Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
