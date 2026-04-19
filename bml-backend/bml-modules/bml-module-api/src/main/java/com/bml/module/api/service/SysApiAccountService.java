package com.bml.module.api.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.common.result.PageResult;
import com.bml.core.common.support.ApiIpWhitelistSupport;
import com.bml.core.framework.license.LicenseQuotaChecker;
import com.bml.core.common.support.ApiSignatureVersionSupport;
import com.bml.module.api.dto.CreateSysApiAccountCommand;
import com.bml.module.api.dto.SysApiAccountDTO;
import com.bml.module.api.dto.SysApiAccountPageQuery;
import com.bml.module.api.dto.UpdateSysApiAccountCommand;
import com.bml.module.api.entity.SysApiAccount;
import com.bml.module.api.mapper.SysApiAccountMapper;
import com.bml.module.api.mapper.SysApiPermissionMapper;
import com.bml.module.api.constant.ApiAccountTypeConstants;
import com.bml.module.api.support.ApiAccountEnvironmentSupport;
import com.bml.module.api.support.ApiAccountEnvironmentWhitelistSupport;
import com.bml.module.api.support.ApiClientTypeSupport;
import com.bml.module.api.support.ApiScopeSupport;
import com.bml.module.api.vo.ApiAccountPermissionCountVO;
import com.bml.module.api.vo.ApiCredentialVO;
import com.bml.module.api.vo.SysApiAccountDetailVO;
import com.bml.module.api.vo.SysApiAccountVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * API 账号服务。
 */
@Service
public class SysApiAccountService extends ServiceImpl<SysApiAccountMapper, SysApiAccount> {

    private static final int DEFAULT_ACCOUNT_TYPE = ApiAccountTypeConstants.DEFAULT;
    private static final int DEFAULT_RATE_LIMIT = 1000;
    private static final int DEFAULT_STATUS = 1;
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;
    private static final String TEXT_MATCH_MODE_EXACT = "exact";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ApiSecretCryptoService secretCryptoService;
    private final SysApiPermissionMapper apiPermissionMapper;
    private final LicenseQuotaChecker licenseQuotaChecker;

    public SysApiAccountService(ApiSecretCryptoService secretCryptoService,
            SysApiPermissionMapper apiPermissionMapper,
            LicenseQuotaChecker licenseQuotaChecker) {
        this.secretCryptoService = secretCryptoService;
        this.apiPermissionMapper = apiPermissionMapper;
        this.licenseQuotaChecker = licenseQuotaChecker;
    }

    public PageResult<SysApiAccountVO> pageAccounts(SysApiAccountPageQuery query) {
        long pageNum = normalizePageNum(query == null ? null : query.getPageNum());
        long pageSize = normalizePageSize(query == null ? null : query.getPageSize());

        LambdaQueryWrapper<SysApiAccount> wrapper = buildAccountQueryWrapper(query);
        long total = this.count(wrapper);
        if (total <= 0) {
            return PageResult.empty(pageNum, pageSize);
        }

        long offset = (pageNum - 1L) * pageSize;
        List<SysApiAccount> accounts = this.list(buildAccountQueryWrapper(query)
                .last("LIMIT " + offset + "," + pageSize));
        return PageResult.of(toVoList(accounts), total, pageNum, pageSize);
    }

    public List<SysApiAccountVO> selectAccountList(SysApiAccountDTO dto) {
        if (dto == null) {
            return toVoList(this.list(buildAccountQueryWrapper(null)));
        }
        SysApiAccountPageQuery query = new SysApiAccountPageQuery();
        query.setAccountName(dto.getAccountName());
        query.setStatus(dto.getStatus());
        query.setClientType(dto.getClientType());
        query.setSystemKeyword(dto.getSystemKeyword());
        query.setAccessEnvironment(dto.getAccessEnvironment());
        return toVoList(this.list(buildAccountQueryWrapper(query)));
    }

    public SysApiAccountDetailVO getAccountDetail(Long id) {
        return toDetailVo(getRequiredAccount(id));
    }

    /**
     * 获取 API 账号副本视图。
     * <p>
     * 业务规则：
     * 1. 追加 "_Copy" 后缀到账号名称；
     * 2. 清空主键 ID、AK、密钥引用、创建时间、更新时间；
     * 3. 清空授权统计数（复制不包含权限关系）。
     * </p>
     *
     * @param id 原账号 ID
     * @return 准备进入新建模式的账号详情副本
     */
    public SysApiAccountDetailVO getAccountCopy(Long id) {
        SysApiAccount account = getRequiredAccount(id);
        SysApiAccountDetailVO copy = toDetailVo(account);

        // 重置为新建模式特征
        copy.setId(null);
        if (StrUtil.isNotBlank(copy.getAccountName())) {
            copy.setAccountName(copy.getAccountName() + "_Copy");
        }
        // AK 在新建保存时重新生成，这里先清空避免误导
        copy.setAccessKey(null);
        // 复制不保留原有权限统计
        copy.setAuthorizedApiCount(0L);
        copy.setEnabledAuthorizedApiCount(0L);
        // 清理物理审计字段
        copy.setCreateTime(null);
        copy.setUpdateTime(null);

        return copy;
    }

