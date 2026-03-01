package com.bml.core.framework.security.model;

import java.util.Collections;

/**
 * API 账号鉴权成功后的安全上下文用户模型。
 * <p>
 * 该模型复用 {@link LoginUser} 作为 Spring Security 主体，目的是兼容现有
 * {@code SecurityUtils.getLoginUser()}、审计填充和方法级权限判断链路。
 * </p>
 * <p>
 * 注意：
 * 1. 这里的 userId 并不代表后台用户，而是使用负数账号 ID 构造的“外部调用主体标识”；
 * 2. 真正的接口访问控制仍以 sys_api_permission 中的接口授权关系为准；
 * 3. 该主体仅用于 API 账号签名调用场景，不替代后台 JWT 登录用户。
 * </p>
 */
public class OpenApiLoginUser extends LoginUser {

    private Long accountId;

    private String appKey;

    public OpenApiLoginUser() {
    }

    public OpenApiLoginUser(Long accountId, String appKey) {
        super(accountId == null ? null : -accountId, null, "api-account:" + appKey, null, 1, Collections.emptySet());
        this.accountId = accountId;
        this.appKey = appKey;
        setUserKey("api-account:" + accountId);
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}
