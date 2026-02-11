package com.bml.module.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.framework.service.OpenApiAuthService;
import com.bml.module.api.entity.ApiApp;
import com.bml.module.api.mapper.ApiAppMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * OpenAPI 认证服务实现
 *
 * @author BML Team
 */
@Service
public class OpenApiAuthServiceImpl implements OpenApiAuthService {

    @Resource
    private ApiAppMapper apiAppMapper;

    @Override
    public String getAppSecret(String appKey) {
        ApiApp app = apiAppMapper.selectOne(new LambdaQueryWrapper<ApiApp>()
                .select(ApiApp::getAppSecret, ApiApp::getStatus)
                .eq(ApiApp::getAppId, appKey));

        if (app != null && app.getStatus() == 1) {
            return app.getAppSecret();
        }
        return null; // 不存在或已停用
    }
}
