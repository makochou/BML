package com.bml.core.common.result;

/**
 * 统一错误码协议。
 * <p>
 * 所有可以序列化到接口响应中的错误定义，都必须实现本接口，
 * 以保证统一响应结构中的 {@code code} 与 {@code message} 有稳定来源。
 * </p>
 *
 * @author BML Team
 */
public interface ErrorCode {

    /**
     * 获取业务错误码。
     *
     * @return 业务错误码
     */
    int getCode();

    /**
     * 获取默认错误消息。
     *
     * @return 默认错误消息
     */
    String getMessage();
}