    public SysApiAccountVO getAccountInfo(Long id) {
        return toVo(getRequiredAccount(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialVO createAccount(CreateSysApiAccountCommand command) {
        // 许可证配额校验：检查当前 API 账号数量是否已达上限
        long currentAccountCount = this.count();
        licenseQuotaChecker.checkApiAccountQuota(currentAccountCount);

        String accountName = normalizeAccountName(command.getAccountName());
        validateAccountNameUnique(accountName, null);
        String accessEnvironment = normalizeAccessEnvironment(command.getAccessEnvironment());

        String plainSecret = RandomUtil.randomString(32);
        SysApiAccount account = new SysApiAccount();
        account.setAccountName(accountName);
        account.setDescription(normalizeDescription(command.getDescription()));
        account.setAccessKey(generateAccessKey());
        account.setSecretKey(secretCryptoService.encrypt(plainSecret));
        account.setAccountType(command.getAccountType() == null ? DEFAULT_ACCOUNT_TYPE : command.getAccountType());
        account.setClientTypes(ApiClientTypeSupport.serializeClientTypes(command.getClientTypes()));
        account.setOwnerName(normalizeOwnerName(command.getOwnerName()));
        account.setOwnerContact(normalizeOwnerContact(command.getOwnerContact()));
        account.setSystemName(normalizeSystemName(command.getSystemName()));
        account.setSystemCode(normalizeSystemCode(command.getSystemCode()));
        account.setAccessEnvironment(accessEnvironment);
        applyWhitelistFields(account, accessEnvironment, command.getIpWhitelist(), command.getEnvironmentIpWhitelist());
        account.setSignVersion(normalizeSignVersion(command.getSignVersion()));
        account.setAllowedScopes(ApiScopeSupport.serializeScopes(command.getAllowedScopes()));
        account.setCallbackUrl(normalizeCallbackUrl(command.getCallbackUrl()));
        account.setRateLimit(command.getRateLimit() == null ? DEFAULT_RATE_LIMIT : command.getRateLimit());
        account.setExpireTime(command.getExpireTime());
        account.setStatus(command.getStatus() == null ? DEFAULT_STATUS : command.getStatus());
        account.setRemark(normalizeRemark(command.getRemark()));
        // 直接调用 baseMapper.insert() 而非 this.save()，
        // 原因：ServiceImpl.save() 内部通过 SqlHelper.getMapper() 获取 MyBatis 代理，
        // 在单元测试中 mock baseMapper 时无法被拦截。
        baseMapper.insert(account);
        return buildCredential(account, plainSecret);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialVO insertAccount(SysApiAccountDTO dto) {
        CreateSysApiAccountCommand command = new CreateSysApiAccountCommand();
        command.setAccountName(dto == null ? null : dto.getAccountName());
        command.setDescription(dto == null ? null : dto.getDescription());
        command.setAccountType(dto == null ? null : dto.getAccountType());
        command.setClientTypes(dto == null ? null : dto.getClientTypes());
        command.setOwnerName(dto == null ? null : dto.getOwnerName());
        command.setOwnerContact(dto == null ? null : dto.getOwnerContact());
        command.setSystemName(dto == null ? null : dto.getSystemName());
        command.setSystemCode(dto == null ? null : dto.getSystemCode());
        command.setAccessEnvironment(dto == null ? null : dto.getAccessEnvironment());
        command.setIpWhitelist(dto == null ? null : dto.getIpWhitelist());
        command.setEnvironmentIpWhitelist(dto == null ? null : dto.getEnvironmentIpWhitelist());
        command.setSignVersion(dto == null ? null : dto.getSignVersion());
        command.setAllowedScopes(dto == null ? null : dto.getAllowedScopes());
        command.setCallbackUrl(dto == null ? null : dto.getCallbackUrl());
        command.setRateLimit(dto == null ? null : dto.getRateLimit());
        command.setExpireTime(dto == null ? null : dto.getExpireTime());
        command.setStatus(dto == null ? null : dto.getStatus());
        command.setRemark(dto == null ? null : dto.getRemark());
        return createAccount(command);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateAccount(Long id, UpdateSysApiAccountCommand command) {
        SysApiAccount account = getRequiredAccount(id);
        String accountName = normalizeAccountName(command.getAccountName());
        validateAccountNameUnique(accountName, id);
        String accessEnvironment = normalizeAccessEnvironment(command.getAccessEnvironment());

        // 许可证配额校验：如果状态变更为「启用」，检查活跃账号数是否会超额
        Integer newStatus = command.getStatus() == null ? DEFAULT_STATUS : command.getStatus();
        checkEnableQuotaIfNeeded(account, newStatus);

        account.setAccountName(accountName);
        account.setDescription(normalizeDescription(command.getDescription()));
        account.setAccountType(command.getAccountType() == null ? DEFAULT_ACCOUNT_TYPE : command.getAccountType());
        account.setClientTypes(ApiClientTypeSupport.serializeClientTypes(command.getClientTypes()));
        account.setOwnerName(normalizeOwnerName(command.getOwnerName()));
        account.setOwnerContact(normalizeOwnerContact(command.getOwnerContact()));
        account.setSystemName(normalizeSystemName(command.getSystemName()));
        account.setSystemCode(normalizeSystemCode(command.getSystemCode()));
        account.setAccessEnvironment(accessEnvironment);
        applyWhitelistFields(account, accessEnvironment, command.getIpWhitelist(), command.getEnvironmentIpWhitelist());
        account.setSignVersion(normalizeSignVersion(command.getSignVersion()));
        account.setAllowedScopes(ApiScopeSupport.serializeScopes(command.getAllowedScopes()));
        account.setCallbackUrl(normalizeCallbackUrl(command.getCallbackUrl()));
        account.setRateLimit(command.getRateLimit() == null ? DEFAULT_RATE_LIMIT : command.getRateLimit());
        account.setExpireTime(command.getExpireTime());
        account.setStatus(newStatus);
        account.setRemark(normalizeRemark(command.getRemark()));
        return this.updateById(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateAccount(SysApiAccountDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("API账号ID不能为空");
        }
        UpdateSysApiAccountCommand command = new UpdateSysApiAccountCommand();
        command.setAccountName(dto.getAccountName());
        command.setDescription(dto.getDescription());
        command.setAccountType(dto.getAccountType());
        command.setClientTypes(dto.getClientTypes());
        command.setOwnerName(dto.getOwnerName());
        command.setOwnerContact(dto.getOwnerContact());
        command.setSystemName(dto.getSystemName());
        command.setSystemCode(dto.getSystemCode());
        command.setAccessEnvironment(dto.getAccessEnvironment());
        command.setIpWhitelist(dto.getIpWhitelist());
        command.setEnvironmentIpWhitelist(dto.getEnvironmentIpWhitelist());
        command.setSignVersion(dto.getSignVersion());
        command.setAllowedScopes(dto.getAllowedScopes());
        command.setCallbackUrl(dto.getCallbackUrl());
        command.setRateLimit(dto.getRateLimit());
        command.setExpireTime(dto.getExpireTime());
        command.setStatus(dto.getStatus());
        command.setRemark(dto.getRemark());
        return updateAccount(dto.getId(), command);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateAccountStatus(Long id, Integer status) {
        SysApiAccount account = getRequiredAccount(id);

        // 许可证配额校验：如果状态变更为「启用」，检查活跃账号数是否会超额
        checkEnableQuotaIfNeeded(account, status);

        account.setStatus(status);
        return this.updateById(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean removeAccount(Long id) {
        getRequiredAccount(id);
        apiPermissionMapper.deleteByAccountId(id);
        return super.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        if (id instanceof Long accountId) {
            return removeAccount(accountId);
        }
        return super.removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiCredentialVO resetSecret(Long id) {
        SysApiAccount account = getRequiredAccount(id);
        String plainSecret = RandomUtil.randomString(32);
        account.setSecretKey(secretCryptoService.encrypt(plainSecret));
        this.updateById(account);
        return buildCredential(account, plainSecret);
    }

    public SysApiAccount getRequiredAccount(Long id) {
        if (id == null) {
            throw new BusinessException("API账号ID不能为空");
        }
        SysApiAccount account = this.getById(id);
        if (account == null) {
            throw new BusinessException("API账号不存在");
        }
        return account;
    }

    /**
     * 构建账号分页查询条件。
     * 统一承接“全字段筛选 + 字符字段模糊/精准模式 + 时间/数值范围”能力。
     */
    private LambdaQueryWrapper<SysApiAccount> buildAccountQueryWrapper(SysApiAccountPageQuery query) {
        boolean exactTextMatch = isExactTextMatchMode(query == null ? null : query.getTextMatchMode());
        String normalizedClientType = ApiClientTypeSupport
                .normalizeSingleClientType(query == null ? null : query.getClientType());
        String normalizedEnvironment = ApiAccountEnvironmentSupport
                .normalizeEnvironment(query == null ? null : query.getAccessEnvironment());

        String normalizedAccountName = normalizeQueryText(query == null ? null : query.getAccountName());
        String normalizedDescription = normalizeQueryText(query == null ? null : query.getDescription());
        String normalizedAccessKey = normalizeQueryText(query == null ? null : query.getAccessKey());
        String normalizedOwnerName = normalizeQueryText(query == null ? null : query.getOwnerName());
        String normalizedOwnerContact = normalizeQueryText(query == null ? null : query.getOwnerContact());
        String normalizedSystemName = normalizeQueryText(query == null ? null : query.getSystemName());
        String normalizedSystemCode = normalizeQueryText(query == null ? null : query.getSystemCode());
        String normalizedSystemKeyword = normalizeQueryText(query == null ? null : query.getSystemKeyword());
        String normalizedSignVersion = normalizeQueryText(query == null ? null : query.getSignVersion());
        String normalizedAllowedScope = ApiScopeSupport.normalizeSingleScope(
                query == null ? null : query.getAllowedScope());
        String normalizedCallbackUrl = normalizeQueryText(query == null ? null : query.getCallbackUrl());
        String normalizedRemark = normalizeQueryText(query == null ? null : query.getRemark());
        String normalizedIpKeyword = normalizeQueryText(query == null ? null : query.getIpKeyword());

        Integer rateLimitMin = query == null ? null : query.getRateLimitMin();
        Integer rateLimitMax = query == null ? null : query.getRateLimitMax();
        validateRange(rateLimitMin, rateLimitMax, "限流范围");

        LocalDateTime expireTimeStart = parseDateTimeQuery(query == null ? null : query.getExpireTimeStart(), "过期开始时间",
                false);
        LocalDateTime expireTimeEnd = parseDateTimeQuery(query == null ? null : query.getExpireTimeEnd(), "过期结束时间",
                true);
        validateRange(expireTimeStart, expireTimeEnd, "过期时间范围");

        LocalDateTime createTimeStart = parseDateTimeQuery(query == null ? null : query.getCreateTimeStart(), "创建开始时间",
                false);
        LocalDateTime createTimeEnd = parseDateTimeQuery(query == null ? null : query.getCreateTimeEnd(), "创建结束时间",
                true);
        validateRange(createTimeStart, createTimeEnd, "创建时间范围");

        LocalDateTime updateTimeStart = parseDateTimeQuery(query == null ? null : query.getUpdateTimeStart(), "更新开始时间",
                false);
        LocalDateTime updateTimeEnd = parseDateTimeQuery(query == null ? null : query.getUpdateTimeEnd(), "更新结束时间",
                true);
        validateRange(updateTimeStart, updateTimeEnd, "更新时间范围");

        LambdaQueryWrapper<SysApiAccount> wrapper = new LambdaQueryWrapper<SysApiAccount>()
                .eq(query != null && query.getAccountId() != null, SysApiAccount::getId,
                        query == null ? null : query.getAccountId())
                .eq(query != null && query.getStatus() != null, SysApiAccount::getStatus,
                        query == null ? null : query.getStatus())
                .eq(query != null && query.getAccountType() != null, SysApiAccount::getAccountType,
                        query == null ? null : query.getAccountType())
                .eq(StrUtil.isNotBlank(normalizedEnvironment), SysApiAccount::getAccessEnvironment,
                        normalizedEnvironment)
                .apply(StrUtil.isNotBlank(normalizedClientType), "FIND_IN_SET({0}, client_types)", normalizedClientType)
                .ge(rateLimitMin != null, SysApiAccount::getRateLimit, rateLimitMin)
                .le(rateLimitMax != null, SysApiAccount::getRateLimit, rateLimitMax)
                .ge(expireTimeStart != null, SysApiAccount::getExpireTime, expireTimeStart)
                .le(expireTimeEnd != null, SysApiAccount::getExpireTime, expireTimeEnd)
                .ge(createTimeStart != null, SysApiAccount::getCreateTime, createTimeStart)
                .le(createTimeEnd != null, SysApiAccount::getCreateTime, createTimeEnd)
                .ge(updateTimeStart != null, SysApiAccount::getUpdateTime, updateTimeStart)
                .le(updateTimeEnd != null, SysApiAccount::getUpdateTime, updateTimeEnd);

        applyStringFilter(wrapper, SysApiAccount::getAccountName, normalizedAccountName, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getDescription, normalizedDescription, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getAccessKey, normalizedAccessKey, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getOwnerName, normalizedOwnerName, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getOwnerContact, normalizedOwnerContact, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getSystemName, normalizedSystemName, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getSystemCode, normalizedSystemCode, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getSignVersion, normalizedSignVersion, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getCallbackUrl, normalizedCallbackUrl, exactTextMatch);
        applyStringFilter(wrapper, SysApiAccount::getRemark, normalizedRemark, exactTextMatch);

        if (StrUtil.isNotBlank(normalizedAllowedScope)) {
            wrapper.apply("FIND_IN_SET({0}, allowed_scopes)", normalizedAllowedScope);
        }

        if (StrUtil.isNotBlank(normalizedSystemKeyword)) {
            wrapper.and(nested -> {
                if (exactTextMatch) {
                    nested.eq(SysApiAccount::getSystemName, normalizedSystemKeyword)
                            .or()
                            .eq(SysApiAccount::getSystemCode, normalizedSystemKeyword);
                } else {
                    nested.like(SysApiAccount::getSystemName, normalizedSystemKeyword)
                            .or()
                            .like(SysApiAccount::getSystemCode, normalizedSystemKeyword);
                }
            });
        }

        if (StrUtil.isNotBlank(normalizedIpKeyword)) {
            wrapper.and(nested -> {
                if (exactTextMatch) {
                    nested.apply("FIND_IN_SET({0}, ip_whitelist)", normalizedIpKeyword)
                            .or()
                            .apply("FIND_IN_SET({0}, test_ip_whitelist)", normalizedIpKeyword)
                            .or()
                            .apply("FIND_IN_SET({0}, staging_ip_whitelist)", normalizedIpKeyword)
                            .or()
                            .apply("FIND_IN_SET({0}, production_ip_whitelist)", normalizedIpKeyword);
                } else {
                    nested.like(SysApiAccount::getIpWhitelist, normalizedIpKeyword)
                            .or()
                            .like(SysApiAccount::getTestIpWhitelist, normalizedIpKeyword)
                            .or()
                            .like(SysApiAccount::getStagingIpWhitelist, normalizedIpKeyword)
                            .or()
                            .like(SysApiAccount::getProductionIpWhitelist, normalizedIpKeyword);
                }
            });
        }

        return wrapper.orderByDesc(SysApiAccount::getCreateTime, SysApiAccount::getId);
    }

    private List<SysApiAccountVO> toVoList(List<SysApiAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> accountIds = accounts.stream().map(SysApiAccount::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> authorizedCountMap = toCountMap(apiPermissionMapper.selectAuthorizedCountList(accountIds));
        Map<Long, Long> enabledAuthorizedCountMap = toCountMap(
                apiPermissionMapper.selectEnabledAuthorizedCountList(accountIds));
        return accounts.stream()
                .map(account -> toVo(account, authorizedCountMap, enabledAuthorizedCountMap))
                .toList();
    }

    private Map<Long, Long> toCountMap(List<ApiAccountPermissionCountVO> countList) {
        if (countList == null || countList.isEmpty()) {
            return Collections.emptyMap();
        }
        return countList.stream()
                .filter(item -> item.getAccountId() != null)
                .collect(Collectors.toMap(
                        ApiAccountPermissionCountVO::getAccountId,
                        item -> item.getAuthorizedApiCount() == null ? 0L : item.getAuthorizedApiCount(),
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    private SysApiAccountVO toVo(SysApiAccount account) {
        return toVo(account, Collections.emptyMap(), Collections.emptyMap());
    }

    private SysApiAccountVO toVo(SysApiAccount account,
            Map<Long, Long> authorizedCountMap,
            Map<Long, Long> enabledAuthorizedCountMap) {
        SysApiAccountVO vo = new SysApiAccountVO();
        vo.setId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setDescription(account.getDescription());
        vo.setAccessKey(account.getAccessKey());
        vo.setAccountType(account.getAccountType());
        vo.setClientTypes(ApiClientTypeSupport.deserializeClientTypes(account.getClientTypes()));
        vo.setOwnerName(account.getOwnerName());
        vo.setOwnerContact(account.getOwnerContact());
        vo.setSystemName(account.getSystemName());
        vo.setSystemCode(account.getSystemCode());
        vo.setAccessEnvironment(account.getAccessEnvironment());
        Map<String, List<String>> environmentWhitelistMap = buildEnvironmentWhitelistMap(account);
        vo.setEnvironmentIpWhitelist(environmentWhitelistMap);
        vo.setIpWhitelist(resolveEffectiveWhitelist(account.getAccessEnvironment(), account.getIpWhitelist(),
                environmentWhitelistMap));
        vo.setSignVersion(account.getSignVersion());
        vo.setAllowedScopes(ApiScopeSupport.deserializeScopes(account.getAllowedScopes()));
        vo.setCallbackUrl(account.getCallbackUrl());
        vo.setRateLimit(account.getRateLimit());
        vo.setExpireTime(account.getExpireTime());
        vo.setStatus(account.getStatus());
        vo.setRemark(account.getRemark());
        vo.setAuthorizedApiCount(authorizedCountMap.getOrDefault(account.getId(), 0L));
        vo.setEnabledAuthorizedApiCount(enabledAuthorizedCountMap.getOrDefault(account.getId(), 0L));
        vo.setCreateTime(account.getCreateTime());
        vo.setUpdateTime(account.getUpdateTime());
        return vo;
    }

    private SysApiAccountDetailVO toDetailVo(SysApiAccount account) {
        List<Long> accountIds = List.of(account.getId());
        Map<Long, Long> authorizedCountMap = toCountMap(apiPermissionMapper.selectAuthorizedCountList(accountIds));
        Map<Long, Long> enabledAuthorizedCountMap = toCountMap(
                apiPermissionMapper.selectEnabledAuthorizedCountList(accountIds));
        SysApiAccountVO baseVo = toVo(account, authorizedCountMap, enabledAuthorizedCountMap);
        SysApiAccountDetailVO detailVO = new SysApiAccountDetailVO();
        detailVO.setId(baseVo.getId());
        detailVO.setAccountName(baseVo.getAccountName());
        detailVO.setDescription(baseVo.getDescription());
        detailVO.setAccessKey(baseVo.getAccessKey());
        detailVO.setAccountType(baseVo.getAccountType());
        detailVO.setClientTypes(baseVo.getClientTypes());
        detailVO.setOwnerName(baseVo.getOwnerName());
        detailVO.setOwnerContact(baseVo.getOwnerContact());
        detailVO.setSystemName(baseVo.getSystemName());
        detailVO.setSystemCode(baseVo.getSystemCode());
        detailVO.setAccessEnvironment(baseVo.getAccessEnvironment());
        detailVO.setIpWhitelist(baseVo.getIpWhitelist());
        detailVO.setEnvironmentIpWhitelist(baseVo.getEnvironmentIpWhitelist());
        detailVO.setSignVersion(baseVo.getSignVersion());
        detailVO.setAllowedScopes(baseVo.getAllowedScopes());
        detailVO.setCallbackUrl(baseVo.getCallbackUrl());
        detailVO.setRateLimit(baseVo.getRateLimit());
        detailVO.setExpireTime(baseVo.getExpireTime());
        detailVO.setStatus(baseVo.getStatus());
        detailVO.setRemark(baseVo.getRemark());
        detailVO.setAuthorizedApiCount(baseVo.getAuthorizedApiCount());
        detailVO.setEnabledAuthorizedApiCount(baseVo.getEnabledAuthorizedApiCount());
        detailVO.setCreateTime(baseVo.getCreateTime());
        detailVO.setUpdateTime(baseVo.getUpdateTime());
        return detailVO;
    }

    private ApiCredentialVO buildCredential(SysApiAccount account, String plainSecret) {
        return ApiCredentialVO.builder()
                .id(account.getId())
                .accountName(account.getAccountName())
                .description(account.getDescription())
                .accessKey(account.getAccessKey())
                .clientTypes(ApiClientTypeSupport.deserializeClientTypes(account.getClientTypes()))
                .ownerName(account.getOwnerName())
                .ownerContact(account.getOwnerContact())
                .systemName(account.getSystemName())
                .systemCode(account.getSystemCode())
                .accessEnvironment(account.getAccessEnvironment())
                .ipWhitelist(resolveEffectiveWhitelist(account.getAccessEnvironment(), account.getIpWhitelist(),
                        buildEnvironmentWhitelistMap(account)))
                .environmentIpWhitelist(buildEnvironmentWhitelistMap(account))
                .signVersion(account.getSignVersion())
                .allowedScopes(ApiScopeSupport.deserializeScopes(account.getAllowedScopes()))
                .callbackUrl(account.getCallbackUrl())
                .secretKey(plainSecret)
                .build();
    }

    /**
     * 兼容旧单白名单字段，并把三环境独立维护的白名单统一写回实体。
     * <p>
     * 新版页面会传入完整环境映射，旧版调用方仍可能只传一个 ipWhitelist。
     * 因此这里统一根据当前账号接入环境回填旧字段，确保鉴权链路和兼容接口都能读取到当前生效白名单。
     * </p>
     */
    private void applyWhitelistFields(SysApiAccount account,
            String accessEnvironment,
            List<String> legacyIpWhitelist,
            Map<String, List<String>> environmentIpWhitelist) {
        Map<String, List<String>> normalizedMap = ApiAccountEnvironmentWhitelistSupport
                .normalizeEnvironmentWhitelistMap(environmentIpWhitelist);
        if (normalizedMap.values().stream().allMatch(List::isEmpty) && legacyIpWhitelist != null) {
            normalizedMap.put(accessEnvironment, ApiIpWhitelistSupport.normalizeEntries(legacyIpWhitelist));
        }
        account.setTestIpWhitelist(ApiIpWhitelistSupport.serializeEntries(
                normalizedMap.get(ApiAccountEnvironmentWhitelistSupport.TEST)));
        account.setStagingIpWhitelist(ApiIpWhitelistSupport.serializeEntries(
                normalizedMap.get(ApiAccountEnvironmentWhitelistSupport.STAGING)));
        account.setProductionIpWhitelist(ApiIpWhitelistSupport.serializeEntries(
                normalizedMap.get(ApiAccountEnvironmentWhitelistSupport.PRODUCTION)));
        account.setIpWhitelist(ApiIpWhitelistSupport.serializeEntries(
                resolveEffectiveWhitelist(accessEnvironment, legacyIpWhitelist, normalizedMap)));
    }

    private Map<String, List<String>> buildEnvironmentWhitelistMap(SysApiAccount account) {
        return ApiAccountEnvironmentWhitelistSupport.buildEnvironmentWhitelistMap(
                account.getTestIpWhitelist(),
                account.getStagingIpWhitelist(),
                account.getProductionIpWhitelist());
    }

    private List<String> resolveEffectiveWhitelist(String accessEnvironment,
            String fallbackSerializedWhitelist,
            Map<String, List<String>> environmentWhitelistMap) {
        return resolveEffectiveWhitelist(accessEnvironment,
                ApiIpWhitelistSupport.deserializeEntries(fallbackSerializedWhitelist),
                environmentWhitelistMap);
    }

    private List<String> resolveEffectiveWhitelist(String accessEnvironment,
            List<String> fallbackWhitelist,
            Map<String, List<String>> environmentWhitelistMap) {
        return ApiAccountEnvironmentWhitelistSupport.resolveEffectiveWhitelist(
                accessEnvironment,
                environmentWhitelistMap,
                fallbackWhitelist);
    }

    /**
     * 校验启用 API 账号时是否会超出许可证配额。
     * <p>
     * 仅在账号从「停用」变更为「启用」时触发校验。
     * 统计当前所有启用状态的账号数，如果已达上限则拒绝操作。
     * </p>
     *
     * @param account 当前账号实体（已从数据库加载的原始状态）
     * @param newStatus 即将设置的新状态值
     */
    private void checkEnableQuotaIfNeeded(SysApiAccount account, Integer newStatus) {
        // 仅处理「从停用变更为启用」的场景
        if (!GlobalConstants.STATUS_NORMAL.equals(newStatus)) {
            return;
        }
        if (GlobalConstants.STATUS_NORMAL.equals(account.getStatus())) {
            // 状态未变化（本来就是启用的），无需校验
            return;
        }
        // 统计当前活跃 API 账号数（不含本账号，因为本账号当前是停用状态）
        long activeAccountCount = baseMapper.selectCount(
                new LambdaQueryWrapper<SysApiAccount>()
                        .eq(SysApiAccount::getStatus, GlobalConstants.STATUS_NORMAL));
        licenseQuotaChecker.checkEnableApiAccountQuota(activeAccountCount);
    }

    /**
     * 校验账号名称唯一性。
     *
     * <p>直接调用 {@code baseMapper.selectCount()} 而非 {@code this.lambdaQuery()}，
     * 原因：{@code ServiceImpl} 的链式查询方法内部通过 {@code SqlHelper.getMapper()} 获取
     * MyBatis 代理，在单元测试中 mock {@code baseMapper} 时无法被拦截，
     * 直接调用 {@code baseMapper} 方法则可以被 Mockito 正常 mock。</p>
     *
     * @param accountName 账号名称
     * @param excludeId   更新时排除的账号 ID，新增时传 null
     * @throws BusinessException 名称已存在时抛出
     */
    private void validateAccountNameUnique(String accountName, Long excludeId) {
        LambdaQueryWrapper<SysApiAccount> wrapper = new LambdaQueryWrapper<SysApiAccount>()
                .eq(SysApiAccount::getAccountName, accountName)
                .ne(excludeId != null, SysApiAccount::getId, excludeId);
        long count = baseMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("API账号名称已存在");
        }
    }

    /**
     * 生成全局唯一的 AccessKey。
     *
     * <p>同 {@link #validateAccountNameUnique}，直接调用 {@code baseMapper.selectCount()}
     * 以确保单元测试中 mock 可以正常生效。</p>
     *
     * @return 唯一的 AccessKey 字符串
     */
    private String generateAccessKey() {
        String accessKey;
        do {
            accessKey = "ak_" + IdUtil.simpleUUID();
        } while (baseMapper.selectCount(
                new LambdaQueryWrapper<SysApiAccount>()
                        .eq(SysApiAccount::getAccessKey, accessKey)) > 0);
        return accessKey;
    }

    private String normalizeAccountName(String accountName) {
        String normalized = StrUtil.trim(accountName);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("API账号名称不能为空");
        }
        return normalized;
    }

    private String normalizeOwnerName(String ownerName) {
        String normalized = StrUtil.trim(ownerName);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("接入方负责人不能为空");
        }
        return normalized;
    }

    private String normalizeOwnerContact(String ownerContact) {
        String normalized = StrUtil.trim(ownerContact);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("负责人联系方式不能为空");
        }
        return normalized;
    }

    private String normalizeSystemName(String systemName) {
        String normalized = StrUtil.trim(systemName);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("业务系统名称不能为空");
        }
        return normalized;
    }

    private String normalizeSystemCode(String systemCode) {
        String normalized = StrUtil.trim(systemCode);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("业务系统编码不能为空");
        }
        if (!normalized.matches("^[A-Za-z0-9_-]{2,64}$")) {
            throw new BusinessException("业务系统编码仅支持2到64位字母、数字、下划线和中划线");
        }
        return normalized.toUpperCase();
    }

    private String normalizeAccessEnvironment(String accessEnvironment) {
        String normalized = ApiAccountEnvironmentSupport.normalizeEnvironment(accessEnvironment);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("接入环境不能为空");
        }
        return normalized;
    }

    private String normalizeSignVersion(String signVersion) {
        String normalized = ApiSignatureVersionSupport.normalizeVersion(signVersion);
        if (StrUtil.isBlank(normalized)) {
            throw new BusinessException("签名算法版本不能为空");
        }
        return normalized;
    }

    private String normalizeCallbackUrl(String callbackUrl) {
        String normalized = StrUtil.trimToNull(callbackUrl);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = new URI(normalized);
            String scheme = StrUtil.blankToDefault(uri.getScheme(), "").toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new BusinessException("回调地址仅支持 http 或 https 协议");
            }
            if (StrUtil.isBlank(uri.getHost())) {
                throw new BusinessException("回调地址必须包含合法域名");
            }
            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new BusinessException("回调地址格式不正确");
        }
    }

    private String normalizeDescription(String description) {
        return StrUtil.trimToNull(description);
    }

    private String normalizeRemark(String remark) {
        return StrUtil.trimToNull(remark);
    }

    private String normalizeQueryText(String value) {
        return StrUtil.trimToNull(value);
    }

    private boolean isExactTextMatchMode(String matchMode) {
        String normalized = matchMode == null ? "" : matchMode.trim();
        return TEXT_MATCH_MODE_EXACT.equalsIgnoreCase(normalized);
    }

    private void applyStringFilter(LambdaQueryWrapper<SysApiAccount> wrapper,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<SysApiAccount, String> column,
            String keyword,
            boolean exactTextMatch) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        if (exactTextMatch) {
            wrapper.eq(column, keyword);
            return;
        }
        wrapper.like(column, keyword);
    }

    private LocalDateTime parseDateTimeQuery(String value, String fieldName, boolean useDayEndWhenOnlyDate) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            if (normalized.length() == 10) {
                LocalDate date = LocalDate.parse(normalized, DATE_FORMATTER);
                return useDayEndWhenOnlyDate ? LocalDateTime.of(date, LocalTime.of(23, 59, 59)) : date.atStartOfDay();
            }
            return LocalDateTime.parse(normalized, DATETIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(fieldName + "格式不正确，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private <T extends Comparable<T>> void validateRange(T start, T end, String rangeName) {
        if (start != null && end != null && start.compareTo(end) > 0) {
            throw new BusinessException(rangeName + "不合法：开始值不能大于结束值");
        }
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize.longValue(), MAX_PAGE_SIZE);
    }
}
