package com.bml.module.enterprise.service;

import cn.hutool.core.util.StrUtil;
import com.bml.core.base.dto.PageQuery;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.common.result.PageResult;
import com.bml.module.enterprise.dto.EnterpriseCompanyPageQuery;
import com.bml.module.enterprise.dto.EnterpriseSystemAccountPageQuery;
import com.bml.module.enterprise.vo.OpenEnterpriseCompanyVO;
import com.bml.module.enterprise.vo.OpenEnterpriseDashboardVO;
import com.bml.module.enterprise.vo.OpenEnterpriseSystemAccountVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 企业开放接口演示服务。
 * <p>
 * 当前阶段使用内置样板数据承载企业管理前台的接口骨架，
 * 目的是先把「开放接口定义 -> 自动同步目录 -> BML API 账号授权」链路打通。
 * 后续切换真实数据源时，只需要将这里替换成数据库或业务服务调用即可，控制器和授权模型都无需改动。
 * </p>
 */
@Service
public class OpenEnterpriseDemoService {

    private final List<OpenEnterpriseCompanyVO> companySamples = buildCompanySamples();
    private final List<OpenEnterpriseSystemAccountVO> systemAccountSamples = buildSystemAccountSamples();

    public OpenEnterpriseDashboardVO getDashboardSummary() {
        OpenEnterpriseDashboardVO summary = new OpenEnterpriseDashboardVO();
        summary.setCompanyCount(companySamples.size());
        summary.setEnabledCompanyCount((int) companySamples.stream().filter(item -> Objects.equals(item.getStatus(), 1)).count());
        summary.setSystemAccountCount(systemAccountSamples.size());
        summary.setEnabledSystemAccountCount((int) systemAccountSamples.stream()
                .filter(item -> Objects.equals(item.getStatus(), 1))
                .count());
        return summary;
    }

    public PageResult<OpenEnterpriseCompanyVO> pageCompanies(EnterpriseCompanyPageQuery query) {
        List<OpenEnterpriseCompanyVO> filtered = companySamples.stream()
                .filter(buildCompanyKeywordPredicate(query == null ? null : query.getKeyword()))
                .filter(item -> query == null || query.getStatus() == null || Objects.equals(item.getStatus(), query.getStatus()))
                .filter(item -> query == null || StrUtil.isBlank(query.getIndustry())
                        || StrUtil.equalsIgnoreCase(item.getIndustry(), StrUtil.trim(query.getIndustry())))
                .sorted(Comparator.comparing(OpenEnterpriseCompanyVO::getUpdatedTime).reversed())
                .toList();
        return buildPageResult(filtered, query);
    }

