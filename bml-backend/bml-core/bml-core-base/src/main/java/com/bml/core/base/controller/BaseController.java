package com.bml.core.base.controller;

import com.bml.core.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制器基类。
 * <p>
 * 统一封装控制器最常见的成功、失败和布尔操作结果转换逻辑，
 * 避免各业务控制器重复拼装响应结构。
 * </p>
 *
 * @author BML Team
 */
public abstract class BaseController {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 返回不带数据的成功响应。
     *
     * @param <T> 数据类型
     * @return 统一成功响应
     */
    protected <T> Result<T> success() {
        return Result.ok();
    }

    /**
     * 返回带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 统一成功响应
     */
    protected <T> Result<T> success(T data) {
        return Result.ok(data);
    }

    /**
     * 返回业务失败响应。
     *
     * @param message 失败消息
     * @param <T>     数据类型
     * @return 统一失败响应
     */
    protected <T> Result<T> fail(String message) {
        return Result.badRequest(message);
    }

    /**
     * 返回指定错误码的失败响应。
     *
     * @param code    业务错误码
     * @param message 失败消息
     * @param <T>     数据类型
     * @return 统一失败响应
     */
    protected <T> Result<T> fail(int code, String message) {
        return Result.fail(code, message);
    }

    /**
     * 将布尔结果转换为标准操作响应。
     * <p>
     * 一般用于增删改操作：成功返回 200，失败返回 500。
     * 如果业务规则校验失败，应优先直接返回业务错误或抛出业务异常，
     * 而不是依赖本方法兜底。
     * </p>
     *
     * @param result 操作结果
     * @return 统一操作响应
     */
    protected Result<Void> toAjax(boolean result) {
        return result ? success() : Result.error("操作失败");
    }
}
