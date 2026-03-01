package com.bml.module.api.support;

/**
 * API 回调状态常量。
 */
public final class ApiCallbackStatusSupport {

    public static final int PENDING = 0;
    public static final int RETRYING = 1;
    public static final int SUCCESS = 2;
    public static final int FAILED = 3;

    private ApiCallbackStatusSupport() {
    }

    public static boolean isRetryable(Integer status) {
        return status != null && (status == PENDING || status == RETRYING || status == FAILED);
    }
}
