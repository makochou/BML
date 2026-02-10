package com.bml.core.base.controller;

import com.bml.core.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础控制器
 *
 * @author BML Team
 */
public abstract class BaseController {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 响应成功
     */
    protected <T> Result<T> success() {
        return Result.ok();
    }

    /**
     * 响应成功
     */
    protected <T> Result<T> success(T data) {
        return Result.ok(data);
    }

    /**
     * 响应失败
     */
    protected <T> Result<T> fail(String message) {
        return Result.fail(message);
    }
    
    /**
     * 响应失败
     */
    protected <T> Result<T> fail(int code, String message) {
        return Result.fail(code, message);
    }
}
