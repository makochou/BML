package com.bml.core.common.result;

/**
 * 错误码接口
 *
 * @author BML Team
 */
public interface ErrorCode {

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    int getCode();

    /**
     * 获取错误信息
     * 
     * @return 错误信息
     */
    String getMessage();
}
