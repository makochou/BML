package com.bml.module.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.framework.service.OpenApiAuthService;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.mapper.SysApiAccountMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * OpenAPI 认证服务实现
 *
 * @author BML Team
 */
@Service
public class SysApiAuthServiceImpl implements OpenApiAuthService {

    @Resource
    private SysApiAccountMapper apiAccountMapper;

    @Override
    public String getAppSecret(String appKey) {
        SysApiAccount account = apiAccountMapper.selectOne(new LambdaQueryWrapper<SysApiAccount>()
                .select(SysApiAccount::getSecretKey)
                .eq(SysApiAccount::getAccessKey, appKey)
                .eq(SysApiAccount::getStatus, 1) // 仅查询正常状态的账号
                .last("LIMIT 1"));

        return account != null ? account.getSecretKey() : null;
    }
}
