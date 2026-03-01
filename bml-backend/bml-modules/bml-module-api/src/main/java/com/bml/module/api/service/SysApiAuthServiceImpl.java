package com.bml.module.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.core.common.support.ApiIpWhitelistSupport;
import com.bml.core.common.support.ApiSignatureVersionSupport;
import com.bml.core.framework.service.OpenApiAuthService;
import com.bml.core.framework.service.model.OpenApiAppAuth;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiAccountMapper;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.mapper.SysApiRegistryMapper;
import com.bml.module.api.support.ApiAccountEnvironmentWhitelistSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysApiAuthServiceImpl implements OpenApiAuthService {

    @Resource
    private SysApiAccountMapper apiAccountMapper;

    @Resource
    private SysApiRegistryMapper apiRegistryMapper;

    @Resource
    private SysApiPermissionMapper apiPermissionMapper;

    @Resource
    private ApiSecretCryptoService secretCryptoService;

    @Override
    public OpenApiAppAuth getAppAuth(String appKey) {
        SysApiAccount account = apiAccountMapper.selectOne(new LambdaQueryWrapper<SysApiAccount>()
                .eq(SysApiAccount::getAccessKey, appKey)
                .eq(SysApiAccount::getStatus, 1)
                .last("LIMIT 1"));

        if (account == null) {
            return null;
        }
        if (account.getExpireTime() != null && account.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        String effectiveWhitelist = ApiIpWhitelistSupport.serializeEntries(
                ApiAccountEnvironmentWhitelistSupport.resolveEffectiveWhitelist(
                        account.getAccessEnvironment(),
                        ApiAccountEnvironmentWhitelistSupport.buildEnvironmentWhitelistMap(
                                account.getTestIpWhitelist(),
                                account.getStagingIpWhitelist(),
                                account.getProductionIpWhitelist()),
                        ApiIpWhitelistSupport.deserializeEntries(account.getIpWhitelist())));
        return OpenApiAppAuth.builder()
                .accountId(account.getId())
                .secretKey(secretCryptoService.decrypt(account.getSecretKey()))
                .signVersion(account.getSignVersion() == null ? ApiSignatureVersionSupport.defaultVersion() : account.getSignVersion())
                .ipWhitelist(effectiveWhitelist)
                .build();
    }

    @Override
    public boolean isApiAuthorized(Long accountId, String path, String method) {
        SysApiRegistry registry = apiRegistryMapper.selectOne(new LambdaQueryWrapper<SysApiRegistry>()
                .eq(SysApiRegistry::getApiUrl, path)
                .eq(SysApiRegistry::getHttpMethod, method == null ? null : method.toUpperCase())
                .eq(SysApiRegistry::getStatus, 1)
                .last("LIMIT 1"));
        if (registry == null) {
            return false;
        }
        return apiPermissionMapper.countPermission(accountId, registry.getId()) > 0;
    }
}
