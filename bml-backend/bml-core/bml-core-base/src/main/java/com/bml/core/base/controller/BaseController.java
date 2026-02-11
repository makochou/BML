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

    /**
     * 返回操作结果
     * <p>
     * 根据布尔值返回成功或失败响应，适用于增删改操作。
     * </p>
     *
     * @param result 操作是否成功
     * @return 统一响应结果
     */
    protected Result<Void> toAjax(boolean result) {
        return result ? success() : fail("操作失败");
    }
}