    public OpenEnterpriseCompanyVO getCompanyDetail(Long companyId) {
        return companySamples.stream()
                .filter(item -> Objects.equals(item.getCompanyId(), companyId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("企业档案不存在或已被移除"));
    }

    public PageResult<OpenEnterpriseSystemAccountVO> pageSystemAccounts(EnterpriseSystemAccountPageQuery query) {
        List<OpenEnterpriseSystemAccountVO> filtered = systemAccountSamples.stream()
                .filter(buildAccountKeywordPredicate(query == null ? null : query.getKeyword()))
                .filter(item -> query == null || query.getStatus() == null || Objects.equals(item.getStatus(), query.getStatus()))
                .filter(item -> query == null || query.getCompanyId() == null || Objects.equals(item.getCompanyId(), query.getCompanyId()))
                .filter(item -> query == null || StrUtil.isBlank(query.getAccountType())
                        || StrUtil.equalsIgnoreCase(item.getAccountType(), StrUtil.trim(query.getAccountType())))
                .sorted(Comparator.comparing(OpenEnterpriseSystemAccountVO::getCreatedTime).reversed())
                .toList();
        return buildPageResult(filtered, query);
    }

    public OpenEnterpriseSystemAccountVO getSystemAccountDetail(Long accountId) {
        return systemAccountSamples.stream()
                .filter(item -> Objects.equals(item.getAccountId(), accountId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("企业系统账号不存在或已被移除"));
    }

    private Predicate<OpenEnterpriseCompanyVO> buildCompanyKeywordPredicate(String keyword) {
        String normalized = StrUtil.trimToEmpty(keyword);
        if (StrUtil.isBlank(normalized)) {
            return item -> true;
        }
        return item -> StrUtil.containsIgnoreCase(item.getCompanyName(), normalized)
                || StrUtil.containsIgnoreCase(item.getCompanyCode(), normalized)
                || StrUtil.containsIgnoreCase(item.getContactPerson(), normalized);
    }

    private Predicate<OpenEnterpriseSystemAccountVO> buildAccountKeywordPredicate(String keyword) {
        String normalized = StrUtil.trimToEmpty(keyword);
        if (StrUtil.isBlank(normalized)) {
            return item -> true;
        }
        return item -> StrUtil.containsIgnoreCase(item.getUsername(), normalized)
                || StrUtil.containsIgnoreCase(item.getDisplayName(), normalized)
                || StrUtil.containsIgnoreCase(item.getCompanyName(), normalized);
    }

    private <T> PageResult<T> buildPageResult(List<T> source, PageQuery query) {
        long pageNum = query == null || query.getPageNum() == null || query.getPageNum() < 1 ? 1L : query.getPageNum();
        long pageSize = query == null || query.getPageSize() == null || query.getPageSize() < 1 ? 10L : query.getPageSize();
        int fromIndex = Math.min((int) ((pageNum - 1) * pageSize), source.size());
        int toIndex = Math.min(fromIndex + (int) pageSize, source.size());
        return PageResult.of(source.subList(fromIndex, toIndex), source.size(), pageNum, pageSize);
    }

    private List<OpenEnterpriseCompanyVO> buildCompanySamples() {
        return List.of(
                buildCompany(1001L, "ENT-2026001", "龙域智能科技", "智能制造", "上海",
                        "张雨辰", "13800001234", 1, 126, 18,
                        LocalDateTime.of(2025, 11, 12, 9, 30, 0),
                        LocalDateTime.of(2026, 2, 24, 15, 10, 0)),
                buildCompany(1002L, "ENT-2026002", "华东智联供应链", "供应链", "杭州",
                        "周明哲", "13900004567", 1, 84, 11,
                        LocalDateTime.of(2025, 10, 6, 10, 0, 0),
                        LocalDateTime.of(2026, 2, 20, 11, 42, 0)),
                buildCompany(1003L, "ENT-2026003", "云桥企业服务", "企业服务", "深圳",
                        "林可欣", "13700007890", 0, 42, 7,
                        LocalDateTime.of(2025, 8, 18, 14, 20, 0),
                        LocalDateTime.of(2026, 1, 29, 18, 5, 0)));
    }

    private List<OpenEnterpriseSystemAccountVO> buildSystemAccountSamples() {
        return List.of(
                buildSystemAccount(3001L, 1001L, "龙域智能科技", "admin.longyu", "龙域平台主管",
                        "admin", 1, "企业管理员、审批中心",
                        LocalDateTime.of(2026, 2, 27, 9, 12, 0),
                        LocalDateTime.of(2025, 11, 12, 10, 10, 0)),
                buildSystemAccount(3002L, 1001L, "龙域智能科技", "ops.longyu", "运营专员",
                        "operator", 1, "运营台、企业档案维护",
                        LocalDateTime.of(2026, 2, 26, 18, 8, 0),
                        LocalDateTime.of(2025, 11, 13, 9, 40, 0)),
                buildSystemAccount(3003L, 1002L, "华东智联供应链", "audit.huadong", "审计经理",
                        "auditor", 1, "审计台、日志导出",
                        LocalDateTime.of(2026, 2, 25, 13, 30, 0),
                        LocalDateTime.of(2025, 10, 8, 11, 0, 0)),
                buildSystemAccount(3004L, 1003L, "云桥企业服务", "service.yunqiao", "客服主管",
                        "operator", 0, "客服工单、客户回访",
                        LocalDateTime.of(2026, 1, 30, 17, 20, 0),
                        LocalDateTime.of(2025, 8, 20, 16, 15, 0)));
    }

    private OpenEnterpriseCompanyVO buildCompany(Long companyId,
            String companyCode,
            String companyName,
            String industry,
            String city,
            String contactPerson,
            String contactPhone,
            Integer status,
            Integer memberCount,
            Integer systemAccountCount,
            LocalDateTime createdTime,
            LocalDateTime updatedTime) {
        OpenEnterpriseCompanyVO company = new OpenEnterpriseCompanyVO();
        company.setCompanyId(companyId);
        company.setCompanyCode(companyCode);
        company.setCompanyName(companyName);
        company.setIndustry(industry);
        company.setCity(city);
        company.setContactPerson(contactPerson);
        company.setContactPhone(contactPhone);
        company.setStatus(status);
        company.setMemberCount(memberCount);
        company.setSystemAccountCount(systemAccountCount);
        company.setCreatedTime(createdTime);
        company.setUpdatedTime(updatedTime);
        return company;
    }

    private OpenEnterpriseSystemAccountVO buildSystemAccount(Long accountId,
            Long companyId,
            String companyName,
            String username,
            String displayName,
            String accountType,
            Integer status,
            String roleSummary,
            LocalDateTime lastLoginTime,
            LocalDateTime createdTime) {
        OpenEnterpriseSystemAccountVO account = new OpenEnterpriseSystemAccountVO();
        account.setAccountId(accountId);
        account.setCompanyId(companyId);
        account.setCompanyName(companyName);
        account.setUsername(username);
        account.setDisplayName(displayName);
        account.setAccountType(accountType);
        account.setStatus(status);
        account.setRoleSummary(roleSummary);
        account.setLastLoginTime(lastLoginTime);
        account.setCreatedTime(createdTime);
        return account;
    }
}
