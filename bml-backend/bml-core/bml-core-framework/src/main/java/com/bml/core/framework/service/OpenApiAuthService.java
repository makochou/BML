package com.bml.core.framework.service;

import com.bml.core.framework.service.model.OpenApiAppAuth;

/**
 * OpenAPI 认证服务接口
 * <p>
 * 用于解耦 Core 与 Module 的依赖
 * </p>
 *
 * @author BML Team
 */
public interface OpenApiAuthService {

    /**
     * 根据 AppKey 获取 AppSecret
     *
     * @param appKey 应用ID
     * @return 密钥, 若不存在或停用则返回 null
     */
    OpenApiAppAuth getAppAuth(String appKey);

    boolean isApiAuthorized(Long accountId, String path, String method);
}
