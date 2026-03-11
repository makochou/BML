package com.bml.module.api.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.module.api.dto.OpenApiRegistryTreeQuery;
import com.bml.module.api.dto.SaveApiAccountAuthorizationCommand;
import com.bml.module.api.entity.SysApiRegistry;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.vo.ApiAccountAuthorizationVO;
import com.bml.module.api.vo.ApiAuthorizationSummaryVO;
import com.bml.module.api.vo.SysApiAccountVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * API账号授权服务。
 */
@Slf4j
@Service
public class SysApiAccountAuthorizationService {

    private final SysApiAccountService apiAccountService;
    private final SysApiPermissionMapper apiPermissionMapper;
    private final SysOpenApiRegistryService openApiRegistryService;

    public SysApiAccountAuthorizationService(SysApiAccountService apiAccountService,
            SysApiPermissionMapper apiPermissionMapper,
            SysOpenApiRegistryService openApiRegistryService) {
        this.apiAccountService = apiAccountService;
        this.apiPermissionMapper = apiPermissionMapper;
        this.openApiRegistryService = openApiRegistryService;
    }

    public ApiAccountAuthorizationVO getAuthorizationSnapshot(Long accountId) {
        SysApiAccountVO account = apiAccountService.getAccountInfo(accountId);
        List<Long> selectedApiIds = apiPermissionMapper.selectApiIdsByAccountId(accountId);

        OpenApiRegistryTreeQuery query = new OpenApiRegistryTreeQuery();
        query.setStatus(1);

        ApiAuthorizationSummaryVO summary = new ApiAuthorizationSummaryVO();
        summary.setTotalApiCount(openApiRegistryService.countOpenApi(null));
        summary.setEnabledApiCount(openApiRegistryService.countOpenApi(1));
        summary.setSelectedApiCount(selectedApiIds.size());
        summary.setSelectedEnabledApiCount(openApiRegistryService.listEnabledByIds(selectedApiIds).size());

        ApiAccountAuthorizationVO authorizationVO = new ApiAccountAuthorizationVO();
        authorizationVO.setAccount(account);
        authorizationVO.setSelectedApiIds(selectedApiIds);
        authorizationVO.setGroups(openApiRegistryService.listRegistryTree(query));
        authorizationVO.setSummary(summary);
        return authorizationVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveAuthorization(Long accountId, SaveApiAccountAuthorizationCommand command) {
        apiAccountService.getRequiredAccount(accountId);
        List<Long> apiIds = normalizeApiIds(command == null ? null : command.getApiIds());
        if (!apiIds.isEmpty()) {
            List<SysApiRegistry> enabledApis = openApiRegistryService.listEnabledByIds(apiIds);
            if (enabledApis.size() != apiIds.size()) {
                List<Long> foundIds = enabledApis.stream().map(SysApiRegistry::getId).toList();
                log.error("API Authorization validation failed for account {}. Requested IDs: {}, but found only: {}", 
                        accountId, apiIds, foundIds);
                throw new BusinessException("授权接口包含不存在或已停用的开放接口");
            }
        }

        apiPermissionMapper.deleteByAccountId(accountId);
        if (!apiIds.isEmpty()) {
            apiPermissionMapper.batchInsert(accountId, apiIds);
        }
        return true;
    }

    private List<Long> normalizeApiIds(List<Long> apiIds) {
        if (apiIds == null || apiIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long apiId : apiIds) {
            if (apiId == null) {
                continue;
            }
            deduplicated.add(apiId);
        }
        return List.copyOf(deduplicated);
    }
}
